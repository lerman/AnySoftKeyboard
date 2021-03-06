package com.anysoftkeyboard.ime;

import android.preference.PreferenceManager;

import com.anysoftkeyboard.AnySoftKeyboardBaseTest;
import com.anysoftkeyboard.SharedPrefsHelper;
import com.anysoftkeyboard.api.KeyCodes;
import com.menny.android.anysoftkeyboard.R;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.List;

@Config(sdk = 22/*the first API level to have support for those*/)
public class AnySoftKeyboardKeyboardTagsSearcherTest extends AnySoftKeyboardBaseTest {

    @Test
    @Config(sdk = 21)
    public void testDefaultFalseBeforeAPI22() {
        Assert.assertNull(mAnySoftKeyboardUnderTest.getQuickTextTagsSearcher());
    }

    @Test
    @Config(sdk = 22)
    public void testDefaultTrueAtAPI22() {
        Assert.assertNotNull(mAnySoftKeyboardUnderTest.getQuickTextTagsSearcher());
    }

    @Test
    public void testOnSharedPreferenceChangedCauseLoading() throws Exception {
        SharedPrefsHelper.setPrefsValue(R.string.settings_key_search_quick_text_tags, false);
        Assert.assertNull(mAnySoftKeyboardUnderTest.getQuickTextTagsSearcher());
        SharedPrefsHelper.setPrefsValue(R.string.settings_key_search_quick_text_tags, true);
        Object searcher = mAnySoftKeyboardUnderTest.getQuickTextTagsSearcher();
        Assert.assertNotNull(searcher);
        SharedPrefsHelper.setPrefsValue(R.string.settings_key_search_quick_text_tags, true);
        Assert.assertSame(searcher, mAnySoftKeyboardUnderTest.getQuickTextTagsSearcher());
    }

    @Test
    public void testUnrelatedOnSharedPreferenceChangedDoesNotCreateSearcher() throws Exception {
        Object searcher = mAnySoftKeyboardUnderTest.getQuickTextTagsSearcher();
        Assert.assertNotNull(searcher);
        //unrelated pref change, should not create a new searcher
        SharedPrefsHelper.setPrefsValue(R.string.settings_key_allow_suggestions_restart, false);
        Assert.assertSame(searcher, mAnySoftKeyboardUnderTest.getQuickTextTagsSearcher());

        SharedPrefsHelper.setPrefsValue(R.string.settings_key_search_quick_text_tags, false);
        Assert.assertNull(mAnySoftKeyboardUnderTest.getQuickTextTagsSearcher());

        SharedPrefsHelper.setPrefsValue(R.string.settings_key_allow_suggestions_restart, true);
        Assert.assertNull(mAnySoftKeyboardUnderTest.getQuickTextTagsSearcher());
    }

    @Test
    public void testEnabledTypingTagProvidesSuggestionsFromTagsOnly() throws Exception {
        mAnySoftKeyboardUnderTest.simulateKeyPress(':');
        verifySuggestions(true, AnySoftKeyboardKeyboardTagsSearcher.MAGNIFYING_GLASS_CHARACTER);
        mAnySoftKeyboardUnderTest.simulateTextTyping("fa");
        verifySuggestions(true, AnySoftKeyboardKeyboardTagsSearcher.MAGNIFYING_GLASS_CHARACTER+"fa");

        //now checking that suggestions will work without colon
        mAnySoftKeyboardUnderTest.simulateKeyPress(KeyCodes.DELETE);
        mAnySoftKeyboardUnderTest.simulateKeyPress(KeyCodes.DELETE);
        mAnySoftKeyboardUnderTest.simulateKeyPress(KeyCodes.DELETE);

        Assert.assertEquals("", mAnySoftKeyboardUnderTest.getCurrentInputConnectionText());

        mAnySoftKeyboardUnderTest.simulateTextTyping("fa");
        verifySuggestions(true, "fa", "face");
    }

