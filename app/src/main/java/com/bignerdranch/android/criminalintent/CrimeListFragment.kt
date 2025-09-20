package com.bignerdranch.android.criminalintent

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.view.MenuProvider
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
import java.util.UUID

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

        // Adapter with click → navigate to detail
        val adapter = CrimeListAdapter { crimeId: UUID ->
            val action = CrimeListFragmentDirections
                .actionCrimeListFragmentToCrimeDetailFragment(crimeId.toString())
            findNavController().navigate(action)
        }

        binding.crimeRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.crimeRecyclerView.adapter = adapter

        // Empty view button: create a new crime (same flow as the (+) menu)
        binding.addFirstCrime.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                val newId = crimeListViewModel.addCrime() // <- see helper below
                val action = CrimeListFragmentDirections
                    .actionCrimeListFragmentToCrimeDetailFragment(newId.toString())
                findNavController().navigate(action)
            }
        }

        // Collect the list and toggle visibility
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                crimeListViewModel.crimes.collect { list ->
                    adapter.submitList(list)
                    val isEmpty = list.isEmpty()
                    binding.crimeRecyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE
                    binding.emptyView.visibility = if (isEmpty) View.VISIBLE else View.GONE
                }
            }
        }

        // Register the (+) menu for this fragment
        addMenu()
    }

    /** Contributes the (+) action only while this fragment is RESUMED. */
    private fun addMenu() {
        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menu.clear() // avoid leftovers from other fragments
                menuInflater.inflate(R.menu.menu_crime_list, menu)
            }

            override fun onMenuItemSelected(item: MenuItem): Boolean {
                return when (item.itemId) {
                    R.id.menu_add_crime -> {
                        viewLifecycleOwner.lifecycleScope.launch {
                            val newId = crimeListViewModel.addCrime()
                            val action = CrimeListFragmentDirections
                                .actionCrimeListFragmentToCrimeDetailFragment(newId.toString())
                            findNavController().navigate(action)
                        }
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}