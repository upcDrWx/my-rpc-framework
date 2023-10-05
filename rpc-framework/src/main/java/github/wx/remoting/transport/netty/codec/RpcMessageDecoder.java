package github.wx.remoting.transport.netty.codec;

import github.wx.compress.Compress;
import github.wx.enums.CompressTypeEnum;
import github.wx.enums.SerializationTypeEnum;
import github.wx.extension.ExtensionLoader;
import github.wx.remoting.constants.RpcConstants;
import github.wx.remoting.dto.RpcMessage;
import github.wx.remoting.dto.RpcRequest;
import github.wx.remoting.dto.RpcResponse;
import github.wx.serialize.Serializer;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;

/**
 * custom protocol decoder
 * <pre>
 *   0     1     2     3     4        5     6     7     8         9          10      11     12  13  14   15 16
 *   +-----+-----+-----+-----+--------+----+----+----+------+-----------+-------+----- --+-----+-----+-------+
 *   |   magic   code        |version | full length         | messageType| codec|compress|    RequestId       |
 *   +-----------------------+--------+---------------------+-----------+-----------+-----------+------------+
 *   |                                                                                                       |
 *   |                                         body                                                          |
 *   |                                                                                                       |
 *   |                                        ... ...                                                        |
 *   +-------------------------------------------------------------------------------------------------------+
 *  4B  magic code（魔法数）   1B version（版本）   4B full length（消息长度）    1B messageType（消息类型）
 *  1B compress（压缩类型） 1B codec（序列化类型）    4B  requestId（请求的Id）
 *  body（object类型数据）
 * </pre>
 * <p>
 * {@link LengthFieldBasedFrameDecoder} 是一个基于长度的解码器，用于解决TCP拆包和粘包问题。
 * </p>
 *
 * @author wx
 * @date 2023/10/4 17:04
 * @see <a href="https://zhuanlan.zhihu.com/p/95621344">LengthFieldBasedFrameDecoder解码器</a>
 */

@Slf4j
public class RpcMessageDecoder extends LengthFieldBasedFrameDecoder {

    public RpcMessageDecoder() {
        /**
         *  lengthFieldOffset: 魔法数是4B，版本是1B，然后是完整长度。所以value是5
         *  lengthFieldLength: length 为4B。所以value是4
         *  lengthAdjustment: 完整长度包含所有数据，之前读取9字节，所以左长度为(fullLength-9)。所以值是-9
         *  initialBytesToStrip: 我们将手动检查魔法数和版本，所以不要剥离任何字节。所以value是0
         */

        this(RpcConstants.MAX_FRAME_LENGTH,
                5, 4, -9, 0);
    }

    /**
     * @param maxFrameLength      最大帧长度。它决定了可以接收的数据的最大长度。如果超过，数据将被丢弃。
     * @param lengthFieldOffset   长度字段偏移量。length字段是跳过指定字节长度的字段。
     * @param lengthFieldLength   长度字段的字节数。
     * @param lengthAdjustment    要添加到长度字段值上的调整值
     * @param initialBytesToStrip 跳过的字节数。如果需要接收所有的报头+正文数据，则该值为0。
     *                            如果只想接收正文数据，那么需要跳过报头所消耗的字节数。
     */
    public RpcMessageDecoder(int maxFrameLength, int lengthFieldOffset, int lengthFieldLength,
                             int lengthAdjustment, int initialBytesToStrip) {
        super(maxFrameLength, lengthFieldOffset, lengthFieldLength, lengthAdjustment, initialBytesToStrip);
    }

    @Override
    protected Object decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
        Object decoded =  super.decode(ctx, in);
        if (decoded instanceof ByteBuf) {
            ByteBuf frame = (ByteBuf) decoded;
            if (frame.readableBytes() >= RpcConstants.TOTAL_LENGTH) {
                try {
                    return decodeFrame(frame);
                } catch (Exception e) {
                    log.error("解码出错!", e);
                    throw e;
                } finally {
                    frame.release();
                }
            }
        }
        return decoded;
    }

    private Object decodeFrame(ByteBuf in) {
        //注意:必须按顺序读取ByteBuf
        checkMagicNumber(in);
        checkVersion(in);
        int fullLength = in.readInt();
        // build RpcMessage object
        byte messageType = in.readByte();
        byte codecType = in.readByte();
        byte compressType = in.readByte();
        int requestId = in.readInt();
        RpcMessage rpcMessage = RpcMessage.builder()
                .codec(codecType)
                .requestId(requestId)
                .messageType(messageType).build();
        if (messageType == RpcConstants.HEARTBEAT_REQUEST_TYPE) {
            rpcMessage.setData(RpcConstants.PING);
            return rpcMessage;
        }
        if (messageType == RpcConstants.HEARTBEAT_RESPONSE_TYPE) {
            rpcMessage.setData(RpcConstants.PONG);
            return rpcMessage;
        }
        int bodyLength = fullLength - RpcConstants.HEAD_LENGTH;
        if (bodyLength > 0) {
            byte[] bs = new byte[bodyLength];
            in.readBytes(bs);
            //解压字节
            String compressName = CompressTypeEnum.getName(compressType);
            Compress compress = ExtensionLoader.getExtensionLoader(Compress.class).getExtension(compressName);
            bs = compress.decompress(bs);

            // 反序列化
            String codecName = SerializationTypeEnum.getName(rpcMessage.getCodec());
            log.info("解码名称: [{}] ", codecName);
//            Serializer serializer = ExtensionLoader.getExtensionLoader(Serializer.class).getExtension(codecName);
            Serializer serializer = null;
            if (messageType == RpcConstants.REQUEST_TYPE) {
                RpcRequest tmpValue = serializer.deserialize(bs, RpcRequest.class);
                rpcMessage.setData(tmpValue);
            } else {
                RpcResponse tmpValue = serializer.deserialize(bs, RpcResponse.class);
                rpcMessage.setData(tmpValue);
            }
        }
        return rpcMessage;
    }

    private void checkMagicNumber(ByteBuf in) {
        int len = RpcConstants.MAGIC_NUMBER.length;
        byte[] tmp = new byte[len];
        in.readBytes(tmp);
        for (int i = 0; i < len; i++) {
            if (tmp[i] != RpcConstants.MAGIC_NUMBER[i]) {
                throw new IllegalArgumentException("未知魔法数: " + Arrays.toString(tmp));
            }
        }

    }

    private void checkVersion(ByteBuf in) {
        byte version = in.readByte();
        if (version != RpcConstants.VERSION) {
            throw new RuntimeException("版本不兼容" + version);
        }
    }

}
