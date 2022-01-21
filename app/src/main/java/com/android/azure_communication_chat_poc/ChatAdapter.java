package com.android.azure_communication_chat_poc;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;


public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {
    private static final int LAYOUT_SENDER = 0;
    private static final int LAYOUT_RECEIVER = 1;
    Context context;
    ArrayList<ChatMessages> chatMessages;

    public ChatAdapter(ArrayList<ChatMessages> chatMessages) {
        this.chatMessages = chatMessages;
    }

    @Override
    public int getItemViewType(int position) {
        ChatMessages message = chatMessages.get(position);
        if (message.isFromMyEnd()) {
            return LAYOUT_SENDER;
        }
        return LAYOUT_RECEIVER;
    }

    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        this.context = parent.getContext();
        View view;
        if (viewType == LAYOUT_SENDER) {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_message_right, parent, false);
        } else {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_message_left, parent, false);
        }
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        ChatMessages message = chatMessages.get(position);
        holder.tvMessageFrom.setText(message.getSenderName() + ":");
        holder.tvMessage.setText(message.getMessage());
        holder.tvDate.setText(message.getDate());
    }

    @Override
    public int getItemCount() {
        return chatMessages.size();
    }

    public static class ChatViewHolder extends RecyclerView.ViewHolder {
        AppCompatTextView tvMessage, tvMessageFrom, tvDate;
        AppCompatImageView ivAttachment;

        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tv_message);
            tvDate = itemView.findViewById(R.id.tv_date);
            tvMessageFrom = itemView.findViewById(R.id.tv_message_from);
        }
    }
}