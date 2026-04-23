package com.assist.GroupAssignerAssist.service;

import com.assist.GroupAssignerAssist.dto.GroupingResult;
import com.assist.GroupAssignerAssist.model.Person;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class GroupAssignerAssistService {

    // 가중치 및 탐색 횟수 설정
    private static final int PENALTY_OVERLAP = -100; // 이전 조원과 겹칠 때의 강력한 페널티 (Hard Constraint)
    private static final int MAX_IMPROVEMENT_STEPS = 2000; // 자리 바꾸기(Swap) 시도 횟수

    public GroupingResult assignBestGrouping(
            List<Person> people, int numGroups, List<String> criteria,
            List<List<List<String>>> previousGroupings) {

        // 1. 이전 조 편성 데이터가 있다면 중복 카운트 맵 생성
        Map<String, Map<String, Integer>> overlapCountMap = new HashMap<>();
        if (criteria.contains("previous") && previousGroupings != null && !previousGroupings.isEmpty()) {
            overlapCountMap = buildOverlapMap(previousGroupings);
        }

        // 2. 초기 무작위 조 편성
        List<List<Person>> currentGroups = splitIntoGroup(people, numGroups);
        int currentScore = evaluateTotalScore(currentGroups, overlapCountMap, criteria);

        Random random = new Random();

        // 3. 점진적 개선 (Hill Climbing - Swap 알고리즘)
        for (int step = 0; step < MAX_IMPROVEMENT_STEPS; step++) {
            // 무작위로 두 개의 서로 다른 조 선택
            int groupIdx1 = random.nextInt(numGroups);
            int groupIdx2 = random.nextInt(numGroups);
            if (groupIdx1 == groupIdx2) continue;

            List<Person> group1 = currentGroups.get(groupIdx1);
            List<Person> group2 = currentGroups.get(groupIdx2);

            // 빈 조가 있으면 스킵
            if (group1.isEmpty() || group2.isEmpty()) continue;

            // 각 조에서 무작위 학생 한 명씩 선택
            int pIdx1 = random.nextInt(group1.size());
            int pIdx2 = random.nextInt(group2.size());

            Person p1 = group1.get(pIdx1);
            Person p2 = group2.get(pIdx2);

            // 일단 자리를 바꿔봄 (Swap)
            group1.set(pIdx1, p2);
            group2.set(pIdx2, p1);

            // 바꾼 후의 점수 계산
            int newScore = evaluateTotalScore(currentGroups, overlapCountMap, criteria);

            // 점수가 개선되었다면 (중복이 줄었거나 성비가 좋아졌다면) 확정, 아니면 원상복구
            if (newScore > currentScore) {
                currentScore = newScore;
            } else {
                group1.set(pIdx1, p1);
                group2.set(pIdx2, p2);
            }
        }

        // 4. 조별로 이름순 정렬하여 깔끔하게 정리
        for (List<Person> group : currentGroups) {
            group.sort(Comparator.comparing(Person::getName));
        }

        // 5. 최종적으로 어쩔 수 없이 겹친 쌍이 있는지 순수 카운트
        List<String> overlappingPairs = new ArrayList<>();
        if (criteria.contains("previous")) {
            overlappingPairs = findOverlappingPairs(currentGroups, overlapCountMap);
        }

        // 6. 결과 DTO 반환
        return new GroupingResult(currentGroups, overlappingPairs.size(), overlappingPairs);
    }

    // =================================================================================
    // ⬇️ 아래부터는 점수 계산 및 유틸리티 내부 메서드들입니다.
    // =================================================================================

    // 초기 인원을 단순히 그룹 수에 맞게 나누는 메서드
    private List<List<Person>> splitIntoGroup(List<Person> people, int numGroups) {
        List<Person> shuffled = new ArrayList<>(people);
        Collections.shuffle(shuffled);
        List<List<Person>> groups = new ArrayList<>();
        
        for (int i = 0; i < numGroups; i++) {
            groups.add(new ArrayList<>());
        }
        
        for (int i = 0; i < shuffled.size(); i++) {
            groups.get(i % numGroups).add(shuffled.get(i));
        }
        return groups;
    }

    // 선택된 조건(criteria)에 따라 전체 점수를 합산
    private int evaluateTotalScore(List<List<Person>> groups, Map<String, Map<String, Integer>> overlapCountMap, List<String> criteria) {
        int score = 0;

        if (criteria.contains("previous")) {
            score += evaluatePreviousOverlapPenalty(groups, overlapCountMap);
        }
        if (criteria.contains("gender")) {
            score += evaluateGenderScore(groups);
        }

        return score;
    }

    // 중복 횟수당 페널티 부과 (-100점)
    private int evaluatePreviousOverlapPenalty(List<List<Person>> groups, Map<String, Map<String, Integer>> overlapCountMap) {
        int penalty = 0;
        for (List<Person> group : groups) {
            for (int i = 0; i < group.size(); i++) {
                for (int j = i + 1; j < group.size(); j++) {
                    String name1 = group.get(i).getName();
                    String name2 = group.get(j).getName();

                    int overlap = overlapCountMap
                            .getOrDefault(name1, Collections.emptyMap())
                            .getOrDefault(name2, 0);

                    if (overlap > 0) {
                        penalty += PENALTY_OVERLAP; 
                    }
                }
            }
        }
        return penalty;
    }

    // 성비 불균형도에 따른 페널티 부과 (Soft Constraint - 오차의 제곱)
    private int evaluateGenderScore(List<List<Person>> groups) {
        int totalMale = 0;
        int totalFemale = 0;

        for (List<Person> group : groups) {
            for (Person p : group) {
                if ("M".equals(p.getGender())) totalMale++;
                else if ("F".equals(p.getGender())) totalFemale++;
            }
        }

        int numGroups = groups.size();
        double idealMalePerGroup = (double) totalMale / numGroups;
        double idealFemalePerGroup = (double) totalFemale / numGroups;

        int score = 0;

        for (List<Person> group : groups) {
            int maleCount = 0;
            int femaleCount = 0;

            for (Person p : group) {
                if ("M".equals(p.getGender())) maleCount++;
                else if ("F".equals(p.getGender())) femaleCount++;
            }

            // 한쪽에 몰리는 것을 방지하기 위해 분산(차이의 제곱)을 빼줍니다.
            score -= (int) Math.pow(maleCount - idealMalePerGroup, 2);
            score -= (int) Math.pow(femaleCount - idealFemalePerGroup, 2);
        }

        return score;
    }

    // 페널티 계산이 아닌, 실제로 "몇 쌍이 겹쳤는지" 순수하게 세어주는 메서드 (알림창 용도)
    private List<String> findOverlappingPairs(List<List<Person>> groups, Map<String, Map<String, Integer>> overlapCountMap) {
        List<String> pairs = new ArrayList<>();
        for (List<Person> group : groups) {
            for (int i = 0; i < group.size(); i++) {
                for (int j = i + 1; j < group.size(); j++) {
                    String name1 = group.get(i).getName();
                    String name2 = group.get(j).getName();

                    int overlap = overlapCountMap
                            .getOrDefault(name1, Collections.emptyMap())
                            .getOrDefault(name2, 0);

                    if (overlap > 0) {
                        // "김철수-이영희" 형태로 저장
                        pairs.add(name1 + "-" + name2); 
                    }
                }
            }
        }
        return pairs;
    }

    // 이전 조 편성 결과 엑셀 데이터를 바탕으로 중복 카운트 맵을 만드는 메서드
    private Map<String, Map<String, Integer>> buildOverlapMap(List<List<List<String>>> previousGroupings) {
        Map<String, Map<String, Integer>> overlapCount = new HashMap<>();

        for (List<List<String>> grouping : previousGroupings) {
            for (List<String> group : grouping) {
                for (int i = 0; i < group.size(); i++) {
                    for (int j = i + 1; j < group.size(); j++) {
                        String p1 = group.get(i);
                        String p2 = group.get(j);

                        overlapCount.computeIfAbsent(p1, k -> new HashMap<>()).merge(p2, 1, Integer::sum);
                        overlapCount.computeIfAbsent(p2, k -> new HashMap<>()).merge(p1, 1, Integer::sum);
                    }
                }
            }
        }

        return overlapCount;
    }
}