# Elasticsearch 基于 search-after 分页
Elastic 搜索引擎级的深度分页问题解决方案, 本代码直接落地分页查询具体实现
> 源码地址: https://github.com/hiparker/es-page-searchafter.git

## 一、Elasticsearch 分页查询方式
### 1. 深度分页
> 使用 form + size 分页
> 缺点也是很明确的，会涉及到深度分页 详情请看 https://mp.weixin.qq.com/s/Icp64LUBcV0JmGc92q-TJg
使用如下操作：
```json
{
    "query": {
        "match_all": {}
    },
    "from": 9999,
    "size": 10
}
```
我们在获取第9999条到10009条数据的时候，其实每个分片都会拿到10009条数据，然后集合在一起，总共是10009*3=30027条数据(分片3个)，针对30027数据再次做排序会获取最后10条数据。
如此一来，搜索得太深，就会造成性能问题，会耗费内存和占用cpu。而且es为了性能，他不支持超过一万条数据以上的分页查询。那么如何解决深度分页带来的问题，我们应该避免深度分页操作（限制分页页数），比如最多只能提供100页的展示，从第101页开始就没了，毕竟用户也不会搜的那么深，我们平时搜索淘宝或者京东也就看个10来页就顶多了。

### 2. 滚动分页
一次性查询1万+数据，往往会造成性能影响，因为数据量太多了。这个时候可以使用滚动搜索，也就是 scroll 。
滚动搜索可以先查询出一些数据，然后再紧接着依次往下查询。在第一次查询的时候会有一个滚动id，相当于一个锚标记 ，随后再次滚动搜索会需要上一次搜索滚动id，根据这个进行下一次的搜索请求。每次搜索都是基于一个历史的数据快照，查询数据的期间，如果有数据变更，那么和搜索是没有关系的。

### 3. searchAfter分页（本方案）
> 这里先阐述一下 为什么选用 searchAfter 方式进行分页，在面对深度分页问题上，滚动分页方式都是有些不尽人意的地方，适用的场景也是非常的局限，无法面对大数据量高并发的查询场景，而searchAfter如果使用得当恰好可以解决这个问题

**本方案采用的是模拟百度查询分页的原理**
> 引用至 《大厂是面对深度分页问题是如何解决的》 原文
> 谷歌、百度目前作为全球和国内最大的搜索引擎（不加之一应该没人反对吧。O(∩_∩)O~）。不约而同的在分页条中删除了“跳页”功能，其目的就是为了避免用户使用深度分页检索。
>这里也许又双叒叕会有人不禁发问：难道删除“跳页”就能阻止用户查询很多页以后的数据了吗？我直接狂点下一页不也是深度分页？好我暂时先不反驳这里的提问，但是我也发出一个反问，至少删除跳页，可以阻挡那些刻意去尝试深度分页的“恶意用户”，真正想通过搜索引擎来完成自己检索需求的用户，通常来说都会首先查看第一页数据，因为搜索引擎是按照“相关度评分”进行排名的，也就是说，第一页的数据很往往是最符合用户预期结果的（暂时不考虑广告、置顶等商业排序情况）。
![Elastic-1](https://www.bedebug.com/upload/2022/03/Elastic-1-da9781b29e474e6d98e1d24297bfad49.jpg)

**原理其实也很简单**
每次请求后端服务会去拉去前端每页数量倍数的数据返回给前端，
前端自身对于这些数据做好分页，
有一个阈值(目前设置的是0.65)，也就是说当前分页位置到达0.65阈值时会去后端自动拉去下一个分页的数据来补充前端分页的数据, 且仅限于在缓存内分页之间进行跳页，可以往前跳，但不能往后跳至当前最大分页数后

## 二、启动步骤
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
