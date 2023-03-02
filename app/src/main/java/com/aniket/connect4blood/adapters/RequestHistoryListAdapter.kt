package com.aniket.connect4blood.adapters

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import com.aniket.connect4blood.R
import com.aniket.connect4blood.activities.DonorSearchStatus
import com.aniket.connect4blood.models.BloodRequestStatus

class RequestHistoryListAdapter(
    private val context: FragmentActivity?,
    private val list: List<Map<String, String>>
) :
    RecyclerView.Adapter<RequestHistoryListAdapter.RequestHistoryListItemVH>() {

    class RequestHistoryListItemVH(view: View) : RecyclerView.ViewHolder(view) {
        val txtBloodRequestId: TextView = view.findViewById(R.id.txtBloodRequestId)
        val txtBloodRequestBloodGroup: TextView = view.findViewById(R.id.txtBloodRequestBloodGroup)
        val txtBloodRequestReqUnits: TextView = view.findViewById(R.id.txtBloodRequestReqUnits)
        val txtBloodRequestStatus: TextView = view.findViewById(R.id.txtBloodRequestStatus)

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RequestHistoryListItemVH {
        return RequestHistoryListItemVH(
            LayoutInflater
                .from(context)
                .inflate(R.layout.layout_recipient_history_single_row, parent, false)
        )
    }

    override fun onBindViewHolder(holder: RequestHistoryListItemVH, position: Int) {
        val request = list[position]
        context?.let {
            holder.txtBloodRequestId.text = String.format(
                it.getString(R.string.request_history_id_format),
                request["id"]
            )
            holder.txtBloodRequestBloodGroup.text = request["blood_group"]
            holder.txtBloodRequestReqUnits.text = request["req_units"]
            holder.txtBloodRequestStatus.text = request["status"]
            when (request["status"]) {
                BloodRequestStatus.PENDING.toString() -> {
                    holder.txtBloodRequestStatus.setTextColor(
                        ContextCompat.getColor(
                            it,
                            R.color.yellow_500
                        )
                    )
                }
                BloodRequestStatus.CONFIRMED.toString() -> {
                    holder.txtBloodRequestStatus.setTextColor(
                        ContextCompat.getColor(
                            it,
                            R.color.blue_500
                        )
                    )
                }
                BloodRequestStatus.COMPLETED.toString() -> {
                    holder.txtBloodRequestStatus.setTextColor(
                        ContextCompat.getColor(
                            it,
                            R.color.green_500
                        )
                    )
                }
                BloodRequestStatus.CANCELLED.toString() -> {
                    holder.txtBloodRequestStatus.setTextColor(
                        ContextCompat.getColor(
                            it,
                            R.color.red_500
                        )
                    )
                }
                else -> {
                    holder.txtBloodRequestStatus.setTextColor(
                        ContextCompat.getColor(
                            it,
                            R.color.black
                        )
                    )
                }
            }
            holder.itemView.setOnClickListener { _ ->
                val requestDetailsIntent = Intent(context, DonorSearchStatus::class.java)
                requestDetailsIntent.putExtra("requestId", request["id"])
                it.startActivity(requestDetailsIntent)
            }
        }
    }

    override fun getItemCount(): Int {
        return list.size
    }


}