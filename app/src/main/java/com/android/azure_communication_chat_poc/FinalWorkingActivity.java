package com.android.azure_communication_chat_poc;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
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
import com.azure.android.communication.chat.models.ChatThreadItem;
import com.azure.android.communication.chat.models.ChatThreadProperties;
import com.azure.android.communication.chat.models.CreateChatThreadOptions;
import com.azure.android.communication.chat.models.CreateChatThreadResult;
import com.azure.android.communication.chat.models.ListChatMessagesOptions;
import com.azure.android.communication.chat.models.ListChatThreadsOptions;
import com.azure.android.communication.chat.models.ListParticipantsOptions;
import com.azure.android.communication.chat.models.ReadReceiptReceivedEvent;
import com.azure.android.communication.chat.models.SendChatMessageOptions;
import com.azure.android.communication.chat.models.TypingIndicatorReceivedEvent;
import com.azure.android.communication.common.CommunicationTokenCredential;
import com.azure.android.communication.common.CommunicationUserIdentifier;
import com.azure.android.core.http.policy.UserAgentPolicy;
import com.azure.android.core.rest.util.paging.PagedAsyncStream;
import com.azure.android.core.rest.util.paging.PagedIterable;
import com.azure.android.core.util.AsyncStreamHandler;
import com.azure.android.core.util.CancellationToken;
import com.azure.android.core.util.RequestContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static androidx.core.app.NotificationCompat.PRIORITY_MAX;

/*
- Delivery status
- Media
- Add participants
 */

public class FinalWorkingActivity extends AppCompatActivity {

    private final String sdkVersion = "1.0.0";
    private static final String APPLICATION_ID = "Helios_Chat_POC";
    private static final String SDK_NAME = "azure-communication-com.azure.android.communication.chat";
    //Required while creating a chat thread
    private static final String firstUserId = "8:acs:a2a000be-2d06-429b-ba20-0d1c504e89b6_0000000d-b421-128a-02c3-593a0d004a1b";

    private static final String resourceUrl = "";
    private String firstUserAccessToken = "";
    private String userDisplayName = "";
    private String threadId = "";
    private ChatThreadAsyncClient chatThreadAsyncClient;
    ChatAsyncClient chatAsyncClient;

