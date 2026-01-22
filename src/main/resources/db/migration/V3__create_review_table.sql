ALTER TABLE book RENAME TO books;

CREATE TABLE reviews (
    id BIGSERIAL PRIMARY KEY,
    rating INT NOT NULL,
    comment VARCHAR(1000),
    book_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE,
    -- Define Foreign Key constraints to link reviews to books and users
    CONSTRAINT fk_book
        FOREIGN KEY(book_id) 
        REFERENCES books(id),
    CONSTRAINT fk_user
        FOREIGN KEY(user_id) 
        REFERENCES users(id)
);