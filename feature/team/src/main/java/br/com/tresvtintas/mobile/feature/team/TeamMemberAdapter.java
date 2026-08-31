package br.com.tresvtintas.mobile.feature.team;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import br.com.tresvtintas.mobile.core.team.TeamMember;
import br.com.tresvtintas.mobile.feature.team.databinding.TeamItemMemberBinding;

final class TeamMemberAdapter
        extends ListAdapter<TeamMember, TeamMemberAdapter.Holder> {
    private static final DiffUtil.ItemCallback<TeamMember> DIFFERENCE =
            new DiffUtil.ItemCallback<>() {
                @Override
                public boolean areItemsTheSame(
                        @NonNull TeamMember oldItem,
                        @NonNull TeamMember newItem) {
                    return oldItem.painterId() == newItem.painterId();
                }

                @Override
                public boolean areContentsTheSame(
                        @NonNull TeamMember oldItem,
                        @NonNull TeamMember newItem) {
                    return oldItem.equals(newItem);
                }
            };

    TeamMemberAdapter() {
        super(DIFFERENCE);
        setHasStableIds(true);
    }

    @Override
    public long getItemId(int position) {
        return getItem(position).painterId();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType) {
        return new Holder(TeamItemMemberBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false));
    }

    @Override
    public void onBindViewHolder(
            @NonNull Holder holder,
            int position) {
        holder.bind(getItem(position), position + 1);
    }

    static final class Holder extends RecyclerView.ViewHolder {
        private final TeamItemMemberBinding binding;

        Holder(TeamItemMemberBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(TeamMember member, int rank) {
            var context = binding.getRoot().getContext();
            binding.teamMemberRank.setText(context.getString(
                    R.string.team_member_rank,
                    rank));
            binding.teamMemberName.setText(member.name());
            binding.teamMemberCompany.setText(member.company().orElseGet(
                    () -> context.getString(R.string.team_not_informed)));
            binding.teamMemberStatus.setText(TeamText.status(
                    context,
                    member.status()));
            binding.teamMemberSales.setText(TeamText.money(
                    member.totalSales()));
            binding.teamMemberPerformance.setText(context.getString(
                    R.string.team_member_performance,
                    context.getResources().getQuantityString(
                            R.plurals.team_member_orders,
                            member.totalOrders(),
                            member.totalOrders()),
                    context.getResources().getQuantityString(
                            R.plurals.team_member_quotes,
                            member.totalQuotes(),
                            member.totalQuotes()),
                    TeamText.percent(member.conversionBasisPoints())));
            binding.teamMemberRegion.setText(context.getString(
                    R.string.team_member_region,
                    member.topRegion()
                            .map(region -> region.label()
                                    + " · "
                                    + region.count())
                            .orElseGet(() -> context.getString(
                                    R.string.team_not_informed)),
                    TeamText.rate(member.commissionRate())));
            binding.getRoot().setContentDescription(context.getString(
                    R.string.team_member_accessibility,
                    rank,
                    member.name(),
                    binding.teamMemberSales.getText(),
                    binding.teamMemberStatus.getText()));
        }
    }
}
