package com.example.collegeentranceguardclocke.entity

data class User(
    var uid: Int? = null,
    var account: String? = null,//账号
    var password: String? = null,//密码
    var name: String? = null,//姓名
    var per: Int? = null,//职位 1 管理员  2 自动化学院 3 通信学院 4 人工智能学院 5 传媒学院
    var pwd: Int? = null,// 开门密码
    var state: Int? = null,// 在宿舍状态 1为在宿舍 0为不在宿舍
    var sex: String? = null,// 性别
    var rid: String? = null,// rf 的id
    var fid: Int? = null,// 人脸id
    var createDateTime: String? = null
)
