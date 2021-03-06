package com.zaoqibu.jiegereader.db;

import android.provider.BaseColumns;

/**
 * Created by vwarship on 2015/3/6.
 */
public class Reader {

    public static final class Newses implements BaseColumns {
        private Newses() {}
        public static final String TABLE_NAME = "newses";

        public static final String COLUMN_NAME_TITLE = "title";
        public static final String COLUMN_NAME_LINK = "link";
        public static final String COLUMN_NAME_SOURCE = "source";
        public static final String COLUMN_NAME_DESCRIPTION = "description";
        public static final String COLUMN_NAME_PUB_DATE = "pub_date";
        public static final String COLUMN_NAME_CREATE_DATE = "created_date";
        public static final String COLUMN_NAME_STATE = "state";
        public static final String COLUMN_NAME_RSS_ID = "rss_id";

        public enum StateValue {
            Unread(0), Readed(1), Deleted(2);
            private int value;
            StateValue(int value) {
                this.value = value;
            }

            public int getValue() {
                return value;
            }
        }
    }

    public static final class Rsses implements BaseColumns {
        private Rsses() {}
        public static final String TABLE_NAME = "rsses";

        public static final String COLUMN_NAME_TITLE = "title";
        public static final String COLUMN_NAME_LINK = "link";
        public static final String COLUMN_NAME_IS_FEED = "is_feed";
        public static final String COLUMN_NAME_CREATE_DATE = "created_date";
        public static final String COLUMN_NAME_UPDATE_DATE = "updated_date";
    }

}
