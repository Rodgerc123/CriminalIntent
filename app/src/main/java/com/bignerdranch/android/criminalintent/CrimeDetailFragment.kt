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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.navArgs
import com.bignerdranch.android.criminalintent.databinding.FragmentCrimeDetailBinding
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.UUID

class CrimeDetailFragment : Fragment() {

    private val args: CrimeDetailFragmentArgs by navArgs()
    private val viewModel: CrimeDetailViewModel by viewModels()

    private var _binding: FragmentCrimeDetailBinding? = null
    private val binding get() = checkNotNull(_binding) { "Binding is null (view not visible)" }

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

        // Observe and render the crime from the ViewModel
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.crime.collect { loaded ->
                    loaded ?: return@collect
                    binding.apply {
                        crimeTitle.setText(loaded.title)
                        crimeDate.text = DateFormat.getDateInstance(DateFormat.MEDIUM).format(loaded.date)
                        crimeSolved.isChecked = loaded.isSolved
                    }
                    updateSuspectUi(loaded.suspect, loaded.suspectPhone)
                }
            }
        }

        // Title & Solved persist immediately via VM
        binding.crimeTitle.doOnTextChanged { text, _, _, _ ->
            viewModel.updateTitle(text?.toString().orEmpty())
        }
        binding.crimeSolved.setOnCheckedChangeListener { _, isChecked ->
            viewModel.updateSolved(isChecked)
        }
        binding.crimeDate.isEnabled = false // (matches book for now)

        // Share
        binding.shareReport.setOnClickListener {
            val current = viewModel.crime.value
            val reportText = buildCrimeReport(current)
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, reportText)
                putExtra(Intent.EXTRA_SUBJECT, getString(R.string.crime_report_subject))
            }
            startActivity(Intent.createChooser(sendIntent, getString(R.string.share_crime_report)))
        }

        // Choose suspect (runtime permission)
        binding.chooseSuspect.setOnClickListener {
            val permission = Manifest.permission.READ_CONTACTS
            val granted = ContextCompat.checkSelfPermission(requireContext(), permission) == PackageManager.PERMISSION_GRANTED
            if (granted) {
                pickContact.launch(null)
            } else if (shouldShowRequestPermissionRationale(permission)) {
                Toast.makeText(requireContext(), getString(R.string.contacts_permission_rationale), Toast.LENGTH_LONG).show()
                requestReadContacts.launch(permission)
            } else {
                requestReadContacts.launch(permission)
            }
        }

        // Call suspect (ACTION_DIAL)
        binding.callSuspect.setOnClickListener {
            val phone = viewModel.crime.value?.suspectPhone
            if (phone.isNullOrBlank()) {
                Toast.makeText(requireContext(), getString(R.string.no_phone_for_contact), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            startActivity(Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:${Uri.encode(phone)}")
            })
        }
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