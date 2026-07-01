package com.yh.qqbot.adapter.web;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class AdminUiForwardController {

    @GetMapping("/favicon.ico")
    @ResponseBody
    public ResponseEntity<Void> favicon() {
        return ResponseEntity.noContent().build();
    }

    @GetMapping({
            "/admin",
            "/admin/",
            "/admin/groups",
            "/admin/memes",
            "/admin/knowledge",
            "/admin/pipeline",
            "/admin/insights",
            "/admin/member-rank",
            "/admin/simulate",
            "/admin/logs",
            "/admin/settings",
            "/admin/runbook"
    })
    public String forwardAdminUi() {
        return "forward:/admin/index.html";
    }
}
