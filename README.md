# 📚 Book Library Management System

A comprehensive RESTful API for managing a digital book library system built with Spring Boot. This application provides features for book management, user authentication, loan tracking, reservations, and waitlist management.

## ✨ Features

### Core Features

- **Book Management**: CRUD operations for books with search and filtering capabilities
- **User Authentication**: JWT-based authentication with Google OAuth2 support
- **Role-Based Access Control**: Admin and User roles with appropriate permissions
- **Loan Management**: Borrow and return books with automatic due date calculation
- **Reservation & Waitlist System**: Reserve books with intelligent queue management
- **Penalty System**: Automatic penalty tracking for overdue books with user suspension
- **Pagination & Sorting**: Efficient data retrieval with customizable pagination

### Advanced Features

- **MapStruct Integration**: Type-safe DTO mapping for clean code architecture
- **Request Logging**: Comprehensive request/response logging for debugging
- **Scheduled Tasks**: Automatic expiration of reservations and penalty checks
- **API Documentation**: Swagger/OpenAPI integration for interactive API documentation

## 🛠️ Tech Stack

- **Framework**: Spring Boot 3.5.7
- **Language**: Java 17
- **Database**: PostgreSQL
- **Security**: Spring Security with JWT
- **OAuth2**: Google OAuth2 Client
- **Mapping**: MapStruct 1.6.3
- **Documentation**: SpringDoc OpenAPI 2.8.13
- **Build Tool**: Maven
- **Utilities**: Lombok

## 📋 Prerequisites

Before you begin, ensure you have the following installed:

- **Java 17** or higher
- **Maven 3.6+**
- **PostgreSQL 12+**
- **IDE** (IntelliJ IDEA, Eclipse, or VS Code recommended)

## 🚀 Getting Started

### 1. Clone the Repository

```bash
git clone <repository-url>
cd book-library
```

### 2. Database Setup

Create a PostgreSQL database:

```sql
CREATE DATABASE book_library;
```

### 3. Configuration

Update `src/main/resources/application.properties` with your database credentials:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/book-library
spring.datasource.username=your_username
spring.datasource.password=your_password
```

### 4. JWT Secret Configuration

Generate a secure JWT secret and update `app.jwtSecret` in `application.properties`:

```bash
# Generate a secure random key (example)
openssl rand -base64 64
```

### 5. OAuth2 Configuration (Optional)

For Google OAuth2, update the following in `application.properties`:

```properties
spring.security.oauth2.client.registration.google.client-id=your_client_id
spring.security.oauth2.client.registration.google.client-secret=your_client_secret
frontend.redirect.url=http://localhost:3000/oauth2/callback
```

### 6. Build the Project

```bash
mvn clean install
```

### 7. Run the Application

```bash
mvn spring-boot:run
```

Or run the main class `BookLibraryApplication` from your IDE.

The application will start on `http://localhost:8080`

## 📖 API Documentation

Once the application is running, access the interactive API documentation at:

- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **OpenAPI JSON**: http://localhost:8080/v3/api-docs

## 🔐 Authentication

### Register a New User

```http
POST /api/auth/signup
Content-Type: application/json

{
  "username": "john_doe",
  "email": "john@example.com",
  "password": "password123",
  "role": ["user"]
}
```

### Login

```http
POST /api/auth/signin
Content-Type: application/json

{
  "username": "john_doe",
  "password": "password123"
}
```

Response includes a JWT token that should be used in subsequent requests:

```http
Authorization: Bearer <your_jwt_token>
```

### Google OAuth2

Access `/login/oauth2/code/google` to initiate Google OAuth2 login.

## 📚 API Endpoints

### Books

| Method | Endpoint                | Description               | Auth Required |
| ------ | ----------------------- | ------------------------- | ------------- |
| GET    | `/api/books`            | Get all books (paginated) | No            |
| GET    | `/api/books/{id}`       | Get book by ID            | No            |
| GET    | `/api/books/search`     | Search books              | No            |
| POST   | `/api/books`            | Create new book           | Admin         |
| PUT    | `/api/books/{id}`       | Update book               | Admin         |
| DELETE | `/api/books/{id}`       | Delete book               | Admin         |
| PUT    | `/api/books/{id}/stock` | Update book stock         | Admin         |

**Search Parameters:**

- `title`, `author`, `isbn`, `category`, `keyword`
- `page`, `size`, `sortBy`, `sortDir`

### Loans

| Method | Endpoint              | Description           | Auth Required |
| ------ | --------------------- | --------------------- | ------------- |
| POST   | `/api/loans/borrow`   | Borrow a book         | User          |
| POST   | `/api/loans/return`   | Return a book         | User          |
| GET    | `/api/loans/my-loans` | Get user's loans      | User          |
| GET    | `/api/loans/`         | Get all loans (admin) | Admin         |

**Loan Status Filter:**

- `ALL`, `ACTIVE`, `RETURNED`, `OVERDUE`

### Reservations

