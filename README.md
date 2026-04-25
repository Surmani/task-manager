# Task Manager

Sistema de gerenciamento de tarefas para equipes de desenvolvimento.

## Stack

**Backend:** Java 21, Spring Boot 3.4, Spring Security + JWT, PostgreSQL  
**Frontend:** React 18, TypeScript, Vite, Tailwind CSS, Zustand

## Como rodar

### Pré-requisitos
- Java 21, Maven, Node 22, Docker

### 1. Banco de dados
```bash
docker compose up -d
```

### 2. Backend
```bash
cd backend
mvn spring-boot:run
```
API: http://localhost:8080  
Swagger: http://localhost:8080/swagger-ui.html

### 3. Frontend
```bash
cd frontend
npm install
npm run dev
```
App: http://localhost:5173
