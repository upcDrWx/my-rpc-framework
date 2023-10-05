package github.wx.config;

import lombok.*;

/**
 * @author wx
 * @date 2023/10/4 10:10
 */

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@ToString
public class RpcServiceConfig {
    /**
     * 服务版本
     */
    private String version = "";
    /**
     * 当接口有多个实现类时，按组区分
     */
    private String group = "";

    /**
     * 目标服务
     */
    private Object service;

    public String getRpcServiceName() {
        return this.getServiceName() + this.getGroup() + this.getVersion();
    }

    public String getServiceName() {
        // 获取 service 对象实现的第一个接口的完全限定类名
        return this.service.getClass().getInterfaces()[0].getCanonicalName();
    }
}
