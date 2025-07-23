package com.assist.GroupAssignerAssist;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeContorller {
	@GetMapping(value={"/","main"})
	public String main() {
		return "main";
	}
}
