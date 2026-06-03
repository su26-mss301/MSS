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

- Add `Wardrobe`, `WardrobeZone`, `Category` and `ClothingItem` entities mapping to the database schema
- Add `.env` file support to manage environment variables securely
- Update `application.yml` to use `spring.config.import` and map local PostgreSQL credentials (`WARDROBE_DB_URL`, etc.)

**New Flow**

| Step | Actor | Action | Result |
| :--- | :--- | :--- | :--- |
| 1 | Developer | Configure `.env` with local PostgreSQL database credentials | Connects securely to the local `wardrobe-service` database |
| 2 | System | Boot up `wardrobe-service` | `application.yml` initializes |
| 3 | System | Load `spring.config.import: optional:file:.env` | Variables are dynamically injected into datasource and server config |

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
