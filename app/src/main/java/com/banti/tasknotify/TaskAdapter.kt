package com.banti.tasknotify

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView

class TaskAdapter(
    private var tasks: List<Task>,
    private val onTaskClick: (Task) -> Unit,
    private val onTaskComplete: (Task) -> Unit,
    private val onTaskDelete: (Task) -> Unit
) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    class TaskViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val cardView: CardView = view.findViewById(R.id.cardTask)
        val checkBox: CheckBox = view.findViewById(R.id.checkBoxComplete)
        val tvTaskName: TextView = view.findViewById(R.id.tvTaskName)
        val tvCategory: TextView = view.findViewById(R.id.tvCategory)
        val tvPriority: TextView = view.findViewById(R.id.tvPriority)
        val tvTime: TextView = view.findViewById(R.id.tvTime)
        val tvRecurring: TextView = view.findViewById(R.id.tvRecurring)
        val btnEdit: ImageButton = view.findViewById(R.id.btnEdit)
        val btnDelete: ImageButton = view.findViewById(R.id.btnDelete)
        val priorityIndicator: View = view.findViewById(R.id.priorityIndicator)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_task, parent, false)
        return TaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = tasks[position]

        holder.checkBox.isChecked = task.completed
        holder.tvTaskName.text = task.name
        holder.tvCategory.text = task.category
        holder.tvPriority.text = task.priority
        holder.tvTime.text = "⏰ ${task.time}"
        holder.tvRecurring.text = task.recurring

        // Strike through if completed
        if (task.completed) {
            holder.tvTaskName.paintFlags = holder.tvTaskName.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            holder.cardView.alpha = 0.7f
        } else {
            holder.tvTaskName.paintFlags = holder.tvTaskName.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            holder.cardView.alpha = 1.0f
        }

        // Priority indicator color
        val indicatorColor = when (task.priority) {
            "High" -> android.graphics.Color.parseColor("#FF5252")
            "Medium" -> android.graphics.Color.parseColor("#FFA726")
            else -> android.graphics.Color.parseColor("#66BB6A")
        }
        holder.priorityIndicator.setBackgroundColor(indicatorColor)

        // Category background
        val categoryBg = when (task.category) {
            "Work" -> R.drawable.badge_work
            "Health" -> R.drawable.badge_health
            "Shopping" -> R.drawable.badge_shopping
            "Study" -> R.drawable.badge_study
            else -> R.drawable.badge_personal
        }
        holder.tvCategory.setBackgroundResource(categoryBg)

        // Animations
        val animation = AnimationUtils.loadAnimation(holder.itemView.context, R.anim.item_animation)
        holder.cardView.startAnimation(animation)

        // Click listeners
        holder.checkBox.setOnClickListener { onTaskComplete(task) }
        holder.btnEdit.setOnClickListener { onTaskClick(task) }
        holder.btnDelete.setOnClickListener { onTaskDelete(task) }
        holder.cardView.setOnClickListener { onTaskClick(task) }
    }

    override fun getItemCount() = tasks.size

    fun updateList(newTasks: List<Task>) {
        tasks = newTasks
        notifyDataSetChanged()
    }
}