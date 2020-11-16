package com.slgerkamp.psychological.safety.game.application.controller;

import com.slgerkamp.psychological.safety.game.domain.game.service.StageService;
import com.slgerkamp.psychological.safety.game.infra.utils.QrCodeGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.web.bind.annotation.*;

import java.io.InputStream;
import java.util.Map;

@RestController
public class APIController {

    @Autowired
    private QrCodeGenerator qrCodeGenerator;
    @Autowired
    private StageService stageService;

    @RequestMapping("/stage/{stageId}/qrcode")
    public ResponseEntity<?> image(@PathVariable String stageId) {
        InputStream in = qrCodeGenerator.readStageJoinUrlQrCode(stageId);
        HttpHeaders headers = new HttpHeaders();
        headers.setPragma("");
        return new ResponseEntity<Resource>(
                new InputStreamResource(in),
                headers,
                HttpStatus.OK);
    }

    @PostMapping("/stage/{stageId}/start")
    @ResponseStatus(HttpStatus.CREATED)
    public String start(@PathVariable String stageId, final OAuth2Authentication oAuth2Authentication) {
        final String userId = getUserId(oAuth2Authentication);
        stageService.startStage(userId, stageId);
        return "ok";
    }

///////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////
///////////////////////  private method  //////////////////////////
///////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////

    private String getUserId(OAuth2Authentication oAuth2Authentication) {
        Map<String, Object> properties =
                (Map<String, Object>) oAuth2Authentication.getUserAuthentication().getDetails();
        return (String) properties.get("userId");
    }

}
