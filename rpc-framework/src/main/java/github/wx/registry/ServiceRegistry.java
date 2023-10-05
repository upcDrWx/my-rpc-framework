package github.wx.registry;

import github.wx.extension.SPI;

import java.net.InetSocketAddress;

/**
 * 服务注册
 *
 * @author wx
 * @date 2023/10/3 20:29
 */
@SPI
public interface ServiceRegistry {
    /**
     * 注册服务
     *
     * @param rpcServiceName    rpc service name
     * @param inetSocketAddress 服务地址
     */
    void registerService(String rpcServiceName, InetSocketAddress inetSocketAddress);
}
