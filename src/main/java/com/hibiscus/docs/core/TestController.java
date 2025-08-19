package com.hibiscus.docs.core;

import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/test")
public class TestController {



    @GetMapping("/hello")
    public String sayHello() {
        return "Hello, World!";
    }

    @GetMapping("/hello/{name}")
    public String sayHelloWithName(@PathVariable("name") String name) {
        return "Hello, " + name + "!";
    }

    @GetMapping("/hello/param")
    public String getWithParam(@RequestParam(name = "name", defaultValue = "World") String name) {
        return "Hello, " + name + "!";
    }

    @PostMapping("/example")
    public PostRequest postExample(@RequestBody PostRequest request) {
        return request;
    }

    @PutMapping("/example")
    public String putExample(@RequestParam("message") String message) {
        return "PUT: " + message;
    }

    @PatchMapping("/example")
    public String patchExample(@RequestParam("message") String message) {
        return "PATCH: " + message;
    }

    @DeleteMapping("/example")
    public String deleteExample(@RequestParam("id") String id) {
        return "Deleted: " + id;
    }





    public static class PostRequest {
        private String name;
        private String message;

        public PostRequest() {}

        public PostRequest(String name, String message) {
            this.name = name;
            this.message = message;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
}