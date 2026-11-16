package lk.sasindi.luxeflora.adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import lk.sasindi.luxeflora.R;
import lk.sasindi.luxeflora.model.Product;

public class WishlistAdapter extends RecyclerView.Adapter<WishlistAdapter.ViewHolder> {

    private List<Product> products;

    public interface OnItemClickListener {
        void onClick(Product product);
    }

    public interface OnRemoveListener {
        void onRemove(int position);
    }

    private OnItemClickListener clickListener;
    private OnRemoveListener removeListener;

    public WishlistAdapter(List<Product> products) {
        this.products = products;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.clickListener = listener;
    }

    public void setOnRemoveListener(OnRemoveListener listener) {
        this.removeListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_wishlist, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        Product product = products.get(position);

        holder.title.setText(product.getTitle());
        holder.price.setText(String.format(Locale.US, "LKR %,.2f", product.getPrice()));

        String imageUrl = null;

        if (product.getImages() != null && !product.getImages().isEmpty()) {
            imageUrl = product.getImages().get(0);
        }

        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(holder.itemView)
                    .load(imageUrl)
                    .centerCrop()
                    .into(holder.image);
        }

        if (product.getStockCount() > 0) {
            holder.stock.setText("In Stock");
            holder.stock.setTextColor(Color.parseColor("#4CAF50"));
        } else {
            holder.stock.setText("Out of Stock");
            holder.stock.setTextColor(Color.parseColor("#F44336"));
        }


        if (product.getStockCount() <= 0) {
            holder.addToCart.setEnabled(false);
            holder.addToCart.setAlpha(0.5f);
        } else {
            holder.addToCart.setEnabled(true);
            holder.addToCart.setAlpha(1f);
        }

        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onClick(product);
            }
        });

        holder.remove.setOnClickListener(v -> {
            if (removeListener != null) {
                removeListener.onRemove(position);
            }
        });

        holder.addToCart.setOnClickListener(v -> {

            if (product.getStockCount() <= 0) {
                Toast.makeText(v.getContext(), "Out of stock", Toast.LENGTH_SHORT).show();
                return;
            }

            FirebaseAuth auth = FirebaseAuth.getInstance();

            if (auth.getCurrentUser() == null) {
                Toast.makeText(v.getContext(), "Please sign in first", Toast.LENGTH_SHORT).show();
                return;
            }

            String uid = auth.getCurrentUser().getUid();
            FirebaseFirestore db = FirebaseFirestore.getInstance();

            db.collection("users")
                    .document(uid)
                    .collection("cart")
                    .whereEqualTo("productId", product.getProductId())
                    .get()
                    .addOnSuccessListener(query -> {

                        if (!query.isEmpty()) {

                            Toast.makeText(v.getContext(),
                                    "Already in cart",
                                    Toast.LENGTH_SHORT).show();

                        } else {

                            Map<String, Object> cartMap = new HashMap<>();
                            cartMap.put("productId", product.getProductId());
                            cartMap.put("quantity", 1);

                            db.collection("users")
                                    .document(uid)
                                    .collection("cart")
                                    .add(cartMap)
                                    .addOnSuccessListener(doc -> {
                                        Toast.makeText(v.getContext(),
                                                "Added to cart from wishlist",
                                                Toast.LENGTH_SHORT).show();
                                    });
                        }
                    });
        });
    }

    @Override
    public int getItemCount() {
        return products.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        ImageView image;
        TextView title, price, stock;
        ImageView remove;
        MaterialButton addToCart;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            image = itemView.findViewById(R.id.item_wishlist_image);
            title = itemView.findViewById(R.id.item_wishlist_title);
            price = itemView.findViewById(R.id.item_wishlist_price);
            stock = itemView.findViewById(R.id.item_wishlist_stock);
            remove = itemView.findViewById(R.id.item_wishlist_remove);
            addToCart = itemView.findViewById(R.id.item_wishlist_move_to_cart);
        }
    }
}