package github.wx.remoting.transport.netty.test.codec;

import github.wx.remoting.transport.netty.test.NettyServer;
import github.wx.remoting.transport.netty.test.serializer.Serializer;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * 自定义解码器。负责处理"入站"消息，它会从 ByteBuf 中读取到业务对象对应的字节序列，然后再将字节序列转换为我们的业务对象
 */

@AllArgsConstructor
public class NettyKryoDecoder extends ByteToMessageDecoder {
    private static final Logger log = LoggerFactory.getLogger(NettyServer.class);

    private final Serializer serializer;
    private final Class<?> genericClass;

    /**
     * Netty传输的消息长度也就是对象序列化后对应的字节数组的大小，存储在 ByteBuf 头部
     */
    private static final int BODY_LENGTH = 4;

    /**
     * 解码 ByteBuf 对象
     *
     * @param ctx 解码器关联的 ChannelHandlerContext 对象
     * @param in  "入站"数据，也就是 ByteBuf 对象。参照编码器，ByteBuf = [dataLength + body]
     * @param out 解码之后的数据对象需要添加到 out 对象里面
     */
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {

        // 1.ByteBuf 的头部已经有4个字节了，所以 ByteBuf 的大小肯定是大于等于 BODY_LENGTH 的
        if (in.readableBytes() >= BODY_LENGTH) {
            // 2. 标记当前readIndex的位置，以便后面重置readIndex 的时候使用
            in.markReaderIndex();
            // 3.读取消息的长度
            int dataLength = in.readInt();
            //4.遇到不合理的情况直接 return
            if (dataLength < 0 || in.readableBytes() < 0) {
                log.error("data length or byteBuf readableBytes is not valid");
                return;
            }
            //5.如果可读字节数小于消息长度的话，说明是不完整的消息，重置readIndex
            if (in.readableBytes() < dataLength) {
                in.resetReaderIndex();
                return;
            }
            // 6.走到这里说明没什么问题了，可以序列化了
            byte[] body = new byte[dataLength];
            in.readBytes(body);
            Object obj = serializer.deserialize(body, genericClass);
            out.add(obj);
            log.info("successful decode ByteBuf to Object");
        }

    }
}
