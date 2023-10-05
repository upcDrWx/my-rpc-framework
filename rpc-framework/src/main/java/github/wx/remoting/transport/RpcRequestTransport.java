package github.wx.remoting.transport;

import github.wx.extension.SPI;
import github.wx.remoting.dto.RpcRequest;

/**
 * 发送 Rpc请求
 *
 * @author wx
 * @date 2023/10/4 9:59
 */
@SPI
public interface RpcRequestTransport {
    /**
     * 发送 rpc 请求给服务器并返回结果
     *
     * @param rpcRequest message body
     * @return   server 返回的数据
     */
    Object sendRpcRequest(RpcRequest rpcRequest);
}
