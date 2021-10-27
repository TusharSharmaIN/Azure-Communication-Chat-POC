package com.android.azure_communication_chat_poc;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.media.RingtoneManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Html;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.core.app.NotificationCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.azure.android.communication.chat.ChatAsyncClient;
import com.azure.android.communication.chat.ChatClientBuilder;
import com.azure.android.communication.chat.ChatThreadAsyncClient;
import com.azure.android.communication.chat.ChatThreadClientBuilder;
import com.azure.android.communication.chat.models.ChatEvent;
import com.azure.android.communication.chat.models.ChatEventType;
import com.azure.android.communication.chat.models.ChatMessage;
import com.azure.android.communication.chat.models.ChatMessageReceivedEvent;
import com.azure.android.communication.chat.models.ChatMessageType;
import com.azure.android.communication.chat.models.ChatParticipant;
import com.azure.android.communication.chat.models.ListChatMessagesOptions;
import com.azure.android.communication.chat.models.SendChatMessageOptions;
import com.azure.android.communication.common.CommunicationIdentifier;
import com.azure.android.communication.common.CommunicationTokenCredential;
import com.azure.android.communication.common.CommunicationUserIdentifier;
import com.azure.android.core.http.policy.UserAgentPolicy;
import com.azure.android.core.rest.util.paging.PagedAsyncStream;
import com.azure.android.core.util.AsyncStreamHandler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static androidx.core.app.NotificationCompat.PRIORITY_MAX;

/*
- Delivery status
- Media
- Add participants
 */

public class FinalWorkingActivity extends AppCompatActivity {

    private final String sdkVersion = "1.0.0";
    private static final String APPLICATION_ID = "Chat POC App";
    private static final String SDK_NAME = "azure-communication-com.azure.android.communication.chat";
    private static final String TAG = "Chat POC App";
    private static final String firstUserId = "";

    private static final String resourceUrl = "";
    private String firstUserAccessToken = "";
    private String userDisplayName = "";
    private String threadId = "";
    private ChatThreadAsyncClient chatThreadAsyncClient;
    ChatAsyncClient chatAsyncClient;

    // The list of ids corresponsding to messages which have already been processed
    ArrayList<String> chatMessages = new ArrayList<>();
    ArrayList<String> allMessageList = new ArrayList<>();
    AppCompatEditText messageBody;
    RecyclerView rvChatMessages;
    private LinearLayoutManager linearLayoutManager;
    ChatAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.final_working_chat);

        rvChatMessages = (RecyclerView) findViewById(R.id.rv_chat_messages);
        adapter = new ChatAdapter(allMessageList);
        linearLayoutManager = new LinearLayoutManager(this);
        rvChatMessages.setLayoutManager(linearLayoutManager);
        rvChatMessages.setAdapter(adapter);

        try {
            chatAsyncClient = new ChatClientBuilder()
                    .endpoint(resourceUrl)
                    .credential(new CommunicationTokenCredential(firstUserAccessToken))
                    .addPolicy(new UserAgentPolicy(APPLICATION_ID, SDK_NAME, sdkVersion))
                    .buildAsyncClient();

            createChatThreadClient();

            // Retrieve all messages accessible to the user
            PagedAsyncStream<ChatMessage> messagePagedAsyncStream
                    = this.chatThreadAsyncClient.listMessages(new ListChatMessagesOptions(), null);
            // Set up a lock to wait until all returned messages have been inspected
            CountDownLatch latch = new CountDownLatch(1);
            // Traverse the returned messages
            messagePagedAsyncStream.forEach(new AsyncStreamHandler<ChatMessage>() {
                @Override
                public void onNext(ChatMessage message) {
                    // Messages that should be displayed in the chat
                    Log.d("sdjasdkh","onNext"+message.getId());

                    if ((message.getType().equals(ChatMessageType.TEXT)
                            || message.getType().equals(ChatMessageType.HTML))
                            && !chatMessages.contains(message.getId())) {
                        allMessageList.add(message.getSenderDisplayName()+": "+message.getContent().getMessage());
                    }
                }
                @Override
                public void onError(Throwable throwable) {
                    latch.countDown();
                }
                @Override
                public void onComplete() {
                    latch.countDown();
                }
            });
            // Wait until the operation completes
            latch.await(1, TimeUnit.MINUTES);
            Collections.reverse(allMessageList);

            adapter.notifyDataSetChanged();

            // <RECEIVE CHAT MESSAGES>
            // Start real time notification
            chatAsyncClient.startRealtimeNotifications(firstUserAccessToken, getApplicationContext());

            // Register a listener for chatMessageReceived event
            chatAsyncClient.addEventHandler(ChatEventType.CHAT_MESSAGE_RECEIVED, (ChatEvent payload) -> {
                ChatMessageReceivedEvent chatMessageReceivedEvent = (ChatMessageReceivedEvent) payload;
                // You code to handle chatMessageReceived event
                allMessageList.add(chatMessageReceivedEvent.getSenderDisplayName()+": "+chatMessageReceivedEvent.getContent());
                Log.d("sdjasdkh","chatMessageReceivedEvent-->"+allMessageList.toString());
                if (!chatMessageReceivedEvent.getSenderDisplayName().trim().equalsIgnoreCase(userDisplayName)) {
                    addNotification(chatMessageReceivedEvent.getSenderDisplayName()+": "+chatMessageReceivedEvent.getContent());
                }

                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        adapter.notifyDataSetChanged();
                    }
                });

            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void addNotification(String message) {
        int NOTIFICATION_ID = 1;
        String NOTIFICATION_CHANNEL_ID = "my_notification_channel";

        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, "My Notifications", NotificationManager.IMPORTANCE_DEFAULT);

            // Configure the notification channel.
            notificationChannel.setDescription("Channel description");
            notificationChannel.enableLights(true);
            notificationChannel.setLightColor(Color.RED);
            notificationChannel.setVibrationPattern(new long[]{0, 1000, 500, 1000});
            notificationChannel.enableVibration(true);
            notificationManager.createNotificationChannel(notificationChannel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setVibrate(new long[]{0, 100, 100, 100, 100, 100})
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Chat-POC")
                .setPriority(PRIORITY_MAX)
                .setContentText(message);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            String channelId = "Your_channel_id";
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    "Channel human readable title",
                    NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(channel);
            builder.setChannelId(channelId);
        }
        notificationManager.notify(NOTIFICATION_ID, builder.build());
    }

    private void createChatThreadClient() {
        AppCompatEditText threadIdView = findViewById(R.id.et_thread_id);
        //threadId = threadIdView.getText().toString();
        // Initialize Chat Thread Client
        chatThreadAsyncClient = new ChatThreadClientBuilder()
                .endpoint(resourceUrl)
                .credential(new CommunicationTokenCredential(firstUserAccessToken))
                .chatThreadId(threadId)
                .buildAsyncClient();
        Button sendMessageButton = findViewById(R.id.btn_send);
        messageBody = findViewById(R.id.et_message_body);
        // Register the method for sending messages and toggle the visibility of chat components
        sendMessageButton.setOnClickListener(l -> sendMessage());
        sendMessageButton.setVisibility(View.VISIBLE);
    }

    private void sendMessage() {
        // Retrieve the typed message content
        // Set request options and send message
        SendChatMessageOptions options = new SendChatMessageOptions();
        options.setContent(messageBody.getText().toString());
        options.setSenderDisplayName(userDisplayName);
        chatThreadAsyncClient.sendMessage(options);
        messageBody.setText("");
    }
}