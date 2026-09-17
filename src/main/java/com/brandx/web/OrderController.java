package com.brandx.web;

import com.brandx.domain.Order;
import com.brandx.domain.OrderStatus;
import com.brandx.service.CustomerService;
import com.brandx.service.OrderService;
import com.brandx.service.ProductService;
import com.brandx.web.form.OrderForm;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
 * CRUD over orders.
 *
 * <p>Unlike products and customers this binds to an {@link OrderForm} rather than the
 * entity, because the screen posts ids plus a variable number of line rows and the
 * line prices are decided server-side.
 */
@Controller
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;
    private final CustomerService customerService;
    private final ProductService productService;

    public OrderController(OrderService orderService,
                           CustomerService customerService,
                           ProductService productService) {
        this.orderService = orderService;
        this.customerService = customerService;
        this.productService = productService;
    }

    @GetMapping
    public String list(@RequestParam(required = false) String search,
                       @PageableDefault(size = 10, sort = "orderedAt", direction = Sort.Direction.DESC)
                       Pageable pageable,
                       Model model) {
        Page<Order> page = orderService.list(search, pageable);
        model.addAttribute("page", page);
        model.addAttribute("search", search);
        model.addAttribute("sort", PageSupport.sortParam(pageable));
        // Totals come from one aggregate query over the ids on this page; the list
        // query does not fetch order.items, so the template cannot sum them itself.
        model.addAttribute("totals",
                orderService.totalsFor(page.getContent().stream().map(Order::getId).toList()));
        return "orders/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        OrderForm form = new OrderForm();
        model.addAttribute("orderForm", form);
        populateLookups(model);
        return "orders/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("orderForm") OrderForm form,
                         BindingResult binding,
                         Model model,
                         RedirectAttributes flash) {
        if (binding.hasErrors()) {
            populateLookups(model);
            return "orders/form";
        }
        Order saved = orderService.create(form);
        flash.addFlashAttribute("successMessage", "Order " + saved.getOrderNumber() + " was created.");
        return "redirect:/orders/" + saved.getId();
    }

    @GetMapping("/{id}")
    public String view(@PathVariable Long id, Model model) {
        model.addAttribute("order", orderService.getDetail(id));
        return "orders/view";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("orderForm", OrderForm.fromOrder(orderService.getDetail(id)));
        populateLookups(model);
        return "orders/form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("orderForm") OrderForm form,
                         BindingResult binding,
                         Model model,
                         RedirectAttributes flash) {
        if (binding.hasErrors()) {
            populateLookups(model);
            return "orders/form";
        }
        Order saved = orderService.update(id, form);
        flash.addFlashAttribute("successMessage", "Order " + saved.getOrderNumber() + " was updated.");
        return "redirect:/orders/" + saved.getId();
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes flash) {
        orderService.delete(id);
        flash.addFlashAttribute("successMessage", "Order was deleted.");
        return "redirect:/orders";
    }

    /**
     * Dropdown contents for the form. Must be re-added on a validation failure too,
     * since returning the view name (rather than redirecting) reuses this request's
     * model and the selects would otherwise render empty.
     */
    private void populateLookups(Model model) {
        model.addAttribute("customers", customerService.allForSelection());
        model.addAttribute("products", productService.activeProducts());
        model.addAttribute("statuses", OrderStatus.values());
    }
}
