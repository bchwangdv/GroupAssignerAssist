package com.assist.GroupAssignerAssist.controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.assist.GroupAssignerAssist.model.Person;

@Controller
public class GroupAssignerAssistController {

	@GetMapping("loadStudentFromProfile")
	public String loadStudentFromProfile(@RequestParam("numGroups") int numGroups, @RequestParam("moduleNo") int moduleNo) {
		System.out.println(numGroups +" + "+ moduleNo);
//		List<Person> people = new ArrayList<>();
//
//		try(BufferedReader br = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
//
//			String line = br.readLine();
//	   
//			if (line.startsWith("\uFEFF")) {
//				line = line.substring(1); // BOM 제거
//			}
//   
//			while ((line = br.readLine()) != null) {
//				String[] tokens = line.split(",");
//				if (tokens.length < 3) continue; // 안전장치
//
//				String name = tokens[0].trim();
//				String gender = tokens[1].trim();
//				String job = tokens[2].trim();
//
//				people.add(new Person(name, gender, job));
//			}
//			
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
		
		
		return "processing";
	}
	@GetMapping("result")
	public String resultPage() {
		return "result";
	}
//    public static List<List<Person>> splitIntoGroup(List<Person> people, int numGroups) {
//
//        List<Person> shuffled = new ArrayList<>(people);
//        Collections.shuffle(shuffled);
//        List<List<Person>> groups = new ArrayList<>();
//        for (int i = 0; i < numGroups; i++) {
//            groups.add(new ArrayList<>());
//        }
//
//        for (int i = 0; i < shuffled.size(); i++) {
//           groups.get(i % numGroups).add(shuffled.get(i));
//        }
//
//        return groups;
//    }
//
//    public static Map<String, Set<String>> loadPastGroups(String filename, int currentRound) {
//        Map<Integer, Map<Integer, List<String>>> pastRounds = new HashMap<>();
//        Map<String, Set<String>> metMap = new HashMap<>();
//
//        File file = new File(filename); // 파일 명
//        if (!file.exists()) return metMap;
//
//        try(BufferedReader br = new BufferedReader(new FileReader(filename))) {
//            String line = br.readLine(); // header skip
//            while ((line = br.readLine()) != null) {
//                String[] tokens = line.split(",");
//                int round = Integer.parseInt(tokens[0]);
//                if(round >= currentRound) {
//                    continue;
//                }
//                int groupNum = Integer.parseInt(tokens[1]);
//                String name = tokens[2];
//
//                pastRounds.computeIfAbsent(round, k -> new HashMap<>())
//                        .computeIfAbsent(groupNum, k -> new ArrayList<>()).add(name);
//            }
//
//            for(Map<Integer, List<String>> roundGroups : pastRounds.values()) {
//                for(List<String> group : roundGroups.values()) {
//                    for(int i = 0; i < group.size(); i++) {
//                        for (int j = i + 1; j < group.size(); j++) {
//                            String a = group.get(i);
//                            String b = group.get(j);
//                            metMap.computeIfAbsent(a, k -> new HashSet<>()).add(b);
//                            metMap.computeIfAbsent(b, k -> new HashSet<>()).add(a);
//                        }
//                    }
//                }
//            }
//
//        } catch (FileNotFoundException e) {
//            throw new RuntimeException(e);
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
//
//        return metMap;
//    }
//
//    public static int evaluateGroupScore(List<List<Person>> groups, Map<String, Set<String>> metMap) {
//        int score = 0;
//
//        for (List<Person> group : groups) {
//            int maleCount = 0;
//            int femaleCount = 0;
//            Set<String> jobSet = new HashSet<>();
//
//            for (Person p : group) {
//                if(p.gender.equals("M")) maleCount++;
//                if(p.gender.equals("F")) femaleCount++;
//
//                jobSet.add(p.job);
//            }
//
//            score -= Math.abs(maleCount - femaleCount); // 성비 균형
//            score += jobSet.size(); // 직업
//
//            for (int i = 0; i < group.size(); i++) {
//                for (int j = i + 1; j < group.size(); j++) {
//                    Person a = group.get(i);
//                    Person b = group.get(j);
//
//                    if (metMap.getOrDefault(a.name, Collections.emptySet()).contains(b.name)) {
//                        score -= 5; // 중복 감점
//                    }
//                }
//            }
//        }
//
//        return score;
//    }
//
//    public static List<List<Person>> assignBestGrouping(List<Person> people, int numGroups, int trialCount, Map<String, Set<String>> metMap) {
//
//        int bestScore = Integer.MIN_VALUE;
//        List<List<Person>> bestGroups= null;
//
//        for (int t = 0; t < trialCount; t++) {
//
//            List<List<Person>> groups = splitIntoGroup(people, numGroups);
//            int score = evaluateGroupScore(groups, metMap);
//            if(score > bestScore) {
//                bestScore = score;
//                bestGroups = groups;
//            }
//
//        }
//
//        return bestGroups;
//    }
//
//    public static void saveGroupsToCSV(List<List<Person>> groups, int round, String filename) {
//        boolean fileExists = new java.io.File(filename).exists();
//
//        try (FileOutputStream fos = new FileOutputStream(filename, true)) {
//            // If file is new (not exists), write BOM first
//            if (!fileExists) {
//                fos.write(0xEF);
//                fos.write(0xBB);
//                fos.write(0xBF);
//            }
//
//            try (Writer writer = new BufferedWriter(new OutputStreamWriter(fos, StandardCharsets.UTF_8))) {
//                if (!fileExists) {
//                    writer.write("Module,Group,Name,Gender,Job\n");
//                }
//
//                for (int i = 0; i < groups.size(); i++) {
//                    for (Person p : groups.get(i)) {
//                        writer.write(String.format("%d,%d,%s,%s,%s\n", round, i + 1, p.name, p.gender, p.job));
//                    }
//                }
//            }
//
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//

    
//	public static void assignGroups(List<Person> people, int numGroups) {
//		splitIntoGroup(people, numGroups);
//		return ;
//	}
	
}
