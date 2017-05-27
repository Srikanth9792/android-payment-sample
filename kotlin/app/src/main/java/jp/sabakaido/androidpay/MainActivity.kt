package jp.sabakaido.androidpay

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.Toast
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.wallet.*
import com.google.android.gms.wallet.fragment.*


class MainActivity : AppCompatActivity(), GoogleApiClient.OnConnectionFailedListener {

    private var mWalletFragment: SupportWalletFragment? = null
    val MASKED_WALLET_REQUEST_CODE = 888
    val WALLET_FRAGMENT_ID = "wallet_fragment"

    val FULL_WALLET_REQUEST_CODE = 889
    private var mFullWallet: FullWallet? = null

    private var mMaskedWallet: MaskedWallet? = null
    private var mGoogleApiClient: GoogleApiClient? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mWalletFragment = supportFragmentManager
                .findFragmentByTag(WALLET_FRAGMENT_ID) as SupportWalletFragment?

        if (mWalletFragment == null) {
            // Wallet fragment style
            val walletFragmentStyle = WalletFragmentStyle()
                    .setBuyButtonText(WalletFragmentStyle.BuyButtonText.BUY_WITH)
                    .setBuyButtonWidth(WalletFragmentStyle.Dimension.MATCH_PARENT)

            // Wallet fragment options
            val walletFragmentOptions = WalletFragmentOptions.newBuilder()
                    .setEnvironment(WalletConstants.ENVIRONMENT_TEST)
                    .setFragmentStyle(walletFragmentStyle)
                    .setTheme(WalletConstants.THEME_LIGHT)
                    .setMode(WalletFragmentMode.BUY_BUTTON)
                    .build()

            // Initialize the WalletFragment
            val startParamsBuilder = WalletFragmentInitParams.newBuilder()
                    .setMaskedWalletRequest(generateMaskedWalletRequest())
                    .setMaskedWalletRequestCode(MASKED_WALLET_REQUEST_CODE)
                    .setAccountName("Google I/O Codelab")
            mWalletFragment = SupportWalletFragment.newInstance(walletFragmentOptions)
            mWalletFragment?.initialize(startParamsBuilder.build())

            // Add the WalletFragment to the UI
            supportFragmentManager.beginTransaction()
                    .replace(R.id.wallet_button_holder, mWalletFragment, WALLET_FRAGMENT_ID)
                    .commit();
        }

        mGoogleApiClient = GoogleApiClient.Builder(this)
                .addOnConnectionFailedListener(this)
                .enableAutoManage(this, 0, this)
                .addApi<Wallet.WalletOptions>(Wallet.API, Wallet.WalletOptions.Builder()
                        .setEnvironment(WalletConstants.ENVIRONMENT_TEST)
                        .setTheme(WalletConstants.THEME_LIGHT)
                        .build())
                .build()
    }

    override fun onConnectionFailed(p0: ConnectionResult) {
        // GoogleApiClient failed to connect, we should log the error and retry
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            MASKED_WALLET_REQUEST_CODE -> when (resultCode) {
                Activity.RESULT_OK -> {
                    mMaskedWallet = data
                            .getParcelableExtra(WalletConstants.EXTRA_MASKED_WALLET)
                    Toast.makeText(this, "Got Masked Wallet", Toast.LENGTH_SHORT).show()
                }
                Activity.RESULT_CANCELED -> {
                }
                WalletConstants.RESULT_ERROR -> Toast.makeText(this, "An Error Occurred", Toast.LENGTH_SHORT).show()
            }
            FULL_WALLET_REQUEST_CODE -> when (resultCode) {
                Activity.RESULT_OK -> {
                    mFullWallet = data
                            .getParcelableExtra(WalletConstants.EXTRA_FULL_WALLET);
                    // Show the credit card number
                    Toast.makeText(this, "Got Full Wallet, Done!", Toast.LENGTH_SHORT).show()
                }
                WalletConstants.RESULT_ERROR -> {
                    Toast.makeText(this, "An Error Occurred", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun generateMaskedWalletRequest(): MaskedWalletRequest? {
        // This is just an example publicKey for the purpose of this codelab.
        // To learn how to generate your own visit:
        // https://github.com/android-pay/androidpay-quickstart
        val publicKey = "BO39Rh43UGXMQy5PAWWe7UGWd2a9YRjNLPEEVe+zWIbdIgALcDcnYCuHbmrrzl7h8FZjl6RCzoi5/cDrqXNRVSo="
        val parameters = PaymentMethodTokenizationParameters.newBuilder()
                .setPaymentMethodTokenizationType(
                        PaymentMethodTokenizationType.NETWORK_TOKEN)
                .addParameter("publicKey", publicKey)
                .build()

        val maskedWalletRequest = MaskedWalletRequest.newBuilder()
                .setMerchantName("Google I/O Codelab")
                .setPhoneNumberRequired(true)
                .setShippingAddressRequired(true)
                .setCurrencyCode("USD")
                .setCart(Cart.newBuilder()
                        .setCurrencyCode("USD")
                        .setTotalPrice("10.00")
                        .addLineItem(LineItem.newBuilder()
                                .setCurrencyCode("USD")
                                .setDescription("Google I/O Sticker")
                                .setQuantity("1")
                                .setUnitPrice("10.00")
                                .setTotalPrice("10.00")
                                .build())
                        .build())
                .setEstimatedTotalPrice("15.00")
                .setPaymentMethodTokenizationParameters(parameters)
                .build()
        return maskedWalletRequest
    }

    private fun generateFullWalletRequest(googleTransactionId: String): FullWalletRequest {
        val fullWalletRequest = FullWalletRequest.newBuilder()
                .setGoogleTransactionId(googleTransactionId)
                .setCart(Cart.newBuilder()
                        .setCurrencyCode("USD")
                        .setTotalPrice("10.10")
                        .addLineItem(LineItem.newBuilder()
                                .setCurrencyCode("USD")
                                .setDescription("Google I/O Sticker")
                                .setQuantity("1")
                                .setUnitPrice("10.00")
                                .setTotalPrice("10.00")
                                .build())
                        .addLineItem(LineItem.newBuilder()
                                .setCurrencyCode("USD")
                                .setDescription("Tax")
                                .setRole(LineItem.Role.TAX)
                                .setTotalPrice(".10")
                                .build())
                        .build())
                .build()
        return fullWalletRequest
    }

    fun requestFullWallet(view: View) {
        if (mMaskedWallet == null) {
            Toast.makeText(this, "No masked wallet, can't confirm", Toast.LENGTH_SHORT).show()
            return
        }

        mMaskedWallet?.let {
            Wallet.Payments.loadFullWallet(mGoogleApiClient,
                    generateFullWalletRequest(it.googleTransactionId),
                    FULL_WALLET_REQUEST_CODE)
        }
    }
}
