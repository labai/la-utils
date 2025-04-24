/*
The MIT License (MIT)

Copyright (c) 2021 Augustus

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
*/
package com.github.labai.utils.keylock.pg;

import kotlin.Pair;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/*
 * @author Augustus
 * created on 2023-11-01
 *
 * for internal keylock usage!
 */
class PgSqlSelect {
    private static final Logger logger = LoggerFactory.getLogger(PgSqlSelect.class);

    private interface JdbcExecuteFunction<R> {
        R apply(PreparedStatement stmt) throws SQLException;
    }

    @Nullable
    static Long selectAsLongWithParam(Connection connection, String sql, Pair<Integer, Integer> intParams) throws SQLException {
        return executeJdbc(connection, sql, stmt -> {
            Long result = null;
            if (intParams != null) {
                stmt.setInt(1, intParams.getFirst());
                stmt.setInt(2, intParams.getSecond());
            }
            ResultSet resultSet = stmt.executeQuery();
            if (resultSet != null) {
                while (resultSet.next()) {
                    Object x = resultSet.getObject(1);
                    if (x instanceof Number)
                        result = ((Number) x).longValue();
                }
                resultSet.close();
            }
            return result;
        });
    }

    private static <R> R executeJdbc(Connection connection, String sql, JdbcExecuteFunction<R> jdbcExecuteFn) throws SQLException {
        PreparedStatement stmt = null;
        try {
            stmt = connection.prepareStatement(sql);
            return jdbcExecuteFn.apply(stmt);
        } finally {
            try {
                if (stmt != null) stmt.close();
            } catch (Exception e) {
                logger.error("Cannot close statement", e);
            }
        }
    }
}
