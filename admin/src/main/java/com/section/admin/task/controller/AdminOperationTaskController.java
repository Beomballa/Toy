package com.section.admin.task.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin/settings/tasks")
public class AdminOperationTaskController {

    @RequestMapping({"", "/list"})
    public String taskList() {
        return "views/task-list";
    }
}
