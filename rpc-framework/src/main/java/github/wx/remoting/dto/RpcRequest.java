package github.wx.remoting.dto;

import lombok.*;

import java.io.Serializable;


/**
 *
 * @author wx
 * @date 2023/10/3 21:57
 */


@AllArgsConstructor
@NoArgsConstructor
@Getter
@Builder
@ToString
public class RpcRequest implements Serializable {
    private static final long serialVersionUID = 1905122041950251207L;
    private String requestId;
    private String interfaceName;
    private String methodName;
    private Object[] parameters;
    private Class<?>[] paramTypes;
    private String version;  // version(服务版本), 主要是为后续不兼容升级提供可能
    private String group;   // group, 用于处理一个接口有多个类实现的情况

    public String getRpcServiceName() {
        return this.getInterfaceName() + this.getGroup() + this.getVersion();
    }
}