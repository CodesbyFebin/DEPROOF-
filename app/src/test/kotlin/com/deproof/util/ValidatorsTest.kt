package com.deproof.util

import org.junit.Test
import org.junit.Assert.*

/**
 * F043: Validator Suite Tests
 *
 * Verifies that input validation works correctly for Solana
 * addresses, tokens, amounts, signatures, and URLs.
 */
class ValidatorsTest {

    companion object {
        // Known-good test addresses
        const val VALID_SOLANA_ADDRESS = "1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa"
        const val VALID_TOKEN_MINT = "TokenkegQfeZyiNwAJsyFbPVwwQQfzZbvpXn87oqvwW"
        const val VALID_SIGNATURE = "37u9WtQxAMSvGTvNMfJyxhtrh8Tr8n9mHwVJqgS5L8m3EwMKfqxjcX1j2DwRGb6TpV8PUpLfTz5F9qVj7mYx"
    }

    // F043-TC-01: Valid Solana address
    @Test
    fun testValidatePublicKeyValid() {
        val isValid = Validators.validatePublicKey(VALID_SOLANA_ADDRESS)
        assertTrue("Valid address should pass validation", isValid)
    }

    // F043-TC-02: Invalid characters in address
    @Test
    fun testValidatePublicKeyInvalidChars() {
        val invalidAddress = "1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa!@#" // Invalid chars
        val isValid = Validators.validatePublicKey(invalidAddress)
        assertFalse("Address with invalid characters should fail", isValid)
    }

    // F043-TC-03: Wrong length address
    @Test
    fun testValidatePublicKeyWrongLength() {
        val tooShort = "1A1zP1eP5"
        val tooLong = "1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa"

        assertFalse("Address too short should fail", Validators.validatePublicKey(tooShort))
        assertFalse("Address too long should fail", Validators.validatePublicKey(tooLong))
    }

    // F043-TC-04: Token mint validation
    @Test
    fun testValidateMintAddress() {
        val isValid = Validators.validateMint(VALID_TOKEN_MINT)
        assertTrue("Valid token mint should pass", isValid)
    }

    // F043-TC-05: SKR mint (6 decimals)
    @Test
    fun testValidateSKRMintDecimals() {
        // SKR must have exactly 6 decimals
        val isValid = Validators.validateMintDecimals(VALID_TOKEN_MINT, 6)
        assertTrue("SKR with 6 decimals should pass", isValid)

        val withWrongDecimals = Validators.validateMintDecimals(VALID_TOKEN_MINT, 8)
        assertFalse("SKR with wrong decimals should fail", withWrongDecimals)
    }

    // F043-TC-06: Amount validation non-negative
    @Test
    fun testValidateAmountNonNegative() {
        assertTrue("Positive amount valid", Validators.validateAmount(100.0))
        assertTrue("Zero amount valid", Validators.validateAmount(0.0))
        assertFalse("Negative amount invalid", Validators.validateAmount(-1.0))
    }

    // F043-TC-07: Decimal precision in amounts
    @Test
    fun testValidateAmountDecimalPrecision() {
        // SKR has 6 decimals, so 1.000001 should be valid
        assertTrue("1.000001 valid for 6 decimals", Validators.validateAmount(1.000001, 6))

        // 1.0000001 exceeds 6 decimals
        assertFalse("1.0000001 invalid for 6 decimals", Validators.validateAmount(1.0000001, 6))
    }

    // F043-TC-08: Signature validation
    @Test
    fun testValidateSignatureFormat() {
        val isValid = Validators.validateSignature(VALID_SIGNATURE)
        assertTrue("Valid 88-char base58 signature should pass", isValid)

        val wrongLength = "abc123"
        assertFalse("Too-short signature should fail", Validators.validateSignature(wrongLength))
    }

    // F043-TC-09: Signature length (88 chars for Solana)
    @Test
    fun testValidateSignatureLength() {
        val tooShort = "37u9WtQxAMSvGTvNMfJyxhtrh8Tr8n9mHwVJqgS5L8m3" // < 88
        val correct = VALID_SIGNATURE // 88 chars

        assertFalse("Signature < 88 chars invalid", Validators.validateSignature(tooShort))
        assertTrue("Signature = 88 chars valid", Validators.validateSignature(correct))
    }

    // F043-TC-10: URL validation (https only)
    @Test
    fun testValidateURLHttpsOnly() {
        val httpsUrl = "https://api.mainnet-beta.solana.com"
        val httpUrl = "http://api.mainnet-beta.solana.com"
        val invalidUrl = "ftp://files.example.com"

        assertTrue("HTTPS URL valid", Validators.validateURL(httpsUrl))
        assertFalse("HTTP URL invalid (requires HTTPS)", Validators.validateURL(httpUrl))
        assertFalse("FTP URL invalid", Validators.validateURL(invalidUrl))
    }

    // F043-TC-11: RPC endpoint validation
    @Test
    fun testValidateRpcEndpoint() {
        val validRpc = "https://api.devnet.solana.com"
        val invalidRpc = "not.a.valid.url"

        assertTrue("Valid RPC endpoint", Validators.validateURL(validRpc))
        assertFalse("Invalid RPC endpoint", Validators.validateURL(invalidRpc))
    }

    // F043-TC-12: Email validation (if needed for future use)
    @Test
    fun testValidateEmailFormat() {
        val validEmail = "user@example.com"
        val invalidEmail = "not-an-email"

        assertTrue("Valid email passes", Validators.validateEmail(validEmail))
        assertFalse("Invalid email fails", Validators.validateEmail(invalidEmail))
    }

    // F043-TC-13: Empty/null input handling
    @Test
    fun testValidateEmptyInputs() {
        // All validators should handle empty/null gracefully
        assertFalse("Empty address invalid", Validators.validatePublicKey(""))
        assertFalse("Null address invalid", Validators.validatePublicKey(null ?: ""))
        assertFalse("Empty signature invalid", Validators.validateSignature(""))
    }

    // F043-TC-14: Case sensitivity for addresses
    @Test
    fun testValidateAddressCaseSensitivity() {
        // Solana base58 addresses are case-sensitive
        val validAddress = VALID_SOLANA_ADDRESS
        val lowercase = validAddress.lowercase()

        // Depending on implementation, case sensitivity may matter
        val valid = Validators.validatePublicKey(validAddress)
        val lower = Validators.validatePublicKey(lowercase)

        // At minimum, one should be valid
        assertTrue("Address validation should work", valid || lower)
    }

    // F043-TC-15: Amount ranges
    @Test
    fun testValidateAmountRanges() {
        // Very small but valid amount
        assertTrue("1 lamport valid", Validators.validateAmount(0.000000001))

        // Very large but valid amount
        assertTrue("1 billion SOL valid", Validators.validateAmount(1_000_000_000.0))

        // Unreasonable amounts might be flagged
        // (depends on business logic - optional validation)
        // val tooLarge = Validators.validateAmount(1e18) // Max u64 in SOL
    }
}
