package com.example.demo.test.modbusTCP;

import lombok.Data;

@Data
public class GgaPosition {

    /**
     * UTC时间
     */
    private String utcTime;

    /**
     * 纬度
     */
    private Double latitude;

    /**
     * 经度
     */
    private Double longitude;

    /**
     * 海拔
     */
    private Double altitude;

    /**
     * 定位状态
     */
    private Integer fixQuality;

    /**
     * 卫星数
     */
    private Integer satellites;

    /**
     * HDOP
     */
    private Double hdop;
}
