package github.wx.spring;

import github.wx.annotation.RpcReference;
import github.wx.annotation.RpcService;
import github.wx.config.RpcServiceConfig;
import github.wx.enums.RpcRequestTransportEnum;
import github.wx.extension.ExtensionLoader;
import github.wx.factory.SingletonFactory;
import github.wx.provider.ServiceProvider;
import github.wx.provider.impl.ZkServiceProviderImpl;
import github.wx.proxy.RpcClientProxy;
import github.wx.remoting.transport.RpcRequestTransport;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;

/**
 * 在 Spring 容器初始化 bean 之前或之后调用，看看类是否被注解
 *
 * @author wx
 * @date 2023/10/5 15:12
 */
@Slf4j
@Component
public class SpringBeanPostProcessor implements BeanPostProcessor {

    private final ServiceProvider serviceProvider;
    private final RpcRequestTransport rpcClient;

    public SpringBeanPostProcessor() {
        this.serviceProvider = SingletonFactory.getInstance(ZkServiceProviderImpl.class);
        this.rpcClient = ExtensionLoader.getExtensionLoader(RpcRequestTransport.class)
                .getExtension(RpcRequestTransportEnum.NETTY.getName());
    }


    /**
     * 检查bean是否被RpcService注解标记。
     * 如果是，获取该注解的 version 和 group 信息，并构建一个RpcServiceConfig对象。
     * 使用服务提供者来发布该服务。(先注册)
     *
     */
    @SneakyThrows
    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException{
        if (bean.getClass().isAnnotationPresent(RpcService.class)) {
            log.info("[{}] 被注解为  [{}]", bean.getClass().getName(), RpcService.class.getCanonicalName());
            // get RpcService annotation
            RpcService rpcService = bean.getClass().getAnnotation(RpcService.class);

            RpcServiceConfig rpcServiceConfig = RpcServiceConfig.builder()
                    .group(rpcService.group())
                    .version(rpcService.version())
                    .service(bean).build();

            serviceProvider.publishService(rpcServiceConfig);
        }
        return bean;
    }

    /**
     * 对bean的每个字段进行遍历。
     * 检查字段是否被 RpcReference 注解标记。
     * 如果是，创建一个新的RPC客户端代理，并为这个字段赋值。（再消费）
     *
     */
    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        Class<?> targetClass = bean.getClass();
        // 获取类中定义的所有字段
        Field[] declaredFields = targetClass.getDeclaredFields();
        for (Field declaredField : declaredFields) {
            RpcReference rpcReference = declaredField.getAnnotation(RpcReference.class);
            if (rpcReference != null) {
                RpcServiceConfig rpcServiceConfig = RpcServiceConfig.builder()
                        .group(rpcReference.group())
                        .version(rpcReference.version()).build();
                RpcClientProxy rpcClientProxy = new RpcClientProxy(rpcClient, rpcServiceConfig);
                Object clientProxy = rpcClientProxy.getProxy(declaredField.getType());
                declaredField.setAccessible(true);
                try {
                    declaredField.set(bean, clientProxy);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
        return bean;
    }
}
