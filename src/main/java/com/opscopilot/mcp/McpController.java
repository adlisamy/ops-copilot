package com.opscopilot.mcp;

import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

@Component
public class McpController {
    private final McpService mcpService;
    public McpController(McpService mcpService){
        this.mcpService = mcpService;
  }

   @McpTool(description = "Adds two integers")
   public int ask(
           @McpToolParam(description = "The first integer", required = true) int a,
           @McpToolParam(description = "The second integer", required = true) int b
  ){
    return mcpService.add(a, b);
  }
}
