package com.example.tawrun.chatbot;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

/**
 * Created by tawrun on 29/11/17.
 */

public class MessageReceived extends RecyclerView.ViewHolder {

    TextView mUserText,mBotText ;
    public MessageReceived(View itemView) {
        super(itemView);
        mUserText=(TextView)itemView.findViewById(R.id.sent_msg);
        mBotText=(TextView)itemView.findViewById(R.id.received_msg);
    }

}
