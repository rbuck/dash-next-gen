package com.github.rbuck.dash.common;

import java.util.LinkedList;
import java.util.List;

/**
 * This SQL parser behaves as follows:
 * <p/>
 * - the statements may be on the same or different lines
 * (line breaks are ignored).
 * <p/>
 * - the SQL statements are (by default) delimited by a semicolon
 * (this can be changed by the DELIMITER instruction)
 * <p/>
 * - two minus signs (--) appearing anywhere is taken as a
 * line comment – all the text from there to the end of the
 * line will be removed from your SQL
 */
public class SqlScript {

    private static final long L_WHITESPACE = lmask("\t\n ");
    private static final long H_WHITESPACE = hmask("\t\n ");

    private static final long L_PRINTABLE = lmask('!', '~');
    private static final long H_PRINTABLE = hmask('!', '~');

    private static final char DEFAULT_DELIMITER = ';';

    public List<String> split(String input) {

        List<String> statements = new LinkedList<>();
        String token;

        char delimiter = DEFAULT_DELIMITER;
        boolean in_skip = false;
        boolean in_single_line_comment = false;
        StringBuilder buffer = new StringBuilder();
        int length = input.length();
        for (int p = 0; p < length; ) {
            // handle printable tokens
            int q = scan(input, p, length, L_PRINTABLE, H_PRINTABLE);
            if (q > p) {
                if (at(input, p, q, '-') && at(input, p, q + 1, '-')) {
                    in_single_line_comment = true;
                    in_skip = true;
                    p = q;
                } else {
                    if (!in_skip) {
                        token = substring(input, p, q);
                        // before appending to the current buffer,
                        // ensure that we don't have a semicolon
                        // surrounded by two terms. split in such
                        // scenarios.
                        int d = seek(token, delimiter);
                        if (d > 0) {
                            buffer.append(token.substring(0, d));
                            statements.add(buffer.toString());
                            buffer.setLength(0);
                            q = p + d + 1;
                        } else {
                            buffer.append(token).append(" ");
                        }
                    }
                    p = q;
                }
                continue;
            }

            // handle whitespace
            q = scan(input, p, p + 1, L_WHITESPACE, H_WHITESPACE);
            if (q > p) {
                // handle whitespace
                if (in_single_line_comment && at(input, p, q, '\n')) {
                    // newlines terminate single line comments
                    in_single_line_comment = false;
                    in_skip = false;
                }
                // skip whitespace
                p = q;
            }

            // if the token starts with -- then change state to in-line-comment
            // if the token ends with the current delimiter then add the current buffer as the next statement
            // otherwise append to the current buffer

        }
        System.out.println(buffer.toString());

        return statements;
    }

    private int scan(String input, int start, int n, long lmask, long hmask) {
        int p = start;
        while (p < n) {
            char c = charat(input, p);
            if (match(c, lmask, hmask)) {
                p++;
                continue;
            }
            break;
        }
        return p;
    }

    private int seek(String input, char c) {
        return input.indexOf(c);
    }

    private boolean at(String input, int start, int end, char c) {
        return (start < end) && (charat(input, start) == c);
    }

    private char charat(String input, int p) {
        return input.charAt(p);
    }

    private String substring(String input, int start, int end) {
        return input.substring(start, end);
    }

    private static boolean match(char c, long lmask, long hmask) {
        return (c < 64) ? ((1L << c) & lmask) != 0 : ((c < 128) && ((1L << (c - 64)) & hmask) != 0);
    }

    private static long lmask(String chars) {
        int n = chars.length();
        long m = 0;
        for (int i = 0; i < n; i++) {
            char c = chars.charAt(i);
            if (c < 64) {
                m |= (1L << c);
            }
        }
        return m;
    }

    private static long hmask(String chars) {
        int n = chars.length();
        long m = 0;
        for (int i = 0; i < n; i++) {
            char c = chars.charAt(i);
            if ((c >= 64) && (c < 128)) {
                m |= (1L << (c - 64));
            }
        }
        return m;
    }

    private static long lmask(char first, char last) {
        long m = 0;
        int f = Math.max(Math.min(first, 63), 0);
        int l = Math.max(Math.min(last, 63), 0);
        for (int i = f; i <= l; i++) {
            m |= 1L << i;
        }
        return m;
    }

    private static long hmask(char first, char last) {
        long m = 0;
        int f = Math.max(Math.min(first, 127), 64) - 64;
        int l = Math.max(Math.min(last, 127), 64) - 64;
        for (int i = f; i <= l; i++) {
            m |= 1L << i;
        }
        return m;
    }
}
