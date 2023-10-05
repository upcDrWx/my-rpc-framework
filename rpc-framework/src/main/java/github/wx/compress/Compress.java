package github.wx.compress;

import github.wx.extension.SPI;

/**
 * 压缩接口
 *
 * @author wx
 * @date 2023/10/4 16:59
 */
@SPI
public interface Compress {

    byte[] compress(byte[] bytes);


    byte[] decompress(byte[] bytes);
}
