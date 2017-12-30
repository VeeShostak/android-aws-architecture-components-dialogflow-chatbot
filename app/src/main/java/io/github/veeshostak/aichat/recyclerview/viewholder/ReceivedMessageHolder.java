package io.github.veeshostak.aichat.recyclerview.viewholder;

import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import io.github.veeshostak.aichat.R;
import io.github.veeshostak.aichat.models.ChatMessage;

/**
 * Created by vladshostak on 12/28/17.
 */

public class ReceivedMessageHolder extends RecyclerView.ViewHolder {

    TextView messageText, timeText, nameText;
    ImageView profileImage;

    public ReceivedMessageHolder(View itemView) {
        super(itemView);

        messageText = (TextView) itemView.findViewById(R.id.message_body);
        timeText = (TextView) itemView.findViewById(R.id.message_time);
        nameText = (TextView) itemView.findViewById(R.id.message_name);
        //profileImage = (ImageView) itemView.findViewById(R.id.message_image_profile);
    }


    /*
    we implement a bind(object) method within the ViewHolder class. This gives view binding to the
    ViewHolder class rather than to onBindViewHolder. It therefore produces cleaner code amidst
    multiple ViewHolders and ViewTypes. It also allows us to add easily OnClickListeners, if necessary.
    */
    public void bind(ChatMessage message) {
        messageText.setText(message.getMessage());

        // Format the stored timestamp into a readable String using method.
        //timeText.setText(DateUtils.formatDateTime(messageText.getContext(), message.getCreatedAt(), DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_NUMERIC_DATE)); // pass UTC milliseconds

        timeText.setText(message.getCreatedAt());
        nameText.setText("Fiona");

    }

}
