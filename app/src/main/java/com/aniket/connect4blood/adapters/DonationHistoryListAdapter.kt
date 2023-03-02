package com.aniket.connect4blood.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import com.aniket.connect4blood.R
import com.aniket.connect4blood.models.BloodRequestStatus

class DonationHistoryListAdapter(private val context: FragmentActivity?, private val list: List<Map<String, Any?>>):
    RecyclerView.Adapter<DonationHistoryListAdapter.DonationHistoryListAdapterVH>() {
    class DonationHistoryListAdapterVH(view: View): RecyclerView.ViewHolder(view){
        val txtDonationHistoryId: TextView = view.findViewById(R.id.txtDonationHistoryId)
        val txtDonationHistoryName: TextView = view.findViewById(R.id.txtDonationHistoryName)
        val txtDonationHistoryDate: TextView = view.findViewById(R.id.txtDonationHistoryDate)
        val txtDonationHistoryStatus: TextView = view.findViewById(R.id.txtDonationHistoryStatus)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): DonationHistoryListAdapterVH {
        return DonationHistoryListAdapterVH(
            LayoutInflater
                .from(context)
                .inflate(
                    R.layout.layout_donation_history_single_row,
                    parent,
                    false
                )
        )
    }

    override fun onBindViewHolder(holder: DonationHistoryListAdapterVH, position: Int) {
        val donationRequest = list[position]
        context?.let {
            holder.txtDonationHistoryId.text = String.format(
                it.getString(R.string.request_history_id_format),
                donationRequest["id"]
            )
            holder.txtDonationHistoryName.text = String.format(
                it.getString(R.string.request_name_format),
                donationRequest["name"]
            )
            holder.txtDonationHistoryDate.text = String.format(
                it.getString(R.string.request_date_format),
                donationRequest["date"]
            )
            holder.txtDonationHistoryStatus.text = donationRequest["status"].toString()

            when(donationRequest["status"]) {
                BloodRequestStatus.COMPLETED -> {
                    holder
                        .txtDonationHistoryStatus
                        .setTextColor(
                            ContextCompat.getColor(it, R.color.green_500)
                        )
                }
                BloodRequestStatus.CANCELLED -> {
                    holder
                        .txtDonationHistoryStatus
                        .setTextColor(
                            ContextCompat.getColor(it, R.color.red_500)
                        )
                }
                else ->
                    holder
                        .txtDonationHistoryStatus
                        .setTextColor(
                            ContextCompat.getColor(it, R.color.black)
                        )
            }
        }

    }

    override fun getItemCount(): Int {
        return list.size
    }
}