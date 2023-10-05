package github.wx.utils;

/**
 * @author wx
 * @date 2023/10/4 11:33
 */
public class RuntimeUtil {
    /**
     * 获取CPU的核心数
     *
     * @return cpu的核心数
     */
    public static int cpus() {
        return Runtime.getRuntime().availableProcessors();
    }
}
