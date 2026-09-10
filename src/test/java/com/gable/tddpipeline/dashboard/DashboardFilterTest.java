package com.gable.tddpipeline.dashboard;

import com.gable.tddpipeline.domain.*;
import com.gable.tddpipeline.repo.DealRepository;
import com.gable.tddpipeline.security.AppUserPrincipal;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class DashboardFilterTest {
    Deal deal(Department dept, int year, int month, int amount) {
        Deal d = new Deal(); d.setId((long)amount); d.setDepartment(dept); d.setDealStatus("PR"); d.setDealStage("PO"); d.setProbability("99% - 100%"); d.setSituation("Best Case");
        d.setCreatedDate(LocalDate.of(year,month,1)); d.setClosedDate(LocalDate.now().withDayOfMonth(1)); d.setAmount(BigDecimal.valueOf(amount)); return d;
    }
    @Test void createdYearQuarterApplyToCardsAndDepartmentSummary() {
        var repo = mock(DealRepository.class); var dept = new Department(); dept.setId(1L); dept.setCode("A");
        var u = new User(); u.setRole(Role.ADMIN);
        when(repo.findAll()).thenReturn(List.of(deal(dept,2026,1,100),deal(dept,2026,4,200),deal(dept,2025,1,400)));
        var result = new DashboardService(repo).summary(List.of(1L),null,null,List.of("99% - 100%"),List.of("PR"),2026,1,new AppUserPrincipal(u));
        var cards = (Map<?,?>)result.get("statCards");
        assertThat(cards.get("totalPipelineAmount")).isEqualTo(BigDecimal.valueOf(100));
        assertThat(cards.get("wonAmount")).isEqualTo(BigDecimal.valueOf(100));
        assertThat((List<?>)result.get("byYear")).hasSize(2);
        assertThat((List<?>)result.get("byDepartment")).hasSize(1);
    }
}
