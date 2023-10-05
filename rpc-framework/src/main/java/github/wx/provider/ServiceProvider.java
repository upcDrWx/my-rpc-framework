package github.wx.provider;

import github.wx.config.RpcServiceConfig;

/**
 * 存储和提供服务的对象
 *
 * @author wx
 * @date 2023/10/4 10:08
 */
public interface ServiceProvider {
    /**
     * @param rpcServiceConfig rpc service related attributes
     */
    void addService(RpcServiceConfig rpcServiceConfig);

    /**
     * @param rpcServiceName rpc service name
     * @return service object
     */
    Object getService(String rpcServiceName);

    /**
     * @param rpcServiceConfig rpc service related attributes
     */
    void publishService(RpcServiceConfig rpcServiceConfig);
}
