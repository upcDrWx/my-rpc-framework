package github.wx.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author wx
 * @date 2023/10/4 21:05
 */
@AllArgsConstructor
@Getter
public enum RpcConfigEnum {

    RPC_CONFIG_PATH("rpc.properties"),
    ZK_ADDRESS("rpc.zookeeper.address");

    private final String propertyValue;

}
