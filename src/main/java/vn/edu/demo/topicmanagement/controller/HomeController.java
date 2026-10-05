package vn.edu.demo.topicmanagement.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controller = nhận yêu cầu từ trình duyệt (URL) và trả về tên trang giao diện.
 * Chuỗi trả về như "home" nghĩa là file templates/home.html.
 */
@Controller
public class HomeController {

    @GetMapping("/")
    public String home() {
        return "home";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }
}
