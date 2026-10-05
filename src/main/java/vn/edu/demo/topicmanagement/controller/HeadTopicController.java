package vn.edu.demo.topicmanagement.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.edu.demo.topicmanagement.service.TopicService;

/**
 * Trưởng khoa duyệt đề tài - CHỈ HEAD (/head/** -> HEAD).
 *   GET  /head/topics               danh sách tất cả đề tài
 *   POST /head/topics/{id}/approve  duyệt
 *   POST /head/topics/{id}/reject   từ chối (kèm lý do)
 *   POST /head/topics/{id}/publish  công bố
 *   POST /head/topics/publish-all   công bố tất cả đề tài đã duyệt
 */
@Controller
@RequestMapping("/head/topics")
public class HeadTopicController {

    private final TopicService topicService;

    public HeadTopicController(TopicService topicService) {
        this.topicService = topicService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("rows", topicService.getAllForHead());
        return "head/topics/list";
    }

    @PostMapping("/{id}/approve")
    public String approve(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            topicService.approve(id);
            redirect.addFlashAttribute("message", "Đã duyệt đề tài.");
        } catch (IllegalStateException | IllegalArgumentException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/head/topics";
    }

    @PostMapping("/{id}/reject")
    public String reject(@PathVariable Long id,
                         @RequestParam(defaultValue = "") String reason,   // lý do từ chối nhập từ form
                         RedirectAttributes redirect) {
        try {
            topicService.reject(id, reason);
            redirect.addFlashAttribute("message", "Đã từ chối đề tài.");
        } catch (IllegalStateException | IllegalArgumentException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/head/topics";
    }

    @PostMapping("/{id}/publish")
    public String publish(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            topicService.publish(id);
            redirect.addFlashAttribute("message", "Đã công bố đề tài.");
        } catch (IllegalStateException | IllegalArgumentException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/head/topics";
    }

    @PostMapping("/publish-all")
    public String publishAll(RedirectAttributes redirect) {
        int count = topicService.publishAllApproved();
        redirect.addFlashAttribute("message", "Đã công bố " + count + " đề tài.");
        return "redirect:/head/topics";
    }
}
