package org.example.springailearning;

import org.springaicommunity.agent.tools.FileSystemTools;
import org.springaicommunity.agent.tools.ShellTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.ai.session.DefaultSessionService;
import org.springframework.ai.session.InMemorySessionRepository;
import org.springframework.ai.session.SessionService;
import org.springframework.ai.session.advisor.SessionMemoryAdvisor;
import org.springframework.ai.session.compaction.RecursiveSummarizationCompactionStrategy;
import org.springframework.ai.session.compaction.SlidingWindowCompactionStrategy;
import org.springframework.ai.session.compaction.TurnCountTrigger;
import org.springframework.ai.session.compaction.TurnWindowCompactionStrategy;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import reactor.core.publisher.Flux;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

@SpringBootApplication
public class SpringaiLearningApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringaiLearningApplication.class, args);
    }

    /*@Bean
    CommandLineRunner commandLineRunner(ChatClient.Builder chatClientBuilder) {  // 内部选择模型，只能是唯一模型
        return args -> {
            System.out.println("\n我是吉娜:.\n");
            try (Scanner scanner = new Scanner(System.in)) {
                while(true) {
                    System.out.print("你:");
                    String message = scanner.nextLine();
                    System.out.println("AI:" + message);
                }
            }
        };
    }*/

    /*@Bean
    CommandLineRunner commandLineRunner(DeepSeekChatModel deepSeekChatModel) {
        return args -> {
            System.out.println("\n我是吉娜:.\n");
            // 由spring构建的动态代理对话模型
            // 对话代理：记忆/advisor大模型拦截器/结构化输出
            ChatClient chatClient = ChatClient.builder(deepSeekChatModel).build();

            try (Scanner scanner = new Scanner(System.in)) {
                while(true) {
                    System.out.print("你:");
                    String message = scanner.nextLine();
                    String content = chatClient.prompt().user(message).call().content();//同步http方式一次响应结果,体验不好
                    System.out.println("AI:" + content);
                }
            }
        };
    }*/

    /*@Bean
    CommandLineRunner commandLineRunner(DeepSeekChatModel deepSeekChatModel) {
        return args -> {
            System.out.println("\n我是吉娜:.\n");
            // 由spring构建的动态代理对话模型
            // 对话代理：记忆/advisor大模型拦截器/结构化输出
            ChatClient chatClient = ChatClient.builder(deepSeekChatModel).build();

            try (Scanner scanner = new Scanner(System.in)) {
                while(true) {
                    System.out.print("\n你:");
                    String message = scanner.nextLine();
                    System.out.println("\nAI:");
                    // 流式调用，服务器调用大模型后，大模型将返回的数据不断地输出给控制台
                    Flux<String> content = chatClient.prompt().user(message).stream().content();
                    // 阻塞线程
                    content.doOnNext(System.out::print).blockLast();
                }
            }
        };
    }*/

    /*@Bean
    CommandLineRunner commandLineRunner(DeepSeekChatModel deepSeekChatModel, ChatMemory chatMemory) { //chatMemory 聊天记忆功能
        return args -> {
            System.out.print("\n我是吉娜:.\n");
            // 由spring构建的动态代理对话模型
            // 对话代理：记忆/advisor大模型拦截器/结构化输出
            ChatClient chatClient = ChatClient.builder(deepSeekChatModel)
                    .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                    .build();

            try (Scanner scanner = new Scanner(System.in)) {
                while (true) {
                    System.out.print("\n你:");
                    String message = scanner.nextLine();
                    System.out.print("\nAI:");
                    // 流式调用，服务器调用大模型后，大模型将返回的数据不断地输出给控制台
                    Flux<String> content = chatClient.prompt()
                            .user(message)
                            // spring ai 2.0 取消了给map传key的默认值，需要手动传，不然报非法算术异常
                            .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, "Gina"))
                            .stream()
                            .content();
                    // 阻塞线程
                    content.doOnNext(System.out::print).blockLast();
                    System.out.print("\n");
                }
            }
        };
    }*/

    /**
     * 压缩策略：SlidingWindowCompactionStrategy 按消息数策略
     *          TurnWindowCompactionStrategy 按轮数策略
     * @param deepSeekChatModel
     * @return
     */
    /*@Bean
    SessionMemoryAdvisor sessionMemoryAdvisor(DeepSeekChatModel deepSeekChatModel) {
        SessionService sessionService = DefaultSessionService.builder()
                .sessionRepository(InMemorySessionRepository.builder().build()).build();
        return SessionMemoryAdvisor.builder(sessionService)
                .defaultUserId("Gina")
                .compactionTrigger(new TurnCountTrigger(1))
//                .compactionStrategy(SlidingWindowCompactionStrategy.builder().maxEvents(1)
                .compactionStrategy(TurnWindowCompactionStrategy.builder().maxTurns(1).build())
                .build();
    }*/

    /**
     *
     * 生成摘要：RecursiveSummarizationCompactionStrategy
     * @param deepSeekChatModel
     * @return
     */
    @Bean
    SessionMemoryAdvisor sessionMemoryAdvisor(DeepSeekChatModel deepSeekChatModel) {
        SessionService sessionService = DefaultSessionService.builder()
                .sessionRepository(InMemorySessionRepository.builder().build()).build();
        return SessionMemoryAdvisor.builder(sessionService)
                .defaultUserId("Gina")
                .compactionTrigger(new TurnCountTrigger(5)) // 使用默认值5
                .compactionStrategy(RecursiveSummarizationCompactionStrategy.builder(
                        ChatClient.builder(deepSeekChatModel).build()
                ).maxEventsToKeep(10).overlapSize(2).build())   //最多保留10个事件,重叠大小为2
                .build();
    }

    /**
     * 压缩策略 spring-ai-session：SlidingWindowCompactionStrategy 按消息数策略
     * @param deepSeekChatModel
     * @param chatMemory
     * @return
     */
    /*@Bean
    CommandLineRunner commandLineRunner(DeepSeekChatModel deepSeekChatModel
            , ChatMemory chatMemory //chatMemory 聊天记忆功能
            , SessionMemoryAdvisor sessionMemoryAdvisor
            , ToolService toolService) {
        return args -> {
            System.out.print("\n我是吉娜:.\n");
            // 由spring构建的动态代理对话模型
            // 对话代理：记忆/advisor大模型拦截器/结构化输出
            ChatClient chatClient = ChatClient.builder(deepSeekChatModel)
                    .defaultAdvisors(sessionMemoryAdvisor, SimpleLoggerAdvisor.builder().build())
                    .defaultTools(toolService) // 可以声明多个
                    .build();
            try (Scanner scanner = new Scanner(System.in)) {
                while (true) {
                    System.out.print("\n你:");
                    String message = scanner.nextLine();
                    System.out.print("\nAI:");
                    // 流式调用，服务器调用大模型后，大模型将返回的数据不断地输出给控制台
                    Flux<String> content = chatClient.prompt()
                            .user(message)
                            // spring ai 2.0 取消了给map传key的默认值，需要手动传，不然报非法算术异常
                            .advisors(a -> a.param(SessionMemoryAdvisor.SESSION_ID_CONTEXT_KEY, "Gina"))
                            .stream()
                            .content();
                    // 阻塞线程
                    content.doOnNext(System.out::print).blockLast();
                    System.out.print("\n");
                }
            }
        };
    }*/

    /**
     * 调用tools工具：mcp
     * @param deepSeekChatModel
     * @param chatMemory
     * @param sessionMemoryAdvisor
     * @param toolService
     * @return
     */
    @Bean
    CommandLineRunner commandLineRunner(DeepSeekChatModel deepSeekChatModel
            //chatMemory 聊天记忆功能,之所以能记住你，是因为每次都将之前的对话内容保存在内存中，而且消耗Token
            , ChatMemory chatMemory
            , SessionMemoryAdvisor sessionMemoryAdvisor
            , ToolService toolService
            , ToolCallbackProvider toolCallbackProvider) {
        return args -> {
            System.out.print("\n我是吉娜:.\n");
            // 由spring构建的动态代理对话模型
            // 对话代理：记忆/advisor大模型拦截器/结构化输出
            ChatClient chatClient = ChatClient.builder(deepSeekChatModel)
                    .defaultAdvisors(sessionMemoryAdvisor, SimpleLoggerAdvisor.builder().build())
                    .defaultTools(FileSystemTools.builder().build(), ShellTools.builder().build()) // 可以声明多个
                    .defaultTools(toolCallbackProvider) // 可以声明多个
                    .build();
            try (Scanner scanner = new Scanner(System.in)) {
                while (true) {
                    System.out.print("\n你:");
                    String message = scanner.nextLine();
                    System.out.print("\nAI:");
                    // 流式调用，服务器调用大模型后，大模型将返回的数据不断地输出给控制台
                    Flux<String> content = chatClient.prompt()
                            .user(message)
                            // spring ai 2.0 取消了给map传key的默认值，需要手动传，不然报非法算术异常
                            .advisors(a -> a.param(SessionMemoryAdvisor.SESSION_ID_CONTEXT_KEY, "Gina"))
                            .stream()
                            .content();
                    // 阻塞线程
                    content.doOnNext(System.out::print).blockLast();
                    System.out.print("\n");
                }
            }
        };
    }
}
