package com.kaoage.cashier.ui.selfcheckout

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.kaoage.cashier.R

class SelfCheckoutFragment : Fragment() {
    private val viewModel = SelfCheckoutPrivacyViewModel()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_self_checkout, container, false)
        val message = view.findViewById<TextView>(R.id.offlineMessage)
        message.text = viewModel.state.offlineAssurance
        return view
    }
}
