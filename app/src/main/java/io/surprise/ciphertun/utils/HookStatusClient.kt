package io.surprise.ciphertun.utils

import android.content.Context
import android.content.pm.PackageInfo
import io.surprise.ciphertun.bg.ParceledListSlice
import io.surprise.ciphertun.xposed.HookStatusKeys
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object HookStatusClient {
    data class Status(val active: Boolean, val lastPatchedAt: Long, val version: Int, val systemPid: Int)

    private val statusFlow = MutableStateFlow<Status?>(null)
    val status: StateFlow<Status?> = statusFlow

    @Volatile
    private var appContext: Context? = null

    fun register(context: Context) {
        appContext = context.applicationContext
        refresh()
    }

    fun refresh() {
        refreshBlocking()
    }

    suspend fun refreshAsync() {
        withContext(Dispatchers.IO) {
            refreshBlocking()
        }
    }

    private fun refreshBlocking() {
        val context = appContext ?: return

        try {
            val binder = ConnectivityBinderUtils.getBinder(context) ?: run {
                statusFlow.value = null
                return
            }

            if (!binder.isBinderAlive) {
                statusFlow.value = null
                return
            }

            ConnectivityBinderUtils.withParcel { data, reply ->
                try {
                    data.writeInterfaceToken(HookStatusKeys.DESCRIPTOR)

                    val ok = binder.transact(
                        HookStatusKeys.TRANSACTION_STATUS,
                        data,
                        reply,
                        0,
                    )

                    if (!ok) {
                        statusFlow.value = null
                        return@withParcel
                    }

                    reply.readException()

                    statusFlow.value = Status(
                        active = reply.readInt() != 0,
                        lastPatchedAt = reply.readLong(),
                        version = reply.readInt(),
                        systemPid = reply.readInt(),
                    )
                } catch (t: Throwable) {
                    statusFlow.value = null
                    android.util.Log.w(
                        "HookStatusClient",
                        "Hook status refresh failed safely",
                        t,
                    )
                }
            }
        } catch (t: Throwable) {
            statusFlow.value = null
            android.util.Log.w(
                "HookStatusClient",
                "Unable to query hook status",
                t,
            )
        }
    }

    fun getInstalledPackages(context: Context, flags: Long, userId: Int): List<PackageInfo>? {
        val binder = ConnectivityBinderUtils.getBinder(context) ?: return null
        return ConnectivityBinderUtils.withParcel { data, reply ->
            data.writeInterfaceToken(HookStatusKeys.DESCRIPTOR)
            data.writeLong(flags)
            data.writeInt(userId)
            val ok = binder.transact(HookStatusKeys.TRANSACTION_GET_INSTALLED_PACKAGES, data, reply, 0)
            if (!ok) return@withParcel null
            reply.readException()
            val slice = ParceledListSlice.CREATOR.createFromParcel(reply, PackageInfo::class.java.classLoader)
            @Suppress("UNCHECKED_CAST")
            (slice as ParceledListSlice<PackageInfo>).list
        }
    }
}
