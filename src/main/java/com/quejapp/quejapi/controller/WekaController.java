package com.quejapp.quejapi.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

import com.quejapp.quejapi.service.WekaService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/weka")
public class WekaController {
    private final WekaService wekaService;
    
    @PostMapping("/train")
    public ResponseEntity<String> trainModel() {
        try {
            String result = wekaService.trainModel();
            return ResponseEntity.ok("Modelo reentrenado: " + result);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }
    
    @PostMapping("/predict")
    public ResponseEntity<String> predict(@RequestBody Map<String, String> weatherData) {
        try {
            String tipo = weatherData.get("tipo");
            String canal = weatherData.get("canal");
            String dia = weatherData.get("dia");
            String mes = weatherData.get("mes");
            
            String prediction = wekaService.predict(tipo, canal, dia, mes, false);
            return ResponseEntity.ok(prediction);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }
    
    @GetMapping("/evaluate")
    public ResponseEntity<String> evaluateModel() {
        try {
            String evaluation = wekaService.evaluateModel();
            return ResponseEntity.ok(evaluation);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }

}
