# Book Library Feature Enhancement Plan

> Detailed implementation plan for extending the book library application with new features

---

## 📋 Feature List from Extensions

1. [ ] Book recommendations (ML/collaborative filtering)
2. [ ] Real-time availability notifications
3. [ ] Reservation/waitlist system
4. [ ] Reviews and ratings
5. [ ] Analytics dashboard (popular books, user behavior)
6. [ ] Multi-library support (federation)
7. [ ] Book search with advanced filters
8. [ ] Email/SMS notifications
9. [ ] File uploads (book covers, documents)
10. [ ] Audit logging and activity tracking
11. [ ] Learn and implement ratelimiting

---

## 🎯 Feature Implementation Plans

### 1. Reservation/Waitlist System

#### Overview

Allow users to reserve books that are currently checked out. When a book becomes available, users are notified and can claim it.

#### Database Schema

```sql
CREATE TABLE reservations (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    book_id BIGINT NOT NULL REFERENCES books(id),
    status VARCHAR(20) NOT NULL, -- PENDING, ACTIVE, FULFILLED, CANCELLED, EXPIRED
    position INTEGER NOT NULL, -- Position in queue
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP, -- Auto-expire after 48 hours if not claimed
    fulfilled_at TIMESTAMP,
    UNIQUE(user_id, book_id, status) WHERE status IN ('PENDING', 'ACTIVE')
);

CREATE INDEX idx_reservations_user_id ON reservations(user_id);
CREATE INDEX idx_reservations_book_id ON reservations(book_id);
CREATE INDEX idx_reservations_status ON reservations(status);
CREATE INDEX idx_reservations_position ON reservations(book_id, position);
```

#### Entity Design

```java
@Entity
@Table(name = "reservations")
public class Reservation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReservationStatus status;

    @Column(nullable = false)
    private Integer position;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime expiresAt;
    private LocalDateTime fulfilledAt;
}

public enum ReservationStatus {
    PENDING,    // Waiting in queue
    ACTIVE,     // Book available, user can claim
    FULFILLED,  // Reservation completed
    CANCELLED,  // User cancelled
    EXPIRED     // Auto-expired
}
```

#### API Endpoints

```
POST   /api/reservations              - Create reservation
GET    /api/reservations/user/{id}    - Get user reservations
GET    /api/reservations/book/{id}     - Get book reservation queue
DELETE /api/reservations/{id}          - Cancel reservation
POST   /api/reservations/{id}/fulfill  - Fulfill reservation (claim book)
```

#### Business Logic

