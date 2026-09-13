package com.frwss.system.workflow.com.frwss.system.workflow.validation;

import com.frwss.system.workflow.model.WorkflowState;
import com.frwss.system.workflow.validation.WorkflowTransitionValidator;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorkflowTransitionValidatorTest {

    @ParameterizedTest(name = "{0} -> {1} should be VALID")
    @CsvSource({
            "ENTERED, REVIEWED",
            "REVIEWED, VERIFIED",
            "REVIEWED, REJECTED"
    })

    void isValidTransition_validTransitions_returnsTrue(
            WorkflowState currentState,
            WorkflowState newState
    ) {
        assertTrue(
                WorkflowTransitionValidator.isValidTransition(currentState, newState)
        );
    }

    @ParameterizedTest(name = "{0} -> {1} should be INVALID")
    @CsvSource({
            "ENTERED, VERIFIED",
            "ENTERED, REJECTED",

            "VERIFIED, ENTERED",
            "VERIFIED, REVIEWED",
            "VERIFIED, REJECTED",

            "REJECTED, ENTERED",
            "REJECTED, REVIEWED",
            "REJECTED, VERIFIED",

            "ENTERED, ENTERED",
            "REVIEWED, REVIEWED",
            "VERIFIED, VERIFIED",
            "REJECTED, REJECTED"
    })
    void isValidTransition_invalidTransitions_returnsFalse(
            WorkflowState currentState,
            WorkflowState newState
    ) {
        assertFalse(
                WorkflowTransitionValidator.isValidTransition(currentState, newState)
        );
    }
}