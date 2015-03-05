package com.zaoqibu.jiegereader.util;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Created by vwarship on 2015/3/5.
 */
public class DateUtil {
    private final long ONE_MINUTE_MS = 1000 * 60;
    private final long ONE_HOUR_MS = ONE_MINUTE_MS * 60;
    private final long ONE_DAY_MS = ONE_HOUR_MS * 24;
    private final long YESTERDAY_MS = ONE_DAY_MS * 2;

    private String[] datePatterns = {
            "EEE, d MMM yyyy HH:mm:ss Z",
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd'T'HH:mm:ss'Z'"
    };

    public String timeToReadable(long time) {
        final long curTimeMS = new Date().getTime();
        final long timeInterval = curTimeMS - time;

        if (timeInterval < ONE_MINUTE_MS)
            return "刚刚";
        else if (timeInterval < ONE_HOUR_MS)
            return String.format("%d分钟前", timeInterval / ONE_MINUTE_MS);
        else if (timeInterval < ONE_DAY_MS)
            return String.format("%d小时前", timeInterval / ONE_HOUR_MS);
        else if (timeInterval < YESTERDAY_MS)
            return String.format("昨天", timeInterval / ONE_DAY_MS);

        return dateFormat(time, "MM-dd HH:mm");
    }

    public String dateFormat(long time, String datePattern) {
        DateFormat dateFormat = new SimpleDateFormat(datePattern, Locale.US);
        return dateFormat.format(new Date(time));
    }

    public long dateParse(String dateString) {
        long timestamp = 0;

        for (String datePattern : datePatterns) {
            try {
                timestamp = dateParse(dateString, datePattern);
                break;
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }

        return timestamp;
    }

    /*
        "Wed, 04 Mar 2015 18:00:00 +0800"   "EEE, d MMM yyyy HH:mm:ss Z"
        "Wed, 04 Mar 2015 05:41:17 GMT"     "EEE, d MMM yyyy HH:mm:ss Z"
        "2015-03-04 18:35:54"               "yyyy-MM-dd HH:mm:ss"
        "2015-03-04T05:41:17Z"              "yyyy-MM-dd'T'HH:mm:ss'Z'"
    */
    private long dateParse(String dateString, String datePattern) throws ParseException {
        DateFormat dateFormat = new SimpleDateFormat(datePattern, Locale.US);
        Date date = dateFormat.parse(dateString);

        return date.getTime();
    }
}
