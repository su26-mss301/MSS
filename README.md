# 👔 AI-Powered Smart Wardrobe Management System

An intelligent wardrobe management platform built with **Microservice Architecture**, **Spring Boot**, and **AI-powered clothing recognition**.

## 📌 Project Overview

The Smart Wardrobe System helps users manage their clothing collections digitally and receive personalized outfit recommendations using Artificial Intelligence.

Key features include:

- 👕 Clothing Detection & Recognition
- 🎨 Style Classification
- 🤖 Outfit Recommendation
- 📷 Camera-Based Clothing Detection
- 🗂️ Smart Wardrobe Organization
- ☁️ Image Storage Management
- 🔐 User Authentication & Authorization

---

## 🏗️ System Architecture

The system follows a Microservice Architecture pattern:

```text
Frontend
    │
    ▼
API Gateway
    │
 ┌──┼───────────────┬──────────────┬─────────────┐
 ▼  ▼               ▼              ▼             ▼

User Service
Wardrobe Service
Recommendation Service
AI Detection Service
Storage Service

    │
    ▼
 Eureka Discovery Server
```

---

## 🚀 Technology Stack

### Backend

- Java 21
- Spring Boot 3.5
- Spring Cloud
- Spring Cloud Gateway
- Eureka Discovery Server
- Spring Data JPA
- MySQL

### AI

- YOLOv8
- CLIP
- DeepFashion Dataset
- MMFashion Framework

### DevOps

- Docker
- Docker Compose
- GitHub

---

## 📂 Microservices

### User Service

Responsible for:

- User Management
- Authentication
- Authorization
- User Preferences

### Wardrobe Service

Responsible for:

- Clothing Management
- Wardrobe Organization
- Clothing Metadata

#### Latest Updates

**Summary**

- Implement full CRUD operations (Controller, Service, Repository) for `Wardrobe`, `WardrobeZone`, `Category`, and `ClothingItem` mapping to database schema
- Create request/response DTOs with validation constraints for all entities
- Implement unified `ApiResponse` wrapper for all REST endpoints
- Implement `GlobalExceptionHandler` to catch and process `AppException` and validation errors
- Add `ErrorCode` enum to centralize all error messages (Vietnamese) and HTTP statuses
- Add `.env` file support to manage environment variables securely (`spring.config.import`)
- Bump `springdoc-openapi` version to `2.8.5` to resolve Spring Boot 3.4 compatibility issue (Swagger 500 Error)

**New Flow (CRUD & Exception Handling)**

| Step | Actor | Action | Result |
| :--- | :--- | :--- | :--- |
| 1 | FE | Call CRUD API (POST/PUT/GET/DELETE) on Wardrobe entities | Request routed to appropriate Controller |
| 2 | BE | Validate DTOs and process business logic in Service layer | Data is persisted/retrieved from DB |
| 3 | BE | Throw `AppException(ErrorCode)` if resource not found or validation fails | `GlobalExceptionHandler` intercepts the error |
| 4 | FE | Receive standardized `ApiResponse` (success/message/data) | Consistent response format for both success and error cases |

**Search Feature Update (Backend)**

**Summary**
- Add `findByWardrobeNameContainingIgnoreCase` and `findByZoneNameContainingIgnoreCase` in Repositories to support partial search.
- Implement `searchWardrobes` and `searchZones` in Services, throwing `AppException` when no results are found.
- Expose `GET /wardrobes/search` and `GET /wardrobe-zones/search` endpoints.

**Search Flow**
| Step | Actor | Action | Result |
| :--- | :--- | :--- | :--- |
| 1 | FE | Call `GET /wardrobes/search?keyword={q}` or `GET /wardrobe-zones/search?keyword={q}` | Request routed to appropriate Controller |
| 2 | BE | Call Repository method `findBy...ContainingIgnoreCase` | Data queried from the Database |
| 3 | BE | Validate fetched List size | If empty, throws `AppException` (`WARDROBE_NOT_FOUND` / `WARDROBE_ZONE_NOT_FOUND`) |
| 4 | FE | Receive response wrapped in `ApiResponse` | Receives JSON payload successfully or intercepts the exception |
### Recommendation Service

Responsible for:

- Outfit Recommendation
- Style Matching
- Personalized Suggestions

### AI Detection Service

Responsible for:

- Clothing Detection
- Attribute Classification
- AI Processing Pipeline

### Storage Service

Responsible for:

- Image Storage
- File Management

---

## 📖 Academic Purpose

This project is developed for learning and demonstrating:

- Microservice Architecture
- Service Discovery with Eureka
- API Gateway Pattern
- AI Integration in Distributed Systems
- Cloud-Native Application Design

---

## 📄 License

For educational and research purposes only.
