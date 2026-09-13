package com.frwss.system.controller;

import com.frwss.system.dto.IngestionResult;
import com.frwss.system.service.IngestionService;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Ingestion Controller Unit Tests")
class IngestionControllerTest {

    @Mock
    private IngestionService ingestionService;

    @InjectMocks
    private IngestionController ingestionController;

    @BeforeEach
    void setUp() {
        // Mockito injects mocked dependencies.
    }

    // =========================================================================
    // ingestionPage()
    // =========================================================================

    @Test
    @DisplayName("Should display ingestion dashboard")
    void ingestionPage_returnsDashboardView() {

        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------

        Model model = new ConcurrentModel();

        when(ingestionService.getDashboardSummary())
                .thenReturn(new IngestionService.DashboardSummary(10, 8, 2));

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------

        String view =
                ingestionController.ingestionPage(model);

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------

        assertEquals("ingestion/dashboard", view);

        assertEquals(10L, model.getAttribute("total"));
        assertEquals(8L, model.getAttribute("validCount"));
        assertEquals(2L, model.getAttribute("invalidCount"));
    }

    @Test
    @DisplayName("Should display ingestion dashboard through dashboard endpoint")
    void dashboard_returnsDashboardView() {

        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------

        Model model = new ConcurrentModel();
        when(ingestionService.getDashboardSummary())
                .thenReturn(new IngestionService.DashboardSummary(4, 3, 1));

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------

        String view = ingestionController.dashboard(model);

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------

        assertEquals("ingestion/dashboard", view);
        assertEquals(4L, model.getAttribute("total"));
        verify(ingestionService).getDashboardSummary();
    }

    @Test
    @DisplayName("Should display the upload page")
    void showUploadPage_returnsUploadView() {

        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------

        Model model = new ConcurrentModel();

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------

        String view = ingestionController.showUploadPage(model);

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------

        assertEquals("ingestion/upload", view);
    }

    // =========================================================================
    // previewFile()
    // =========================================================================

    @Test
    @DisplayName("Should preview uploaded file successfully")
    void previewFile_validFile_returnsResultPage() throws Exception {

        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------

        MockMultipartFile file =
                new MockMultipartFile(
                        "file",
                        "receipt.csv",
                        "text/csv",
                        "data".getBytes(StandardCharsets.UTF_8)
                );

        Model model = new ConcurrentModel();

        HttpSession session = new MockHttpSession();

        IngestionResult result =
                new IngestionResult(List.of(), 0, 0, 0);

        when(ingestionService.processFileForPreview(file, "RECEIPT"))
                .thenReturn(result);

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------

        String view =
                ingestionController.previewFile(
                        file,
                        "RECEIPT",
                        model,
                        session);

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------

        assertEquals("ingestion/result", view);

        assertNotNull(session.getAttribute("pendingRecords"));

        verify(ingestionService)
                .processFileForPreview(file, "RECEIPT");
    }

    @Test
    @DisplayName("Should return upload page when no file is selected")
    void previewFile_emptyFile_returnsUploadPage() {

        MockMultipartFile file =
                new MockMultipartFile(
                        "file",
                        "",
                        "text/csv",
                        new byte[0]
                );

        Model model = new ConcurrentModel();

        HttpSession session = new MockHttpSession();

        String view =
                ingestionController.previewFile(
                        file,
                        "RECEIPT",
                        model,
                        session);

        assertEquals("ingestion/upload", view);

        assertEquals(
                "Please select a file before uploading.",
                model.getAttribute("error")
        );
    }

    @Test
    @DisplayName("Should return upload page when the file is null")
    void previewFile_nullFile_returnsUploadPage() {

        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------

        Model model = new ConcurrentModel();
        HttpSession session = new MockHttpSession();

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------

        String view = ingestionController.previewFile(null, "RECEIPT", model, session);

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------

        assertEquals("ingestion/upload", view);
        assertEquals(
                "Please select a file before uploading.",
                model.getAttribute("error")
        );
        verifyNoInteractions(ingestionService);
    }

    @Test
    @DisplayName("Should return upload page when preview processing fails")
    void previewFile_serviceFailure_returnsUploadPage() throws Exception {

        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "records.csv",
                "text/csv",
                "data".getBytes(StandardCharsets.UTF_8)
        );
        Model model = new ConcurrentModel();
        HttpSession session = new MockHttpSession();
        when(ingestionService.processFileForPreview(file, "RECEIPT"))
                .thenThrow(new RuntimeException("Preview failure"));

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------

        String view = ingestionController.previewFile(file, "RECEIPT", model, session);

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------

