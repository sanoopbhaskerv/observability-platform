package com.yourorg.observability.starter.core;

import com.yourorg.observability.contract.ObsMdcKeys;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class CorrelationIdFilterTest {

    private final CorrelationIdFilter filter = new CorrelationIdFilter("X-Correlation-Id");

    @AfterEach
    void cleanUp() {
        MDC.clear();
    }

    @Test
    void extractsCorrelationIdFromRequestHeader() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Correlation-Id", "abc-123");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        // Capture MDC value during doFilter
        doAnswer(invocation -> {
            assertThat(MDC.get(ObsMdcKeys.CORRELATION_ID)).isEqualTo("abc-123");
            return null;
        }).when(chain).doFilter(request, response);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        assertThat(response.getHeader("X-Correlation-Id")).isEqualTo("abc-123");
    }

    @Test
    void generatesUuidWhenNoHeaderPresent() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        doAnswer(invocation -> {
            String generated = MDC.get(ObsMdcKeys.CORRELATION_ID);
            assertThat(generated).isNotNull().isNotEmpty();
            // Should be a valid UUID format
            assertThat(generated).matches("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$");
            return null;
        }).when(chain).doFilter(request, response);

        filter.doFilter(request, response, chain);

        // Response should also get the generated ID
        assertThat(response.getHeader("X-Correlation-Id")).isNotNull();
    }

    @Test
    void cleansUpMdcAfterRequest() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Correlation-Id", "abc-123");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        // MDC should be clean after the filter completes
        assertThat(MDC.get(ObsMdcKeys.CORRELATION_ID)).isNull();
    }

    @Test
    void cleansUpMdcEvenOnException() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Correlation-Id", "abc-123");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);
        doThrow(new RuntimeException("boom")).when(chain).doFilter(request, response);

        try {
            filter.doFilter(request, response, chain);
        } catch (RuntimeException ignored) {
        }

        // MDC must be clean even after exception
        assertThat(MDC.get(ObsMdcKeys.CORRELATION_ID)).isNull();
    }
}
