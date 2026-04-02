package com.pms.patient.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DebugController {

    @Autowired(required = false)
    private io.micrometer.tracing.Tracer tracer;

    @GetMapping("/debug-tracer")
    public String debug() {
        return tracer == null ? "Tracer is NULL ❌" : "Tracer is OK ✅";
    }
}
