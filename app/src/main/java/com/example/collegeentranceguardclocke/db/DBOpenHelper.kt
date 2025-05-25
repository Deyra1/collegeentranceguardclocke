package com.example.collegeentranceguardclocke.db

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.collegeentranceguardclocke.utils.TimeCycle

class DBOpenHelper(context: Context) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {
    companion object {
        const val DB_NAME = "iacs.db"
        const val DB_VERSION = 1
    }

    // 创建数据库时触发
    override fun onCreate(p0: SQLiteDatabase?) {
        var sql = "create table `history` (" +
                "`hid` INTEGER primary key autoincrement," +
                "`uid` INTEGER," +
                "`state` INTEGER," + // 1为进宿舍 0为出宿舍
                "`createDateTime` VARCHAR(255))"
        p0?.execSQL(sql) //执行sql语句，
        sql = "create table `user` (" +
                "`uid` INTEGER primary key autoincrement," +
                "`account` VARCHAR(20)," + //账号
                "`password` VARCHAR(20)," + //密码
                "`name` VARCHAR(20)," + //姓名
                "`per` INTEGER," + //职位/学院 1 管理员 2 自动化 3 通信 4 人工智能 5 传媒
                "`pwd` INTEGER," + // 开门密码
                "`state` INTEGER," + // 在寝室状态 1为在寝室 0为离开寝室
                "`sex` VARCHAR(20)," + // 性别
                "`rid` VARCHAR(255)," + // RFID 的id
                "`fid` INTEGER," + // 人脸id
                "`createDateTime` VARCHAR(255))"
        p0?.execSQL(sql) //执行sql语句，
        var values = ContentValues()
        values.put("account", "admin")
        values.put("name", "admin")
        values.put("password", "123456")
        values.put("per", 1)
        values.put("fid", -1)
        values.put("pwd", -1)
        values.put("createDateTime", TimeCycle.getDateTime())
    }

    // 更新数据库时触发
    override fun onUpgrade(p0: SQLiteDatabase?, p1: Int, p2: Int) {

    }
}