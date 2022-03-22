# Elasticsearch 基于 search-after 分页
Elastic 分页查询

## 启动步骤
![QQ截图20220322232025](https://www.bedebug.com/upload/2022/03/QQ截图20220322232025-e2a227e939d04a68bae3e83e6ab16251.jpg)


### 1. Kibana 导入数据文件
> 数据文件地址 data-dsl/earthquakes.txt

### 2. 启动SpringBoot服务
#### 2.1 修改配置文件
```yaml
server:
  port: 8080

spring:
  application:
    name: spring-boot-es-page
  servlet:
    multipart:
      max-file-size: 10MB
      max-request-size: 10MB
  #json 时间戳统一转换
  jackson:
    date-format: yyyy-MM-dd HH:mm:ss
    time-zone: GMT+8
  #开启aop
  aop:
    proxy-target-class: true

# Elasticsearch 配置
elastic:
  # 如果本地有账号密码认证 则设置为 true
  auth-enable: true
  # 嗅探器是否开启，一般情况下不会有什么问题
  # 但在类似于 阿里云 这种云原生环境下 Elastic是跑在 k8s上的 ，嗅探器拿到的地址是k8s内部虚拟ip 就会出现问题
  sniff-enable: true
  username: "elastic"
  password: "123456"
  hosts:
    - "localhost:9200"
```

#### 2.2 启动SpringBoot主程序
> Run SpringBootEsPageApplication.java

### 3. 启动Vue前端
#### 3.1 安装node基础环境
> 这个自己找一下包 安装一下，程序员应该是有点动手能力的
#### 3.2 安装 npm 本地包
> npm install
#### 3.3 修改配置文件
> 文件路径 src/util/request.js
```js
// 创建axios实例
const service = axios.create({
  // 修改为后端 Java服务地址  
  baseURL: 'http://localhost:8080', // api 的 base_url
  headers: {
    'Content-Type': 'application/json;charset=UTF-8'
  },
  timeout: 20000 // 请求超时时间
})
```
#### 3.4 启动Vue前端
> npm run dev
