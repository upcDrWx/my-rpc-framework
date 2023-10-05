package github.wx.remoting.transport.netty.test;


import github.wx.remoting.dto.RpcRequest;
import github.wx.remoting.dto.RpcResponse;
import github.wx.remoting.transport.netty.test.codec.NettyKryoDecoder;
import github.wx.remoting.transport.netty.test.codec.NettyKryoEncoder;
import github.wx.remoting.transport.netty.test.handler.NettyServerHandler;
import github.wx.remoting.transport.netty.test.serializer.KryoSerializer;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NettyServer {
    private static final Logger logger = LoggerFactory.getLogger(NettyServer.class);
    private final int port;

    private NettyServer(int port) {
        this.port = port;
    }

    private void run() {
        /**
         * NioEventLoopGroup 是用于处理I/O操作的多线程事件循环
         * bossGroup 用于接收连接，workerGroup 用于具体的处理
         */
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        KryoSerializer kryoSerializer = new KryoSerializer();

        try {
            /**
             * ServerBootstrap 是服务端的启动引导类/辅助类
             */
            ServerBootstrap b = new ServerBootstrap();

            // 给引导类配置两大线程组,确定了线程模型
            b.group(bossGroup, workerGroup)
                    // 指定 IO 模型
                    .channel(NioServerSocketChannel.class)
                    // TCP默认开启了 Nagle 算法，该算法的作用是尽可能的发送大数据快，减少网络传输。
                    // TCP_NODELAY 参数的作用就是控制是否启用 Nagle 算法。
                    .childOption(ChannelOption.TCP_NODELAY, true)

                    // 是否开启 TCP 底层心跳机制
                    .childOption(ChannelOption.SO_KEEPALIVE, true)

                    //表示系统用于临时存放已完成三次握手的请求的队列的最大长度,
                    // 如果连接建立频繁，服务器处理创建新连接较慢，可以适当调大这个参数
                    .option(ChannelOption.SO_BACKLOG, 128)

                    // 配置日志处理器, LogLevel.INFO 表示只记录 INFO 级别以上的日志信息
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch)  {
                            ch.pipeline().addLast(new NettyKryoDecoder(kryoSerializer, RpcRequest.class));
                            ch.pipeline().addLast(new NettyKryoEncoder(kryoSerializer, RpcResponse.class));
                            ch.pipeline().addLast(new NettyServerHandler());
                        }
                    });
            // 绑定端口，同步等待绑定成功
            ChannelFuture f = b.bind(port).sync();
            // 等待服务端监听端口关闭
            f.channel().closeFuture().sync();

        } catch (InterruptedException e) {
            logger.error("occur exception when start server:", e);
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    public static void main(String[] args) {
        new NettyServer(8889).run();
    }
}
