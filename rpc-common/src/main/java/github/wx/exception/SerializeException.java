package github.wx.exception;

/**
 * 序列化异常类
 *
 * @author wx
 * @date 2023/10/4 10:40
 */
public class SerializeException extends RuntimeException {
    public SerializeException(String message) {
        super(message);
    }
}
