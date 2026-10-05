package vn.edu.demo.topicmanagement.controller;

import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.edu.demo.topicmanagement.dto.TopicForm;
import vn.edu.demo.topicmanagement.entity.Department;
import vn.edu.demo.topicmanagement.entity.RegistrationPeriod;
import vn.edu.demo.topicmanagement.entity.User;
import vn.edu.demo.topicmanagement.service.TopicService;

/**
 * Giảng viên đăng ký đề tài - CHỈ giảng viên (/lecturer/** -> LECTURER).
 *   GET  /lecturer/topics            đề tài của tôi
 *   GET  /lecturer/topics/new        form đăng ký
 *   POST /lecturer/topics/new        gửi form đăng ký
 *   GET  /lecturer/topics/{id}/edit  form sửa
 *   POST /lecturer/topics/{id}/edit  gửi form sửa
 */
@Controller
@RequestMapping("/lecturer/topics")
public class LecturerTopicController {

    private final TopicService topicService;

    public LecturerTopicController(TopicService topicService) {
        this.topicService = topicService;
    }

    @ModelAttribute("departments")
    public List<Department> departments() { return topicService.getDepartments(); }

    @ModelAttribute("periods")
    public List<RegistrationPeriod> periods() { return topicService.getOpenPeriods(); }

    @ModelAttribute("lecturers")
    public List<User> lecturers(Principal principal) {
        return topicService.getCoSupervisorCandidates(principal.getName());
    }

    @GetMapping
    public String list(Principal principal, Model model) {
        model.addAttribute("rows", topicService.getMyTopics(principal.getName()));
        return "lecturer/topics/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("topicForm", new TopicForm());
        setFormInfo(model, "/lecturer/topics/new", "Đăng ký đề tài mới");
        return "lecturer/topics/form";
    }

    @PostMapping("/new")
    public String create(@Valid @ModelAttribute("topicForm") TopicForm form, BindingResult result,
                         Principal principal, Model model, RedirectAttributes redirect) {
        addRuleErrors(form, principal.getName(), result);
        if (result.hasErrors()) {
            setFormInfo(model, "/lecturer/topics/new", "Đăng ký đề tài mới");
            return "lecturer/topics/form";
        }
        topicService.create(form, principal.getName());
        redirect.addFlashAttribute("message", "Đã gửi đề tài, đang chờ Trưởng khoa duyệt.");
        return "redirect:/lecturer/topics";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Principal principal, Model model,
                           RedirectAttributes redirect) {
        try {
            model.addAttribute("topicForm", topicService.getFormForEdit(id, principal.getName()));
        } catch (IllegalStateException e) {
            redirect.addFlashAttribute("error", e.getMessage());
            return "redirect:/lecturer/topics";
        }
        setFormInfo(model, "/lecturer/topics/" + id + "/edit", "Sửa đề tài");
        return "lecturer/topics/form";
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id, @Valid @ModelAttribute("topicForm") TopicForm form,
                         BindingResult result, Principal principal, Model model,
                         RedirectAttributes redirect) {
        addRuleErrors(form, principal.getName(), result);
        if (result.hasErrors()) {
            setFormInfo(model, "/lecturer/topics/" + id + "/edit", "Sửa đề tài");
            return "lecturer/topics/form";
        }
        try {
            topicService.update(id, form, principal.getName());
            redirect.addFlashAttribute("message", "Đã cập nhật đề tài và gửi duyệt lại.");
        } catch (IllegalStateException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/lecturer/topics";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, Principal principal, RedirectAttributes redirect) {
        try {
            topicService.deleteOwnUnsubmitted(id, principal.getName());
            redirect.addFlashAttribute("message", "Đã xóa đề tài.");
        } catch (IllegalStateException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/lecturer/topics";
    }

    private void addRuleErrors(TopicForm form, String username, BindingResult result) {
        Map<String, String> errors = topicService.checkRules(form, username);
        errors.forEach((field, message) -> result.rejectValue(field, "rule", message));
    }

    private void setFormInfo(Model model, String action, String title) {
        model.addAttribute("action", action);
        model.addAttribute("pageTitle", title);
    }
}
