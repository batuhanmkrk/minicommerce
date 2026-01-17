package com.minicommerceapi.minicommerce.util;

import java.text.Normalizer;
import java.util.Locale;

/**
 * URL dostu linkler olusturmak icin basit bir yardimci sinif.
 * Regex kullanarak URL-dostu bir "slug" uretmek icin kucuk bir yardimci. Gerektikce genisletilebilir.
 */
public final class SlugUtil {

    private SlugUtil() {
        // utility class
    }

    public static String slugify(String input) {
        if (input == null) return "";

        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");

        String lower = normalized.toLowerCase(Locale.ROOT).trim();
        String dashed = lower.replaceAll("[^a-z0-9]+", "-");
        return dashed.replaceAll("(^-+)|(-+$)", "");
    }
}
