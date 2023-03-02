package com.aniket.connect4blood.adapters

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import com.aniket.connect4blood.R
import com.bumptech.glide.Glide

class DonorsListAdapter(
    private val context: FragmentActivity?, private val donorList: List<Map<String, String>>
) : RecyclerView.Adapter<DonorsListAdapter.DonorsListItemViewHolder>() {

    class DonorsListItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imgDonor: ImageView = view.findViewById(R.id.imgDonor)
        val txtDonorName: TextView = view.findViewById(R.id.txtDonorName)
        val txtDonorAge: TextView = view.findViewById(R.id.txtDonorAge)
        val txtDonorGender: TextView = view.findViewById(R.id.txtDonorGender)
        val txtContactDonorLink: TextView = view.findViewById(R.id.txtContactDonorLink)
        val txtReplaceDonorLink: TextView = view.findViewById(R.id.txtReplaceDonorLink)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DonorsListItemViewHolder {
        return DonorsListItemViewHolder(
            LayoutInflater
                .from(context)
                .inflate(R.layout.layout_donors_single_row, parent, false)
        )
    }

    override fun onBindViewHolder(holder: DonorsListItemViewHolder, position: Int) {
        val donor = donorList[position]
        holder.txtDonorName.text = donor["name"]
        holder.txtDonorAge.text = donor["age"]
        holder.txtDonorGender.text = donor["gender"]
        if(donor["image"] != null && context != null) {
            Glide
                .with(context as Context)
                .load(Uri.parse(donor["image"]))
                .centerCrop()
                .placeholder(R.drawable.ic_person)
                .into(holder.imgDonor)
        }
    }

    override fun getItemCount(): Int {
        return donorList.size
    }
}