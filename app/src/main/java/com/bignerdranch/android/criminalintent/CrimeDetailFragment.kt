package com.bignerdranch.android.criminalintent

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.navArgs
import androidx.navigation.fragment.findNavController
import com.bignerdranch.android.criminalintent.databinding.FragmentCrimeDetailBinding
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date
import java.util.UUID

class CrimeDetailFragment : Fragment() {

    private val args: CrimeDetailFragmentArgs by navArgs()
    private val viewModel: CrimeDetailViewModel by viewModels()

    private var _binding: FragmentCrimeDetailBinding? = null
    private val binding get() = checkNotNull(_binding) { "Binding is null (view not visible)" }

    private var isProgrammaticTitleUpdate = false

    // --- permissions & activity results ---

    private val requestReadContacts =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                pickContact.launch(null)
            } else {
                Toast.makeText(requireContext(), getString(R.string.contacts_permission_denied), Toast.LENGTH_SHORT).show()
            }
        }

    private val pickContact =
        registerForActivityResult(ActivityResultContracts.PickContact()) { contactUri ->
            if (contactUri == null) return@registerForActivityResult
            val suspectName = queryDisplayName(contactUri)
            val phone = queryFirstPhoneNumber(contactUri)

            if (phone == null) {
                Toast.makeText(requireContext(), getString(R.string.no_phone_for_contact), Toast.LENGTH_SHORT).show()
            }

            // Persist via ViewModel; UI will update from the collector
            viewModel.updateSuspect(suspectName, phone)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Load the Crime by ID passed via Safe Args
        val crimeId = UUID.fromString(args.crimeId)
        viewModel.load(crimeId)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCrimeDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // --- Observe DB state and render UI ---
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.crime.collect { loaded ->
                    if (loaded == null) return@collect

                    // Safe text update (prevents cursor jumping)
                    val edit = binding.crimeTitle
                    val currentText = edit.text?.toString().orEmpty()
                    if (currentText != loaded.title) {
                        isProgrammaticTitleUpdate = true
                        val oldSel = edit.selectionStart
                        edit.text?.replace(0, edit.text?.length ?: 0, loaded.title)
                        val newSel = loaded.title.length.coerceAtMost(oldSel.coerceAtLeast(0))
                        edit.setSelection(newSel)
                        isProgrammaticTitleUpdate = false
                    }

                    binding.crimeDate.text =
                        java.text.DateFormat.getDateInstance(java.text.DateFormat.MEDIUM).format(loaded.date)
                    binding.crimeSolved.isChecked = loaded.isSolved
                    updateSuspectUi(loaded.suspect, loaded.suspectPhone)
                }
            }
        }

        // Receive date picked from DatePickerFragment
        childFragmentManager.setFragmentResultListener(
            DatePickerFragment.REQUEST_KEY,
            viewLifecycleOwner
        ) { _, bundle ->
            val millis = bundle.getLong(DatePickerFragment.BUNDLE_KEY_DATE_MILLIS)
            val picked = Date(millis)
            // Persist via ViewModel and UI will refresh via your collector
            viewModel.updateDate(picked)
        }

        // --- Static widget config ---
        binding.crimeDate.isEnabled = true
        binding.crimeDate.setOnClickListener {
            // Use the current crime date if present; otherwise "now"
            val initial = viewModel.crime.value?.date ?: Date()
            DatePickerFragment.newInstance(initial)
                .show(childFragmentManager, "DATE_PICKER")
        }
        // --- Listeners that push changes to VM (do NOT reference `loaded` here) ---
        binding.crimeTitle.doOnTextChanged { text, _, _, _ ->
            if (!isProgrammaticTitleUpdate) {
                viewModel.updateTitle(text?.toString().orEmpty())
            }
        }
        binding.crimeSolved.setOnCheckedChangeListener { _, checked ->
            viewModel.updateSolved(checked)
        }

        // Share report (read current from VM, not `loaded`)
        binding.shareReport.setOnClickListener {
            val current = viewModel.crime.value
            val reportText = buildCrimeReport(current)
            val send = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, reportText)
                putExtra(Intent.EXTRA_SUBJECT, getString(R.string.crime_report_subject))
            }
            startActivity(Intent.createChooser(send, getString(R.string.share_crime_report)))
        }

        // Choose suspect (keep your existing permission + picker flow)

        // Call suspect (read current from VM)
        binding.callSuspect.setOnClickListener {
            val phone = viewModel.crime.value?.suspectPhone
            if (phone.isNullOrBlank()) {
                Toast.makeText(requireContext(), getString(R.string.no_phone_for_contact), Toast.LENGTH_SHORT).show()
            } else {
                startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${Uri.encode(phone)}")))
            }
        }

        // --- Back block: no untitled crimes ---
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    val titleNow = binding.crimeTitle.text?.toString()?.trim().orEmpty()
                    if (titleNow.isBlank()) {
                        binding.crimeTitle.error = getString(R.string.error_title_required)
                        binding.crimeTitle.requestFocus()
                        Toast.makeText(requireContext(), getString(R.string.toast_title_required), Toast.LENGTH_SHORT).show()
                    } else {
                        viewModel.updateTitle(titleNow) // defensive
                        findNavController().popBackStack()
                    }
                }
            }
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // --- helpers ---

    private fun buildCrimeReport(current: Crime?): String {
        val c = current ?: return ""
        val solvedString = if (c.isSolved) getString(android.R.string.yes) else getString(android.R.string.no)
        val dateString = DateFormat.getDateInstance(DateFormat.MEDIUM).format(c.date)
        val suspect = c.suspect ?: getString(R.string.no_suspect)
        return getString(R.string.crime_report, c.title, dateString, solvedString, suspect)
    }

    private fun updateSuspectUi(suspectName: String?, suspectPhone: String?) {
        binding.chooseSuspect.text = suspectName ?: getString(R.string.choose_suspect)
        val enabled = !suspectPhone.isNullOrBlank()
        binding.callSuspect.isEnabled = enabled
        binding.callSuspect.alpha = if (enabled) 1f else 0.5f
    }

    private fun queryDisplayName(contactUri: Uri): String? =
        requireContext().contentResolver.query(
            contactUri,
            arrayOf(ContactsContract.Contacts.DISPLAY_NAME),
            null, null, null
        )?.use { c ->
            if (c.moveToFirst()) {
                val idx = c.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME)
                c.getString(idx)
            } else null
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
        return requireContext().contentResolver.query(
            phones,
            arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
            "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID}=?",
            arrayOf(contactId),
            null
        )?.use { p ->
            if (p.moveToFirst()) {
                val idx = p.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)
                p.getString(idx)
            } else null
        }
    }
}