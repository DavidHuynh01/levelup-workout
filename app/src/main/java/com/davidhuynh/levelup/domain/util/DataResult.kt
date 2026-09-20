package com.davidhuynh.levelup.domain.util

/**
 * Result of an operation that can fail in a way the UI must explain to the user.
 * Kept deliberately small: a success value, or a message plus an optional field key
 * so a form can attach the error to the right input.
 */
sealed interface DataResult<out T> {
    data class Success<out T>(val value: T) : DataResult<T>
    data class Failure(val message: String, val field: String? = null) : DataResult<Nothing>
}

inline fun <T, R> DataResult<T>.map(transform: (T) -> R): DataResult<R> = when (this) {
    is DataResult.Success -> DataResult.Success(transform(value))
    is DataResult.Failure -> this
}

fun <T> DataResult<T>.valueOrNull(): T? = (this as? DataResult.Success)?.value
