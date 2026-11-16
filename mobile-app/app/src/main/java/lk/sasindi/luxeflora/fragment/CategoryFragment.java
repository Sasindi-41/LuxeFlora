package lk.sasindi.luxeflora.fragment;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import lk.sasindi.luxeflora.R;
import lk.sasindi.luxeflora.adapter.CategoryAdapter;
import lk.sasindi.luxeflora.databinding.FragmentCategoryBinding;
import lk.sasindi.luxeflora.model.Category;

public class CategoryFragment extends Fragment {

    private FragmentCategoryBinding binding;
    private CategoryAdapter adapter;
    private List<Category> categoryList;
    private List<Category> filteredCategoryList;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentCategoryBinding.inflate(inflater, container,false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.recyclerViewCategories.setLayoutManager(new GridLayoutManager(getContext(),2));

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        categoryList = new ArrayList<>();
        filteredCategoryList = new ArrayList<>();

        db.collection("categories").get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {

                        if (task.isSuccessful() && task.getResult() != null) {

                            categoryList.clear();
                            categoryList.addAll(task.getResult().toObjects(Category.class));

                            filteredCategoryList.clear();
                            filteredCategoryList.addAll(categoryList);

                            adapter = new CategoryAdapter(filteredCategoryList, category -> {

                                Bundle bundle = new Bundle();
                                bundle.putString("categoryId", category.getCategoryId());

                                ListingFragment fragment = new ListingFragment();
                                fragment.setArguments(bundle);

                                getParentFragmentManager().beginTransaction()
                                        .replace(R.id.fragment_container, fragment)
                                        .addToBackStack(null)
                                        .commit();

                            }, R.layout.item_category);

                            binding.recyclerViewCategories.setAdapter(adapter);
                        }
                    }
                });
    }

    public void filterCategories(String query) {
        filteredCategoryList.clear();

        if (query.isEmpty()) {
            filteredCategoryList.addAll(categoryList);
        } else {
            String lowerQuery = query.toLowerCase();
            for (Category c : categoryList) {
                if (c.getName() != null && c.getName().toLowerCase().contains(lowerQuery)) {
                    filteredCategoryList.add(c);
                }
            }
        }

        if (adapter != null) adapter.notifyDataSetChanged();
    }

    public void sortCategories(boolean ascending) {
        Collections.sort(filteredCategoryList, (c1, c2) -> {
            if (c1.getName() == null) return -1;
            if (c2.getName() == null) return 1;
            return ascending ? c1.getName().compareToIgnoreCase(c2.getName())
                    : c2.getName().compareToIgnoreCase(c1.getName());
        });

        if (adapter != null) adapter.notifyDataSetChanged();
    }
}