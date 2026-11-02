package com.j4x.flowta.feature.integration.provider

import android.content.ContentProvider
import android.content.ContentValues
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.util.Log
import com.j4x.flowta.feature.integration.domain.repository.IntegrationLinkRepository
import com.j4x.flowta.feature.integration.domain.usecase.RecordExternalIncomeUseCase
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.runBlocking

/**
 * `call()`-only entry point for sibling apps signed with the same key (contract v1).
 * All real work lives in [IntegrationDispatcher]; this class only enforces the caller's
 * permission and adapts `Bundle` to the dispatcher.
 */
class IntegrationProvider : ContentProvider() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface IntegrationEntryPoint {
        fun recordExternalIncome(): RecordExternalIncomeUseCase
        fun integrationLinks(): IntegrationLinkRepository
    }

    override fun onCreate(): Boolean = true

    override fun call(method: String, arg: String?, extras: Bundle?): Bundle {
        val context = context ?: throw IllegalStateException("Provider not attached")

        // android:permission on <provider> does NOT guard call(): enforce it here.
        if (context.checkCallingPermission(IntegrationContract.PERMISSION) != PackageManager.PERMISSION_GRANTED) {
            throw SecurityException("Requires ${IntegrationContract.PERMISSION}")
        }
        val caller = callingPackage ?: throw SecurityException("Unknown calling package")

        val reply = try {
            // Resolved lazily: onCreate() runs before Application.onCreate(), Hilt is not ready yet.
            val entryPoint = EntryPointAccessors.fromApplication(
                context.applicationContext, IntegrationEntryPoint::class.java
            )
            IntegrationDispatcher(
                recordIncome = { income -> runBlocking { entryPoint.recordExternalIncome()(income) } },
                isLinked = { source -> runBlocking { entryPoint.integrationLinks().get(source) != null } }
            ).dispatch(method, caller, extras.asAccessor())
        } catch (e: Exception) {
            Log.e(TAG, "Integration call '$method' failed", e)
            IntegrationReply(IntegrationContract.STATUS_ERROR, "internal error")
        }

        return Bundle().apply {
            putString(IntegrationContract.RESULT_STATUS, reply.status)
            reply.message?.let { putString(IntegrationContract.RESULT_MESSAGE, it) }
        }
    }

    @Suppress("DEPRECATION")
    private fun Bundle?.asAccessor(): (String) -> Any? = { key ->
        if (this != null && containsKey(key)) get(key) else null
    }

    override fun query(
        uri: Uri, projection: Array<out String>?, selection: String?,
        selectionArgs: Array<out String>?, sortOrder: String?
    ): Cursor? = throw UnsupportedOperationException()

    override fun insert(uri: Uri, values: ContentValues?): Uri? = throw UnsupportedOperationException()

    override fun update(
        uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?
    ): Int = throw UnsupportedOperationException()

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int =
        throw UnsupportedOperationException()

    override fun getType(uri: Uri): String? = throw UnsupportedOperationException()

    private companion object {
        const val TAG = "IntegrationProvider"
    }
}
