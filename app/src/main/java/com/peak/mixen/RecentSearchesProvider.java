package com.peak.mixen;

import android.content.SearchRecentSuggestionsProvider;

public class RecentSearchesProvider extends SearchRecentSuggestionsProvider {
    public final static String AUTHORITY = "com.peak.mixen.RecentSearchesProvider";
    public final static int MODE = DATABASE_MODE_QUERIES;

    public RecentSearchesProvider() {
        setupSuggestions(AUTHORITY, MODE);
    }
}