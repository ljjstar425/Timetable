package com.bar.timetable2.ui.friend;

import android.app.Dialog;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.bar.timetable2.data.repository.FriendRepository;

public class AddFriendDialogFragment extends DialogFragment {

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {

        EditText et = new EditText(requireContext());
        et.setHint("친구 아이디 입력");
        et.setInputType(InputType.TYPE_CLASS_TEXT);

        int padding = (int) (16 * getResources().getDisplayMetrics().density);

        LinearLayout container = new LinearLayout(requireContext());
        container.setPadding(padding, padding, padding, padding);
        container.addView(et);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle("친구 추가")
                .setView(container)
                .setPositiveButton("요청 보내기", (d, which) -> {
                    String friendId = et.getText().toString().trim();
                    if (friendId.isEmpty()) {
                        Toast.makeText(getContext(),
                                "친구 아이디를 입력하세요.",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    FriendRepository.getInstance()
                            .sendFriendRequest(friendId, new FriendRepository.SimpleCallback() {
                                @Override
                                public void onSuccess() {
                                    if (getContext() != null) {
                                        Toast.makeText(getContext(),
                                                "친구 요청을 보냈습니다.",
                                                Toast.LENGTH_SHORT).show();
                                    }
                                }

                                @Override
                                public void onError(String message) {
                                    if (getContext() != null) {
                                        Toast.makeText(getContext(),
                                                "친구 요청 실패: " + message,
                                                Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });
                })
                .setNegativeButton("취소", (d, which) -> {
                    d.dismiss();
                })
                .create();

        // 다이얼로그가 표시된 후 버튼 색상 변경
        dialog.setOnShowListener(dialogInterface -> {
            Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            Button negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);

            if (positiveButton != null) {
                positiveButton.setTextColor(Color.parseColor("#3182F7"));
            }
            if (negativeButton != null) {
                negativeButton.setTextColor(Color.parseColor("#3182F7"));
            }
        });

        return dialog;
    }
}
