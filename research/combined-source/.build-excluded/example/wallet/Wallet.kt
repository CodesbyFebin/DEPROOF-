package com.example.wallet

import android.net.Uri
import com.solana.mobilewalletadapter.clientlib.*
import com.deproof.app.BuildConfig
import com.example.domain.*
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters
import org.bouncycastle.crypto.signers.Ed25519Signer

class Wallet(private val sender: ActivityResultSender) {
    private val adapter = MobileWalletAdapter(ConnectionIdentity(Uri.parse(BuildConfig.WALLET_IDENTITY_URI),Uri.parse("depr.svg"),"Deproof")).apply { blockchain = Solana.Devnet }
    var account: String? = null; private set
    var strict = false; private set
    suspend fun connect(): String {
        when(val r = adapter.transact(sender) { auth ->
            ensure(auth.accounts.isNotEmpty(),"NO_WALLET_ACCOUNT")
            auth.accounts.first().chains?.let { ensure(it.contains("solana:devnet"),"CLUSTER_MISMATCH") }
            // sign_transactions is optional in MWA 2.0; inspect advertised capability.
            val cap = getCapabilities()
            val supports = cap.supportedOptionalFeatures.contains("solana:signTransactions")
            Base58.encode(auth.accounts.first().publicKey) to supports
        }) {
            is TransactionResult.Success -> { val value = r.successPayload ?: throw Failure("NO_WALLET_ACCOUNT"); Base58.pubkey(value.first); account=value.first; strict=value.second; return value.first }
            is TransactionResult.NoWalletFound -> throw Failure("NO_COMPATIBLE_WALLET")
            is TransactionResult.Failure -> throw Failure("WALLET_REJECTED")
        }
    }
    suspend fun disconnect() {
        try { when(adapter.disconnect(sender)) { is TransactionResult.Success -> Unit; else -> throw Failure("WALLET_REVOCATION_UNAVAILABLE") } }
        finally { adapter.authToken=null; account=null; strict=false }
    }
    suspend fun sign(review: Review): ByteArray {
        ensure(strict,"STRICT_SIGNING_UNSUPPORTED"); ensure(account == review.context.account,"NO_WALLET_ACCOUNT")
        ensure(review.context.cluster == "devnet","CLUSTER_MISMATCH")
        val bytes = review.unsignedBytes()
        when(val r = adapter.transact(sender) { auth ->
            ensure(auth.accounts.any { Base58.encode(it.publicKey) == review.context.account },"WALLET_ACCOUNT_CHANGED")
            review.assertUnchanged(bytes,review.context)
            signTransactions(arrayOf(bytes))
        }) {
            is TransactionResult.Success -> {
                val signed = r.successPayload?.signedPayloads ?: throw Failure("STRICT_SIGNING_UNSUPPORTED")
                ensure(signed.size == 1,"WALLET_PAYLOAD_COUNT")
                validateReturnedTransaction(review,signed[0],::verifyEd25519)
                return signed[0].copyOf()
            }
            is TransactionResult.NoWalletFound -> throw Failure("NO_COMPATIBLE_WALLET")
            is TransactionResult.Failure -> throw Failure("WALLET_REJECTED")
        }
    }
}
fun verifyEd25519(key: ByteArray, message: ByteArray, sig: ByteArray): Boolean = runCatching {
    Ed25519Signer().run { init(false,Ed25519PublicKeyParameters(key,0)); update(message,0,message.size); verifySignature(sig) }
}.getOrDefault(false)
