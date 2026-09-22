package com.brandx.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end checks on the rules that {@code sec:authorize} only makes invisible.
 *
 * <p>Deliberately drives real HTTP requests rather than calling the services: hiding a
 * button and refusing the POST behind it are separate mechanisms, and only the second
 * one is security. Each "forbidden" case below is a request a STAFF user could make by
 * hand after the button was hidden from them.
 */
@SpringBootTest
@AutoConfigureMockMvc
class SecurityRulesTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithAnonymousUser
    void anonymousRequestToAnAppScreen_isSentToLogin() throws Exception {
        mockMvc.perform(get("/products"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    @WithAnonymousUser
    void loginPage_isReachableWithoutSigningIn() throws Exception {
        mockMvc.perform(get("/login")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "STAFF")
    void staff_canReadProducts() throws Exception {
        mockMvc.perform(get("/products")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "STAFF")
    void staff_cannotCreateAProduct() throws Exception {
        mockMvc.perform(post("/products").with(csrf())
                        .param("sku", "BX-TEST-01")
                        .param("name", "Test Widget")
                        .param("price", "9.99")
                        .param("stockQuantity", "5"))
                .andExpect(status().isForbidden())
                .andExpect(forwardedUrl("/access-denied"));
    }

    @Test
    @WithMockUser(roles = "STAFF")
    void staff_cannotDeleteAnOrder() throws Exception {
        mockMvc.perform(post("/orders/1/delete").with(csrf()))
                .andExpect(status().isForbidden())
                .andExpect(forwardedUrl("/access-denied"));
    }

    /**
     * The other half of the two tests above, and not redundant with them: MockMvc
     * records the forward to /access-denied but does not execute it, so those two would
     * still pass if nothing could actually handle the forwarded request. A forward keeps
     * the original method, so the refusal of a blocked POST arrives here as a POST —
     * which a {@code @GetMapping} would answer with 405 rather than 403.
     */
    @Test
    @WithMockUser(roles = "STAFF")
    void accessDeniedPage_answersTheForwardedPostRatherThanRejectingTheMethod() throws Exception {
        mockMvc.perform(post("/access-denied").with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void admin_canCreateAProduct() throws Exception {
        mockMvc.perform(post("/products").with(csrf())
                        .param("sku", "BX-ADMIN-01")
                        .param("name", "Admin Widget")
                        .param("price", "12.50")
                        .param("stockQuantity", "3"))
                .andExpect(status().is3xxRedirection());
    }

    /**
     * The gap this closes: before Spring Security was added, every state-changing route
     * in the app was a public, tokenless POST.
     */
    @Test
    @WithMockUser(roles = "ADMIN")
    void postWithoutACsrfToken_isRejectedEvenForAnAdmin() throws Exception {
        mockMvc.perform(post("/products")
                        .param("sku", "BX-NOCSRF-01")
                        .param("name", "No Token")
                        .param("price", "1.00")
                        .param("stockQuantity", "1"))
                .andExpect(status().isForbidden());
    }
}
