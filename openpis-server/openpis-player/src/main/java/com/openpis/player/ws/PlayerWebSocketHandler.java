package com.openpis.player.ws;

import com.openpis.player.service.PlayerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 播放端 WebSocket 处理器：
 * - 终端连接后按设备编码登记会话
 * - 收到 "PING" 回复 "PONG"，并顺带刷新设备心跳
 * - 计划变更时向所有终端广播 {"type":"PLAN_CHANGED"}
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PlayerWebSocketHandler extends TextWebSocketHandler {

    private final PlayerService playerService;

    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String code = extractCode(session);
        if (code != null) {
            sessions.put(code, session);
            try {
                playerService.heartbeat(code, null);
            } catch (Exception e) {
                log.warn("WebSocket 连接心跳更新失败: {}", e.getMessage());
            }
            log.info("播放终端已连接: {}, 当前在线: {}", code, sessions.size());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, org.springframework.web.socket.CloseStatus status) {
        String code = extractCode(session);
        if (code != null) {
            sessions.remove(code);
            log.info("播放终端已断开: {}, 当前在线: {}", code, sessions.size());
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        String payload = message.getPayload();
        if ("PING".equalsIgnoreCase(payload.trim())) {
            String code = extractCode(session);
            if (code != null) {
                try {
                    playerService.heartbeat(code, null);
                } catch (Exception ignored) {
                }
            }
            try {
                session.sendMessage(new TextMessage("PONG"));
            } catch (IOException e) {
                log.warn("回复 PONG 失败: {}", e.getMessage());
            }
        }
    }

    /**
     * 向所有在线终端广播消息
     */
    public void broadcast(String message) {
        sessions.values().forEach(session -> {
            try {
                if (session.isOpen()) {
                    synchronized (session) {
                        session.sendMessage(new TextMessage(message));
                    }
                }
            } catch (IOException e) {
                log.warn("WebSocket 广播失败: {}", e.getMessage());
            }
        });
        log.info("已向 {} 个终端广播: {}", sessions.size(), message);
    }

    private String extractCode(WebSocketSession session) {
        URI uri = session.getUri();
        if (uri == null) {
            return null;
        }
        String query = uri.getQuery();
        if (query == null) {
            return null;
        }
        for (String param : query.split("&")) {
            String[] pair = param.split("=", 2);
            if (pair.length == 2 && "code".equals(pair[0])) {
                return pair[1];
            }
        }
        return null;
    }
}
