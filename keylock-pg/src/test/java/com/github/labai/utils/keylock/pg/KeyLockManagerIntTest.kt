package com.github.labai.utils.keylock.pg

import com.github.labai.utils.keylock.KeyLockConnProvider
import com.github.labai.utils.keylock.KeyLockConnProviderDb
import com.github.labai.utils.keylock.KeyLockManager
import com.github.labai.utils.keylock.LockedResourceException
import com.github.labai.utils.keylock.TestDbConfig
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import org.junit.jupiter.api.fail
import org.slf4j.LoggerFactory
import org.testcontainers.containers.PostgreSQLContainer
import java.lang.Thread.sleep
import javax.sql.DataSource

/**
 * @author Augustus
 *         created on 2021.02.17
 *
 *  tests with db
 *
 */
@Disabled // requires docker
@TestInstance(PER_CLASS)
class KeyLockManagerIntTest {

    private lateinit var postgresContainer: PostgreSQLContainer<*>
    private lateinit var dataSource: DataSource
    private lateinit var lockProvider: KeyLockConnProviderWrap
    private lateinit var lockMgr: KeyLockManager

    @BeforeAll
    fun init() {
        postgresContainer = PostgreSQLContainer<Nothing>("postgres:15.4").apply {
            withDatabaseName("testdb")
            withUsername(TestDbConfig.getDataSourceEnvValue("username"))
            withPassword(TestDbConfig.getDataSourceEnvValue("password"))
            start()
        }

        dataSource = TestDbConfig.createDataSource(postgresContainer.jdbcUrl)

        lockProvider = KeyLockConnProviderWrap(KeyLockConnProviderDb(dataSource, KeyLockManagerPgDao()))
        lockMgr = KeyLockManager(lockProvider)
        // warmup
        lockMgr.runLocked(-999, 101) { }
        lockProvider.clearCounts()
    }

    @Test
    fun test_simple_lock() {
        val dao = KeyLockManagerPgDao()
        val provider = KeyLockConnProviderDb(dataSource, dao)
        val manager = KeyLockManager(provider)

        val result = manager.runLocked(1, 100) {
            "locked and done"
        }

        assertEquals("locked and done", result)
    }

    @Test
    internal fun test_one_connection_is_used(): Unit = runBlocking {
        lockProvider.clearCounts()

        val job = GlobalScope.launch {
            lockMgr.runLocked(-999, 101) {
                sleep(100)
            }
        }
        lockMgr.runLocked(-999, 102) {
            sleep(10)
        }

        lockProvider.assertCounts(
            onFirstLockCount = 1,
            onLastLockCount = 0,
            tryLockCount = 2,
            unlockCount = 1,
        )

        job.join()

        lockProvider.assertCounts(
            onFirstLockCount = 1,
            onLastLockCount = 1,
            tryLockCount = 2,
            unlockCount = 2,
        )

    }

    @Test
    internal fun test_lock_exception(): Unit = runBlocking {

        val job = GlobalScope.launch {
            lockMgr.runLocked(-999, 101) {
                sleep(100)
            }
        }
        sleep(10) // wait to be sure locked in coroutine
        try {
            lockMgr.runLocked(-999, 101) { }
            fail("expected LockedResourceException")
        } catch (e: LockedResourceException) {
            // ok
        } catch (e: Exception) {
            fail("expected LockedResourceException")
        }

        job.join()
    }

    @Test
    internal fun test_few_instances(): Unit = runBlocking {

        val lockProvider2 = KeyLockConnProviderWrap(KeyLockConnProviderDb(dataSource, KeyLockManagerPgDao()))
        val lockMgr2 = KeyLockManager(lockProvider2)

        val job = GlobalScope.launch {
            lockMgr.runLocked(-999, 101) {
                sleep(100)
            }
        }
        sleep(10) // wait to be sure locked in coroutine
        try {
            lockMgr2.runLocked(-999, 101) { }
            fail("expected LockedResourceException")
        } catch (e: LockedResourceException) {
            // ok
        } catch (e: Exception) {
            fail("expected LockedResourceException")
        }

        job.join()

    }


    private class KeyLockConnProviderWrap(private val delegate: KeyLockConnProvider) : KeyLockConnProvider {
        val verbose: Boolean = true
        var onFirstLockCount = 0
        var onLastLockCount = 0
        var tryLockCount = 0
        var unlockCount = 0

        override fun onFirstLock() {
            if (verbose)
                logger.info("onFirstLock")
            onFirstLockCount++
            delegate.onFirstLock()
        }

        override fun onLastUnlock() {
            if (verbose)
                logger.info("onLastUnlock")
            onLastLockCount++
            delegate.onLastUnlock()
        }

        override fun tryLock(sysId: Int, keyId: Int): Boolean {
            if (verbose)
                logger.info("tryLock")
            tryLockCount++
            return delegate.tryLock(sysId, keyId)
        }

        override fun unlock(sysId: Int, keyId: Int) {
            if (verbose)
                logger.info("unlock")
            unlockCount++
            return delegate.unlock(sysId, keyId)
        }

        fun assertCounts(
            onFirstLockCount: Int,
            onLastLockCount: Int,
            tryLockCount: Int,
            unlockCount: Int,
        ) {
            assertEquals(onFirstLockCount, this.onFirstLockCount)
            assertEquals(onLastLockCount, this.onLastLockCount)
            assertEquals(tryLockCount, this.tryLockCount)
            assertEquals(unlockCount, this.unlockCount)
        }

        fun clearCounts() {
            onFirstLockCount = 0
            onLastLockCount = 0
            tryLockCount = 0
            unlockCount = 0
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(KeyLockManagerIntTest::class.java)
    }
}
