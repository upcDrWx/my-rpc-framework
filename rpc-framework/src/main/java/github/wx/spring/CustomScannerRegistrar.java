package github.wx.spring;

import github.wx.annotation.RpcScan;
import github.wx.annotation.RpcService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.StandardAnnotationMetadata;
import org.springframework.stereotype.Component;

/**
 * 扫描自定义注解标识的bean然后放入Spring 容器中
 *
 * ImportBeanDefinitionRegistrar接口：
 *      就是在spring应用启动过程，一些被@Import注解的类（这些类都实现了ImportBeanDefinitionRegistrar接口）
 *      会执行ImportBeanDefinitionRegistrar的registerBeanDefinitions方法，然后生成BeanDefinition对象，
 *      并最终注册到BeanDefinitionRegistry中，为后续实例化bean做准备的
 *
 * 实现了ResourceLoaderAware接口，使得它可以获得对Spring的ResourceLoader的访问，这使得它能够加载外部资源
 *
 * @author wx
 * @date 2023/10/5 14:56
 */
@Slf4j
public class CustomScannerRegistrar implements ImportBeanDefinitionRegistrar, ResourceLoaderAware {

    // Spring Bean扫描的基础包路径
    private static final String SPRING_BEAN_BASE_PACKAGE = "github.wx";
    // 要从RpcScan注解中提取的属性名
    private static final String BASE_PACKAGE_ATTRIBUTE_NAME = "basePackage";

    private ResourceLoader resourceLoader;

    /**
     * 	如果把该Bean部署在Spring容器中，该方法将会由Spring容器负责调用。
     * 	Spring容器调用该方法时，Spring会将自身作为参数传给该方法
     * @param resourceLoader
     */
    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }


    /**
     * 自定义地注册Bean
     *
     * @param importingClassMetadata
     * @param beanDefinitionRegistry  注册中心，用于注册新的bean
     */
    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata,
                                        BeanDefinitionRegistry beanDefinitionRegistry) {
        // 获取 RpcScan 注解的 attributes 和 values  eg: basePackage = {"github.wx"}
        AnnotationAttributes rpcScanAnnotationAttributes =
                AnnotationAttributes.fromMap(importingClassMetadata.getAnnotationAttributes(RpcScan.class.getName()));
        String[] rpcScanBasePackages = new String[0];
        if (rpcScanAnnotationAttributes != null) {
            // 获取 basePackage 的值
            rpcScanBasePackages = rpcScanAnnotationAttributes.getStringArray(BASE_PACKAGE_ATTRIBUTE_NAME);
        }

        // 扫描包 为null，默认设置扫描包为当前被注解标记的类的包名
        if (rpcScanBasePackages.length == 0) {
            rpcScanBasePackages = new String[]{
                    ((StandardAnnotationMetadata) importingClassMetadata).getIntrospectedClass().getPackage().getName()
            };
        }

        // 扫描 RpcService 注解
        CustomScanner rpcServiceScanner = new CustomScanner(beanDefinitionRegistry, RpcService.class);
        // 扫描 Component 注解
        CustomScanner springBeanScanner = new CustomScanner(beanDefinitionRegistry, Component.class);


        if (resourceLoader != null) {
            rpcServiceScanner.setResourceLoader(resourceLoader);
            springBeanScanner.setResourceLoader(resourceLoader);
        }


        int springBeanAmount = springBeanScanner.scan(SPRING_BEAN_BASE_PACKAGE);
        log.info("springBeanScanner扫描的数量 [{}]", springBeanAmount);
        int rpcServiceCount = rpcServiceScanner.scan(rpcScanBasePackages);
        log.info("rpcServiceScanner扫描的数量 [{}]", rpcServiceCount);

    }
}
