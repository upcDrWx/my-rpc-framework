package github.wx.remoting.transport.netty.client;

import github.wx.enums.CompressTypeEnum;
import github.wx.enums.SerializationTypeEnum;
import github.wx.enums.ServiceDiscoveryEnum;
import github.wx.extension.ExtensionLoader;
import github.wx.factory.SingletonFactory;
import github.wx.registry.ServiceDiscovery;
import github.wx.remoting.constants.RpcConstants;
import github.wx.remoting.dto.RpcMessage;
import github.wx.remoting.dto.RpcRequest;
import github.wx.remoting.dto.RpcResponse;
import github.wx.remoting.transport.RpcRequestTransport;
import github.wx.remoting.transport.netty.codec.RpcMessageDecoder;
import github.wx.remoting.transport.netty.codec.RpcMessageEncoder;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * 初始化 Bootstrap 对象
 *
 * @author wx
 * @date 2023/10/4 19:37
 */

@Slf4j
public class NettyRpcClient implements RpcRequestTransport {

    private final ServiceDiscovery serviceDiscovery;
    private final UnprocessedRequests unprocessedRequests;
    private final ChannelProvider channelProvider;
    private final Bootstrap bootstrap;
    private final EventLoopGroup eventLoopGroup;


    public NettyRpcClient() {
        //初始化 EventLoopGroup, Bootstrap等资源
        eventLoopGroup = new NioEventLoopGroup();
        bootstrap = new Bootstrap();
        bootstrap.group(eventLoopGroup)
                .channel(NioSocketChannel.class)
                .handler(new LoggingHandler(LogLevel.INFO))
                //连接超时时间。如果超过此时间或连接无法建立，则连接失败。
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch)  {
                        ChannelPipeline p = ch.pipeline();
                        // 如果在15秒内没有向服务器发送数据，则发送心跳请求
                        p.addLast(new IdleStateHandler(0, 5, 0, TimeUnit.SECONDS));
                        p.addLast(new RpcMessageEncoder());
                        p.addLast(new RpcMessageDecoder());
                        p.addLast(new NettyRpcClientHandler());
                    }
                });

        this.serviceDiscovery = ExtensionLoader.getExtensionLoader(ServiceDiscovery.class)
                .getExtension(ServiceDiscoveryEnum.ZK.getName());
        this.unprocessedRequests = SingletonFactory.getInstance(UnprocessedRequests.class);
        this.channelProvider = SingletonFactory.getInstance(ChannelProvider.class);

    }

    /**
     * 连接服务器并获取 channel，以便可以向服务器发送 RPC 消息
     *
     * @param inetSocketAddress  server address
     * @return  the channel
     */
    @SneakyThrows
    public Channel doConnect(InetSocketAddress inetSocketAddress) {
        CompletableFuture<Channel> completableFuture = new CompletableFuture<>();
        bootstrap.connect(inetSocketAddress).addListener((ChannelFutureListener) future -> {
            if (future.isSuccess()) {
                log.info("客户端连接 [{}] 成功!", inetSocketAddress.toString());
                completableFuture.complete(future.channel());
            } else {
                throw new IllegalStateException();
            }
        });

        return completableFuture.get();
    }

    public Channel getChannel(InetSocketAddress inetSocketAddress) {
        Channel channel = channelProvider.get(inetSocketAddress);
        if (channel == null) {
            channel = doConnect(inetSocketAddress);
            channelProvider.set(inetSocketAddress, channel);
        }
        return channel;
    }

    @Override
    public Object sendRpcRequest(RpcRequest rpcRequest) {
        // build return value
        CompletableFuture<RpcResponse<Object>> resultFuture = new CompletableFuture<>();
        // get server address
        InetSocketAddress inetSocketAddress = serviceDiscovery.lookupService(rpcRequest);
        // get  server address related channel
        Channel channel = getChannel(inetSocketAddress);
        if (channel.isActive()) {
            // put unprocessed request
            unprocessedRequests.put(rpcRequest.getRequestId(), resultFuture);
            RpcMessage rpcMessage = RpcMessage.builder().data(rpcRequest)
                    .codec(SerializationTypeEnum.HESSIAN.getCode())
                    .compress(CompressTypeEnum.GZIP.getCode())
                    .messageType(RpcConstants.REQUEST_TYPE).build();

            channel.writeAndFlush(rpcMessage).addListener((ChannelFutureListener) future -> {
                if (future.isSuccess()) {
                    log.info("客户端发送消息: [{}]", rpcMessage);
                } else {
                    future.channel().close();
                    resultFuture.completeExceptionally(future.cause());
                    log.error("发送失败:", future.cause());
                }
            });
        } else {
            throw new IllegalStateException();
        }
        return resultFuture;
    }

    public void close() {
        eventLoopGroup.shutdownGracefully();
    }
}
