package com.example.collegeentranceguardclocke.utils

import android.content.Context
import android.util.Log
import com.example.collegeentranceguardclocke.entity.Receive
import com.example.collegeentranceguardclocke.entity.Send
import com.example.collegeentranceguardclocke.entity.User
import com.google.gson.Gson
import com.itfitness.mqttlibrary.MQTTHelper

object Common {

//    const val PORT = "6002" // mqtt服务器端口号
//    const val SERVER_ADDRESS = "192.168.5.14"// mqtt 服务器地址

    const val PORT = "1883" // mqtt服务器端口号
    const val SERVER_ADDRESS = "iot-06z00axdhgfk24n.mqtt.iothub.aliyuncs.com"// mqtt 服务器地址
    const val URL = "tcp://$SERVER_ADDRESS:$PORT" // mqtt连接地址
    //const val URL = "k2a95upm1Oj.iot-as-mqtt.cn-shanghai.aliyuncs.com:1883" // mqtt连接地址
    const val RECEIVE_TOPIC = "/broadcast/k2a95upm1Oj/test2" // 接收消息订阅的主题 - 下位机发送消息的主题
    const val PUSH_TOPIC = "/broadcast/k2a95upm1Oj/test1" // 推送消息的主题 - 下位机接收消息的主题
    const val DRIVER_ID =
        "k2a95upm1Oj.android_app|securemode=2,signmethod=hmacsha256,timestamp=1747999519458|" // mqtt id
    const val DRIVER_NAME = "android_app&k2a95upm1Oj" // mqtt 用户名
    const val DRIVER_PASSWORD =
        "1f9bab7a6334a468f86d0df003b2afe233533979bd4d8bd5fae57be2a91146ba" // mqtt 鉴权或者密码
    var mqttHelper: MQTTHelper? = null // mqtt 连接服务函数
    var user: User? = null
    var registryFlag = false
    var receive: Receive? = null

    /***
     * 随机生成四位数密码
     */
    fun randomCipher(): String {
        return (1000..9999).random().toString()
    }

    /***
     * @brief 包装发送函数，只有建立了连接才发送消息
     */
    fun sendMessage(context: Context, cmd: Int, vararg data: String): String {
        return if (mqttHelper == null || !mqttHelper!!.connected) {
            MToast.mToast(context, "未建立连接")
            ""
        } else {
            try {
                val send = Send(cmd = cmd)
                when (cmd) {
                    1 -> {
                        send.en_face = data[0].toInt()
                    }

                    2 -> {
                        send.en_rfid = data[0].toInt()
                    }

                    3 -> {
                        send.door = data[0].toInt()
                        send.door_time = data[1].toInt()
                    }

                    4 -> {
                        send.did = data[0].toInt()
                    }

                    5 -> {
                        send.heart = data[0].toInt()
                    }

                    7 -> {
                        send.door_time = data[0].toInt()
                    }
                }
                val result = Gson().toJson(send)
                for (i in 0 until 3) {
                    mqttHelper!!.publish(PUSH_TOPIC, result, 1)
                }
                result
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("发送错误", e.message.toString())
                MToast.mToast(context, "数据发送失败")
                ""
            }
        }
    }

}