package com.example.collegeentranceguardclocke.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import com.blankj.utilcode.util.ThreadUtils.runOnUiThread
import com.example.collegeentranceguardclocke.databinding.ListUserItemBinding
import com.example.collegeentranceguardclocke.db.UserDao
import com.example.collegeentranceguardclocke.entity.User
import com.example.collegeentranceguardclocke.utils.Common
import com.example.collegeentranceguardclocke.utils.Common.registryFlag
import com.example.collegeentranceguardclocke.utils.CustomDialog
import com.example.collegeentranceguardclocke.utils.MToast

class UserListViewAdapter(private val context: Context, private val list: MutableList<Any>) :
    BaseAdapter() {
    override fun getCount(): Int {
        return list.size
    }

    override fun getItem(p0: Int): Any {
        return list[p0]
    }

    override fun getItemId(p0: Int): Long {
        return p0.toLong()
    }

    override fun getView(p0: Int, p1: View?, p2: ViewGroup?): View {
        var view = p1
        val holder: ViewHolder
        if (view == null) {
            val binding = ListUserItemBinding.inflate(LayoutInflater.from(context), p2, false)
            view = binding.root
            holder = ViewHolder(binding)
            view.tag = holder
        } else {
            holder = view.tag as ViewHolder
        }

        initViews(holder.binding, p0)

        return view
    }

    private fun initViews(binding: ListUserItemBinding, index: Int) {
        val user = list[index] as User
        binding.nameText.text = user.name
        binding.accountText.text = user.account
        binding.createTimeText.text = user.createDateTime
        binding.roleText.text = if (user.per == 2) "教师" else "学生"
        binding.sexText.text = user.sex
        binding.stateText.text = if (user.state == 1) "在校" else "离校"
        binding.pwdText.text = user.pwd.toString()
        binding.deleteButton.setOnClickListener {
            if (user.fid != -1) {
                Common.sendMessage(context, 4, user.fid.toString())
                val dialog = CustomDialog(context, "删除人脸")
                Thread {
                    try {
                        runOnUiThread {
                            dialog.show()
                        }
                        var time = 0
                        while (true) {
                            if (time > 30) {
                                runOnUiThread {
                                    MToast.mToast(context, "超时,删除失败")
                                    dialog.dismiss()
                                }
                                break
                            } else {
                                if (registryFlag) {
                                    runOnUiThread {
                                        UserDao(context).delete(user.uid.toString())
                                        list.removeAt(index)
                                        notifyDataSetChanged()
                                        dialog.dismiss()
                                    }
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
            } else {
                UserDao(context).delete(user.uid.toString())
                list.removeAt(index)
                notifyDataSetChanged()
            }
        }
    }

    private class ViewHolder(val binding: ListUserItemBinding)
}