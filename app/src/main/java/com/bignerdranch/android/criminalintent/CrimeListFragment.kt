package com.bignerdranch.android.criminalintent

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.bignerdranch.android.criminalintent.databinding.FragmentCrimeListBinding
import kotlinx.coroutines.launch
import java.util.UUID

private const val TAG = "CrimeListFragment"

/**
 * Hosts the RecyclerView and collects crimes from the ViewModel's StateFlow.
 */
class CrimeListFragment : Fragment() {

    /** Activity implements this to handle navigation to detail. */
    interface Callbacks {
        fun onCrimeSelected(crimeId: UUID)
    }

    private var callbacks: Callbacks? = null

    private var _binding: FragmentCrimeListBinding? = null
    private val binding
        get() = checkNotNull(_binding) {
            "Cannot access binding because it is null. Is the view visible?"
        }

    private val crimeListViewModel: CrimeListViewModel by viewModels()

    override fun onAttach(context: Context) {
        super.onAttach(context)
        callbacks = context as? Callbacks
    }

    override fun onDetach() {
        super.onDetach()
        callbacks = null
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCrimeListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // RecyclerView basics
        binding.crimeRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Start with an empty adapter; we'll feed it as data arrives from the flow.
        var adapter = CrimeListAdapter(emptyList()) { id -> callbacks?.onCrimeSelected(id) }
        binding.crimeRecyclerView.adapter = adapter

        // Lifecycle-aware collection of the StateFlow<List<Crime>>
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                crimeListViewModel.crimes.collect { list ->
                    adapter = CrimeListAdapter(list) { id -> callbacks?.onCrimeSelected(id) }
                    binding.crimeRecyclerView.adapter = adapter
                }
            }
        }

        // (Optional) If you want to test inserts once, uncomment briefly:
        // viewLifecycleOwner.lifecycleScope.launch {
        //     crimeListViewModel.addCrime("Debug insert (remove me)")
        // }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}