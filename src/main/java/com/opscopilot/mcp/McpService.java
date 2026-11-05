package com.opscopilot.mcp;

import org.springframework.stereotype.Service;

@Service
public class McpService implements McpServerOperations {
    @Override
    public int add(int a, int b) {
        return a+b;
    }
}
