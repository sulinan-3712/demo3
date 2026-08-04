package com.example.demo.modbusRTU;

import com.example.demo.modbusRTU.config.ModbusConfig;
import com.example.demo.modbusRTU.model.Gs8000Event;
import com.example.demo.modbusRTU.service.Gs8000ModbusService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;


/**
 * 功能描述: 任务队列
 */
@Component
@Slf4j
public class Gs8000Runner implements CommandLineRunner {

    @Override
    public void run(String... args) throws Exception {
        log.info("========== GS8000 Modbus 通信服务启动 ==========");

        // 1. 加载配置（支持 VM 参数覆盖）
        ModbusConfig config = ModbusConfig.fromSystemProperties();

        // 2. 创建服务并注入事件监听器（业务持久化逻辑在此）
        Gs8000ModbusService service = new Gs8000ModbusService(config, new Gs8000ModbusService.EventListener() {
            @Override
            public void onEvent(Gs8000Event event) {
                // 例如：插入数据库、发送MQ、推送告警等
                log.info("【业务处理】事件已接收: {}", event);

                // 示例：根据不同事件类型做不同处理
                switch (event.getEventType()) {
                    case 0x01:
                        log.warn("🚨 火警事件！设备: {}, 地址: {}",
                                event.getDeviceTypeName(), event.getDeviceCode());
                        // TODO: 调用告警服务
                        break;
                    case 0x02:
                        log.error("⚠️ 故障事件！设备: {}, 地址: {}",
                                event.getDeviceTypeName(), event.getDeviceCode());
                        // TODO: 记录故障日志
                        break;
                    case 0x0A:
                        log.info("🔄 复位操作事件");
                        break;
                    default:
                        log.info("📌 普通事件: {}", event.getEventTypeName());
                }
            }
        });

        // 3. 启动轮询
        service.start();
    }


}