    @Test
    public void testOnlyTagsAreSuggestedWhenTypingColon() throws Exception {
        verifyNoSuggestionsInteractions();
        mAnySoftKeyboardUnderTest.simulateKeyPress(':');
        verifySuggestions(true, AnySoftKeyboardKeyboardTagsSearcher.MAGNIFYING_GLASS_CHARACTER);
        mAnySoftKeyboardUnderTest.simulateTextTyping("face");
        List suggestions = verifyAndCaptureSuggestion(true);
        Assert.assertNotNull(suggestions);
        Assert.assertEquals(131, suggestions.size());
        Assert.assertEquals(AnySoftKeyboardKeyboardTagsSearcher.MAGNIFYING_GLASS_CHARACTER+"face", suggestions.get(0));
        Assert.assertEquals("\uD83D\uDE00", suggestions.get(1));
    }

    @Test
    public void testTagsSearchDoesNotAutoPick() throws Exception {
        verifyNoSuggestionsInteractions();
        mAnySoftKeyboardUnderTest.simulateTextTyping(":face");

        mAnySoftKeyboardUnderTest.simulateKeyPress(' ');

        Assert.assertEquals(":face ", mAnySoftKeyboardUnderTest.getCurrentInputConnectionText());
    }

    @Test
    public void testTagsSearchThrice() throws Exception {
        verifyNoSuggestionsInteractions();
        mAnySoftKeyboardUnderTest.simulateTextTyping(":face");
        List suggestions = verifyAndCaptureSuggestion(true);
        Assert.assertNotNull(suggestions);
        Assert.assertEquals(131, suggestions.size());

        mAnySoftKeyboardUnderTest.simulateKeyPress(' ');

        mAnySoftKeyboardUnderTest.simulateTextTyping(":face");
        suggestions = verifyAndCaptureSuggestion(true);
        Assert.assertNotNull(suggestions);
        Assert.assertEquals(131, suggestions.size());

        mAnySoftKeyboardUnderTest.pickSuggestionManually(1, "\uD83D\uDE00");

        mAnySoftKeyboardUnderTest.simulateTextTyping(":face");
        suggestions = verifyAndCaptureSuggestion(true);
        Assert.assertNotNull(suggestions);
        Assert.assertEquals(131, suggestions.size());
    }

    @Test
    public void testPickingEmojiOutputsToInput() throws Exception {
        verifyNoSuggestionsInteractions();
        mAnySoftKeyboardUnderTest.simulateTextTyping(":face");

        mAnySoftKeyboardUnderTest.pickSuggestionManually(1, "\uD83D\uDE00");

        verifySuggestions(true);
        Assert.assertEquals("\uD83D\uDE00", mAnySoftKeyboardUnderTest.getCurrentInputConnectionText());

        //deleting

        //correctly, this is a bug with TestInputConnection: it reports that there is one character in the input
        //but that's because it does not support deleting multi-character emojis.
        Assert.assertEquals(2, mAnySoftKeyboardUnderTest.getCurrentInputConnectionText().length());
        mAnySoftKeyboardUnderTest.simulateKeyPress(KeyCodes.DELETE);
        //so, it was two characters, and now it's one
        Assert.assertEquals(1, mAnySoftKeyboardUnderTest.getCurrentInputConnectionText().length());
    }

    @Test
    public void testPickingEmojiDoesNotTryToGetNextWords() throws Exception {
        verifyNoSuggestionsInteractions();
        mAnySoftKeyboardUnderTest.simulateTextTyping(":face");

        Mockito.reset(mAnySoftKeyboardUnderTest.getSpiedSuggest());
        mAnySoftKeyboardUnderTest.pickSuggestionManually(1, "\uD83D\uDE00");

        Mockito.verify(mAnySoftKeyboardUnderTest.getSpiedSuggest(), Mockito.never()).getNextSuggestions(Mockito.any(CharSequence.class), Mockito.anyBoolean());
    }

    @Test
    public void testPickingTypedTagDoesNotTryToAddToAutoDictionary() throws Exception {
        verifyNoSuggestionsInteractions();
        mAnySoftKeyboardUnderTest.simulateTextTyping(":face");

        Mockito.reset(mAnySoftKeyboardUnderTest.getSpiedSuggest());
        mAnySoftKeyboardUnderTest.pickSuggestionManually(0, ":face");

        Mockito.verify(mAnySoftKeyboardUnderTest.getSpiedSuggest(), Mockito.never()).isValidWord(Mockito.any(CharSequence.class));
    }

