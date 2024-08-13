package com.vnicy_ty.controller;

import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.GenerationParam;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.utils.JsonUtils;

import com.google.gson.JsonObject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.vnicy_ty.service.IpRateService;

@RestController
@RequestMapping("/ai")
public class AiController {
    @Value("${ai-api-key}")
    private String appKey;

    private final Generation generation;
    private List<Message> messages = new ArrayList<>();
    private final IpRateService ipRateLimiter;


    //spring 注入
    @Autowired
    public AiController(Generation generation, IpRateService ipRateLimiter) {
        this.generation = generation;
        this.ipRateLimiter = ipRateLimiter;
    }

    //单轮对话
    @PostMapping("/send")
    public String aiTalk(@RequestBody String question) throws NoApiKeyException, InputRequiredException {
        // 构建消息对象
        Message message = Message.builder().role(Role.USER.getValue()).content(question).build();

        // 构建通义千问参数对象
        GenerationParam param = GenerationParam.builder()
                .model(Generation.Models.QWEN_PLUS)
                .messages(Arrays.asList(message))
                .resultFormat(GenerationParam.ResultFormat.MESSAGE)
                .topP(0.8)
                .apiKey(appKey)
                .build();

        // 执行文本生成操作，并流式返回结果
        Generation gen = new Generation();

        return JsonUtils.toJson(gen.call(param));
    }

    private static Message createMessage(Role role, String content) {
        return Message.builder().role(role.getValue()).content(content).build();
    }


    //多轮对话参数构造
    public static GenerationParam createGenerationParam(List<Message> messages, String appKey) {
        return GenerationParam.builder()
                .model("qwen-turbo")
                .messages(messages)
                .resultFormat(GenerationParam.ResultFormat.MESSAGE)
                .topP(0.8)
                .apiKey(appKey)
                .build();
    }

    //获取ip
    private String getClientIP(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader != null) {
            return xfHeader.split(",")[0];
        }
        return request.getRemoteAddr();
    }

    @PostMapping("/send1")
    public String aiTalk1(@RequestBody String question, HttpServletRequest request) throws NoApiKeyException, InputRequiredException, JSONException {
        //获取当前ip
        String clientIP = getClientIP(request);

        if (!ipRateLimiter.isIpAllowed(clientIP)) {
            JsonObject responseJson = new JsonObject ();
            responseJson.addProperty("content", "已达最大聊天次数");
            responseJson.addProperty("output", "exit");
            return JsonUtils.toJson(responseJson);
        }


        messages.add(createMessage(Role.SYSTEM, "You are a helpful assistant."));

        JSONObject temp = new JSONObject(question);
        if ("exit".equalsIgnoreCase(temp.getString("question"))) {

            JsonObject responseJson = new JsonObject ();

            responseJson.addProperty("content", "已退出聊天");
            responseJson.addProperty("output", "exit");
            this.messages = new ArrayList<>();
            return JsonUtils.toJson(responseJson);
        }
        messages.add(createMessage(Role.USER, question));
        GenerationParam param = createGenerationParam(messages, appKey);

        Generation gen = new Generation();
        return JsonUtils.toJson(gen.call(param));

    }
}

