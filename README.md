# HOJ在线判题模块后端部分


### 主流框架 & 特性

- Spring Cloud Alibaba
- Spring Boot 2.7.2
- Spring MVC
- MyBatis + MyBatis Plus 数据访问（开启分页）
- Spring Boot 调试工具和项目处理器
- Spring AOP 切面编程
- Spring 事务注解


### 数据存储

- MySQL 数据库
- Redis 内存数据库

### 工具类

- Hutool 工具库
- Apache Commons Lang3 工具类
- Lombok 注解

### 业务特性

- 业务代码生成器（支持自动生成 Service、Controller、数据模型代码）
- Spring Session Redis 分布式登录
- 全局请求响应拦截器（记录日志）
- 全局异常处理器
- 自定义错误码
- 封装通用响应类
- Swagger + Knife4j 接口文档
- 自定义权限注解 + 全局校验
- 全局跨域处理
- 长整数丢失精度解决
- 多环境配置


## 业务功能

- 题目创建、删除、编辑、更新、数据库检索
- 代码沙箱编译、运行用户代码，返回测试用例输出结果
- 用户登录、注册、更新、权限管理
- 

### 单元测试

- JUnit5 单元测试
- 示例单元测试类

### 架构设计
- 用户服务: 提供用户登录、用户信息的增删改查等功能
- 题目服务: 提供题目/题目提交记录的增删改查管理、题目提交功能
- 判题服务: 提供判题功能，调用代码沙箱并比对判题结果
- 代码沙箱: 提供编译执行代码、返回结果的功能
- 公共模块: 提供公共代码、如数据模型、全局请求响应封装、全局异常处理、工具类等
- 网关服务: 提供统一的API转发、聚合文档、全局跨域解决等功能

  ![架构图.jpg](doc/架构图.jpg)

## 快速上手


### MySQL 数据库

1）修改 `application.yml` 的数据库配置为你自己的：

```yml
spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/my_db
    username: root
    password: 123456
```

2）执行 `sql/create_table.sql` 中的数据库语句，自动创建库表

3）启动项目，访问 `http://localhost:8101/api/doc.html` 即可打开接口文档，不需要写前端就能在线调试接口了~

![](doc/swagger.png)

### Redis 分布式登录

1）修改 `application.yml` 的 Redis 配置：

```yml
spring:
  redis:
    database: 1
    host: localhost
    port: 6379
    timeout: 5000
    password: 123456
```

2）修改 `application.yml` 中的 session 存储方式：

```yml
spring:
  session:
    store-type: redis
```

3）移除 `MainApplication` 类开头 `@SpringBootApplication` 注解内的 exclude 参数：

修改前：

```java
@SpringBootApplication(exclude = {RedisAutoConfiguration.class})
```

修改后：

```java
@SpringBootApplication
```
### RabbitMQ消息队列
```yml
spring:
  datasource:
    rabbitmq:
      host: localhost
      port: 5672
      password: guest
      username: guest
```

### 业务代码生成器

支持自动生成 Service、Controller、数据模型代码，配合 MyBatisX 插件，可以快速开发增删改查等实用基础功能。

找到 `generate.CodeGenerator` 类，修改生成参数和生成路径，并且支持注释掉不需要的生成逻辑，然后运行即可。

```
// 指定生成参数
String packageName = "com.mirror.hoj";
String dataName = "用户评论";
String dataKey = "userComment";
String upperDataKey = "UserComment";
```

生成代码后，可以移动到实际项目中，并且按照 `// todo` 注释的提示来针对自己的业务需求进行修改。