    @Test
    public void testPickingSearchCellInSuggestionsOutputTypedWord() throws Exception {
        mAnySoftKeyboardUnderTest.simulateTextTyping(":face");

        mAnySoftKeyboardUnderTest.pickSuggestionManually(0, AnySoftKeyboardKeyboardTagsSearcher.MAGNIFYING_GLASS_CHARACTER+"face");

        //outputs the typed word
        Assert.assertEquals(":face ", mAnySoftKeyboardUnderTest.getCurrentInputConnectionText());
        //clears suggestions
        verifySuggestions(true);
    }

    @Test
    public void testDisabledTypingTagDoesNotProvidesSuggestions() throws Exception {
        SharedPrefsHelper.setPrefsValue(R.string.settings_key_search_quick_text_tags, false);
        mAnySoftKeyboardUnderTest.simulateKeyPress(':');
        verifySuggestions(true);
        mAnySoftKeyboardUnderTest.simulateTextTyping("fa");
        verifySuggestions(true, "fa", "face");
    }

    @Test
    public void testQuickTextEnabledPluginsPrefsChangedCauseReload() throws Exception {
        Object searcher = mAnySoftKeyboardUnderTest.getQuickTextTagsSearcher();
        mAnySoftKeyboardUnderTest.onSharedPreferenceChanged(PreferenceManager.getDefaultSharedPreferences(RuntimeEnvironment.application),
                RuntimeEnvironment.application.getString(R.string.settings_key_ordered_active_quick_text_keys));

        Assert.assertNotSame(searcher, mAnySoftKeyboardUnderTest.getQuickTextTagsSearcher());
    }

    @Test
    public void testQuickTextEnabledPluginsPrefsChangedDoesNotCauseReloadIfTagsSearchIsDisabled() throws Exception {
        SharedPrefsHelper.setPrefsValue(R.string.settings_key_search_quick_text_tags, false);
        Assert.assertNull(mAnySoftKeyboardUnderTest.getQuickTextTagsSearcher());
        mAnySoftKeyboardUnderTest.onSharedPreferenceChanged(PreferenceManager.getDefaultSharedPreferences(RuntimeEnvironment.application),
                RuntimeEnvironment.application.getString(R.string.settings_key_ordered_active_quick_text_keys));

        Assert.assertNull(mAnySoftKeyboardUnderTest.getQuickTextTagsSearcher());
    }

    @Test
    public void testEnsureSuggestionsAreIterable() throws Exception {
        mAnySoftKeyboardUnderTest.simulateTextTyping(":face");
        List suggestions = verifyAndCaptureSuggestion(true);
        int suggestionsCount = suggestions.size();
        for (Object suggestion : suggestions) {
            Assert.assertNotNull(suggestion);
            Assert.assertTrue(suggestion instanceof CharSequence);
            suggestionsCount--;
        }
        Assert.assertEquals(0, suggestionsCount);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testRemoveIteratorUnSupported() throws Exception {
        mAnySoftKeyboardUnderTest.simulateTextTyping(":face");
        List suggestions = verifyAndCaptureSuggestion(true);
        suggestions.iterator().remove();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testAddingAtIndexToSuggestionsUnSupported() throws Exception {
        mAnySoftKeyboardUnderTest.simulateTextTyping(":face");
        List suggestions = verifyAndCaptureSuggestion(true);
        suggestions.add(0, "demo");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testAddingToSuggestionsUnSupported() throws Exception {
        mAnySoftKeyboardUnderTest.simulateTextTyping(":face");
        List suggestions = verifyAndCaptureSuggestion(true);
        suggestions.add("demo");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testListIteratorUnSupported() throws Exception {
        mAnySoftKeyboardUnderTest.simulateTextTyping(":face");
        List suggestions = verifyAndCaptureSuggestion(true);
        suggestions.listIterator();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testRemoteAtIndexUnSupported() throws Exception {
        mAnySoftKeyboardUnderTest.simulateTextTyping(":face");
        List suggestions = verifyAndCaptureSuggestion(true);
        suggestions.remove(0);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testRemoteObjectUnSupported() throws Exception {
        mAnySoftKeyboardUnderTest.simulateTextTyping(":face");
        List suggestions = verifyAndCaptureSuggestion(true);
        suggestions.remove("DEMO");
    }

}