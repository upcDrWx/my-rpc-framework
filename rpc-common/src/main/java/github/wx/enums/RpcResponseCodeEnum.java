package github.wx.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

/**
 * @author wx
 * @date 2023/10/4 16:31
 */
@AllArgsConstructor
@Getter
@ToString
public enum RpcResponseCodeEnum {

    SUCCESS(200, "The remote call is successful"),
    FAIL(500, "The remote call is fail");
    private final int code;

    private final String message;

}