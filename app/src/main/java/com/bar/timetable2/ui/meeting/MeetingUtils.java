package com.bar.timetable2.ui.meeting;

import com.bar.timetable2.data.model.ClassSlot;
import com.bar.timetable2.ui.timetable.view.TimetableView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MeetingUtils {

    // 하루 범위 (필요하면 조정 가능)
    private static final int DAY_START_MIN = 9 * 60;   // 09:00
    private static final int DAY_END_MIN   = 22 * 60;  // 22:00

    /**
     * 나 + 친구들의 슬롯을 받아서
     * 각 참가자별 공강시간을 FreeTimeBlock으로 모두 만든다.
     * (겹친 부분은 자연스럽게 여러 번 칠해져서 더 진하게 보임)
     */
    public static List<TimetableView.FreeTimeBlock> buildFreeBlocksForAllParticipants(
            List<ClassSlot> mySlots,
            Map<String, List<ClassSlot>> friendsSlotsByUser
    ) {
        List<TimetableView.FreeTimeBlock> result = new ArrayList<>();

        int participantCount = 0;
        if (mySlots != null && !mySlots.isEmpty()) participantCount++;
        if (friendsSlotsByUser != null) {
            for (List<ClassSlot> slots : friendsSlotsByUser.values()) {
                if (slots != null && !slots.isEmpty()) {
                    participantCount++;
                }
            }
        }
        if (participantCount == 0) {
            return result;
        }

        // 인원 수에 따라 1인당 알파를 조금 낮춤
        // (겹치면 자연스럽게 더 진해짐)
        float singleAlpha = 0.35f / participantCount;  // 적당히 튜닝 가능
        if (singleAlpha < 0.08f) singleAlpha = 0.08f;  // 너무 옅어지지 않게 하한 설정

        // 나
        appendFreeBlocksForOneParticipant(result, mySlots, singleAlpha);

        // 친구들
        if (friendsSlotsByUser != null) {
            for (List<ClassSlot> slots : friendsSlotsByUser.values()) {
                appendFreeBlocksForOneParticipant(result, slots, singleAlpha);
            }
        }

        return result;
    }

    // 한 명에 대한 공강 블록 생성 후 result에 추가
    private static void appendFreeBlocksForOneParticipant(
            List<TimetableView.FreeTimeBlock> out,
            List<ClassSlot> slots,
            float alpha
    ) {
        if (slots == null || slots.isEmpty()) return;

        Map<Integer, List<int[]>> busyByDay = groupSlotsByDay(slots);

        for (int day = 1; day <= 7; day++) {
            List<int[]> busy = busyByDay.getOrDefault(day, new ArrayList<>());
            busy = mergeIntervals(busy);
            List<int[]> free = invertBusyToFree(busy, DAY_START_MIN, DAY_END_MIN);

            for (int[] iv : free) {
                int s = iv[0];
                int e = iv[1];
                if (e - s < 15) continue; // 15분 미만은 무시

                TimetableView.FreeTimeBlock fb = new TimetableView.FreeTimeBlock();
                fb.dayOfWeek = day;
                fb.startMin = s;
                fb.endMin = e;
                fb.alpha = alpha;
                out.add(fb);
            }
        }
    }

    // ===== 공통 헬퍼 함수들 =====

    // ClassSlot 리스트 -> 요일별 busy interval [start,end]
    private static Map<Integer, List<int[]>> groupSlotsByDay(List<ClassSlot> slots) {
        Map<Integer, List<int[]>> map = new HashMap<>();
        if (slots == null) return map;

        for (ClassSlot slot : slots) {
            if (slot == null) continue;
            int d = slot.getDayOfWeek();
            int s = slot.getStartMin();
            int e = slot.getEndMin();
            if (e <= s) continue;

            List<int[]> list = map.get(d);
            if (list == null) {
                list = new ArrayList<>();
                map.put(d, list);
            }
            list.add(new int[]{s, e});
        }
        return map;
    }

    // 겹치는 interval들 머지
    private static List<int[]> mergeIntervals(List<int[]> intervals) {
        List<int[]> res = new ArrayList<>();
        if (intervals == null || intervals.isEmpty()) return res;

        intervals.sort((a, b) -> Integer.compare(a[0], b[0]));
        int[] cur = intervals.get(0).clone();

        for (int i = 1; i < intervals.size(); i++) {
            int[] next = intervals.get(i);
            if (next[0] <= cur[1]) {
                cur[1] = Math.max(cur[1], next[1]);
            } else {
                res.add(cur);
                cur = next.clone();
            }
        }
        res.add(cur);
        return res;
    }

    // busy -> [dayStart, dayEnd] 안에서 free interval 리스트로 변환
    private static List<int[]> invertBusyToFree(List<int[]> busy,
                                                int dayStart,
                                                int dayEnd) {
        List<int[]> free = new ArrayList<>();
        if (dayEnd <= dayStart) return free;

        if (busy == null || busy.isEmpty()) {
            free.add(new int[]{dayStart, dayEnd});
            return free;
        }

        int cur = dayStart;
        for (int[] b : busy) {
            int bs = Math.max(b[0], dayStart);
            int be = Math.min(b[1], dayEnd);
            if (be <= bs) continue;

            if (bs > cur) {
                free.add(new int[]{cur, bs});
            }
            cur = Math.max(cur, be);
        }
        if (cur < dayEnd) {
            free.add(new int[]{cur, dayEnd});
        }

        return free;
    }
}
