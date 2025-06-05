package pl.juhas.todo.database

enum class Category {
    NORMAL,
    WORK,
    PERSONAL,
    SHOPPING,
    URGENT,
    OTHER;

    companion object {
        fun fromString(value: String): Category {
            return Category.entries.find { it.name.equals(value, ignoreCase = true) } ?: NORMAL
        }
    }
}