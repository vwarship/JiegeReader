package com.zaoqibu.jiegereader.util;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import com.zaoqibu.jiegereader.R;

public class Share
{
	public static void share(Context context, String title, String url)
	{
        final String appName = context.getResources().getString(R.string.app_name);
		String text = String.format("%s\n%s\n来自 [ %s ]", title, url, appName);

		try
		{
	    	Intent intent = new Intent(Intent.ACTION_SEND);
		    intent.setType("text/plain");
		    intent.putExtra(Intent.EXTRA_SUBJECT, title);
		    intent.putExtra(Intent.EXTRA_TEXT, text);

		    context.startActivity(Intent.createChooser(intent, appName));
	    }
	    catch (ActivityNotFoundException e)
	    {
	    	Toast.makeText(context, "您的手机上没有可分享的应用。", Toast.LENGTH_SHORT).show();
	    }
	}
	
}
