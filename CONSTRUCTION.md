## Spring Batch

* Spring Batch 的 Transaction Manager 会和 JPA 的 Transaction Manager 重名，因此需要重新指定 JPA
  Transaction Manager
  的名字，参考 tp-batch-service 的主类实现。

## 开发环境配置

1. 复制根目录 `.env.template` 为 `.env`，根据需要修改对应的参数信息。
2. 在 IDEA 的启动配置中，勾选 `Enable EnvFile`，并在下方添加刚刚创建的 `.env` 文件。

**注意** `.env` 文件已经在 `.gitignore` 中忽略，请不要强制添加推送到版本库。

## KEYCLOAK

**注意** `ssl-required` 一定要设置为 `none`，包括系统中也要同步设置，否则对于 http 请求，会一直 401.

## KAFKA

正式环境部署时，若 Java 程序和 Minio 都在同一服务器时 KAFKA_HOST 配置为 localhost，否则，应配置为 IP
地址。

## MINIO

### MINIO 在系统中的作用

1. 客户端发起上传请求到 `biz-service` 换取 MINIO 签发的 `preSignedURL`，然后直接将文件上传到
   MINIO，不通过 `biz-service`
   中转。
2. 客户端指定上传路径，不同路径对应不同功能，`biz-service` 判断前端指定的路径的存在性，及业务层面的鉴权，若满足上述两个要求，则到
   MINIO 换取 `preSignedURL` 返回给客户端。
3. MINO 接收到上传的文件后，会发送 MINIO Bucket Notification 消息，消息的 Topic
   是 `minio_bucket_notification`
   。`biz-batch-service` 监听该消息，并根据 Key 中的路径判断是否对应需要启动对应的批处理程序，有则启动对应程序，没有则忽略消息。

### MINIO 的目录划分及用途

* `import_datasource/` 存储数据源导入的文档，`biz-batch-service` 分配资源对其进行异步的解析导入图谱。
* `images_and_videos/` 存储上传的图片和视频，只做存储和访问。

## 构建中遇到的问题汇总

1. Spring Boot 单元测试时，报错 Test Container test cases are failing due to "Could not find a valid
   Docker environment"
   可能是本地 Docker 没启动。
2. Spring Boot 单元测试时，报错 Failed to load ApplicationContext 可能是 application.yml
   缺失定义，导致程序启动时没读到配置，必要时给默认值，或者在
   @SpringBootTest 的 properties 列表中定义。
3. jsonpath 在线测试路径 http://jsonpath.herokuapp.com/

## 单元测试管理

* .run 文件夹下是单元测试配置，主要配置了测试所锚定的类或包，便于 IDEA 做代码覆盖率的统计。
* 继承测试时，不建议使用 WebClient，因为可能会出现请求结果不准确的问题，尽量使用 MockMvc。

## JPA 基础

Entity 与 Database
同步模式，参考文献：<https://stackoverflow.com/questions/1784981/jpa-saving-changes-without-persist-invoked>

## Liquibase 与 JPA

**任何情况下，不要直接操作数据库**

1. 通过修改 JPA 的 Model 来定义目标数据库。
2. 通过 liquibase maven 插件的 diff 指令来执行差异化计算，注意在根节点设置 quote 属性，具体参考其他文件。
3. 将差异化计算后的结果文件 include 到 changelog-master.xml 中。
4. 程序下次启动时，会自动更新数据库。
5. 需要注意的是，差异化计算的远程数据库是 144，因此，任何时候不要手动操作 144
   上的数据库，本地数据库若发现无法对齐，那么直接清掉，重启程序会自动重新创建。

## 代码规范

1. 代码提交时，必须保证代码是可以编译通过的。
2. 代码提交时，必须保证代码是可以通过单元测试的。
3. google style 规范，参考：<https://google.github.io/styleguide/javaguide.html>

## 注意事项

1. `datasource` 与 `datasourceContent` 未创建外键，虽然`datasourceContent`采用的`datasource`
   的id，但是请务必手动操作同步删除。