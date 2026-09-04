package com.etozhesandy.redpanda.core.common.error

import android.content.Context
import com.etozhesandy.redpanda.core.common.R
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.FileNotFoundException
import java.io.IOException
import javax.inject.Inject

class ErrorHandlerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : ErrorHandler {

    override fun messageFor(throwable: Throwable): String = when (throwable) {
        is FileNotFoundException -> context.getString(R.string.error_file_not_found)
        is IOException -> context.getString(R.string.error_file_read)
        else -> throwable.message ?: context.getString(R.string.error_unknown)
    }
}
