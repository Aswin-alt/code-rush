package com.example.jdeps;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.Executors;

/**
 * Simple HTTP server for handling file uploads and serving the web UI
 */
public class JDepsWebServer {

    // Explicit constructor calling super()
    public JDepsWebServer() {
        super();
    }
    
    private static final int PORT = 8080;
    private static final String UPLOAD_DIR = "uploads";
    private static final String WEB_DIR = "web-ui";
    
    public static void main(String[] args) throws IOException {
        JDepsWebServer server = new JDepsWebServer();
        server.start();
    }
    
    public void start() throws IOException {
        // Create necessary directories
        Files.createDirectories(Paths.get(UPLOAD_DIR));
        Files.createDirectories(Paths.get("web-reports"));
        
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        
        // Add CORS headers to all responses
        server.createContext("/", new CorsWrapper(new StaticFileHandler()));
        server.createContext("/upload", new CorsWrapper(new FileUploadHandler()));
        server.createContext("/analyze", new CorsWrapper(new AnalysisHandler()));
        server.createContext("/reports", new CorsWrapper(new ReportHandler()));
        server.createContext("/reports", new ReportHandler());
        
        server.setExecutor(Executors.newFixedThreadPool(10));
        server.start();
        
        System.out.println("JDeps Web Server started on http://localhost:" + PORT);
        System.out.println("Open your browser and navigate to the URL above");
    }
    
    // Handler for static files (HTML, CSS, JS)
    static class StaticFileHandler implements HttpHandler {
        public StaticFileHandler() {
            super();
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (path.equals("/")) {
                path = "/index.html";
            }
            
            Path filePath = Paths.get(WEB_DIR + path);
            
            if (Files.exists(filePath) && !Files.isDirectory(filePath)) {
                String contentType = getContentType(path);
                
                exchange.getResponseHeaders().set("Content-Type", contentType);
                exchange.sendResponseHeaders(200, Files.size(filePath));
                
                try (OutputStream os = exchange.getResponseBody()) {
                    Files.copy(filePath, os);
                }
            } else {
                // File not found
                String response = "404 - File not found";
                exchange.sendResponseHeaders(404, response.length());
                exchange.getResponseBody().write(response.getBytes());
                exchange.getResponseBody().close();
            }
        }
        
        private String getContentType(String path) {
            if (path.endsWith(".html")) return "text/html";
            if (path.endsWith(".css")) return "text/css";
            if (path.endsWith(".js")) return "text/javascript";
            if (path.endsWith(".json")) return "application/json";
            return "text/plain";
        }
    }
    
    // Handler for file uploads
    static class FileUploadHandler implements HttpHandler {
        public FileUploadHandler() {
            super();
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, 0);
                exchange.getResponseBody().close();
                return;
            }
            
