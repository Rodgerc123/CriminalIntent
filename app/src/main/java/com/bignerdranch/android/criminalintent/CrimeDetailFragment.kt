package com.bignerdranch.android.criminalintent

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import com.bignerdranch.android.criminalintent.databinding.FragmentCrimeDetailBinding
import java.util.Date
import java.util.UUID

/**
 * Detail screen for a single Crime.
 * For now this fragment creates/holds a Crime in memory.
 * In a later chapter you'll load the Crime by ID from a repository/DB.
 */
class CrimeDetailFragment : Fragment() {

    // ---- View Binding plumbing (book pattern) ----
    private var _binding: FragmentCrimeDetailBinding? = null
    private val binding
        get() = checkNotNull(_binding) {
            "Cannot access binding because it is null. Is the view visible?"
        }

    // ---- Local UI model for this screen ----
    private lateinit var crime: Crime

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Read the Crime ID passed from the list (if any).
        // For now we just initialize our local Crime with that id.
        val passedId: UUID? = arguments
            ?.getString(ARG_CRIME_ID)
            ?.let(UUID::fromString)

        Log.d(TAG, "CrimeDetailFragment opened with id=$passedId")

        // Initialize the Crime instance.
        // If an id was passed, keep it; otherwise generate a new Crime.
        crime = if (passedId != null) {
            Crime(
                id = passedId,
                title = "",
                date = Date(),
                isSolved = false
            )
        } else {
            Crime() // uses all defaults (including a random id)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCrimeDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    // Wire up view listeners AFTER the view exists.
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.apply {
            // Title text changes update our local Crime copy
            crimeTitle.doOnTextChanged { text, _, _, _ ->
                crime = crime.copy(title = text?.toString().orEmpty())
            }

            // Show the current date. Disabled for now; enabled in later chapter with a DatePicker.
            crimeDate.text = crime.date.toString()
            crimeDate.isEnabled = false

            // Checkbox toggles "solved" state
            crimeSolved.setOnCheckedChangeListener { _, isChecked ->
                crime = crime.copy(isSolved = isChecked)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // avoid leaking the view
    }

    companion object {
        private const val TAG = "CrimeDetailFragment"
        private const val ARG_CRIME_ID = "crime_id"

        /**
         * Factory method to create the fragment with its argument Bundle.
         * Used by MainActivity when a list row is clicked.
         */
        fun newInstance(crimeId: UUID): CrimeDetailFragment {
            val args = Bundle().apply {
                putString(ARG_CRIME_ID, crimeId.toString())
            }
            return CrimeDetailFragment().apply { arguments = args }
        }
    }
}