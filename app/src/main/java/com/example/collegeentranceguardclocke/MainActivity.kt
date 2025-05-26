package com.example.collegeentranceguardclocke

import android.content.Intent
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import com.blankj.utilcode.util.LogUtils
import com.example.collegeentranceguardclocke.databinding.ActivityMainBinding
import com.example.collegeentranceguardclocke.databinding.UpdateUserDetailBinding
import com.example.collegeentranceguardclocke.db.HistoryDao
import com.example.collegeentranceguardclocke.db.UserDao
import com.example.collegeentranceguardclocke.entity.History
import com.example.collegeentranceguardclocke.entity.Receive
import com.example.collegeentranceguardclocke.entity.User
import com.example.collegeentranceguardclocke.utils.Common
import com.example.collegeentranceguardclocke.utils.Common.randomCipher
import com.example.collegeentranceguardclocke.utils.Common.receive
import com.example.collegeentranceguardclocke.utils.Common.registryFlag
import com.example.collegeentranceguardclocke.utils.CustomBottomSheetDialogFragment
import com.example.collegeentranceguardclocke.utils.CustomDialog
import com.example.collegeentranceguardclocke.utils.MToast
import com.example.collegeentranceguardclocke.utils.TimeCycle
import com.google.gson.Gson
import com.gyf.immersionbar.ImmersionBar
import com.itfitness.mqttlibrary.MQTTHelper
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.MqttCallback
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var isDebugView = false //是否显示debug界面
    private val arrayList = mutableListOf<String>() // debug消息数据
    private var adapter: ArrayAdapter<*>? = null // debug消息适配器
    private var isAgain = false // 是否重新注册
    private lateinit var dao: UserDao
    private lateinit var hdao: HistoryDao
    private lateinit var sharedPreferences: SharedPreferences // 临时存储
    private lateinit var editor: SharedPreferences.Editor // 修改提交
    private var isADDFlag = false // 是否进入注册状态
    private var time = "1"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        sharedPreferences = getSharedPreferences("local", MODE_PRIVATE)
        editor = sharedPreferences.edit()
        dao = UserDao(this)
        hdao = HistoryDao(this)
        initViews()
        mqttConfig() // Call mqttConfig here
        EventBus.getDefault().register(this)
    }

    /****
     * @brief 配置mqtt连接参数并进行连接
     */
    private fun mqttConfig() {
        if (Common.mqttHelper == null) {
            // 配置mqtt参数
            Common.mqttHelper = MQTTHelper(
                this,
                Common.URL,
                Common.DRIVER_ID,
                Common.DRIVER_NAME,
                Common.DRIVER_PASSWORD,
                true,
                30,
                30
            )
            try {
                // 尝试连接mqtt服务器
                Common.mqttHelper!!.connect(Common.RECEIVE_TOPIC, 1, true, object : MqttCallback {
                    override fun connectionLost(cause: Throwable?) {
                        // 连接中断或丢失时触发
                    }

                    //接受到消息时触发
                    override fun messageArrived(topic: String?, message: MqttMessage?) {
                        LogUtils.eTag(
                            "接收到消息-未解码",
                            if (message!!.payload != null) String(message.payload) else ""
                        )

                        val receive = message.toString()
                        //数据转换
                        val data: Receive = Gson().fromJson(receive, Receive::class.java)
                        LogUtils.eTag(
                            "接收到消息-解码", if (message.payload != null) data else ""
                        )
                        EventBus.getDefault().post(data) // Post event here
                    }

                    // 消息发送完成时触发
                    override fun deliveryComplete(token: IMqttDeliveryToken?) {

                    }

                })
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("mqttConfig()", e.message.toString())
                MToast.mToast(this, "连接时发生错误")
            }
        }
    }

    private fun initViews() {
        registryFlag = false
        setSupportActionBar(binding.toolbar)
        binding.toolbarLayout.title = title
        ImmersionBar.with(this).init()
        time = sharedPreferences.getString("openTime", "1").toString()
        debugView()
        eventManager()
        if (Common.user?.fid != -1) {
            binding.faceIDText.text = Common.user?.fid.toString()
        } else {
            binding.faceIDText.text = "未注册"
        }

        if (Common.user?.pwd != 0 && Common.user?.pwd != null) {
            binding.openPassword.text = Common.user?.pwd.toString()
        } else {
            val pwd = randomCipher()
            binding.openPassword.text = pwd
            Common.user?.pwd = pwd.toInt()
            dao.update(Common.user!!, Common.user?.uid.toString())
        }

        binding.RFIDText.text =
            if (Common.user?.rid != null && Common.user?.rid != "") Common.user?.rid else "未注册"

        binding.openPassword.text = Common.user?.pwd.toString()
        binding.nameText.text = Common.user?.name.toString()
        val currentUserPer = Common.user?.per
        val roleDescription = when (currentUserPer) {
            1 -> "管理员"
            2 -> "自动化学院"
            3 -> "通信学院"
            4 -> "人工智能学院"
            5 -> "传媒学院"
            else -> {
                if (Common.user == null) {
                    "用户未登录"
                } else {
                    "未知角色"
                }
            }
        }
        binding.roleText.text = roleDescription
    }


    /**
     * @brief 控件监听
     */
    private fun eventManager() {
        binding.openBtn.setOnClickListener {
            debugViewData(
                1,
                Common.sendMessage(this, 3, "1", time)
            )
            // Record history for remote unlock triggered by the current user
            val currentUser = Common.user
            if (currentUser != null) {
                val h = History()
                h.uid = currentUser.uid
                // Assuming remote unlock is recorded as '进宿舍' (state = 1)
                // You might need to adjust the state based on your requirements
                h.state = 1 // Example: record as 'in'
                h.method = "远程开锁" // Set unlock method to Remote Unlock
                hdao.insert(h)
                Log.d("HistoryRecord", "Remote unlock history inserted for user: ${currentUser.name}")
            }
        }

//        binding.openPassword.setOnClickListener {
//            MToast.mToast(this, "重置密码")
//        }
    }

    /**
     * @brief debug界面的初始化
     */
    private fun debugView() {
        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, arrayList)
        binding.debugView.adapter = adapter
    }

    /**
     * @param str  如果为 1 添加发送数据到界面   为 2 添加接受消息到界面
     * @param data 数据字符串
     * @brief debug界面数据添加
     */
    private fun debugViewData(str: Int, data: String) {
        if (arrayList.size >= 255) {
            arrayList.clear()
        }
        runOnUiThread {
            when (str) {
                1 -> arrayList.add("目标主题:${Common.RECEIVE_TOPIC} 时间:${TimeCycle.getDateTime()}发送消息:$data".trimIndent())
                2 -> arrayList.add("来自主题:${Common.RECEIVE_TOPIC} 时间:${TimeCycle.getDateTime()}接到消息:$data".trimIndent())
            }
            // 在添加新数据之后调用以下方法，滚动到列表底部
            binding.debugView.post { binding.debugView.setSelection(if (adapter != null) adapter!!.count - 1 else 0) }
            adapter?.notifyDataSetChanged()
        }
    }

    /**
     * 解析数据
     * @param data
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun receiveDataFormat(data: Receive) {
        debugViewData(2, data.toString())
        try {
            receive = data
            // Log the received data fields
            Log.d("ReceiveData", "Received data: face_id=${data.face_id}, rfid=${data.rfid}, pwd=${data.pwd}, door=${data.door}, door_time=${data.door_time}, fid=${data.fid}, frfid=${data.frfid}, did=${data.did}")

            if (isADDFlag) {
                isADDFlag = false
                if (data.fid != null && data.fid != "0" || data.frfid != null && data.frfid != "0" || data.did != null) {
                    registryFlag = true
                    // Log when registryFlag is set
                    Log.d("ReceiveData", "Registry flag set based on received data.")
                    return
                }
            }
            receive = null
            val list = dao.query()!!
            // Log the size of the user list for query
            Log.d("ReceiveData", "User list size for query: ${list.size}")

            if (data.face_id != null && data.face_id != "0") {
                Log.d("ReceiveData", "Processing face_id: ${data.face_id}")
                for (d in list) {
                    val da = d as User
                    if (da.fid.toString() == data.face_id) {
                        Log.d("ReceiveData", "User found by face_id: ${da.name}")
                        Common.sendMessage(this, 3, "1", time)
                        val h = History()
                        h.uid = da.uid
                        h.state = if (da.state == 0) 1 else 0 //记录开门类型
                        da.state = if (da.state == 0) 1 else 0 // 修改在宿舍状态
                        h.method = "人脸" // Set unlock method
                        dao.update(da, da.uid.toString())
                        hdao.insert(h)
                        Log.d("ReceiveData", "History inserted for face_id user: ${da.name}")
                        break
                    }
                }
            }
            if (data.rfid != null && data.rfid != "0") {
                Log.d("ReceiveData", "Processing rfid: ${data.rfid}")
                for (d in list) {
                    val da = d as User
                    if (da.rid == data.rfid) {
                        Log.d("ReceiveData", "User found by rfid: ${da.name}")
                        Common.sendMessage(this, 3, "1", time)
                        val h = History()
                        h.uid = da.uid
                        h.state = if (da.state == 0) 1 else 0 //记录开门类型
                        da.state = if (da.state == 0) 1 else 0 // 修改在宿舍状态
                        h.method = "RFID" // Set unlock method
                        dao.update(da, da.uid.toString())
                        hdao.insert(h)
                        Log.d("ReceiveData", "History inserted for rfid user: ${da.name}")
                        break
                    }
                }
            }

            if (data.door_time != null) {
                Log.d("ReceiveData", "Processing door_time: ${data.door_time}")
                if (time != data.door_time) {
                    time = data.door_time!!
                    editor.putString("openTime", data.door_time)
                    editor.commit()
                    Log.d("ReceiveData", "Door time updated to: ${time}")
                }
            }
            if (data.pwd != null) {
                Log.d("ReceiveData", "Processing pwd: ${data.pwd}")
                for (d in list) {
                    val da = d as User
                    if (da.pwd.toString() == data.pwd) {
                        Log.d("ReceiveData", "User found by pwd: ${da.name}")
                        Common.sendMessage(this, 3, "1", time)
                        val h = History()
                        h.uid = da.uid
                        h.state = if (da.state == 0) 1 else 0 //记录开门类型
                        da.state = if (da.state == 0) 1 else 0 // 修改在宿舍状态
                        h.method = "密码" // Set unlock method to Password
                        dao.update(da, da.uid.toString())
                        hdao.insert(h)
                        Log.d("ReceiveData", "History inserted for pwd user: ${da.name}")
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
     * @brief 注册人脸或者RF信息
     * @param type 0-注册 1-删除
     * @param code 0-人脸 1-RF
     */
    private fun recordFaceOrRF(type: Int, code: Int) {
        when (code) {
            0 -> {
                val dialog: CustomDialog
                when (type) {
                    0 -> { // 注册
                        if (Common.user?.fid != null && Common.user?.fid != -1) {
                            isAgain = true
                            recordFaceOrRF(1, code)
                            return
                        } else {
                            dialog = CustomDialog(this, "注册人脸")
                            debugViewData(
                                1,
                                Common.sendMessage(this, 1, "1")
                            )
                        }
                    }

                    else -> { // 删除
                        dialog = CustomDialog(this, "删除人脸")
                        debugViewData(
                            1,
                            Common.sendMessage(this, 4, Common.user?.fid.toString())
                        )
                    }
                }
                Thread {
                    try {
                        runOnUiThread {
                            dialog.show()
                        }
                        var time = 0
                        while (true) {
                            isADDFlag = true
                            if (time > 30) {
                                runOnUiThread {
                                    MToast.mToast(this, "超时")
                                    dialog.dismiss()
                                    isADDFlag = false
                                }
                                break
                            } else {
                                if (registryFlag) {
                                    registryFlag = false
                                    runOnUiThread {
                                        if (type == 0) {
                                            Common.user?.fid = receive?.fid?.toInt()
                                        } else {
                                            Common.user?.fid = -1
                                        }
                                        if (dao.update(
                                                Common.user!!,
                                                Common.user?.uid.toString()
                                            ) == -1
                                        ) {
                                            MToast.mToast(this, "已有该ID")
                                        }
                                        if (isAgain) {
                                            isAgain = false
                                            recordFaceOrRF(0, code)
                                        }
                                        if (Common.user?.fid != -1) {
                                            binding.faceIDText.text = Common.user?.fid.toString()
                                        } else {
                                            binding.faceIDText.text = "未注册"
                                        }
                                        dialog.dismiss()
                                    }
                                    isADDFlag = false
                                    break
                                }
                            }
                            time++
                            Thread.sleep(100)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }.start()
            }

            1 -> {
                val dialog: CustomDialog
                when (type) {
                    0 -> {
                        if (Common.user?.rid != null && Common.user?.rid != "") {
                            isAgain = true
                            recordFaceOrRF(1, code)
                            return
                        } else {
                            dialog = CustomDialog(this, "注册RF卡")
                            debugViewData(
                                1,
                                Common.sendMessage(this, 2, "1")
                            )
                        }
                    }

                    else -> {
                        Common.user?.rid = ""
                        binding.RFIDText.text = "未注册"
                        dao.update(Common.user!!, Common.user?.uid.toString())
                        if (isAgain) {
                            isAgain = false
                            recordFaceOrRF(0, code)
                        }
                        return
                    }
                }
                Thread {
                    try {
                        runOnUiThread {
                            dialog.show()
                        }
                        var time = 0
                        while (true) {
                            isADDFlag = true
                            if (time > 30) {
                                runOnUiThread {
                                    MToast.mToast(this, "超时")
                                    dialog.dismiss()
                                    isADDFlag = false
                                }
                                break
                            } else {
                                if (registryFlag) {
                                    runOnUiThread {
                                        registryFlag = false
                                        if (dao.query(
                                                receive?.frfid.toString(),
                                                "rid"
                                            )?.size!! > 0
                                        ) {
                                            MToast.mToast(this, "已有该ID")
                                        } else {
                                            Common.user?.rid = receive?.frfid
                                            if (dao.update(
                                                    Common.user!!,
                                                    Common.user?.uid.toString()
                                                ) == -1
                                            ) {
                                                MToast.mToast(this, "修改失败")
                                            }
                                            runOnUiThread {
                                                binding.RFIDText.text =
                                                    if (Common.user?.rid != null && Common.user?.rid != "") Common.user?.rid else "未注册"
                                            }
                                        }
                                        dialog.dismiss()
                                    }
                                    isADDFlag = false
                                    break
                                }
                            }
                            time++
                            Thread.sleep(100)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }.start()
            }
        }
    }

    //填充右上角目录
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_scrolling, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        when (id) {
            R.id.debugView -> {
                // 调试窗口
                isDebugView = !isDebugView
                binding.debugView.visibility = if (isDebugView) View.VISIBLE else View.GONE
            }

            R.id.historyView -> {
                //历史记录
                val customBottomSheetDialogFragment =
                    CustomBottomSheetDialogFragment(0, Common.user?.uid!!)
                customBottomSheetDialogFragment.show(
                    supportFragmentManager,
                    customBottomSheetDialogFragment.tag
                )
            }

            R.id.loginOutView -> {
                val builder = AlertDialog.Builder(this)
                builder.setTitle("请确定退出登录").setNegativeButton("确定") { _, _ ->
                    builder.create().dismiss()
                    Common.user = null
                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()
                }.setPositiveButton("取消") { _, _ ->
                    builder.create().dismiss()
                }
                builder.create().show()
            }

            R.id.updateView -> {
                val builder = AlertDialog.Builder(this)
                val view = UpdateUserDetailBinding.inflate(LayoutInflater.from(this))
                builder.setTitle("修改用户数据").setView(view.root)
                val dialog = builder.create()
                view.inputNameEdit.setText(Common.user?.name)
                view.inputPasswordEdit.setText(Common.user?.password)
                view.sexSpinner.setSelection(if (Common.user?.sex == "男") 0 else 1)
                view.roleLayout.visibility = View.GONE
                view.confirmButton.setOnClickListener {
                    if (verifyData(view)) {
                        val name = view.inputNameEdit.text.toString()
                        val password = view.inputPasswordEdit.text.toString()
                        val sex = view.sexSpinner.selectedItem.toString()
                        Common.user?.name = name
                        Common.user?.password = password
                        Common.user?.sex = sex
                        dao.update(Common.user!!, Common.user?.uid.toString())
                        MToast.mToast(this, "修改成功")
                        binding.openPassword.text = Common.user?.pwd.toString()
                        binding.nameText.text = Common.user?.name.toString()
                        //binding.sexText.text = Common.user?.sex.toString()
                        dialog.dismiss()
                    }
                }
                view.cancelButton.setOnClickListener {
                    dialog.dismiss()
                }
                dialog.show()
            }

            R.id.inRFView -> {
                recordFaceOrRF(0, 1)
                isADDFlag = true
            }

            R.id.inFaceView -> {
                recordFaceOrRF(0, 0)
                isADDFlag = true
            }
        }

        return super.onOptionsItemSelected(item)
    }

    /***
     * 数据验证
     */
    private fun verifyData(view: UpdateUserDetailBinding): Boolean {
        val name = view.inputNameEdit.text.toString()
        val password = view.inputPasswordEdit.text.toString()

        if (name.isEmpty()) {
            MToast.mToast(this, "用户名不能为空")
            return false
        }
        if (password.isEmpty()) {
            MToast.mToast(this, "密码不能为空")
            return false
        }
        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        Common.user = null
        EventBus.getDefault().unregister(this)
    }
}