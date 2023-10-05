package github.wx.registry.zk;

import github.wx.enums.LoadBalanceEnum;
import github.wx.enums.RpcErrorMessageEnum;
import github.wx.exception.RpcException;
import github.wx.extension.ExtensionLoader;
import github.wx.loadbalance.LoadBalance;
import github.wx.registry.ServiceDiscovery;
import github.wx.registry.zk.util.CuratorUtils;
import github.wx.remoting.dto.RpcRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.springframework.util.CollectionUtils;

import java.net.InetSocketAddress;
import java.util.List;

/**
 * 基于zookeeper的服务发现
 *
 * @author wx
 * @date 2023/10/3 21:53
 */

@Slf4j
public class ZkServiceDiscoveryImpl implements ServiceDiscovery {
    private final LoadBalance loadBalance;

    public ZkServiceDiscoveryImpl() {
        this.loadBalance = ExtensionLoader.getExtensionLoader(LoadBalance.class)
                .getExtension(LoadBalanceEnum.LOADBALANCE.getName());
    }

    /**
     * 找到对应服务的所有子节点（服务提供者），由负载均衡策略选择一个节点
     *
     * @param rpcRequest  rpc service pojo
     * @return 服务提供者的地址
     */
    @Override
    public InetSocketAddress lookupService(RpcRequest rpcRequest) {
        String rpcServiceName = rpcRequest.getRpcServiceName();
        CuratorFramework zkClient = CuratorUtils.getZkClient();
        // 获取所有服务地址
        List<String> serviceUrlList = CuratorUtils.getChildrenNodes(zkClient, rpcServiceName);
        if (CollectionUtils.isEmpty(serviceUrlList)) {
            throw new RpcException(RpcErrorMessageEnum.SERVICE_CAN_NOT_BE_FOUND, rpcServiceName);
        }
        String targetServiceUrl = loadBalance.selectServiceAddress(serviceUrlList, rpcRequest);
        log.info("Successfully found the service address:[{}]", targetServiceUrl);
        String[] socketAddressArray = targetServiceUrl.split(":");
        String host = socketAddressArray[0];
        int port = Integer.parseInt(socketAddressArray[1]);
        return new InetSocketAddress(host, port);
    }
}
