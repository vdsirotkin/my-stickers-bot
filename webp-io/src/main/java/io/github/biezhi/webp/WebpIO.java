package io.github.biezhi.webp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

/**
 * Webp converter
 *
 * @author biezhi
 * @date 2017/10/2
 */
public class WebpIO {

    private static final Logger log = LoggerFactory.getLogger(WebpIO.class);

    public static WebpIO create() {
        return new WebpIO();
    }

    /**
     * Converter webp file to normal image
     *
     * @param src  webp file path
     * @param dest normal image path
     */
    public void toNormalImage(String src, String dest) {
        toNormalImage(new File(src), new File(dest));
    }

    /**
     * Converter webp file to normal image
     *
     * @param src  webp file path
     * @param dest normal image path
     */
    public void toNormalImage(File src, File dest, String... params) {
        StringBuilder paramsResult = new StringBuilder();
        for (String param : params) {
            paramsResult.append(param).append(" ");
        }
        String command = (dest.getName().endsWith(".gif") ? "gif2webp" : "dwebp ") + src.getPath() + " " + paramsResult.toString() + " -o " + dest.getPath();
        this.executeCommand(command);
    }

    /**
     * Convert normal image to webp file
     *
     * @param src  nomal image path
     * @param dest webp file path
     */
    public void toWEBP(String src, String dest) {
        toWEBP(new File(src), new File(dest));
    }

    /**
     * Convert normal image to webp file
     *
     * @param src  nomal image path
     * @param dest webp file path
     */
    public void toWEBP(File src, File dest) {
        try {
            String command = (src.getName().endsWith(".gif") ? "gif2webp " : "cwebp ") + src.getPath() + " -o " + dest.getPath();
            this.executeCommand(command);
        } catch (Exception e) {
            throw new WebpIOException(e);
        }
    }

    /**
     * execute command
     *
     * @param command command direct
     * @return
     */
    private String executeCommand(String command) {
        log.debug("Execute: " + command);

        StringBuilder output = new StringBuilder();
        Process       p;
        try {
            p = Runtime.getRuntime().exec(command);
            p.waitFor();
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String         line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        } catch (Exception e) {
            throw new WebpIOException(e);
        }
        if (!"".equals(output.toString())) {
            log.debug("Output: " + output);
        }
        return "";
    }

}
