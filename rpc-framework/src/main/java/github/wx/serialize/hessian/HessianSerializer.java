package github.wx.serialize.hessian;

import com.caucho.hessian.io.HessianInput;
import com.caucho.hessian.io.HessianOutput;
import github.wx.exception.SerializeException;
import github.wx.serialize.Serializer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Hessian 是一种动态类型、二进制序列化和Web服务协议，专为面向对象的传输而设计。
 *
 * @author wx
 * @date 2023/10/5 11:49
 */
public class HessianSerializer implements Serializer {
    @Override
    public byte[] serialize(Object obj) {
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
            HessianOutput hessianOutput = new HessianOutput(byteArrayOutputStream);
            hessianOutput.writeObject(obj);

            return byteArrayOutputStream.toByteArray();
        } catch (IOException e) {
            throw new SerializeException("Serialization failed");
        }
    }

    @Override
    public <T> T deserialize(byte[] bytes, Class<T> clazz) {
        try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes)) {
            HessianInput hessianInput = new HessianInput(byteArrayInputStream);
            Object o = hessianInput.readObject();

            return clazz.cast(o);
        } catch (IOException e) {
            throw new SerializeException("Deserialization failed");
        }
    }
}
