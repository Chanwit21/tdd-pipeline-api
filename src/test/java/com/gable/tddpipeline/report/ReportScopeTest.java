package com.gable.tddpipeline.report;

import com.gable.tddpipeline.domain.*;
import com.gable.tddpipeline.masterconfig.MasterConfigService;
import com.gable.tddpipeline.repo.DealRepository;
import com.gable.tddpipeline.security.AppUserPrincipal;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class ReportScopeTest {
    Department dept(long id, String code) { Department d = new Department(); d.setId(id); d.setCode(code); return d; }
    Deal deal(Department dept, int amount) { Deal d = new Deal(); d.setDepartment(dept); d.setDealStatus("PR"); d.setClosedDate(LocalDate.of(2026,1,1)); d.setAmount(BigDecimal.valueOf(amount)); return d; }
    AppUserPrincipal principal(Role role, Department dept) { User u = new User(); u.setRole(role); u.setDepartment(dept); return new AppUserPrincipal(u); }
    @Test void selectedDepartmentsControlRowsAndTotalsAndManagerCannotWidenScope() {
        var repo = mock(DealRepository.class); var master = mock(MasterConfigService.class);
        var a = dept(1,"A"); var b = dept(2,"B"); var c = dept(3,"C");
        when(master.departments()).thenReturn(List.of(a,b,c));
        when(repo.findAll()).thenReturn(List.of(deal(a,100), deal(b,200), deal(c,400)));
        var service = new ReportService(repo, master);
        var admin = service.prByTeam(List.of(1L,2L),2026,principal(Role.ADMIN,null));
        assertThat(admin.get("grandTotal")).isEqualTo(BigDecimal.valueOf(300));
        assertThat((List<?>)admin.get("rows")).hasSize(2);
        var manager = service.prByTeam(List.of(2L,3L),2026,principal(Role.MANAGER,a));
        assertThat(manager.get("grandTotal")).isEqualTo(BigDecimal.valueOf(100));
        assertThat((List<?>)manager.get("rows")).hasSize(1);
        var none = service.prByTeam(List.of(-1L),2026,principal(Role.ADMIN,null));
        assertThat((List<?>)none.get("rows")).isEmpty();
        assertThat(none.get("grandTotal")).isEqualTo(BigDecimal.ZERO);
    }
}
