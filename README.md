### OVERVIEW

ConveneSpace is a backend-focused concert booking platform built to explore how modern high-traffic ticketing systems work under real-world conditions.

Instead of focusing primarily on the user interface, the project was designed to understand the engineering challenges behind large-scale booking platforms
such as handling thousands of concurrent booking requests, preventing overselling, maintaining data consistency during payments, and designing APIs that remain reliable under heavy load.

The frontend acts only as a lightweight client responsible for interacting with the backend through REST APIs and WebSockets. 

The primary objective of the project was to build a production-oriented Spring Boot backend capable of handling concurrency, asynchronous processing, secure authentication, scalable deployment, and fault-tolerant booking workflows.

### MOTIVATION

I started building ConveneSpace with the intention of understanding how real world ticketing platforms are engineered behind the scenes. 
While users usually see only the booking experience, I was more interested in the backend systems that make that experience reliable under heavy traffic.

Throughout this project, my main focus was on learning concepts such as concurrency handling, distributed locking, idempotent payment processing, asynchronous messaging, scalable API design, and secure system architecture. 
My goal was to build a project that reflects production oriented backend engineering while improving my understanding of modern Spring Boot development.

### TECH STACK

**Backend**
- Java 21
- Spring Boot 3
- Spring Security
- Spring Data JPA
- Hibernate
- Maven

**Database & Storage**
- PostgreSQL
- Redis
- Flyway

**Communication**
- RabbitMQ
- STOMP
- WebSockets
- Brevo SMTP

**Infrastructure**
- Docker
- Docker Compose
- AWS EC2
- Nginx
- GitHub Actions
- Self Hosted GitHub Runner
- 
**Monitoring**

- Prometheus
- Grafana
  
**Frontend**
- React
- Vite
- Tailwind CSS
- Axios
- Zustand

### BACKEND ARCHITECTURE

The backend follows a layered architecture separating business logic, persistence, security and infra concerns into independent modules. 
This approach improves maintainability, testing and future scalability while keeping each component focused on a single responsibility.

Supporting infrastructure such as Redis, RabbitMQ, and Razorpay integrate with the service layer without tightly coupling business logic to external services.

<img width="1920" height="1080" alt="image" src="https://github.com/user-attachments/assets/cccb6dd2-29dd-429a-8d72-8d4c7ebf2be9" />

### AUTHENTICATION & SECURITY

Security was designed around stateless authentication and role-based authorization to support independent API requests without maintaining server-side sessions.

Features include:

- JWT Authentication
- Stateless Security
- RBAC (User, Organizer, Admin)
- Spring Security Filters
- BCrypt Password Encoding
- Secure API Authorization
- CORS Configuration
- Content Security Policy (CSP)
- Global Exception Handling

### REST API DESIGN

The backend exposes RESTful APIs designed around predictable resource-oriented endpoints.

Features include:

- DTO-based communication
- Consistent API response structure
- Validation
- Pagination
- Filtering
- Sorting
- Versioned APIs
- Swagger Documentation

### PERFORMANCE & SCALABILITY

The backend incorporates multiple techniques to improve performance and support increasing traffic while maintaining consistent application behaviour.

Highlights include:

- Redis Caching
- Asynchronous Processing
- RabbitMQ Message Queue
- Optimized Database Queries
- Pagination
- Lazy Loading
- Dockerized Services
- Horizontal Deployment Ready Architecture

### BOOKING SYSTEM

The booking workflow was designed around reliability and consistency.

Features include:

- Reserved Seating
- General Admission (GA)
- Venue Section Management
- Payment Verification
- Booking History
- Digital Tickets
- Booking Status Tracking

### CONCURRENCY HANDLING

ConveneSpace was designed with backend reliability in mind, especially for handling multiple booking requests at the same time.

Key concepts explored in the project include:

- Redis Distributed Locks for coordinating booking operations.
- Lua Scripts to perform atomic operations in Redis.
- SETNX for efficient distributed lock management.
- RabbitMQ for asynchronous event processing.
- Idempotent request handling for safe payment processing.
- Transaction management to maintain booking consistency.
  
### PAYMENT PROCESSING

Payment operations are designed to maintain consistency throughout the booking lifecycle.

Features:

- Razorpay Integration
- Secure Webhook Verification
- Payment Validation
- Booking Confirmation Workflow
- Transaction Management

### VIRTUAL QUEUE

The Virtual Queue was introduced to study how high traffic ticketing platforms regulate incoming booking requests during peak demand. 
Rather than processing every request simultaneously, the queue helps distribute load more efficiently, improving system stability and maintaining a consistent booking experience.

### Features

- Request throttling during peak traffic
- Fair request ordering
- Reduced backend load spikes
- Improved booking reliability
- Scalable queue-based request handling

### TESTING
The project has been tested through multiple approaches during development.

- Unit Testing
- Integration Testing
- API Testing
- Authentication Testing
- Payment Workflow Validation
- Load Testing (Gatling-Java)
  
### LIVE DEMO

**Application**: https://convenespace.space

The React frontend and Spring Boot backend are deployed together on a single AWS EC2 instance behind an Nginx reverse proxy.

## Screenshots

<details>
<summary>View Application Screenshots</summary>

### Home Page
<img width="1920" height="1080" alt="image" src="https://github.com/user-attachments/assets/99132949-53b0-4be2-a0b6-b8cfd69bc1ff" />

### Concert Detail and Ticket tiers
<img width="1920" height="1080" alt="image" src="https://github.com/user-attachments/assets/336924a0-932c-436b-a9f1-c26dea4e69e9" />

### Seat Selection and Booking
<img width="1920" height="1080" alt="image" src="https://github.com/user-attachments/assets/4323e154-259a-4b34-a16c-fe4b4dc8309f" />

### Organizer Dashboard
<img width="1920" height="1080" alt="image" src="https://github.com/user-attachments/assets/4f5add70-01cc-4b7d-9f7b-764b517ede2d" />

### Admin Dashboard
<img width="1920" height="1080" alt="image" src="https://github.com/user-attachments/assets/4e3ab84d-3c78-4f03-af8a-2f800bf71ca8" />

### API Documentation (Swagger UI)
<img width="1920" height="1080" alt="image" src="https://github.com/user-attachments/assets/ff982cef-9435-4aab-b1ed-70423101a253" />


</details>

### DEPLOYMENT

The application is containerized using Docker and deployed on an AWS EC2 instance.

Deployment stack includes:

- Docker & Docker Compose
- AWS EC2
- Nginx Reverse Proxy
- GitHub Actions CI/CD
- Self Hosted GitHub Runner

The deployment pipeline automatically builds and deploys the latest changes after successful commits to the repository.

### GETTING STARTED

**Prerequisites**

- Java 21
- Maven
- Docker & Docker Compose

**Clone the repository**

git clone https://github.com/AnmolSingh28/ConveneSpace-.git
cd ConveneSpace-

**Run locally**

docker compose up -d

Local URLs:

- Frontend: http://localhost:5173
- Backend API: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui.html