| Method | Endpoint                            | Description             | Auth Required |
| ------ | ----------------------------------- | ----------------------- | ------------- |
| POST   | `/api/reservations`                 | Create reservation      | User          |
| DELETE | `/api/reservations/{id}`            | Cancel reservation      | User          |
| GET    | `/api/reservations/my-reservations` | Get user's reservations | User          |
| GET    | `/api/reservations/{id}/position`   | Get waitlist position   | User          |

### User Management

| Method | Endpoint                   | Description      | Auth Required |
| ------ | -------------------------- | ---------------- | ------------- |
| GET    | `/api/user/current-user`   | Get current user | User          |
| PUT    | `/api/user/update-profile` | Update profile   | User          |

## 🏗️ Project Structure

```
book-library/
├── src/
│   ├── main/
│   │   ├── java/com/imdmanuel/book_library/
│   │   │   ├── config/              # Configuration classes
│   │   │   ├── controllers/          # REST controllers
│   │   │   ├── enums/                # Enumeration types
│   │   │   ├── exception/            # Exception handlers
│   │   │   ├── mappers/              # MapStruct mappers
│   │   │   ├── models/               # Entity models
│   │   │   ├── payload/              # DTOs (request/response)
│   │   │   ├── repository/           # JPA repositories
│   │   │   ├── security/             # Security configuration
│   │   │   └── services/             # Business logic
│   │   └── resources/
│   │       └── application.properties
│   └── test/                         # Test files
├── pom.xml                           # Maven configuration
└── README.md
```

## 🔑 Key Components

### Models

- **Book**: Book entity with inventory tracking
- **User**: User entity with roles and suspension tracking
- **Loan**: Loan entity tracking book borrow/return
- **Reservation**: Reservation entity with waitlist queue management
- **Role**: Role entity for authorization

### Services

- **BookService**: Book management operations
- **LoanService**: Loan operations with penalty integration
- **ReservationService**: Reservation and waitlist management
- **UserService**: User management
- **PenaltyService**: Overdue book penalty system

### Mappers (MapStruct)

- **BookMapper**: Book entity ↔ DTO mapping
- **LoanMapper**: Loan entity ↔ DTO mapping
- **ReservationMapper**: Reservation entity ↔ DTO mapping
- **UserMapper**: User entity ↔ DTO mapping

## ⚙️ Configuration

### Application Properties

Key configuration options in `application.properties`:

```properties
# Database
spring.datasource.url=jdbc:postgresql://localhost:5432/book-library
spring.jpa.hibernate.ddl-auto=update

# JWT
app.jwtSecret=your_secret_key
app.jwtExpirationMs=86400000  # 24 hours

# Logging
logging.level.root=INFO
logging.level.com.imdmanuel.book_library=DEBUG
```

### Scheduled Tasks

The application includes scheduled tasks:

- **Reservation Expiration**: Runs daily at midnight to expire old reservations
- **Penalty Checks**: Automatic penalty application for overdue books

## 🧪 Testing

Run tests with Maven:

```bash
mvn test
```

### API Testing

Use the provided `api.http` file for testing endpoints, or use tools like:

- Postman
- Insomnia
- HTTP Client (IntelliJ IDEA)

## 🔒 Security Features

- **JWT Authentication**: Secure token-based authentication
- **Password Encryption**: BCrypt password hashing
- **Role-Based Access Control**: Admin and User role separation
- **OAuth2 Integration**: Google OAuth2 support
- **CORS Configuration**: Configurable CORS settings
- **Request Validation**: Input validation using Jakarta Validation

## 📝 Loan & Penalty System

### Loan Duration

- Default loan period: **14 days**
- Automatic due date calculation

### Penalty System

- **Strikes**: Users receive strikes for overdue books
- **Suspension**: Users with 3+ strikes are suspended for 7 days
- **Automatic Checks**: Scheduled tasks check for overdue books

## 📋 Reservation System

### Features

- **Smart Queue Management**: Automatic position tracking in waitlist
- **Auto-Fulfillment**: When a book is returned, next reservation is activated
- **Expiration**: Reservations expire after 7 days
- **Status Tracking**: PENDING → ACTIVE → FULFILLED/EXPIRED/CANCELLED

### Reservation Flow

1. User creates reservation for unavailable book → Added to waitlist
2. Book becomes available → Next reservation automatically activated
3. User has 48 hours to borrow after activation
4. Reservation expires after 7 days if not fulfilled

## 🐛 Troubleshooting

### Common Issues

**Database Connection Error**

- Verify PostgreSQL is running
- Check database credentials in `application.properties`
- Ensure database exists

**JWT Token Issues**

- Verify `app.jwtSecret` is set correctly
- Check token expiration time
- Ensure token is included in Authorization header

**Port Already in Use**

- Change port in `application.properties`: `server.port=8081`

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 👤 Author

**ImDmanuel**

## 🙏 Acknowledgments

- Spring Boot team for the excellent framework
- MapStruct for type-safe mapping
- All contributors and open-source libraries used

## 📞 Support

For support, email chiwuzoemmanuel@gmail.com or open an issue in the repository.

---

**Happy Coding! 🚀**
