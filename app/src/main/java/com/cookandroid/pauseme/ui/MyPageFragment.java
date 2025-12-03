package com.cookandroid.pauseme.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.cookandroid.pauseme.R;
import com.google.android.material.bottomsheet.BottomSheetDialog;

public class MyPageFragment extends Fragment {

    // 헤더 / 포인트
    private TextView txtProfileName;
    private TextView txtPointValue;

    // 임팩트, 월별 활동 등 더미 데이터
    private TextView txtImpactPointValue;
    private TextView txtImpactPeopleValue;
    private TextView txtMonthCheckinCount;
    private TextView txtMonthRoutineCount;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_mypage, container, false);

        // ---- 뷰 찾기 ----
        txtProfileName = root.findViewById(R.id.txt_profile_name);
        txtPointValue = root.findViewById(R.id.txt_point_value);

        txtImpactPointValue = root.findViewById(R.id.txt_impact_point_value);
        txtImpactPeopleValue = root.findViewById(R.id.txt_impact_people_value);
        txtMonthCheckinCount = root.findViewById(R.id.txt_month_checkin_count);
        txtMonthRoutineCount = root.findViewById(R.id.txt_month_routine_count);

        Button btnOpenDonate = root.findViewById(R.id.btn_open_donate);

        // ---- 더미 데이터 ----
        String nickname = "포즈미";
        int currentPoint = 1250;
        int donatedPoint = 850;
        int helpedPeople = 8;
        int monthCheckin = 25;
        int monthCheckinGoal = 30;
        int monthRoutine = 18;
        int monthRoutineGoal = 20;

        txtProfileName.setText(nickname + "님");
        txtPointValue.setText(currentPoint + "P");
        txtImpactPointValue.setText(donatedPoint + "P");
        txtImpactPeopleValue.setText(helpedPeople + "명");
        txtMonthCheckinCount.setText(monthCheckin + " / " + monthCheckinGoal);
        txtMonthRoutineCount.setText(monthRoutine + " / " + monthRoutineGoal);

        // 기부하기 버튼 -> 바텀시트
        btnOpenDonate.setOnClickListener(v -> showDonateBottomSheet(currentPoint));

        return root;
    }

    /** 기부 바텀시트 */
    private void showDonateBottomSheet(int currentPoint) {
        if (getContext() == null) return;

        // 스타일은 굳이 안 줘도 됨 (버전마다 상수 이름이 달라서 오류 날 수 있음)
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        View sheet = LayoutInflater.from(getContext())
                .inflate(R.layout.bottomsheet_donate, null);
        dialog.setContentView(sheet);

        // 상단 텍스트
        TextView txtPointInfo = sheet.findViewById(R.id.txt_donate_point_info);
        if (txtPointInfo != null) {
            txtPointInfo.setText("보유 포인트: " + currentPoint + "P");
        }

        // 단체 카드들
        LinearLayout cardOrg1 = sheet.findViewById(R.id.card_org_1);
        LinearLayout cardOrg2 = sheet.findViewById(R.id.card_org_2);
        LinearLayout cardOrg3 = sheet.findViewById(R.id.card_org_3);

        // 포인트 칩
        TextView chip100 = sheet.findViewById(R.id.chip_point_100);
        TextView chip300 = sheet.findViewById(R.id.chip_point_300);
        TextView chip500 = sheet.findViewById(R.id.chip_point_500);
        TextView chip1000 = sheet.findViewById(R.id.chip_point_1000);

        TextView txtMessage = sheet.findViewById(R.id.txt_donate_message);
        Button btnDonateConfirm = sheet.findViewById(R.id.btn_donate_confirm);

        // 선택 상태
        final String[] selectedOrg = {null};
        final int[] selectedPoint = {0};

        // 단체 선택
        View.OnClickListener orgClickListener = v -> {
            if (getContext() == null) return;

            resetOrgCardBackground(cardOrg1, cardOrg2, cardOrg3);

            v.setBackgroundResource(R.drawable.bg_chip_solid_purple);
            if (txtMessage != null) {
                txtMessage.setText("기부 단체와 포인트를 선택해 주세요");
            }

            int id = v.getId();
            if (id == R.id.card_org_1) {
                selectedOrg[0] = "청소년 마음건강센터";
            } else if (id == R.id.card_org_2) {
                selectedOrg[0] = "청년 멘탈케어";
            } else if (id == R.id.card_org_3) {
                selectedOrg[0] = "희망의 심리학";
            }
        };

        if (cardOrg1 != null) cardOrg1.setOnClickListener(orgClickListener);
        if (cardOrg2 != null) cardOrg2.setOnClickListener(orgClickListener);
        if (cardOrg3 != null) cardOrg3.setOnClickListener(orgClickListener);

        // 포인트 선택
        View.OnClickListener pointClickListener = v -> {
            if (getContext() == null) return;

            resetPointChips(chip100, chip300, chip500, chip1000);

            TextView tv = (TextView) v;
            tv.setBackgroundResource(R.drawable.bg_chip_solid_purple);
            tv.setTextColor(ContextCompat.getColor(requireContext(), R.color.white));

            int id = v.getId();
            if (id == R.id.chip_point_100) {
                selectedPoint[0] = 100;
            } else if (id == R.id.chip_point_300) {
                selectedPoint[0] = 300;
            } else if (id == R.id.chip_point_500) {
                selectedPoint[0] = 500;
            } else if (id == R.id.chip_point_1000) {
                selectedPoint[0] = 1000;
            }

            if (txtMessage != null) {
                txtMessage.setText("기부 단체와 포인트를 선택해 주세요");
            }
        };

        if (chip100 != null) chip100.setOnClickListener(pointClickListener);
        if (chip300 != null) chip300.setOnClickListener(pointClickListener);
        if (chip500 != null) chip500.setOnClickListener(pointClickListener);
        if (chip1000 != null) chip1000.setOnClickListener(pointClickListener);

        // 기부 버튼
        if (btnDonateConfirm != null) {
            btnDonateConfirm.setOnClickListener(v -> {
                if (getContext() == null) return;

                if (selectedOrg[0] == null || selectedPoint[0] == 0) {
                    if (txtMessage != null) {
                        txtMessage.setText("기부 단체와 포인트를 모두 선택해 주세요");
                    }
                    Toast.makeText(getContext(),
                            "단체와 포인트를 선택해 주세요", Toast.LENGTH_SHORT).show();
                    return;
                }

                String msg = selectedOrg[0] + "에 " + selectedPoint[0] + "P 를 기부했어요 💜";
                Toast.makeText(getContext(), msg, Toast.LENGTH_LONG).show();
                dialog.dismiss();
            });
        }

        dialog.show();
    }

    private void resetOrgCardBackground(LinearLayout card1,
                                        LinearLayout card2,
                                        LinearLayout card3) {
        if (card1 != null) card1.setBackgroundResource(R.drawable.bg_card_solid_lavender);
        if (card2 != null) card2.setBackgroundResource(R.drawable.bg_card_solid_lavender);
        if (card3 != null) card3.setBackgroundResource(R.drawable.bg_card_solid_lavender);
    }

    private void resetPointChips(TextView... chips) {
        if (getContext() == null) return;
        int defaultTextColor =
                ContextCompat.getColor(requireContext(), R.color.poseme_purple_dark);

        for (TextView chip : chips) {
            if (chip == null) continue;
            chip.setBackgroundResource(R.drawable.bg_chip_solid_lavender);
            chip.setTextColor(defaultTextColor);
        }
    }
}
