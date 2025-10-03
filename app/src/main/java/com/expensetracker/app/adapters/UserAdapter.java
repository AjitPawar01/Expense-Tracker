package com.expensetracker.app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.expensetracker.app.R;
import com.expensetracker.app.models.User;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import com.google.android.material.switchmaterial.SwitchMaterial;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {
    private List<User> users;
    private OnUserClickListener listener;

    public interface OnUserClickListener {
        void onUserClick(User user);
        void onToggleStatus(User user);
    }

    public UserAdapter(List<User> users, OnUserClickListener listener) {
        this.users = users;
        this.listener = listener;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_user, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        User user = users.get(position);
        holder.bind(user);
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    class UserViewHolder extends RecyclerView.ViewHolder {
        private TextView  tvUsername, tvEmail, tvRole, tvBalance, tvCreatedDate;
        private SwitchMaterial switchStatus;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            //ivUserProfile = itemView.findViewById(R.id.ivUserProfile);
            tvUsername = itemView.findViewById(R.id.tvUsername);
            tvEmail = itemView.findViewById(R.id.tvEmail);
            tvRole = itemView.findViewById(R.id.tvRole);
            tvBalance = itemView.findViewById(R.id.tvBalance);
            tvCreatedDate = itemView.findViewById(R.id.tvCreatedDate);
            switchStatus = itemView.findViewById(R.id.switchStatus); // This will now work

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onUserClick(users.get(position));
                }
            });

            switchStatus.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onToggleStatus(users.get(position));
                    // Reset switch to current state (will be updated after server response)
                    switchStatus.setChecked(users.get(position).isActive());
                }
            });
        }

        public void bind(User user) {
            tvUsername.setText(user.getUsername());
            tvEmail.setText(user.getEmail());

            // Set role with appropriate styling
            tvRole.setText(user.getRole());
            if ("ADMIN".equals(user.getRole())) {
                tvRole.setBackgroundResource(R.drawable.bg_admin_tag);
            } else {
                tvRole.setBackgroundResource(R.drawable.bg_user_tag);
            }

            // Set balance
            tvBalance.setText(String.format(Locale.getDefault(), "₹%.2f", user.getCurrentBalance()));

            // Set created date
            SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            Date createdDate = new Date(user.getCreatedAt());
            tvCreatedDate.setText("Joined: " + dateFormat.format(createdDate));

            // Set status switch
            switchStatus.setChecked(user.isActive());
            switchStatus.setText(user.isActive() ? "Active" : "Inactive");

            // Change text color based on status
            if (user.isActive()) {
                tvUsername.setTextColor(itemView.getContext().getResources().getColor(android.R.color.black));
                tvEmail.setTextColor(itemView.getContext().getResources().getColor(android.R.color.darker_gray));
            } else {
                tvUsername.setTextColor(itemView.getContext().getResources().getColor(android.R.color.darker_gray));
                tvEmail.setTextColor(itemView.getContext().getResources().getColor(android.R.color.darker_gray));
            }
        }
    }
}