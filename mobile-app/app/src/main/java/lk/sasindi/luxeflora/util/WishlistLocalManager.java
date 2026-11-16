package lk.sasindi.luxeflora.util;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

import lk.sasindi.luxeflora.model.Product;

public class WishlistLocalManager {

    private WishlistDbHelper dbHelper;

    public WishlistLocalManager(Context context) {
        dbHelper = new WishlistDbHelper(context);
    }

    public void insert(Product product) {

        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put("productId", product.getProductId());
        values.put("title", product.getTitle());
        values.put("price", product.getPrice());

        String image = "";
        if (product.getImages() != null && !product.getImages().isEmpty()) {
            image = product.getImages().get(0);
        }
        values.put("image", image);

        db.insertWithOnConflict(
                WishlistDbHelper.TABLE,
                null,
                values,
                SQLiteDatabase.CONFLICT_REPLACE
        );
    }

    public List<Product> getAll() {

        List<Product> list = new ArrayList<>();

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + WishlistDbHelper.TABLE, null);

        while (cursor.moveToNext()) {

            Product p = new Product();
            p.setProductId(cursor.getString(0));
            p.setTitle(cursor.getString(1));
            p.setPrice(cursor.getDouble(2));

            List<String> images = new ArrayList<>();
            images.add(cursor.getString(3));
            p.setImages(images);

            list.add(p);
        }

        cursor.close();
        return list;
    }

    public void delete(String productId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete(WishlistDbHelper.TABLE, "productId=?", new String[]{productId});
    }

    public void clear() {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete(WishlistDbHelper.TABLE, null, null);
    }
}