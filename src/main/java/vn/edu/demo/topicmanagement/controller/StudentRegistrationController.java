package vn.edu.demo.topicmanagement.controller;

import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.edu.demo.topicmanagement.dto.TopicRegistrationForm;
import vn.edu.demo.topicmanagement.service.StudentPortalService;

@Controller
@RequestMapping("/student/registrations")
public class StudentRegistrationController {
    private final StudentPortalService studentService;

    public StudentRegistrationController(StudentPortalService studentService) { this.studentService = studentService; }

    @GetMapping
    public String list(Principal principal, Model model) {
        model.addAttribute("myGroup", studentService.getMyGroup(principal.getName()).orElse(null));
        model.addAttribute("registrations", studentService.getMyRegistrations(principal.getName()));
        model.addAttribute("topics", studentService.getAvailableTopics());
        model.addAttribute("registrationForm", new TopicRegistrationForm());
        return "student/registrations/list";
    }

    @PostMapping
    public String register(@Valid @ModelAttribute("registrationForm") TopicRegistrationForm form,
                           BindingResult result, Principal principal, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            redirect.addFlashAttribute("error", "Vui lòng chọn đề tài.");
            return "redirect:/student/registrations";
        }
        try {
            studentService.registerTopic(form.getTopicId(), principal.getName());
            redirect.addFlashAttribute("message", "Đã gửi đăng ký đề tài, đang chờ Trưởng khoa duyệt.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/student/registrations";
    }
}
