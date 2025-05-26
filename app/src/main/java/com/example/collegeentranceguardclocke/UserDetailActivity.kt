package com.example.collegeentranceguardclocke

import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AlertDialog
import com.example.collegeentranceguardclocke.adapter.UserListViewAdapter
import com.example.collegeentranceguardclocke.databinding.ActivityUserDetailBinding
import com.example.collegeentranceguardclocke.databinding.UpdateUserDetailBinding
import com.example.collegeentranceguardclocke.db.HistoryDao
import com.example.collegeentranceguardclocke.db.UserDao
import com.example.collegeentranceguardclocke.entity.History
import com.example.collegeentranceguardclocke.entity.Receive
import com.example.collegeentranceguardclocke.entity.User
import com.example.collegeentranceguardclocke.utils.Common
import com.example.collegeentranceguardclocke.utils.CustomBottomSheetDialogFragment
import com.example.collegeentranceguardclocke.utils.MToast
import com.gyf.immersionbar.ImmersionBar
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.util.Objects

class UserDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityUserDetailBinding
    private var isReceive = false
    private lateinit var dao: UserDao
    private lateinit var hdao: HistoryDao
    private lateinit var sharedPreferences: SharedPreferences // 临时存储
    private lateinit var editor: SharedPreferences.Editor // 修改提交
    private var time = "1"
    private var list: MutableList<Any>? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        sharedPreferences = getSharedPreferences("local", MODE_PRIVATE)
        editor = sharedPreferences.edit()
        dao = UserDao(this)
        hdao = HistoryDao(this)
        initViews()
        EventBus.getDefault().register(this)
    }

    private fun initViews() {
        Common.registryFlag = false
        setSupportActionBar(binding.toolbar)
        ImmersionBar.with(this).init()
        Objects.requireNonNull(supportActionBar)?.setDisplayHomeAsUpEnabled(true) //添加默认的返回图标
        supportActionBar!!.setHomeButtonEnabled(true) //设置返回键可用
        time = sharedPreferences.getString("openTime", "1").toString()
        list = dao.query()
        if (list == null || list!!.size <= 0) {
            isShowListView(false)
        }
        binding.assetsList.adapter = UserListViewAdapter(this, list!!)
        eventManager()
    }

    private fun eventManager() {
        binding.searchButton.setOnClickListener {
            val text = binding.searchText.text.toString()
            isShowListView(true)
            if (text != "") {
                list = dao.query(text, "name")
                if (list == null || list!!.size <= 0) {
                    isShowListView(false)
                }
            } else {
                list = dao.query()
                if (list == null || list!!.size <= 0) {
                    isShowListView(false)
                }
            }
            binding.assetsList.adapter = UserListViewAdapter(this, list!!)
        }

        binding.assetsList.setOnItemClickListener { adapterView, view, p2, l ->
            val builder = AlertDialog.Builder(this@UserDetailActivity)
            builder.setTitle("选择").setMessage("请选择")
                .setNegativeButton("取消") { _, _ ->
                    builder.create().dismiss()
                }.setPositiveButton("修改信息") { _, _ ->
                    builder.create().dismiss()
                    updateItemDialog(p2)
                }.setNeutralButton("查看记录") { _, _ ->
                    val t = list?.get(p2) as User
                    //历史记录
                    val customBottomSheetDialogFragment =
                        CustomBottomSheetDialogFragment(0, t.uid!!)
                    customBottomSheetDialogFragment.show(
                        supportFragmentManager,
                        customBottomSheetDialogFragment.tag
                    )
                }.setPositiveButton("删除用户") { _, _ ->
                    val userToDelete = list?.get(p2) as User
                    AlertDialog.Builder(this@UserDetailActivity)
                        .setTitle("确认删除")
                        .setMessage("确定要删除用户 ${userToDelete.name} 吗？")
                        .setPositiveButton("确定") { _, _ ->
                            val deleteResult = dao.delete(userToDelete.uid.toString())
                            if (deleteResult == 0) {
                                MToast.mToast(this@UserDetailActivity, "删除成功")
                                // 刷新列表
                                list = dao.query()
                                binding.assetsList.adapter = UserListViewAdapter(this@UserDetailActivity, list!!)
                                if (list == null || list!!.size <= 0) {
                                    isShowListView(false)
                                }
                            } else {
                                MToast.mToast(this@UserDetailActivity, "删除失败")
                            }
                        }
                        .setNegativeButton("取消") { dialog, _ ->
                            dialog.dismiss()
                        }
                        .show()
                }
            builder.create().show()

        }

    }

    /***
     * @brief 显示修改物品弹窗
     * @param p2 item在list中的索引
     */
    private fun updateItemDialog(p2: Int) {
        val t = list?.get(p2) as User
        val builder = AlertDialog.Builder(this)
        val view = UpdateUserDetailBinding.inflate(LayoutInflater.from(this))
        builder.setTitle("修改用户数据").setView(view.root)
        val dialog = builder.create()
        view.inputNameEdit.setText(t.name)
        view.inputPasswordEdit.setText(t.password)
        //view.sexSpinner.setSelection(if (t.sex == "男") 0 else 1)
        val selectionIndex = when (t.per) {
            2 -> 0  // 自动化学院 对应 Spinner 选项索引 0
            3 -> 1  // 通信学院 对应 Spinner 选项索引 1
            4 -> 2  // 人工智能学院 对应 Spinner 选项索引 2
            5 -> 3  // 传媒学院 对应 Spinner 选项索引 3
            1 -> 4  // 管理员 对应 Spinner 选项索引 4 (假设管理员是最后一个选项)
            else -> 0 // 默认选项，例如选中第一个"自动化学院"，或者你可以设置为一个表示"请选择"的项的索引
        }
        view.roleSpinner.setSelection(selectionIndex)
        view.confirmButton.setOnClickListener {
            if (verifyData(view)) {
                val name = view.inputNameEdit.text.toString()
                val password = view.inputPasswordEdit.text.toString()
                val sex = view.sexSpinner.selectedItem.toString()
                val selectedRoleText = view.roleSpinner.selectedItem.toString() // 获取 Spinner 中选中的文本
                val role = when (selectedRoleText) {
                    "自动化学院" -> 2
                    "通信学院" -> 3
                    "人工智能学院" -> 4
                    "传媒学院" -> 5
                    "管理员" -> 1
                    else -> {
                        Log.w("RoleSelection", "未知的角色选项: $selectedRoleText，将使用默认值或标记为错误。")
                        1
                    }
                }
                t.name = name
                t.password = password
                t.sex = sex
                t.per = role
                dao.update(t, t.uid.toString())
                MToast.mToast(this, "修改成功")
                dialog.dismiss()
            }
        }
        view.cancelButton.setOnClickListener {
            dialog.dismiss()
        }
        dialog.show()
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

    /***
     * @brief 是否显示数据列表
     * @param f true 显示
     */
    private fun isShowListView(f: Boolean) {
        binding.assetsList.visibility = if (f) View.VISIBLE else View.GONE
        binding.nullDataView.visibility = if (f) View.GONE else View.VISIBLE
    }

    /**
     * 解析数据
     * @param data
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun receiveDataFormat(data: Receive) {
        try {
            if (data.fid != null && data.fid != "0" || data.frfid != null && data.frfid != "0" || data.did != null) {
                Common.registryFlag = true
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
                        da.state = if (da.state == 0) 1 else 0 // 修改在宿舍状态
                        dao.update(da, da.uid.toString())
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
                        dao.update(da, da.uid.toString())
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
                        dao.update(da, da.uid.toString())
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

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        finish()
        return super.onOptionsItemSelected(item)
    }

    override fun onDestroy() {
        super.onDestroy()
        EventBus.getDefault().unregister(this)
    }
}