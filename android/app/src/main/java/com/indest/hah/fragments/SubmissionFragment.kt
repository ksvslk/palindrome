package com.indest.hah.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.firestore.*
import com.indest.hah.databinding.SubmissionFragmentBinding


class SubmissionFragment : BottomSheetDialogFragment() {
    private val TAG = "SubmissionFragment"
    private var _binding: SubmissionFragmentBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d(TAG, "onCreateView")
        val b = SubmissionFragmentBinding.inflate(layoutInflater, container, false)
        _binding = b
        requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val db = FirebaseFirestore.getInstance()

        db.collection("user_submitted_levels")
            .whereEqualTo("is_featured", true)
            .addSnapshotListener(EventListener { snapshots, e ->
                if (e != null) {
                    Log.w(TAG, "listen:error", e)
                    return@EventListener
                }
                for (dc in snapshots!!.documentChanges) {
                    when (dc.type) {
                        DocumentChange.Type.ADDED -> Log.d(TAG, "New featured level: " + dc.document.data)
                        DocumentChange.Type.MODIFIED -> Log.d(TAG, "Modified level: " + dc.document.data)
                        DocumentChange.Type.REMOVED -> Log.d(TAG, "Removed level: " + dc.document.data)
                    }
                }
            })

        binding.sendLevelButton.setOnClickListener {
            // Create a new user with a first, middle, and last name
            val level = hashMapOf(
                "is_featured" to false,
                "level" to binding.playerSubmittedLevel.text.toString(),
                "timestamp" to FieldValue.serverTimestamp(),
                "user_name" to "Mina"
            )

            // Add a new document with a generated ID
            db.collection("user_submitted_levels")
                .add(level)
                .addOnSuccessListener { documentReference ->
                    Log.d(TAG, "DocumentSnapshot added with ID: ${documentReference.id}")
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "Error adding document", e)
                }
        }
    }

}