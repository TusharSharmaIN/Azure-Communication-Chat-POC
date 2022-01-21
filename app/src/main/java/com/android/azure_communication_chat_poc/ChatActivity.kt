package com.android.azure_communication_chat_poc

import android.app.NotificationChannel
import android.app.NotificationManager
import android.graphics.Color
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.azure.android.communication.chat.models.ChatMessageReceivedEvent
import com.azure.android.communication.chat.models.ChatParticipant
import kotlinx.android.synthetic.main.activity_chat.*
import java.util.*
import java.util.concurrent.ExecutionException

class ChatActivity : AppCompatActivity(), ChatService.OnChatMessageReceived, AzureChatHelper.AzureCommunicationCallbacks {

    private var linearLayoutManager: LinearLayoutManager? = null
    var adapter: ChatAdapter? = null
    var myName = ""
    var allMessageList: ArrayList<ChatMessages> = ArrayList<ChatMessages>()
    private var threadId = ""
    private var token = ""
    private var resourceUrl = ""
    private lateinit var azureChatHelper: AzureChatHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        threadId = intent.extras?.getString("THREAD_ID").toString()
        myName = intent.extras?.getString("USER_NAME").toString()
        token = intent.extras?.getString("TOKEN").toString()
        resourceUrl = intent.extras?.getString("RESOURCE_URL").toString()

        adapter =
            ChatAdapter(allMessageList)
        linearLayoutManager = LinearLayoutManager(this)
        rv_chat_messages.layoutManager = linearLayoutManager
        rv_chat_messages.adapter = adapter
        azureChatHelper = AzureChatHelper(threadId, myName, token)
        azureChatHelper.resourceUrl = resourceUrl

        try {
            azureChatHelper.createChatAsyncClient()
            azureChatHelper.createChatThreadAsyncClient()
            val participantsPagedAsyncStream = azureChatHelper.getMembersListOfTheThread()
            val participantNames = mutableListOf<String>()
            var allMembers = "Thread members: "

            participantsPagedAsyncStream.forEach { chatParticipant: ChatParticipant ->
                participantNames.add(chatParticipant.displayName)
                allMembers = allMembers + chatParticipant.displayName + ", "
                tv_members.text = allMembers
            }

            btn_send.setOnClickListener {
                azureChatHelper.sendMessage(et_message_body.text.toString(), myName)
                et_message_body.setText("")
            }

            azureChatHelper.getAllMessagesOfThatThread(myName, this, this)

            azureChatHelper.receiveChatMessages(this, this)
        } catch (exception: Exception) {
            exception.printStackTrace()
        }
    }


    private fun addNotification(message: String) {
        val NOTIFICATION_ID = 1
        val NOTIFICATION_CHANNEL_ID = "my_notification_channel"
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "My Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            )

            // Configure the notification channel.
            notificationChannel.description = "Channel description"
            notificationChannel.enableLights(true)
            notificationChannel.lightColor = Color.RED
            notificationChannel.vibrationPattern = longArrayOf(0, 1000, 500, 1000)
            notificationChannel.enableVibration(true)
            notificationManager.createNotificationChannel(notificationChannel)
        }
        val builder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setVibrate(longArrayOf(0, 100, 100, 100, 100, 100))
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Chat-POC")
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setContentText(message)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "Your_channel_id"
            val channel = NotificationChannel(
                channelId,
                "Channel human readable title",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
            builder.setChannelId(channelId)
        }
        notificationManager.notify(NOTIFICATION_ID, builder.build())
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
        }
        return super.onOptionsItemSelected(item)
    }


    override fun onChatMessageReceived(
        chatMessageReceivedEvent: ChatMessageReceivedEvent,
        date: Date
    ) {
        // You code to handle chatMessageReceived event
        allMessageList.add(
            ChatMessages(
                chatMessageReceivedEvent.content,
                "",
                myName == chatMessageReceivedEvent.senderDisplayName,
                chatMessageReceivedEvent.senderDisplayName,
                azureChatHelper.format(
                    date
                )
            )
        )
        Log.d("sdjasdkh", "chatMessageReceivedEvent-->" + allMessageList.toString())
        Log.d("sdjasdkh", "senderDisplayName-->" + chatMessageReceivedEvent.senderDisplayName)
        if (!chatMessageReceivedEvent.senderDisplayName.trim { it <= ' ' }.equals(
                myName,
                ignoreCase = true
            )) {
            try {
                Log.d("sdjasdkh", "sendReadReceipt() -->" + chatMessageReceivedEvent.id)
                //chatThreadAsyncClient!!.sendReadReceipt(chatMessageReceivedEvent.id).get()
            } catch (e: InterruptedException) {
                e.printStackTrace()
            } catch (e: ExecutionException) {
                e.printStackTrace()
            }
            addNotification(chatMessageReceivedEvent.senderDisplayName + ": " + chatMessageReceivedEvent.content)
        }
        Handler(Looper.getMainLooper()).post { adapter!!.notifyDataSetChanged() }
    }

    override fun onMessageRetrievalComplete(allMessages: ArrayList<ChatMessages>) {
        allMessages.reverse()
        allMessageList = allMessages
        Handler(Looper.getMainLooper()).post {
            adapter = ChatAdapter(
                allMessageList
            )
            linearLayoutManager = LinearLayoutManager(this)
            rv_chat_messages.layoutManager = linearLayoutManager
            rv_chat_messages.adapter = adapter
        }
    }

    override fun onMessageRetrievalError(error: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(this, error, Toast.LENGTH_LONG).show()
        }
    }

    override fun onThreadRetrievalComplete(userListMap: LinkedHashMap<String, String>) {
        //Not in use
    }

    override fun onThreadRetrievalError(error: String) {
        //Not in use
    }
}