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
    private float timeTextSize;
    private float dayTextSize;
    private float blockTextSize;

    // 페인트
    private Paint gridLinePaint;
    private Paint borderPaint;
    private Paint timeTextPaint;
    private Paint dayHeaderTextPaint;
    private Paint backgroundPaint;
    private Paint blockPaint;
    private Paint blockTextPaint;
    private Paint freeTimePaint;

    // 뷰 크기
    private int viewWidth;
    private int viewHeight;

    // 시간표 상태
    private TimetableState timetableState;

    // 동적으로 계산된 그리기 기준
    private int visibleStartHour = DEFAULT_START_HOUR;
    private int visibleEndHour = DEFAULT_END_HOUR;

    private List<Integer> activeDays = new ArrayList<>();

    // ===== 공강 오버레이 블록 =====
    public static class FreeTimeBlock {
        public int dayOfWeek;
        public int startMin;
        public int endMin;
        public float alpha;
    }

    private List<FreeTimeBlock> freeTimeBlocks = new ArrayList<>();

    public void setFreeTimeBlocks(@Nullable List<FreeTimeBlock> blocks) {
        freeTimeBlocks.clear();
        if (blocks != null) {
            freeTimeBlocks.addAll(blocks);
        }
        invalidate();
    }

    // 블록 클릭 콜백 인터페이스
    public interface OnSlotClickListener {
        void onSlotClick(ClassSlot slot);
    }

    private OnSlotClickListener onSlotClickListener;

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

        // 레이아웃 치수
        topHeaderHeight = 36 * density;
        leftTimeWidth = 36 * density;

        // 텍스트 크기
        timeTextSize = 13 * density;
        dayTextSize = 14 * density;
        blockTextSize = 13 * density;

        // 배경
        backgroundPaint = new Paint();
        backgroundPaint.setColor(0xFFFFFFFF);

        // 그리드 라인 (매우 얇고 연한 회색)
        gridLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        gridLinePaint.setColor(0xFF9E9E9E);
        gridLinePaint.setStrokeWidth(0.5f * density);

        // 시간표 테두리용 페인트 추가
        borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        borderPaint.setColor(0xFF9E9E9E);
        borderPaint.setStrokeWidth(1.5f * density);
        borderPaint.setStyle(Paint.Style.STROKE);

        // 시간 텍스트
        timeTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        timeTextPaint.setColor(0xFF424242);
        timeTextPaint.setTextSize(timeTextSize);

        // 요일 헤더 텍스트
        dayHeaderTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        dayHeaderTextPaint.setColor(0xFF424242);
        dayHeaderTextPaint.setTextSize(dayTextSize);
        dayHeaderTextPaint.setFakeBoldText(true);

        // 과목 블록
        blockPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        blockPaint.setStyle(Paint.Style.FILL);

        // 블록 내 텍스트
        blockTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        blockTextPaint.setColor(Color.WHITE);
        blockTextPaint.setTextSize(blockTextSize);
        blockTextPaint.setFakeBoldText(true);

        // 공강 오버레이
        freeTimePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        freeTimePaint.setStyle(Paint.Style.FILL);
        freeTimePaint.setColor(0xFF4CAF50);

        // 기본 활성 요일: 월~금
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

        // 시작 시간: 기본값과 실제 슬롯 중 더 이른 시간
        int startHour = Math.min(DEFAULT_START_HOUR, minStartMin / MINUTES_PER_HOUR);

        // 끝 시간: 기본값과 실제 슬롯 중 더 늦은 시간
        int endHour = (int) Math.ceil(maxEndMin / (float) MINUTES_PER_HOUR);
        endHour = Math.max(DEFAULT_END_HOUR, endHour);

        visibleStartHour = startHour;
        visibleEndHour = endHour;
        if (visibleStartHour < 0) visibleStartHour = 0;
        if (visibleEndHour <= visibleStartHour) {
            visibleEndHour = visibleStartHour + 1;
        }
    }

    // 슬롯들을 보고 표시할 요일 목록 결정
    private void recomputeActiveDays() {
        activeDays.clear();

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
        if (hasSun) {
            activeDays.add(6);
            activeDays.add(7);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);

        // 시간표 전체 높이 계산
        float density = getResources().getDisplayMetrics().density;
        float rowHeight = 60 * density;  // 한 시간당 높이

        int hourCount = visibleEndHour - visibleStartHour;
        if (hourCount <= 0) hourCount = 7; // 기본값

        int totalHeight = (int) (topHeaderHeight + rowHeight * hourCount);

        setMeasuredDimension(width, totalHeight);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        slotRects.clear();
        slotRectsSlots.clear();

        // 배경
        canvas.drawRect(0, 0, viewWidth, viewHeight, backgroundPaint);

        float contentLeft = leftTimeWidth;
        float contentTop = topHeaderHeight;
        float contentRight = viewWidth;

        int dayCount = activeDays.isEmpty() ? 5 : activeDays.size();
        int hourCount = visibleEndHour - visibleStartHour;

        if (dayCount <= 0 || hourCount <= 0) return;

        float density = getResources().getDisplayMetrics().density;
        float columnWidth = (contentRight - contentLeft) / dayCount;
        float rowHeight = 60 * density;

        float contentBottom = contentTop + rowHeight * hourCount;

        // ===== 1) 요일 헤더 영역 그리드 =====
        // 요일 헤더 아래쪽 가로선
        canvas.drawLine(0, contentTop, contentRight, contentTop, gridLinePaint);

        // 요일 헤더 영역의 세로선들 (왼쪽 시간 영역 포함)
        canvas.drawLine(contentLeft, 0, contentLeft, contentTop, gridLinePaint);
        for (int i = 0; i <= dayCount; i++) {
            float x = contentLeft + columnWidth * i;
            canvas.drawLine(x, 0, x, contentTop, gridLinePaint);
        }

        // 요일 텍스트
        for (int i = 0; i < dayCount; i++) {
            int dayOfWeek = activeDays.get(i);
            String dayLabel = (dayOfWeek >= 1 && dayOfWeek <= 7) ? DAY_LABELS[dayOfWeek] : "?";

            float colCenterX = contentLeft + columnWidth * i + columnWidth / 2f;
            float textWidth = dayHeaderTextPaint.measureText(dayLabel);
            float x = colCenterX - textWidth / 2f;
            float y = topHeaderHeight / 2f + (dayTextSize / 3f);
            canvas.drawText(dayLabel, x, y, dayHeaderTextPaint);
        }

        // ===== 2) 시간표 영역 가로줄 =====
        for (int i = 0; i <= hourCount; i++) {
            float y = contentTop + rowHeight * i;
            canvas.drawLine(0, y, contentRight, y, gridLinePaint);
        }

        // ===== 3) 시간 라벨 (그리드 셀 상단에 배치) =====
        for (int i = 0; i < hourCount; i++) {
            int hour = visibleStartHour + i;
            float y = contentTop + rowHeight * i;

            // 시간 라벨을 셀의 상단에 배치
            String timeLabel = hour + "";
            float textWidth = timeTextPaint.measureText(timeLabel);

            // 셀 상단에서 약간 아래로 (padding 추가)
            float textX = (leftTimeWidth - textWidth) / 2f;
            float textY = y + timeTextSize + 8;  // 상단에서 8dp 여백

            canvas.drawText(timeLabel, textX, textY, timeTextPaint);
        }

        // ===== 4) 시간표 영역 세로줄 =====
        // 왼쪽 시간 영역 오른쪽 경계선
        canvas.drawLine(contentLeft, 0, contentLeft, contentBottom, gridLinePaint);

        // 요일 구분 세로선들
        for (int i = 0; i <= dayCount; i++) {
            float x = contentLeft + columnWidth * i;
            canvas.drawLine(x, contentTop, x, contentBottom, gridLinePaint);
        }

        // ===== 5) 공강 오버레이 =====
        if (freeTimeBlocks != null && !freeTimeBlocks.isEmpty()) {
            drawFreeTimeBlocks(canvas, contentLeft, contentTop, columnWidth, rowHeight);
        }

        // ===== 6) 과목 블록 =====
        if (timetableState != null && timetableState.getSlots() != null) {
            drawCourseBlocks(canvas, contentLeft, contentTop, contentRight, contentBottom,
                    columnWidth, rowHeight);
        }

        // ===== 7) 시간표 전체 테두리 =====
        RectF outerBorderRect = new RectF(0, 0, contentRight, contentBottom);
        canvas.drawRect(outerBorderRect, borderPaint);
    }

    private void drawFreeTimeBlocks(Canvas canvas,
                                    float contentLeft,
                                    float contentTop,
                                    float columnWidth,
                                    float rowHeight) {

        for (FreeTimeBlock block : freeTimeBlocks) {
            if (block == null) continue;
            if (block.alpha <= 0f) continue;

            int dayIndex = activeDays.indexOf(block.dayOfWeek);
            if (dayIndex < 0) continue;

            int startMin = block.startMin;
            int endMin = block.endMin;
            if (endMin <= startMin) continue;

            float minutesFromStart = (startMin - visibleStartHour * 60);
            float minutesToEnd = (endMin - visibleStartHour * 60);

            if (minutesToEnd <= 0 ||
                    minutesFromStart >= (visibleEndHour - visibleStartHour) * 60) {
                continue;
            }

            if (minutesFromStart < 0) minutesFromStart = 0;
            float maxMinutes = (visibleEndHour - visibleStartHour) * 60;
            if (minutesToEnd > maxMinutes) minutesToEnd = maxMinutes;

            float top = contentTop + (minutesFromStart / 60f) * rowHeight;
            float bottom = contentTop + (minutesToEnd / 60f) * rowHeight;

            float left = contentLeft + columnWidth * dayIndex + 2;
            float right = contentLeft + columnWidth * (dayIndex + 1) - 2;

            RectF r = new RectF(left, top, right, bottom);

            float a = block.alpha;
            if (a < 0f) a = 0f;
            if (a > 1f) a = 1f;
            int alphaInt = (int) (255 * a);

            freeTimePaint.setAlpha(alphaInt);
            canvas.drawRoundRect(r, 8f, 8f, freeTimePaint);
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
        Map<String, Course> courseMap = timetableState.getCourseMap();

        for (ClassSlot slot : slots) {
            if (slot == null) continue;

            int dayOfWeek = slot.getDayOfWeek();
            int startMin = slot.getStartMin();
            int endMin = slot.getEndMin();

            int dayIndex = activeDays.indexOf(dayOfWeek);
            if (dayIndex < 0) continue;

            float minutesFromStart = (startMin - visibleStartHour * 60);
            float minutesToEnd = (endMin - visibleStartHour * 60);

            if (minutesToEnd <= 0 ||
                    minutesFromStart >= (visibleEndHour - visibleStartHour) * 60) {
                continue;
            }

            float top = contentTop + (minutesFromStart / 60f) * rowHeight;
            float bottom = contentTop + (minutesToEnd / 60f) * rowHeight;

            // 여백을 최소화하여 블록이 셀을 거의 꽉 채우도록
            float left = contentLeft + columnWidth * dayIndex + 2;
            float right = contentLeft + columnWidth * (dayIndex + 1) - 2;

            RectF blockRectF = new RectF(left, top, right, bottom);

            Course course = null;
            if (courseMap != null && slot.getCourseId() != null) {
                course = courseMap.get(slot.getCourseId());
            }
            int color = parseCourseColor(course);
            blockPaint.setColor(color);

            // 둥근 모서리
            canvas.drawRoundRect(blockRectF, 8f, 8f, blockPaint);

            // 과목 이름 텍스트
            if (course != null && !TextUtils.isEmpty(course.getName())) {
                String title = course.getName();

                float padding = 10f;
                float textX = left + padding;
                float textY = top + blockTextSize + padding;

                float maxWidth = right - left - 2 * padding;
                String drawText = title;
                float width = blockTextPaint.measureText(title);
                if (width > maxWidth) {
                    int len = (int) (title.length() * (maxWidth / width));
                    if (len > 0 && len < title.length()) {
                        drawText = title.substring(0, len - 1) + "…";
                    }
                }

                canvas.drawText(drawText, textX, textY, blockTextPaint);
            }

            // 장소 텍스트

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

    private int parseCourseColor(@Nullable Course course) {
        if (course == null || TextUtils.isEmpty(course.getColorHex())) {
            return 0xFF4CAF50;
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
        return true;
    }
}