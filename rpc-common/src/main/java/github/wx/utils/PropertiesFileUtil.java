package github.wx.utils;

import lombok.extern.slf4j.Slf4j;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

/**
 * @author wx
 * @date 2023/10/4 21:07
 */
@Slf4j
public class PropertiesFileUtil {
    private PropertiesFileUtil() {
    }

    public static Properties readPropertiesFile(String fileName) {
        URL url = Thread.currentThread().getContextClassLoader().getResource("");
        String rpcConfigPath = "";
        if (url != null) {
            rpcConfigPath = url.getPath() + fileName;
        }
        Properties properties = null;
        try (InputStreamReader inputStreamReader =
                     new InputStreamReader(new FileInputStream(rpcConfigPath), StandardCharsets.UTF_8)) {
            properties = new Properties();
            properties.load(inputStreamReader);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return properties;
    }
}
