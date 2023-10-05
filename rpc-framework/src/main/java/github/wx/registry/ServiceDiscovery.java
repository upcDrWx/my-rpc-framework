package github.wx.registry;

import github.wx.extension.SPI;
import github.wx.remoting.dto.RpcRequest;

import java.net.InetSocketAddress;

/**
 * 服务发现
 *
 * @author wx
 * @date 2023/10/3 20:26
 */
@SPI
public interface ServiceDiscovery {

    /**
     * 通过rpcServiceName查找服务
     *
     * @param rpcRequest  rpc service pojo
     * @return  服务地址
     */
    InetSocketAddress lookupService(RpcRequest rpcRequest);
}
