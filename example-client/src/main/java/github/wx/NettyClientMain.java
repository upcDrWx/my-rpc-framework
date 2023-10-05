package github.wx;

import github.wx.annotation.RpcScan;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * @author wx
 * @date 2023/10/5 16:20
 */
@RpcScan(basePackage = {"github.wx"})
public class NettyClientMain {
    public static void main(String[] args) throws InterruptedException {
        AnnotationConfigApplicationContext applicationContext =
                new AnnotationConfigApplicationContext(NettyClientMain.class);

        HelloController helloController = (HelloController) applicationContext.getBean("helloController");
        helloController.test();
    }
}
