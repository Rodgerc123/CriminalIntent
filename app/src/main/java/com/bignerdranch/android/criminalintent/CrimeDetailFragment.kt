package com.bignerdranch.android.criminalintent

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.content.pm.PackageManager
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.lifecycleScope
import java.text.DateFormat
import com.bignerdranch.android.criminalintent.databinding.FragmentCrimeDetailBinding
import kotlinx.coroutines.launch
import java.util.Date
import java.util.UUID

/**
 * Detail screen for a single Crime.
 * For now this fragment creates/holds a Crime in memory.
 * In a later chapter you'll load the Crime by ID from a repository/DB.
 */
class CrimeDetailFragment : Fragment() {

    // ---- View Binding plumbing (book pattern) ----

    private val args: CrimeDetailFragmentArgs by navArgs()

    private val viewModel: CrimeDetailViewModel by viewModels()

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
        val passedId = UUID.fromString(args.crimeId)
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

    // >>>New: permission launcher
    private val requestReadContacts =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                pickContact.launch(null)
            } else {
                Toast.makeText(requireContext(), "Contacts permission denied", Toast.LENGTH_SHORT).show()
            }
        }

    // >>>New: contact picker
    private val pickContact =
        registerForActivityResult(ActivityResultContracts.PickContact()) { contactUri ->
            if (contactUri == null) return@registerForActivityResult

            val suspectName = queryDisplayName(contactUri)
            val phone = queryFirstPhoneNumber(contactUri)

            if (phone == null) {
                Toast.makeText(requireContext(), "That contact has no phone number", Toast.LENGTH_SHORT).show()
            }

            // Immediate UI feedback
            updateSuspectUi(suspectName, phone)

            // Persist (and update the observed state) via ViewModel
            viewModel.updateSuspect(suspectName, phone)

            // 1) Update local in-memory copy (so UI updates immediately)
            val updated = crime.copy(suspect = suspectName, suspectPhone = phone)
            crime = updated
            updateSuspectUi(suspectName, phone)

            // 2) Persist to Room (so it survives process/app restarts)
            viewLifecycleOwner.lifecycleScope.launch {
                // CrimeRepository is a singleton you already initialize in Application.onCreate()
                CrimeRepository.get().upsert(updated)
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

        // Load the crime using Safe Args
        val crimeId = UUID.fromString(args.crimeId)
        viewModel.load(crimeId)

// Observe and render the crime from the ViewModel
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.crime.collect { loaded ->
                    loaded ?: return@collect
                    crime = loaded  // keep your local field in sync if you still use it

                    // Reflect current state on screen
                    binding.apply {
                        crimeTitle.setText(loaded.title)
                        crimeDate.text = loaded.date.toString()
                        crimeSolved.isChecked = loaded.isSolved
                    }
                    updateSuspectUi(loaded.suspect, loaded.suspectPhone)
                }
            }
        }

        binding.apply {
            crimeTitle.doOnTextChanged { text, _, _, _ ->
                viewModel.updateTitle(text?.toString().orEmpty())
            }
            crimeDate.isEnabled = false
            crimeSolved.setOnCheckedChangeListener { _, isChecked ->
                viewModel.updateSolved(isChecked)
            }
        }

        // Share report
        binding.shareReport.setOnClickListener {
            val reportText = buildCrimeReport()
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, reportText)
                putExtra(Intent.EXTRA_SUBJECT, getString(R.string.crime_report_subject))
            }
            startActivity(Intent.createChooser(sendIntent, getString(R.string.share_crime_report)))
        }

        // >>>New: Choose suspect (runtime READ_CONTACTS)
        binding.chooseSuspect.setOnClickListener {
            val permission = Manifest.permission.READ_CONTACTS
            val granted = ContextCompat.checkSelfPermission(requireContext(), permission) == PackageManager.PERMISSION_GRANTED

            if (granted) {
                pickContact.launch(null)
            } else if (shouldShowRequestPermissionRationale(permission)) {
                // Simple rationale; you could show a Snackbar guiding to Settings too
                Toast.makeText(requireContext(), "Contacts permission is needed to pick a suspect", Toast.LENGTH_LONG).show()
                requestReadContacts.launch(permission)
            } else {
                requestReadContacts.launch(permission)
            }
        }

// >>>New: Call suspect (ACTION_DIAL, no CALL_PHONE permission required)
        binding.callSuspect.setOnClickListener {
            val phone = crime.suspectPhone
            if (phone.isNullOrBlank()) {
                Toast.makeText(requireContext(), "No phone on file for suspect", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            startActivity(Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:${Uri.encode(phone)}")
            })
        }

// >>>New: On first load, reflect current suspect state (if any)
        updateSuspectUi(crime.suspect, crime.suspectPhone)

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // avoid leaking the view
    }

    companion object {
        private const val TAG = "CrimeDetailFragment"

    }
    // >>>New: Build a readable report from local 'crime'
    private fun buildCrimeReport(): String {
        val solvedString = if (crime.isSolved) getString(android.R.string.yes) else getString(android.R.string.no)
        val dateString = DateFormat.getDateInstance(DateFormat.MEDIUM).format(crime.date)
        val suspect = crime.suspect ?: getString(R.string.no_suspect)

        return getString(
            R.string.crime_report,
            crime.title,
            dateString,
            solvedString,
            suspect
        )
    }

    // >>>New: Reflect suspect UI state
    private fun updateSuspectUi(suspectName: String?, suspectPhone: String?) {
        binding.chooseSuspect.text = suspectName ?: getString(R.string.choose_suspect)
        val enabled = !suspectPhone.isNullOrBlank()
        binding.callSuspect.isEnabled = enabled
        binding.callSuspect.alpha = if (enabled) 1f else 0.5f
    }

    // >>>New: Contact queries
    private fun queryDisplayName(contactUri: Uri): String? {
        requireContext().contentResolver.query(
            contactUri,
            arrayOf(ContactsContract.Contacts.DISPLAY_NAME),
            null, null, null
        )?.use { c ->
            if (c.moveToFirst()) {
                val idx = c.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME)
                return c.getString(idx)
            }
        }
        return null
    }

    private fun queryFirstPhoneNumber(contactUri: Uri): String? {
        val contactId = requireContext().contentResolver.query(
            contactUri,
            arrayOf(ContactsContract.Contacts._ID),
            null, null, null
        )?.use { c ->
            if (c.moveToFirst()) c.getString(c.getColumnIndexOrThrow(ContactsContract.Contacts._ID)) else null
        } ?: return null

        val phones = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        requireContext().contentResolver.query(
            phones,
            arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
            "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID}=?",
            arrayOf(contactId),
            null
        )?.use { p ->
            if (p.moveToFirst()) {
                val idx = p.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)
                return p.getString(idx)
            }
        }
        return null
    }
}