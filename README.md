# Jutjubić - Video Sharing Platform

YouTube-like video sharing platform built with Spring Boot and Angular.

## Prerequisites

Before running the application, you need to have the following installed:

### Required
1. **Java 17+** - For running Spring Boot backend
2. **Node.js 18+** - For Angular frontend
3. **PostgreSQL 14+** - Database
4. **RabbitMQ** - Message broker for video transcoding
5. **FFmpeg** - For video transcoding and duration extraction

### Installing Prerequisites

#### PostgreSQL
```bash
# Create database
psql -U postgres
CREATE DATABASE jutjubic;
```

#### RabbitMQ
- Windows: Download from https://www.rabbitmq.com/install-windows.html
- Or use Docker: `docker run -d --name rabbitmq -p 5672:5672 -p 15672:15672 rabbitmq:management`

#### FFmpeg
- Windows: Download from https://ffmpeg.org/download.html and add to PATH
- Linux: `sudo apt install ffmpeg`
- Mac: `brew install ffmpeg`

## Configuration

### Backend Configuration
Edit `jutjubic-backend/src/main/resources/application.properties`:

```properties
# Database - change these to match your setup
spring.datasource.url=jdbc:postgresql://localhost:5432/jutjubic
spring.datasource.username=postgres
spring.datasource.password=YOUR_PASSWORD

# RabbitMQ - default credentials
spring.rabbitmq.host=localhost
spring.rabbitmq.port=5672
spring.rabbitmq.username=guest
spring.rabbitmq.password=guest
```

## Running the Application

### Backend (Spring Boot)
```bash
cd jutjubic-backend
./mvnw spring-boot:run
```
Backend will start on `http://localhost:8080`

### Frontend (Angular)
```bash
cd jutjubic-frontend
npm install
ng serve
```
Frontend will start on `http://localhost:4200`

## Database Setup

**The database schema is automatically created by Hibernate!**

When you run the backend for the first time with an empty database, Hibernate will automatically create all necessary tables because of this configuration:
```properties
spring.jpa.hibernate.ddl-auto=update
```

No manual SQL migration is required.

## Features

### Core Features
- User registration and authentication (JWT)
- Video upload with thumbnail
- Video playback with comments
- Like/view counting
- User profiles

### Advanced Features
- **Watch Party (3.15)** - Watch videos together in real-time via WebSocket
- **Thumbnail Compression (3.9)** - Daily scheduled job compresses old thumbnails
- **Video Transcoding (3.4)** - RabbitMQ-based transcoding to 720p/1080p with FFmpeg

## Project Structure

```
jutjubic/
├── jutjubic-backend/     # Spring Boot backend
│   ├── src/main/java/    # Java source code
│   └── src/main/resources/
│       └── application.properties
├── jutjubic-frontend/    # Angular frontend
│   └── src/app/
└── uploads/              # Uploaded files (created automatically)
    ├── videos/
    ├── thumbnails/
    ├── compressed/
    └── transcoded/
```

## API Documentation

### Main Endpoints
- `POST /api/auth/register` - Register new user
- `POST /api/auth/login` - Login
- `GET /api/videos` - List videos
- `POST /api/videos` - Upload video
- `GET /api/videos/{id}` - Get video
- `POST /api/watch-party/create` - Create watch party
- `POST /api/watch-party/join/{code}` - Join watch party

### WebSocket
- Endpoint: `/ws` (SockJS)
- Topics: `/topic/watch-party/{roomId}`, `/topic/chat/{videoId}`

## Troubleshooting

### RabbitMQ Connection Failed
Make sure RabbitMQ is running:
```bash
# Check status
rabbitmqctl status

# Or start via Docker
docker start rabbitmq
```

### FFmpeg Not Found
Make sure FFmpeg is in your system PATH:
```bash
ffmpeg -version
```

### Database Connection Failed
1. Make sure PostgreSQL is running
2. Check that database `jutjubic` exists
3. Verify username/password in application.properties


