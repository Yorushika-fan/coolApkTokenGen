package com.coolapk;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;

/**
 * 酷安 X-App-Token HTTP API 服务
 * 
 * API:
 *   GET  /token?device_id=xxx  - 生成 token
 *   GET  /health               - 健康检查
 * 
 * 启动: java -jar unidbg-coolapk.jar server [port]
 */
public class CoolapkTokenServer {
    
    private static final int DEFAULT_PORT = 8080;
    private static CoolapkToken tokenGenerator;
    
    public static void main(String[] args) throws Exception {
        int port = DEFAULT_PORT;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid port: " + args[0]);
                System.exit(1);
            }
        }
        
        startServer(port);
    }
    
    public static void startServer(int port) throws Exception {
        System.out.println("============================================================");
        System.out.println("酷安 X-App-Token API 服务");
        System.out.println("============================================================");
        
        // 初始化 token 生成器
        System.out.println("[*] Initializing token generator...");
        tokenGenerator = new CoolapkToken();
        System.out.println("[+] Token generator ready");
        
        // 创建 HTTP 服务器
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        
        // 注册路由
        server.createContext("/token", new TokenHandler());
        server.createContext("/health", new HealthHandler());
        
        // 使用线程池
        server.setExecutor(Executors.newFixedThreadPool(4));
        
        server.start();
        System.out.println("\n[+] Server started on port " + port);
        System.out.println("[*] Endpoints:");
        System.out.println("    GET /token?device_id=xxx  - Generate token");
        System.out.println("    GET /health               - Health check");
        System.out.println("\n[*] Press Ctrl+C to stop");
    }
    
    // 默认 device_id
    private static final String DEFAULT_DEVICE_ID = "sxWduByOxADMuITM5ADNy4SQzEVQgszREZjTQZENwMjMgsTat9WYphFI7kWbvFWaYByOgsDI7AyOwc2d3gXY1pVMvNFSsZTR5pUZE5mM2oWQvpnc3IkSWh0aEVFR";
    
    /**
     * Token 生成接口
     */
    static class TokenHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // 只允许 GET 请求
            if (!"GET".equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }
            
            // 解析参数，使用默认值
            Map<String, String> params = parseQuery(exchange.getRequestURI().getQuery());
            String deviceId = params.getOrDefault("device_id", DEFAULT_DEVICE_ID);
            
            try {
                // 生成 token
                long startTime = System.currentTimeMillis();
                String token = tokenGenerator.getToken(deviceId);
                long elapsed = System.currentTimeMillis() - startTime;
                
                if (token != null) {
                    String response = String.format(
                        "{\"token\":\"%s\",\"elapsed_ms\":%d}",
                        token, elapsed
                    );
                    sendResponse(exchange, 200, response);
                    System.out.println("[+] Generated token in " + elapsed + "ms");
                } else {
                    sendResponse(exchange, 500, "{\"error\":\"Failed to generate token\"}");
                }
            } catch (Exception e) {
                e.printStackTrace();
                sendResponse(exchange, 500, "{\"error\":\"" + e.getMessage() + "\"}");
            }
        }
    }
    
    /**
     * 健康检查接口
     */
    static class HealthHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            sendResponse(exchange, 200, "{\"status\":\"ok\"}");
        }
    }
    
    /**
     * 发送 HTTP 响应
     */
    private static void sendResponse(HttpExchange exchange, int code, String response) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(code, bytes.length);
        
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
    
    /**
     * 解析 URL 查询参数
     */
    private static Map<String, String> parseQuery(String query) {
        Map<String, String> params = new HashMap<>();
        if (query == null || query.isEmpty()) {
            return params;
        }
        
        for (String param : query.split("&")) {
            String[] pair = param.split("=", 2);
            if (pair.length == 2) {
                try {
                    String key = URLDecoder.decode(pair[0], "UTF-8");
                    String value = URLDecoder.decode(pair[1], "UTF-8");
                    params.put(key, value);
                } catch (UnsupportedEncodingException e) {
                    // ignore
                }
            }
        }
        return params;
    }
}