1. **Creating Reservation**:
   - Check if book is available (if yes, don't allow reservation)
   - Check if user already has active reservation for this book
   - Calculate position in queue
   - Set expiration (48 hours from creation)
   - Send confirmation email

2. **Book Returned**:
   - Check for pending reservations
   - Promote first reservation to ACTIVE
   - Send notification to user
   - Set expiration (24 hours to claim)

3. **Reservation Expiration**:
   - Scheduled job runs every hour
   - Expire ACTIVE reservations older than 24 hours
   - Expire PENDING reservations older than 48 hours
   - Move to next user in queue

4. **Fulfilling Reservation**:
   - Check if reservation is ACTIVE
   - Create loan automatically
   - Update reservation status to FULFILLED
   - Remove from queue

#### Implementation Steps

1. Create Reservation entity and repository
2. Implement ReservationService with business logic
3. Create ReservationController with REST endpoints
4. Add scheduled job for expiration
5. Integrate with email notification service
6. Add unit and integration tests
7. Update API documentation

#### System Design Considerations

- **Concurrency**: Use database locks to handle concurrent reservations
- **Performance**: Index on (book_id, position) for fast queue queries
- **Scalability**: Consider message queue for notifications
- **Consistency**: Use transactions for reservation operations

---

### 2. Reviews and Ratings

#### Overview

Allow users to rate and review books after returning them.

#### Database Schema

```sql
CREATE TABLE reviews (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    book_id BIGINT NOT NULL REFERENCES books(id),
    rating INTEGER NOT NULL CHECK (rating >= 1 AND rating <= 5),
    comment TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    UNIQUE(user_id, book_id) -- One review per user per book
);

CREATE TABLE book_ratings (
    book_id BIGINT PRIMARY KEY REFERENCES books(id),
    average_rating DECIMAL(3,2) NOT NULL DEFAULT 0.00,
    total_ratings INTEGER NOT NULL DEFAULT 0,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_reviews_book_id ON reviews(book_id);
CREATE INDEX idx_reviews_user_id ON reviews(user_id);
CREATE INDEX idx_reviews_rating ON reviews(rating);
```

#### Entity Design

```java
@Entity
@Table(name = "reviews")
public class Review {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @Column(nullable = false)
    @Min(1)
    @Max(5)
    private Integer rating;

    @Column(length = 2000)
    private String comment;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}

@Entity
@Table(name = "book_ratings")
public class BookRating {
    @Id
    @Column(name = "book_id")
    private Long bookId;

    @OneToOne
    @JoinColumn(name = "book_id")
    @MapsId
    private Book book;

    @Column(nullable = false, precision = 3, scale = 2)
    private BigDecimal averageRating;

    @Column(nullable = false)
    private Integer totalRatings;

    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
```

#### API Endpoints

```
POST   /api/reviews                    - Create review
GET    /api/reviews/book/{id}          - Get book reviews (paginated)
GET    /api/reviews/user/{id}          - Get user reviews
PUT    /api/reviews/{id}                - Update review
DELETE /api/reviews/{id}                - Delete review
GET    /api/books/{id}/rating          - Get book rating summary
```

#### Business Logic

1. **Creating Review**:
   - User must have borrowed the book (check loan history)
   - User can only review once per book
   - Calculate and update book average rating
   - Update book_ratings table

2. **Updating Review**:
   - Only review owner can update
   - Recalculate average rating
   - Update updated_at timestamp

3. **Deleting Review**:
   - Only review owner or admin can delete
   - Recalculate average rating
   - Remove from book_ratings

4. **Rating Calculation**:
   - Use weighted average
   - Update on every review create/update/delete
   - Cache rating for performance

#### Implementation Steps

1. Create Review and BookRating entities
2. Implement ReviewService with business logic
3. Create ReviewController with REST endpoints
4. Add rating calculation logic
5. Add validation (user must have borrowed book)
6. Add pagination for reviews
7. Add unit and integration tests
8. Update API documentation

---

### 3. Email/SMS Notifications

#### Overview

Send notifications for loan due dates, reservations, penalties, etc.

#### Architecture

```
┌─────────────┐
│  API Call   │
└──────┬──────┘
       │
       ├─────────────────┐
       ▼                 ▼
┌──────────┐      ┌──────────────┐
│ Response │      │ Message Queue│
│(Immediate)│      │  (RabbitMQ)  │
└──────────┘      └──────┬───────┘
                          │
                          ▼
                  ┌──────────────┐
                  │Notification │
                  │   Service   │
                  └──────┬───────┘
                         │
            ┌────────────┼────────────┐
            ▼            ▼            ▼
      ┌─────────┐  ┌─────────┐  ┌─────────┐
      │  Email  │  │   SMS   │  │  Push   │
      │ Service │  │ Service │  │ Service │
      └─────────┘  └─────────┘  └─────────┘
```

#### Notification Types

1. **Loan Notifications**:
   - Loan confirmation
   - Due date reminder (3 days before)
   - Overdue notice (1 day after)
   - Return confirmation

2. **Reservation Notifications**:
   - Reservation confirmation
   - Book available (reservation active)
   - Reservation expired
   - Reservation fulfilled

3. **Penalty Notifications**:
   - Penalty applied
   - Suspension warning
   - Suspension notice

4. **System Notifications**:
   - Account created
   - Password reset
   - Profile updated

#### Implementation

**Email Service**:

```java
@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Async
    public void sendLoanDueReminder(Loan loan) {
        EmailMessage message = EmailMessage.builder()
            .to(loan.getUser().getEmail())
            .subject("Book Due Soon: " + loan.getBook().getTitle())
            .template("loan-due-reminder")
            .data(Map.of(
                "userName", loan.getUser().getName(),
                "bookTitle", loan.getBook().getTitle(),
                "dueDate", loan.getDueDate()
            ))
            .build();

        sendEmail(message);
    }
}
```

**Message Queue Integration**:

```java
@Service
public class NotificationService {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    public void sendLoanDueReminder(Loan loan) {
        NotificationMessage message = NotificationMessage.builder()
            .type(NotificationType.LOAN_DUE_REMINDER)
            .userId(loan.getUser().getId())
            .data(loan)
            .build();

        rabbitTemplate.convertAndSend("notifications", message);
    }
}
```

#### Implementation Steps

1. Set up email service (JavaMailSender or SendGrid)
2. Create notification message models
3. Implement message queue (RabbitMQ/Kafka)
4. Create notification workers
5. Add email templates
6. Integrate with loan/reservation services
7. Add scheduled jobs for reminders
8. Add unit and integration tests

---

### 4. Book Search with Advanced Filters

#### Overview

Enhanced search with multiple filters, sorting options, and faceted search.

#### Search Features

1. **Basic Search**:
   - Keyword search (title, author, ISBN, description)
   - Full-text search capabilities

2. **Advanced Filters**:
   - Genre/Category
   - Publication year range
   - Author
   - Language
   - Availability status
   - Rating range
   - Format (hardcover, paperback, ebook)

3. **Sorting Options**:
   - Relevance (default)
   - Title (A-Z, Z-A)
   - Author
   - Publication date
   - Rating
   - Popularity

4. **Faceted Search**:
   - Show available filters with counts
   - Dynamic filter options based on results

#### Implementation

**Search Service**:

```java
@Service
public class BookSearchService {

    @Autowired
    private BookRepository bookRepository;

    public Page<Book> searchBooks(BookSearchRequest request, Pageable pageable) {
        Specification<Book> spec = buildSpecification(request);
        return bookRepository.findAll(spec, pageable);
    }

    private Specification<Book> buildSpecification(BookSearchRequest request) {
        return Specification.where(
            hasKeyword(request.getKeyword())
                .and(hasGenre(request.getGenre()))
                .and(hasAuthor(request.getAuthor()))
                .and(hasYearRange(request.getStartYear(), request.getEndYear()))
                .and(hasRating(request.getMinRating()))
                .and(isAvailable(request.getAvailableOnly()))
        );
    }
}
```

**Full-Text Search** (PostgreSQL):

```sql
-- Add full-text search index
CREATE INDEX idx_books_search ON books
USING gin(to_tsvector('english', title || ' ' || author || ' ' || description));

-- Search query
SELECT * FROM books
WHERE to_tsvector('english', title || ' ' || author)
      @@ plainto_tsquery('english', 'search term');
```

#### API Endpoint

```
GET /api/books/search?keyword=spring&genre=fiction&minRating=4&sort=rating&page=0&size=20
```

#### Implementation Steps

1. Create BookSearchRequest DTO
2. Implement Specification-based filtering
3. Add full-text search indexes
4. Create search service
5. Add search endpoint
6. Implement faceted search
7. Add caching for popular searches
8. Add unit and integration tests

---

### 5. Analytics Dashboard

#### Overview

Track and display analytics about popular books, user behavior, and library usage.

#### Metrics to Track

1. **Book Metrics**:
   - Most borrowed books
   - Most reserved books
   - Highest rated books
   - Books by genre popularity
   - Average loan duration

2. **User Metrics**:
   - Active users
   - New users
   - Most active borrowers
   - User retention rate

3. **Library Metrics**:
   - Total loans per day/week/month
   - Average books per user
   - Return rate
   - Overdue rate
   - Reservation fulfillment rate

#### Database Schema

```sql
CREATE TABLE analytics_events (
    id BIGSERIAL PRIMARY KEY,
    event_type VARCHAR(50) NOT NULL, -- LOAN, RETURN, RESERVATION, SEARCH, etc.
    user_id BIGINT REFERENCES users(id),
    book_id BIGINT REFERENCES books(id),
    metadata JSONB, -- Additional event data
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_analytics_event_type ON analytics_events(event_type);
CREATE INDEX idx_analytics_created_at ON analytics_events(created_at);
CREATE INDEX idx_analytics_user_id ON analytics_events(user_id);
CREATE INDEX idx_analytics_book_id ON analytics_events(book_id);
```

#### Implementation

**Analytics Service**:

```java
@Service
public class AnalyticsService {

    @Autowired
    private AnalyticsEventRepository eventRepository;

    public void trackEvent(AnalyticsEvent event) {
        // Async processing via message queue
        rabbitTemplate.convertAndSend("analytics", event);
    }

    public PopularBooksReport getPopularBooks(LocalDate startDate, LocalDate endDate) {
        // Aggregate analytics data
        return eventRepository.findPopularBooks(startDate, endDate);
    }
}
```

**Dashboard API**:

```
GET /api/analytics/popular-books?period=week
GET /api/analytics/user-stats?period=month
GET /api/analytics/library-metrics?period=year
```

#### Implementation Steps

1. Create analytics event model
2. Implement event tracking
3. Add message queue for async processing
4. Create aggregation queries
5. Build dashboard API endpoints
6. Add caching for dashboard data
7. Create scheduled jobs for pre-aggregation
8. Add unit and integration tests

---

### 6. File Uploads (Book Covers, Documents)

#### Overview

Allow uploading book cover images and PDF documents.

#### Storage Strategy

**Options**:

1. **Local Storage**: Simple, but not scalable
2. **Cloud Storage**: AWS S3, Google Cloud Storage (recommended)
3. **CDN**: CloudFront, Cloudflare (for delivery)

#### Implementation

**File Upload Service**:

```java
@Service
public class FileUploadService {

    @Autowired
    private AmazonS3 s3Client;

    public String uploadBookCover(MultipartFile file, Long bookId) {
        String fileName = "covers/" + bookId + "/" + file.getOriginalFilename();
        s3Client.putObject("book-library", fileName, file.getInputStream(), metadata);
        return s3Client.getUrl("book-library", fileName).toString();
    }

    public void deleteBookCover(Long bookId) {
        s3Client.deleteObject("book-library", "covers/" + bookId);
    }
}
```

**API Endpoints**:

```
POST   /api/books/{id}/cover     - Upload book cover
DELETE /api/books/{id}/cover     - Delete book cover
GET    /api/books/{id}/cover      - Get book cover URL
POST   /api/books/{id}/document  - Upload book document (PDF)
```

#### Implementation Steps

1. Set up cloud storage (AWS S3)
2. Create file upload service
3. Add file validation (size, type)
4. Implement image resizing (thumbnails)
5. Add file upload endpoints
6. Update Book entity with cover URL
7. Add file deletion on book delete
8. Add unit and integration tests

---

### 7. Audit Logging and Activity Tracking

#### Overview

Track all user activities and system events for security and compliance.

#### Database Schema

```sql
CREATE TABLE audit_logs (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id),
    action VARCHAR(100) NOT NULL, -- CREATE_BOOK, BORROW_BOOK, etc.
    entity_type VARCHAR(50) NOT NULL, -- BOOK, LOAN, USER, etc.
    entity_id BIGINT,
    old_values JSONB,
    new_values JSONB,
    ip_address VARCHAR(45),
    user_agent TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_audit_logs_user_id ON audit_logs(user_id);
CREATE INDEX idx_audit_logs_action ON audit_logs(action);
CREATE INDEX idx_audit_logs_created_at ON audit_logs(created_at);
CREATE INDEX idx_audit_logs_entity ON audit_logs(entity_type, entity_id);
```

#### Implementation

**AOP-Based Auditing**:

```java
@Aspect
@Component
public class AuditAspect {

    @Autowired
    private AuditLogService auditLogService;

    @AfterReturning("@annotation(Auditable)")
    public void audit(JoinPoint joinPoint) {
        AuditLog log = AuditLog.builder()
            .userId(getCurrentUserId())
            .action(extractAction(joinPoint))
            .entityType(extractEntityType(joinPoint))
            .entityId(extractEntityId(joinPoint))
            .ipAddress(getClientIp())
            .userAgent(getUserAgent())
            .build();

        auditLogService.save(log);
    }
}
```

**Usage**:

```java
@Auditable(action = "BORROW_BOOK")
public Loan borrowBook(Long bookId, Long userId) {
    // Implementation
}
```

#### Implementation Steps

1. Create AuditLog entity
2. Implement AOP aspect for auditing
3. Create AuditLogService
4. Add @Auditable annotation to key methods
5. Create audit log query API (admin only)
6. Add log retention policy
7. Add unit and integration tests

---

### 8. Real-Time Availability Notifications

#### Overview

Notify users in real-time when a reserved book becomes available.

#### Implementation

**WebSocket Integration**:

```java
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic");
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws").withSockJS();
    }
}
```

**Notification Service**:

```java
@Service
public class RealTimeNotificationService {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    public void notifyBookAvailable(Long userId, Book book) {
        NotificationMessage message = NotificationMessage.builder()
            .type("BOOK_AVAILABLE")
            .message("Book '" + book.getTitle() + "' is now available!")
            .bookId(book.getId())
            .build();

        messagingTemplate.convertAndSend("/topic/user/" + userId, message);
    }
}
```

#### Implementation Steps

1. Add WebSocket dependencies
2. Configure WebSocket
3. Create notification service
4. Integrate with reservation system
5. Add frontend WebSocket client
6. Add unit and integration tests

---

### 9. Book Recommendations (ML/Collaborative Filtering)

#### Overview

Recommend books to users based on their borrowing history and similar users.

#### Approaches

1. **Collaborative Filtering**:
   - Find users with similar preferences
   - Recommend books they liked

2. **Content-Based Filtering**:
   - Analyze book features (genre, author, etc.)
   - Recommend similar books

3. **Hybrid Approach**:
   - Combine both methods
   - Better accuracy

#### Implementation

**Recommendation Service**:

```java
@Service
public class RecommendationService {

    public List<Book> getRecommendations(Long userId) {
        // 1. Get user's borrowing history
        List<Book> userBooks = getBorrowedBooks(userId);

        // 2. Find similar users
        List<Long> similarUsers = findSimilarUsers(userId);

        // 3. Get books liked by similar users
        List<Book> recommendations = getBooksFromSimilarUsers(similarUsers, userBooks);

        // 4. Rank and return top N
        return rankRecommendations(recommendations, userId).stream()
            .limit(10)
            .collect(Collectors.toList());
    }
}
```

#### Implementation Steps

1. Research recommendation algorithms
2. Create recommendation service
3. Implement collaborative filtering
4. Add caching for recommendations
5. Create recommendation API endpoint
6. Add scheduled job to pre-compute recommendations
7. Add unit and integration tests

---

### 10. Multi-Library Support (Federation)

#### Overview

Support multiple libraries in a federated system where users can borrow from any library.

#### Architecture

```
┌─────────────┐
│   Library 1 │
│  (Instance) │
└──────┬──────┘
       │
       ├─────────────────┐
       ▼                 ▼
┌─────────────┐    ┌─────────────┐
│   Library 2 │    │   Library 3 │
│  (Instance) │    │  (Instance) │
└──────┬──────┘    └──────┬───────┘
       │                 │
       └─────────┬───────┘
                 │
                 ▼
         ┌───────────────┐
         │  Federation   │
         │    Service    │
         └───────────────┘
```

#### Database Schema

```sql
CREATE TABLE libraries (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    address TEXT,
    contact_email VARCHAR(100),
    api_endpoint VARCHAR(255),
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE books ADD COLUMN library_id BIGINT REFERENCES libraries(id);
ALTER TABLE loans ADD COLUMN source_library_id BIGINT REFERENCES libraries(id);
ALTER TABLE loans ADD COLUMN destination_library_id BIGINT REFERENCES libraries(id);
```

#### Implementation Steps

1. Add Library entity
2. Update Book and Loan entities
3. Create federation service
4. Implement inter-library API communication
5. Add library selection in UI
6. Handle cross-library loans
7. Add unit and integration tests

---

## 🎯 Implementation Priority

### Phase 1: High Value, Low Complexity (2-3 weeks)

1. Reservation/waitlist system
2. Email notifications
3. Advanced search filters
4. Audit logging

### Phase 2: Medium Complexity (1-2 months)

5. Reviews and ratings
6. File uploads
7. Analytics dashboard

### Phase 3: Advanced Features (2-3 months)

8. Real-time notifications
9. Book recommendations (ML)
10. Multi-library support

---

## 📊 Success Metrics

### For Each Feature

1. **Adoption Rate**: % of users using the feature
2. **Usage Frequency**: How often feature is used
3. **Performance**: Response times, throughput
4. **Error Rate**: Failures and issues
5. **User Satisfaction**: Feedback and ratings

---

**Remember**: Implement features incrementally, test thoroughly, and gather user feedback. Each feature should add value and improve the user experience! 🚀
