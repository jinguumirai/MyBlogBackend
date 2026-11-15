package net.myblog.login.controller;

import net.myBlog.common.responses.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test")
public class TestController {
    @GetMapping("/hello")
    public ResponseEntity<ApiResponse<String>> test() {
        // フロントのapi-clientがApiResponse(code/message/data)形式を期待するためエンベロープで返す
        return ResponseEntity.ok(ApiResponse.success("Hello, this is a test."));
    }
}
