package com.example.collegeentranceguardclocke.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import com.example.collegeentranceguardclocke.databinding.HistoryListItemBinding
import com.example.collegeentranceguardclocke.db.HistoryDao
import com.example.collegeentranceguardclocke.db.UserDao
import com.example.collegeentranceguardclocke.entity.History
import com.example.collegeentranceguardclocke.entity.User


class HistoryListViewAdapter(
    private val context: Context,
    private var listData: MutableList<Any>
) : BaseAdapter() {
    private val dao = UserDao(context)
    override fun getCount(): Int {
        return listData.size
    }

    override fun getItem(i: Int): Any {
        return listData[i]
    }

    override fun getItemId(i: Int): Long {
        return i.toLong()
    }

    override fun getView(i: Int, p0: View?, viewGroup: ViewGroup): View {
        var view = p0
        val holder: ViewHolder
        if (view == null) {
            val binding = HistoryListItemBinding.inflate(
                LayoutInflater.from(
                    context
                ), viewGroup, false
            )
            view = binding.root
            holder = ViewHolder(binding)
            view.tag = holder
        } else {
            holder = view.tag as ViewHolder
        }
        initView(holder.binding, i)
        return view
    }

    private fun initView(binding: HistoryListItemBinding, index: Int) {
        val history = listData[index] as History
        val u = dao.query(history.uid.toString())?.get(0) as User
        val userRoleText = when (u.per) {
            1 -> "管理员"
            2 -> "自动化学院"
            3 -> "通信学院"
            4 -> "人工智能学院"
            5 -> "传媒学院"
            else -> "未知角色"
        }
        binding.roleText.text = userRoleText
        binding.nameText.text = u.name
        binding.openTime.text = history.createDateTime
        binding.stateText.text = if (history.state == 1) "进宿舍" else "离开宿舍"
    }

    class ViewHolder(var binding: HistoryListItemBinding)
}