package github.wx.registry.zk;

import github.wx.registry.ServiceRegistry;
import github.wx.registry.zk.util.CuratorUtils;
import org.apache.curator.framework.CuratorFramework;

import java.net.InetSocketAddress;

/**
 * 基于zookeeper的服务注册
 *
 * @author wx
 * @date 2023/10/3 21:48
 */
public class ZkServiceRegistryImpl implements ServiceRegistry {


    @Override
    public void registerService(String rpcServiceName, InetSocketAddress inetSocketAddress) {
        String servicePath = CuratorUtils.ZK_REGISTER_ROOT_PATH + "/" + rpcServiceName + inetSocketAddress.toString();
        CuratorFramework zkClient = CuratorUtils.getZkClient();
        CuratorUtils.createPersistentNode(zkClient, servicePath);
    }
}
