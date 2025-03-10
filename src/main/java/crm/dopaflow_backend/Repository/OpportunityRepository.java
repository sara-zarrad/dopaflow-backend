package crm.dopaflow_backend.Repository;

import crm.dopaflow_backend.Model.Opportunity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OpportunityRepository extends JpaRepository<Opportunity, Long> {
}
