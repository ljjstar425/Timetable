package com.bar.timetable2.ui.timetable.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

import com.bar.timetable2.data.model.ClassSlot;
import com.bar.timetable2.data.model.Course;
import com.bar.timetable2.data.model.TimetableState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class TimetableView extends View {

    // ====== 기본 설정 ======
    private static final int DEFAULT_START_HOUR = 9;
    private static final int DEFAULT_END_HOUR = 16;
    private static final int MINUTES_PER_HOUR = 60;

    // 요일 라벨 (1=월 ~ 7=일)
    private static final String[] DAY_LABELS = {
            "", "월", "화", "수", "목", "금", "토", "일"
    };
    private final List<Rect> slotRects = new ArrayList<>();
    private final List<ClassSlot> slotRectsSlots = new ArrayList<>();

    // 화면 여백
    private float topHeaderHeight;
    private float leftTimeWidth;
    private float textSize;

    // 페인트
    private Paint linePaint;
    private Paint textPaint;
    private Paint backgroundPaint;
    private Paint blockPaint;
    private Paint blockTextPaint;

    // 뷰 크기
    private int viewWidth;
    private int viewHeight;

    // 시간표 상태
    private TimetableState timetableState;

    // 동적으로 계산된 그리기 기준
    private int visibleStartHour = DEFAULT_START_HOUR;
    private int visibleEndHour = DEFAULT_END_HOUR;

    private List<Integer> activeDays = new ArrayList<>();  // 실제 표시할 요일들 (1~7)

    // 블록 클릭 콜백 인터페이스
    public interface OnSlotClickListener {
        void onSlotClick(ClassSlot slot);
    }

    private OnSlotClickListener onSlotClickListener;

    // 외부에서 리스너 설정
    public void setOnSlotClickListener(OnSlotClickListener listener) {
        this.onSlotClickListener = listener;
    }

    public TimetableView(Context context) {
        super(context);
        init();
    }

    public TimetableView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public TimetableView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    // ====== 초기 설정 ======
    private void init() {
        float density = getResources().getDisplayMetrics().density;
        topHeaderHeight = 32 * density;
        leftTimeWidth = 44 * density;
        textSize = 12 * density;

        linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        linePaint.setColor(0xFFCCCCCC);
        linePaint.setStrokeWidth(1 * density);

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(0xFF333333);
        textPaint.setTextSize(textSize);

        backgroundPaint = new Paint();
        backgroundPaint.setColor(Color.WHITE);

        blockPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        blockPaint.setStyle(Paint.Style.FILL);

        blockTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        blockTextPaint.setColor(Color.WHITE);
        blockTextPaint.setTextSize(textSize);

        // 🔥 기본 활성 요일: 월~금 (데이터 없을 때도 비어있지 않게)
        recomputeActiveDays();
    }


    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        viewWidth = w;
        viewHeight = h;
    }

    // ====== 외부에서 상태 세팅 ======
    public void setTimetableState(@Nullable TimetableState state) {
        this.timetableState = state;
        recomputeVisibleRange();
        recomputeActiveDays();
        invalidate();
    }

    // 슬롯들을 보고 시간 범위 재계산
    private void recomputeVisibleRange() {
        if (timetableState == null || timetableState.getSlots() == null
                || timetableState.getSlots().isEmpty()) {
            visibleStartHour = DEFAULT_START_HOUR;
            visibleEndHour = DEFAULT_END_HOUR;
            return;
        }

        int minStartMin = Integer.MAX_VALUE;
        int maxEndMin = Integer.MIN_VALUE;

        for (ClassSlot slot : timetableState.getSlots()) {
            if (slot == null) continue;
            minStartMin = Math.min(minStartMin, slot.getStartMin());
            maxEndMin = Math.max(maxEndMin, slot.getEndMin());
        }

        if (minStartMin == Integer.MAX_VALUE || maxEndMin == Integer.MIN_VALUE) {
            visibleStartHour = DEFAULT_START_HOUR;
            visibleEndHour = DEFAULT_END_HOUR;
            return;
        }

        int defaultStartMin = DEFAULT_START_HOUR * MINUTES_PER_HOUR;
        int defaultEndMin = DEFAULT_END_HOUR * MINUTES_PER_HOUR;

        int startMin = Math.min(defaultStartMin, minStartMin);
        int endMin = Math.max(defaultEndMin, maxEndMin);

        int startHour = startMin / MINUTES_PER_HOUR;
        if (startMin % MINUTES_PER_HOUR != 0) {
            startHour -= 1;
        }

        int endHour = (int) Math.ceil(endMin / (float) MINUTES_PER_HOUR);

        visibleStartHour = startHour;
        visibleEndHour = endHour;
        if (visibleStartHour < 0) visibleStartHour = 0;
        if (visibleEndHour <= visibleStartHour) {
            visibleEndHour = visibleStartHour + 1;
        }
    }

    // 슬롯들을 보고 표시할 요일 목록 결정 (월~금 + 필요 시 토/일)
    private void recomputeActiveDays() {
        activeDays.clear();

        // 기본: 월~금
        for (int d = 1; d <= 5; d++) {
            activeDays.add(d);
        }

        if (timetableState == null || timetableState.getSlots() == null) {
            return;
        }

        boolean hasSat = false;
        boolean hasSun = false;

        for (ClassSlot slot : timetableState.getSlots()) {
            if (slot == null) continue;
            int day = slot.getDayOfWeek();
            if (day == 6) hasSat = true;
            if (day == 7) hasSun = true;
        }

        if (hasSat) activeDays.add(6);
        if (hasSun) activeDays.add(7);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        slotRects.clear();
        slotRectsSlots.clear();

        canvas.drawRect(0, 0, viewWidth, viewHeight, backgroundPaint);

        float contentLeft = leftTimeWidth;
        float contentTop = topHeaderHeight;
        float contentRight = viewWidth;
        float contentBottom = viewHeight;

        int dayCount = activeDays.isEmpty() ? 5 : activeDays.size();
        int hourCount = visibleEndHour - visibleStartHour;

        if (dayCount <= 0 || hourCount <= 0) return;

        float columnWidth = (contentRight - contentLeft) / dayCount;
        float rowHeight = (contentBottom - contentTop) / hourCount;

        // ===== 1) 요일 헤더 =====
        for (int i = 0; i < dayCount; i++) {
            int dayOfWeek = activeDays.get(i);
            String dayLabel = (dayOfWeek >= 1 && dayOfWeek <= 7) ? DAY_LABELS[dayOfWeek] : "?";

            float colCenterX = contentLeft + columnWidth * i + columnWidth / 2f;
            float textWidth = textPaint.measureText(dayLabel);
            float x = colCenterX - textWidth / 2f;
            float y = topHeaderHeight / 2f + (textSize / 2f);
            canvas.drawText(dayLabel, x, y, textPaint);
        }

        // ===== 2) 시간 라벨 + 가로줄 =====
        for (int i = 0; i <= hourCount; i++) {
            int hour = visibleStartHour + i;
            float y = contentTop + rowHeight * i;

            canvas.drawLine(contentLeft, y, contentRight, y, linePaint);

            if (i < hourCount) {
                String timeLabel = hour + ":00";
                float textWidth = textPaint.measureText(timeLabel);
                float textX = leftTimeWidth - textWidth - 4;
                float textY = y + rowHeight / 2f + (textSize / 2f) - 4;
                canvas.drawText(timeLabel, textX, textY, textPaint);
            }
        }

        // ===== 3) 세로줄 (요일 컬럼 경계) =====
        for (int i = 0; i <= dayCount; i++) {
            float x = contentLeft + columnWidth * i;
            canvas.drawLine(x, contentTop, x, contentBottom, linePaint);
        }

        // ===== 4) 과목 블록 그리기 =====
        if (timetableState != null && timetableState.getSlots() != null) {
            drawCourseBlocks(canvas, contentLeft, contentTop, contentRight, contentBottom,
                    columnWidth, rowHeight);
        }
    }

    private void drawCourseBlocks(Canvas canvas,
                                  float contentLeft,
                                  float contentTop,
                                  float contentRight,
                                  float contentBottom,
                                  float columnWidth,
                                  float rowHeight) {

        if (timetableState == null || timetableState.getSlots() == null) return;

        List<ClassSlot> slots = timetableState.getSlots();
        // 과목 정보 맵 (courseId -> Course)
        Map<String, Course> courseMap = timetableState.getCourseMap(); // 메서드 이름 다르면 수정

        for (ClassSlot slot : slots) {
            if (slot == null) continue;

            int dayOfWeek = slot.getDayOfWeek();   // 1~7 (월~일)
            int startMin = slot.getStartMin();     // 분 단위
            int endMin   = slot.getEndMin();

            // 1) 이 요일이 activeDays에 있는지 확인
            int dayIndex = activeDays.indexOf(dayOfWeek);
            if (dayIndex < 0) continue;

            // 2) 시간 범위 계산
            float minutesFromStart = (startMin - visibleStartHour * 60);
            float minutesToEnd     = (endMin - visibleStartHour * 60);

            // 화면 위/아래를 벗어나면 스킵
            if (minutesToEnd <= 0 ||
                    minutesFromStart >= (visibleEndHour - visibleStartHour) * 60) {
                continue;
            }

            float top = contentTop + (minutesFromStart / 60f) * rowHeight;
            float bottom = contentTop + (minutesToEnd / 60f) * rowHeight;

            float left = contentLeft + columnWidth * dayIndex + 4;
            float right = contentLeft + columnWidth * (dayIndex + 1) - 4;

            RectF blockRectF = new RectF(left, top, right, bottom);

            // 과목 찾기 & 색상 설정
            Course course = null;
            if (courseMap != null && slot.getCourseId() != null) {
                course = courseMap.get(slot.getCourseId());
            }
            int color = parseCourseColor(course);
            blockPaint.setColor(color);

            // 블록 그리기
            canvas.drawRoundRect(blockRectF, 16f, 16f, blockPaint);

            // 과목 이름 텍스트 그리기
            if (course != null && !TextUtils.isEmpty(course.getName())) {
                String title = course.getName();

                // 블록 안쪽 여백
                float padding = 8f;
                float textX = left + padding;
                float textY = top + textSize + padding;

                // 너무 길면 대충 한 줄만 잘라 쓰기 (심플 버전)
                float maxWidth = right - left - 2 * padding;
                String drawText = title;
                float width = blockTextPaint.measureText(title);
                if (width > maxWidth) {
                    // 아주 간단하게, 글자 수 기준으로 자르기
                    int len = (int) (title.length() * (maxWidth / width));
                    if (len > 0 && len < title.length()) {
                        drawText = title.substring(0, len - 1) + "…";
                    }
                }

                canvas.drawText(drawText, textX, textY, blockTextPaint);
            }

            // 터치 판정용 Rect 저장
            Rect clickRect = new Rect(
                    (int) left,
                    (int) top,
                    (int) right,
                    (int) bottom
            );

            slotRects.add(clickRect);
            slotRectsSlots.add(slot);
        }
    }



    // 과목 색상 파싱 (colorHex가 없으면 기본 색)
    private int parseCourseColor(@Nullable Course course) {
        if (course == null || TextUtils.isEmpty(course.getColorHex())) {
            return 0xFF4CAF50; // 기본 초록색
        }
        try {
            return Color.parseColor(course.getColorHex());
        } catch (IllegalArgumentException e) {
            return 0xFF4CAF50;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP) {
            float x = event.getX();
            float y = event.getY();

            // 마지막에 그린 게 위에 있으니까 역순으로 탐색해도 됨
            for (int i = slotRects.size() - 1; i >= 0; i--) {
                Rect r = slotRects.get(i);
                if (r.contains((int) x, (int) y)) {
                    if (onSlotClickListener != null) {
                        ClassSlot slot = slotRectsSlots.get(i);
                        onSlotClickListener.onSlotClick(slot);
                    }
                    return true;
                }
            }
        }
        return true; // 터치 이벤트는 소비함
    }

}
