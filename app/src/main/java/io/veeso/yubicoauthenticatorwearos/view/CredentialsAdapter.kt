package io.veeso.yubicoauthenticatorwearos.view

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.yubico.yubikit.oath.Code
import io.veeso.yubicoauthenticatorwearos.R
import kotlin.math.roundToInt

internal class CredentialsAdapter(private val codes: ArrayList<Totp>) :
    RecyclerView.Adapter<CredentialsAdapter.ViewHolder>() {

    internal inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        var totpTextView: TextView = view.findViewById(R.id.totp)
        var credentialNameTextView: TextView = view.findViewById(R.id.credentialName)
        var totpExpirationProgressBarView: ProgressBar = view.findViewById(R.id.totpExpiration)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.credential_list_item, parent, false)
        return ViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val code = codes[position]
        holder.totpTextView.text = code.getCode().value
        holder.credentialNameTextView.text = code.getCredential().accountName
        holder.totpExpirationProgressBarView.progress = calcExpiration(code.getCode())
    }

    override fun getItemCount(): Int {
        return codes.size
    }

    private fun calcExpiration(code: Code): Int {
        val validFrom = code.validFrom
        val validUntil = code.validUntil
        val now = System.currentTimeMillis()
        //
        val total = validUntil - validFrom
        val elapsed = now - validFrom

        if (elapsed < 0) {
            return 0
        }

        return ((elapsed / total) * 100.0).roundToInt()

    }

}
