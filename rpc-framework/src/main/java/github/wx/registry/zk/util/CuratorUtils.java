package github.wx.registry.zk.util;

import github.wx.enums.RpcConfigEnum;
import github.wx.utils.PropertiesFileUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.imps.CuratorFrameworkState;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * @author wx
 * @date 2023/10/3 20:31
 */

@Slf4j
public class CuratorUtils {

    private static final int BASE_SLEEP_TIME = 1000;
    private static final int MAX_RETRIES = 3;
    public static final String ZK_REGISTER_ROOT_PATH = "/my-rpc";

    private static final Set<String> REGISTERED_PATH_SET = ConcurrentHashMap.newKeySet();
    private static final Map<String, List<String>> SERVICE_ADDRESS_MAP = new ConcurrentHashMap<>();

    private static CuratorFramework zkClient;
    private static final String DEFAULT_ZOOKEEPER_ADDRESS = "192.168.241.130:2181";

    private CuratorUtils() {
    }

    /**
     * 创建 ZkClient
     *
     */
    public static CuratorFramework getZkClient() {
        // 检查用户是否设置了zk地址，否则使用默认地址
        Properties properties = PropertiesFileUtil.readPropertiesFile(RpcConfigEnum.RPC_CONFIG_PATH.getPropertyValue());
        String zookeeperAddress = properties != null &&
                properties.getProperty(RpcConfigEnum.ZK_ADDRESS.getPropertyValue()) != null ?
                properties.getProperty(RpcConfigEnum.ZK_ADDRESS.getPropertyValue()) : DEFAULT_ZOOKEEPER_ADDRESS;

        // zk 已经启动，则直接返回
        if (zkClient != null && zkClient.getState() == CuratorFrameworkState.STARTED) {
            return zkClient;
        }

        zkClient = CuratorFrameworkFactory.builder()
                // 要连接的服务器(可以是服务器列表)
                .connectString(zookeeperAddress)
                // 设置重试策略。重试3次，将增加重试之间的睡眠时间。
                .retryPolicy(new ExponentialBackoffRetry(BASE_SLEEP_TIME, MAX_RETRIES))
                .build();

        zkClient.start();

        try {
            // 等待30s，直到连接到 zookeeper
            if (!zkClient.blockUntilConnected(30, TimeUnit.SECONDS)) {
                throw new RuntimeException("Time out waiting to connect to ZK!");
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return zkClient;
    }

    /**
     * 创建持久节点。
     *
     * @param path node path
     */
    public static void createPersistentNode(CuratorFramework zkClient, String path) {

        try {
            // 先判断节点是否已经存在
            if (REGISTERED_PATH_SET.contains(path) || zkClient.checkExists().forPath(path) != null) {
                log.info("The node already exists. The node is:[{}]", path);
            } else {
                // 创建持久节点  //eg: /my-rpc/github.wx.HelloService/127.0.0.1:9999
                zkClient.create().creatingParentsIfNeeded().withMode(CreateMode.PERSISTENT).forPath(path);
                log.info("The node was created successfully. The node is:[{}]", path);
            }
            REGISTERED_PATH_SET.add(path);
        } catch (Exception e) {
            log.error("create persistent node for path [{}] fail", path);
        }
    }

    /**
     * 获取 rpcServiceName 对应 node下的所有孩子节点
     *
     * @param rpcServiceName rpc service name  eg:github.wx.HelloServicetest2version1
     * @return 指定节点下的所有子节点
     */
    public static List<String> getChildrenNodes(CuratorFramework zkClient, String rpcServiceName) {
        // 先判断 rpcServiceName 对应 node 的子节点列表是否已经存在
        if (SERVICE_ADDRESS_MAP.containsKey(rpcServiceName)) {
            return SERVICE_ADDRESS_MAP.get(rpcServiceName);
        }
        List<String> result = null;
        // rpcServiceName 对应的节点 path
        String servicePath = ZK_REGISTER_ROOT_PATH + "/" + rpcServiceName;
        try {
            result = zkClient.getChildren().forPath(servicePath);
            SERVICE_ADDRESS_MAP.put(servicePath, result);
            registerWatcher(rpcServiceName, zkClient);
        } catch (Exception e) {
            log.error("get children nodes for path [{}] fail", servicePath);
        }

        return result;
    }

    /**
     * 注册以监听对指定节点的更改
     *
     * @param rpcServiceName  rpc Service Name  eg:github.wx.HelloServicetest2version
     */
    private static void registerWatcher(String rpcServiceName, CuratorFramework zkClient) throws Exception {
        String servicePath = ZK_REGISTER_ROOT_PATH + "/" + rpcServiceName;

        // 监听 servicePath 节点下子节点的变化
        PathChildrenCache pathChildrenCache = new PathChildrenCache(zkClient, servicePath, true);

        // PathChildrenCacheListener 接口来处理监听到的事件（节点变化时，更新 SERVICE_ADDRESS_MAP）
        // pathChildrenCacheEvent：事件对象
        PathChildrenCacheListener pathChildrenCacheListener = (curatorFramework, pathChildrenCacheEvent) -> {
            List<String> serviceAddresses = curatorFramework.getChildren().forPath(servicePath);
            SERVICE_ADDRESS_MAP.put(rpcServiceName, serviceAddresses);
        };

        // 添加监听
        pathChildrenCache.getListenable().addListener(pathChildrenCacheListener);
        pathChildrenCache.start();
    }

    /**
     * 清空数据注册表
     * @param inetSocketAddress 服务地址
     */
    public static void clearRegistry(CuratorFramework zkClient, InetSocketAddress inetSocketAddress) {
        REGISTERED_PATH_SET.stream().parallel().forEach(p -> {
            if (p.endsWith(inetSocketAddress.toString())) {
                try {
                    zkClient.delete().forPath(p);
                } catch (Exception e) {
                    log.error("clear registry for path [{}] fail", p);
                }
            }
        });
        log.info("All registered services on the server are cleared:[{}]", REGISTERED_PATH_SET.toString());
    }
}
