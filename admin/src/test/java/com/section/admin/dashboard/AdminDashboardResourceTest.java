package com.section.admin.dashboard;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class AdminDashboardResourceTest {

    @Test
    void dashboardKeepsPinnedDeferredChartAndLifecycleContract() throws IOException {
        String html = readResource("templates/views/dashboard.html");
        String script = readResource("static/js/view/dashboard/dashboard-list.js");

        assertThat(html)
                .contains("/vendor/chart.js/4.5.1/chart.umd.min.js")
                .doesNotContain("src=\"https://cdn.jsdelivr.net/npm/chart.js\"");
        assertThat(html.indexOf("/vendor/chart.js/4.5.1/chart.umd.min.js"))
                .isGreaterThan(html.indexOf("</main>"))
                .isLessThan(html.indexOf("/js/view/dashboard/dashboard-list.js"));
        assertThat(script)
                .contains("charts: new Map()")
                .contains("this.replaceChart('sales'")
                .contains("this.replaceChart('top-products'")
                .contains("this.replaceChart('top-brands'")
                .contains("this.charts.get(key)?.destroy()")
                .contains("this.charts.set(key, chart)")
                .contains("typeof Chart !== 'function'");
    }

    private String readResource(String path) throws IOException {
        ClassPathResource resource = new ClassPathResource(path);
        return resource.getContentAsString(StandardCharsets.UTF_8);
    }
}
