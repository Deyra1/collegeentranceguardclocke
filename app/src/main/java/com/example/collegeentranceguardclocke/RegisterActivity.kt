package com.example.collegeentranceguardclocke

import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import com.example.collegeentranceguardclocke.databinding.ActivityRegisterBinding
import com.example.collegeentranceguardclocke.db.HistoryDao
import com.example.collegeentranceguardclocke.db.UserDao
import com.example.collegeentranceguardclocke.entity.History
import com.example.collegeentranceguardclocke.entity.Receive
import com.example.collegeentranceguardclocke.entity.User
import com.example.collegeentranceguardclocke.utils.Common
import com.example.collegeentranceguardclocke.utils.Common.registryFlag
import com.example.collegeentranceguardclocke.utils.MToast
import com.gyf.immersionbar.ImmersionBar
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.util.Objects

class RegisterActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRegisterBinding
    private var isReceive = false
    private lateinit var dao: UserDao
    private lateinit var hdao: HistoryDao
    private lateinit var sharedPreferences: SharedPreferences // 临时存储
    private lateinit var editor: SharedPreferences.Editor // 修改提交
    private var time = "1"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)
        sharedPreferences = getSharedPreferences("local", MODE_PRIVATE)
        editor = sharedPreferences.edit()
        dao = UserDao(this)
        hdao = HistoryDao(this)
        initViews()
        EventBus.getDefault().register(this)
    }

    private fun initViews() {
        registryFlag = false
        setSupportActionBar(binding.toolbar)
        binding.toolbarLayout.title = "注册用户"
        ImmersionBar.with(this).init()
        Objects.requireNonNull(supportActionBar)?.setDisplayHomeAsUpEnabled(true) //添加默认的返回图标
        supportActionBar!!.setHomeButtonEnabled(true) //设置返回键可用
        time = sharedPreferences.getString("openTime", "1").toString()
        binding.registerBtn.setOnClickListener { verifyData() }
    }

    /**
     * 解析数据
     * @param data
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun receiveDataFormat(data: Receive) {
        try {
            if (data.fid != null && data.fid != "0" || data.frfid != null && data.frfid != "0" || data.did != null) {
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


    /***
     * 数据验证
     */
    private fun verifyData() {

        val account = binding.inputAccountEdit.text.toString()
        val name = binding.inputNameEdit.text.toString()
        val password = binding.inputPasswordEdit.text.toString()
        val sex = binding.sexSpinner.selectedItem.toString()
        val role = when (binding.roleSpinner.selectedItem.toString()) {
            "管理员" -> 1
            "自动化学院" -> 2
            "通信学院" -> 3
            "人工智能学院" -> 4
            "传媒学院" -> 5
            else -> 2
        }
        if (account.isEmpty()) {
            MToast.mToast(this, "账号不能为空")
            return
        }
        if (!isValidInput(account)) {
            MToast.mToast(this, "账号格式错误")
            return
        }

        if (name.isEmpty()) {
            MToast.mToast(this, "用户名不能为空")
            return
        }
        if (password.isEmpty()) {
            MToast.mToast(this, "密码不能为空")
            return
        }

        val objects: List<Any>? = dao.query(account, "account")
        if (objects!!.isNotEmpty()) {
            MToast.mToast(this, "已有该用户，请直接登录")
            return
        }
        val user = User(
            account = account,
            name = name,
            password = password,
            state = 0,
            sex = sex,
            per = role,
            pwd = Common.randomCipher().toInt()
        )

        dao.insert(user)
        MToast.mToast(this, "添加成功")

    }

    /**
     * @brief 匹配正则 只能输入数字
     */
    private fun isValidInput(input: String): Boolean {
        val regex = "^[1-9][0-9]*$".toRegex()   // 创建正则表达式对象
        return regex.matches(input)  // 检查输入是否匹配正则表达式
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        finish()
        return super.onOptionsItemSelected(item)
    }

    override fun onDestroy() {
        super.onDestroy()
        EventBus.getDefault().unregister(this)
    }


}