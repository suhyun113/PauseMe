package com.cookandroid.pauseme.ui;

import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.cookandroid.pauseme.R;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class MyPageFragment extends Fragment {

    private TextView txtProfileName;
    private TextView txtProfileSubtitle;
    private TextView txtPointValue;
    private ImageButton btnEditNickname;
    private LinearLayout layoutDonateHistoryContainer;
    private ProgressBar progressMonthCheckin;
    private TextView txtMonthCheckinCount;
    private TextView txtNoHistory;

    // Firebase 경로 상수
    private static final String DB_ROOT_USER_POINTS = "user_points";
    private static final String DB_ROOT_DONATION_HISTORY = "donation_history";
    private static final String DB_ROOT_NICKNAME = "nickname";
    private static final String DB_ROOT_MOODS = "moods";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_mypage, container, false);

        // ---- 뷰 찾기 ----
        txtProfileName = root.findViewById(R.id.txt_profile_name);
        txtProfileSubtitle = root.findViewById(R.id.txt_profile_subtitle);
        txtPointValue = root.findViewById(R.id.txt_point_value);
        btnEditNickname = root.findViewById(R.id.btn_edit_nickname);
        layoutDonateHistoryContainer = root.findViewById(R.id.layout_donate_history_container);
        txtMonthCheckinCount = root.findViewById(R.id.txt_month_checkin_count);
        progressMonthCheckin = root.findViewById(R.id.progress_month_checkin);
        txtNoHistory = root.findViewById(R.id.txt_no_history);

        Button btnOpenDonate = root.findViewById(R.id.btn_open_donate);

        // 닉네임 수정 버튼 리스너
        if (btnEditNickname != null) {
            btnEditNickname.setOnClickListener(v -> showNicknameEditDialog());
        }

        // 기부하기 버튼 리스너
        if (btnOpenDonate != null) {
            btnOpenDonate.setOnClickListener(v -> {
                try {
                    String pointText = txtPointValue.getText().toString().replace("P", "").replace(",", "").trim();
                    int currentPoint = Integer.parseInt(pointText);
                    showDonateBottomSheet(currentPoint);
                } catch (NumberFormatException e) {
                    Toast.makeText(getContext(), "포인트 로드 중 오류 발생", Toast.LENGTH_SHORT).show();
                }
            });
        }

        loadAllStats();
        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadAllStats(); // 화면 재진입 시 데이터 갱신
    }

    private void loadAllStats() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            txtProfileName.setText("로그인 필요");
            txtProfileSubtitle.setText("로그인하여 활동을 시작하세요.");
            txtPointValue.setText("0P");
            return;
        }

        String uid = user.getUid();
        loadNickname(uid, user.getEmail());
        loadPoints(uid);
        loadDonationHistory(uid);
        loadMonthlyCheckinCount(uid);
        // TODO: txtImpactPointValue, txtImpactPeopleValue 등 나머지 통계 로드 함수도 구현 필요
    }

    /**
     * 닉네임 로딩 (없으면 이메일 사용)
     */
    private void loadNickname(String uid, String email) {
        DatabaseReference nicknameRef = FirebaseDatabase.getInstance()
                .getReference("users").child(uid).child(DB_ROOT_NICKNAME);

        txtProfileSubtitle.setText(email); // 이메일을 부제로 표시

        nicknameRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String nickname = snapshot.getValue(String.class);
                if (nickname != null && !nickname.isEmpty()) {
                    txtProfileName.setText(nickname + "님");
                } else {
                    String defaultName = email != null && email.contains("@") ? email.substring(0, email.indexOf("@")) : "사용자";
                    txtProfileName.setText(defaultName + "님");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                txtProfileName.setText("사용자님");
            }
        });
    }

    /**
     * 닉네임 수정 다이얼로그
     */
    private void showNicknameEditDialog() {
        if (getContext() == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        LayoutInflater inflater = requireActivity().getLayoutInflater();

        View dialogView = inflater.inflate(R.layout.dialog_edit_nickname, null);
        builder.setView(dialogView);

        final AlertDialog dialog = builder.create();

        final EditText editNewNickname = dialogView.findViewById(R.id.edit_new_nickname);
        Button btnCancel = dialogView.findViewById(R.id.btn_cancel_nickname_edit);
        Button btnSave = dialogView.findViewById(R.id.btn_save_nickname_edit);

        String currentName = txtProfileName.getText().toString().replace("님", "").trim();
        editNewNickname.setText(currentName);
        editNewNickname.setSelection(editNewNickname.getText().length());

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String newNickname = editNewNickname.getText().toString().trim();
            if (!newNickname.isEmpty()) {
                saveNickname(newNickname);
                dialog.dismiss();
            } else {
                Toast.makeText(getContext(), "닉네임을 입력해 주세요.", Toast.LENGTH_SHORT).show();
            }
        });

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        dialog.show();
    }

    /**
     * Firebase에 닉네임 저장
     */
    private void saveNickname(String nickname) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || getContext() == null) return;

        DatabaseReference nicknameRef = FirebaseDatabase.getInstance()
                .getReference("users").child(user.getUid()).child(DB_ROOT_NICKNAME);

        nicknameRef.setValue(nickname).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(getContext(), "닉네임이 성공적으로 변경되었습니다.", Toast.LENGTH_SHORT).show();
                loadNickname(user.getUid(), user.getEmail()); // UI 갱신
            } else {
                Toast.makeText(getContext(), "닉네임 변경에 실패했습니다.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * 사용자 포인트 로딩
     */
    private void loadPoints(String uid) {
        DatabaseReference pointRef = FirebaseDatabase.getInstance()
                .getReference("users").child(uid).child(DB_ROOT_USER_POINTS);

        pointRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Long points = snapshot.getValue(Long.class);
                long currentPoint = points != null ? points : 0;

                txtPointValue.setText(String.format(Locale.getDefault(), "%,d P", currentPoint));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                txtPointValue.setText("Error");
            }
        });
    }

    /**
     * 기부 내역 로딩 및 UI 생성
     */
    private void loadDonationHistory(String uid) {
        if (layoutDonateHistoryContainer == null) return;

        DatabaseReference historyRef = FirebaseDatabase.getInstance()
                .getReference("users").child(uid).child(DB_ROOT_DONATION_HISTORY);

        historyRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // UI 갱신 준비
                layoutDonateHistoryContainer.removeAllViews();

                List<DonationRecord> historyList = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    Map<String, Object> map = (Map<String, Object>) child.getValue();
                    if (map != null) {
                        historyList.add(new DonationRecord(map));
                    }
                }

                // 기부 내역 없음 메시지 처리
                if (txtNoHistory != null) {
                    txtNoHistory.setVisibility(historyList.isEmpty() ? View.VISIBLE : View.GONE);
                }

                // 최신순 정렬 (timestamp 기준)
                Collections.sort(historyList, (r1, r2) -> Long.compare(r2.timestamp, r1.timestamp));

                // UI 동적 추가
                for (DonationRecord record : historyList) {
                    addHistoryItemToUI(record);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "기부 내역 로드 실패", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * 기부 내역 UI 아이템 동적 추가
     */
    private void addHistoryItemToUI(DonationRecord record) {
        if (getContext() == null || layoutDonateHistoryContainer == null) return;

        // 동적 레이아웃 생성
        LinearLayout itemLayout = new LinearLayout(getContext());
        itemLayout.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        itemLayout.setOrientation(LinearLayout.HORIZONTAL);
        itemLayout.setGravity(android.view.Gravity.CENTER_VERTICAL);
        itemLayout.setBackgroundResource(R.drawable.bg_card_solid_lavender);
        itemLayout.setPadding(14, 14, 14, 14);

        if (layoutDonateHistoryContainer.getChildCount() > 0 && layoutDonateHistoryContainer.getChildAt(0) != txtNoHistory) {
            LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) itemLayout.getLayoutParams();
            lp.topMargin = 10;
        }

        TextView emoji = new TextView(getContext());
        emoji.setText("💜");
        emoji.setTextSize(22);
        itemLayout.addView(emoji);

        LinearLayout textLayout = new LinearLayout(getContext());
        LinearLayout.LayoutParams textLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        textLp.setMarginStart(10);
        textLayout.setLayoutParams(textLp);
        textLayout.setOrientation(LinearLayout.VERTICAL);

        TextView orgName = new TextView(getContext());
        orgName.setText(record.organization);
        orgName.setTextColor(ContextCompat.getColor(getContext(), R.color.poseme_text_dark));
        orgName.setTextSize(14);
        orgName.setTypeface(null, android.graphics.Typeface.BOLD);
        textLayout.addView(orgName);

        TextView date = new TextView(getContext());
        date.setText(record.date);
        date.setTextColor(ContextCompat.getColor(getContext(), R.color.poseme_text_sub));
        date.setTextSize(12);
        textLayout.addView(date);

        itemLayout.addView(textLayout);

        TextView points = new TextView(getContext());
        points.setText(String.format(Locale.getDefault(), "%d P", record.points));
        points.setTextColor(ContextCompat.getColor(getContext(), R.color.poseme_purple_dark));
        points.setTextSize(14);
        points.setTypeface(null, android.graphics.Typeface.BOLD);
        itemLayout.addView(points);

        layoutDonateHistoryContainer.addView(itemLayout);
    }

    // 기부 기록 데이터 모델 (내부 클래스)
    private static class DonationRecord {
        public final String organization;
        public final int points;
        public final String date;
        public final long timestamp;

        public DonationRecord(Map<String, Object> map) {
            this.organization = (String) map.get("organization");
            this.points = ((Long) map.get("points")).intValue();
            this.timestamp = (Long) map.get("timestamp");

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd", Locale.getDefault());
            this.date = sdf.format(new java.util.Date(this.timestamp));
        }
    }

    /**
     * 월별 체크인 횟수 로딩
     */
    private void loadMonthlyCheckinCount(String uid) {
        SimpleDateFormat ymFormat = new SimpleDateFormat("yyyy-MM", Locale.getDefault());
        String currentMonthKey = ymFormat.format(Calendar.getInstance().getTime());

        DatabaseReference checkinRef = FirebaseDatabase.getInstance()
                .getReference(DB_ROOT_MOODS)
                .child(uid)
                .child(currentMonthKey);

        checkinRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                long checkinDays = snapshot.getChildrenCount();

                int totalDaysInMonth = Calendar.getInstance().getActualMaximum(Calendar.DAY_OF_MONTH);

                txtMonthCheckinCount.setText(String.format(Locale.getDefault(), "%d / %d", checkinDays, totalDaysInMonth));

                if (progressMonthCheckin != null) {
                    progressMonthCheckin.setMax(totalDaysInMonth);
                    progressMonthCheckin.setProgress((int) checkinDays);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                txtMonthCheckinCount.setText("로드 오류");
            }
        });
    }

    /**
     * 기부 바텀시트 로직
     */
    private void showDonateBottomSheet(int currentPoint) {
        if (getContext() == null) return;
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        View sheet = LayoutInflater.from(getContext()).inflate(R.layout.bottomsheet_donate, null);
        dialog.setContentView(sheet);

        TextView txtPointInfo = sheet.findViewById(R.id.txt_donate_point_info);
        TextView txtMessage = sheet.findViewById(R.id.txt_donate_message);
        Button btnDonateConfirm = sheet.findViewById(R.id.btn_donate_confirm);

        // 포인트 칩
        TextView chip100 = sheet.findViewById(R.id.chip_point_100);
        TextView chip300 = sheet.findViewById(R.id.chip_point_300);
        TextView chip500 = sheet.findViewById(R.id.chip_point_500);
        TextView chip1000 = sheet.findViewById(R.id.chip_point_1000);
        TextView[] chips = {chip100, chip300, chip500, chip1000};

        // 단체 카드
        LinearLayout cardOrg1 = sheet.findViewById(R.id.card_org_1);
        LinearLayout cardOrg2 = sheet.findViewById(R.id.card_org_2);
        LinearLayout cardOrg3 = sheet.findViewById(R.id.card_org_3);
        LinearLayout[] orgCards = {cardOrg1, cardOrg2, cardOrg3};

        if (txtPointInfo != null)
            txtPointInfo.setText(String.format(Locale.getDefault(), "보유 포인트: %,d P", currentPoint));

        final String[] selectedOrg = {null};
        final int[] selectedPoint = {0};

        // 1. 단체 선택 리스너
        View.OnClickListener orgClickListener = v -> {
            if (getContext() == null) return;
            resetOrgCardBackground(orgCards);
            v.setBackgroundResource(R.drawable.bg_button_primary_dark);

            String orgName = "";
            if (v.getId() == R.id.card_org_1) orgName = "청소년 마음건강센터";
            else if (v.getId() == R.id.card_org_2) orgName = "청년 멘탈케어";
            else if (v.getId() == R.id.card_org_3) orgName = "희망의 심리학";

            selectedOrg[0] = orgName;

            if (txtMessage != null) txtMessage.setText("기부 단체와 포인트를 선택해 주세요");
        };

        for (LinearLayout card : orgCards) {
            if (card != null) card.setOnClickListener(orgClickListener);
        }

        // 2. 포인트 선택 리스너
        View.OnClickListener pointClickListener = v -> {
            if (getContext() == null) return;

            resetPointChips(chips);
            TextView tv = (TextView) v;
            tv.setBackgroundResource(R.drawable.bg_chip_solid_purple);
            tv.setTextColor(ContextCompat.getColor(requireContext(), R.color.white));

            try {
                selectedPoint[0] = Integer.parseInt(tv.getText().toString().replace("P", "").trim());
            } catch (NumberFormatException e) {
                selectedPoint[0] = 0;
            }

            if (txtMessage != null) txtMessage.setText("기부 단체와 포인트를 선택해 주세요");
        };

        for (TextView chip : chips) {
            if (chip != null) chip.setOnClickListener(pointClickListener);
        }

        // 3. 기부 버튼 (Firebase 연동)
        if (btnDonateConfirm != null) {
            btnDonateConfirm.setOnClickListener(v -> {
                if (selectedOrg[0] == null) {
                    if (txtMessage != null) txtMessage.setText("기부 단체를 선택해 주세요.");
                    return;
                }
                if (selectedPoint[0] == 0) {
                    if (txtMessage != null) txtMessage.setText("기부 포인트를 선택해 주세요.");
                    return;
                }
                if (selectedPoint[0] > currentPoint) {
                    Toast.makeText(getContext(), "포인트가 부족합니다.", Toast.LENGTH_SHORT).show();
                    return;
                }

                processDonation(selectedOrg[0], selectedPoint[0], dialog);
            });
        }

        dialog.show();
    }

    // 기부 처리: 포인트 차감 및 기록 저장
    private void processDonation(String organization, int points, BottomSheetDialog dialog) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || getContext() == null) return;

        DatabaseReference pointRef = FirebaseDatabase.getInstance()
                .getReference("users").child(user.getUid()).child(DB_ROOT_USER_POINTS);

        // 1. 포인트 차감 트랜잭션
        pointRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Long currentPoints = snapshot.getValue(Long.class);
                long newPoints = (currentPoints != null ? currentPoints : 0) - points;

                if (newPoints < 0) {
                    Toast.makeText(getContext(), "포인트가 부족하여 기부에 실패했습니다.", Toast.LENGTH_SHORT).show();
                    return;
                }

                // 2. 포인트 업데이트
                pointRef.setValue(newPoints).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // 3. 기부 기록 저장
                        saveDonationRecord(user.getUid(), organization, points);
                        Toast.makeText(getContext(), organization + "에 " + points + "P 를 기부했어요 💜", Toast.LENGTH_LONG).show();

                        // 기부 성공 후 내역을 다시 로드하여 UI 갱신 (마이페이지 하단 내역 갱신)
                        loadDonationHistory(user.getUid());

                        dialog.dismiss();
                    } else {
                        Toast.makeText(getContext(), "기부 처리 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "잔액 확인 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // 기부 기록 저장
    private void saveDonationRecord(String uid, String organization, int points) {
        DatabaseReference historyRef = FirebaseDatabase.getInstance()
                .getReference("users").child(uid).child(DB_ROOT_DONATION_HISTORY)
                .child(String.valueOf(System.currentTimeMillis()));

        historyRef.child("organization").setValue(organization);
        historyRef.child("points").setValue(points);
        historyRef.child("timestamp").setValue(System.currentTimeMillis());
    }

    // 기부 보조 메서드
    private void resetOrgCardBackground(LinearLayout[] cards) {
        for (LinearLayout card : cards) {
            if (card != null) card.setBackgroundResource(R.drawable.bg_card_solid_lavender);
        }
    }

    private void resetPointChips(TextView[] chips) {
        if (getContext() == null) return;
        int defaultTextColor = ContextCompat.getColor(requireContext(), R.color.poseme_purple_dark);

        for (TextView chip : chips) {
            if (chip == null) continue;
            chip.setBackgroundResource(R.drawable.bg_chip_solid_lavender);
            chip.setTextColor(defaultTextColor);
        }
    }
}