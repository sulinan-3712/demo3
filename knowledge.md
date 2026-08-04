# Project knowledge

This file gives Freebuff context about your project: goals, commands, conventions, and gotchas.

## What this is
`demo3` — a Spring Boot 3.5.7 (Java 17, Maven) sandbox/integration-demo app for industrial fire-alarm (GS8000) hardware. Chinese-language codebase that acts as a scratchpad for Modbus RTU polling, Kafka pub/sub, Netty TCP, and HTTP utilities. Entry point: `src/main/java/com/example/demo/Demo3Application.java` (no web UI; mostly a service host + a couple of HTTP endpoints).

## Key code locations
- `modbusRTU/` — **core feature**: polls a GS8000 fire alarm controller over Modbus RTU serial.
  - `Gs8000Runner.java` — `CommandLineRunner` that starts the polling service on boot.
  - `service/Gs8000ModbusService.java` — serial master (modbus4j + jSerialComm), polls holding regs 40001–40004, parses events (fire/fault/reset/etc.), auto-reconnects after `maxRetryCount` failures; has `syncTime()` (regs 0x0020–0x0022). Applies `modbus.timeout` to `master.setTimeout()` (modbus4j default is 500ms×2 retries, which the config previously ignored); init failure now auto-retries every 5s instead of dying silently; exposes `getStatus()`.
  - `config/ModbusConfig.java` — builder config, overridable via system properties (see Gotchas).
  - `model/Gs8000Event.java`, `model/DeviceTypeMapper.java` — event model + device-type-code→Chinese-name map.
  - `JSerialCommSerialPortWrapper.java`, `ModbusRTUExample.java` — standalone `main()` demo.
- `kafka/` — `KafkaDemo.java` (producer/consumer demos, SASL SCRAM commented out, hardcoded broker `192.168.1.53:9092`), `SendDemo.java` (JSON DTO via fastjson2).
- `test/modbusTCP/` — Netty TCP server (`NettyServer`, `MyDecoder`/`MyEncoder`, GGA/GPS parsing, DTU management). **Currently disabled**: `LaunchRunner` is fully commented out, so the socket server does NOT start on boot.
- `utils/HttpUtils.java` — Apache HttpClient wrapper (GET/POST/PUT/DELETE/multipart, SSL-bypass, optional local-address/virtual-IP binding with fallback).
- `controller/HealthController.java` — `GET /health/check` (echoes request info) and `GET /health/test` (proxies a request, optional virtual-IP bind).
- `config/WebMvcConfig.java` — maps `/iconBase/**` to hardcoded external dir `file:D:/code/back/erms_platform_v6_three/frontend/src/assets/iconBase/`.

## Commands
- Build: `./mvnw clean package` (or `mvnw.cmd` on Windows)
- Run: `./mvnw spring-boot:run`
- Test: `./mvnw test` (note: the only test class `Demo3ApplicationTests` is commented out)
- Run standalone demo: `./mvnw compile exec:java` (or run `main()` of `ModbusRTUExample`/`KafkaDemo` in IDE)

## Configuration
- Active config: `src/main/resources/application.yml` — `server.port=3712`, `socket.host=0.0.0.0`, `socket.port=9558`.
- `application.properties` is effectively dead (everything commented out) — prefer `application.yml`.
- Modbus tuning via VM args (from `ModbusConfig.fromSystemProperties()`): `-Dmodbus.port=COM5 -Dmodbus.baud=4800 -Dmodbus.slave=1 -Dmodbus.poll=500 -Dmodbus.retry=5 -Dmodbus.timeout=2000`.

## Conventions & gotchas
- Comments/logs are in Chinese — keep that style for new code.
- **Serial port required**: default port is `COM3` (Windows) or `/dev/ttyUSB0` (Linux), baud 4800. With no device attached, boot still succeeds but polling logs failures and reconnects in a loop.
- **Modbus failure diagnosis**: `ModbusInitException` at `master.init()` (start/reconnect/retry) = serial port NOT connected (port missing/occupied/no permission; log lists available ports). `TimeoutException` at `master.send()` (poll/syncTime) = port IS open and the request went out, but the slave didn't answer within the timeout — check slave address, baud rate, A/B wiring, device power. `getStatus()` prints `portConnected` to tell them apart.
- The Modbus TCP/Netty feature is inert (`LaunchRunner` commented out); if you re-enable it, it binds `socket.host:port` (0.0.0.0:9558).
- Hardcoded IPs/paths scattered around (`192.168.1.60`, `192.168.1.119:10088`, the `D:/code/back/...` icon dir) — adjust per environment.
- Only `spring-boot-starter` + `spring-boot-starter-web`; no DB/JPA — persistence is expected to be added in the `EventListener.onEvent` callback in `Gs8000Runner`.
- Dependencies of note: modbus4j 3.0.3, jSerialComm 2.10.4, netty-all 4.1.100, kafka-clients 3.9.1, fastjson2 2.0.51, httpclient 4.5.13, Lombok.
- Spring Boot 3.5.7 parent; Maven `spring-snapshots` repo configured.
- Some files use CRLF line endings (Windows-authored).
