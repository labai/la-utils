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
package com.github.labai.utils.keylock.pg

import org.intellij.lang.annotations.Language
import org.slf4j.LoggerFactory
import com.github.labai.utils.keylock.IKeyLockDaoProvider
import java.sql.Connection
import java.sql.SQLException

/**
 * @author Augustus
 *         created on 2021.02.09
 *
 * use advisory lock in PostgreSql mechanism
 */
class KeyLockManagerPgDao : IKeyLockDaoProvider {
    private val logger = LoggerFactory.getLogger(javaClass)

    // transaction level lock - will release lock at end of transaction
    fun advisoryTxLockTask(connection: Connection, typeId: Int, lockKeyId: Int): Boolean {
        @Language("PostgreSQL")
        val sql = "select case when pg_try_advisory_xact_lock(?, ?) then 1 else 0 end as longVal"
        val res: Long?
        try {
            res = PgSqlSelect.selectAsLongWithParam(connection, sql, Pair(typeId, lockKeyId))
        } catch (e: SQLException) {
            logger.info("advisory tx lock {} SQLException: {} {}", lockKeyId, e.errorCode, e.message)
            return false
        }
        return (res ?: 0) > 0
    }

    // session level lock - requires to unlock (end on end of session (connection))
    private fun advisorySessionLock(connection: Connection, typeId: Int, lockKeyId: Int): Boolean {
        @Language("PostgreSQL")
        val sql = "select case when pg_try_advisory_lock(?, ?) then 1 else 0 end as longVal"
        val res: Long?
        try {
            res = PgSqlSelect.selectAsLongWithParam(connection, sql, Pair(typeId, lockKeyId))
        } catch (e: SQLException) {
            logger.info("advisory session lock {} SQLException: {} {}", lockKeyId, e.errorCode, e.message)
            return false
        }
        return (res ?: 0) > 0
    }

    private fun advisorySessionUnlock(connection: Connection, typeId: Int, lockKeyId: Int): Boolean {
        @Language("PostgreSQL")
        val sql = "select case when pg_advisory_unlock(?, ?) then 1 else 0 end as longVal"
        val res: Long?
        try {
            res = PgSqlSelect.selectAsLongWithParam(connection, sql, Pair(typeId, lockKeyId))
        } catch (e: SQLException) {
            logger.info("advisory session unlock {} SQLException: {} {}", lockKeyId, e.errorCode, e.message)
            return false
        }
        return (res ?: 0) > 0
    }

    private fun advisorySessionUnlockAll(connection: Connection) {
        @Language("PostgreSQL")
        val sql = "select pg_advisory_unlock_all()"
        try {
            PgSqlSelect.selectAsLongWithParam(connection, sql, null)
        } catch (e: SQLException) {
            logger.info("advisory session unlockAll {} SQLException: {} {}", "all", e.errorCode, e.message)
            return
        }
        return
    }
    override fun tryLock(conn: Connection, sysId: Int, keyId: Int): Boolean {
        return advisorySessionLock(conn, sysId, keyId)
    }

    override fun unlock(conn: Connection, sysId: Int, keyId: Int) {
        advisorySessionUnlock(conn, sysId, keyId)
    }

    override fun unlockAll(conn: Connection) {
        advisorySessionUnlockAll(conn)
    }
}
