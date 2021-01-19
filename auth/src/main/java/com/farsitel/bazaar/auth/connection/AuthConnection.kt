package com.farsitel.bazaar.auth.connection

import android.content.Context
import android.os.Bundle
import androidx.lifecycle.LifecycleOwner
import com.farsitel.bazaar.BazaarResponse
import com.farsitel.bazaar.auth.callback.BazaarSignInCallback
import com.farsitel.bazaar.auth.model.BazaarSignInAccount
import com.farsitel.bazaar.util.InAppLoginLogger

internal abstract class AuthConnection(private val context: Context) {

    abstract fun getLastAccountId(owner: LifecycleOwner?, callback: BazaarSignInCallback)
    abstract fun getLastAccountIdSync(owner: LifecycleOwner?): BazaarResponse<BazaarSignInAccount>?

    fun getLastAccountResponse(extras: Bundle?): BazaarResponse<BazaarSignInAccount> {
        return if (AuthResponseHandler.isSuccessful(extras)) {
            val account = AuthResponseHandler.getAccountByBundle(
                requireNotNull(extras)
            )
            BazaarResponse(isSuccessful = true, data = account)
        } else {
            val errorResponse = AuthResponseHandler.getErrorResponse(extras)
            InAppLoginLogger.logError(errorResponse.errorMessage)
            BazaarResponse(isSuccessful = false, errorResponse = errorResponse)
        }
    }

    companion object {
        const val PACKAGE_NAME_KEY = "packageName"

        private lateinit var authConnection: AuthConnection
        private val lockObject = Object()

        fun getAuthConnection(context: Context): AuthConnection {
            if (!::authConnection.isInitialized) {
                synchronized(lockObject) {
                    if (!::authConnection.isInitialized) {
                        initializeAuthConnection(context)

                    }
                }
            }
            return authConnection
        }

        private fun initializeAuthConnection(context: Context) {
            val serviceConnection = ServiceAuthConnection(context)
            val canConnectWithService = serviceConnection.connect()

            authConnection = if (canConnectWithService) {
                serviceConnection
            } else {
                ReceiverAuthConnection(context)
            }
        }
    }
}