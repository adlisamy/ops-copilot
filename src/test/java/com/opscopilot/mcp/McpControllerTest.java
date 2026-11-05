package com.opscopilot.mcp;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class McpServiceTest {

    private final McpService mcpService = new McpService();

    @Test
    void shouldReturnTheSumOfTwoIntegers() {
        int result = mcpService.add(2, 3);

        assertThat(result).isEqualTo(5);
    }

    @Test
    void shouldHandleNegativeNumbers() {
        assertThat(mcpService.add(-2, 3)).isEqualTo(1);
    }
}