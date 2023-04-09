package com.aamba.tarabuja

import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment


class PermMan()
{
    private var activity: AppCompatActivity? = null
    private lateinit var permissions: Array<String>
    private var result: PermResult? = null
    private val requestedItems = ArrayList<Boolean>()


    fun activity(activity: AppCompatActivity): PermMan
    {
        this.activity = activity
        return this
    }

    fun permission(vararg permissions: String): PermMan
    {
        this.permissions = permissions as Array<String>
        return this
    }

    fun listener(listener: PermResult): PermMan
    {
        this.result = listener
        return this
    }

    fun ask()
    {
        if (requestedItems.size < permissions.size)
        {
            val requestPermissionLauncher =
                activity?.registerForActivityResult(
                    RequestPermission()
                ) { isGranted: Boolean ->
                    requestedItems.add(isGranted)
                    ask()
                }

            requestPermissionLauncher?.launch(permissions[requestedItems.size]) ?: throw Exception("No Activity or Fragment is provided")
        } else
        {
            result?.let {
                requestedItems.forEach { item ->
                    if (!item)
                    {
                        it.denied()
                        return
                    }
                }
                it.granted()
            }
        }
    }

    companion object
    {
        fun build(): PermMan = PermMan()

        fun isGranted(context: Context, vararg permissions: String): Boolean
        {
            for (permission in permissions)
            {
                if (ContextCompat
                        .checkSelfPermission(
                            context,
                            permission
                        ) != PackageManager.PERMISSION_GRANTED
                )
                {
                    return false
                }
            }
            return true
        }
    }

    open class PermResult
    {
        open fun granted()
        {
        }

        open fun denied()
        {
        }
    }
}