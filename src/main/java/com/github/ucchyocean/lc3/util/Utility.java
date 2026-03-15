/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2020
 */
package com.github.ucchyocean.lc3.util;

import com.github.ucchyocean.lc3.LunaChat;
import com.github.ucchyocean.lc3.LunaChatMode;
import com.google.common.io.Files;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;

/**
 * ユーティリティクラス
 *
 * @author ucchy
 */
public class Utility {

    private static final Pattern PATTERN_COLOR_ALT = Pattern.compile("&([0-9a-fk-orA-FK-OR])");
    private static final Pattern PATTERN_WEB_COLOR_6 = Pattern.compile("#([0-9a-fA-F])([0-9a-fA-F])([0-9a-fA-F])([0-9a-fA-F])([0-9a-fA-F])([0-9a-fA-F])");
    private static final Pattern PATTERN_WEB_COLOR_3 = Pattern.compile("#([0-9a-fA-F])([0-9a-fA-F])([0-9a-fA-F])");
    private static final Pattern PATTERN_SECTION_CODE = Pattern.compile("§([0-9a-fk-orxA-FK-ORX])");
    private static final Pattern PATTERN_ALT_CODE = Pattern.compile("&([0-9a-fk-orxA-FK-ORX])");
    private static final Pattern PATTERN_HEX_6 = Pattern.compile("#[0-9a-fA-F]{6}");
    private static final Pattern PATTERN_HEX_3 = Pattern.compile("#[0-9a-fA-F]{3}");
    public static final Pattern PATTERN_HALFWIDTH_KATAKANA = Pattern.compile("[ \\uFF61-\\uFF9F]+");

