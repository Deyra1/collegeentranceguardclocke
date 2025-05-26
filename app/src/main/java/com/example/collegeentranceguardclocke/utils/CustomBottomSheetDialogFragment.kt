package com.example.collegeentranceguardclocke.utils

import android.content.SharedPreferences
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import com.example.collegeentranceguardclocke.adapter.UserListViewAdapter
import com.example.collegeentranceguardclocke.databinding.BottomSheetDialogFrgmentLayoutBinding
import com.example.collegeentranceguardclocke.db.UserDao
import com.example.collegeentranceguardclocke.adapter.HistoryListViewAdapter
import com.example.collegeentranceguardclocke.db.HistoryDao
import com.example.collegeentranceguardclocke.entity.History
import com.example.collegeentranceguardclocke.entity.Receive
import com.example.collegeentranceguardclocke.entity.User
import com.example.collegeentranceguardclocke.utils.Common.registryFlag

import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode


class CustomBottomSheetDialogFragment(private val type: Int, private val id: Int) :
    BottomSheetDialogFragment(),
    HandlerAction {
    private lateinit var binding: BottomSheetDialogFrgmentLayoutBinding
    private lateinit var sharedPreferences: SharedPreferences // 临时存储
    private lateinit var editor: SharedPreferences.Editor // 修改提交
    private var time = "1"
    private lateinit var dao: UserDao
    private lateinit var hdao: HistoryDao
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        registryFlag = false
        EventBus.getDefault().register(this)
        dao = UserDao(requireContext())
        hdao = HistoryDao(requireContext())
        sharedPreferences =
            requireContext().getSharedPreferences("local", AppCompatActivity.MODE_PRIVATE)
        editor = sharedPreferences.edit()
        // 填充底部弹窗的布局文件
        binding = BottomSheetDialogFrgmentLayoutBinding.inflate(
            inflater, container, false
        )
        time = sharedPreferences.getString("openTime", "1").toString()

        when (type) {
            0 -> { // 查询记录
                val dao = HistoryDao(requireContext())
                // Query all history initially
                var allHistoryList: MutableList<Any>? = dao.query() // Call the parameterless query
                Log.d("HistoryViewDebug", "Queried all history. Count: ${allHistoryList?.size}")
                allHistoryList?.forEach { history ->
                    history as History
                    Log.d("HistoryViewDebug", "All history item - uid: ${history.uid}, state: ${history.state}, time: ${history.createDateTime}")
                }

                binding.outButton.setOnClickListener {
                    binding.outButton.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20F) //设置字体大小
                    binding.inButton.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15F) //设置字体大小
                    binding.outButton.typeface = Typeface.defaultFromStyle(Typeface.BOLD) // 加粗
                    binding.inButton.typeface = Typeface.defaultFromStyle(Typeface.NORMAL) // 取消加粗
                    // Filter all history for 'out' state (assuming state 0 is 'out') and current user or uid 0
                    val filteredList = allHistoryList?.filter { history ->
                        history as History
                        (history.uid == id || history.uid == 0) && history.state == 0
                    }?.toMutableList()
                    Log.d("HistoryViewDebug", "Filtered history for OUT. Count: ${filteredList?.size}")
                    filteredList?.forEach { history ->
                        history as History
                        Log.d("HistoryViewDebug", "OUT filtered item - uid: ${history.uid}, state: ${history.state}")
                    }
                    updateListUI(filteredList)
                }

                binding.inButton.setOnClickListener {
                    binding.inButton.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20F) //设置字体大小
                    binding.outButton.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15F) //设置字体大小
                    binding.inButton.typeface = Typeface.defaultFromStyle(Typeface.BOLD) // 加粗
                    binding.outButton.typeface = Typeface.defaultFromStyle(Typeface.NORMAL) // 取消加粗
                    // Filter all history for 'in' state (assuming state 1 is 'in') and current user or uid 0
                    val filteredList = allHistoryList?.filter { history ->
                        history as History
                        (history.uid == id || history.uid == 0) && history.state == 1
                    }?.toMutableList()
                    Log.d("HistoryViewDebug", "Filtered history for IN. Count: ${filteredList?.size}")
                    filteredList?.forEach { history ->
                        history as History
                        Log.d("HistoryViewDebug", "IN filtered item - uid: ${history.uid}, state: ${history.state}")
                    }
                    updateListUI(filteredList)
                }

                // Initially show all history for the current user and all generic events (uid 0)
                val initialList = allHistoryList?.filter { history ->
                     history as History
                     history.uid == id || history.uid == 0
                }?.toMutableList()
                Log.d("HistoryViewDebug", "Initial history list for UI. Count: ${initialList?.size}")
                initialList?.forEach { history ->
                    history as History
                    Log.d("HistoryViewDebug", "Initial list item - uid: ${history.uid}, state: ${history.state}")
                }
                // Sort by date/time in descending order for initial display
                initialList?.sortByDescending { (it as History).createDateTime }
                updateListUI(initialList)
            }

            else -> { //
                val dao = UserDao(requireContext())
                val list = dao.query()
                if (list != null) {
                    list.removeAt(0)
                    if (list.size > 0) {
                        binding.settingList.adapter = UserListViewAdapter(requireContext(), list)
                        binding.settingList.setOnItemClickListener { parent, view, position, id ->
                        }
                    } else {
                        dismiss()
                        MToast.mToast(requireContext(), "还没有数据")
                    }
                } else {
                    dismiss()
                    MToast.mToast(requireContext(), "还没有数据")
                }
            }
        }

        return binding.root
    }

    private fun updateListUI(list: MutableList<Any>?) {
        if (list != null) {
            if (list.size > 0) {
                binding.settingList.adapter = HistoryListViewAdapter(
                    requireContext(),
                    list
                )
            } else {
                Log.d("HistoryDebug", "History list is empty or null. Size: ${list?.size}")
                dismiss()
                MToast.mToast(requireContext(), "还没有数据")
            }
        } else {
            Log.d("HistoryDebug", "History list is null.")
            dismiss()
            MToast.mToast(requireContext(), "还没有数据")
        }
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
            val list = UserDao(requireContext()).query()!!
            if (data.face_id != null && data.face_id != "0") {
                for (d in list) {
                    val da = d as User
                    if (da.fid.toString() == data.face_id) {
                        Common.sendMessage(requireContext(), 3, "1", time)
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
            if (data.rfid != null && data.rfid != "0") {
                for (d in list) {
                    val da = d as User
                    if (da.rid.toString() == data.rfid) {
                        Common.sendMessage(requireContext(), 3, "1", time)
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
                        Common.sendMessage(requireContext(), 3, "1", time)
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
            MToast.mToast(requireContext(), "数据解析失败")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        EventBus.getDefault().unregister(this)
    }
}
