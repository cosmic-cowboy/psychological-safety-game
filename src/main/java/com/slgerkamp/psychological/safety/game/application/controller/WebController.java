package com.slgerkamp.psychological.safety.game.application.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class WebController {

    @GetMapping("/stage")
    public String stage(){
        return "stage";
    }

}