    /**
     * jarファイルの中に格納されているファイルを、jarファイルの外にコピーするメソッド
     *
     * @param jarFile        jarファイル
     * @param targetFile     コピー先
     * @param sourceFilePath コピー元
     * @param isBinary       バイナリファイルかどうか
     */
    public static void copyFileFromJar(File jarFile, File targetFile, String sourceFilePath, boolean isBinary) {

        File parent = targetFile.getParentFile();
        if (!parent.exists()) {
            parent.mkdirs();
        }

        if (jarFile.isDirectory()) {
            File file = new File(jarFile, sourceFilePath);

            try {
                Files.copy(file, targetFile);
            } catch (IOException e) {
                Logger.getLogger("LunaChat").log(Level.WARNING, "Failed to copy resource file", e);
            }

        } else {

            try (JarFile jar = new JarFile(jarFile)) {
                ZipEntry zipEntry = jar.getEntry(sourceFilePath);
                InputStream is = jar.getInputStream(zipEntry);

                try (FileOutputStream fos = new FileOutputStream(targetFile)) {

                    if (isBinary) {
                        byte[] buf = new byte[8192];
                        int len;
                        while ((len = is.read(buf)) != -1) {
                            fos.write(buf, 0, len);
                        }

                    } else {

                        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8)); BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(fos, StandardCharsets.UTF_8))) {

                            String line;
                            while ((line = reader.readLine()) != null) {
                                writer.write(line);
                                writer.newLine();
                            }
                        }
                    }
                }
            } catch (IOException e) {
                Logger.getLogger("LunaChat").log(Level.WARNING, "Failed to copy resource file", e);
            }
        }
    }

    /**
     * 文字列内のカラーコード候補（&a）を、カラーコード（§a）に置き換えする
     *
     * @param source 置き換え元の文字列
     * @return 置き換え後の文字列
     */
    public static String replaceColorCode(String source) {
        if (source == null) return null;
        return PATTERN_COLOR_ALT.matcher(replaceWebColorCode(source)).replaceAll("§$1");
    }

    /**
     * Webカラーコード（#99AABBなど）を、カラーコードに置き換えする
     *
     * @param source 置き換え元の文字列
     * @return 置き換え後の文字列
     */
    private static String replaceWebColorCode(String source) {
        String result = PATTERN_WEB_COLOR_6.matcher(source).replaceAll("§x§$1§$2§$3§$4§$5§$6");
        return PATTERN_WEB_COLOR_3.matcher(result).replaceAll("§x§$1§$1§$2§$2§$3§$3");
    }

    /**
     * 文字列に含まれているカラーコード（§a）やカラーコード候補（&aや#99AABB）を除去して返す
     *
     * @param source 置き換え元の文字列
     * @return 置き換え後の文字列
     */
    public static String stripColorCode(String source) {
        if (source == null) return null;
        return PATTERN_SECTION_CODE.matcher(stripAltColorCode(source)).replaceAll("");
    }

    /**
     * 文字列に含まれているカラーコード候補（&aや#99AABB）を除去して返す
     *
     * @param source 置き換え元の文字列
     * @return 置き換え後の文字列
     */
    public static String stripAltColorCode(String source) {
        if (source == null) return null;
        source = PATTERN_HEX_6.matcher(source).replaceAll("");
        source = PATTERN_HEX_3.matcher(source).replaceAll("");
        return PATTERN_ALT_CODE.matcher(source).replaceAll("");
    }

    /**
     * カラーコード（§a）かどうかを判断する
     *
     * @param code カラーコード
     * @return カラーコードかどうか
     */
    public static boolean isColorCode(String code) {
        if (code == null) return false;
        return PATTERN_SECTION_CODE.matcher(code).matches();
    }

    /**
     * カラーコード候補（&aや#99AABB）かどうかを判断する
     *
     * @param color カラーコード候補
     * @return カラーコード候補かどうか
     */
    private static final Pattern PATTERN_IS_ALT_COLOR = Pattern.compile("(&[0-9a-fk-orA-FK-OR]|#[0-9a-fA-F]{3}|#[0-9a-fA-F]{6})");

    public static boolean isAltColorCode(String code) {
        if (code == null) return false;
        return PATTERN_IS_ALT_COLOR.matcher(code).matches();
    }

    /**
     * ChatColorで指定可能な色（REDとかGREENとか）かどうかを判断する
     *
     * @param color カラー表記の文字列
     * @return 指定可能かどうか
     */
    public static boolean isValidColor(String color) {
        if (color == null) return false;
        for (ChatColor c : ChatColor.values()) {
            if (c.name().equals(color.toUpperCase())) {
                return true;
            }
        }
        return false;
    }

    /**
     * カラー表記の文字列（REDとかGREENとか）を、カラーコード候補（&a）に変換する
     *
     * @param color カラー表記の文字列
     * @return カラーコード候補
     */
    public static String changeToColorCode(String color) {
        return "&" + changeToChatColor(color).getChar();
    }

    /**
     * カラー表記の文字列を、ChatColorクラスに変換する
     *
     * @param color カラー表記の文字列
     * @return ChatColorクラス
     */
    public static ChatColor changeToChatColor(String color) {
        if (isValidColor(color)) {
            return ChatColor.valueOf(color.toUpperCase());
        }
        return ChatColor.WHITE;
    }

    /**
     * 指定された文字数のアスタリスクの文字列を返す
     *
     * @param length アスタリスクの個数
     * @return 指定された文字数のアスタリスク
     */
    public static String getAstariskString(int length) {
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < length; i++) {
            buf.append("*");
        }
        return buf.toString();
    }

    /**
     * 指定された名前のプレイヤーが接続したことがあるかどうかを検索する
     *
     * @param name プレイヤー名
     * @return 接続したことがあるかどうか
     */
    public static boolean existsOfflinePlayer(String name) {
        if (LunaChat.getUUIDCacheData().getUUIDFromName(name) != null) {
            return true;
        }
        if (LunaChat.getMode() == LunaChatMode.BUKKIT) {
            return UtilityBukkit.existsOfflinePlayer(name);
        }
        return false;
    }

    /**
     * 動作環境のロケールを取得する。
     *
     * @return 動作環境のロケール
     */
    public static Locale getDefaultLocale() {
        Locale locale = Locale.getDefault();
        if (locale == null) return Locale.ENGLISH;
        return locale;
    }
}
