# demo3

基于 **Spring Boot 3.5.7 (Java 17, Maven)** 的工业消防报警控制器（GS8000）集成演示项目 / 沙盒工程。

项目主要承担三类职责：

1. **Modbus RTU 串口轮询** — 通过串口轮询 GS8000 火灾报警控制器的保持寄存器（40001~40004），解析火警/故障/复位等事件（核心功能，开机自动启动）。
2. **Kafka 发布/订阅示例** — 生产者/消费者 Demo（含 SASL SCRAM 认证的注释示例）。
3. **Netty TCP 服务器示例** — modbusTCP 长连接服务（GGA/GPS 解析、DTU 管理，**当前已禁用**，`LaunchRunner` 被整体注释）。

> 另有 Apache HttpClient 封装（虚拟 IP 绑定）、健康检查接口等辅助工具。

---

## 技术栈

| 组件 | 版本 | 用途 |
|---|---|---|
| Spring Boot | 3.5.7 | 应用框架 |
| modbus4j | 3.0.3 | Modbus 协议栈（RTU 主站） |
| jSerialComm | 2.10.4 | 串口通信 |
| kafka-clients | 3.9.1 | Kafka 生产/消费 |
| fastjson2 | 2.0.51 | JSON 序列化 |
| netty-all | 4.1.100.Final | Netty TCP（当前禁用） |
| httpclient / httpmime | 4.5.13 | HTTP 请求封装 |
| Lombok | - | 简化 POJO |

---

## 快速开始

### 环境要求

- JDK 17+
- Maven（项目自带 `mvnw` wrapper，无需单独安装）
- 串口设备：Windows 默认 `COM3`，Linux 默认 `/dev/ttyUSB0`，波特率 4800

### 依赖安装（重要）

`com.infiniteautomation:modbus4j:3.0.3` **不在 Maven 中央仓库**，需先从本地 jar 安装到本地仓库：

```bash
./mvnw install:install-file \
  -Dfile=/path/to/modbus4j-3.0.3.jar \
  -DgroupId=com.infiniteautomation \
  -DartifactId=modbus4j \
  -Dversion=3.0.3 \
  -Dpackaging=jar \
  -DgeneratePom=true
```

### 构建 & 运行

```bash
# 构建
./mvnw clean package        # Windows 下使用 ./mvnw.cmd

# 运行（Spring Boot）
./mvnw spring-boot:run

# 运行单元测试（注意：唯一的测试类 Demo3ApplicationTests 目前是注释掉的）
./mvnw test

# 运行独立 Demo（Modbus RTU 示例 / Kafka Demo）
./mvnw compile exec:java    # 或在 IDE 中直接运行 main()
```

启动后：

- HTTP 服务监听 `http://localhost:3712`
- GS8000 轮询服务随应用启动（`Gs8000Runner` 为 `CommandLineRunner`）

---

## 项目结构

```
src/main/java/com/example/demo/
├── Demo3Application.java              # Spring Boot 入口
├── config/
│   └── WebMvcConfig.java              # /iconBase/** → 外部静态资源目录映射
├── controller/
│   └── HealthController.java          # GET /health/check、/health/test
├── kafka/
│   ├── KafkaDemo.java                 # 生产者/消费者/多主题生产者示例
│   └── SendDemo.java                  # 消息 DTO
├── modbusRTU/                         # ★ 核心：GS8000 Modbus RTU 轮询
│   ├── Gs8000Runner.java              # 开机启动轮询服务（业务监听器在此注册）
│   ├── ModbusRTUExample.java          # 独立 main() 演示（读写保持寄存器）
│   ├── JSerialCommSerialPortWrapper.java
│   ├── config/ModbusConfig.java       # 配置（支持 VM 参数覆盖）
│   ├── model/
│   │   ├── Gs8000Event.java           # 事件模型（火警/故障/监管/反馈/复位/恢复…）
│   │   └── DeviceTypeMapper.java      # 设备类型码 → 中文名（GS8000 附录二）
│   └── service/Gs8000ModbusService.java  # 串口主站、轮询、事件解析、对时、重连
├── test/modbusTCP/                    # Netty TCP 服务器（当前禁用）
│   ├── NettyServer.java / NettyServerHandler.java / NettyServerChannelInitializer.java
│   ├── MyDecoder.java / MyEncoder.java
│   ├── GgaParser.java / GgaPosition.java   # GPS GGA 报文解析
│   ├── DtuManage.java / ChannelMap.java    # DTU 连接管理
│   ├── SocketProperties.java          # socket.host / socket.port 配置
│   └── LaunchRunner.java              # 启动入口（已整体注释）
└── utils/
    └── HttpUtils.java                 # HttpClient 封装（GET/POST/PUT/DELETE/multipart、SSL 绕过、虚拟 IP 绑定）
```

---

## 核心功能：Modbus RTU 轮询（GS8000）

### 寄存器协议

| 寄存器 | 内容 |
|---|---|
| 40001 (偏移 0x0000) | 高字节=事件类型（0x01~0x0B），低字节保留 |
| 40002 (0x0001) | 高字节=设备类型码，低字节=控制器地址百位（BCD） |
| 40003 (0x0002) | 控制器地址十位/个位（BCD）+ 回路号（BCD） |
| 40004 (0x0003) | 设备编码（BCD，如 000~255） |

事件类型（`Gs8000Event.getEventTypeName()`）：

