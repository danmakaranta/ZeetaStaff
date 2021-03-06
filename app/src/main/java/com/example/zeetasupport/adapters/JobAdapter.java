package com.example.zeetasupport.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.example.zeetasupport.R;
import com.example.zeetasupport.data.JobsInfo;
import com.google.firebase.Timestamp;

import java.util.ArrayList;
import java.util.Date;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

public class JobAdapter extends ArrayAdapter<JobsInfo> {


    public JobAdapter(@NonNull Context context, ArrayList<JobsInfo> jobsInfos, int resource) {
        super(context, 0, jobsInfos);
    }


    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {

        View listItemView = convertView;
        if (listItemView == null) {
            listItemView = LayoutInflater.from(getContext()).inflate(R.layout.job_list_item, parent, false);
        }

        JobsInfo jobsInfo = getItem(position);

        // Find the TextView in the list_item.xml layout with the ID version_name
        TextView nameTextView = (TextView) listItemView.findViewById(R.id.job_client_name);
        assert jobsInfo != null;
        String nameTemp = "" + jobsInfo.getName();
        nameTextView.setText(nameTemp);

        // Find the TextView in the list_item.xml layout with the ID version_name
        TextView dateTextView = (TextView) listItemView.findViewById(R.id.job_date);
        Timestamp tp = jobsInfo.getDateRendered();
        if (tp != null) {
            Date dt = tp.toDate();
            dateTextView.setText("Date: " + dt);
        } else {
            dateTextView.setText("Date: ");
        }


        TextView statusTextView = (TextView) listItemView.findViewById(R.id.job_status);
        String status = jobsInfo.getStatus();
        String statusTxt = "Status: " + jobsInfo.getStatus();
        if (status.equalsIgnoreCase("Ongoing")) {
            int colorStatus = ContextCompat.getColor(getContext(), R.color.red3);
            statusTextView.setText(statusTxt);
            statusTextView.setTextColor(colorStatus);
        } else if (status.equalsIgnoreCase("Closed")) {
            int colorStatus = ContextCompat.getColor(getContext(), R.color.orange1);
            statusTextView.setTextColor(colorStatus);
            statusTextView.setText(statusTxt);
        } else {
            int colorStatus = ContextCompat.getColor(getContext(), R.color.green1);
            statusTextView.setTextColor(colorStatus);
            statusTextView.setText(statusTxt);
        }


        //Refresh views
        nameTextView.invalidate();

        View textContainer = listItemView.findViewById(R.id.job_list_container);

        // find the color
        int color = ContextCompat.getColor(getContext(), R.color.White);

        // set the background color of the text view
        textContainer.setBackgroundColor(color);
        // Return the whole list item layout
        // so that it can be shown in the ListView
        return listItemView;

    }
}
