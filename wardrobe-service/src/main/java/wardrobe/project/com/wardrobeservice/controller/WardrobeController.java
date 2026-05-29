package wardrobe.project.com.wardrobeservice.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/wardrobe")
public class WardrobeController {

    @GetMapping("/test")
    public String test() {
        return "Wardrobe Service is running";
    }
}