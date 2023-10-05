package github.wx.remoting.transport.socket.test;

import github.wx.remoting.dto.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;


public class HelloServer {
    private static final Logger log = LoggerFactory.getLogger(HelloServer.class);

    public void start(int port) {
        //1. 创建socket并绑定端口
        try(ServerSocket server = new ServerSocket(port)) {
            Socket socket;

            //2. 通过accept方法监听客户端请求
            while ((socket = server.accept()) != null) {
                log.info("client connected");
                try (ObjectInputStream objectInputStream = new ObjectInputStream(socket.getInputStream());
                     ObjectOutputStream objectOutputStream = new ObjectOutputStream(socket.getOutputStream())) {

                    //3. 通过输入流读取客户端发送的消息
                    Message message = (Message) objectInputStream.readObject();
                    log.info("server receive message:" + message.getContent());
                    message.setContent("new content");
                    //4. 通过输出流向客户端发送响应信息
                    objectOutputStream.writeObject(message);
                    objectOutputStream.flush();
                } catch (IOException | ClassNotFoundException e) {
                    log.error("occur exception:", e);
                }
            }

        } catch (IOException e) {
            log.error("occur IOException:", e);
        }

    }
    public static void main(String[] args) {
        HelloServer helloServer = new HelloServer();
        helloServer.start(6666);
    }
}
