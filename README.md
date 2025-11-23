# Shopping-Chat-Agent

AI shopping chat agent that helps customers discover, compare, and buy mobile phones.

## Features

- **Natural Language Processing**: Understands user queries about phones
- **Intent Detection**: Automatically detects user intent (search, compare, recommend)
- **Phone Catalog**: Search by brand, price, features, or name
- **AI-Powered Responses**: Uses OpenAI GPT for natural conversation (with fallback)
- **Conversation Management**: Maintains context across multiple messages
- **Safety Checks**: Validates and sanitizes user input
- **Thread-Safe**: Built with concurrent data structures for production use

## Tech Stack

- **Backend**: Spring Boot 3.2.0
- **Java**: 17
- **AI Integration**: OpenAI GPT-3.5-turbo (optional)
- **Build Tool**: Maven
- **Containerization**: Docker

## Prerequisites

- Java 17 or higher
- Maven 3.6+
- Docker (optional, for containerized deployment)
- OpenAI API Key (optional, for AI-powered responses)

## Quick Start

### Local Development

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd Shopping-Chat-Agent
   ```

2. **Set environment variables** (optional)
   ```bash
   export OPENAI_API_KEY=your-api-key-here
   export OPENAI_MODEL=gpt-3.5-turbo  # Optional, defaults to gpt-3.5-turbo
   ```

3. **Build the project**
   ```bash
   cd backend
   mvn clean package
   ```

4. **Run the application**
   ```bash
   mvn spring-boot:run
   ```
   Or run the JAR:
   ```bash
   java -jar target/shopping-chat-agent-1.0.0.jar
   ```

5. **Test the API**
   ```bash
   # Health check
   curl http://localhost:8080/api/chat/health
   
   # Chat endpoint
   curl -X POST http://localhost:8080/api/chat \
     -H "Content-Type: application/json" \
     -d '{"message": "Show me phones under 30000"}'
   ```

## API Endpoints

### POST `/api/chat`

Send a chat message to the agent.

**Request Body:**
```json
{
  "message": "Show me phones under 30000",
  "conversationId": "optional-conversation-id",
  "context": "optional-context"
}
```

**Response:**
```json
{
  "message": "Here are some phones under ₹30,000...",
  "intent": "search_by_price",
  "conversationId": "generated-or-provided-id",
  "recommendations": [...],
  "comparisonPhones": [...],
  "safetyResult": {
    "isSafe": true
  }
}
```

### GET `/api/chat/health`

Health check endpoint.

**Response:**
```json
{
  "status": "UP",
  "service": "shopping-chat-agent"
}
```

## Docker Deployment

### Build Docker Image

```bash
cd backend
docker build -t shopping-chat-agent:latest .
```

### Run Docker Container

```bash
docker run -d \
  -p 8080:8080 \
  -e OPENAI_API_KEY=your-api-key-here \
  -e OPENAI_MODEL=gpt-3.5-turbo \
  --name shopping-chat-agent \
  shopping-chat-agent:latest
```

### Docker Compose

Create a `docker-compose.yml`:

```yaml
version: '3.8'
services:
  shopping-chat-agent:
    build: ./backend
    ports:
      - "8080:8080"
    environment:
      - OPENAI_API_KEY=${OPENAI_API_KEY}
      - OPENAI_MODEL=${OPENAI_MODEL:-gpt-3.5-turbo}
    restart: unless-stopped
```

Run with:
```bash
docker-compose up -d
```

## Configuration

### Application Properties

Edit `backend/src/main/resources/application.properties`:

```properties
server.port=8080
spring.application.name=shopping-chat-agent

# OpenAI Configuration (optional)
openai.api.key=${OPENAI_API_KEY:}
openai.api.model=${OPENAI_MODEL:gpt-3.5-turbo}

# CORS
spring.web.cors.allowed-origins=*
```

### Environment Variables

- `OPENAI_API_KEY`: Your OpenAI API key (optional, app works without it using fallback responses)
- `OPENAI_MODEL`: OpenAI model to use (default: `gpt-3.5-turbo`)

## Testing

### Run Unit Tests

```bash
cd backend
mvn test
```

### Manual Testing

Use the provided test examples:

```bash
# Search by price
curl -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "Show me phones under 30000"}'

# Search by brand
curl -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "Show me Samsung phones"}'

# Get recommendations
curl -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "Recommend a good phone with fast charging"}'

# Compare phones
curl -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "Compare OnePlus 12R and Pixel 8a"}'
```

## Production Deployment

### Requirements

1. **Java Runtime**: Java 17 JRE
2. **Memory**: Minimum 512MB, recommended 1GB+
3. **Network**: Port 8080 (configurable)

### Steps

1. **Build the application**
   ```bash
   cd backend
   mvn clean package -DskipTests
   ```

2. **Copy files to server**
   ```bash
   scp target/shopping-chat-agent-1.0.0.jar user@server:/opt/shopping-chat-agent/
   scp phones.json user@server:/opt/shopping-chat-agent/
   ```

3. **Create systemd service** (Linux)
   Create `/etc/systemd/system/shopping-chat-agent.service`:
   ```ini
   [Unit]
   Description=Shopping Chat Agent
   After=network.target

   [Service]
   Type=simple
   User=appuser
   WorkingDirectory=/opt/shopping-chat-agent
   ExecStart=/usr/bin/java -jar /opt/shopping-chat-agent/shopping-chat-agent-1.0.0.jar
   Environment="OPENAI_API_KEY=your-api-key"
   Restart=always
   RestartSec=10

   [Install]
   WantedBy=multi-user.target
   ```

4. **Start the service**
   ```bash
   sudo systemctl daemon-reload
   sudo systemctl enable shopping-chat-agent
   sudo systemctl start shopping-chat-agent
   ```

### Cloud Deployment

#### AWS (Elastic Beanstalk / EC2)
- Use the Dockerfile or deploy the JAR directly
- Set environment variables in the platform configuration

#### Google Cloud Platform
- Deploy to Cloud Run using the Dockerfile
- Set environment variables in Cloud Run configuration

#### Heroku
- Add `Procfile`:
  ```
  web: java -jar target/shopping-chat-agent-1.0.0.jar
  ```
- Set environment variables in Heroku dashboard

## Monitoring and Logging

The application uses SLF4J/Logback for logging. Logs include:
- Request/response information
- Intent detection
- Error details
- OpenAI API interactions

### Log Levels

- `INFO`: General application flow
- `WARN`: Non-critical issues (e.g., OpenAI fallback)
- `ERROR`: Exceptions and critical errors
- `DEBUG`: Detailed debugging information

## Troubleshooting

### Application won't start
- Check Java version: `java -version` (should be 17+)
- Verify port 8080 is not in use
- Check application logs

### OpenAI not working
- Verify `OPENAI_API_KEY` is set correctly
- Check API key validity
- Application will use fallback responses if OpenAI is unavailable

### No phones loaded
- Verify `phones.json` exists in `src/main/resources/`
- Check application logs for loading errors

## Development

### Project Structure

```
backend/
├── src/
│   ├── main/
│   │   ├── java/com/example/agent/
│   │   │   ├── controller/     # REST controllers
│   │   │   ├── service/        # Business logic
│   │   │   ├── model/          # Data models
│   │   │   └── exception/       # Exception handlers
│   │   └── resources/
│   │       ├── application.properties
│   │       └── phones.json     # Phone catalog
│   └── test/                   # Unit and integration tests
├── pom.xml
└── Dockerfile
```

## License

[Add your license here]

## Contributing

[Add contribution guidelines here]
