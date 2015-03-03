package com.zaoqibu.jiegereader;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

/**
 * Created by vwarship on 2015/3/4.
 */
public class SubscriptionArrayAdapter extends ArrayAdapter<Subscription> {
    private int resource;

    public SubscriptionArrayAdapter(Context context, int resource) {
        super(context, resource);
        this.resource = resource;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;

        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(getContext());
            view = inflater.inflate(resource, parent, false);
        }

        Subscription subscription = getItem(position);

        TextView tvSubscriptionName = (TextView)view.findViewById(R.id.tvSubscriptionName);
        tvSubscriptionName.setText(subscription.getName());

        return view;
    }
}
