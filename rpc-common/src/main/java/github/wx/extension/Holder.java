package github.wx.extension;

/**
 * @author wx
 * @date 2023/10/4 21:12
 */
public class Holder<T> {

    private volatile T value;

    public T get() {
        return value;
    }

    public void set(T value) {
        this.value = value;
    }
}
