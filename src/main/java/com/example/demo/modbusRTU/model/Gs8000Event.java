package com.example.demo.modbusRTU.model;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Gs8000Event {

    private final int eventType;          // 0x01~0x0B
    private final int deviceType;         // 设备类型码（见附录二）
    private final String deviceTypeName;  // 设备类型中文名
    private final int controllerAddr;     // 0~255
    private final int loopNo;             // 0~99
    private final int deviceCode;         // 0~255
    private final long timestampMillis;
    private final LocalDateTime timestamp;

    public Gs8000Event(int eventType, int deviceType, String deviceTypeName,
                       int controllerAddr, int loopNo, int deviceCode) {
        this.eventType = eventType;
        this.deviceType = deviceType;
        this.deviceTypeName = deviceTypeName;
        this.controllerAddr = controllerAddr;
        this.loopNo = loopNo;
        this.deviceCode = deviceCode;
        this.timestampMillis = System.currentTimeMillis();
        this.timestamp = LocalDateTime.now();
    }

    public String getEventTypeName() {
        return switch (eventType) {
            case 0x01 -> "火警";
            case 0x02 -> "故障";
            case 0x03 -> "启动";
            case 0x04 -> "停动";
            case 0x05 -> "隔离";
            case 0x06 -> "释放";
            case 0x07 -> "监管";
            case 0x08 -> "反馈";
            case 0x09 -> "动作";
            case 0x0A -> "复位操作";
            case 0x0B -> "恢复";
            default -> "未知(0x" + Integer.toHexString(eventType) + ")";
        };
    }

    public boolean isValid() {
        return eventType != 0x00;
    }
}