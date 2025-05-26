package com.example.collegeentranceguardclocke.db

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import com.example.collegeentranceguardclocke.entity.History
import com.example.collegeentranceguardclocke.utils.MToast
import com.example.collegeentranceguardclocke.utils.TimeCycle

class HistoryDao(private val context: Context) : BaseDao {
    private var helper = DBOpenHelper(context)
    private lateinit var db: SQLiteDatabase
    private val TAG = "HistoryDao"
    override fun insert(bean: Any): Int {
        return try {
            val box = bean as History
            db = helper.writableDatabase
            val values = ContentValues()
            values.put("uid", box.uid)
            values.put("state", box.state)
            values.put("createDateTime", TimeCycle.getDateTime())
            values.put("method", box.method)
            db.insert("history", null, values)
            db.close()
            1
        } catch (e: Exception) {
            e.printStackTrace()
            MToast.mToast(context, "添加失败")
            -1
        }
    }

    override fun update(bean: Any, vararg data: String): Int {
        return try {
            db = helper.writableDatabase
            val values = ContentValues()
            val box = bean as History
            values.put("uid", box.uid)
            values.put("state", box.state)
            values.put("createDateTime", box.createDateTime)
            db.update("history", values, "hid=?", arrayOf(data[0]))
            db.close()
            1
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            MToast.mToast(context, "修改失败")
            -1
        }
    }

    override fun query(vararg data: String): MutableList<Any>? {
        return try {
            val result: MutableList<Any> = ArrayList()
            val cursor: Cursor
            val sql: String
            db = helper.readableDatabase
            when (data.size) {
                1 -> {
                    sql = "SELECT * from history where hid = ? order by createDateTime desc"
                    cursor = db.rawQuery(sql, arrayOf(data[0]))
                }

                2 -> {
                    sql =
                        if (data[1] == "uid") "SELECT * from history where uid = ? order by createDateTime desc" else
                            "SELECT * from history where state = ? order by createDateTime desc"
                    cursor = db.rawQuery(sql, arrayOf(data[0]))
                }

                3 -> {
                    sql =
                        "SELECT * from history where uid = ?  and state = ? order by createDateTime desc"
                    cursor = db.rawQuery(sql, arrayOf(data[0], data[1]))
                }

                else -> {
                    sql = "select * from history order by createDateTime desc"
                    cursor = db.rawQuery(sql, null)
                }
            }
            while (cursor.moveToNext()) {
                val box = History(
                    cursor.getInt(cursor.getColumnIndexOrThrow("hid")),
                    cursor.getInt(cursor.getColumnIndexOrThrow("uid")),
                    cursor.getInt(cursor.getColumnIndexOrThrow("state")),
                    cursor.getString(cursor.getColumnIndexOrThrow("createDateTime")),
                    cursor.getString(cursor.getColumnIndexOrThrow("method"))
                )
                result.add(box)
            }
            cursor.close()
            db.close()
            result
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            Log.e("查询", e.toString())
            MToast.mToast(context, "查询失败")
            null
        }
    }

    override fun delete(vararg data: String): Int {
        return try {
            if (data.isEmpty()) {
                return 0
            }
            db = helper.writableDatabase
            db.delete("history", "hid=?", arrayOf(data[0]))
            db.close()
            1
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            MToast.mToast(context, "删除失败")
            -1
        }
    }
}