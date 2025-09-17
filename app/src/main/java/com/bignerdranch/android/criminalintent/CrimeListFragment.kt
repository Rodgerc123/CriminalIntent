package com.bignerdranch.android.criminalintent

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.bignerdranch.android.criminalintent.databinding.FragmentCrimeListBinding
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class CrimeListFragment : Fragment() {

    private var _binding: FragmentCrimeListBinding? = null
    private val binding get() = checkNotNull(_binding) { "Binding is null (view not visible)" }

    // ViewModel exposes crimes as a StateFlow<List<Crime>>
    private val crimeListViewModel: CrimeListViewModel by viewModels()

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

        // 1) Create the adapter ONCE (moved OUT of the collector)
        val adapter = CrimeListAdapter { crimeId ->
            val action = CrimeListFragmentDirections
                .actionCrimeListFragmentToCrimeDetailFragment(crimeId.toString())
            findNavController().navigate(action)
        }

        binding.crimeRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.crimeRecyclerView.adapter = adapter

        // 2) Collect updates and SUBMIT the new list (no adapter recreation)
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                crimeListViewModel.crimes.collect { list ->
                    adapter.submitList(list) // 👈 this replaces recreating the adapter
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}