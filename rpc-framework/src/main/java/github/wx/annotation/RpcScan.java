package github.wx.annotation;

import github.wx.spring.CustomScannerRegistrar;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * 扫描自定义注解
 *
 * @author wx
 * @date 2023/10/5 11:34
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Import(CustomScannerRegistrar.class)
@Documented
public @interface RpcScan {
    String[] basePackage();
}
