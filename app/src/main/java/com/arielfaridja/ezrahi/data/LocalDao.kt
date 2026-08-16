package com.arielfaridja.ezrahi.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.arielfaridja.ezrahi.entities.ActPermission
import com.arielfaridja.ezrahi.entities.ActUser

@Dao
interface LocalDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun addPermission(permission: ActPermission)

    @Query("DELETE from permissions")
    fun deleteAllPermissions()

    @Query("DELETE from Users")
    fun deleteAllUsers()

    @Query("SELECT * from permissions where id == :permission")
    fun getPermission(permission: Int): ActPermission?

    @Query("SELECT * from Users where id == :uId")
    fun getUser(uId: String): ActUser?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertUser(user: ActUser)

    @Update
    fun updatePermission(permission: ActPermission)

    @Update
    fun updateUser(u: ActUser)
}
