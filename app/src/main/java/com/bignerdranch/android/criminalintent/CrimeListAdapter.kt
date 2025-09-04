package com.bignerdranch.android.criminalintent

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bignerdranch.android.criminalintent.databinding.ListItemCrimeBinding
import com.bignerdranch.android.criminalintent.databinding.ListItemCrimePoliceBinding

private const val VIEW_TYPE_NORMAL = 0
private const val VIEW_TYPE_POLICE = 1

// Base holder so the adapter has one generic parameter
sealed class CrimeHolder(root: View) : RecyclerView.ViewHolder(root) {
    abstract fun bind(crime: Crime)
}

// Normal row (has checkbox, no police button)
class CrimeNormalHolder(
    private val binding: ListItemCrimeBinding
) : CrimeHolder(binding.root) {

    override fun bind(crime: Crime) {
        binding.crimeTitle.text = crime.title
        binding.crimeDate.text = crime.date.toString()
        binding.crimeSolved.isChecked = crime.isSolved
    }
}

// Police row (has "Contact police" button)
class CrimePoliceHolder(
    private val binding: ListItemCrimePoliceBinding
) : CrimeHolder(binding.root) {

    private lateinit var boundCrime: Crime

    init {
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

class CrimeListAdapter(
    private val crimes: List<Crime>
) : RecyclerView.Adapter<CrimeHolder>() {

    override fun getItemViewType(position: Int): Int {
        return if (crimes[position].requiresPolice) VIEW_TYPE_POLICE else VIEW_TYPE_NORMAL
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CrimeHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_POLICE -> {
                val binding = ListItemCrimePoliceBinding.inflate(inflater, parent, false)
                CrimePoliceHolder(binding)
            }
            else -> {
                val binding = ListItemCrimeBinding.inflate(inflater, parent, false)
                CrimeNormalHolder(binding)
            }
        }
    }

    override fun onBindViewHolder(holder: CrimeHolder, position: Int) {
        holder.bind(crimes[position])
    }

    override fun getItemCount(): Int = crimes.size
}