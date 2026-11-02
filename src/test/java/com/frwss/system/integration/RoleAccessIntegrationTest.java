package com.frwss.system.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Checks which roles may change payment and reconciliation data.
 * Only requests that are refused are sent with real effect; the "allowed"
 * checks use requests that stop at validation, so no data is written.
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Role Access Integration Tests")
class RoleAccessIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    // =========================================================================
    // Roles that must be refused
    // =========================================================================

    @Test
    @DisplayName("Should refuse an auditor who tries to run payment reconciliation")
    void auditor_runPaymentReconciliation_isForbidden() throws Exception {
        mockMvc.perform(post("/payment/api/reconcile")
                        .with(user("auditor1").roles("AUDITOR"))
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should refuse an approver who tries to process a payment")
    void approver_processPayment_isForbidden() throws Exception {
        mockMvc.perform(post("/payment/api/process")
                        .with(user("approver1").roles("APPROVER"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should refuse an auditor who tries to refund a payment")
    void auditor_refundPayment_isForbidden() throws Exception {
        mockMvc.perform(post("/payment/api/refund/TXN-1")
                        .with(user("auditor1").roles("AUDITOR"))
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should refuse an auditor who tries to run exact reconciliation")
    void auditor_runExactReconciliation_isForbidden() throws Exception {
        mockMvc.perform(post("/reconciliation/run-exact")
                        .with(user("auditor1").roles("AUDITOR"))
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should refuse an approver who tries to resolve an orphan")
    void approver_resolveOrphan_isForbidden() throws Exception {
        mockMvc.perform(post("/reconciliation/orphans/1/resolve")
                        .with(user("approver1").roles("APPROVER"))
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    // =========================================================================
    // Roles that must get through
    // =========================================================================

    @Test
    @DisplayName("Should let entry staff past the role check on payments (the request then fails validation)")
    void entryStaff_processPayment_isNotForbidden() throws Exception {
        // An empty body is rejected by payment validation with 400, which proves the
        // role check passed and nothing was processed.
        mockMvc.perform(post("/payment/api/process")
                        .with(user("entry1").roles("ENTRY_STAFF"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should let entry staff past the role check on reconciliation")
    void entryStaff_resolveOrphan_isNotForbidden() throws Exception {
        // Required form fields are missing, so the request stops before any change is made.
        mockMvc.perform(post("/reconciliation/orphans/999999/resolve")
                        .with(user("entry1").roles("ENTRY_STAFF"))
                        .with(csrf()))
                .andExpect(result -> assertNotEquals(403, result.getResponse().getStatus()));
    }

    @Test
    @DisplayName("Should still let an auditor read payment statistics")
    void auditor_readPaymentStats_isAllowed() throws Exception {
        mockMvc.perform(get("/payment/api/stats")
                        .with(user("auditor1").roles("AUDITOR")))
                .andExpect(status().isOk());
    }
}