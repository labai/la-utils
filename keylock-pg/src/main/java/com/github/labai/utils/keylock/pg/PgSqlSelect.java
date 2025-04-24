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
