package lk.sasindi.luxeflora.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lk.sasindi.luxeflora.R;
import lk.sasindi.luxeflora.activity.SignInActivity;
import lk.sasindi.luxeflora.model.Product;

public class ListingAdapter extends RecyclerView.Adapter<ListingAdapter.ViewHolder> {

    private List<Product> products;
    private OnProductClickListener listener;

    public ListingAdapter(List<Product> products, OnProductClickListener listener){
        this.products = products;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_listing, parent,false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        Product product = products.get(position);
        String productId = product.getProductId();
        Context context = holder.itemView.getContext();

        holder.productTitle.setText(product.getTitle());
        holder.productPrice.setText("LKR " + product.getPrice());

        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        if (product.getImages() != null && !product.getImages().isEmpty()) {
            Glide.with(context)
                    .load(product.getImages().get(0))
                    .centerCrop()
                    .into(holder.productImage);
        } else {
            holder.productImage.setImageResource(R.drawable.person_24px);
        }

        holder.itemView.setOnClickListener(view -> {
            Animation animation = AnimationUtils.loadAnimation(view.getContext(), R.anim.click_animation);
            view.startAnimation(animation);
            if (listener != null) listener.onClick(product);
        });

        if (product.getStockCount() <= 0) {
            holder.addToCart.setEnabled(false);
            holder.addToCart.setText("Out of Stock");
        } else {
            holder.addToCart.setEnabled(true);
            holder.addToCart.setText("Add to Cart");

            holder.addToCart.setOnClickListener(v -> {

                if (auth.getCurrentUser() == null){
                    Intent intent = new Intent(v.getContext(), SignInActivity.class);
                    v.getContext().startActivity(intent);
                    Toast.makeText(v.getContext(), "Please sign in first", Toast.LENGTH_SHORT).show();
                    return;
                }

                String uid = auth.getCurrentUser().getUid();

                Map<String, Object> cartMap = new HashMap<>();
                cartMap.put("productId", productId);
                cartMap.put("quantity", 1);

                db.collection("users")
                        .document(uid)
                        .collection("cart")
                        .whereEqualTo("productId", productId)
                        .get()
                        .addOnSuccessListener(query -> {
                            if (!query.isEmpty()) {
                                DocumentSnapshot ds = query.getDocuments().get(0);
                                Long currentQty = ds.getLong("quantity");
                                if (currentQty != null){
                                    ds.getReference().update("quantity", currentQty + 1);
                                }
                                Toast.makeText(v.getContext(), "Quantity updated", Toast.LENGTH_SHORT).show();
                            } else {
                                db.collection("users")
                                        .document(uid)
                                        .collection("cart")
                                        .add(cartMap)
                                        .addOnSuccessListener(doc -> {
                                            Toast.makeText(v.getContext(), "Added to cart", Toast.LENGTH_SHORT).show();
                                        });
                            }
                        });
            });
        }

        holder.wishlist.setOnClickListener(v -> {
            if (auth.getCurrentUser() == null){
                Intent intent = new Intent(v.getContext(), SignInActivity.class);
                v.getContext().startActivity(intent);
                Toast.makeText(v.getContext(), "Please sign in first", Toast.LENGTH_SHORT).show();
                return;
            }

            String uid = auth.getCurrentUser().getUid();

            db.collection("users")
                    .document(uid)
                    .collection("wishlist")
                    .document(productId)
                    .get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            db.collection("users")
                                    .document(uid)
                                    .collection("wishlist")
                                    .document(productId)
                                    .delete()
                                    .addOnSuccessListener(unused -> {
                                        holder.wishlist.setImageResource(R.drawable.favorite_24px);
                                        Toast.makeText(v.getContext(), "Removed from wishlist", Toast.LENGTH_SHORT).show();
                                    });
                        } else {
                            Map<String, Object> data = new HashMap<>();
                            data.put("productId", productId);
                            data.put("timestamp", System.currentTimeMillis());
                            db.collection("users")
                                    .document(uid)
                                    .collection("wishlist")
                                    .document(productId)
                                    .set(data)
                                    .addOnSuccessListener(unused -> {
                                        holder.wishlist.setImageResource(R.drawable.favorite_fill_24px);
                                        Toast.makeText(v.getContext(), "Added to wishlist", Toast.LENGTH_SHORT).show();
                                    });
                        }
                    });
        });

        if (auth.getCurrentUser() != null){
            String uid = auth.getCurrentUser().getUid();
            db.collection("users")
                    .document(uid)
                    .collection("wishlist")
                    .document(productId)
                    .get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            holder.wishlist.setImageResource(R.drawable.favorite_fill_24px);
                        } else {
                            holder.wishlist.setImageResource(R.drawable.favorite_24px);
                        }
                    });
        } else {
            holder.wishlist.setImageResource(R.drawable.favorite_24px);
        }
    }

    @Override
    public int getItemCount() {
        return products.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder{

        ImageView productImage;
        ImageView wishlist;
        TextView productTitle;
        TextView productPrice;
        MaterialButton addToCart;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            productImage = itemView.findViewById(R.id.listing_item_image);
            wishlist = itemView.findViewById(R.id.listing_item_wishlist);
            productTitle = itemView.findViewById(R.id.listing_item_name);
            productPrice = itemView.findViewById(R.id.listing_item_price);
            addToCart = itemView.findViewById(R.id.add_to_cart);
        }
    }

    public interface OnProductClickListener {
        void onClick(Product product);
    }


}