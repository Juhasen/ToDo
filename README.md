# 📝 To-Do List App (Android)

A modern and intuitive To-Do List application that helps users manage tasks effectively. Create, edit, and track tasks, get notified before deadlines, and organize your workflow with categories and attachments.

---

## 📱 Features

### ✅ Task Management
- Create, edit, and delete tasks
- Each task includes:
    - Title
    - Description
    - Creation time
    - Deadline (due date & time)
    - Completion status (Done / Not done)
    - Notification toggle
    - Category
    - Attachments (images/files) with visual indicator

### 🔔 Notifications
- Configurable reminders before task deadlines
- Tap on a notification to open the task editor directly

### 🗂️ Task List View
- Scrollable task list using `LazyColumn`
- Sorting by deadline (most urgent tasks at the top)
- Search bar to find tasks quickly
- Visual icon for tasks with attachments

### ⚙️ Settings
- Option to hide completed tasks
- Filter visible tasks by category
- Set how many minutes before the due time a notification should appear

### 💾 Data Storage
- All tasks and settings stored locally using **SQLite**
- Attachments saved in app-specific internal storage

## 🧱 Tech Stack

- **Kotlin**
- **Jetpack Compose**
- **SQLite** for persistent local storage
- **LazyColumn** for task list
- **AlarmManager** for notifications
- **FileProvider** for securely sharing attachments

---

## 🚀 Getting Started

Clone the repo:

```bash
git clone https://github.com/your-username/todo-app.git
```