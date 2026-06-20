package com.demo.mcp.onboarding;

import io.modelcontextprotocol.spec.McpSchema;
import java.util.List;
import org.springaicommunity.mcp.annotation.McpArg;
import org.springaicommunity.mcp.annotation.McpPrompt;
import org.springframework.stereotype.Component;

@Component
public class OnboardingPrompts {

    @McpPrompt(
            name = "onboarding_walkthrough",
            description = "Start a guided walkthrough of the RideFlex project for a new joiner.")
    public McpSchema.GetPromptResult onboardingWalkthrough(
            @McpArg(
                    name = "focusArea",
                    description = "Optional focus area (e.g., architecture, modules, flows, requirements)",
                    required = false)
                    String focusArea) {

        StringBuilder instructions = new StringBuilder();
        instructions.append("You are helping a new joiner learn the fictional RideFlex project from local markdown documentation exposed via MCP.\n\n");
        instructions.append("First, call the list_topics tool so you know the exact document ids.\n\n");
        instructions.append(
                "Then walk the person through the documentation in this order, reading full documents via the docs://{docId} resource as needed:\n");
        instructions.append("1) overview (doc id: overview)\n");
        instructions.append("2) high-level architecture (doc id: high-level-architecture)\n");
        instructions.append("3) modules (doc id: modules)\n");
        instructions.append("4) key flows (doc id: flows)\n\n");
        instructions.append("After each major section, pause and ask whether they have questions before continuing.\n");
        instructions.append("If they seem lost, use search_docs to find relevant excerpts.\n\n");

        if (focusArea != null && !focusArea.isBlank()) {
            instructions.append("The joiner asked to focus on: ")
                    .append(focusArea.strip())
                    .append(
                            ". Spend extra time on that theme: pull additional excerpts with search_docs and read related docs (for example requirements, features, low-level-architecture, tech-stack, glossary) when helpful.\n");
        } else {
            instructions.append("No specific focus was provided—cover the end-to-end story at a sensible depth for a first day.\n");
        }

        return new McpSchema.GetPromptResult(
                "RideFlex onboarding walkthrough",
                List.of(new McpSchema.PromptMessage(
                        McpSchema.Role.USER, new McpSchema.TextContent(instructions.toString()))));
    }
}
