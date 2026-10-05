package vn.edu.demo.topicmanagement.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.edu.demo.topicmanagement.service.StudentPortalService;

@Controller
@RequestMapping("/head/registrations")
public class HeadRegistrationController {
    private final StudentPortalService studentService;

    public HeadRegistrationController(StudentPortalService studentService) { this.studentService = studentService; }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("registrations", studentService.getPendingRegistrations());
        return "head/registrations/list";
    }

    @PostMapping("/{id}/approve")
    public String approve(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            studentService.approveRegistration(id);
            redirect.addFlashAttribute("message", "Đã duyệt đăng ký đề tài.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/head/registrations";
    }

    @PostMapping("/{id}/reject")
    public String reject(@PathVariable Long id, @RequestParam(defaultValue = "") String reason,
                         RedirectAttributes redirect) {
        try {
            studentService.rejectRegistration(id, reason);
            redirect.addFlashAttribute("message", "Đã từ chối đăng ký đề tài.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/head/registrations";
    }
}
