package com.example.demo.modbusRTU;

import com.fazecast.jSerialComm.SerialPort;
import com.serotonin.modbus4j.serial.SerialPortWrapper;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.io.OutputStream;

@Slf4j
public class JSerialCommSerialPortWrapper implements SerialPortWrapper {

    private final String portName;
    private final int baudRate;
    private final int dataBits;
    private final int stopBits;
    private final int parity;

    private SerialPort serialPort;

    public JSerialCommSerialPortWrapper(String portName, int baudRate,
                                        int dataBits, int stopBits, int parity) {
        this.portName = portName;
        this.baudRate = baudRate;
        this.dataBits = dataBits;
        this.stopBits = stopBits;
        this.parity = parity;
    }

    @Override
    public void open() throws Exception {
        serialPort = SerialPort.getCommPort(portName);
        serialPort.setBaudRate(baudRate);
        serialPort.setNumDataBits(dataBits);
        serialPort.setNumStopBits(stopBits);
        serialPort.setParity(parity);

        if (!serialPort.openPort()) {
            throw new RuntimeException("无法打开串口: " + portName);
        }
        log.info("串口已打开: {}", portName);
    }

    @Override
    public void close() throws Exception {
        if (serialPort != null && serialPort.isOpen()) {
            serialPort.closePort();
            log.info("串口 {} 已关闭", portName);
        }
    }

    @Override
    public InputStream getInputStream() {
        return serialPort.getInputStream();
    }

    @Override
    public OutputStream getOutputStream() {
        return serialPort.getOutputStream();
    }

    @Override
    public int getBaudRate() { return baudRate; }

    @Override
    public int getFlowControlIn() {
        return 0;
    }

    @Override
    public int getFlowControlOut() {
        return 0;
    }

    @Override
    public int getDataBits() { return dataBits; }
    @Override
    public int getStopBits() { return stopBits; }
    @Override
    public int getParity() { return parity; }
}

