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

        val adapter = CrimeListAdapter { crimeId ->
            android.util.Log.d("CrimeListFragment", "Navigate on click: $crimeId")
            val action = CrimeListFragmentDirections
                .actionCrimeListFragmentToCrimeDetailFragment(crimeId.toString())
            findNavController().navigate(action)
        }
        binding.crimeRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.crimeRecyclerView.adapter = adapter

        addMenu()

        // Seed on first run if empty (no-op when data exists)
        viewLifecycleOwner.lifecycleScope.launch {
            crimeListViewModel.seedIfEmpty()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                crimeListViewModel.crimes.collect { list ->
                    adapter.submitList(list)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun addMenu() {
        (requireActivity() as androidx.core.view.MenuHost).addMenuProvider(
            object : androidx.core.view.MenuProvider {
                override fun onCreateMenu(menu: android.view.Menu, menuInflater: android.view.MenuInflater) {
                    menuInflater.inflate(R.menu.menu_crime_list, menu)
                }

                override fun onMenuItemSelected(menuItem: android.view.MenuItem): Boolean {
                    return when (menuItem.itemId) {
                        R.id.menu_add_crime -> {
                            val newCrime = Crime(title = "", isSolved = false)
                            viewLifecycleOwner.lifecycleScope.launch {
                                CrimeRepository.get().upsert(newCrime)
                                val action = CrimeListFragmentDirections
                                    .actionCrimeListFragmentToCrimeDetailFragment(newCrime.id.toString())
                                findNavController().navigate(action)
                            }
                            true
                        }
                        else -> false
                    }
                }
            },
            viewLifecycleOwner,
            androidx.lifecycle.Lifecycle.State.STARTED
        )
    }
}