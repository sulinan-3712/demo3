package com.example.demo.modbusRTU.service;

import com.example.demo.modbusRTU.JSerialCommSerialPortWrapper;
import com.example.demo.modbusRTU.config.ModbusConfig;
import com.example.demo.modbusRTU.model.DeviceTypeMapper;
import com.example.demo.modbusRTU.model.Gs8000Event;
import com.fazecast.jSerialComm.SerialPort;
import com.serotonin.modbus4j.ModbusFactory;
import com.serotonin.modbus4j.ModbusMaster;
import com.serotonin.modbus4j.exception.ModbusInitException;
import com.serotonin.modbus4j.msg.ReadHoldingRegistersRequest;
import com.serotonin.modbus4j.msg.ReadHoldingRegistersResponse;
import com.serotonin.modbus4j.msg.WriteRegistersRequest;
import com.serotonin.modbus4j.msg.WriteRegistersResponse;
import com.serotonin.modbus4j.serial.SerialPortWrapper;
import com.serotonin.modbus4j.sero.messaging.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class Gs8000ModbusService {

    private static final Logger log = LoggerFactory.getLogger(Gs8000ModbusService.class);

    /**
     * 初始化（串口打开）失败后的自动重试间隔
     */
    private static final long INIT_RETRY_DELAY_MS = 5000L;

    private final ModbusConfig config;
    private ModbusMaster master;
    private ScheduledExecutorService scheduler = newScheduler("modbus-poll");
    /**
     * 专门用于"初始化/重连失败后重试"的调度器，与轮询线程分离，
     * 避免初始化失败时 pollTask 未建立、服务静默死亡的问题。
     */
    private ScheduledExecutorService retryScheduler = newScheduler("modbus-init-retry");
    private ScheduledFuture<?> pollTask;
    /** 当前待执行的重试任务，用于 stop() 时取消 */
    private ScheduledFuture<?> retryTask;

    private static ScheduledExecutorService newScheduler(String name) {
        return Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, name);
            t.setDaemon(true);
            return t;
        });
    }

    private final EventListener listener;
    private final AtomicInteger consecutiveFailures = new AtomicInteger(0);
    private volatile boolean running = false;

    /**
     * 串口是否已成功打开。
     * true  = 串口已连接（此时若轮询失败，说明是从站无响应）；
     * false = 串口未连上（端口不存在/被占用/无权限）。
     */
    private volatile boolean portConnected = false;

    private volatile boolean hookRegistered = false;

    /**
     * 是否已调用过 stop()。
     * 用于防止 stop() 之后，先前已排队的重试任务重新拉起服务（"复活"问题）。
     */
    private volatile boolean stopped = false;

    public interface EventListener {
        void onEvent(Gs8000Event event);
    }

    public Gs8000ModbusService(ModbusConfig config, EventListener listener) {
        this.config = config;
        this.listener = listener;
    }

    /**
     * 初始化 Modbus Master（可独立调用，用于重连）。
     * <p>
     * 诊断要点：此处抛出的 {@link ModbusInitException} 表示"串口没有连接上"——
     * 端口不存在、被占用、无权限或驱动缺失，请求根本无法发出。
     */
    private synchronized void initMaster() throws ModbusInitException {
        // 销毁旧连接
        if (master != null) {
            try {
                master.destroy();
            } catch (Exception e) {
                log.error("销毁旧连接时异常: {}", e.getMessage());
            }
        }

        // 列出系统当前可用串口，便于诊断
        SerialPort[] availablePorts = SerialPort.getCommPorts();
        String[] portNames = new String[availablePorts.length];
        for (int i = 0; i < availablePorts.length; i++) {
            portNames[i] = availablePorts[i].getSystemPortName();
        }
        log.info("系统当前可用串口: {}", portNames.length == 0 ? "(无)" : String.join(", ", portNames));

        // 校验配置的串口是否存在
        boolean portExists = Arrays.stream(availablePorts)
                .anyMatch(p -> config.getPortName().equals(p.getSystemPortName()));
        if (!portExists) {
            throw new ModbusInitException("配置的串口 " + config.getPortName() + " 不存在！"
                    + "当前可用串口: " + (portNames.length == 0 ? "(无)" : String.join(", ", portNames))
                    + "（可用 -Dmodbus.port=xxx 指定正确串口）");
        }

        // 创建串口包装器
        SerialPortWrapper wrapper = new JSerialCommSerialPortWrapper(
                config.getPortName(),
                config.getBaudRate(),
                8,                  // 数据位
                1,                          // 停止位
                SerialPort.NO_PARITY
        );

        ModbusFactory factory = new ModbusFactory();
        this.master = factory.createRtuMaster(wrapper);

        // 应用配置的超时与重试参数（modbus4j 默认 500ms × 2 次，这里改为使用 -Dmodbus.timeout）
        this.master.setTimeout(config.getTimeoutMillis());
        this.master.setRetries(1);

        // 打开串口，失败会抛 ModbusInitException（端口被占用、权限不足等）
        this.master.init();

        this.portConnected = true;
        log.info("Modbus Master 初始化成功，串口: {}, 波特率: {}, 从站ID: {}, 超时: {}ms, 重试: {}",
                config.getPortName(), config.getBaudRate(), config.getSlaveId(),
                config.getTimeoutMillis(), master.getRetries());
    }

    /**
     * 启动服务。
     * 初始化（打开串口）失败时不会静默退出，而是定时自动重试。
     */
    public synchronized void start() {
        if (running) {
            log.error("服务已启动，无需重复启动");
            return;
        }
        stopped = false;

        // 若之前被 stop() 关闭过调度器，则重建（支持 stop 后再次 start）
        if (scheduler.isShutdown()) {
            scheduler = newScheduler("modbus-poll");
        }
        if (retryScheduler.isShutdown()) {
            retryScheduler = newScheduler("modbus-init-retry");
        }

        // 注册JVM关闭钩子（只注册一次）
        if (!hookRegistered) {
            hookRegistered = true;
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                log.info("JVM关闭，停止服务...");
                stop();
            }));
        }

        try {
            initMaster();
            startPolling();
            log.info("Modbus 服务启动成功");
        } catch (ModbusInitException e) {
            // 诊断结论：串口未连接成功（端口不存在 / 被占用 / 无权限）
            log.error("Modbus 初始化失败：无法打开串口 {}。原因: {}", config.getPortName(), e.getMessage());
            log.error("【诊断】串口未连接成功（端口不存在、被占用或无权限），"
                    + "{}ms 后将自动重试连接...", INIT_RETRY_DELAY_MS);
            scheduleRetryInit();
        }
    }

    /**
     * 停止服务
     */
    public synchronized void stop() {
        if (!running && stopped) return;
        stopped = true;
        running = false;
        portConnected = false;

        if (pollTask != null) {
            pollTask.cancel(false);
            pollTask = null;
        }

        // 取消待执行的重试任务，防止 stop 之后服务被重新拉起
        if (retryTask != null) {
            retryTask.cancel(false);
            retryTask = null;
        }

        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(2, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        retryScheduler.shutdownNow();

        if (master != null) {
            try {
                master.destroy();
            } catch (Exception e) {
                log.error("销毁 Master 异常: {}", e.getMessage());
            }
        }

        log.info("Modbus 服务已停止");
    }

    /**
     * 启动定时轮询任务
     */
    private void startPolling() {
        running = true;
        consecutiveFailures.set(0);

        // 定时轮询（文档要求 >300ms，默认500ms）
        pollTask = scheduler.scheduleAtFixedRate(
                this::pollAndProcess,
                1000,  // 延迟1秒启动（给设备稳定时间）
                config.getPollIntervalMillis(),
                TimeUnit.MILLISECONDS
        );

        log.info("轮询任务已启动，间隔: {}ms", config.getPollIntervalMillis());
    }

    /**
     * 初始化失败后的自动重试（使用独立的 retryScheduler）
     */
    private synchronized void scheduleRetryInit() {
        if (stopped) return;
        try {
            // 取消上一个未执行的重试，避免堆积多个重试任务
            if (retryTask != null) {
                retryTask.cancel(false);
            }
            retryTask = retryScheduler.schedule(this::retryInitTask, INIT_RETRY_DELAY_MS, TimeUnit.MILLISECONDS);
        } catch (RejectedExecutionException e) {
            log.warn("重试任务被拒绝，服务可能已停止: {}", e.getMessage());
        }
    }

    /**
     * 重试初始化任务：尝试重新打开串口，成功后恢复轮询
     */
    private synchronized void retryInitTask() {
        if (running || stopped) return;
        retryTask = null; // 本次任务已开始执行

        log.info("========== 重试初始化 Modbus 连接 ==========");
        try {
            initMaster();
            startPolling();
            log.info("初始化成功，轮询已恢复");
        } catch (ModbusInitException e) {
            log.error("初始化仍失败（串口未连接）：{}，{}ms 后再次重试",
                    e.getMessage(), INIT_RETRY_DELAY_MS);
            scheduleRetryInit();
        }
    }

    /**
     * 轮询主逻辑
     */
    private void pollAndProcess() {
        if (!running) {
            log.error("服务已停止，跳过轮询");
            return;
        }

        log.info("========== 开始轮询 ==========");

        try {
            // 1. 读取 40001~40004 (偏移 0x0000)
            ReadHoldingRegistersResponse response = readRegisters(0x0000, 4);

            if (response == null) {
                // 具体原因（超时/通信异常）已在 readRegisters 中输出 ERROR 日志
                log.warn("读取失败：未获得有效响应（原因见上方 ERROR 日志，portConnected={}）", portConnected);
                consecutiveFailures.incrementAndGet();
                checkAndReconnect();
                return;
            }

            if (response.isException()) {
                log.warn("响应异常: {}", response.getExceptionMessage());
                consecutiveFailures.incrementAndGet();
                checkAndReconnect();
                return;
            }

            // 成功读取数据
            short[] data = response.getShortData();
            log.info("原始寄存器数据: {}", Arrays.toString(data));

            // 解析事件
            Gs8000Event event = parseEvent(data);
            if (event != null && event.isValid()) {
                if (listener != null) {
                    listener.onEvent(event);
                }
                log.info("✅ 收到事件: {}", event);
            } else {
                log.error("无新事件 (事件类型=0x00)");
            }

            // 重置失败计数
            consecutiveFailures.set(0);

        } catch (Exception e) {
            log.error("轮询异常", e);
            int failCount = consecutiveFailures.incrementAndGet();
            log.error("连续失败次数: {}/{}", failCount, config.getMaxRetryCount());
            if (failCount >= config.getMaxRetryCount()) {
                log.error("连续失败次数超限，尝试重连...");
                reconnect();
            }
        }
    }

    /**
     * 读取保持寄存器（带超时处理）
     * <p>
     * 诊断要点：master.send 抛出的 {@link ModbusTransportException} 若 cause 链中包含
     * {@link TimeoutException}，说明"串口已连接、请求已发送，但从站在超时时间内没有响应"——
     * 请检查从站地址、波特率、A/B 接线、设备电源；这与 init 阶段"串口没连接上"
     * （{@link ModbusInitException}）是两种不同故障。
     */
    private ReadHoldingRegistersResponse readRegisters(int startOffset, int quantity) {
        try {
            ReadHoldingRegistersRequest request = new ReadHoldingRegistersRequest(
                    config.getSlaveId(), startOffset, quantity);

            long startTime = System.currentTimeMillis();
            ReadHoldingRegistersResponse response = (ReadHoldingRegistersResponse) master.send(request);
            long cost = System.currentTimeMillis() - startTime;

            log.info("读取寄存器 [offset={}, qty={}] 成功，耗时: {}ms", startOffset, quantity, cost);
            return response;

        } catch (Exception e) {
            if (isTimeoutException(e)) {
                // 请求已写入串口（TX已发出），但超时时间内未等到从站响应
                log.error("读取寄存器超时 [offset={}, qty={}]：串口已连接（portConnected=true），请求已发送，"
                                + "但从站在 {}ms 内未响应。请检查：从站地址(slave={})、波特率({})、A/B 接线、设备电源。",
                        startOffset, quantity, config.getTimeoutMillis(), config.getSlaveId(), config.getBaudRate());
            } else {
                // 其他串口通信异常
                log.error("读取寄存器失败 [offset={}, qty={}]：串口通信异常: {}", startOffset, quantity, e.toString());
            }
            return null;
        }
    }

    /**
     * 判断异常（含 cause 链）是否为 modbus4j 的 TimeoutException：
     * 表示请求已发出但从站未在超时时间内响应（串口本身是通的）。
     */
    private static boolean isTimeoutException(Throwable e) {
        Throwable t = e;
        while (t != null) {
            if (t instanceof TimeoutException) {
                return true;
            }
            t = t.getCause();
        }
        return false;
    }

    /**
     * 解析 4 个寄存器为事件对象
     */
    private Gs8000Event parseEvent(short[] data) {
        if (data == null || data.length < 4) {
            log.info("数据长度不足: {}", data == null ? "null" : data.length);
            return null;
        }

        int reg0 = data[0] & 0xFFFF;
        int reg1 = data[1] & 0xFFFF;
        int reg2 = data[2] & 0xFFFF;
        int reg3 = data[3] & 0xFFFF;

        int eventType = (reg0 >> 8) & 0xFF;
        int lowByte = reg0 & 0xFF;  // 低字节保留

        int deviceType = (reg1 >> 8) & 0xFF;
        String deviceTypeName = DeviceTypeMapper.getName(deviceType);

        // 控制器地址: BCD码组合
        int addrHundreds = reg1 & 0xFF;
        int addrTens = (reg2 >> 12) & 0x0F;
        int addrUnits = (reg2 >> 8) & 0x0F;
        int controllerAddr = addrHundreds * 100 + addrTens * 10 + addrUnits;

        // 回路号
        int loopTens = (reg2 >> 4) & 0x0F;
        int loopUnits = reg2 & 0x0F;
        int loopNo = loopTens * 10 + loopUnits;

        // 设备编码
        int codeHundreds = (reg3 >> 8) & 0xFF;
        int codeTens = (reg3 >> 4) & 0x0F;
        int codeUnits = reg3 & 0x0F;
        int deviceCode = codeHundreds * 100 + codeTens * 10 + codeUnits;

        return new Gs8000Event(eventType, deviceType, deviceTypeName,
                controllerAddr, loopNo, deviceCode);
    }

    /**
     * 检查是否需要重连
     */
    private void checkAndReconnect() {
        int failCount = consecutiveFailures.get();
        if (failCount >= config.getMaxRetryCount()) {
            log.error("连续失败 {} 次，触发重连", failCount);
            reconnect();
        }
    }

    /**
     * 重连。重连（重新打开串口）失败时自动定时重试，不再静默退出。
     */
    private synchronized void reconnect() {
        try {
            log.info("开始重连...");
            running = false;
            portConnected = false;

            // 取消当前轮询任务
            if (pollTask != null) {
                pollTask.cancel(false);
                pollTask = null;
            }

            // 销毁旧 Master
            if (master != null) {
                master.destroy();
                master = null;
            }

            // 等待端口释放
            Thread.sleep(1000);

            // 重新初始化
            initMaster();
            startPolling();
            log.info("重连成功，轮询已恢复");

        } catch (ModbusInitException e) {
            log.error("重连失败：无法打开串口 {}（端口不存在、被占用或无权限）。{}ms 后自动重试...",
                    config.getPortName(), INIT_RETRY_DELAY_MS);
            running = false;
            scheduleRetryInit();
        } catch (Exception e) {
            log.error("重连异常", e);
            running = false;
            scheduleRetryInit();
        }
    }

    /**
     * ===================== 对时功能 =====================
     */
    public boolean syncTime(LocalDateTime datetime) {
        int year = datetime.getYear() - 2000;
        int month = datetime.getMonthValue();
        int day = datetime.getDayOfMonth();
        int hour = datetime.getHour();
        int minute = datetime.getMinute();
        int second = datetime.getSecond();

        // 范围校验
        if (year < 1 || year > 37 || month < 1 || month > 12 || day < 1 || day > 31 ||
                hour < 0 || hour > 23 || minute < 0 || minute > 59 || second < 0 || second > 59) {
            log.error("对时参数超出范围: {}", datetime);
            return false;
        }

        int reg20 = (year << 8) | month;
        int reg21 = (day << 8) | hour;
        int reg22 = (minute << 8) | second;

        short[] values = new short[]{(short) reg20, (short) reg21, (short) reg22};

        try {
            WriteRegistersRequest request = new WriteRegistersRequest(
                    config.getSlaveId(), 0x0020, values);
            WriteRegistersResponse response = (WriteRegistersResponse) master.send(request);

            if (!response.isException()) {
                log.info("对时成功: {}", datetime);
                return true;
            } else {
                log.error("对时响应异常: {}", response.getExceptionMessage());
                return false;
            }
        } catch (Exception e) {
            if (isTimeoutException(e)) {
                log.error("对时请求超时：串口已连接但从站无响应（{}ms）。请检查从站地址/接线/电源。",
                        config.getTimeoutMillis());
            } else {
                log.error("对时请求发送失败", e);
            }
            return false;
        }
    }

    public boolean isRunning() {
        return running;
    }

    /**
     * 当前连接状态摘要，便于排查问题
     */
    public String getStatus() {
        return "running=" + running
                + ", portConnected=" + portConnected
                + ", master=" + (master != null)
                + ", consecutiveFailures=" + consecutiveFailures.get()
                + ", port=" + config.getPortName()
                + ", baud=" + config.getBaudRate()
                + ", slave=" + config.getSlaveId()
                + ", timeout=" + config.getTimeoutMillis() + "ms";
    }
}
