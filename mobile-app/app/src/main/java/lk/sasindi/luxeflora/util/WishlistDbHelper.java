package lk.sasindi.luxeflora.util;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class WishlistDbHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "wishlist.db";
    private static final int DB_VERSION = 1;

    public static final String TABLE = "wishlist";

    public WishlistDbHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        String CREATE_TABLE = "CREATE TABLE " + TABLE + " (" +
                "productId TEXT PRIMARY KEY," +
                "title TEXT," +
                "price REAL," +
                "image TEXT" +
                ")";

        db.execSQL(CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}