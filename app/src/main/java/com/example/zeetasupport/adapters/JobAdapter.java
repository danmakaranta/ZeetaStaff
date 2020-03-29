package com.example.zeetasupport.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.example.zeetasupport.R;
import com.example.zeetasupport.data.JobsInfo;

import java.util.ArrayList;

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

            listItemView = LayoutInflater.from(getContext()).inflate(R.layout.jobs_list_items, parent, false);
        }

        JobsInfo jobsInfo = getItem(position);

        // Find the TextView in the list_item.xml layout with the ID version_name
        TextView nameTextView = (TextView) listItemView.findViewById(R.id.job_serviceRendered_name);
        nameTextView.setText("Service Rendered to: " + jobsInfo.getName());

        // Find the TextView in the list_item.xml layout with the ID version_name
        TextView amountpaidTextView = (TextView) listItemView.findViewById(R.id.job_serviceRendered_AmountPaid);
        amountpaidTextView.setText("Amount Paid: " + jobsInfo.getAmountPaid());

        // Find the TextView in the list_item.xml layout with the ID version_name
        TextView phoneNumberTextView = (TextView) listItemView.findViewById(R.id.job_serviceRendered_phoneNumber);
        phoneNumberTextView.setText("Phone number: " + jobsInfo.getPhoneNumber());

        // Find the TextView in the list_item.xml layout with the ID version_name
        TextView dateTextView = (TextView) listItemView.findViewById(R.id.job_serviceRendered_date);
        dateTextView.setText("Amount Paid: " + jobsInfo.getDateRendered());

        View textContainer = listItemView.findViewById(R.id.job_details_container);

        // find the color
        int color = ContextCompat.getColor(getContext(), R.color.DarkGrey);

        // set the background color of the text view
        textContainer.setBackgroundColor(color);

        // Return the whole list item layout
        // so that it can be shown in the ListView
        return listItemView;

    }
}
