package github.wx.provider.impl;

import github.wx.config.RpcServiceConfig;
import github.wx.enums.RpcErrorMessageEnum;
import github.wx.enums.ServiceRegistryEnum;
import github.wx.exception.RpcException;
import github.wx.extension.ExtensionLoader;
import github.wx.provider.ServiceProvider;
import github.wx.registry.ServiceRegistry;
import github.wx.registry.zk.ZkServiceRegistryImpl;
import github.wx.remoting.transport.netty.server.NettyRpcServer;
import lombok.extern.slf4j.Slf4j;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 服务提供者的实现，负责发布和管理zk服务
 *
 * @author wx
 * @date 2023/10/4 10:17
 */

@Slf4j
public class ZkServiceProviderImpl implements ServiceProvider {

    /**
     * 缓存
     * key: rpc service name(interface name + version + group)
     * value: service object
     */
    private final Map<String, Object> serviceMap;
    private final Set<String> registeredService;

    // 用于注册服务到zk
    private final ServiceRegistry serviceRegistry;

    public ZkServiceProviderImpl() {
        serviceMap = new ConcurrentHashMap<>();
        registeredService = ConcurrentHashMap.newKeySet();
        serviceRegistry = ExtensionLoader.getExtensionLoader(ServiceRegistry.class)
                .getExtension(ServiceRegistryEnum.ZK.getName());
    }

    @Override
    public void addService(RpcServiceConfig rpcServiceConfig) {
        String rpcServiceName = rpcServiceConfig.getRpcServiceName();
        if (registeredService.contains(rpcServiceName)) {
            return;
        }
        registeredService.add(rpcServiceName);
        serviceMap.put(rpcServiceName, rpcServiceConfig.getService());
        log.info("Add service: {} and interfaces:{}",
                rpcServiceName, rpcServiceConfig.getService().getClass().getInterfaces());
    }

    @Override
    public Object getService(String rpcServiceName) {
        Object service = serviceMap.get(rpcServiceName);
        if (null == service) {
            throw new RpcException(RpcErrorMessageEnum.SERVICE_CAN_NOT_BE_FOUND);
        }
        return service;
    }

    @Override
    public void publishService(RpcServiceConfig rpcServiceConfig) {
        try {
            String host = InetAddress.getLocalHost().getHostAddress();
            this.addService(rpcServiceConfig);
            serviceRegistry.registerService(rpcServiceConfig.getRpcServiceName(),
                    new InetSocketAddress(host, NettyRpcServer.PORT));

        } catch (UnknownHostException e) {
            log.error("occur exception when getHostAddress", e);
        }
    }
}
