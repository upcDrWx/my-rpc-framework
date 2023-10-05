package github.wx.remoting.transport.netty.client;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * store and get Channel object
 *
 * @author wx
 * @date 2023/10/4 19:58
 */
@Slf4j
public class ChannelProvider {

    private final Map<String, Channel> channelMap;

    public ChannelProvider() {
        channelMap = new ConcurrentHashMap<>();
    }

    public Channel get(InetSocketAddress inetSocketAddress) {
        String key = inetSocketAddress.toString();
        // 确定是否有对应地址的连接
        if (channelMap.containsKey(key)) {
            Channel channel = channelMap.get(key);
            // 如果是，确定连接是否可用，如果可用，直接获取它
            if (channel != null && channel.isActive()) {
                return channel;
            } else {
                remove(inetSocketAddress);
            }
        }

        return null;
    }

    public void set(InetSocketAddress inetSocketAddress, Channel channel) {
        String key = inetSocketAddress.toString();
        channelMap.put(key, channel);
    }

    public void remove(InetSocketAddress inetSocketAddress) {
        String key = inetSocketAddress.toString();
        channelMap.remove(key);
        log.info("Channel map size :[{}]", channelMap.size());
    }
}
