package lk.sasindi.luxeflora.util;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;

public class WishlistManager {

    private static final String PREF_NAME = "wishlist_pref";
    private static final String KEY_LIST = "wishlist_items";

    private SharedPreferences prefs;

    public WishlistManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void add(String productId) {
        Set<String> list = new HashSet<>(getAll());
        list.add(productId);
        prefs.edit().putStringSet(KEY_LIST, list).apply();
    }

    public void remove(String productId) {
        Set<String> list = new HashSet<>(getAll());
        list.remove(productId);
        prefs.edit().putStringSet(KEY_LIST, list).apply();
    }

    public boolean isAdded(String productId) {
        return getAll().contains(productId);
    }

    public Set<String> getAll() {
        return new HashSet<>(prefs.getStringSet(KEY_LIST, new HashSet<>()));
    }
}