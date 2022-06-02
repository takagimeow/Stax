package com.hover.stax.transactions

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.hover.sdk.actions.HoverAction
import com.hover.sdk.transactions.Transaction
import com.hover.stax.databinding.TransactionListItemBinding
import com.hover.stax.transactions.TransactionHistoryAdapter.HistoryViewHolder
import com.hover.stax.utils.DateUtils.humanFriendlyDate
import java.util.*

class TransactionHistoryAdapter(private val selectListener: SelectListener) : ListAdapter<TransactionHistory, HistoryViewHolder>(diffUtil) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val binding =
            TransactionListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return HistoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        val history = getItem(holder.adapterPosition)
        val t = history.staxTransaction
        val action = history.action
        holder.binding.liTitle.text = String.format("%s%s", t.description.substring(0, 1).uppercase(Locale.getDefault()), t.description.substring(1))
        holder.binding.liAmount.text = t.getSignedAmount(t.amount)
        holder.binding.liHeader.visibility = if (shouldShowDate(t, position)) View.VISIBLE else View.GONE
        holder.binding.liHeader.text = humanFriendlyDate(t.initiated_at)
        holder.itemView.setOnClickListener { selectListener.viewTransactionDetail(t.uuid) }
        setStatus(t, action, holder)
    }

    private fun setStatus(t: StaxTransaction, a: HoverAction?, holder: HistoryViewHolder) {
        holder.binding.liAmount.alpha = (if (t.status == Transaction.FAILED) 0.54 else 1.0).toFloat()
        holder.binding.transactionItemLayout.setBackgroundColor(ContextCompat.getColor(holder.binding.root.context, t.getBackgroundColor()))
        holder.binding.liDetail.text = t.shortDescription(a, holder.itemView.context)
        holder.binding.liDetail.setCompoundDrawablesRelativeWithIntrinsicBounds(t.getIcon(), 0, 0, 0)
    }

    private fun shouldShowDate(t: StaxTransaction, position: Int): Boolean {
        if (position > 1) {
            val history = getItem(position - 1)
            val transaction = history.staxTransaction
            return position == 0 || humanFriendlyDate(transaction.initiated_at) != humanFriendlyDate(t.initiated_at)
        }
        return true
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    interface SelectListener {
        fun viewTransactionDetail(uuid: String?)
    }

    class HistoryViewHolder(var binding: TransactionListItemBinding) :
        RecyclerView.ViewHolder(binding.root)

    companion object {
        private val diffUtil = object : DiffUtil.ItemCallback<TransactionHistory>() {
            override fun areItemsTheSame(oldItem: TransactionHistory, newItem: TransactionHistory): Boolean {
                return oldItem.staxTransaction.uuid == newItem.staxTransaction.uuid
            }

            override fun areContentsTheSame(oldItem: TransactionHistory, newItem: TransactionHistory): Boolean {
                return oldItem.staxTransaction.uuid == newItem.staxTransaction.uuid
            }

        }
    }
}