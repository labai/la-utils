package com.github.labai.utils.keylock

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import org.slf4j.LoggerFactory
import java.lang.Thread.sleep
import kotlin.test.assertEquals

/**
 * @author Augustus
 *         created on 2021.02.17
 */
class KeyLockManagerTest {
    private val logger = LoggerFactory.getLogger(KeyLockManagerTest::class.java)

    @Test
    internal fun test_one_connection_is_used(): Unit = runBlocking {
        val lockProvider = lockProvider()
        val lockMgr = KeyLockManager(lockProvider)

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
        val lockProvider = lockProvider(false)
        val lockMgr = KeyLockManager(lockProvider)

        val job = GlobalScope.launch {
            lockMgr.runLocked(-999, 101) {
                sleep(50)
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

    private fun lockProvider(verbose: Boolean = true) = object : KeyLockConnProvider {
        var onFirstLockCount = 0
        var onLastLockCount = 0
        var tryLockCount = 0
        var unlockCount = 0

        override fun onFirstLock() {
            if (verbose)
                logger.info("onFirstLock")
            onFirstLockCount++
        }

        override fun onLastUnlock() {
            if (verbose)
                logger.info("onLastLock")
            onLastLockCount++
        }

        override fun tryLock(sysId: Int, keyId: Int): Boolean {
            if (verbose)
                logger.info("tryLock")
            tryLockCount++
            return true
        }

        override fun unlock(sysId: Int, keyId: Int) {
            if (verbose)
                logger.info("unlock")
            unlockCount++
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
}
