package com.yam.myaiagent.controller;

import com.yam.myaiagent.agent.MyManus;
import com.yam.myaiagent.agent.MyManusDeepThinking;
import com.yam.myaiagent.app.LoveApp;
import com.yam.myaiagent.app.UIApp;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;


/**
 * 功能：
 * 日期：2025/5/29 16:31
 */
@RestController
@RequestMapping("/ai")
public class AiController {


    @Resource
    private LoveApp loveApp;

    @Resource
    private UIApp uiApp;


    @Resource
    private ToolCallback[] allTools;

    @Resource
    private ChatModel dashscopeChatModel;


    /**
     * SSE 流式调用 AI 恋爱大师应用
     *
     * @param message
     * @param chatId
     * @return
     */
    @GetMapping(value = "/love_app/chat/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> doChatWithLoveAppSSE(String message, String chatId) {
        return loveApp.doChatByStream(message, chatId);
    }

    /**
     * SSE 流式调用 UI 优化大师 应用
     *
     * @param message
     * @param chatId
     * @return
     */
    @GetMapping(value = "/ui/chat/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> doChatWithUISSE(String message, String chatId) {
        return uiApp.doChatByStream(message, chatId);
    }



    /**
     * SSE 流式调用 cook 大师 应用
     *
     * @param message
     * @param chatId
     * @return
     */
    @GetMapping(value = "/cook/chat/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> doChatWithCookSSE(String message, String chatId) {
        return uiApp.doChatByStream(message, chatId);
    }


    /**
     * SSE 流式调用 AI 恋爱大师应用
     *
     * @param message
     * @param chatId
     * @return
     */
    @GetMapping(value = "/love_app/chat/server_sent_event")
    public Flux<ServerSentEvent<String>> doChatWithLoveAppServerSentEvent(String message, String chatId) {
        return loveApp.doChatByStream(message, chatId)
                .map(chunk -> ServerSentEvent.<String>builder()
                        .data(chunk)
                        .build());
    }


    /**
     * SSE 流式调用 AI 恋爱大师应用
     *
     * @param message
     * @param chatId
     * @return
     */
    @GetMapping(value = "/love_app/chat/sse_emitter")
    public SseEmitter doChatWithLoveAppServerSseEmitter(String message, String chatId) {
        // 创建一个超时时间较长的 SseEmitter
        SseEmitter sseEmitter = new SseEmitter(180000L); // 3 分钟超时
        // 获取 Flux 响应式数据流并且直接通过订阅推送给 SseEmitter
        loveApp.doChatByStream(message, chatId)
                .subscribe(chunk -> {
                    try {
                        sseEmitter.send(chunk);
                    } catch (IOException e) {
                        sseEmitter.completeWithError(e);
                    }
                }, sseEmitter::completeWithError, sseEmitter::complete);
        // 返回
        return sseEmitter;
    }

    /**
     * 流式调用 Manus 超级智能体（原版多步骤模式）
     * @param message
     * @return
     */
    @GetMapping("/manus/chat")
    public SseEmitter doChatWithManus(String message) {
        MyManus yuManus = new MyManus(allTools, dashscopeChatModel);
        return yuManus.runStream(message);
    }

    /**
     * 流式调用 Manus 超级智能体（深度思考模式）
     * @param message
     * @return
     */
    @GetMapping("/manus/chat/deep-thinking")
    public SseEmitter doChatWithManusDeepThinking(String message) {
        MyManusDeepThinking yuManusDeepThinking = new MyManusDeepThinking(allTools, dashscopeChatModel);
        return yuManusDeepThinking.runStream(message);
    }


}