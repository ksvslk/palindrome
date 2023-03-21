package com.indest


import com.google.common.truth.Truth.assertThat
import com.indest.hah.other.isPalindrome
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner


@RunWith(MockitoJUnitRunner::class)
class IsPalindromeTest {

    @Test
    fun string_with_regular_letters_is_not_a_palindrome() {
        val str = "tere"
        val isPalindrome = str.isPalindrome()
        assertThat(isPalindrome).isEqualTo(false)
    }

    @Test
    fun string_sentence_with_regular_letters_is_a_palindrome() {
        val str = "aias sadas saia!"
        val isPalindrome = str.isPalindrome()
        assertThat(isPalindrome).isEqualTo(true)
    }

    @Test
    fun string_with_regular_letters_is_a_palindrome() {
        val str = "tat"
        val isPalindrome = str.isPalindrome()
        assertThat(isPalindrome).isEqualTo(true)
    }


    @Test
    fun string_where_actual_letters_count_is_less_than_3_is_not_a_palindrome() {
        val str = "tt."
        val isPalindrome = str.isPalindrome()
        assertThat(isPalindrome).isEqualTo(false)
    }


    @Test
    fun string_with_non_letters_is_not_a_palindrome_when_not_a_palindrome() {
        val str = ":=)"
        val isPalindrome = str.isPalindrome()
        assertThat(isPalindrome).isEqualTo(false)
    }


    @Test
    fun string_with_non_letters_is_not_a_palindrome_when_it_actually_is_a_palindrome() {
        val str = ":==:"
        val isPalindrome = str.isPalindrome()
        assertThat(isPalindrome).isEqualTo(false)
    }



}