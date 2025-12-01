package com.cookandroid.pauseme.util;

import android.content.Context;
import android.content.SharedPreferences;

public class PreferenceManager {
    private static final String PREF_NAME = "pause_me_pref";
    private static final String KEY_LOGGED_IN = "logged_in";
    private static final String KEY_NICKNAME = "nickname";

    private static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    // 로그인 여부 저장
    public static void setLoggedIn(Context context, boolean loggedIn) {
        getPrefs(context).edit()
                .putBoolean(KEY_LOGGED_IN, loggedIn)
                .apply();
    }

    // 로그인 여부 조회
    public static boolean isLoggedIn(Context context) {
        return getPrefs(context).getBoolean(KEY_LOGGED_IN, false);
    }

    // 닉네임 저장
    public static void setNickname(Context context, String nickname) {
        getPrefs(context).edit()
                .putString(KEY_NICKNAME, nickname)
                .apply();
    }

    // 닉네임 조회
    public static String getNickname(Context context) {
        return getPrefs(context).getString(KEY_NICKNAME, "");
    }
}
