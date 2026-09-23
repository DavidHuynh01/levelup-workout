package com.davidhuynh.levelup.domain.util

sealed interface DataResult<out T> {
    data class Success<out T>(val value: T) : DataResult<T>
    data class Failure(val message: String, val field: String? = null) : DataResult<Nothing>
}

inline fun <T, R> DataResult<T>.map(transform: (T) -> R): DataResult<R> = when (this) {
    is DataResult.Success -> DataResult.Success(transform(value))
    is DataResult.Failure -> this
}

fun <T> DataResult<T>.valueOrNull(): T? = (this as? DataResult.Success)?.value
