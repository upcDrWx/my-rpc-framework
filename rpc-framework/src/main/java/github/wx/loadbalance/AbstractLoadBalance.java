package github.wx.loadbalance;

import github.wx.remoting.dto.RpcRequest;
import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * @author wx
 * @date 2023/10/5 9:23
 */
public abstract class AbstractLoadBalance implements LoadBalance {
    @Override
    public String selectServiceAddress(List<String> serviceUrlList, RpcRequest rpcRequest) {
        if (CollectionUtils.isEmpty(serviceUrlList)) {
            return null;
        }
        if (serviceUrlList.size() == 1) {
            return serviceUrlList.get(0);
        }

        return doSelect(serviceUrlList, rpcRequest);
    }

    protected abstract String doSelect(List<String> serviceUrlList, RpcRequest rpcRequest);
}
