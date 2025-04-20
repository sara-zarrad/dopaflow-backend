package crm.dopaflow_backend.Controller;

import crm.dopaflow_backend.DTO.KeyIndicatorsDTO;
import crm.dopaflow_backend.DTO.ReportDTO;
import crm.dopaflow_backend.Service.ReportingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.ArrayList;

@RestController
@RequestMapping("/api/reporting")
@RequiredArgsConstructor
public class ReportingController {

    private final ReportingService reportingService;

    @GetMapping
    public ResponseEntity<ReportDTO> getReport() {
        try {
            ReportDTO report = reportingService.generateReport();
            return ResponseEntity.ok(report);
        } catch (SecurityException e) {
            // Handle authentication/authorization errors (e.g., user not logged in)
            ReportDTO errorResponse = createEmptyReportDTO();
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
        } catch (Exception e) {
            // Handle any other unexpected errors
            ReportDTO errorResponse = createEmptyReportDTO();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    private ReportDTO createEmptyReportDTO() {
        ReportDTO errorResponse = new ReportDTO();

        // Initialize keyIndicators with zero values
        KeyIndicatorsDTO errorIndicators = new KeyIndicatorsDTO();
        errorIndicators.setTotalOpportunityValue(BigDecimal.ZERO);
        errorIndicators.setNewOpportunities(0L);
        errorIndicators.setCompletedTasks(0L);
        errorIndicators.setCustomerSatisfaction(0.0);
        errorIndicators.setNewCompanies(0L);
        errorIndicators.setNewContacts(0L);
        errorResponse.setKeyIndicators(errorIndicators);

        // Initialize empty lists for salesEvolution and salesPerformance
        errorResponse.setSalesEvolution(new ArrayList<>());
        errorResponse.setSalesPerformance(new ArrayList<>());

        return errorResponse;
    }
}