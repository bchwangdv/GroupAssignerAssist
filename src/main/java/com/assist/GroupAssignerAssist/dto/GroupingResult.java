package com.assist.GroupAssignerAssist.dto;

import com.assist.GroupAssignerAssist.model.Person;
import java.util.List;

public class GroupingResult {
    private List<List<Person>> groups;
    private int overlapCount; // 겹친 조합 수
    private List<String> overlappingPairs;

    public GroupingResult(List<List<Person>> groups, int overlapCount, List<String> overlappingPairs) {
        this.groups = groups;
        this.overlapCount = overlapCount;
        this.overlappingPairs = overlappingPairs;
    }

    public List<List<Person>> getGroups() {
    	return groups;
    }
    public int getOverlapCount() {
    	return overlapCount;
    }
    public List<String> getOverlappingPairs() {
    	return overlappingPairs;
    }
}