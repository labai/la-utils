package com.github.labai.utils.keylock

import com.zaxxer.hikari.HikariDataSource
import java.io.FileNotFoundException
import java.io.IOException
import java.util.*
import javax.sql.DataSource
import kotlin.system.exitProcess

/**
 * @author Augustus
 * created on 2018.12.27
 */
internal object TestDbConfig {

    internal fun getDataSourceEnvValue(name: String): String {
        val properties = readPropertiesFile("ted-keylock-pg.properties")
        val prefix = "datasource"
        return properties.getProperty("$prefix.$name")
    }

    internal fun createDataSource(jdbcUrl: String): DataSource {
        val dataSource: HikariDataSource
        try {
            val properties = readPropertiesFile("ted-keylock-pg.properties")
            val prefix = "datasource"
            val driver = properties.getProperty("$prefix.driverClassName")
            val user = properties.getProperty("$prefix.username")
            val password = properties.getProperty("$prefix.password")

            Class.forName(driver)

            dataSource = HikariDataSource()
            dataSource.jdbcUrl = jdbcUrl
            dataSource.username = user
            dataSource.password = password

        } catch (e: IOException) {
            e.printStackTrace()
            exitProcess(1)
        } catch (ex: ClassNotFoundException) {
            println("Error: unable to load jdbc driver class!")
            exitProcess(1)
        }
        return dataSource
    }


    @Throws(IOException::class)
    private fun readPropertiesFile(propFileName: String): Properties {
        val properties = Properties()
        val inputStream = TestDbConfig::class.java.classLoader.getResourceAsStream(propFileName) ?: throw FileNotFoundException("property file '$propFileName' not found in the classpath")
        properties.load(inputStream)
        return properties
    }
}
