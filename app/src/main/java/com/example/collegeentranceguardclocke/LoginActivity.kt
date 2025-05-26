package com.example.collegeentranceguardclocke

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.blankj.utilcode.util.LogUtils
import com.example.collegeentranceguardclocke.databinding.ActivityLoginBinding
import com.example.collegeentranceguardclocke.db.UserDao
import com.example.collegeentranceguardclocke.entity.Receive
import com.example.collegeentranceguardclocke.entity.User
import com.example.collegeentranceguardclocke.utils.Common
import com.example.collegeentranceguardclocke.utils.MToast

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding

    private lateinit var dao: UserDao
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        dao = UserDao(this)
        initViews()
    }

    private fun initViews() {
        binding.loginBtn.setOnClickListener { verifyData() }

        /***
         * 跳转注册
         */
        binding.skipRegisterBtn.setOnClickListener { view ->
            startActivity(
                Intent(
                    this@LoginActivity,
                    RegisterActivity::class.java
                )
            )
        }
    }

    private fun verifyData() {
        val name = binding.inputNameEdit.text.toString()
        val password = binding.inputPasswordEdit.text.toString()
        if (name.isEmpty()) {
            MToast.mToast(this, "用户名不能为空")
            return
        }
        if (password.isEmpty()) {
            MToast.mToast(this, "密码不能为空")
            return
        }
        if (name == "admin" && password == "123456") {
            startActivity(Intent(this, AdminActivity::class.java))
            finish()
        } else {
            val objects: List<Any>? = dao.query(name, password)
            if (objects!!.isEmpty()) {
                MToast.mToast(this, "账号或密码错误")
                return
            }
            val user: User = objects[0] as User
            if (user.account == name && user.password == password) {
                Common.user = user
                val intent = Intent(this, MainActivity::class.java)
                intent.putExtra("role", user.per)
                startActivity(intent)
                finish()
            } else {
                MToast.mToast(this, "账号或密码错误")
            }
        }
    }
}