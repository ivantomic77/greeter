package dev.tomic.greetingservice.greet;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1.0")
public class GreetController {
    private final GreetService greetService;

    public GreetController(GreetService greetService) {
        this.greetService = greetService;
    }

    @GetMapping("/greet")
    public ResponseEntity<String> greet(@RequestHeader("x-user-auth") String authHeader, @RequestHeader("Authorization") String auth) {
        return ResponseEntity.ok(greetService.greet(authHeader));
    }
}
