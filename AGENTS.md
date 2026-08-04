# AGENTS.md

Spring Boot 3.5.7 / Java 17 / Maven sandbox that polls a GS8000 fire-alarm controller over Modbus RTU serial. No DB/JPA, no real tests. All comments and logs are in Chinese — keep that style for new code.

## Commands (Windows: use `mvnw.cmd`)
- Build: `mvnw.cmd clean package`
- Run: `mvnw.cmd spring-boot:run`
- Test: `mvnw.cmd test` — no-op; the only test class `Demo3ApplicationTests` is fully commented out.
- Standalone demos (`modbusRTU/ModbusRTUExample`, `kafka/KafkaDemo`) have `main()`: run them from the IDE only. There is NO exec-maven-plugin in pom.xml, so `exec:java` fails.

## Boot behavior & gotchas
- `Demo3Application.java` starts `Gs8000Runner` (a `CommandLineRunner`) which opens the serial port immediately and polls holding regs 40001–40004. With no device attached, boot still succeeds but logs poll failures and reconnects in a loop.
- Modbus tuning is via VM args read in `ModbusConfig.fromSystemProperties()`: `-Dmodbus.port=COM5 -Dmodbus.baud=4800 -Dmodbus.slave=1 -Dmodbus.poll=500 -Dmodbus.retry=5 -Dmodbus.timeout=2000`. Defaults: `COM3` (Windows) / `/dev/ttyUSB0` (Linux), baud 4800, poll 500ms, retry 5, timeout 2000ms.
- Business logic (DB/MQ/alert) goes in the `EventListener.onEvent` callback inside `Gs8000Runner.run()` — currently it only logs. Event parsing is in `Gs8000ModbusService.parseEvent()`; `syncTime()` writes regs 0x0020–0x0022.
- The Netty TCP code under `test/modbusTCP/` is DEAD: `LaunchRunner` is fully commented out, so `socket.host`/`socket.port` (0.0.0.0:9558) in application.yml are unused. Don't expect the socket server to start.
- Active config is `src/main/resources/application.yml` (`server.port=3712`). `application.properties` is fully commented out — ignore it.
- Hardcoded environment-specific values scattered around: `/iconBase/**` → `file:D:/code/back/erms_platform_v6_three/frontend/src/assets/iconBase/` in `WebMvcConfig`; `HealthController` proxies `/health/test` to `192.168.1.119:10088` bound to virtual IP `192.168.1.60`; `KafkaDemo` broker is `192.168.1.53:9092`. Adjust per environment.
- JSON uses `com.alibaba.fastjson2.JSON` (fastjson2 arrives transitively via the `com.alibaba:fastjson:2.0.51` compat artifact in pom.xml).
- Some files use CRLF line endings (Windows-authored); `.gitattributes` pins `mvnw` to LF and `*.cmd` to CRLF.
