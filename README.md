# AI IDE Assistant

IDE-style application with multi-file editing and AI chat capabilities. Built with Java 21, Spring Boot, and React.

## Features

- **File Tree Browser**: Navigate project directory structure
- **Multi-File Editor**: Monaco Editor with syntax highlighting and tab support
- **AI Chat**: Integrate with cloud providers (OpenAI, Anthropic) or local Ollama models
- **Terminal Panel**: Integrated terminal for command execution
- **Resizable Layout**: Adjustable panel widths for optimal workflow

## Architecture

### Backend (Java 21 + Spring Boot 3.2)
- REST API for file operations (`/api/file`, `/api/project`)
- WebSocket support for real-time chat streaming
- Provider abstraction for multiple AI models
- Configurable workspace directory

### Frontend (React + Vite)
- Monaco Editor for code editing
- React Split for resizable panels
- WebSocket (SockJS + STOMP) for chat
- VSCode-inspired dark theme

## Build & Run

### Prerequisites
- Java 21 or higher
- Maven 3.8+
- Node.js 20+ (auto-installed during build)

### Build Executable JAR

```bash
mvn clean package
```

This single command:
1. Installs Node.js and npm locally
2. Builds the React frontend
3. Bundles frontend into JAR static resources
4. Compiles Java backend
5. Creates executable JAR with all dependencies

### Run Application

**Default (current directory as workspace):**
```bash
java -jar target/ai-ide-assistant-0.0.1-SNAPSHOT.jar
```

**Explicit current directory:**
```bash
java -jar target/ai-ide-assistant-0.0.1-SNAPSHOT.jar .
```

**Use specific directory as workspace:**
```bash
java -jar target/ai-ide-assistant-0.0.1-SNAPSHOT.jar /path/to/your/project
```

Application starts on **http://localhost:8080**

**Note:** The workspace directory is the directory where you run the command from, unless you specify a different path as an argument.

### Development Mode

Backend (port 8080):
```bash
mvn spring-boot:run
```

Frontend (port 5173):
```bash
cd frontend
npm install
npm run dev
```

Frontend proxies API requests to backend automatically.

## Configuration

Edit `src/main/resources/application.yml`:

```yaml
server:
  port: 8080

workspace:
  root-path: .  # Current directory by default
```

## Model Configuration

Configure AI providers in the chat panel:
- **OpenAI**: Provider=`openai`, Model=`gpt-4`
- **Anthropic**: Provider=`anthropic`, Model=`claude-3-opus`
- **Ollama (Local)**: Provider=`ollama`, Model=`llama2`

Provider implementations are placeholders. Add API integration in:
- `src/main/java/de/itsourcerer/aiideassistant/service/provider/`

## Project Structure

```
.
├── src/main/java/de/itsourcerer/aiideassistant/
│   ├── controller/          # REST & WebSocket controllers
│   ├── service/             # Business logic
│   ├── model/               # Domain models
│   └── config/              # Spring configuration
├── src/main/resources/
│   └── application.yml      # App configuration
├── frontend/
│   ├── src/
│   │   ├── components/      # React UI components
│   │   └── services/        # API client layer
│   └── package.json
└── pom.xml                  # Maven build configuration
```

## API Endpoints

### File Operations
- `GET /api/file?path={path}` - Read file
- `POST /api/file` - Save file
- `DELETE /api/file?path={path}` - Delete file

### Project Operations
- `GET /api/project/tree?path={path}` - Get directory tree
- `POST /api/project/directory?path={path}` - Create directory
- `DELETE /api/project?path={path}` - Delete file/directory

### WebSocket
- Connect: `/ws` (SockJS endpoint)
- Subscribe: `/topic/messages`
- Send: `/app/chat`

## Next Steps

1. **Implement Real AI Providers**: Add HTTP clients to provider implementations
2. **Terminal Integration**: Connect backend terminal execution
3. **Authentication**: Add user authentication for workspace isolation
4. **Git Integration**: Add version control features
5. **Syntax Validation**: Integrate language servers

## License

[Your License Here]
