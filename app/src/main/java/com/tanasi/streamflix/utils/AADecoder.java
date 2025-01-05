package com.tanasi.streamflix.utils;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AADecoder {

    public static String decode(String text, boolean alt) {
        // Remove whitespace and comments
        text = text.replaceAll("\\s+|/\\*.*?\\*/", "");

        String data;
        String[] chars;
        String char1;
        String char2;

        if (alt) {
            data = text.split("\\+\\(ﾟɆﾟ\\)\\[ﾟoﾟ]")[1];
            chars = data.split("\\+\\(ﾟɆﾟ\\)\\[ﾟεﾟ]\\+");
            char1 = "ღ";
            char2 = "(ﾟɆﾟ)[ﾟΘﾟ]";
        } else {
            data = text.split("\\+\\(ﾟДﾟ\\)\\[ﾟoﾟ]")[1];
            chars = data.split("\\+\\(ﾟДﾟ\\)\\[ﾟεﾟ]\\+");
            char1 = "c";
            char2 = "(ﾟДﾟ)['0']";
        }

        StringBuilder txt = new StringBuilder();
        for (int i = 1; i < chars.length; i++) {
            String charSeq = chars[i].replace("(oﾟｰﾟo)", "u").replace(char1, "0").replace(char2, "c").replace("ﾟΘﾟ", "1").replace("!+[]", "1").replace("-~", "1+").replace("o", "3").replace("_", "3").replace("ﾟｰﾟ", "4").replace("(+", "(");

            charSeq = charSeq.replaceAll("\\((\\d)\\)", "$1");

            StringBuilder subchar = new StringBuilder();
            StringBuilder c = new StringBuilder();
            for (char v : charSeq.toCharArray()) {
                c.append(v);
                try {
                    int x = eval(c.toString());
                    subchar.append(x);
                    c = new StringBuilder();
                } catch (Exception e) {
                    // Ignore and continue
                }
            }
            if (subchar.length() > 0) {
                txt.append(subchar).append("|");
            }
        }

        if (txt.length() > 0) {
            txt.setLength(txt.length() - 1); // Remove the last '|'
        }

        String[] txtArray = txt.toString().replace("+", "").split("\\|");
        StringBuilder txtResult = new StringBuilder();
        for (String n : txtArray) {
            txtResult.append((char) Integer.parseInt(n, 8));
        }

        return toStringCases(txtResult.toString());
    }

    public static int eval(String expression) {
        try {
            Context rhino = Context.enter();
            rhino.setOptimizationLevel(-1);
            Scriptable scope = rhino.initStandardObjects();
            Object result = rhino.evaluateString(scope, expression, "JavaScript", 1, null);
            return ((Double) result).intValue();
        } finally {
            Context.exit();
        }
    }


    private static String toStringCases(String txtResult) {
        String sumBase = "";
        boolean m3 = false;
        Pattern pattern;
        Matcher matcher;

        if (txtResult.contains(".toString(")) {
            if (txtResult.contains("+(")) {
                m3 = true;
                pattern = Pattern.compile(".toString...([0-9]+).", Pattern.DOTALL);
                matcher = pattern.matcher(txtResult);
                if (matcher.find()) {
                    sumBase = "+" + matcher.group(1);
                }
            }
            pattern = Pattern.compile("..([0-9]),([0-9]+).", Pattern.DOTALL);
            matcher = pattern.matcher(txtResult);
            while (matcher.find()) {
                int numero = Integer.parseInt(Objects.requireNonNull(matcher.group(2)));
                int base = Integer.parseInt(matcher.group(1) + sumBase);
                String code = toString(numero, base);
                if (m3) {
                    txtResult = txtResult.replace("(" + base + "," + numero + ")", code).replace("\"", "").replace("+", "");
                } else {
                    txtResult = txtResult.replace(numero + ".0.toString(" + base + ")", code).replace("'", "").replace("+", "");
                }
            }
        }
        return txtResult;
    }

    private static String toString(int number, int base) {
        String chars = "0123456789abcdefghijklmnopqrstuvwxyz";
        if (number < base) {
            return String.valueOf(chars.charAt(number));
        } else {
            return toString(number / base, base) + chars.charAt(number % base);
        }
    }
}
