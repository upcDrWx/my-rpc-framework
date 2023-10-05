package github.wx.remoting.transport.netty.client;

import github.wx.remoting.dto.RpcResponse;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 用于存放未被服务端处理的请求，即客户端发送给服务端等待返回结果的请求（建议限制 map 容器大小，避免未处理请求过多 OOM)
 *
 *
 * @author wx
 * @date 2023/10/4 20:03
 */
public class UnprocessedRequests {

    private static final Map<String, CompletableFuture<RpcResponse<Object>>>
            UNPROCESSED_RESPONSE_FUTURES = new ConcurrentHashMap<>();

    public void put(String requestId, CompletableFuture<RpcResponse<Object>> future) {
        UNPROCESSED_RESPONSE_FUTURES.put(requestId, future);
    }

    //
    public void complete(RpcResponse<Object> rpcResponse) {
        CompletableFuture<RpcResponse<Object>> future = UNPROCESSED_RESPONSE_FUTURES.remove(rpcResponse.getRequestId());

        if (null != future) {
            future.complete(rpcResponse);
        } else {
            throw new IllegalStateException();
        }

    }
}
