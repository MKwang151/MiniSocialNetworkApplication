# SoMe

**SoMe** is a mini social network application built with a **Client-Server** architecture, featuring an Android frontend (Kotlin + Jetpack Compose) and a Spring Boot backend with Firebase integration.

---

## 🚀 Prerequisites

**Backend:**
- Java 17+
- Maven
- Firebase Project (Firestore + Authentication)

**Frontend:**
- Android Studio (Giraffe or newer)
- Android SDK 33+

---

## 🛠️ How to Run

### Backend

```bash
cd Backend
# Add serviceAccountKey.json to src/main/resources/
mvn spring-boot:run
```

- API runs at: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui.html`

### Frontend

1. Open `FrontEnd` folder in Android Studio
2. Add `google-services.json` to `app/`
3. Sync Gradle and Run on emulator/device

---

## 📂 Project Structure

```
SoMe/
├── Backend/                    # Spring Boot REST API
│   └── src/main/java/com/social/backend/
│       ├── controller/         # REST Controllers
│       ├── model/              # Entities & DTOs
│       ├── repository/         # Firebase Repositories
│       ├── service/            # Business Logic
│       └── security/           # Spring Security
│
└── FrontEnd/                   # Android Application
    └── app/src/main/java/.../
        ├── core/
        │   ├── data/           # API & Data Sources
        │   ├── di/             # Hilt Modules
        │   ├── domain/         # Models, Repos, UseCases
        │   └── util/           # Utilities
        └── ui/                 # Jetpack Compose Screens
            ├── auth/           # Login/Register
            ├── feed/           # News Feed
            ├── chat/           # Messaging
            ├── friends/        # Friend Management
            ├── group/          # Groups
            ├── profile/        # User Profile
            └── admin/          # Admin Panel
```

---

## 🎯 Features

| Feature | Description |
|---------|-------------|
| **Authentication** | Login/Register with Firebase Auth |
| **Posts** | Create, view, comment on posts |
| **Friends** | Send/accept friend requests |
| **Chat** | Real-time messaging (WebSocket) |
| **Groups** | Create and manage social groups |
| **Notifications** | Push notifications for activities |
| **Reports** | Report users/posts (Admin moderation) |

---

## 🔧 Tech Stack

| Layer | Technologies |
|-------|--------------|
| **Frontend** | Kotlin, Jetpack Compose, Hilt, Retrofit |
| **Backend** | Spring Boot 3.4.1, Java 17, Maven |
| **Database** | Firebase Firestore |
| **Auth** | Firebase Authentication |
| **Real-time** | WebSocket |
| **API Docs** | SpringDoc OpenAPI (Swagger) |

---

## 📡 API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/auth/login` | User login |
| `POST` | `/api/auth/register` | User registration |
| `GET/PUT` | `/api/users/{id}` | Get/Update user |
| `GET/POST` | `/api/posts` | Get/Create posts |
| `POST` | `/api/comments` | Add comment |
| `GET/POST` | `/api/friends` | Friends management |
| `GET/POST` | `/api/groups` | Groups management |
| `GET` | `/api/notifications` | Get notifications |
| `POST` | `/api/reports` | Submit report |

---

## � Security

- Firebase Authentication for user verification
- Spring Security for API protection
- Token-based authentication (Firebase ID Token)

---

## 🤝 Contributing

1. Fork the repository
2. Create feature branch (`git checkout -b feature/NewFeature`)
3. Commit changes (`git commit -m 'Add NewFeature'`)
4. Push to branch (`git push origin feature/NewFeature`)
5. Open a Pull Request

---

## 📄 License

This project is developed for educational purposes.
