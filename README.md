**OVERVIEW**
ConveneSpace is a backend-focused concert booking platform built to explore how modern high-traffic ticketing systems work under real-world conditions.

Instead of focusing primarily on the user interface, the project was designed to understand the engineering challenges behind large-scale booking platforms
such as handling thousands of concurrent booking requests, preventing overselling, maintaining data consistency during payments, and designing APIs that remain reliable under heavy load.

The frontend acts only as a lightweight client responsible for interacting with the backend through REST APIs and WebSockets. 

The primary objective of the project was to build a production-oriented Spring Boot backend capable of handling concurrency, asynchronous processing, secure authentication, scalable deployment, and fault-tolerant booking workflows.

**MOTIVATION**
I started building ConveneSpace with the intention of understanding how real world ticketing platforms are engineered behind the scenes. 
While users usually see only the booking experience, I was more interested in the backend systems that make that experience reliable under heavy traffic.

Throughout this project, my main focus was on learning concepts such as concurrency handling, distributed locking, idempotent payment processing, asynchronous messaging, scalable API design, and secure system architecture. 
My goal was to build a project that reflects production oriented backend engineering while improving my understanding of modern Spring Boot development.

**TECH STACK**

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

**Frontend**
- React
- Vite
- Tailwind CSS
- Axios
- Zustand

**BACKEND ARCHITECTURE**

The backend follows a layered architecture separating business logic, persistence, security and infra concerns into independent modules. 
This approach improves maintainability, testing and future scalability while keeping each component focused on a single responsibility.
Supporting infrastructure such as Redis, RabbitMQ, and Razorpay integrate with the service layer without tightly coupling business logic to external services.

**AUTHENTICATION & SECURITY**

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

**REST API DESIGN**
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

**PERFORMANCE & SCALABILITY**
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

**BOOKING SYSTEM**
The booking workflow was designed around reliability and consistency.

Features include:

- Reserved Seating
- General Admission (GA)
- Venue Section Management
- Payment Verification
- Booking History
- Digital Tickets
- Booking Status Tracking

**Concurrency Handling**
ConveneSpace was designed with backend reliability in mind, especially for handling multiple booking requests at the same time.

Key concepts explored in the project include:

- Redis Distributed Locks for coordinating booking operations.
- Lua Scripts to perform atomic operations in Redis.
- SETNX for efficient distributed lock management.
- RabbitMQ for asynchronous event processing.
- Idempotent request handling for safe payment processing.
- Transaction management to maintain booking consistency.
  
**PAYMENT PROCESSING**
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

**TESTING**
The project has been tested through multiple approaches during development.

- Unit Testing
- Integration Testing
- API Testing
- Authentication Testing
- Payment Workflow Validation
- Load Testing (Gatling-Java)
