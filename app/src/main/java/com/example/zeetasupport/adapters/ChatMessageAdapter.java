package com.example.zeetasupport.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.example.zeetasupport.R;
import com.example.zeetasupport.data.Message;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

public class ChatMessageAdapter extends ArrayAdapter<Message> {

    public ChatMessageAdapter(@NonNull Context context, ArrayList<Message> messages, int resource) {
        super(context, 0, messages);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {

        View listItemView = convertView;
        if (listItemView == null) {
            listItemView = LayoutInflater.from(getContext()).inflate(R.layout.rider_message, parent, false);
        }
        Message message = getItem(position);

        TextView messageTxt = listItemView.findViewById(R.id.riderMessage);
        assert message != null;
        String messageContent = message.getContent();
        messageTxt.setText(messageContent);

        View textContainer = listItemView.findViewById(R.id.riderMessaeContainer);
        int color = ContextCompat.getColor(getContext(), R.color.White);
        textContainer.setBackgroundColor(color);

        return listItemView;
    }
}
