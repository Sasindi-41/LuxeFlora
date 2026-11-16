package lk.sasindi.luxeflora.adapter;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.List;
import java.util.Locale;

import lk.sasindi.luxeflora.R;
import lk.sasindi.luxeflora.model.Order;

public class OrdersAdapter extends RecyclerView.Adapter<OrdersAdapter.OrderViewHolder> {

    private Context context;
    private List<Order> orderList;

    FirebaseStorage storage = FirebaseStorage.getInstance();

    public OrdersAdapter(Context context, List<Order> orderList) {
        this.context = context;
        this.orderList = orderList;
    }

    @NonNull
    @Override
    public OrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_order, parent, false);
        return new OrderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderViewHolder holder, int position) {

        Order order = orderList.get(position);

        holder.orderId.setText("Order #" + order.getOrderId());
        holder.orderStatus.setText(order.getStatus());

        if (order.getOrderItems() != null && !order.getOrderItems().isEmpty()) {

            Order.OrderItem item = order.getOrderItems().get(0);

            holder.quantity.setText(
                    String.valueOf(order.getTotalQuantity())
            );

            holder.price.setText(
                    String.format(Locale.US, "LKR %,.2f", order.getTotalAmount())
            );

        } else {
            holder.quantity.setText("Qty: 0");
            holder.price.setText("$0");
        }

        if (order.getOrderDate() != null) {
            holder.deliveryDate.setText(order.getOrderDate().toDate().toString());
        } else {
            holder.deliveryDate.setText("No Date");
        }

        String status = order.getStatus();

        if (status != null) {
            switch (status) {
                case "Pending":
                    holder.orderStatus.setBackgroundResource(R.drawable.bg_status_pending);
                    break;
                case "Shipped":
                    holder.orderStatus.setBackgroundResource(R.drawable.bg_status_progress);
                    break;
                case "Delivered":
                    holder.orderStatus.setBackgroundResource(R.drawable.bg_status_completed);
                    break;
                default:
                    holder.orderStatus.setBackgroundResource(R.drawable.bg_status_pending);
                    break;
            }
        }

        holder.btnDetails.setOnClickListener(v -> showOrderDialog(order));
    }

    @Override
    public int getItemCount() {
        return orderList.size();
    }

    public static class OrderViewHolder extends RecyclerView.ViewHolder {

        TextView orderId, productName, price, quantity, deliveryDate, orderStatus;
        ImageView productImage;
        Button btnDetails;

        public OrderViewHolder(@NonNull View itemView) {
            super(itemView);

            orderId = itemView.findViewById(R.id.orderId);
            price = itemView.findViewById(R.id.price);
            quantity = itemView.findViewById(R.id.quantity);
            deliveryDate = itemView.findViewById(R.id.deliveryDate);
            orderStatus = itemView.findViewById(R.id.orderStatus);
            productImage = itemView.findViewById(R.id.productImage);
            btnDetails = itemView.findViewById(R.id.btnDetails);
        }
    }

    private void showOrderDialog(Order order) {

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View view = LayoutInflater.from(context)
                .inflate(R.layout.dialog_order_details, null);

        TextView orderId = view.findViewById(R.id.dialogOrderId);
        TextView total = view.findViewById(R.id.dialogTotal);
        LinearLayout container = view.findViewById(R.id.productContainer);

        orderId.setText("Order #" + order.getOrderId());

        total.setText(String.format(Locale.US,
                "Total: LKR %,.2f", order.getTotalAmount()));

        if (order.getOrderItems() != null) {

            for (Order.OrderItem item : order.getOrderItems()) {

                View itemView = LayoutInflater.from(context)
                        .inflate(R.layout.item_dialog_product, container, false);

                ImageView img = itemView.findViewById(R.id.img);
                TextView name = itemView.findViewById(R.id.name);
                TextView qty = itemView.findViewById(R.id.qty);
                TextView price = itemView.findViewById(R.id.price);

                name.setText(item.getProductName());
                qty.setText("Qty: " + item.getQuantity());

                double totalPrice = item.getUnitPrice() * item.getQuantity();
                price.setText(String.format(Locale.US, "LKR %,.2f", totalPrice));

                String imageUrl = item.getProductImage();

                if (imageUrl != null && !imageUrl.isEmpty()) {

                    Glide.with(context)
                            .load(imageUrl)
                            .placeholder(R.drawable.orders_24px)
                            .into(img);

                } else {
                    img.setImageResource(R.drawable.orders_24px);
                }

                container.addView(itemView);
            }
        }

        builder.setView(view);
        builder.setPositiveButton("Close", (d, w) -> d.dismiss());
        AlertDialog dialog = builder.create();
        dialog.getWindow().setBackgroundDrawableResource(R.drawable.dialog_bg);
        dialog.show();
    }
}