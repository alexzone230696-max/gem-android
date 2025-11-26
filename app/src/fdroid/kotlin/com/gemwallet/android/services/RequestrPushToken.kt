package com.gemwallet.android.services

import android.content.Context
import com.gemwallet.android.cases.device.RequestPushToken

class StoreRequestPushToken : RequestPushToken {

    override fun initRequester(context: Context) {
        // Пустая реализация для FDroid flavor
    }

    override suspend fun requestToken(callback: (String) -> Unit) {
        // Возвращаем пустой токен, так как пуши недоступны
        callback("")
    }
}

// Функция проверки доступности уведомлений
fun isNotificationsAvailable() = false