package com.example.demo.modbusRTU;

import com.fazecast.jSerialComm.SerialPort;
import com.serotonin.modbus4j.*;
import com.serotonin.modbus4j.exception.ModbusInitException;
import com.serotonin.modbus4j.msg.*;
import com.serotonin.modbus4j.serial.SerialPortWrapper;

public class ModbusRTUExample {

    public static void main(String[] args) {
        SerialPortWrapper wrapper = new JSerialCommSerialPortWrapper(
                "COM3",          // Windows: COM3, Linux: /dev/ttyUSB0
                9600,
                8,
                1,
                SerialPort.NO_PARITY
        );


        // Modbus RTU Master
        ModbusFactory factory = new ModbusFactory();
        ModbusMaster master = factory.createRtuMaster(wrapper);

        try {
            master.init();
            System.out.println("Modbus RTU Master 初始化成功");

            int slaveId = 1;       // 从站地址
            int start = 0;          // 寄存器地址
            int len = 2;            // 读取长度（寄存器数量）

            // -------------------------------
            // 读取保持寄存器 (功能码 03)
            // -------------------------------
            ReadHoldingRegistersRequest req = new ReadHoldingRegistersRequest(slaveId, start, len);
            ReadHoldingRegistersResponse resp = (ReadHoldingRegistersResponse) master.send(req);

            if (!resp.isException()) {
                short[] data = resp.getShortData();
                System.out.println("寄存器数据：");
                for (short s : data) {
                    System.out.println("  " + (s & 0xFFFF));
                }
            }
//
//            // -------------------------------
//            // 写单个保持寄存器 (功能码 06)
//            // -------------------------------
//
//            WriteRegistersRequest request =
//                    new WriteRegistersRequest(slaveId, 0, new short[]{37, 3, 0, 0, 0, 2, 194, 239});
//
//            WriteRegistersResponse response = (WriteRegistersResponse) master.send(request);


//            if (response.isException()) {
//                System.out.println("写寄存器失败：" + response.getExceptionMessage());
//            } else {
//                System.out.println("写寄存器成功");
//            }

        } catch (ModbusInitException e) {
            System.err.println("Modbus 初始化失败：" + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            master.destroy();
        }
    }
}

