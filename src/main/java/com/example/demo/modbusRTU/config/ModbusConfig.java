package com.example.demo.modbusRTU.config;

import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Data
public class ModbusConfig {

    private static final Logger log = LoggerFactory.getLogger(ModbusConfig.class);

    // 默认值（可通过 VM 参数或环境变量覆盖）
    private static final String DEFAULT_PORT_WINDOWS = "COM3";
    private static final String DEFAULT_PORT_LINUX = "/dev/ttyUSB0";

    private final String portName;
    private final int baudRate;
    private final int slaveId;
    private final long pollIntervalMillis;
    private final int maxRetryCount;
    private final int timeoutMillis;

    private ModbusConfig(Builder builder) {
        this.portName = builder.portName;
        this.baudRate = builder.baudRate;
        this.slaveId = builder.slaveId;
        this.pollIntervalMillis = builder.pollIntervalMillis;
        this.maxRetryCount = builder.maxRetryCount;
        this.timeoutMillis = builder.timeoutMillis;
    }

    /**
     * 自动识别操作系统，返回默认串口号
     */
    public static String getDefaultPort() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            log.info("当前操作系统为Windows，默认使用COM3串口");
            return DEFAULT_PORT_WINDOWS;
        } else if (os.contains("linux") || os.contains("nix") || os.contains("nux")) {
            log.info("当前操作系统为Linux，默认使用/dev/ttyUSB0串口");
            return DEFAULT_PORT_LINUX;
        } else {
            log.warn("未知操作系统，默认使用Linux串口路径: {}", DEFAULT_PORT_LINUX);
            return DEFAULT_PORT_LINUX;
        }
    }

    /**
     * 从 VM 参数 / 环境变量构建配置
     * VM参数示例: -Dmodbus.port=COM5 -Dmodbus.baud=4800 -Dmodbus.slave=1
     */
    public static ModbusConfig fromSystemProperties() {
        String port = System.getProperty("modbus.port", getDefaultPort());
        int baud = Integer.getInteger("modbus.baud", 4800);
        int slave = Integer.getInteger("modbus.slave", 1);
        long poll = Long.getLong("modbus.poll", 500L);
        int retry = Integer.getInteger("modbus.retry", 5);
        int timeout = Integer.getInteger("modbus.timeout", 2000);

        log.info("加载配置: port={}, baud={}, slave={}, poll={}ms, retry={}, timeout={}ms",
                port, baud, slave, poll, retry, timeout);

        return new Builder()
                .portName(port)
                .baudRate(baud)
                .slaveId(slave)
                .pollIntervalMillis(poll)
                .maxRetryCount(retry)
                .timeoutMillis(timeout)
                .build();
    }

    // ---------- Builder ----------
    public static class Builder {
        private String portName;
        private int baudRate = 4800;
        private int slaveId = 1;
        private long pollIntervalMillis = 5000;
        private int maxRetryCount = 5;
        private int timeoutMillis = 2000;

        public Builder portName(String portName) { this.portName = portName; return this; }
        public Builder baudRate(int baudRate) { this.baudRate = baudRate; return this; }
        public Builder slaveId(int slaveId) { this.slaveId = slaveId; return this; }
        public Builder pollIntervalMillis(long pollIntervalMillis) { this.pollIntervalMillis = pollIntervalMillis; return this; }
        public Builder maxRetryCount(int maxRetryCount) { this.maxRetryCount = maxRetryCount; return this; }
        public Builder timeoutMillis(int timeoutMillis) { this.timeoutMillis = timeoutMillis; return this; }
        public ModbusConfig build() { return new ModbusConfig(this); }
    }
}