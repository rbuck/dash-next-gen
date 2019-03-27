package com.github.rbuck.dash.common;

import java.util.ArrayList;
import java.util.List;

/**
 * Splits SQL scripts into multiple statements.
 */
public class SqlScriptSplitter {

    private static final int STATE_SQL = 0;
    private static final int STATE_SINGLE_LINE_COMMENT = 1;
    private static final int STATE_MULTI_LINE_COMMENT = 2;
    private static final int STATE_LITERAL_STRING = 3;
    private static final int STATE_ESCAPED_NAME = 4;
    private static final int STATE_MULTI_LINE_COMMENT_END = 5;

    private static final int STATEMENT_GENERIC = 0;
    private static final int STATEMENT_SET_DELIMETER = 9;

    public List<String> splitStatements(String sql) {
        // normalize end-of-line
        sql = sql.replace("\r", "");

        int state = STATE_SQL;
        int statement_state = STATEMENT_GENERIC;

        String token = "";
        StringBuilder atom = new StringBuilder();
        String sqlStatement = "";
        String current_delimiter = ";";

        List<String> ret = new ArrayList<>();

        boolean inside_comment = false;
        boolean inside_literal = false;

        for (int i = 0; i < sql.length(); i++) {
            char ch = sql.charAt(i);
            atom.append(ch);

            // if previous the character the end of comment switch state back to SQL
            if (state == STATE_MULTI_LINE_COMMENT_END) {
                state = STATE_SQL;
            }

            switch (ch) {
                case '\'':
                    if (state == STATE_SQL) {
                        state = STATE_LITERAL_STRING;
                    } else if (state == STATE_LITERAL_STRING) {
                        state = STATE_SQL;
                    }
                    break;
                case '\"':
                    if (state == STATE_SQL) {
                        state = STATE_ESCAPED_NAME;
                    } else if (state == STATE_ESCAPED_NAME) {
                        state = STATE_SQL;
                    }
                    break;

                case '-':
                    if (i > 0 && state == STATE_SQL && sql.charAt(i - 1) == '-') {
                        state = STATE_SINGLE_LINE_COMMENT;
                    }
                    break;
                case '/':
                    if (i < sql.length() - 1 && state == STATE_SQL) {
                        char next = sql.charAt(i + 1);
                        if (next == '/') {
                            state = STATE_SINGLE_LINE_COMMENT;
                        } else if (next == '*') {
                            state = STATE_MULTI_LINE_COMMENT;
                        }
                    }
                    if (i > 0 && state == STATE_MULTI_LINE_COMMENT && sql.charAt(i - 1) == '*') {// end start multi-line
                        state = STATE_MULTI_LINE_COMMENT_END;
                    }

                    break;
                case '\n':
                    if (state == STATE_SINGLE_LINE_COMMENT) {// end single line comment
                        state = STATE_SQL;
                    } else if (statement_state == STATEMENT_SET_DELIMETER) {
                        current_delimiter = token;
                    }
                    break;
                default:
                    if (!inside_comment && !inside_literal) {
                        if (!Character.isWhitespace(ch)) {
                            token += ch;
                        } else if (statement_state == STATEMENT_SET_DELIMETER) {
                            current_delimiter = token;
                        }
                    }
                    break;
            }

            sqlStatement += ch;

            inside_comment = state == STATE_SINGLE_LINE_COMMENT || state == STATE_MULTI_LINE_COMMENT
                    || state == STATE_MULTI_LINE_COMMENT_END;
            inside_literal = state == STATE_LITERAL_STRING || state == STATE_ESCAPED_NAME;

            if (token.endsWith(current_delimiter) && state == STATE_SQL) {

                if (statement_state != STATEMENT_SET_DELIMETER) {// Remove SET DELIMITER from statement

                    if (sqlStatement.endsWith(current_delimiter)) {
                        // Remove delimiter
                        sqlStatement = sqlStatement.substring(0, sqlStatement.length() - current_delimiter.length());
                    }

                    sqlStatement = trimWhitespace(sqlStatement);
                    if (!sqlStatement.isEmpty()) {
                        ret.add(sqlStatement);
                    }
                }
                sqlStatement = "";
                statement_state = STATEMENT_GENERIC;
            }

            if ((!inside_comment && !inside_literal)
                    && (Character.isWhitespace(ch) || token.endsWith(current_delimiter))) {
                statement_state = getStateFromToken(token, statement_state);
                token = "";
                atom = new StringBuilder();
            }

        }// end for

        // Add Pending SQL
        if (state == STATE_SQL) {
            sqlStatement = trimWhitespace(sqlStatement);
            if (!sqlStatement.isEmpty()) {
                ret.add(sqlStatement);
            }
        }

        return ret;
    }

    private int getStateFromToken(String token, int current_state) {
        int state = current_state;
        if (token != null) {
            if (token.compareToIgnoreCase("DELIMITER") == 0) {
                state = STATEMENT_SET_DELIMETER;
            } else {
                state = current_state;
            }
        }
        return state;
    }

    private String trimWhitespace(String sqlStatement) {
        while (sqlStatement.endsWith("\n") || sqlStatement.endsWith(" ")) {
            sqlStatement = sqlStatement.substring(0, sqlStatement.length() - 1);
        }
        return sqlStatement;
    }
}
