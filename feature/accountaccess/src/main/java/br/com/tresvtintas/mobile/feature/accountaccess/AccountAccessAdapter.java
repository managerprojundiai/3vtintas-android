package br.com.tresvtintas.mobile.feature.accountaccess;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import br.com.tresvtintas.mobile.core.accountaccess.AccountAccessEntry;
import br.com.tresvtintas.mobile.core.accountaccess.AccountAccessStatus;
import br.com.tresvtintas.mobile.feature.accountaccess.databinding.AccountAccessItemBinding;
import java.util.Objects;
import java.util.Optional;

final class AccountAccessAdapter extends ListAdapter<
        AccountAccessRow,
        AccountAccessAdapter.Holder> {
    private static final DiffUtil.ItemCallback<AccountAccessRow>
            DIFFERENCE = new DiffUtil.ItemCallback<>() {
                @Override
                public boolean areItemsTheSame(
                        @NonNull AccountAccessRow oldItem,
                        @NonNull AccountAccessRow newItem) {
                    return oldItem.entry().getClass()
                                    == newItem.entry().getClass()
                            && oldItem.entry().id().equals(
                                    newItem.entry().id());
                }

                @Override
                public boolean areContentsTheSame(
                        @NonNull AccountAccessRow oldItem,
                        @NonNull AccountAccessRow newItem) {
                    return oldItem.equals(newItem);
                }
            };

    @FunctionalInterface
    interface RevocationListener {
        void onRevoke(AccountAccessEntry entry);
    }

    private final RevocationListener listener;
    private final boolean revocationEnabled;
    private Optional<String> busyEntryId = Optional.empty();

    AccountAccessAdapter(RevocationListener listener) {
        this(listener, true);
    }

    AccountAccessAdapter(
            RevocationListener listener,
            boolean revocationEnabled) {
        super(DIFFERENCE);
        this.listener = Objects.requireNonNull(
                listener,
                "Account access revocation listener is required.");
        this.revocationEnabled = revocationEnabled;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType) {
        return new Holder(AccountAccessItemBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false));
    }

    @Override
    public void onBindViewHolder(
            @NonNull Holder holder,
            int position) {
        holder.bind(
                getItem(position),
                listener,
                revocationEnabled,
                busyEntryId);
    }

    void setBusyEntryId(Optional<String> entryId) {
        Optional<String> next = Objects.requireNonNull(
                entryId,
                "Busy account access ID is required.");
        if (busyEntryId.equals(next)) {
            return;
        }
        Optional<String> previous = busyEntryId;
        busyEntryId = next;
        previous.map(this::positionOf)
                .filter(position -> position >= 0)
                .ifPresent(this::notifyItemChanged);
        next.map(this::positionOf)
                .filter(position -> position >= 0)
                .filter(position -> previous
                        .map(this::positionOf)
                        .filter(position::equals)
                        .isEmpty())
                .ifPresent(this::notifyItemChanged);
    }

    private int positionOf(String entryId) {
        for (int index = 0; index < getCurrentList().size(); index++) {
            if (getCurrentList().get(index).entry().id()
                    .equals(entryId)) {
                return index;
            }
        }
        return RecyclerView.NO_POSITION;
    }

    static final class Holder extends RecyclerView.ViewHolder {
        private final AccountAccessItemBinding binding;

        Holder(AccountAccessItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(
                AccountAccessRow row,
                RevocationListener listener,
                boolean revocationEnabled,
                Optional<String> busyEntryId) {
            var context = binding.getRoot().getContext();
            AccountAccessEntry entry = row.entry();
            binding.accountAccessItemTitle.setText(
                    AccountAccessText.title(context, entry));
            binding.accountAccessItemStatus.setText(
                    AccountAccessText.status(entry.status()));
            binding.accountAccessItemCurrent.setVisibility(
                    entry.current() ? View.VISIBLE : View.GONE);
            binding.accountAccessItemDetails.setText(
                    AccountAccessText.details(context, entry));
            binding.accountAccessItemLastSeen.setText(
                    AccountAccessText.lastSeen(context, entry));
            binding.accountAccessItemTiming.setText(
                    AccountAccessText.timing(context, entry));
            boolean active =
                    entry.status() == AccountAccessStatus.ACTIVE;
            boolean revoking = row.revoking()
                    || busyEntryId.filter(entry.id()::equals).isPresent();
            binding.accountAccessItemRevoke.setVisibility(
                    active && revocationEnabled
                            ? View.VISIBLE
                            : View.GONE);
            binding.accountAccessItemRevoke.setEnabled(
                    active && !revoking && busyEntryId.isEmpty());
            binding.accountAccessItemRevoke.setText(
                    revoking
                            ? R.string.account_access_revoking
                            : AccountAccessText.revokeLabel(entry));
            binding.accountAccessItemRevoke.setOnClickListener(
                    ignored -> listener.onRevoke(entry));
            binding.getRoot().setContentDescription(
                    AccountAccessText.title(context, entry)
                            + ". "
                            + context.getString(
                                    AccountAccessText.status(
                                            entry.status())));
        }
    }
}
