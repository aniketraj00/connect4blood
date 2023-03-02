package com.aniket.connect4blood.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.aniket.connect4blood.R
import com.aniket.connect4blood.models.FAQ

class FAQsListAdapter(private val context: Context, private val list: List<FAQ>): RecyclerView.Adapter<FAQsListAdapter.FAQsListAdapterVH>() {

    class FAQsListAdapterVH(view: View): RecyclerView.ViewHolder(view) {
        val txtFaqsQuestion: TextView = view.findViewById(R.id.txtFaqsQuestion)
        val txtFaqsAnswer: TextView = view.findViewById(R.id.txtFaqsAnswer)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FAQsListAdapterVH {
        return FAQsListAdapterVH(
            LayoutInflater
                .from(context)
                .inflate(
                    R.layout.layout_faqs_single_row,
                    parent,
                    false
                )
        )
    }

    override fun onBindViewHolder(holder: FAQsListAdapterVH, position: Int) {
        val faq = list[position]
        holder.txtFaqsQuestion.text = faq.question
        holder.txtFaqsAnswer.text = faq.answer
    }

    override fun getItemCount(): Int {
        return list.size
    }
}