| 类型码 | 含义 |
|---|---|
| 0x01 | 火警 |
| 0x02 | 故障 |
| 0x07 | 监管 |
| 0x08 | 反馈 |
| 0x09 | 动作 |
| 0x0A | 复位操作 |
| 0x0B | 恢复 |

设备类型码 → 中文名映射见 `DeviceTypeMapper`（感烟、感温、手动按钮、声光警报、防火卷帘等 200+ 种）。

### 行为特性

- **轮询间隔**：默认 500ms（协议文档要求 >300ms）。
- **事件回调**：解析到有效事件（事件类型 ≠ 0x00）后调用 `EventListener.onEvent()`——数据库/消息推送/告警等业务逻辑在 `Gs8000Runner` 的监听器里接入。
- **自动重连**：连续失败达到 `maxRetryCount` 后触发重连；**初始化（串口打开）失败时每 5 秒自动重试**，不再静默退出。
- **对时功能**：`service.syncTime(LocalDateTime)` 向寄存器 0x0020~0x0022 写入年月日/时分/秒分。

### 配置（VM 参数覆盖）

| 参数 | 默认值 | 说明 |
|---|---|---|
| `-Dmodbus.port` | `COM3` (Win) / `/dev/ttyUSB0` (Linux) | 串口号 |
| `-Dmodbus.baud` | `4800` | 波特率 |
| `-Dmodbus.slave` | `1` | 从站地址 |
| `-Dmodbus.poll` | `500` | 轮询间隔（ms，>300） |
| `-Dmodbus.retry` | `5` | 连续失败多少次触发重连 |
| `-Dmodbus.timeout` | `2000` | 单次请求超时（ms） |

示例：

```bash
./mvnw spring-boot:run \
  -Dmodbus.port=COM5 -Dmodbus.baud=4800 -Dmodbus.slave=1 \
  -Dmodbus.poll=500 -Dmodbus.retry=5 -Dmodbus.timeout=2000
```

### 故障诊断（串口没连上 vs 从站无响应）

日志中出现读取失败时，可按异常类型区分两种完全不同的故障：

| 现象 | 异常 | 结论 | 排查方向 |
|---|---|---|---|
| 启动/重连时 `Modbus 初始化失败` | `ModbusInitException`（`master.init()` 抛出） | **串口没有连接上** | 端口不存在、被占用、无权限、驱动缺失。日志会打印系统当前可用串口列表，用 `-Dmodbus.port` 指定正确串口 |
| 轮询/对时时 `读取寄存器超时` | `ModbusTransportException`，cause 链含 `TimeoutException` | **串口已连接、请求已发出，但从站没在超时时间内响应** | 从站地址（`-Dmodbus.slave`）、波特率（`-Dmodbus.baud`）、A/B 接线、设备电源 |

> 提示：`master.send()` 声明只抛 `ModbusTransportException`，超时异常被包在 cause 链中；`Gs8000ModbusService` 通过 cause 链识别 `TimeoutException` 并输出对应诊断日志。可用 `service.getStatus()` 查看 `portConnected` 等连接状态。

---

## 其他模块

### Kafka（`kafka/`）

- `KafkaDemo.myProducer()`：单主题生产者，每秒发送一条 JSON 消息（key 为设备编号，保证同设备有序）。
- `KafkaDemo.myProducerMultipleTopics()`：多主题生产者示例。
- `KafkaDemo.myConsumer()`：消费者示例（手动提交 offset，`earliest` 重置策略）。
- SASL SCRAM 认证代码已注释，需要时取消注释并填入用户名/密码。
- Broker 地址硬编码为 `192.168.1.53:9092`，按环境修改。

### Netty TCP（`test/modbusTCP/`，已禁用）

- 长连接 TCP 服务器（默认绑定 `0.0.0.0:9558`），自定义编解码器、GPS GGA 报文解析、DTU 连接管理。
- **当前不随应用启动**（`LaunchRunner` 整体注释）。如需启用：取消 `LaunchRunner` 注释并确认 `SocketProperties` 配置。

### HTTP 工具 & 健康检查

- `GET /health/check` — 回显请求信息（IP、方法、URI、Header 等）。
- `GET /health/test` — 代理请求 `http://192.168.1.119:10088/location/health/check`，可选绑定虚拟 IP（`HealthController.virtualIp`，默认 `192.168.1.60`）。
- `HttpUtils` — Apache HttpClient 封装：GET/POST/PUT/DELETE/multipart、SSL 证书绕过、本地地址/虚拟 IP 绑定（带 fallback）。

---

## 配置文件

- 生效配置：`src/main/resources/application.yml`（`server.port=3712`、`socket.host/port`、modbus4j 日志级别）。
- `application.properties` 基本废弃（内容已注释），**优先修改 `application.yml`**。

---

## 注意事项

- **串口必需**：无设备接入时应用仍能启动，但轮询会持续报错并自动重连（日志中可看到明确的诊断提示）。
- **硬编码地址**：项目内散落着测试环境 IP/路径（如 `192.168.1.53:9092`、`192.168.1.119:10088`、`192.168.1.60`、`D:/code/back/...` 图标目录），部署时按环境调整。
- **无数据库**：当前只有 `spring-boot-starter` / `spring-boot-starter-web`，持久化需在 `Gs8000Runner` 的 `EventListener.onEvent` 回调中自行接入。
- **文件编码**：部分源文件为 CRLF 行尾（Windows 环境编写），提交时注意 diff。
