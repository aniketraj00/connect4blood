package com.aniket.connect4blood.fragments

import android.content.Context
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.aniket.connect4blood.R
import com.aniket.connect4blood.activities.RecipientDashboard
import com.aniket.connect4blood.models.Complaint
import com.aniket.connect4blood.models.ComplaintStatus
import com.aniket.connect4blood.models.User
import com.aniket.connect4blood.utils.AppVals
import com.aniket.connect4blood.utils.Utils
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class RecipientSettingsRaiseComplaint : Fragment() {

    private lateinit var etComplaintMobile: TextInputEditText
    private lateinit var etComplaintEmail: TextInputEditText
    private lateinit var etComplaintSubject: TextInputEditText
    private lateinit var etComplaintMsg: TextInputEditText
    private lateinit var btnSubmit: MaterialButton
    private lateinit var mAuth: FirebaseAuth
    private var progressDialog: AlertDialog? = null
    private var currentUser: User? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view =
            inflater.inflate(R.layout.layout_donor_settings_raise_complaint, container, false)
        activity?.let {
            Utils
                .getInstance()
                .enableBackButton((it as RecipientDashboard).supportActionBar, "Raise Complaint")
            progressDialog = Utils
                .getInstance()
                .initProgressDialog(
                    it as Context,
                    AppVals.PROGRESS_GENERIC
                )
        }

        etComplaintMobile = view.findViewById(R.id.etComplaintMobile)
        etComplaintEmail = view.findViewById(R.id.etComplaintEmail)
        etComplaintSubject = view.findViewById(R.id.etComplaintSubject)
        etComplaintMsg = view.findViewById(R.id.etComplaintMsg)
        btnSubmit = view.findViewById(R.id.btnSubmit)
        mAuth = FirebaseAuth.getInstance()

        if (mAuth.currentUser != null) {
            progressDialog?.show()
            Firebase
                .database
                .getReference("/users/${mAuth.currentUser?.uid}")
                .get()
                .addOnSuccessListener {
                    progressDialog?.dismiss()
                    currentUser = it.getValue<User>()
                    if (currentUser != null && currentUser!!.email != null && currentUser!!.phoneNo != null) {
                        etComplaintMobile.setText(currentUser!!.phoneNo)
                        etComplaintEmail.setText(currentUser!!.email)
                    } else {
                        enableInputField(etComplaintMobile, InputType.TYPE_CLASS_NUMBER)
                        enableInputField(
                            etComplaintEmail,
                            InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
                        )
                    }
                    btnSubmit.setOnClickListener(onGenerateSupportTicket())
                }
                .addOnFailureListener { e ->
                    progressDialog?.dismiss()
                    activity?.let {
                        Utils
                            .getInstance()
                            .showToast(
                                it as Context,
                                e.message.toString()
                            )
                    }
                }

        }
        return view
    }

    private fun enableInputField(inputField: TextInputEditText, inputFieldType: Int) {
        inputField.inputType = inputFieldType
        inputField.isFocusable = true
        inputField.isFocusableInTouchMode = true
        inputField.isEnabled = true
    }

    private fun onGenerateSupportTicket(): View.OnClickListener {
        return View.OnClickListener {
            if (!validInputs()) return@OnClickListener
            progressDialog?.show()
            progressDialog?.findViewById<TextView>(R.id.txtProgressDialogMsg)?.text =
                AppVals.CREATE_SUPPORT_TICKET
            val currentDateTime = LocalDateTime.now()
            val complaintId =
                Utils.getInstance().generateComplaintId(
                    currentUser?.phoneNo?:etComplaintMobile.text.toString(),
                    currentDateTime
                )
            val complaint = Complaint(
                etComplaintEmail.text.toString(),
                etComplaintMobile.text.toString(),
                etComplaintSubject.text.toString(),
                etComplaintMsg.text.toString(),
                ComplaintStatus.PENDING,
                currentDateTime.format(DateTimeFormatter.ofPattern(AppVals.APP_DEFAULT_DATETIME_FORMAT))
            )
            Firebase
                .database
                .getReference("/complaints/${mAuth.currentUser!!.uid}/$complaintId")
                .setValue(complaint)
                .addOnSuccessListener {
                    progressDialog?.dismiss()
                    etComplaintSubject.text?.clear()
                    etComplaintMsg.text?.clear()
                    activity?.let { it1 ->
                        Utils
                            .getInstance()
                            .showToast(
                                it1 as Context,
                                AppVals.SUCCESS_SUPPORT_TICKET_GENERATED
                            )
                        (it1 as RecipientDashboard).closeFragment()
                    }
                }
                .addOnFailureListener { e ->
                    progressDialog?.dismiss()
                    activity?.let {
                        Utils
                            .getInstance()
                            .showToast(
                                it as Context,
                                e.message.toString()
                            )
                    }
                }
        }
    }

    private fun validInputs(): Boolean {
        if (etComplaintMobile.text.isNullOrEmpty() ||
            etComplaintEmail.text.isNullOrEmpty() ||
            etComplaintSubject.text.isNullOrEmpty() ||
            etComplaintMsg.text.isNullOrEmpty()
        ) {
            Utils
                .getInstance()
                .showToast(
                    requireContext(),
                    AppVals.ERROR_FIELDS_EMPTY
                )
            return false
        }
        if(etComplaintMobile.text.toString().length != 10) {
            Utils
                .getInstance()
                .showToast(
                    requireContext(),
                    AppVals.ERROR_INVALID_PHONE_NO
                )
            return false
        }
        return true
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Utils
            .getInstance()
            .disableBackButton((activity as RecipientDashboard).supportActionBar)
    }

    companion object {
        fun newInstance(): RecipientSettingsRaiseComplaint {
            val fragment = RecipientSettingsRaiseComplaint()
            val args = Bundle()
            fragment.arguments = args
            return fragment
        }
    }
}