package io.github.veeshostak.aichat.recyclerview.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

import io.github.veeshostak.aichat.R;
import io.github.veeshostak.aichat.models.ChatMessage;
import io.github.veeshostak.aichat.recyclerview.viewholder.ReceivedMessageHolder;
import io.github.veeshostak.aichat.recyclerview.viewholder.SentMessageHolder;

/**
 * Created by vladshostak on 12/28/17.
 */

public class MessageListAdapter extends RecyclerView.Adapter {

    private static final int VIEW_TYPE_MESSAGE_SENT = 1;
    private static final int VIEW_TYPE_MESSAGE_RECEIVED = 2;

    private Context mContext;
    private List<ChatMessage> mMessageList;

    public MessageListAdapter(Context context, List<ChatMessage> messageList) {
        mContext = context;
        mMessageList = messageList;
    }

    public void add(ChatMessage message) {
        mMessageList.add(message);
        notifyDataSetChanged();
    }

    public void addItems(List<ChatMessage> chatMessages) {
        this.mMessageList = chatMessages;
        notifyDataSetChanged();
    }

    public void clearMessages() {
        mMessageList.clear();
    }



    @Override
    public int getItemCount() {
        return mMessageList.size();
    }

    // Return the view type of the item at position for the purposes of view recycling.
    @Override
    public int getItemViewType(int position) {
        ChatMessage message = (ChatMessage) mMessageList.get(position);

        // Determines the appropriate ViewType according to the sender of the message.
        if (message.getIsMe()) {
            // If the current user is the sender of the message
            return VIEW_TYPE_MESSAGE_SENT;
        } else {
            // If some other user sent the message
            return VIEW_TYPE_MESSAGE_RECEIVED;
        }
    }

    /* The onCreateViewHolder() function is where a new, empty view (wrapped by a
       RecyclerView.ViewHolder) is created and added to the pool of views. */
    // Inflates the appropriate layout according to the ViewType.
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view;
        if (viewType == VIEW_TYPE_MESSAGE_SENT) {
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_sent, parent, false);
            return new SentMessageHolder(view);
        } else if (viewType == VIEW_TYPE_MESSAGE_RECEIVED) {
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_recieved, parent, false);
            return new ReceivedMessageHolder(view);
        }
        return null;
    }

    // The onBindViewHolder() function gets a view from the empty pool and populates this view
    // using the data you supplied to the adapter.

    // Passes the message object to a ViewHolder so that the contents can be bound to UI.
    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        ChatMessage message = (ChatMessage) mMessageList.get(position);

        switch (holder.getItemViewType()) {
            case VIEW_TYPE_MESSAGE_SENT:
                ((SentMessageHolder) holder).bind(message);
                break;
            case VIEW_TYPE_MESSAGE_RECEIVED:
                ((ReceivedMessageHolder) holder).bind(message);
                break;
        }
    }

    // You can use the onViewRecycled() method to perform specific actions like setting an
    // ImageView's bitmap to null (on detach) in order to reduce memory usage.

}