        assertEquals("ingestion/upload", view);
        assertEquals(
                "Preview failed: Preview failure",
                model.getAttribute("error")
        );
    }

    // =========================================================================
    // saveFile()
    // =========================================================================

    @Test
    @DisplayName("Should save pending records successfully")
    void saveFile_validSession_redirectsToUpload() {

        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------

        HttpSession session = new MockHttpSession();

        session.setAttribute("pendingType", "RECEIPT");
        session.setAttribute("pendingRecords", List.of());

        RedirectAttributes ra =
                new RedirectAttributesModelMap();

        when(ingestionService.saveProcessedRecords(anyList(), eq("RECEIPT")))
                .thenReturn(new IngestionService.SaveSummary(5, 1, 0));

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------

        String view =
                ingestionController.saveFile(session, ra);

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------

        assertEquals("redirect:/ingestion/upload", view);

        assertTrue(
                ra.getFlashAttributes()
                        .get("message")
                        .toString()
                        .contains("Saved 5 records")
        );

        verify(ingestionService)
                .saveProcessedRecords(anyList(), eq("RECEIPT"));
    }

    @Test
    @DisplayName("Should redirect with error when session has expired")
    void saveFile_missingSessionData_redirectsWithError() {

        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------

        HttpSession session =
                new MockHttpSession();

        RedirectAttributes ra =
                new RedirectAttributesModelMap();

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------

        String view =
                ingestionController.saveFile(session, ra);

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------

        assertEquals("redirect:/ingestion/upload", view);

        assertEquals(
                "Session expired or file missing.",
                ra.getFlashAttributes().get("error")
        );
    }

    @Test
    @DisplayName("Should include duplicate and invalid counts in save message")
    void saveFile_duplicatesAndInvalidRecords_includesCounts() {

        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------

        HttpSession session = new MockHttpSession();
        session.setAttribute("pendingType", "RECEIPT");
        session.setAttribute("pendingRecords", List.of("record"));
        RedirectAttributes ra = new RedirectAttributesModelMap();
        when(ingestionService.saveProcessedRecords(anyList(), eq("RECEIPT")))
                .thenReturn(new IngestionService.SaveSummary(2, 3, 4));

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------

        ingestionController.saveFile(session, ra);

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------

        String message = ra.getFlashAttributes().get("message").toString();
        assertTrue(message.contains("Saved 2 records"));
        assertTrue(message.contains("Skipped 3 duplicate records"));
        assertTrue(message.contains("Skipped 4 invalid records"));
        assertNull(session.getAttribute("pendingRecords"));
        assertNull(session.getAttribute("pendingType"));
    }

    @Test
    @DisplayName("Should return save failure in flash attributes")
    void saveFile_serviceFailure_returnsError() {

        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------

        HttpSession session = new MockHttpSession();
        session.setAttribute("pendingType", "RECEIPT");
        session.setAttribute("pendingRecords", List.of("record"));
        RedirectAttributes ra = new RedirectAttributesModelMap();
        when(ingestionService.saveProcessedRecords(anyList(), eq("RECEIPT")))
                .thenThrow(new RuntimeException("Save failure"));

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------

        ingestionController.saveFile(session, ra);

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------

        assertEquals(
                "Save failed: Save failure",
                ra.getFlashAttributes().get("error")
        );
        assertNotNull(session.getAttribute("pendingRecords"));
    }

    // =========================================================================
    // handleFileUpload()
    // =========================================================================

    @Test
    @DisplayName("Should process CSV upload successfully")
    void handleFileUpload_success_returnsRedirect() throws Exception {

        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------

        MockMultipartFile file =
                new MockMultipartFile(
                        "file",
                        "records.csv",
                        "text/csv",
                        "data".getBytes(StandardCharsets.UTF_8)
                );

        RedirectAttributes ra =
                new RedirectAttributesModelMap();

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------

        String view =
                ingestionController.handleFileUpload(file, ra);

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------

        assertEquals("redirect:/ingestion/upload", view);

        verify(ingestionService)
                .processCSV(file);

        assertTrue(
                ra.getFlashAttributes()
                        .containsKey("message")
        );
    }

    @Test
    @DisplayName("Should return CSV processing failure in flash attributes")
    void handleFileUpload_serviceFailure_returnsError() throws Exception {

        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "records.csv",
                "text/csv",
                "data".getBytes(StandardCharsets.UTF_8)
        );
        RedirectAttributes ra = new RedirectAttributesModelMap();
        doThrow(new RuntimeException("CSV failure"))
                .when(ingestionService)
                .processCSV(file);

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------

        ingestionController.handleFileUpload(file, ra);

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------

        assertEquals(
                "Failed to process: CSV failure",
                ra.getFlashAttributes().get("error")
        );
    }

    // =========================================================================
    // handleMultipartFailure()
    // =========================================================================

    @Test
    @DisplayName("Should return upload page when multipart upload fails")
    void handleMultipartFailure_returnsUploadView() {

        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------

        Model model =
                new ConcurrentModel();

        Exception exception =
                new RuntimeException("Upload failed");

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------

        String view =
                ingestionController.handleMultipartFailure(
                        exception,
                        model);

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------

        assertEquals("ingestion/upload", view);

        assertTrue(
                model.getAttribute("error")
                        .toString()
                        .contains("Upload failed")
        );
    }
}
