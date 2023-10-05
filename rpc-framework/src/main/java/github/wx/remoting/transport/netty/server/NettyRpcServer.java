package github.wx.remoting.transport.netty.server;

import github.wx.config.CustomShutdownHook;
import github.wx.config.RpcServiceConfig;
import github.wx.factory.SingletonFactory;
import github.wx.provider.ServiceProvider;
import github.wx.provider.impl.ZkServiceProviderImpl;
import github.wx.remoting.transport.netty.codec.RpcMessageDecoder;
import github.wx.remoting.transport.netty.codec.RpcMessageEncoder;
import github.wx.utils.RuntimeUtil;
import github.wx.utils.threadpool.ThreadPoolFactoryUtil;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.util.concurrent.TimeUnit;

/**
 * 接收客户端消息，根据客户端消息调用相应的方法，然后将结果返回给客户端。
 *
 * @author wx
 * @date 2023/10/4 10:03
 */
@Slf4j
@Component
public class NettyRpcServer {

    public static final int PORT = 9998;

    private final ServiceProvider serviceProvider = SingletonFactory.getInstance(ZkServiceProviderImpl.class);

    public void registerService(RpcServiceConfig rpcServiceConfig) {
        serviceProvider.publishService(rpcServiceConfig);
    }

    @SneakyThrows
    public void start() {
        CustomShutdownHook.getCustomShutdownHook().clearAll();
        String host = InetAddress.getLocalHost().getHostAddress();
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        DefaultEventLoopGroup serviceHandlerGroup = new DefaultEventLoopGroup(
                RuntimeUtil.cpus() * 2,
                ThreadPoolFactoryUtil.createThreadFactory("service-handler-group", false)
        );

        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)

                    /**
                     * option影响服务器 Channel（负责接受新连接），
                     * childOption影响由服务器为每个已接受的连接创建的Channel
                     */

                    // TCP默认开启了 Nagle 算法，该算法的作用是尽可能的发送大数据快，减少网络传输。
                    // TCP_NODELAY 参数的作用就是控制是否启用 Nagle 算法。
                    .childOption(ChannelOption.TCP_NODELAY, true)
                    // 是否开启 TCP 底层心跳机制
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    // 表示系统用于临时存放已完成三次握手但未被accept的连接的最大数量,
                    // 如果连接建立频繁，服务器处理创建新连接较慢，可以适当调大这个参数
                    .option(ChannelOption.SO_BACKLOG, 128)
                    // 配置日志处理器, LogLevel.INFO 表示只记录 INFO 级别以上的日志信息
                    .handler(new LoggingHandler(LogLevel.INFO))
                    // 当客户端第一次进行请求的时候才会进行初始化
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ChannelPipeline p = ch.pipeline();
                            // 30 秒之内没有收到客户端请求的话就关闭连接（执行 userEventTriggered 方法）
                            p.addLast(new IdleStateHandler(
                                    30, 0, 0, TimeUnit.SECONDS));
                            p.addLast(new RpcMessageEncoder());
                            p.addLast(new RpcMessageDecoder());
                            p.addLast(serviceHandlerGroup, new NettyRpcServerHandler());
                        }
                    });
            // 绑定端口，同步等待绑定成功
            ChannelFuture f = bootstrap.bind(host, NettyRpcServer.PORT).sync();
            // 等待服务端监听端口关闭
            f.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            log.error("occur exception when start server:", e);
        } finally {
            log.error("shutdown bossGroup and workerGroup");
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
            serviceHandlerGroup.shutdownGracefully();
        }
    }
}
