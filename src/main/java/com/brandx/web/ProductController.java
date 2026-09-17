package com.brandx.web;

import com.brandx.domain.Product;
import com.brandx.service.DuplicateValueException;
import com.brandx.service.EntityInUseException;
import com.brandx.service.ProductService;
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

/**
 * CRUD over products.
 *
 * <p>Updates and deletes are POSTs, not PUT/DELETE: an HTML form can only emit GET
 * or POST, and Boot's {@code HiddenHttpMethodFilter} is disabled by default.
 */
@Controller
@RequestMapping("/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public String list(@RequestParam(required = false) String search,
                       @PageableDefault(size = 10, sort = "name") Pageable pageable,
                       Model model) {
        model.addAttribute("page", productService.list(search, pageable));
        model.addAttribute("search", search);
        model.addAttribute("sort", PageSupport.sortParam(pageable));
        return "products/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("product", new Product());
        return "products/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("product") Product product,
                         BindingResult binding,
                         RedirectAttributes flash) {
        if (binding.hasErrors()) {
            return "products/form";
        }
        try {
            Product saved = productService.create(product);
            flash.addFlashAttribute("successMessage", "Product '" + saved.getName() + "' was created.");
            return "redirect:/products/" + saved.getId();
        } catch (DuplicateValueException ex) {
            // Attach to the offending field so the user sees it next to the input.
            binding.rejectValue(ex.getField(), "duplicate", ex.getMessage());
            return "products/form";
        }
    }

    @GetMapping("/{id}")
    public String view(@PathVariable Long id, Model model) {
        model.addAttribute("product", productService.get(id));
        return "products/view";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("product", productService.get(id));
        return "products/form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("product") Product product,
                         BindingResult binding,
                         RedirectAttributes flash) {
        if (binding.hasErrors()) {
            return "products/form";
        }
        try {
            // The path id wins; the submitted id is display-only and untrusted.
            Product saved = productService.update(id, product);
            flash.addFlashAttribute("successMessage", "Product '" + saved.getName() + "' was updated.");
            return "redirect:/products/" + saved.getId();
        } catch (DuplicateValueException ex) {
            binding.rejectValue(ex.getField(), "duplicate", ex.getMessage());
            return "products/form";
        }
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes flash) {
        try {
            productService.delete(id);
            flash.addFlashAttribute("successMessage", "Product was deleted.");
        } catch (EntityInUseException ex) {
            flash.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/products";
    }
}
