package org.example.springailearning;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

@Service
public class ToolService {

    @Tool(description = "执行shell命令") // 告诉大模型工具可以做什么
    public String getShellCommand(@ToolParam(description = "命令") String command) {
        System.out.println("执行命令：" + command);
        return "执行成功！";
    }
}
