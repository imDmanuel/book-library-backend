-- Baseline schema for fresh environments
CREATE TABLE IF NOT EXISTS roles (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(255),
    password VARCHAR(255),
    email VARCHAR(255),
    strikes INTEGER DEFAULT 0,
    suspension_until TIMESTAMP,
    is_suspended BOOLEAN DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS user_roles (
    user_id BIGINT REFERENCES users(id),
    role_id BIGINT REFERENCES roles(id),
    PRIMARY KEY (user_id, role_id)
);

CREATE TABLE IF NOT EXISTS book (
    id BIGSERIAL PRIMARY KEY,
    author VARCHAR(255),
    title VARCHAR(255),
    isbn VARCHAR(255),
    category VARCHAR(255),
    total_copies VARCHAR(255),
    available_copies VARCHAR(255),
    cover_image VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS loans (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id),
    book_id BIGINT REFERENCES book(id),
    borrow_date TIMESTAMP,
    due_date TIMESTAMP,
    return_date TIMESTAMP
);

CREATE TABLE IF NOT EXISTS reservations (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id),
    book_id BIGINT REFERENCES book(id),
    reservation_date TIMESTAMP,
    expiry_date TIMESTAMP,
    status VARCHAR(255),
    position_in_queue INTEGER
);
