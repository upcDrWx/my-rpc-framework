package github.wx.annotation;

import java.lang.annotation.*;

/**
 * 消费服务
 *
 * @author wx
 * @date 2023/10/5 11:31
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
@Inherited
public @interface RpcReference{

    /**
     * Service version
     */
    String version() default "";

    /**
     * Service group,
     */
    String group() default "";
}
