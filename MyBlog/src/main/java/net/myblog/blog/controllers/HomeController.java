package net.myblog.blog.controllers;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HomeController {
    @GetMapping("/")
    @CrossOrigin(origins = "http://localhost:3000")
    public String home() {
        return "Welcome to MyBlog!";
    }
}
