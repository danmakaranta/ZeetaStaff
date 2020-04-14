package com.example.zeetasupport.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import com.example.zeetasupport.R;
import com.example.zeetasupport.data.CompletedJobs;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

public class CompletedJobsAdapter extends ArrayAdapter<CompletedJobs> {

    public CompletedJobsAdapter(@NonNull Context context, ArrayList<CompletedJobs> jobsInfos, int resource) {
        super(context, 0, jobsInfos);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View listItemView = convertView;
        if (listItemView == null) {

            listItemView = LayoutInflater.from(getContext()).inflate(R.layout.jobs_list, parent, false);
        }

        CompletedJobs jobsInfo = getItem(position);


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
