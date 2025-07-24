package com.assist.GroupAssignerAssist.controller;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import com.assist.GroupAssignerAssist.HomeContorller;
import com.assist.GroupAssignerAssist.model.Person;


@Controller
public class GroupAssignerAssistController {
	private static final int TRIAL_COUNT = 1000; // 조편성 시도 횟수
	
	public List<Person> loadStudentFromProfile(MultipartFile profile
		) {
		
		List<Person> people = new ArrayList<>();
		
		try(BufferedReader br = new BufferedReader(new InputStreamReader(profile.getInputStream(), StandardCharsets.UTF_8))) {
			String line = br.readLine();
			
			if (line.startsWith("\uFEFF")) {
                line = line.substring(1); // BOM 제거
            }
			
	        while ((line = br.readLine()) != null) {
				String[] tokens = line.split(",");	
				if (tokens.length < 3) continue;

				String name = tokens[0].trim();
				String gender = tokens[1].trim();
				String job = tokens[2].trim();

				people.add(new Person(name, gender, job));
			}
	        
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return people;
	}
	
    public static List<List<Person>> splitIntoGroup(List<Person> people, int numGroups) {

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
    
    public static int evaluateGroupScore(List<List<Person>> groups) {
        int score = 0;

        for (List<Person> group : groups) {
            int maleCount = 0;
            int femaleCount = 0;
            Set<String> jobSet = new HashSet<>();

            for (Person p : group) {
                if(p.getGender().equals("M")) maleCount++;
                if(p.getGender().equals("F")) femaleCount++;

                jobSet.add(p.getJob());
            }

            score -= Math.abs(maleCount - femaleCount); // 성비 균형
            score += jobSet.size(); // 직업
        }
        return score;
    }

    public static List<List<Person>> assignBestGrouping(List<Person> people, int numGroups, int trialCount) {

        int bestScore = Integer.MIN_VALUE;
        List<List<Person>> bestGroups= null;

        for (int t = 0; t < trialCount; t++) {

            List<List<Person>> groups = splitIntoGroup(people, numGroups);
            int score = evaluateGroupScore(groups);
            if(score > bestScore) {
                bestScore = score;
                bestGroups = groups;
            }
            
        }
        return bestGroups;
    }
    
	@PostMapping("result")
	public String result(
		@RequestParam("numGroups") int numGroups,
		@RequestParam("profile") MultipartFile profile,
		Model model) {
		
		int trialCount = TRIAL_COUNT;
		
		List<Person> people = loadStudentFromProfile(profile);
		List<List<Person>> bestGroups = assignBestGrouping(people, numGroups, trialCount);
		
		model.addAttribute("numGroups", numGroups);
		model.addAttribute("bestGroups", bestGroups);
		return "result";
	}
}
