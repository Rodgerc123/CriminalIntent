package com.bignerdranch.android.criminalintent

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bignerdranch.android.criminalintent.databinding.ListItemCrimeBinding
import com.bignerdranch.android.criminalintent.databinding.ListItemCrimePoliceBinding
import com.bignerdranch.android.criminalintent.util.toAuDisplayString
import java.util.UUID

private const val VIEW_TYPE_NORMAL = 0
private const val VIEW_TYPE_POLICE = 1

// DiffUtil tells ListAdapter what changed between two lists
private class CrimeDiff : DiffUtil.ItemCallback<Crime>() {
    override fun areItemsTheSame(old: Crime, new: Crime) = old.id == new.id
    override fun areContentsTheSame(old: Crime, new: Crime) = old == new
}

/** Base holder so adapter keeps a single VH type regardless of layout. */
sealed class CrimeHolder(root: View) : RecyclerView.ViewHolder(root) {
    abstract fun bind(crime: Crime)
}

/** Normal row: title + date + handcuffs (visible when solved). */
class CrimeNormalHolder(
    private val binding: ListItemCrimeBinding,
    private val onCrimeClick: (UUID) -> Unit
) : CrimeHolder(binding.root) {

    override fun bind(crime: Crime) {
        binding.crimeTitle.text = crime.title.ifBlank { "(Untitled crime)" }
        binding.crimeDate.text = crime.date.toAuDisplayString(binding.root.context, withWeekday = false)
        binding.crimeSolvedIcon.isVisible = crime.isSolved

        // Attach click AFTER we know which crime this row represents
        binding.root.setOnClickListener {
            android.util.Log.d("CrimeAdapter", "Row click (normal): ${crime.id}")
            onCrimeClick(crime.id) }
    }
}

/** Police row: same info + Contact Police button (kept as a Toast for now). */
class CrimePoliceHolder(
    private val binding: ListItemCrimePoliceBinding,
    private val onCrimeClick: (UUID) -> Unit
) : CrimeHolder(binding.root) {

    override fun bind(crime: Crime) {
        binding.crimeTitle.text = crime.title.ifBlank { "(Untitled crime)" }
        binding.crimeDate.text = crime.date.toAuDisplayString(binding.root.context, withWeekday = false)

        // Row click navigates to detail
        binding.root.setOnClickListener {
            android.util.Log.d("CrimeAdapter", "Row click (police): ${crime.id}")
            onCrimeClick(crime.id)
        }

        // Button click shows a toast (you can later replace with ACTION_DIAL)
        binding.contactPoliceButton.setOnClickListener {
            android.widget.Toast.makeText(
                binding.root.context,
                "Contacting police for ${crime.title}",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }
}

/**
 * ListAdapter with two row types (normal + police).
 * Fragment passes a click lambda with the clicked Crime's UUID.
 */
class CrimeListAdapter(
    private val onCrimeClick: (UUID) -> Unit
) : ListAdapter<Crime, CrimeHolder>(CrimeDiff()) {

    // Enable stable IDs for nicer animations
    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int): Long {
        val id = getItem(position).id
        // Combine UUID halves into a stable Long
        return id.mostSignificantBits xor id.leastSignificantBits
    }

    override fun getItemViewType(position: Int): Int =
        if (getItem(position).requiresPolice) VIEW_TYPE_POLICE else VIEW_TYPE_NORMAL

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CrimeHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == VIEW_TYPE_POLICE) {
            val binding = ListItemCrimePoliceBinding.inflate(inflater, parent, false)
            CrimePoliceHolder(binding, onCrimeClick)
        } else {
            val binding = ListItemCrimeBinding.inflate(inflater, parent, false)
            CrimeNormalHolder(binding, onCrimeClick)
        }
    }

    override fun onBindViewHolder(holder: CrimeHolder, position: Int) {
        val item = getItem(position)

        // Fallback: always wire a click on the actual itemView
        holder.itemView.isClickable = true
        holder.itemView.setOnClickListener {
            android.util.Log.d("CrimeAdapter", "itemView click (fallback): ${item.id}")
            onCrimeClick(item.id)
        }

        // Bind normally (holders also wire their own root click inside bind())
        holder.bind(item)
    }
}