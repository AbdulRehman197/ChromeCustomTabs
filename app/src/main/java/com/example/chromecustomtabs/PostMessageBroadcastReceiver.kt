package com.example.chromecustomtabs

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.browser.customtabs.CustomTabsSession

class PostMessageBroadcastReceiver(mSession: CustomTabsSession?) : BroadcastReceiver() {
    private var customTabsSession: CustomTabsSession? = mSession

    companion object {
        const val POST_MESSAGE_ACTION = "com.example.post message.POST_MESSAGE_ACTION"
    }

    override fun onReceive(context: Context, intent: Intent) {
        customTabsSession?.postMessage("Got it!", null)
    }


}