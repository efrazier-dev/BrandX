package com.brandx.web;

import com.brandx.service.CustomerService;
import com.brandx.service.OrderService;
import com.brandx.service.ProductService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/** Landing page: record counts and links into each CRUD area. */
@Controller
public class HomeController {

    private final ProductService productService;
    private final CustomerService customerService;
    private final OrderService orderService;

    public HomeController(ProductService productService,
                          CustomerService customerService,
                          OrderService orderService) {
        this.productService = productService;
        this.customerService = customerService;
        this.orderService = orderService;
    }

    @GetMapping("/")
    public String dashboard(Model model) {
        model.addAttribute("productCount", productService.count());
        model.addAttribute("customerCount", customerService.count());
        model.addAttribute("orderCount", orderService.count());
        return "index";
    }
}
