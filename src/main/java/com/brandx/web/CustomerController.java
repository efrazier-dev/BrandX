package com.brandx.web;

import com.brandx.domain.Customer;
import com.brandx.service.CustomerService;
import com.brandx.service.DuplicateValueException;
import com.brandx.service.EntityInUseException;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** CRUD over customers. Same POST-only mutation pattern as {@link ProductController}. */
@Controller
@RequestMapping("/customers")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @GetMapping
    public String list(@RequestParam(required = false) String search,
                       @PageableDefault(size = 10, sort = "lastName") Pageable pageable,
                       Model model) {
        model.addAttribute("page", customerService.list(search, pageable));
        model.addAttribute("search", search);
        model.addAttribute("sort", PageSupport.sortParam(pageable));
        return "customers/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("customer", new Customer());
        return "customers/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("customer") Customer customer,
                         BindingResult binding,
                         RedirectAttributes flash) {
        if (binding.hasErrors()) {
            return "customers/form";
        }
        try {
            Customer saved = customerService.create(customer);
            flash.addFlashAttribute("successMessage", "Customer '" + saved.getFullName() + "' was created.");
            return "redirect:/customers/" + saved.getId();
        } catch (DuplicateValueException ex) {
            binding.rejectValue(ex.getField(), "duplicate", ex.getMessage());
            return "customers/form";
        }
    }

    @GetMapping("/{id}")
    public String view(@PathVariable Long id, Model model) {
        model.addAttribute("customer", customerService.get(id));
        model.addAttribute("orderCount", customerService.orderCount(id));
        return "customers/view";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("customer", customerService.get(id));
        return "customers/form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("customer") Customer customer,
                         BindingResult binding,
                         RedirectAttributes flash) {
        if (binding.hasErrors()) {
            return "customers/form";
        }
        try {
            Customer saved = customerService.update(id, customer);
            flash.addFlashAttribute("successMessage", "Customer '" + saved.getFullName() + "' was updated.");
            return "redirect:/customers/" + saved.getId();
        } catch (DuplicateValueException ex) {
            binding.rejectValue(ex.getField(), "duplicate", ex.getMessage());
            return "customers/form";
        }
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes flash) {
        try {
            customerService.delete(id);
            flash.addFlashAttribute("successMessage", "Customer was deleted.");
        } catch (EntityInUseException ex) {
            flash.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/customers";
    }
}
