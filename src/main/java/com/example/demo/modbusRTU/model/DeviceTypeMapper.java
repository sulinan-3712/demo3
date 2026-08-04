package com.example.demo.modbusRTU.model;

import java.util.HashMap;
import java.util.Map;

/**
 * 总线设备类型映射（GS8000 附录二）
 * 根据设备类型码返回中文名称
 */
public class DeviceTypeMapper {

    private static final Map<Integer, String> TYPE_MAP = new HashMap<>();

    static {
        TYPE_MAP.put(0, "预留未定义");
        TYPE_MAP.put(3, "图形显示装置");
        TYPE_MAP.put(10, "火灾示盘");
        TYPE_MAP.put(11, "探测回路");
        TYPE_MAP.put(13, "主电源");
        TYPE_MAP.put(14, "备用电源");
        TYPE_MAP.put(15, "充电电路");
        TYPE_MAP.put(16, "声光警报");
        TYPE_MAP.put(23, "点型感烟");
        TYPE_MAP.put(24, "光束感烟");
        TYPE_MAP.put(25, "吸气感烟");
        TYPE_MAP.put(31, "点型感温");
        TYPE_MAP.put(32, "线型感温");
        TYPE_MAP.put(34, "空气管探测");
        TYPE_MAP.put(35, "光纤测温");
        TYPE_MAP.put(41, "点型红外");
        TYPE_MAP.put(42, "点型紫外");
        TYPE_MAP.put(52, "图像探测");
        TYPE_MAP.put(61, "手动按钮");
        TYPE_MAP.put(62, "消火栓");
        TYPE_MAP.put(63, "急启按钮");
        TYPE_MAP.put(64, "急停按钮");
        TYPE_MAP.put(72, "输入模块");
        TYPE_MAP.put(73, "输出模块");
        TYPE_MAP.put(74, "输入/输出模块");
        TYPE_MAP.put(75, "中继模块");
        TYPE_MAP.put(76, "总线隔离器");
        TYPE_MAP.put(82, "讯响器");
        TYPE_MAP.put(83, "声警报器");
        TYPE_MAP.put(84, "光警报器");
        TYPE_MAP.put(85, "气体释放警报器");
        TYPE_MAP.put(86, "警铃");
        TYPE_MAP.put(100, "可燃气体报警控制器");
        TYPE_MAP.put(102, "可燃气体");
        TYPE_MAP.put(110, "电气监控");
        TYPE_MAP.put(111, "电气火灾");
        TYPE_MAP.put(113, "漏电测温");
        TYPE_MAP.put(114, "故障电弧");
        TYPE_MAP.put(120, "应急照明控制器");
        TYPE_MAP.put(121, "应急标志");
        TYPE_MAP.put(122, "应急照明");
        TYPE_MAP.put(123, "应急电源");
        TYPE_MAP.put(124, "应急配电");
        TYPE_MAP.put(130, "防火门监控器");
        TYPE_MAP.put(140, "消防电源监控器");
        TYPE_MAP.put(150, "气体灭火控制器");
        TYPE_MAP.put(151, "泡沫灭火");
        TYPE_MAP.put(152, "干粉灭火");
        TYPE_MAP.put(153, "防排烟阀");
        TYPE_MAP.put(154, "防火卷帘");
        TYPE_MAP.put(155, "挡烟垂壁");
        TYPE_MAP.put(156, "消防广播");
        TYPE_MAP.put(157, "消防电话");
        TYPE_MAP.put(158, "消防电源");
        TYPE_MAP.put(159, "传输设备");
        TYPE_MAP.put(161, "水位监视");
        TYPE_MAP.put(162, "消防电梯");
        TYPE_MAP.put(180, "消防栓泵");
        TYPE_MAP.put(181, "水喷雾泵");
        TYPE_MAP.put(182, "细水雾泵");
        TYPE_MAP.put(183, "稳压泵");
        TYPE_MAP.put(184, "喷淋泵");
        TYPE_MAP.put(185, "雨淋泵");
        TYPE_MAP.put(186, "泡沫液泵");
        TYPE_MAP.put(187, "水流指示器");
        TYPE_MAP.put(188, "报警阀");
        TYPE_MAP.put(189, "压力开关");
        TYPE_MAP.put(190, "阀驱动装置");
        TYPE_MAP.put(191, "防火阀");
        TYPE_MAP.put(192, "70度防火阀");
        TYPE_MAP.put(193, "280度防火阀");
        TYPE_MAP.put(194, "通风空调");
        TYPE_MAP.put(195, "管网电磁阀");
        TYPE_MAP.put(196, "防烟排烟风机");
        TYPE_MAP.put(197, "排烟防火阀");
        TYPE_MAP.put(198, "常闭送风口");
        TYPE_MAP.put(199, "排烟口");
        TYPE_MAP.put(200, "消防水炮");
        TYPE_MAP.put(201, "电动门");
        TYPE_MAP.put(202, "排烟机");
        TYPE_MAP.put(203, "送风机");
        TYPE_MAP.put(204, "电磁阀");
        TYPE_MAP.put(205, "照明配电");
        TYPE_MAP.put(206, "动力配电");
        TYPE_MAP.put(207, "空压机");
        TYPE_MAP.put(208, "阀门");
        TYPE_MAP.put(209, "配电箱");
        TYPE_MAP.put(235, "广播模块");
        // 230-234, 236-255 用户自定义
        for (int i = 230; i <= 234; i++) TYPE_MAP.put(i, "用户自定义");
        for (int i = 236; i <= 255; i++) TYPE_MAP.put(i, "用户自定义");
        // 其余未定义范围补全
        for (int i = 1; i <= 255; i++) {
            TYPE_MAP.putIfAbsent(i, "预留(" + i + ")");
        }
    }

    public static String getName(int typeCode) {
        return TYPE_MAP.getOrDefault(typeCode, "未知类型(" + typeCode + ")");
    }
}