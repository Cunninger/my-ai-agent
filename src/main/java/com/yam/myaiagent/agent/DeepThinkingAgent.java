package com.yam.myaiagent.agent;

import cn.hutool.core.util.StrUtil;
import com.yam.myaiagent.agent.model.AgentState;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 深度思考代理类，实现类似DeepSeek的深度思考模式
 * 将多步骤推理合并为单次AI调用，使用thinking标签包裹推理过程
 */
@Data
@Slf4j
public class DeepThinkingAgent {

    // 核心属性
    private String name;
    private String systemPrompt;
    private AgentState state = AgentState.IDLE;
    
    // LLM 大模型
    private ChatClient chatClient;
    
    // Memory 记忆
    private List<Message> messageList = new ArrayList<>();
    
    // 可用的工具
    private ToolCallback[] availableTools;

    public DeepThinkingAgent(ToolCallback[] availableTools) {
        this.availableTools = availableTools;
    }

    /**
     * 运行代理（深度思考模式）
     *
     * @param userPrompt 用户提示词
     * @return 执行结果
     */
    public String run(String userPrompt) {
        // 1、基础校验
        if (this.state != AgentState.IDLE) {
            throw new RuntimeException("Cannot run agent from state: " + this.state);
        }
        if (StrUtil.isBlank(userPrompt)) {
            throw new RuntimeException("Cannot run agent with empty user prompt");
        }

        // 2、执行深度思考
        this.state = AgentState.RUNNING;
        messageList.add(new UserMessage(userPrompt));

        try {
            // 构建深度思考提示词
            String deepThinkingPrompt = buildDeepThinkingPrompt(userPrompt);
            
            // 单次AI调用，包含完整推理过程
            ChatResponse chatResponse = chatClient
                    .prompt()
                    .user(deepThinkingPrompt)
                    .system(systemPrompt)
                    .tools(availableTools)
                    .call()
                    .chatResponse();

            AssistantMessage assistantMessage = chatResponse.getResult().getOutput();
            String result = assistantMessage.getText();
            
            // 记录消息上下文
            messageList.add(assistantMessage);
            
            this.state = AgentState.FINISHED;
            log.info("Deep thinking completed: {}", result);

            return result;
            
        } catch (Exception e) {
            state = AgentState.ERROR;
            log.error("Error executing deep thinking agent", e);
            return "执行错误：" + e.getMessage();
        } finally {
            this.cleanup();
        }
    }

    /**
     * 运行代理（流式输出）
     *
     * @param userPrompt 用户提示词
     * @return SSE发射器
     */
    public SseEmitter runStream(String userPrompt) {
        SseEmitter sseEmitter = new SseEmitter(300000L); // 5分钟超时
        
        CompletableFuture.runAsync(() -> {
            try {
                // 基础校验
                if (this.state != AgentState.IDLE) {
                    sseEmitter.send("错误：无法从状态运行代理：" + this.state);
                    sseEmitter.complete();
                    return;
                }
                if (StrUtil.isBlank(userPrompt)) {
                    sseEmitter.send("错误：不能使用空提示词运行代理");
                    sseEmitter.complete();
                    return;
                }

                // 执行深度思考
                this.state = AgentState.RUNNING;
                messageList.add(new UserMessage(userPrompt));

                // 构建深度思考提示词
                String deepThinkingPrompt = buildDeepThinkingPrompt(userPrompt);
                
                // 单次AI调用，包含完整推理过程
                ChatResponse chatResponse = chatClient
                        .prompt()
                        .user(deepThinkingPrompt)
                        .system(systemPrompt)
                        .tools(availableTools)
                        .call()
                        .chatResponse();

                AssistantMessage assistantMessage = chatResponse.getResult().getOutput();
                String result = assistantMessage.getText();
                
                // 记录消息上下文
                messageList.add(assistantMessage);
                
                // 流式发送结果
                sseEmitter.send(result);
                
                this.state = AgentState.FINISHED;
                sseEmitter.complete();
                
            } catch (Exception e) {
                state = AgentState.ERROR;
                log.error("Error executing deep thinking agent", e);
                try {
                    sseEmitter.send("执行错误：" + e.getMessage());
                    sseEmitter.complete();
                } catch (IOException ex) {
                    sseEmitter.completeWithError(ex);
                }
            } finally {
                this.cleanup();
            }
        });

        // 设置超时和完成回调
        sseEmitter.onTimeout(() -> {
            this.state = AgentState.ERROR;
            this.cleanup();
            log.warn("SSE connection timeout");
        });
        
        sseEmitter.onCompletion(() -> {
            if (this.state == AgentState.RUNNING) {
                this.state = AgentState.FINISHED;
            }
            this.cleanup();
            log.info("SSE connection completed");
        });
        
        return sseEmitter;
    }

    /**
     * 构建深度思考提示词
     */
    private String buildDeepThinkingPrompt(String userPrompt) {
        return String.format("""
                请使用深度思考模式回答用户问题。你需要：
                
                1. 将你的完整推理过程包裹在<thinking></thinking>标签内
                2. 在thinking标签内，详细分析问题、考虑各种可能性、制定解决方案
                3. 在thinking标签外，提供简洁明确的最终回答
                
                用户问题：%s
                
                请按照以下格式回答：
                
                <thinking>
                [在这里进行详细的推理分析，包括：
                - 问题理解和分解
                - 可能的解决方案分析
                - 工具选择和使用策略
                - 步骤规划等]
                </thinking>
                
                [在这里提供最终的简洁回答]
                """, userPrompt);
    }

    /**
     * 清理资源
     */
    protected void cleanup() {
        // 清理资源的逻辑
    }
}
