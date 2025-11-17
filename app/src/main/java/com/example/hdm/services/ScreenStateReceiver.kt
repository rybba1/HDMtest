package com.example.hdm.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.hdm.model.UserManager

class ScreenStateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_SCREEN_ON -> {
                Log.d("ScreenStateReceiver", "Ekran został włączony.")
                UserManager.setScreenState(true)
            }
            Intent.ACTION_SCREEN_OFF -> {
                Log.d("ScreenStateReceiver", "Ekran został wyłączony.")
                UserManager.setScreenState(false)
            }
        }
    }
}