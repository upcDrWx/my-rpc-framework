package github.wx.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author wx
 * @date 2023/10/4 21:06
 */
@AllArgsConstructor
@Getter
public enum ServiceRegistryEnum {

    ZK("zk");

    private final String name;
}