package com.android.azure_communication_chat_poc

import android.content.Context
import android.util.Log
import com.azure.android.communication.chat.ChatAsyncClient
import com.azure.android.communication.chat.ChatClientBuilder
import com.azure.android.communication.chat.ChatThreadAsyncClient
import com.azure.android.communication.chat.ChatThreadClientBuilder
import com.azure.android.communication.chat.models.*
import com.azure.android.communication.common.CommunicationTokenCredential
import com.azure.android.core.http.policy.UserAgentPolicy
import com.azure.android.core.rest.util.paging.PagedAsyncStream
import com.azure.android.core.util.AsyncStreamHandler
import org.threeten.bp.DateTimeUtils
import java.util.*
import kotlin.collections.HashMap

class AzureChatHelper {
    var resourceUrl = ""
    private val sdkVersion = "1.0.0"
    private val applicationId = "Azure_Chat_POC"
    private val sdkName = "azure-communication-com.azure.android.communication.chat"
    private var threadId = ""
    private var chatParticipantName = ""
    private var userAccessToken = ""
    var chatAsyncClient: ChatAsyncClient? = null
    var chatThreadAsyncClient: ChatThreadAsyncClient? = null

    constructor(threadId: String, chatParticipantName: String, userAccessToken: String) {
        this.threadId = threadId
        this.chatParticipantName = chatParticipantName
        this.userAccessToken = userAccessToken
    }

    constructor(userAccessToken: String) {
        this.userAccessToken = userAccessToken
    }

    fun createChatAsyncClient() {
        try {
            chatAsyncClient = ChatClientBuilder()
                    .endpoint(resourceUrl)
                    .credential(CommunicationTokenCredential(userAccessToken))
                    .addPolicy(UserAgentPolicy(applicationId, sdkName, sdkVersion))
                    .buildAsyncClient()
        } catch (exception: Exception) {
            exception.printStackTrace()
        }
    }

    fun createChatThreadAsyncClient() {
        try {
            // Initialize Chat Thread Client
            chatThreadAsyncClient = ChatThreadClientBuilder()
                    .endpoint(resourceUrl)
                    .credential(CommunicationTokenCredential(userAccessToken))
                    .chatThreadId(threadId)
                    .buildAsyncClient()
        } catch (exception: Exception) {
            exception.printStackTrace()
        }
    }

    fun getMembersListOfTheThread(): PagedAsyncStream<ChatParticipant> {
        return chatThreadAsyncClient!!.listParticipants()
    }

    fun sendMessage(message: String, senderDisplayName: String, imageUrl: String? = null) {
        val options = SendChatMessageOptions()
        options.type = ChatMessageType.TEXT
        options.content = message
        options.senderDisplayName = senderDisplayName
        if (imageUrl != null) {
            val metadata: MutableMap<String, String> = HashMap()
            metadata["hasAttachment"] = "true"
            metadata["attachmentUrl"] = imageUrl
            options.metadata = metadata
        }
        chatThreadAsyncClient!!.sendMessage(options)
    }

    fun getAllMessagesOfThatThread(myDisplayName: String, context: Context, azureCommunicationCallbacks: AzureCommunicationCallbacks) {
        val allMessageList: ArrayList<ChatMessages> = ArrayList<ChatMessages>()
        val messagePagedAsyncStream = chatThreadAsyncClient!!.listMessages(ListChatMessagesOptions(), null)
        try {
            messagePagedAsyncStream.forEach(object : AsyncStreamHandler<ChatMessage> {
                override fun onNext(message: ChatMessage) {
                    if ((message.type == ChatMessageType.TEXT || message.type == ChatMessageType.HTML)) {
                        val date: Date = DateTimeUtils.toDate(message.createdOn.toInstant())
                        allMessageList.add(ChatMessages(message.content.message, "", myDisplayName == message.senderDisplayName, message.senderDisplayName,
                                format(date)))
                    }
                }

                override fun onError(throwable: Throwable) {
                    azureCommunicationCallbacks.onMessageRetrievalError(throwable.message.toString())
                }

                override fun onComplete() {
                    azureCommunicationCallbacks.onMessageRetrievalComplete(allMessageList)
                }
            })
        } catch (exception: Exception) {
            exception.printStackTrace()
            azureCommunicationCallbacks.onMessageRetrievalError(exception.printStackTrace().toString())
        }
    }

    fun receiveChatMessages(context: Context, onChatMessageReceived: ChatService.OnChatMessageReceived) {
        chatAsyncClient!!.startRealtimeNotifications(userAccessToken, context)
        chatAsyncClient!!.addEventHandler(ChatEventType.CHAT_MESSAGE_RECEIVED, ChatService(onChatMessageReceived))
    }

    fun getAllThreads(userListMap: LinkedHashMap<String, String>, azureCommunicationCallbacks: AzureCommunicationCallbacks) {
        try {
            val listChatThreads: PagedAsyncStream<ChatThreadItem> = chatAsyncClient!!.listChatThreads()
            val listOfThreadIds = ArrayList<String>()

            listChatThreads.forEach(object : AsyncStreamHandler<ChatThreadItem> {
                override fun onNext(chatThreadItem: ChatThreadItem?) {
                    listOfThreadIds.add(chatThreadItem!!.id)
                    userListMap[chatThreadItem.id] = chatThreadItem.topic
                }

                override fun onComplete() {
                    azureCommunicationCallbacks.onThreadRetrievalComplete(userListMap)
                }

                override fun onError(throwable: Throwable?) {
                    azureCommunicationCallbacks.onThreadRetrievalError(throwable?.message!!)
                }
            })

        } catch (exception: Exception) {
            exception.printStackTrace()
            azureCommunicationCallbacks.onThreadRetrievalError(exception.printStackTrace().toString())
        }
    }

    fun createAChatThread(participants: MutableList<ChatParticipant>, chatName: String): String {
        // The topic for the thread.
        //val topic = chatViewModel.getUserProfile().firstName + " " + chatViewModel.getUserProfile().lastName + "-" + userName
        // Optional, set a repeat request ID.
        val repeatabilityRequestID = ""
        val createChatThreadOptions = CreateChatThreadOptions()
                .setTopic(chatName)
                .setParticipants(participants)
                .setIdempotencyToken(repeatabilityRequestID)
        val createChatThreadResult = chatAsyncClient!!.createChatThread(createChatThreadOptions).get()
        val chatThreadProperties = createChatThreadResult.chatThreadProperties
        val threadId = chatThreadProperties.id
        return threadId
    }


    interface AzureCommunicationCallbacks {
        fun onMessageRetrievalComplete(allMessages: ArrayList<ChatMessages>)
        fun onMessageRetrievalError(error: String)

        fun onThreadRetrievalComplete(userListMap: LinkedHashMap<String, String>)
        fun onThreadRetrievalError(error: String)
    }

    fun format(date: Date): String {
        return when {
            ChatDateFormatter.isToday(date) -> {
                "Today" + "  " + ChatDateFormatter.format(date, ChatDateFormatter.Template.TIME)

            }
            ChatDateFormatter.isYesterday(date) -> {
                "Yesterday" + "  " + ChatDateFormatter.format(date, ChatDateFormatter.Template.TIME)
            }
            else -> {
                ChatDateFormatter.format(date, ChatDateFormatter.Template.STRING_DAY_MONTH_YEAR) + "  " + ChatDateFormatter.format(date, ChatDateFormatter.Template.TIME)
            }
        }
    }
}