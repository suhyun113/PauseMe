package com.cookandroid.pauseme;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.cookandroid.pauseme.ui.CheckInFragment;
import com.cookandroid.pauseme.ui.HomeFragment;
import com.cookandroid.pauseme.ui.MyPageFragment;
import com.cookandroid.pauseme.ui.RoutineFragment;
import com.cookandroid.pauseme.ui.TimerFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class HomeActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        bottomNavigationView = findViewById(R.id.bottom_nav);

        // 🔹 탭 선택 시 프래그먼트 교체
        bottomNavigationView.setOnItemSelectedListener(item -> {
            Fragment selected = null;
            int itemId = item.getItemId();

            if (itemId == R.id.nav_checkin) {
                selected = new CheckInFragment();
            } else if (itemId == R.id.nav_timer) {
                selected = new TimerFragment();
            } else if (itemId == R.id.nav_home) {
                selected = new HomeFragment();
            } else if (itemId == R.id.nav_routine) {
                selected = new RoutineFragment();
            } else if (itemId == R.id.nav_mypage) {
                selected = new MyPageFragment();
            }

            return loadFragment(selected);
        });

        // 처음 진입 시 "홈" 탭을 선택 상태로 만들기
        bottomNavigationView.setSelectedItemId(R.id.nav_home);
        bottomNavigationView.getMenu().findItem(R.id.nav_home).setChecked(true);
    }

    private boolean loadFragment(Fragment fragment) {
        if (fragment == null) return false;

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.main_frame, fragment)
                .commit();
        return true;
    }
}
