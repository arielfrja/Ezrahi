package com.arielfaridja.ezrahi.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.arielfaridja.ezrahi.entities.ActPermission;
import com.arielfaridja.ezrahi.entities.ActUser;

@Dao
public interface LocalDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void addPermission(ActPermission permission);

    @Query("DELETE from permissions")
    void deleteAllPermissions();

    @Query("DELETE from Users")
    void deleteAllUsers();

    @Query("SELECT * from permissions where id == :permission")
    ActPermission getPermission(int permission);

    @Query("SELECT * from Users where id == :uId")
    ActUser getUser(String uId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertUser(ActUser user);

    @Update
    void updatePermission(ActPermission permission);

    @Update
    void updateUser(ActUser u);
}
