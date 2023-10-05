package github.wx.loadbalance.loadbalancer;

import github.wx.loadbalance.AbstractLoadBalance;
import github.wx.remoting.dto.RpcRequest;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 参考 dubbo 一致哈希负载均衡实现:
 * https://github.com/apache/dubbo/blob/2d9583adf26a2d8bd6fb646243a9fe80a77e65d5/dubbo-cluster/src/main/java/org/apache/dubbo/rpc/cluster/loadbalance/ConsistentHashLoadBalance.java
 *
 * @author wx
 * @date 2023/10/5 9:29
 */
public class ConsistentHashLoadBalance extends AbstractLoadBalance {

    private final ConcurrentHashMap<String, ConsistentHashSelector> selectors = new ConcurrentHashMap<>();

    @Override
    protected String doSelect(List<String> serviceUrlList, RpcRequest rpcRequest) {
        int identityHashCode = System.identityHashCode(serviceUrlList);
        String rpcServiceName = rpcRequest.getRpcServiceName();
        ConsistentHashSelector selector = selectors.get(rpcServiceName);

        if (selector == null || selector.identityHashCode != identityHashCode) {
            selectors.put(
                    rpcServiceName,
                    new ConsistentHashSelector(serviceUrlList, 160, identityHashCode)
            );
            selector = selectors.get(rpcServiceName);
        }

        return selector.select(rpcServiceName + Arrays.stream(rpcRequest.getParameters()));

    }

    private static final class ConsistentHashSelector {
        // 虚拟节点和 serviceUrlList 的映射 Map
        private final TreeMap<Long, String> virtualInvokers;

        private final int identityHashCode;

        ConsistentHashSelector(List<String> invokers, int replicaNumber, int identityHashCode) {

            this.virtualInvokers = new TreeMap<>();
            // 调用结点的 HashCode
            this.identityHashCode = identityHashCode;

            // 构建一致性hash表
            for (String invoker : invokers) {
                // 对每一个invoker，为其创建 replicaNumber / 4个虚拟节点
                for (int i = 0; i < replicaNumber / 4; i++) {
                    byte[] digest = md5(invoker + i);
                    for (int h = 0; h < 4; h++) {
                        long m = hash(digest, h);
                        virtualInvokers.put(m, invoker);
                    }
                }
            }
        }

        static byte[] md5(String key) {
            MessageDigest md;
            try {
                md = MessageDigest.getInstance("MD5");
                byte[] bytes = key.getBytes(StandardCharsets.UTF_8);
                md.update(bytes);
            } catch (NoSuchAlgorithmException e) {
                throw new IllegalStateException(e.getMessage(), e);
            }

            return md.digest();
        }

        static long hash(byte[] digest, int idx) {
            return ((long) (digest[3 + idx * 4] & 255) << 24
                    | (long) (digest[2 + idx * 4] & 255) << 16
                    | (long) (digest[1 + idx * 4] & 255) << 8
                    | (long) (digest[idx * 4] & 255)) & 4294967295L;
        }

        public String select(String rpcServiceKey) {
            byte[] digest = md5(rpcServiceKey);
            return selectForKey(hash(digest, 0));
        }

        public String selectForKey(long hashCode) {
            // 返回所有key大于 hashCode 的第一个键值对
            Map.Entry<Long, String> entry = virtualInvokers.tailMap(hashCode, true).firstEntry();

            if (entry == null) {
                entry = virtualInvokers.firstEntry();
            }

            return entry.getValue();
        }

    }
}
