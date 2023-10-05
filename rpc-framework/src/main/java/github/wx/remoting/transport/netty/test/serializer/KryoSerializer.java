package github.wx.remoting.transport.netty.test.serializer;


import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import github.wx.remoting.dto.RpcRequest;
import github.wx.remoting.dto.RpcResponse;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;


/**
 * 自定义 kryo 序列化实现类
 */
public class KryoSerializer implements Serializer {
    /**
     * 由于 Kryo 不是线程安全的。每个线程都应该有自己的 Kryo，Input 和 Output 实例。
     * 所以，使用 ThreadLocal 存放 Kryo 对象
     */
    private static final ThreadLocal<Kryo> kryoThreadLocal = ThreadLocal.withInitial(() -> {
       Kryo kryo = new Kryo();
       kryo.register(RpcResponse.class);
       kryo.register(RpcRequest.class);

       // 默认值为true,是否关闭注册行为,关闭之后可能存在序列化问题，一般推荐设置为 true
       kryo.setReferences(true);
        //默认值为false,是否关闭循环引用，可以提高性能，但是一般不推荐设置为 true
       kryo.setRegistrationRequired(false);
       return kryo;
    });


    @Override
    public byte[] serialize(Object obj) {
        try(ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            Output output = new Output(byteArrayOutputStream)) {

            Kryo kryo = kryoThreadLocal.get();
            kryo.writeObject(output, obj);
            kryoThreadLocal.remove();
            return output.toBytes();
        } catch (IOException e) {
            throw new SerializeException("序列化失败");
        }
    }

    @Override
    public <T> T deserialize(byte[] bytes, Class<T> clazz) {
        try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);
             Input input = new Input(byteArrayInputStream)) {
            Kryo kryo = kryoThreadLocal.get();
            Object o = kryo.readObject(input, clazz);
            kryoThreadLocal.remove();
            return clazz.cast(o);
        } catch (IOException e) {
            throw new SerializeException("反序列化失败");
        }
    }
}