    // The list of ids corresponsding to messages which have already been processed
    ArrayList<String> chatMessages = new ArrayList<>();
    ArrayList<ChatModel> allMessageList = new ArrayList<>();
    AppCompatEditText messageBody;
    RecyclerView rvChatMessages;
    AppCompatButton btnTypingIndicator, btnListUsersInThread, btnCreateChatThread;
    private LinearLayoutManager linearLayoutManager;
    ChatAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.final_working_chat);

        rvChatMessages = (RecyclerView) findViewById(R.id.rv_chat_messages);
        btnTypingIndicator = (AppCompatButton) findViewById(R.id.btn_typing_indicator);
        btnListUsersInThread = (AppCompatButton) findViewById(R.id.btn_list_users_in_thread);
        btnCreateChatThread = (AppCompatButton) findViewById(R.id.btn_create_chat_thread);
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

            retrieveAllMessagesAndUpdateAdapter();

            receiveChatReceipt();

            receiveChatMessages();

        } catch (Exception e) {
            e.printStackTrace();
        }

        sendTypingNotification();

        getListOfUsersInSameThread();

        createANewThread();

        //PagedFlux<ChatThreadItem> listOfThreadIds1 = chatAsyncClient.listChatThreads();

        ListChatThreadsOptions listChatThreadsOptions = new ListChatThreadsOptions()
                .setMaxPageSize(10);
        PagedAsyncStream<ChatThreadItem> listOfThreadIds = chatAsyncClient.listChatThreads(listChatThreadsOptions, RequestContext.NONE);
        listOfThreadIds.forEach(chatThreadItem-> {
            Log.d("hsdghdas","ID->"+chatThreadItem.getId());
            Log.d("hsdghdas","getTopic->"+chatThreadItem.getTopic());
        });
    }

    private void createANewThread() {

        /*
        Add participants in thread with user Id
        String secondUserDisplayName = "a new participant";
        ChatParticipant participant = new ChatParticipant()
                .setCommunicationIdentifier(new CommunicationUserIdentifier(secondUserId))
                .setDisplayName(secondUserDisplayName);

        chatThreadAsyncClient.addParticipant(participant);*/

        btnCreateChatThread.setOnClickListener(v -> {
        try {
            // A list of ChatParticipant to start the thread with.
            List<ChatParticipant> participants = new ArrayList<>();
            // The display name for the thread participant.
            participants.add(new ChatParticipant()
                    .setCommunicationIdentifier(new CommunicationUserIdentifier(firstUserId))
                    .setDisplayName(userDisplayName));

            // The topic for the thread.
            final String topic = "General";
            // Optional, set a repeat request ID.
            final String repeatabilityRequestID = "";
            // Options to pass to the create method.
            CreateChatThreadOptions createChatThreadOptions = new CreateChatThreadOptions()
                    .setTopic(topic)
                    .setParticipants(participants)
                    .setIdempotencyToken(repeatabilityRequestID);

            CreateChatThreadResult createChatThreadResult =
                    chatAsyncClient.createChatThread(createChatThreadOptions).get();
            ChatThreadProperties chatThreadProperties = createChatThreadResult.getChatThreadProperties();
            threadId = chatThreadProperties.getId();
            Log.d("sdasjdlksa","New threadId->"+threadId);
        } catch (Exception e) {
            e.printStackTrace();
        }
    });
    }

    private void sendTypingNotification() {
        btnTypingIndicator.setOnClickListener(v -> chatThreadAsyncClient.sendTypingNotification());
    }

    private void getListOfUsersInSameThread() {
        btnListUsersInThread.setOnClickListener(v -> {
            // The maximum number of participants to be returned per page, optional.
            int maxPageSize = 10;

            // Skips participants up to a specified position in response.
            int skip = 0;

            // Options to pass to the list method.
            ListParticipantsOptions listParticipantsOptions = new ListParticipantsOptions()
                    .setMaxPageSize(maxPageSize)
                    .setSkip(skip);

            PagedAsyncStream<ChatParticipant> participantsPagedAsyncStream =
                    chatThreadAsyncClient.listParticipants(listParticipantsOptions, RequestContext.NONE);
            List<String> usersList = new ArrayList();

            participantsPagedAsyncStream.forEach(chatParticipant -> {
                // You code to handle participant
                usersList.add(chatParticipant.getDisplayName()+"\n");
                Log.d("usersList","chatParticipant.getDisplayName()--->"+chatParticipant.getDisplayName());

            });

            Toast.makeText(this, "Participants"+usersList.toString(), Toast.LENGTH_LONG).show();
        });
    }

    private void receiveChatMessages() {
        // <RECEIVE CHAT MESSAGES>
        // Start real time notification
        // Register a listener for chatMessageReceived event
        chatAsyncClient.addEventHandler(ChatEventType.CHAT_MESSAGE_RECEIVED, (ChatEvent payload) -> {
            ChatMessageReceivedEvent chatMessageReceivedEvent = (ChatMessageReceivedEvent) payload;

            // You code to handle chatMessageReceived event
            allMessageList.add(new ChatModel(chatMessageReceivedEvent.getSenderDisplayName()+": "+chatMessageReceivedEvent.getContent(),
                    chatMessageReceivedEvent.getMetadata().containsKey("hasAttachment"), chatMessageReceivedEvent.getMetadata().get("attachmentUrl")));
            Log.d("sdjasdkh","chatMessageReceivedEvent-->"+allMessageList.toString());
            if (!chatMessageReceivedEvent.getSenderDisplayName().trim().equalsIgnoreCase(userDisplayName)) {
                try {
                    Log.d("sdjasdkh","sendReadReceipt() -->"+chatMessageReceivedEvent.getId());
                    chatThreadAsyncClient.sendReadReceipt(chatMessageReceivedEvent.getId()).get();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                }
                addNotification(chatMessageReceivedEvent.getSenderDisplayName()+": "+chatMessageReceivedEvent.getContent());
            }

            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    adapter.notifyDataSetChanged();
                }
            });

        });
    }

    private void receiveChatReceipt() {
        // <RECEIVE CHAT RECEIPT>
        // Start real time notification
        chatAsyncClient.startRealtimeNotifications(firstUserAccessToken, getApplicationContext());

        chatAsyncClient.addEventHandler(ChatEventType.READ_RECEIPT_RECEIVED, (ChatEvent payload) -> {
            ReadReceiptReceivedEvent readReceiptReceivedEvent = (ReadReceiptReceivedEvent) payload;
            Log.d("sdjasdkh",""+readReceiptReceivedEvent.getChatMessageId()+" getReadOn();-->"+readReceiptReceivedEvent.getReadOn());
        });



        chatAsyncClient.addEventHandler(ChatEventType.TYPING_INDICATOR_RECEIVED, (ChatEvent payload) -> {
            TypingIndicatorReceivedEvent typingIndicatorReceivedEvent = (TypingIndicatorReceivedEvent) payload;
            Log.d("sdjasdkh","TYPING "+typingIndicatorReceivedEvent.getChatThreadId()+" getVersion();-->"+typingIndicatorReceivedEvent.getVersion());
        });
    }

    private void retrieveAllMessagesAndUpdateAdapter() {
        try {
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
                        Log.d("djhskjdhs","hasAttachment->"+message.getMetadata().containsKey("hasAttachment"));
                        Log.d("djhskjdhs","attachmentUrl->"+message.getMetadata().get("attachmentUrl"));
                        allMessageList.add(new ChatModel(message.getSenderDisplayName()+": "+message.getContent().getMessage(),
                                message.getMetadata().containsKey("hasAttachment"), message.getMetadata().get("attachmentUrl")));
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
        final Map<String, String> metadata = new HashMap<>();
        metadata.put("hasAttachment", "true");
        metadata.put("attachmentUrl", "https://c8.alamy.com/comp/M9M2PK/drm-neon-sign-glowing-neon-sign-on-brickwall-wall-3d-rendered-royalty-free-stock-illustration-M9M2PK.jpg");
        SendChatMessageOptions options = new SendChatMessageOptions();
        options.setType(ChatMessageType.TEXT);
        options.setContent(messageBody.getText().toString());
        options.setSenderDisplayName(userDisplayName);
        options.setMetadata(metadata);

        chatThreadAsyncClient.sendMessage(options);
        messageBody.setText("");
    }
}