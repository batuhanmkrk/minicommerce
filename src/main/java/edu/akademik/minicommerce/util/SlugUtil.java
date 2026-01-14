\
    package edu.akademik.minicommerce.util;

    import java.text.Normalizer;
    import java.util.Locale;

    public final class SlugUtil {
        private SlugUtil() {}

        public static String slugify(String input) {
            if (input == null) return "";
            String normalized = Normalizer.normalize(input, Normalizer.Form.NFD)
                    .replaceAll("\\p{M}+", "");
            String lower = normalized.toLowerCase(Locale.ROOT).trim();
            String dashed = lower.replaceAll("[^a-z0-9]+", "-");
            return dashed.replaceAll("(^-+)|(-+$)", "");
        }
    }
