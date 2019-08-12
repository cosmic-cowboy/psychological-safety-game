package com.slgerkamp.psychological.safety.game.application.controller;

import com.slgerkamp.psychological.safety.game.domain.game.service.StageService;
import com.slgerkamp.psychological.safety.game.infra.utils.QrCodeGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

@RestController
public class APIController {

    @Autowired
    private StageService stageService;

    @Autowired
    private QrCodeGenerator qrCodeGenerator;

    @RequestMapping("/stage/{stageId}/qrcode")
    public ResponseEntity<?> image(@PathVariable String stageId) throws FileNotFoundException {

        InputStream in = qrCodeGenerator.read(stageId);
        HttpHeaders headers = new HttpHeaders();
        headers.setPragma("");
        return new ResponseEntity<Resource>(
                new InputStreamResource(in),
                headers,
                HttpStatus.OK);
    }

}
