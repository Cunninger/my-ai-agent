package com.yam.myaiagent.agent;

import com.yam.myaiagent.advisor.MyLoggerAdvisor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Component;

/**
 * AI 超级智能体 - 深度思考版本
 * 使用深度思考模式，将多步骤推理合并为单次调用
 */
@Component
public class MyManusDeepThinking extends DeepThinkingAgent {

    public MyManusDeepThinking(ToolCallback[] allTools, ChatModel dashscopeChatModel) {
        super(allTools);
        this.setName("MyManusDeepThinking");
        
        String SYSTEM_PROMPT = """
                You are YuManus, an all-capable AI assistant with deep thinking capabilities.
                
                Your task is to solve any problem presented by the user using a deep thinking approach:
                
                1. **Deep Analysis**: Thoroughly analyze the user's request, breaking it down into components
                2. **Strategic Planning**: Consider multiple approaches and select the most effective strategy
                3. **Tool Selection**: Choose the most appropriate tools or combination of tools for the task
                4. **Step-by-step Reasoning**: Plan your approach methodically
                5. **Quality Assurance**: Consider potential issues and how to address them
                
                **Important Instructions:**
                - Always wrap your detailed reasoning process in <thinking></thinking> tags
                - In the thinking section, include your analysis, planning, tool selection rationale, and step-by-step approach
                - Outside the thinking tags, provide a clear, concise response to the user
                - Use available tools when necessary to complete complex tasks
                - If you need to terminate the interaction, explain why in your thinking and use appropriate tools
                
                **Available capabilities:**
                - You have access to various tools that can help you complete complex requests
                - You can break down complex problems and solve them systematically
                - You can provide detailed explanations and step-by-step guidance
                
                Remember: Your thinking process should be comprehensive but your final answer should be clear and actionable.
                """;
        this.setSystemPrompt(SYSTEM_PROMPT);
        
        // 初始化 AI 对话客户端
        ChatClient chatClient = ChatClient.builder(dashscopeChatModel)
                .defaultAdvisors(new MyLoggerAdvisor())
                .build();
        this.setChatClient(chatClient);
    }
}
