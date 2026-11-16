package lk.sasindi.luxeflora.util;

import android.content.Context;
import android.content.SharedPreferences;

public class SeenManager {

    private static final String PREF = "chat_pref";
    private static final String KEY_LAST_SEEN = "last_seen";

    public static void setLastSeen(Context context, long time) {
        SharedPreferences pref = context.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        pref.edit().putLong(KEY_LAST_SEEN, time).apply();
    }

    public static long getLastSeen(Context context) {
        SharedPreferences pref = context.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        return pref.getLong(KEY_LAST_SEEN, 0);
    }
}