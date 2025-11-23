# Deployment Checklist

## Pre-Deployment

- [x] Error handling implemented (GlobalExceptionHandler)
- [x] Logging added throughout the application
- [x] Thread-safe conversation history (ConcurrentHashMap)
- [x] OpenAiService resource cleanup (@PreDestroy)
- [x] Health check endpoint
- [x] Unit and integration tests
- [x] Dockerfile ready
- [x] README with deployment instructions

## Build Steps

1. **Build the application**
   ```bash
   cd backend
   mvn clean package
   ```

2. **Run tests** (optional but recommended)
   ```bash
   mvn test
   ```

3. **Verify JAR is created**
   ```bash
   ls -lh target/shopping-chat-agent-1.0.0.jar
   ```

## Environment Setup

### Required
- Java 17 JRE
- Port 8080 available (or configure different port)

### Optional
- `OPENAI_API_KEY` - For AI-powered responses
- `OPENAI_MODEL` - Defaults to `gpt-3.5-turbo`

## Quick Start Commands

### Local Run
```bash
cd backend
java -jar target/shopping-chat-agent-1.0.0.jar
```

### With Environment Variables
```bash
export OPENAI_API_KEY=your-key-here
cd backend
java -jar target/shopping-chat-agent-1.0.0.jar
```

### Docker
```bash
cd backend
docker build -t shopping-chat-agent:latest .
docker run -p 8080:8080 -e OPENAI_API_KEY=your-key shopping-chat-agent:latest
```

## Verification

1. **Health Check**
   ```bash
   curl http://localhost:8080/api/chat/health
   ```
   Expected: `{"status":"UP","service":"shopping-chat-agent"}`

2. **Test Chat Endpoint**
   ```bash
   curl -X POST http://localhost:8080/api/chat \
     -H "Content-Type: application/json" \
     -d '{"message": "Show me phones under 30000"}'
   ```

3. **Use Test Script**
   ```bash
   ./test-api.sh
   ```

## Production Considerations

1. **Logging**: Configure log levels in `application.properties` or via environment variables
2. **Monitoring**: Set up application monitoring (e.g., Prometheus, New Relic)
3. **Load Balancing**: Use a load balancer if deploying multiple instances
4. **Database**: Consider adding persistent storage for conversation history if needed
5. **Rate Limiting**: Add rate limiting for production use
6. **Security**: 
   - Configure CORS properly (currently allows all origins)
   - Add authentication if needed
   - Use HTTPS in production

## Known Limitations

- Conversation history is stored in-memory (lost on restart)
- No persistent storage for conversations
- CORS allows all origins (configure for production)
- No rate limiting implemented
- No authentication/authorization

## Next Steps (Future Enhancements)

- [ ] Add database for persistent conversation storage
- [ ] Implement rate limiting
- [ ] Add authentication/authorization
- [ ] Create frontend UI
- [ ] Add metrics and monitoring
- [ ] Implement caching for phone catalog
- [ ] Add API versioning

