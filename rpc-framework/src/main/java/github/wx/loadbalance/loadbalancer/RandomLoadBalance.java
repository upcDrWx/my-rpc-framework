package github.wx.loadbalance.loadbalancer;

import github.wx.loadbalance.AbstractLoadBalance;
import github.wx.remoting.dto.RpcRequest;

import java.util.List;
import java.util.Random;

/**
 * 实现随机负载均衡策略
 *
 * @author wx
 * @date 2023/10/5 9:27
 */
public class RandomLoadBalance extends AbstractLoadBalance {
    @Override
    protected String doSelect(List<String> serviceUrlList, RpcRequest rpcRequest) {
        Random random = new Random();
        return serviceUrlList.get(random.nextInt(serviceUrlList.size()));
    }
}
