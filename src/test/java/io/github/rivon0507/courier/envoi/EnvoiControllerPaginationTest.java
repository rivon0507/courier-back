package io.github.rivon0507.courier.envoi;

import io.github.rivon0507.courier.common.pagination.PageInfo;
import io.github.rivon0507.courier.common.pagination.PagedResponse;
import io.github.rivon0507.courier.common.pagination.SortInfo;
import io.github.rivon0507.courier.envoi.api.EnvoiResponse;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = io.github.rivon0507.courier.envoi.EnvoiController.class)
@AutoConfigureMockMvc(addFilters = false)
class EnvoiControllerPaginationSliceTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    EnvoiService envoiService;

    @Test
    void noPageNumber_defaultsToPage0() throws Exception {
        long workspaceId = 1L;

        when(envoiService.getPage(any(Pageable.class), eq(workspaceId), any()))
                .thenReturn(null);

        mockMvc.perform(get("/workspaces/1/envois", workspaceId))
                .andExpect(status().isOk());

        Pageable pageable = capturePageable(workspaceId);
        assertThat(pageable.getPageNumber()).isEqualTo(0);
    }

    @Test
    void noPageSize_defaultsToSize10() throws Exception {
        long workspaceId = 1L;

        when(envoiService.getPage(any(Pageable.class), eq(workspaceId), any()))
                .thenReturn(null);

        mockMvc.perform(get("/workspaces/1/envois", workspaceId))
                .andExpect(status().isOk());

        Pageable pageable = capturePageable(workspaceId);
        assertThat(pageable.getPageSize()).isEqualTo(10);
    }

    @Test
    void noSortKey_defaultsToSortByDateEnvoi() throws Exception {
        long workspaceId = 1L;

        when(envoiService.getPage(any(Pageable.class), eq(workspaceId), any()))
                .thenReturn(null);

        mockMvc.perform(get("/workspaces/1/envois", workspaceId))
                .andExpect(status().isOk());

        Pageable pageable = capturePageable(workspaceId);

        Sort.Order order = pageable.getSort().getOrderFor("dateEnvoi");
        assertThat(order).isNotNull();
    }

    @Test
    void noSortDirection_defaultsToAscending() throws Exception {
        long workspaceId = 1L;

        when(envoiService.getPage(any(Pageable.class), eq(workspaceId), any()))
                .thenReturn(null);

        mockMvc.perform(get("/workspaces/1/envois", workspaceId))
                .andExpect(status().isOk());

        Pageable pageable = capturePageable(workspaceId);

        Sort.Order order = pageable.getSort().getOrderFor("dateEnvoi");
        assertThat(order).isNotNull();
        assertThat(order.getDirection()).isEqualTo(Sort.Direction.ASC);
    }

    @Test
    void getPage_returnsPagedResponseShape_withItems() throws Exception {
        long workspaceId = 1L;

        when(envoiService.getPage(any(Pageable.class), eq(workspaceId), any())).thenAnswer(invocation -> {
            Pageable p = invocation.getArgument(0, Pageable.class);
            Sort.Order order = p.getSort().stream().findFirst().orElseThrow();

            return new PagedResponse<>(
                    List.of(new EnvoiResponse(1L, "REF", "dest", null, LocalDate.of(2026, 2, 10))),
                    new PageInfo(p.getPageNumber(), p.getPageSize(), 0, 0),
                    new SortInfo(order)
            );
        });

        mockMvc.perform(get("/workspaces/1/envois", workspaceId))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$._items").isArray())
                .andExpect(jsonPath("$._page.pageIndex").value(0))
                .andExpect(jsonPath("$._page.pageSize").value(10))
                .andExpect(jsonPath("$._sort.key").value("dateEnvoi"))
                .andExpect(jsonPath("$._sort.direction").value("ASC"));
    }


    private Pageable capturePageable(long workspaceId) {
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(envoiService).getPage(captor.capture(), eq(workspaceId), any());
        return captor.getValue();
    }
}
