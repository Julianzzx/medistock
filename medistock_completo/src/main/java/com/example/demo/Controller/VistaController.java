package com.example.demo.Controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class VistaController {

    @GetMapping("/farmacias")
    public String farmacias() {
        return "farmacias"; 
    }

    @GetMapping("/medicamentos")
    public String medicamentos() {
        return "medicamentos"; 
    }

    @GetMapping("/soporte")
    public String soporte() {
        return "soporte"; 
    }
}
