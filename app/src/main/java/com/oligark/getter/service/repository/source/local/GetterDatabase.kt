package com.oligark.getter.service.repository.source.local

import android.arch.persistence.room.Database
import android.arch.persistence.room.Room
import android.arch.persistence.room.RoomDatabase
import android.content.Context
import com.oligark.getter.service.model.Business
import com.oligark.getter.service.model.Store
import com.oligark.getter.service.model.User

/**
 * Created by pmvb on 17-09-25.
 */
/**
 * Getter database class
 * Potential for Dependency Injection
 */
@Database(entities = arrayOf(
        User::class,
        Business::class,
        Store::class
), version = 1)
abstract class GetterDatabase : RoomDatabase() {
    companion object {
        @JvmStatic private var INSTANCE: GetterDatabase? = null
        @JvmStatic private val lock = Any()
        @JvmStatic
        fun getInstance(context: Context): GetterDatabase {
            synchronized(lock) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.applicationContext,
                            GetterDatabase::class.java,
                            "GetterDb.db"
                    ).build()
                }
                return INSTANCE!!
            }
        }
    }

    abstract fun storesDao(): StoresDao
}