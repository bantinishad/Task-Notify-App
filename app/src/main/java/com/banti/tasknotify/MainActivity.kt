package com.banti.tasknotify

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.*

class MainActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var taskAdapter: TaskAdapter
    private lateinit var fabAdd: FloatingActionButton
    private lateinit var chipAll: TextView
    private lateinit var chipPending: TextView
    private lateinit var chipCompleted: TextView
    private lateinit var tvTotalCount: TextView
    private lateinit var tvCompletedCount: TextView
    private lateinit var tvPendingCount: TextView
    private lateinit var tvHighCount: TextView
    private val tasks = mutableListOf<Task>()

    companion object {
        const val CHANNEL_ID = "task_reminder_channel"
        const val PREFS_NAME = "TaskPrefs"
        const val TASKS_KEY = "tasks"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        createNotificationChannel()
        initViews()
        loadTasks()
        setupRecyclerView()
        setupListeners()
        updateStats()
    }

    private fun initViews() {
        recyclerView = findViewById(R.id.recyclerViewTasks)
        fabAdd = findViewById(R.id.fabAddTask)
        chipAll = findViewById(R.id.chipAll)
        chipPending = findViewById(R.id.chipPending)
        chipCompleted = findViewById(R.id.chipCompleted)
        tvTotalCount = findViewById(R.id.tvTotalCount)
        tvCompletedCount = findViewById(R.id.tvCompletedCount)
        tvPendingCount = findViewById(R.id.tvPendingCount)
        tvHighCount = findViewById(R.id.tvHighCount)
    }

    private fun setupRecyclerView() {
        taskAdapter = TaskAdapter(tasks,
            onTaskClick = { task -> editTask(task) },
            onTaskComplete = { task -> toggleTaskComplete(task) },
            onTaskDelete = { task -> deleteTask(task) }
        )
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = taskAdapter

        // Add animation
        val layoutAnimation = AnimationUtils.loadLayoutAnimation(this, R.anim.layout_animation_fall_down)
        recyclerView.layoutAnimation = layoutAnimation
    }

    private fun setupListeners() {
        fabAdd.setOnClickListener {
            showAddTaskDialog(null)
        }

        chipAll.setOnClickListener {
            setActiveChip(chipAll)
            filterTasks("All")
        }
        chipPending.setOnClickListener {
            setActiveChip(chipPending)
            filterTasks("Pending")
        }
        chipCompleted.setOnClickListener {
            setActiveChip(chipCompleted)
            filterTasks("Completed")
        }
    }

    private fun setActiveChip(activeChip: TextView) {
        listOf(chipAll, chipPending, chipCompleted).forEach { chip ->
            if (chip == activeChip) {
                chip.setBackgroundResource(R.drawable.chip_selected)
                chip.setTextColor(resources.getColor(android.R.color.white, null))
            } else {
                chip.setBackgroundResource(R.drawable.chip_unselected)
                chip.setTextColor(resources.getColor(R.color.purple_500, null))
            }
        }
    }

    private fun showAddTaskDialog(taskToEdit: Task?) {
        val dialog = android.app.AlertDialog.Builder(this, R.style.RoundedDialog)
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_task, null)

        val etTaskName = dialogView.findViewById<EditText>(R.id.etTaskName)
        val spinnerCategory = dialogView.findViewById<Spinner>(R.id.spinnerCategory)
        val spinnerPriority = dialogView.findViewById<Spinner>(R.id.spinnerPriority)
        val timePicker = dialogView.findViewById<TimePicker>(R.id.timePicker)
        val spinnerRecurring = dialogView.findViewById<Spinner>(R.id.spinnerRecurring)

        val categories = arrayOf("Personal", "Work", "Health", "Shopping", "Study", "Other")
        val priorities = arrayOf("Low", "Medium", "High")
        val recurring = arrayOf("None", "Daily", "Weekly", "Monthly")

        // Setup Category Spinner
        val categoryAdapter = ArrayAdapter(this, R.layout.spinner_selected_item, categories)
        categoryAdapter.setDropDownViewResource(R.layout.spinner_item)
        spinnerCategory.adapter = categoryAdapter

        // Setup Priority Spinner
        val priorityAdapter = ArrayAdapter(this, R.layout.spinner_selected_item, priorities)
        priorityAdapter.setDropDownViewResource(R.layout.spinner_item)
        spinnerPriority.adapter = priorityAdapter

        // Setup Recurring Spinner
        val recurringAdapter = ArrayAdapter(this, R.layout.spinner_selected_item, recurring)
        recurringAdapter.setDropDownViewResource(R.layout.spinner_item)
        spinnerRecurring.adapter = recurringAdapter

        // If editing, populate fields
        taskToEdit?.let {
            etTaskName.setText(it.name)
            spinnerCategory.setSelection(categories.indexOf(it.category))
            spinnerPriority.setSelection(priorities.indexOf(it.priority))
            spinnerRecurring.setSelection(recurring.indexOf(it.recurring))
            val timeParts = it.time.split(":")
            timePicker.hour = timeParts[0].toInt()
            timePicker.minute = timeParts[1].toInt()
        }

        dialog.setView(dialogView)
            .setPositiveButton("💾 Save Task") { _, _ ->
                val taskName = etTaskName.text.toString()
                if (taskName.isNotEmpty()) {
                    val time = String.format("%02d:%02d", timePicker.hour, timePicker.minute)

                    if (taskToEdit == null) {
                        val task = Task(
                            id = UUID.randomUUID().toString(),
                            name = taskName,
                            category = spinnerCategory.selectedItem.toString(),
                            priority = spinnerPriority.selectedItem.toString(),
                            time = time,
                            recurring = spinnerRecurring.selectedItem.toString(),
                            completed = false,
                            createdAt = System.currentTimeMillis()
                        )
                        tasks.add(0, task)
                        scheduleNotification(task)
                    } else {
                        taskToEdit.name = taskName
                        taskToEdit.category = spinnerCategory.selectedItem.toString()
                        taskToEdit.priority = spinnerPriority.selectedItem.toString()
                        taskToEdit.time = time
                        taskToEdit.recurring = spinnerRecurring.selectedItem.toString()
                        scheduleNotification(taskToEdit)
                    }

                    saveTasks()
                    taskAdapter.notifyDataSetChanged()
                    recyclerView.scheduleLayoutAnimation()
                    updateStats()
                    Toast.makeText(this, "✅ Task saved successfully!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "⚠️ Please enter task name", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("❌ Cancel", null)
            .show()
    }

    private fun editTask(task: Task) {
        showAddTaskDialog(task)
    }

    private fun toggleTaskComplete(task: Task) {
        task.completed = !task.completed
        saveTasks()
        taskAdapter.notifyDataSetChanged()
        updateStats()
    }

    private fun deleteTask(task: Task) {
        android.app.AlertDialog.Builder(this, R.style.RoundedDialog)
            .setTitle("Delete Task")
            .setMessage("Are you sure you want to delete this task?")
            .setPositiveButton("Delete") { _, _ ->
                tasks.remove(task)
                cancelNotification(task)
                saveTasks()
                taskAdapter.notifyDataSetChanged()
                updateStats()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun filterTasks(filter: String) {
        val filteredList = when (filter) {
            "All" -> tasks
            "Pending" -> tasks.filter { !it.completed }
            "Completed" -> tasks.filter { it.completed }
            else -> tasks
        }
        taskAdapter.updateList(filteredList)
    }

    private fun updateStats() {
        val total = tasks.size
        val completed = tasks.count { it.completed }
        val pending = total - completed
        val high = tasks.count { it.priority == "High" && !it.completed }

        tvTotalCount.text = total.toString()
        tvCompletedCount.text = completed.toString()
        tvPendingCount.text = pending.toString()
        tvHighCount.text = high.toString()
    }

    private fun scheduleNotification(task: Task) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, NotificationReceiver::class.java).apply {
            putExtra("task_name", task.name)
            putExtra("task_id", task.id)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            this,
            task.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            val timeParts = task.time.split(":")
            set(Calendar.HOUR_OF_DAY, timeParts[0].toInt())
            set(Calendar.MINUTE, timeParts[1].toInt())
            set(Calendar.SECOND, 0)

            if (timeInMillis < System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        when (task.recurring) {
            "Daily" -> alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                AlarmManager.INTERVAL_DAY,
                pendingIntent
            )
            else -> alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        }
    }

    private fun cancelNotification(task: Task) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, NotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            task.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Task Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Channel for task reminder notifications"
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun saveTasks() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val gson = Gson()
        val json = gson.toJson(tasks)
        prefs.edit().putString(TASKS_KEY, json).apply()
    }

    private fun loadTasks() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val gson = Gson()
        val json = prefs.getString(TASKS_KEY, null)
        if (json != null) {
            val type = object : TypeToken<MutableList<Task>>() {}.type
            val loadedTasks: MutableList<Task> = gson.fromJson(json, type)
            tasks.clear()
            tasks.addAll(loadedTasks)
        }
    }
}