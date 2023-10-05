package github.wx.loadbalance;

import github.wx.extension.SPI;
import github.wx.remoting.dto.RpcRequest;

import java.util.List;

/**
 * 负载均衡策略接口
 *
 * @author wx
 * @date 2023/10/5 9:21
 */
@SPI
public interface LoadBalance {
    /**
     * 从现有服务地址列表中选择一个
     *
     * @param serviceUrlList Service address list
     * @param rpcRequest
     * @return target service address
     */
    String selectServiceAddress(List<String> serviceUrlList, RpcRequest rpcRequest);
}
