package github.wx.serialize.protostuff;

import github.wx.serialize.Serializer;
import io.protostuff.LinkedBuffer;
import io.protostuff.ProtostuffIOUtil;
import io.protostuff.Schema;
import io.protostuff.runtime.RuntimeSchema;

/**
 * @author wx
 * @date 2023/10/5 11:59
 */
public class ProtostuffSerializer implements Serializer {

    /**
     * 避免每次序列化时重新应用缓冲区空间
     * LinkedBuffer是 Protostuff 序列化库中的一个类，用于提供缓冲功能，以便在序列化和反序列化操作中重复使用以提高性能
     */
    private static final LinkedBuffer BUFFER = LinkedBuffer.allocate(LinkedBuffer.DEFAULT_BUFFER_SIZE);



    @Override
    public byte[] serialize(Object obj) {
        Class<?> clazz = obj.getClass();

         // Schema是一个接口，它代表了一个特定的类（或"消息"）的结构描述。
         // 当序列化或反序列化时，Schema知道如何读取和写入对象的字段
        Schema schema = RuntimeSchema.getSchema(clazz);
        byte[] bytes;
        try {
            bytes = ProtostuffIOUtil.toByteArray(obj, schema, BUFFER);
        } finally {
            BUFFER.clear();
        }
        return bytes;
    }

    @Override
    public <T> T deserialize(byte[] bytes, Class<T> clazz) {
        Schema<T> schema = RuntimeSchema.getSchema(clazz);
        T obj = schema.newMessage();
        // 从 bytes 数组中，以 schema 的结构读取数据，存储到 obj 中
        ProtostuffIOUtil.mergeFrom(bytes, obj, schema);
        return obj;
    }
}
