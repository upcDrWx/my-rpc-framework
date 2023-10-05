package github.wx.spring;

import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.core.type.filter.AnnotationTypeFilter;

import java.lang.annotation.Annotation;

/**
 * 自定义扫描包的类
 *
 * ClassPathBeanDefinitionScanner 是Spring提供的一个用于从类路径扫描符合特定条件的bean定义并注册到Spring容器中的工具类
 *
 * @author wx
 * @date 2023/10/5 12:09
 */
public class CustomScanner extends ClassPathBeanDefinitionScanner {

    public CustomScanner(BeanDefinitionRegistry registry, Class<? extends Annotation> annoType) {
        super(registry);
        super.addIncludeFilter(new AnnotationTypeFilter(annoType));
    }

    @Override
    public int scan(String... basePackages) {
        return super.scan(basePackages);
    }

}
