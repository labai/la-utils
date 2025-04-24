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
package com.github.labai.utils.keylock

import org.slf4j.LoggerFactory
import java.sql.Connection
import javax.sql.DataSource

/**
 * @author Augustus
 *         created on 2021.02.17
 *
 * KeyLockManager
 *   - runLocked()
 *      throws LockedResourceException() if resource is locked
 *
 * Tries to reuse one db connection for several locks.
 * Takes a connection on first lock and close connection when no active lock exists.
 *
 */

private val logger = LoggerFactory.getLogger(KeyLockManager::class.java)

interface IKeyLockProvider {
    fun tryLock(sysId: Int, keyId: Int): Boolean
    fun unlock(sysId: Int, keyId: Int)
}

interface KeyLockConnProvider : IKeyLockProvider {
    // will be called on first lock (or after LastLock) - init connection
    fun onFirstLock()

    // will be called on last lock close (no more left) - free connection
    fun onLastUnlock()
}

interface IKeyLockDaoProvider {
    fun tryLock(conn: Connection, sysId: Int, keyId: Int): Boolean
    fun unlock(conn: Connection, sysId: Int, keyId: Int)
    fun unlockAll(conn: Connection)
}

class LockedResourceException(message: String) : RuntimeException(message)

class KeyLockConnProviderDb(
    private val dataSource: DataSource,
    private val dao: IKeyLockDaoProvider
) : KeyLockConnProvider {

    @Volatile private var initialized = false
    private var connection: Connection? = null

    override fun onFirstLock() {
        synchronized(this) {
            if (!initialized) {
                connection = connection ?: dataSource.connection
                initialized = true
            }
        }
    }

    override fun onLastUnlock() {
        synchronized(this) {
            try {
                connection?.let {
                    dao.unlockAll(it) // // just in case
                    it.close()
                }
            } catch (e: Exception) {
                logger.warn("Cannot close DB connection", e)
            } finally {
                connection = null
                initialized = false
            }
        }
    }

    override fun tryLock(sysId: Int, keyId: Int): Boolean {
        synchronized(this) {
            checkNotNull(connection) { "Call onFirstLock() before using tryLock()" }
            return dao.tryLock(connection!!, sysId, keyId)
        }
    }

    override fun unlock(sysId: Int, keyId: Int) {
        synchronized(this) {
            checkNotNull(connection) { "Call onFirstLock() before using unlock()" }
            dao.unlock(connection!!, sysId, keyId)
        }
    }
}

class KeyLockManager(
    private val lockProvider: KeyLockConnProvider
) {
    private val ourLocks = mutableSetOf<Pair<Int, Int>>()

    fun <T> runLocked(sysId: Int, keyId: Int, taskFn: () -> T): T {
        var wasLockedDb = false
        val pair = Pair(sysId, keyId)
        synchronized(this) {
            if (!ourLocks.add(pair)) {
                throw LockedResourceException("Lock already acquired for $sysId-$keyId")
            }
            if (ourLocks.size == 1) {
                lockProvider.onFirstLock()
            }
        }

        try {
            if (!lockProvider.tryLock(sysId, keyId))
                throw LockedResourceException("Can't acquire task lock for $sysId-$keyId (pg)")
            wasLockedDb = true
            return taskFn()
        } finally {
            if (wasLockedDb) {
                try {
                    lockProvider.unlock(sysId, keyId)
                } catch (e: Throwable) {
                    logger.error("Failed to release DB lock for $keyId-$sysId", e)
                }
            }
            synchronized(this) {
                ourLocks.remove(pair)
                if (ourLocks.isEmpty()) {
                    lockProvider.onLastUnlock() // release connection
                }
            }
        }
    }
}
