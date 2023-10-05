package github.wx.annotation;

import java.lang.annotation.*;

/**
 * 注册服务，标记在服务实现类上
 *
 * @author wx
 * @date 2023/10/5 11:35
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@Inherited
public @interface RpcService {
    /**
     * Service version
     */
    String version() default "";

    /**
     * Service group
     */
    String group() default "";
}
