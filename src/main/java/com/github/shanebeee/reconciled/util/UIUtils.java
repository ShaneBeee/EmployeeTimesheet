package com.github.shanebeee.reconciled.util;

import javax.imageio.ImageIO;
import java.awt.Image;
import java.io.IOException;
import java.net.URL;

public class UIUtils {

    public static Image createAppIcon(int size) {
        try {
            URL url = UIUtils.class.getResource("/images/1024.png");
            if (url != null) {
                Image image = ImageIO.read(url);
                return image.getScaledInstance(size, size, Image.SCALE_SMOOTH);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Fallback if image fails to load
        return null;
    }

}
