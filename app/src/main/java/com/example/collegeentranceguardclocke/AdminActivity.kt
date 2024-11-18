package com.example.collegeentranceguardclocke

import android.content.Intent
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import com.example.collegeentranceguardclocke.databinding.ActivityAdminBinding
import com.example.collegeentranceguardclocke.db.HistoryDao
import com.example.collegeentranceguardclocke.db.UserDao
import com.example.collegeentranceguardclocke.entity.History
import com.example.collegeentranceguardclocke.entity.Receive
import com.example.collegeentranceguardclocke.entity.User
import com.example.collegeentranceguardclocke.utils.Common
import com.example.collegeentranceguardclocke.utils.Common.registryFlag
import com.example.collegeentranceguardclocke.utils.CustomBottomSheetDialogFragment
import com.example.collegeentranceguardclocke.utils.MToast
import com.gyf.immersionbar.ImmersionBar
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class AdminActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAdminBinding
    private val titles = arrayOf(
        "用户管理",
        "添加用户",
        "设置开门时长",
        "退出登录"
    )
    private lateinit var dao: UserDao
    private lateinit var hdao: HistoryDao
    private lateinit var sharedPreferences: SharedPreferences // 临时存储
    private lateinit var editor: SharedPreferences.Editor // 修改提交
    private var time = "1"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminBinding.inflate(layoutInflater)
        setContentView(binding.root)
        sharedPreferences = getSharedPreferences("local", MODE_PRIVATE)
        editor = sharedPreferences.edit()
        dao = UserDao(this)
        hdao = HistoryDao(this)
        initViews()
    }


    private fun initViews() {
        registryFlag = false
        ImmersionBar.with(this).init()
        time = sharedPreferences.getString("openTime", "1").toString()
        binding.adminListView.adapter =
            ArrayAdapter(this, android.R.layout.simple_list_item_1, titles)
        binding.adminListView.setOnItemClickListener { _, _, i, _ ->
            when (i) {
                0 -> {
                    startActivity(Intent(this,UserDetailActivity::class.java))
                }

                1 -> { //添加用户
                    startActivity(Intent(this, RegisterActivity::class.java))
                }

                2 -> {
                    val builder = AlertDialog.Builder(this)
                    val v = View.inflate(
                        this,
                        R.layout.update_open_time_dialog,
                        null
                    )
                    val inputText =
                        v.findViewById<EditText>(R.id.inputTime)
                    val submitBtn =
                        v.findViewById<Button>(R.id.submitBtn)
                    builder.setView(v)
                    val alertDialog = builder.create()
                    submitBtn.setOnClickListener {
                        val input = inputText.text.toString()
                        if ((input.matches(Regex("\\d+")) && input.toInt() > 0 && input.toInt() < 100)) {
                            editor.putString("openTime", input)
                            editor.commit()

                            MToast.mToast(this, "修改成功")
                            time = input
                            Common.sendMessage(this, 7, time)
                            alertDialog.dismiss()
                        } else {
                            MToast.mToast(this, "输入的时间不符合要求")
                        }
                    }
                    alertDialog.show()
                }

                3 -> {
                    //退出登录
                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()
                }
            }
        }
    }

    /**
     * 解析数据
     * @param data
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun receiveDataFormat(data: Receive) {
        try {
            if (data.fid != null && data.fid != "0" || data.frfid != null && data.frfid != "0") {
                registryFlag = true
                Common.receive = data
                return
            }
            Common.receive = null
            val list = dao.query()!!
            if (data.face_id != null && data.face_id != "0") {
                for (d in list) {
                    val da = d as User
                    if (da.fid.toString() == data.face_id) {
                        Common.sendMessage(this, 3, "1", time)
                        val h = History()
                        h.uid = da.uid
                        h.state = if (da.state == 0) 1 else 0 //记录开门类型
                        da.state = if (da.state == 0) 1 else 0 // 修改在校状态
                        dao.update(da,da.uid.toString())
                        hdao.insert(h)
                        break
                    }
                }
            }
            if (data.rfid != null && data.rfid != "0") {
                for (d in list) {
                    val da = d as User
                    if (da.rid.toString() == data.rfid) {
                        Common.sendMessage(this, 3, "1", time)
                        val h = History()
                        h.uid = da.uid
                        h.state = if (da.state == 0) 1 else 0 //记录开门类型
                        da.state = if (da.state == 0) 1 else 0 // 修改在校状态
                        dao.update(da,da.uid.toString())
                        hdao.insert(h)
                        break
                    }
                }
            }
            if (data.pwd != null) {
                for (d in list) {
                    val da = d as User
                    if (da.pwd.toString() == data.pwd) {
                        Common.sendMessage(this, 3, "1", time)
                        val h = History()
                        h.uid = da.uid
                        h.state = if (da.state == 0) 1 else 0 //记录开门类型
                        da.state = if (da.state == 0) 1 else 0 // 修改在校状态
                        dao.update(da,da.uid.toString())
                        hdao.insert(h)
                        break
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("数据解析", e.message.toString())
            MToast.mToast(this, "数据解析失败")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        EventBus.getDefault().unregister(this)
    }
}