# Payment Processing System

这是一个适合 Java / Spring Boot 初学者的小组项目版本。

技术栈：

- Java 17
- Spring Boot 3
- Spring Web
- Spring Data JPA
- Spring Validation
- MySQL
- HTML + CSS + JavaScript
- Swagger UI
- JUnit 5 + Mockito + JaCoCo

## 1. 功能

已实现：

- 创建支付
- 查询所有支付
- 按状态筛选支付
- 查询支付详情
- 查询状态历史
- 支付状态流转
  - CREATED → VALIDATED
  - VALIDATED → SENT
  - SENT → COMPLETED
  - CREATED / VALIDATED / SENT → FAILED
- 非法状态转换拦截
- 幂等键重复检查
- 支付失败错误码和错误信息
- 状态历史审计记录
- 前端页面
  - 创建支付
  - 支付列表
  - 状态筛选
  - 支付详情
  - 状态历史时间线
  - 状态操作按钮
- MySQL 测试数据
- 后端单元测试

## 2. 项目结构

```text
payment-processing-system
├── pom.xml
├── sql
│   ├── init.sql
│   └── sample-api-requests.http
├── src/main/java/com/example/paymentprocessing
│   ├── controller
│   ├── dto
│   ├── entity
│   ├── enums
│   ├── exception
│   ├── mapper
│   ├── repository
│   └── service
└── src/main/resources/static
    ├── index.html
    ├── styles.css
    └── app.js
```

## 3. MySQL 配置

本项目默认使用本地 MySQL：

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/payment_processing_db?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
spring.datasource.username=root
spring.datasource.password=123456
```

## 4. 初始化数据库和测试数据

方式一：使用 MySQL Workbench。

打开：

```text
sql/init.sql
```

然后全部执行。

方式二：命令行执行。

```bash
mysql -u root -p < sql/init.sql
```

输入密码：

```text
123456
```

脚本会创建数据库：

```text
payment_processing_db
```

并插入 4 条测试支付数据。

## 5. 运行后端和前端

在项目根目录执行：

```bash
mvn spring-boot:run
```

然后打开浏览器：

```text
http://localhost:8080
```

前端页面由 Spring Boot 直接提供，不需要单独启动前端服务器。

## 6. Swagger UI

打开：

```text
http://localhost:8080/swagger-ui.html
```

可以直接测试 API。

## 7. API 列表

```http
POST   /api/payments
GET    /api/payments
GET    /api/payments?status=CREATED
GET    /api/payments/{id}
GET    /api/payments/{id}/details
GET    /api/payments/{id}/history
POST   /api/payments/{id}/validate
POST   /api/payments/{id}/send
POST   /api/payments/{id}/complete
POST   /api/payments/{id}/fail
```

## 8. 创建支付示例

```json
{
  "idempotencyKey": "PAY-LOCAL-001",
  "sourceAccount": "ACC-001",
  "destinationAccount": "ACC-002",
  "amount": 120.50,
  "currency": "GBP",
  "reference": "Local API test"
}
```

## 9. 失败支付示例

```json
{
  "errorCode": "NETWORK_ERROR",
  "errorMessage": "Simulated network error"
}
```

## 10. 演示流程建议

成功流程：

```text
Create Payment
→ Validate
→ Send
→ Complete
→ 查看 Status History
```

失败流程：

```text
Create Payment
→ Validate
→ Fail
→ 查看 Error Code / Error Message / Status History
```

重复提交测试：

```text
使用相同 idempotencyKey 创建第二笔支付
→ 返回 DUPLICATE_PAYMENT
```

非法状态转换测试：

```text
对 CREATED 状态的支付直接 Send
→ 返回 INVALID_STATUS_TRANSITION
```

## 11. 运行测试

```bash
mvn test
```

JaCoCo 报告生成位置：

```text
target/site/jacoco/index.html
```

## 12. 业务规则说明

账户规则：

- 账户必须以 `ACC-` 开头
- `ACC-000` 被模拟为不存在账户
- `ACC-999` 被模拟为余额不足账户
- sourceAccount 和 destinationAccount 不能相同

金额规则：

- 金额必须大于 0
- 金额不能超过 1,000,000.00
- 最多 2 位小数

模拟网络失败：

- 如果 reference 包含 `FAIL-SEND`
- 调用 Send 时会自动进入 FAILED
- 错误码为 `NETWORK_ERROR`

## 13. 给小组成员的分工建议

成员 A：

- Payment Entity
- Repository
- Create / Get API
- 幂等性

成员 B：

- 状态机
- Validate / Send / Complete / Fail
- History

成员 C：

- index.html
- styles.css
- 支付列表
- 创建表单

成员 D：

- app.js 详情页
- 错误展示
- 测试
- Swagger / README / 演示脚本
