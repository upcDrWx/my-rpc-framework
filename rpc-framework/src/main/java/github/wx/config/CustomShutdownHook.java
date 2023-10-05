package github.wx.config;

import github.wx.registry.zk.util.CuratorUtils;
import github.wx.remoting.transport.netty.server.NettyRpcServer;
import github.wx.utils.threadpool.ThreadPoolFactoryUtil;
import lombok.extern.slf4j.Slf4j;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

/**
 * 当服务器关闭时，执行一些操作，例如注销所有服务
 *
 * @author wx
 * @date 2023/10/4 11:27
 */
@Slf4j
public class CustomShutdownHook {
    private static final CustomShutdownHook CUSTOM_SHUTDOWN_HOOK = new CustomShutdownHook();

    public static CustomShutdownHook getCustomShutdownHook() {
        return CUSTOM_SHUTDOWN_HOOK;
    }

    public void clearAll() {
        log.info("addShutdownHook for clearAll");
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                InetSocketAddress inetSocketAddress =
                        new InetSocketAddress(InetAddress.getLocalHost().getHostAddress(), NettyRpcServer.PORT);
                CuratorUtils.clearRegistry(CuratorUtils.getZkClient(), inetSocketAddress);
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
            ThreadPoolFactoryUtil.shutDownAllThreadPool();
        }));
    }
}
