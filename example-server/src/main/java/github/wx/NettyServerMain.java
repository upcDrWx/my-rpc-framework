package github.wx;

import github.wx.annotation.RpcScan;
import github.wx.config.RpcServiceConfig;
import github.wx.remoting.transport.netty.server.NettyRpcServer;
import github.wx.serviceimpl.HelloServiceImpl2;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * @author wx
 * @date 2023/10/5 16:22
 */
@RpcScan(basePackage = {"github.wx"})
public class NettyServerMain {
    public static void main(String[] args) {
        // 通过注解注册服务
        AnnotationConfigApplicationContext applicationContext =
                new AnnotationConfigApplicationContext(NettyServerMain.class);

        NettyRpcServer nettyRpcServer = (NettyRpcServer) applicationContext.getBean("nettyRpcServer");

        // 手动注册服务
        HelloService helloService2 = new HelloServiceImpl2();
        RpcServiceConfig rpcServiceConfig = RpcServiceConfig.builder()
                .group("test2").version("version2").service(helloService2).build();
        nettyRpcServer.registerService(rpcServiceConfig);
        nettyRpcServer.start();
    }
}
