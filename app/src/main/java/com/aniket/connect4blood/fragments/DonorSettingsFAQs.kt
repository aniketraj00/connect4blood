package com.aniket.connect4blood.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.LayoutManager
import com.aniket.connect4blood.R
import com.aniket.connect4blood.activities.DonorDashboard
import com.aniket.connect4blood.adapters.FAQsListAdapter
import com.aniket.connect4blood.models.FAQ
import com.aniket.connect4blood.utils.Utils

class DonorSettingsFAQs: Fragment() {

    private lateinit var recyclerFaqs: RecyclerView
    private lateinit var recyclerFaqsLM: LayoutManager
    private lateinit var recyclerFaqsAdapter: FAQsListAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.layout_settings_faqs, container, false)
        activity?.let {
            Utils
                .getInstance()
                .enableBackButton((it as DonorDashboard).supportActionBar, "FAQs")
        }

        recyclerFaqs = view.findViewById(R.id.recyclerFaqs)
        recyclerFaqsLM = LinearLayoutManager(requireContext())
        recyclerFaqsAdapter = FAQsListAdapter(requireContext(), getFAQsList())

        recyclerFaqs.layoutManager = recyclerFaqsLM
        recyclerFaqs.adapter = recyclerFaqsAdapter

        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Utils
            .getInstance()
            .disableBackButton((activity as DonorDashboard).supportActionBar)
    }

    companion object {
        fun newInstance(): DonorSettingsFAQs {
            val fragment = DonorSettingsFAQs()
            val args = Bundle()
            fragment.arguments = args
            return fragment
        }
        private fun getFAQsList(): List<FAQ> {
            return listOf(
                FAQ(
                    "What is connect4blood?",
                    "Connect4Blood is an android application that provides a social platform for blood donors and recipients."
                ),
                FAQ(
                    "How does it work?",
                    "Users who are willing to donate their blood can register as donor on this app. Whenever a blood donation request is posted by a recipient in the locality same as the donor, they get notified on the app and they can accept those request at their own free will."
                ),
                FAQ(
                    "What happens when a blood donation request is posted on the app by a recipient?",
                    "A blood donation request contains the location of the recipient as well as the search radius (default 15 K.M). Once the request is posted online, all the registered donors living in the locality same as the recipient gets notified."
                ),
                FAQ(
                    "Is there any age requirement for donating blood?",
                    "Yes, Mandatory age for blood donation is minimum 18 years"
                ),
                FAQ(
                    "How many times a person can donate blood?",
                    "The safe limit is once in every 3 months. Each time a maximum of 1 unit blood can be donated."
                ),
                FAQ(
                    "What is the maximum unit of blood that can be donated?",
                    "A maximum of 1 unit blood can be donated at once."
                ),
                FAQ(
                    "What if the blood donation request requires more than 1 unit of blood?",
                    "Since the safe limit for blood donation is 1 unit per person. In that case the application forms a pool of donors having size equal to the number of units of blood required. After all the users accept the request, it gets confirmed and the recipient gets notified."
                ),
                FAQ(
                    "Can a donor cancel the request after accepting it?",
                    "Yes, the donor can cancel the blood donation request even after accepting it. If the request was earlier confirmed, after cancellation it gets into pending state. Once some other user accepts that request, it gets confirmed again."
                ),
                FAQ(
                    "Can a recipient cancel the request after posting it?",
                    "Yes, a recipient can cancel the blood donation request after posting it. However, try to avoid such situation since it may cause inconvenience to the donors especially in case of confirmed requests."
                ),
                FAQ(
                    "Is it necessary for a user to register as donor on this app?",
                    "Blood donation via this application is completely voluntarily."
                ),
                FAQ(
                    "Does this application charge any fee for posting blood donation request?",
                    "No, posting blood donation request on this application is completely free."
                ),
                FAQ(
                    "What happens when a blood donation request after posting gets confirmed?",
                    "A confirmed request means donor(s) have accepted your request and they are on their way to your location. Their details will be visible on the request status page."
                ),
                FAQ(
                    "What happens once the blood donation is completed?",
                    "Once the blood donation is completed, make sure to mark the request as completed on the application."
                ),
                FAQ(
                    "Does the recipient need to pay for the blood donation?",
                    "No, paid blood donation is completely banned in our country. This platform neither practice nor does it promote paid blood donation, except recipient may choose to compensate for the donor(s) travelling allowances."
                ),
                FAQ(
                    "Can a blood donor close their account on connect4blood once created?",
                    "Yes, the donors are free to exit this platform anytime they want. They are given the option to deactivate their account."
                )
            )
        }
    }
}