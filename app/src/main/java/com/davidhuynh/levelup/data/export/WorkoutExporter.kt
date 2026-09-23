package com.davidhuynh.levelup.data.export

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.davidhuynh.levelup.data.repository.WorkoutRepositoryImpl
import com.davidhuynh.levelup.domain.logic.CsvExport
import com.davidhuynh.levelup.domain.model.WeightUnit
import com.davidhuynh.levelup.domain.repository.AuthRepository
import com.davidhuynh.levelup.domain.util.AppClock
import com.davidhuynh.levelup.domain.util.DataResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Writes the training history to a CSV file and hands back a shareable URI.
 *
 * The file goes in the cache directory rather than anywhere permanent: it is a copy of data
 * the app already holds, the user is about to send it somewhere, and Android is free to
 * reclaim it afterwards. Sharing goes through FileProvider, so the receiving app gets a
 * grant for this one file and nothing else.
 */
class WorkoutExporter(
    private val context: Context,
    private val workoutRepository: WorkoutRepositoryImpl,
    private val authRepository: AuthRepository,
    private val clock: AppClock,
) {

    suspend fun exportCsv(userId: String): DataResult<Export> = withContext(Dispatchers.IO) {
        val workouts = workoutRepository.observeHistory(userId).first()
        if (workouts.isEmpty()) {
            return@withContext DataResult.Failure("Log a workout first — there is nothing to export yet")
        }

        val unit = authRepository.getUser(userId)?.weightUnit ?: WeightUnit.LB
        val csv = CsvExport.toCsv(workouts, unit)

        try {
            val directory = File(context.cacheDir, "exports").apply { mkdirs() }
            // One file name per day, overwritten on repeat exports, so the cache does not
            // fill with near-identical copies.
            val file = File(directory, CsvExport.fileName(clock.today().toString()))
            file.writeText(csv)

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file,
            )
            DataResult.Success(
                Export(
                    uri = uri,
                    fileName = file.name,
                    workoutCount = workouts.size,
                    setCount = workouts.sumOf { it.setCount },
                )
            )
        } catch (e: Exception) {
            DataResult.Failure("Could not write the file: ${e.message ?: "unknown error"}")
        }
    }

    fun shareIntent(export: Export): Intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/csv"
        putExtra(Intent.EXTRA_STREAM, export.uri)
        putExtra(Intent.EXTRA_SUBJECT, export.fileName)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    data class Export(
        val uri: Uri,
        val fileName: String,
        val workoutCount: Int,
        val setCount: Int,
    )
}
