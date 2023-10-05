package github.wx.remoting.transport.netty.server;

import github.wx.factory.SingletonFactory;
import github.wx.remoting.constants.RpcConstants;
import github.wx.remoting.dto.RpcMessage;
import github.wx.enums.*;
import github.wx.remoting.dto.RpcRequest;
import github.wx.remoting.dto.RpcResponse;
import github.wx.remoting.handler.RpcRequestHandler;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * 自定义服务器的ChannelHandler来处理客户端发送的数据。
 * <p>
 * 如果继承自 SimpleChannelInboundHandler 的话就不要考虑 ByteBuf 的释放 ，{@link SimpleChannelInboundHandler} 内部的
 * channelRead 方法会替你释放 ByteBuf ，避免可能导致的内存泄露问题。详见《Netty进阶之路 跟着案例学 Netty》
 *
 * @author wx
 * @date 2023/10/4 16:14
 */
@Slf4j
public class NettyRpcServerHandler extends ChannelInboundHandlerAdapter {

    private final RpcRequestHandler rpcRequestHandler;

    public NettyRpcServerHandler() {
        this.rpcRequestHandler = SingletonFactory.getInstance(RpcRequestHandler.class);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        try {
            if (msg instanceof RpcMessage) {
                log.info("服务器收到消息: [{}] ", msg);
                byte messageType = ((RpcMessage) msg).getMessageType();
                RpcMessage rpcMessage = new RpcMessage();
                rpcMessage.setCodec(SerializationTypeEnum.HESSIAN.getCode());
                rpcMessage.setCompress(CompressTypeEnum.GZIP.getCode());
                if (messageType == RpcConstants.HEARTBEAT_REQUEST_TYPE) {
                    rpcMessage.setMessageType(RpcConstants.HEARTBEAT_RESPONSE_TYPE);
                    rpcMessage.setData(RpcConstants.PONG);
                } else {
                    RpcRequest rpcRequest = (RpcRequest) ((RpcMessage) msg).getData();
                    // 执行目标方法(客户端需要执行的方法)并返回方法结果
                    Object result = rpcRequestHandler.handle(rpcRequest);
                    log.info(String.format("服务器获得执行结果: %s", result.toString()));
                    rpcMessage.setMessageType(RpcConstants.RESPONSE_TYPE);
                    if (ctx.channel().isActive() && ctx.channel().isWritable()) {
                        RpcResponse<Object> rpcResponse = RpcResponse.success(result, rpcRequest.getRequestId());
                        rpcMessage.setData(rpcResponse);
                    } else {
                        RpcResponse<Object> rpcResponse = RpcResponse.fail(RpcResponseCodeEnum.FAIL);
                        rpcMessage.setData(rpcResponse);
                        log.error("现在不可写，消息丢失");
                    }
                }
                ctx.writeAndFlush(rpcMessage).addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
            }
        } finally {

            //确保ByteBuf被释放，否则可能会有内存泄漏
            ReferenceCountUtil.release(msg);
        }
    }

    /**
     * Netty 心跳机制相关。保证客户端和服务端的连接不被断掉，避免重连。
     *
     * 使用空闲状态检测，需要添加 IdleStateHandler 到 ChannelPipeline 中
     */

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleState state = ((IdleStateEvent) evt).state();
            if (state == IdleState.READER_IDLE) {
                log.info("检查发生 idel，因此关闭连接");
                ctx.close();
            } else {
                super.userEventTriggered(ctx, evt);
            }
        }
    }

    /**
     * 当处理I/O操作中的事件或数据时，如果发生任何异常，Netty会调用 exceptionCaught方法。
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("服务器捕获异常");
        cause.printStackTrace();
        ctx.close();
    }
}
