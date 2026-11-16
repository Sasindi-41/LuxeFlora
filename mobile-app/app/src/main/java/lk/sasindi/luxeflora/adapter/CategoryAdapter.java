package lk.sasindi.luxeflora.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

import lk.sasindi.luxeflora.R;
import lk.sasindi.luxeflora.model.Category;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.ViewHolder> {

    private List<Category> categories;
    private OnCategoryClickListener listener;
    private int layoutId;

    // constructor (UNCHANGED)
    public CategoryAdapter(List<Category> categories, OnCategoryClickListener listener, int layoutId){
        this.categories = categories;
        this.listener = listener;
        this.layoutId = layoutId;
    }

    @NonNull
    @Override
    public CategoryAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(parent.getContext())
                .inflate(layoutId, parent,false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryAdapter.ViewHolder holder, int position) {

        Category category = categories.get(position);
        holder.categoryName.setText(category.getName());

        if (category.getImageUrl() != null && !category.getImageUrl().isEmpty()) {

            Glide.with(holder.itemView.getContext())
                    .load(category.getImageUrl())
                    .centerCrop()
                    .into(holder.categoryImage);

        } else {
            holder.categoryImage.setImageResource(R.drawable.person_24px);
        }

        holder.itemView.setOnClickListener(view -> {

            Animation animation = AnimationUtils.loadAnimation(view.getContext(),R.anim.click_animation);
            view.startAnimation(animation);

            if (listener != null){
                listener.onCategoryClick(category);
            }
        });
    }

    @Override
    public int getItemCount() {
        return categories.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder{
        ImageView categoryImage;
        TextView categoryName;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            categoryImage = itemView.findViewById(R.id.category_image);
            categoryName = itemView.findViewById(R.id.category_name);
        }
    }

    public interface OnCategoryClickListener{
        void onCategoryClick(Category category);
    }
}