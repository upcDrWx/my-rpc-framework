package github.wx.remoting.transport.netty.codec;

import github.wx.compress.Compress;
import github.wx.enums.CompressTypeEnum;
import github.wx.enums.SerializationTypeEnum;
import github.wx.extension.ExtensionLoader;
import github.wx.remoting.constants.RpcConstants;
import github.wx.remoting.dto.RpcMessage;
import github.wx.serialize.Serializer;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author wx
 * @date 2023/10/4 16:46
 */
@Slf4j
public class RpcMessageEncoder extends MessageToByteEncoder<RpcMessage> {

    private static final AtomicInteger ATOMIC_INTEGER = new AtomicInteger(0);

    @Override
    protected void encode(ChannelHandlerContext ctx, RpcMessage rpcMessage, ByteBuf byteBuf) {
        try {
            byteBuf.writeBytes(RpcConstants.MAGIC_NUMBER);
            byteBuf.writeByte(RpcConstants.VERSION);

            // 留一个地方写 full length
            byteBuf.writerIndex(byteBuf.writerIndex() + 4);
            byte messageType = rpcMessage.getMessageType();
            byteBuf.writeByte(messageType);
            byteBuf.writeByte(rpcMessage.getCodec());
            byteBuf.writeByte(CompressTypeEnum.GZIP.getCode());
            byteBuf.writeInt(ATOMIC_INTEGER.getAndIncrement());

            // 计算 full length
            byte[] bodyBytes = null;
            int fullLength = RpcConstants.HEAD_LENGTH;

            // 如果messageType不是心跳消息，fullLength =头部长度+身体长度
            if (messageType != RpcConstants.HEARTBEAT_REQUEST_TYPE
                    && messageType != RpcConstants.HEARTBEAT_RESPONSE_TYPE) {
                // 序列化对象
                String codecName = SerializationTypeEnum.getName(rpcMessage.getCodec());
                log.info("采用的编码名称: [{}] ", codecName);

                Serializer serializer = ExtensionLoader.getExtensionLoader(Serializer.class).getExtension(codecName);
                bodyBytes = serializer.serialize(rpcMessage.getData());

                // 压缩数组
                String compressName = CompressTypeEnum.getName(rpcMessage.getCompress());

                Compress compress = ExtensionLoader.getExtensionLoader(Compress.class).getExtension(compressName);
                bodyBytes = compress.compress(bodyBytes);
                fullLength += bodyBytes.length;
            }

            if (bodyBytes != null) {
                byteBuf.writeBytes(bodyBytes);
            }
            int writeIndex = byteBuf.writerIndex();
            byteBuf.writerIndex(writeIndex - fullLength + RpcConstants.MAGIC_NUMBER.length + 1);
            byteBuf.writeInt(fullLength);
            byteBuf.writerIndex(writeIndex);
        } catch (Exception e) {
            log.error("压缩请求错误!", e);
        }
    }
}
