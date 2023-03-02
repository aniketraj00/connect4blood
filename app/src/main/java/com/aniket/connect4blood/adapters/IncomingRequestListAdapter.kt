package com.aniket.connect4blood.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import com.aniket.connect4blood.R
import com.aniket.connect4blood.utils.AppVals
import com.aniket.connect4blood.utils.Utils
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class IncomingRequestListAdapter(
    private val context: FragmentActivity?,
    private val list: List<Map<String, String>>,
    private val requestFulfillmentProvider: RequestFulfillmentProvider
) :
    RecyclerView.Adapter<IncomingRequestListAdapter.IncomingRequestListItemVH>() {

    class IncomingRequestListItemVH(view: View) : RecyclerView.ViewHolder(view) {
        val txtRecipientName: TextView = view.findViewById(R.id.txtRecipientName)
        val txtHospitalNameAndDistance: TextView = view.findViewById(R.id.txtHospitalNameAndDistance)
        val txtHospitalAddress: TextView = view.findViewById(R.id.txtHospitalAddress)
        val btnAcceptRequest: MaterialButton = view.findViewById(R.id.btnAcceptRequest)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): IncomingRequestListItemVH {
        return IncomingRequestListItemVH(
            LayoutInflater
                .from(context)
                .inflate(
                    R.layout.layout_donor_home_single_row,
                    parent,
                    false
                )
        )
    }

    override fun onBindViewHolder(holder: IncomingRequestListItemVH, position: Int) {
        val requestData = list[position]
        holder.txtRecipientName.text = requestData["name"]
        if(context != null) {
            holder.txtHospitalNameAndDistance.text = String.format(
                context.getString(R.string.hospital_name_distance_view_format),
                requestData["hospitalName"],
                requestData["distance"]
            )
        }
        holder.txtHospitalAddress.text = requestData["hospitalAddress"]
        holder.btnAcceptRequest.setOnClickListener {
            if (context != null) {
                val acceptDialog = MaterialAlertDialogBuilder(context as Context)
                    .setView(R.layout.layout_donor_declaration)
                    .setCancelable(false)
                    .create()

                acceptDialog.show()

                val cbTerms = acceptDialog.findViewById<CheckBox>(R.id.cbTerms)
                val acceptBtn = acceptDialog.findViewById<Button>(R.id.btnAccept)
                val cancelBtn = acceptDialog.findViewById<Button>(R.id.btnCancel)

                cancelBtn?.setOnClickListener {
                    acceptDialog.dismiss()
                }

                acceptBtn?.setOnClickListener {
                    if(cbTerms?.isChecked == true) {
                        acceptDialog.dismiss()
                        //Setup request accept functionality
                        requestFulfillmentProvider.onAccept(requestData)
                    } else {
                        Utils
                            .getInstance()
                            .showToast(
                                context as Context,
                                AppVals.ERROR_ACCEPT_TERMS
                            )
                    }
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return list.size
    }

    interface RequestFulfillmentProvider {
        fun onAccept(requestData: Map<String, String>)
    }
}