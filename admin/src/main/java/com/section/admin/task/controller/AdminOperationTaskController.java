package com.section.admin.task.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin/settings/tasks")
public class AdminOperationTaskController {

    @RequestMapping({"", "/list"})
    public String taskList() {
        return "views/task-list";
    }

    @GetMapping("/get")
    public String taskDetail(@RequestParam("no") Long taskNo,
                             @RequestParam(value = "returnTo", required = false) String returnTo,
                             Model model) {
        model.addAttribute("taskNo", taskNo);
        model.addAttribute("returnTo", returnTo == null || returnTo.isBlank() ? "/admin/settings/tasks" : returnTo);
        return "views/task-get";
    }
}
