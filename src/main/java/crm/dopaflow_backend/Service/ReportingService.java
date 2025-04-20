package crm.dopaflow_backend.Service;

import crm.dopaflow_backend.DTO.KeyIndicatorsDTO;
import crm.dopaflow_backend.DTO.ReportDTO;
import crm.dopaflow_backend.DTO.SalesEvolutionDTO;
import crm.dopaflow_backend.DTO.SalesPerformanceDTO;
import crm.dopaflow_backend.Model.Opportunity;
import crm.dopaflow_backend.Model.StatutOpportunity;
import crm.dopaflow_backend.Model.StatutTask;
import crm.dopaflow_backend.Model.Task;
import crm.dopaflow_backend.Model.User;
import crm.dopaflow_backend.Repository.CompanyRepository;
import crm.dopaflow_backend.Repository.ContactRepository;
import crm.dopaflow_backend.Repository.OpportunityRepository;
import crm.dopaflow_backend.Repository.TaskRepository;
import crm.dopaflow_backend.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReportingService {

    private static final Logger logger = LoggerFactory.getLogger(ReportingService.class);

    private final OpportunityRepository opportunityRepository;
    private final ContactRepository contactRepository;
    private final CompanyRepository companyRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public ReportDTO generateReport() {
        logger.info("Generating report...");
        long startTime = System.currentTimeMillis();
        ReportDTO report = new ReportDTO();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime sixMonthsAgo = now.minus(6, ChronoUnit.MONTHS);
        Date sixMonthsAgoDate = Date.from(sixMonthsAgo.atZone(ZoneId.systemDefault()).toInstant());

        // Get current user (for authentication purposes only)
        User currentUser = getCurrentUser();
        logger.info("Current user: {}", currentUser.getEmail());

        // --- KPIs ---
        KeyIndicatorsDTO keyIndicators = new KeyIndicatorsDTO();

        // 1. Total value of WON opportunities
        long queryStart = System.currentTimeMillis();
        BigDecimal totalOpportunityValue = BigDecimal.valueOf(opportunityRepository.getTotalOpportunityValue(StatutOpportunity.WON));
        keyIndicators.setTotalOpportunityValue(totalOpportunityValue != null ? totalOpportunityValue : BigDecimal.ZERO);
        logger.info("getTotalOpportunityValue took {} ms", System.currentTimeMillis() - queryStart);

        // 2. Count of new opportunities in the last month
        queryStart = System.currentTimeMillis();
        LocalDateTime oneMonthAgo = now.minus(1, ChronoUnit.MONTHS);
        long newOpportunities = opportunityRepository.countOpenOpportunities(StatutOpportunity.IN_PROGRESS);
        keyIndicators.setNewOpportunities(newOpportunities);
        logger.info("countOpenOpportunities took {} ms", System.currentTimeMillis() - queryStart);

        // 3. Total completed tasks (global for all roles)
        queryStart = System.currentTimeMillis();
        long completedTasks = taskRepository.countCompletedTasks(StatutTask.Done);
        keyIndicators.setCompletedTasks(completedTasks);
        logger.info("countCompletedTasks took {} ms", System.currentTimeMillis() - queryStart);

        // 4. Customer satisfaction (mocked as 92% since not in database)
        keyIndicators.setCustomerSatisfaction(92.0); // Placeholder

        // 5. New companies in the last month
        queryStart = System.currentTimeMillis();
        long newCompanies = companyRepository.countNewCompaniesSince(oneMonthAgo);
        keyIndicators.setNewCompanies(newCompanies);
        logger.info("countNewCompaniesSince took {} ms", System.currentTimeMillis() - queryStart);

        // 6. New contacts in the last month
        queryStart = System.currentTimeMillis();
        long newContacts = contactRepository.countNewContactsSince(oneMonthAgo);
        keyIndicators.setNewContacts(newContacts);
        logger.info("countNewContactsSince took {} ms", System.currentTimeMillis() - queryStart);

        report.setKeyIndicators(keyIndicators);

        // --- Evolution Chart (Last 6 Months) ---
        // Line 1: Number of completed tasks per month
        // Line 2: Total value of WON opportunities per month
        queryStart = System.currentTimeMillis();
        List<Task> completedTasksList = taskRepository.findCompletedTasksSince(StatutTask.Done, sixMonthsAgoDate);
        List<Opportunity> wonOpportunities = opportunityRepository.findWonOpportunitiesSince(StatutOpportunity.WON, sixMonthsAgo);
        logger.info("findCompletedTasksSince and findWonOpportunitiesSince took {} ms", System.currentTimeMillis() - queryStart);

        // Initialize chart data for the last 6 months
        List<SalesEvolutionDTO> salesEvolutionData = new ArrayList<>();
        for (int i = 5; i >= 0; i--) {
            LocalDateTime monthStart = now.minus(i, ChronoUnit.MONTHS).withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
            SalesEvolutionDTO monthData = new SalesEvolutionDTO();
            monthData.setMonth(monthStart.getMonth().toString().substring(0, 3)); // e.g., "JAN", "FEB"
            monthData.setCompletedTasks(0L); // Number of completed tasks
            monthData.setOpportunityValue(BigDecimal.ZERO); // Total value of WON opportunities
            salesEvolutionData.add(monthData);
        }

        // Populate completed tasks
        for (Task task : completedTasksList) {
            if (task.getDeadline() == null) {
                continue;
            }
            LocalDateTime deadline = LocalDateTime.ofInstant(task.getDeadline().toInstant(), ZoneId.systemDefault());
            String monthKey = deadline.getMonth().toString().substring(0, 3);
            salesEvolutionData.stream()
                    .filter(data -> data.getMonth().equals(monthKey))
                    .findFirst()
                    .ifPresent(data -> data.setCompletedTasks(data.getCompletedTasks() + 1));
        }

        // Populate WON opportunities value
        for (Opportunity op : wonOpportunities) {
            String monthKey = op.getCreatedAt().getMonth().toString().substring(0, 3);
            salesEvolutionData.stream()
                    .filter(data -> data.getMonth().equals(monthKey))
                    .findFirst()
                    .ifPresent(data -> {
                        BigDecimal currentValue = data.getOpportunityValue();
                        BigDecimal opValue = op.getValue() != null ? op.getValue() : BigDecimal.ZERO;
                        data.setOpportunityValue(currentValue.add(opValue));
                    });
        }
        report.setSalesEvolution(salesEvolutionData);

        // --- Performance Table ---
        // Show performance for all users (visible to all roles)
        List<SalesPerformanceDTO> salesPerformance = new ArrayList<>();
        List<User> usersToReport = userRepository.findAll();

        for (User user : usersToReport) {
            queryStart = System.currentTimeMillis();
            List<Opportunity> userWonOpportunities = opportunityRepository.findWonOpportunitiesSinceForUser(
                    StatutOpportunity.WON, sixMonthsAgo, user.getId());
            logger.info("findWonOpportunitiesSinceForUser for user {} took {} ms", user.getEmail(), System.currentTimeMillis() - queryStart);

            BigDecimal achieved = userWonOpportunities.stream()
                    .map(op -> op.getValue() != null ? op.getValue() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal target = new BigDecimal("15000.0"); // Mocked target

            SalesPerformanceDTO userPerformance = new SalesPerformanceDTO();
            userPerformance.setName(user.getUsername() != null ? user.getUsername() : user.getEmail());
            userPerformance.setTarget(target);
            userPerformance.setAchieved(achieved);
            // Calculate progress as a percentage: (achieved / target) * 100
            double progress = target.compareTo(BigDecimal.ZERO) != 0
                    ? achieved.divide(target, 4, BigDecimal.ROUND_HALF_UP)
                    .multiply(new BigDecimal("100"))
                    .doubleValue()
                    : 0.0;
            userPerformance.setProgress(progress);
            salesPerformance.add(userPerformance);
        }
        report.setSalesPerformance(salesPerformance);

        logger.info("Report generated successfully in {} ms", System.currentTimeMillis() - startTime);
        return report;
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            logger.error("No authenticated user found");
            throw new SecurityException("No authenticated user found. Please log in.");
        }
        String email = authentication.getName();
        logger.debug("Fetching user by email: {}", email);
        return userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    logger.error("User not found in database: {}", email);
                    return new RuntimeException("Current user not found in database: " + email);
                });
    }
}