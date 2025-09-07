package com.bignerdranch.android.criminalintent

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.bignerdranch.android.criminalintent.databinding.ListItemCrimeBinding
import com.bignerdranch.android.criminalintent.databinding.ListItemCrimePoliceBinding
import java.util.UUID

private const val VIEW_TYPE_NORMAL = 0
private const val VIEW_TYPE_POLICE = 1

/**
 * Base ViewHolder so the adapter can use a single generic type.
 * Each concrete holder implements bind(crime) for its own layout.
 */
sealed class CrimeHolder(root: View) : RecyclerView.ViewHolder(root) {
    abstract fun bind(crime: Crime)
}

/**
 * Normal row (title + date + optional "solved" icon).
 * Layout: res/layout/list_item_crime.xml
 */
class CrimeNormalHolder(
    private val binding: ListItemCrimeBinding,
    private val onCrimeClick: (UUID) -> Unit
) : CrimeHolder(binding.root) {

    private lateinit var boundCrime: Crime

    init {
        // Tapping anywhere on the row opens the detail screen.
        binding.root.setOnClickListener {
            onCrimeClick(boundCrime.id)
        }
    }

    override fun bind(crime: Crime) {
        boundCrime = crime
        binding.crimeTitle.text = crime.title
        binding.crimeDate.text = crime.date.toString()
        // Show the solved icon only when solved
        binding.crimeSolvedIcon.isVisible = crime.isSolved
    }
}

/**
 * “Requires police” row (title + date + Contact Police button).
 * Layout: res/layout/list_item_crime_police.xml
 */
class CrimePoliceHolder(
    private val binding: ListItemCrimePoliceBinding,
    private val onCrimeClick: (UUID) -> Unit
) : CrimeHolder(binding.root) {

    private lateinit var boundCrime: Crime

    init {
        // Tapping the row opens the detail screen.
        binding.root.setOnClickListener {
            onCrimeClick(boundCrime.id)
        }
        // Button action demo (you can replace with a real flow later).
        binding.contactPoliceButton.setOnClickListener {
            Toast.makeText(
                binding.root.context,
                "Contacting police for ${boundCrime.title}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun bind(crime: Crime) {
        boundCrime = crime
        binding.crimeTitle.text = crime.title
        binding.crimeDate.text = crime.date.toString()
    }
}

/**
 * RecyclerView adapter that switches between two row types:
 * - Normal (no police button)
 * - Police (shows a Contact Police button)
 *
 * @param crimes immutable snapshot of items to display
 * @param onCrimeClick callback invoked with the clicked crime's UUID
 */
class CrimeListAdapter(
    private val crimes: List<Crime>,
    private val onCrimeClick: (UUID) -> Unit
) : RecyclerView.Adapter<CrimeHolder>() {

    override fun getItemViewType(position: Int): Int {
        val item = crimes[position]
        return if (item.requiresPolice) VIEW_TYPE_POLICE else VIEW_TYPE_NORMAL
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CrimeHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_POLICE -> {
                val binding = ListItemCrimePoliceBinding.inflate(inflater, parent, false)
                CrimePoliceHolder(binding, onCrimeClick)
            }
            else -> {
                val binding = ListItemCrimeBinding.inflate(inflater, parent, false)
                CrimeNormalHolder(binding, onCrimeClick)
            }
        }
    }

    override fun onBindViewHolder(holder: CrimeHolder, position: Int) {
        holder.bind(crimes[position])
    }

    override fun getItemCount(): Int = crimes.size
}