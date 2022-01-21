package com.android.azure_communication_chat_poc

import com.azure.android.communication.chat.models.ChatEvent
import com.azure.android.communication.chat.models.ChatMessageReceivedEvent
import com.azure.android.communication.chat.models.RealTimeNotificationCallback
import org.threeten.bp.DateTimeUtils
import java.util.*

class ChatService(private val onChatMessageReceived: OnChatMessageReceived) : RealTimeNotificationCallback {
    override fun onChatEvent(payload: ChatEvent) {

        val chatMessageReceivedEvent = payload as ChatMessageReceivedEvent
        val date: Date = DateTimeUtils.toDate(chatMessageReceivedEvent.createdOn.toInstant())
        onChatMessageReceived.onChatMessageReceived(chatMessageReceivedEvent, date)
    }

    interface OnChatMessageReceived {
        fun onChatMessageReceived(chatMessageReceivedEvent: ChatMessageReceivedEvent, date: Date)
    }
}