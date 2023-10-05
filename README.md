# my-rpc-framework

## 介绍

 my-rpc-framework 是一款基于 Netty+Hessian+Zookeeper 实现的 RPC 框架。代码注释详细，结构清晰，非常适合阅读和学习。


### 项目主要完成的模块

- **服务注册**：使用 Zookeeper 管理相关服务地址信息
- **网络传输**：使用 Netty 作为 RPC 框架的网络通信工具
- **序列化机制**：使用开源的序列化机制 Hessian、Kyro、protostuff 
- **负载均衡**：实现了随机负载均衡和一致性哈希算法
- **数据压缩**：使用 gzip 压缩
- **注解**：继承 spring，通过注解注册和消费服务
- **加载机制**：使用 SPI 实现扩展点加载机制


## 运行项目

### 导入项目

克隆项目到自己的本地：`git clone https://github.com/upcDrWx/my-rpc-framework.git`

### 下载运行 zookeeper

这里使用 Docker 来下载安装。

下载：

```shell
docker pull zookeeper:3.5.8
```

运行：

```shell
docker run -d --name zookeeper -p 2181:2181 zookeeper:3.5.8
```

修改项目中 rpc.properties 文件中的 zookeeper 地址：

```
rpc.zookeeper.address=192.168.241.130:2181
```

## 使用

### 服务提供端

运行：

```
example-server 模块中的 NettyServerMain 类
```

### 服务消费端

运行：

```
example-client 模块中的 NettyClientMain 类
```



## 参考

项目参考javaGuide哥的 [guide-rpc-framework](https://github.com/Snailclimb/guide-rpc-framework#guide-rpc-framework)，仅供个人学习使用。
