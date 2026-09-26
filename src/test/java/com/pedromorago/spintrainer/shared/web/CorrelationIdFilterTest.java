package com.pedromorago.spintrainer.shared.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class CorrelationIdFilterTest {

    final CorrelationIdFilter filter = new CorrelationIdFilter();
    final AtomicReference<String> seenInMdc = new AtomicReference<>();

    @Test
    void propagatesAValidIncomingIdToLogsAndResponse() throws Exception {
        MockHttpServletResponse response = run("web-3f2a.9_b");

        assertThat(response.getHeader(CorrelationId.HEADER)).isEqualTo("web-3f2a.9_b");
        assertThat(seenInMdc).hasValue("web-3f2a.9_b");
    }

    @Test
    void generatesOneWhenMissing() throws Exception {
        MockHttpServletResponse response = run(null);

        assertThat(response.getHeader(CorrelationId.HEADER)).matches("[0-9a-f-]{36}");
        assertThat(seenInMdc).hasValue(response.getHeader(CorrelationId.HEADER));
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "",
                "con espacios",
                "x\r\nSet-Cookie: a=b",
                "<script>",
                "0123456789012345678901234567890123456789012345678901234567890123456789"
            })
    void replacesUnsafeOrOversizedValues(String incoming) throws Exception {
        MockHttpServletResponse response = run(incoming);

        assertThat(response.getHeader(CorrelationId.HEADER))
                .isNotEqualTo(incoming)
                .matches("[0-9a-f-]{36}");
    }

    @Test
    void cleansTheMdcAfterTheRequest() throws Exception {
        run("abc");

        assertThat(MDC.get(CorrelationId.MDC_KEY)).isNull();
    }

    private MockHttpServletResponse run(String incoming) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/situations");
        if (incoming != null) {
            request.addHeader(CorrelationId.HEADER, incoming);
        }
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain() {
            @Override
            public void doFilter(jakarta.servlet.ServletRequest req, jakarta.servlet.ServletResponse res) {
                seenInMdc.set(MDC.get(CorrelationId.MDC_KEY));
            }
        });
        return response;
    }
}