            try {
                String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
                System.out.println("Content-Type: " + contentType);
                
                if (contentType != null && contentType.startsWith("multipart/form-data")) {
                    // Extract boundary
                    String boundary = null;
                    String[] parts = contentType.split(";");
                    for (String part : parts) {
                        part = part.trim();
                        if (part.startsWith("boundary=")) {
                            boundary = part.substring(9);
                            break;
                        }
                    }
                    
                    if (boundary == null) {
                        throw new IOException("No boundary found in multipart data");
                    }
                    
                    // Generate unique filename
                    String fileName = "upload-" + System.currentTimeMillis() + ".zip";
                    Path uploadPath = Paths.get(UPLOAD_DIR, fileName);
                    
                    // Parse multipart and extract file
                    parseMultipartAndSaveFile(exchange.getRequestBody(), boundary, uploadPath);
                    
                    System.out.println("File saved to: " + uploadPath);
                    
                    // Return the file path
                    String response = "{\"status\":\"success\",\"filePath\":\"" + uploadPath.toString() + "\"}";
                    
                    exchange.getResponseHeaders().set("Content-Type", "application/json");
                    exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
                    exchange.sendResponseHeaders(200, response.length());
                    exchange.getResponseBody().write(response.getBytes());
                    exchange.getResponseBody().close();
                } else {
                    throw new IOException("Expected multipart/form-data content type");
                }
                
            } catch (Exception e) {
                System.err.println("Upload error: " + e.getMessage());
                e.printStackTrace();
                String response = "{\"status\":\"error\",\"message\":\"" + e.getMessage() + "\"}";
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
                exchange.sendResponseHeaders(500, response.length());
                exchange.getResponseBody().write(response.getBytes());
                exchange.getResponseBody().close();
            }
        }
        
        private void parseMultipartAndSaveFile(InputStream inputStream, String boundary, Path outputPath) throws IOException {
            byte[] boundaryBytes = ("--" + boundary).getBytes();
            byte[] headerEndMarker = "\r\n\r\n".getBytes();
            
            // Read all data into memory (for files up to 100MB this should be fine)
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] chunk = new byte[8192];
            int bytesRead;
            
            while ((bytesRead = inputStream.read(chunk)) != -1) {
                buffer.write(chunk, 0, bytesRead);
            }
            
            byte[] allData = buffer.toByteArray();
            
            // Find the file part boundary
            int partStart = findBytes(allData, boundaryBytes, 0);
            if (partStart == -1) {
                throw new IOException("No boundary found in multipart data");
            }
            
            // Move past the boundary and CRLF
            partStart += boundaryBytes.length + 2; // +2 for CRLF after boundary
            
            // Find the end of headers (double CRLF)
            int headerEnd = findBytes(allData, headerEndMarker, partStart);
            if (headerEnd == -1) {
                // Try with just \n\n for compatibility
                byte[] altHeaderEnd = "\n\n".getBytes();
                headerEnd = findBytes(allData, altHeaderEnd, partStart);
                if (headerEnd != -1) {
                    headerEnd += altHeaderEnd.length;
                } else {
                    throw new IOException("Could not find end of headers in multipart data");
                }
            } else {
                headerEnd += headerEndMarker.length;
            }
            
            // Check if this part contains a filename
            String headers = new String(allData, partStart, headerEnd - partStart);
            if (!headers.contains("filename=")) {
                throw new IOException("No filename found in multipart headers");
            }
            
            // Find the next boundary (end of file data)
            int fileDataEnd = findBytes(allData, boundaryBytes, headerEnd);
            if (fileDataEnd == -1) {
                fileDataEnd = allData.length;
            }
            
            // Remove trailing CRLF before boundary
            while (fileDataEnd > headerEnd && 
                   (allData[fileDataEnd - 1] == '\r' || allData[fileDataEnd - 1] == '\n')) {
                fileDataEnd--;
            }
            
            // Extract file data
            int fileLength = fileDataEnd - headerEnd;
            if (fileLength <= 0) {
                throw new IOException("No file data found");
            }
            
            byte[] fileData = new byte[fileLength];
            System.arraycopy(allData, headerEnd, fileData, 0, fileLength);
            
            // Write to file
            Files.write(outputPath, fileData);
            System.out.println("Successfully extracted " + fileData.length + " bytes to " + outputPath);
        }
        
        private int findBytes(byte[] data, byte[] pattern, int startIndex) {
            if (pattern.length == 0 || startIndex >= data.length) return -1;
            
            for (int i = startIndex; i <= data.length - pattern.length; i++) {
                boolean found = true;
                for (int j = 0; j < pattern.length; j++) {
                    if (data[i + j] != pattern[j]) {
                        found = false;
                        break;
                    }
                }
                if (found) return i;
            }
            return -1;
        }
    }
    
    // Handler for analysis requests
    static class AnalysisHandler implements HttpHandler {
        public AnalysisHandler() {
            super();
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, 0);
                exchange.getResponseBody().close();
                return;
            }
            
            try {
                // Read request body to get file path
                String requestBody = new String(exchange.getRequestBody().readAllBytes());
                System.out.println("Analysis request: " + requestBody);
                
                String filePath = extractFilePathFromJson(requestBody);
                
                if (filePath == null) {
                    throw new IllegalArgumentException("Invalid request: missing filePath");
                }
                
                System.out.println("Analyzing file: " + filePath);
                
                // Analyze the project
                ZipProjectAnalyzer analyzer = new ZipProjectAnalyzer();
                String reportPath = analyzer.analyzeZipProject(filePath);
                
                // Extract report ID from path - reportPath is the directory path
                Path reportPathObj = Paths.get(reportPath);
                String reportId = reportPathObj.getFileName().toString();
                
                // Return the report info
                String response = "{\"status\":\"success\",\"reportPath\":\"" + reportPath + "\",\"reportId\":\"" + reportId + "\"}";
                
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
                exchange.sendResponseHeaders(200, response.length());
                exchange.getResponseBody().write(response.getBytes());
                exchange.getResponseBody().close();
                
            } catch (Exception e) {
                System.err.println("Analysis error: " + e.getMessage());
                e.printStackTrace();
                String response = "{\"status\":\"error\",\"message\":\"" + e.getMessage() + "\"}";
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
                exchange.sendResponseHeaders(500, response.length());
                exchange.getResponseBody().write(response.getBytes());
                exchange.getResponseBody().close();
            }
        }
        
        private String extractFilePathFromJson(String json) {
            // Simple JSON parsing for filePath
            int start = json.indexOf("\"filePath\":\"") + 12;
            if (start < 12) return null;
            int end = json.indexOf("\"", start);
            if (end < 0) return null;
            return json.substring(start, end);
        }
    }
    
    // Handler for serving generated reports
    static class ReportHandler implements HttpHandler {
        public ReportHandler() {
            super();
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            
            // Remove /reports prefix
            String reportPath = path.substring("/reports".length());
            if (reportPath.startsWith("/")) {
                reportPath = reportPath.substring(1);
            }
            
            Path filePath = Paths.get("web-reports", reportPath);
            
            if (Files.exists(filePath) && !Files.isDirectory(filePath)) {
                String contentType = getContentType(filePath.toString());
                
                exchange.getResponseHeaders().set("Content-Type", contentType);
                exchange.sendResponseHeaders(200, Files.size(filePath));
                
                try (OutputStream os = exchange.getResponseBody()) {
                    Files.copy(filePath, os);
                }
            } else {
                String response = "404 - Report not found";
                exchange.sendResponseHeaders(404, response.length());
                exchange.getResponseBody().write(response.getBytes());
                exchange.getResponseBody().close();
            }
        }
        
        private String getContentType(String path) {
            if (path.endsWith(".html")) return "text/html";
            if (path.endsWith(".css")) return "text/css";
            if (path.endsWith(".js")) return "text/javascript";
            if (path.endsWith(".json")) return "application/json";
            return "text/plain";
        }
    }
    
    // CORS wrapper to add CORS headers to all responses
    static class CorsWrapper implements HttpHandler {
        private final HttpHandler handler;
        
        public CorsWrapper(HttpHandler handler) {
            this.handler = handler;
        }
        
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // Add CORS headers
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, Authorization");
            
            // Handle OPTIONS preflight requests
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(200, 0);
                exchange.getResponseBody().close();
                return;
            }
            
            // Delegate to the actual handler
            handler.handle(exchange);
        }
    }
}
