package github.wx.exception;

import github.wx.enums.RpcErrorMessageEnum;

/**
 * Rpc 异常类
 *
 * @author wx
 * @date 2023/10/4 10:36
 */
public class RpcException extends RuntimeException {
    public RpcException(RpcErrorMessageEnum rpcErrorMessageEnum, String detail) {
        super(rpcErrorMessageEnum.getMessage() + ":" + detail);
    }

    public RpcException(String message, Throwable cause) {
        super(message, cause);
    }

    public RpcException(RpcErrorMessageEnum rpcErrorMessageEnum) {
        super(rpcErrorMessageEnum.getMessage());
    }
}
