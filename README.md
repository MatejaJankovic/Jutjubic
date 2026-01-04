## Project: Jutjubić - Video Sharing Social Network

---

## 🚀 How to Run

### Prerequisites
- PostgreSQL database running on `localhost:5432`
- Database name: `jutjubic`
- Backend running on `http://localhost:8080`
- Node.js 18+ installed

### Start Backend
```bash
cd jutjubic-backend
./mvnw spring-boot:run
```

### Start Frontend
```bash
cd jutjubic-frontend
npm install
npm start
```

### Access Application
```
Frontend: http://localhost:4200
Backend API: http://localhost:8080
Swagger UI: http://localhost:8080/swagger-ui.html
```

---

## 🔧 Technology Stack

### Frontend
- **Framework**: Angular 21 (latest)
- **Language**: TypeScript 5.9
- **Forms**: Reactive Forms
- **HTTP**: HttpClient with Interceptors
- **Routing**: Angular Router with Guards
- **State**: RxJS BehaviorSubject
- **Styling**: SCSS with gradients

### Architecture
- **Components**: Standalone components (modern Angular)
- **Services**: Injectable services with dependency injection
- **Guards**: Functional guards (CanActivateFn)
- **Interceptors**: Functional interceptors (HttpInterceptorFn)
- **Models**: TypeScript interfaces for type safety
