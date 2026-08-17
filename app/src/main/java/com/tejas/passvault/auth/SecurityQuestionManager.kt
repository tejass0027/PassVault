package com.tejas.passvault.auth

import com.tejas.passvault.crypto.DekWrapper
import com.tejas.passvault.crypto.KeyDerivation

/**
 * Recovery path for when the user can't log in with biometric+pattern (new device,
 * forgot the pattern, etc). Answers are never stored - like the pattern, they only
 * ever exist transiently to derive a key that unwraps the same DEK used everywhere
 * else, so recovering via security questions and logging in via pattern reach the
 * exact same vault.
 */
class SecurityQuestionManager(private val authPrefs: AuthPrefs) {

    companion object {
        const val REQUIRED_QUESTION_COUNT = 3

        val SUGGESTED_QUESTIONS = listOf(
            "What was the name of your first pet?",
            "What city were you born in?",
            "What was the model of your first car?",
            "What is your mother's maiden name?",
            "What was the name of your first school?",
            "What is your favorite childhood nickname?"
        )
    }

    fun hasSecurityQuestions(): Boolean =
        authPrefs.securitySalt() != null && authPrefs.securityWrappedDek() != null

    fun questions(): List<String> = authPrefs.securityQuestions()

    /** Wraps [dek] under a key derived from [answers] (same order as [questions]) and persists it. */
    fun setQuestions(questions: List<String>, answers: List<String>, dek: ByteArray) {
        require(questions.size == REQUIRED_QUESTION_COUNT && answers.size == REQUIRED_QUESTION_COUNT) {
            "Exactly $REQUIRED_QUESTION_COUNT security questions are required"
        }
        val salt = KeyDerivation.newSalt()
        val wrapped = DekWrapper.wrap(dek, answers.toCharArray(), salt)
        authPrefs.saveSecurityWrap(salt, wrapped, questions)
    }

    /** Returns the DEK if [answers] (same order as [questions]) are correct, or null otherwise. */
    fun tryUnlock(answers: List<String>): ByteArray? {
        val salt = authPrefs.securitySalt() ?: return null
        val wrapped = authPrefs.securityWrappedDek() ?: return null
        return DekWrapper.tryUnwrap(wrapped, answers.toCharArray(), salt)
    }

    private fun List<String>.toCharArray(): CharArray =
        joinToString(separator = "|") { it.trim().lowercase() }.toCharArray()
}
