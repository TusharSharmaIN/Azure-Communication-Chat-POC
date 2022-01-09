package com.android.azure_communication_chat_poc;

import android.content.Context;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {
    ArrayList<ChatModel> chatMessages;
    Context context;
    public ChatAdapter(ArrayList<ChatModel> chatMessages) {
        this.chatMessages = chatMessages;
    }
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        this.context = parent.getContext();
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_chat_item, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        ChatModel chatModel = chatMessages.get(position);

        if (chatModel.getHasImage()) {
            holder.ivAttachment.setVisibility(View.VISIBLE);
            Glide.with(this.context).load(chatModel.getImageUrl()).into(holder.ivAttachment);
        } else {
            holder.ivAttachment.setVisibility(View.GONE);
        }
        holder.tvMessage.setText(boldPartOfString(0, chatModel.getTextMessage().indexOf(":"), chatModel.getTextMessage()));
    }

    @Override
    public int getItemCount() {
        return chatMessages.size();
    }

    public static class ChatViewHolder extends RecyclerView.ViewHolder {
        AppCompatTextView tvMessage;
        AppCompatImageView ivAttachment;

        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tv_message);
            ivAttachment = itemView.findViewById(R.id.iv_attachment);
        }
    }

    public String boldPartOfString(int start, int end, String text) {
        SpannableStringBuilder str = new SpannableStringBuilder(text);
        str.setSpan(new android.text.style.StyleSpan(android.graphics.Typeface.BOLD), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        return str.toString();
    }
}
