CREATE TABLE IF NOT EXISTS users (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    username    VARCHAR(50)  NOT NULL,
    email       VARCHAR(255),
    password    VARCHAR(255) NOT NULL,
    created_at  TIMESTAMP,
    updated_at  TIMESTAMP,
    CONSTRAINT uq_users_username UNIQUE (username)
);

CREATE TABLE IF NOT EXISTS user_roles (
    user_id BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    role    VARCHAR(50) NOT NULL,
    CONSTRAINT uq_user_roles UNIQUE (user_id, role)
);

CREATE TABLE IF NOT EXISTS listings (
    id                 BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    owner_id           BIGINT       NOT NULL REFERENCES users (id),
    title              VARCHAR(255) NOT NULL,
    description        VARCHAR(2000),
    status             VARCHAR(50)  NOT NULL DEFAULT 'DRAFT',
    version            BIGINT,
    created_by         VARCHAR(255),
    created_date       TIMESTAMP,
    last_modified_by   VARCHAR(255),
    last_modified_date TIMESTAMP
);

CREATE TABLE IF NOT EXISTS bookings (
    id                 BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    listing_id         BIGINT      NOT NULL REFERENCES listings (id),
    guest_id           BIGINT      NOT NULL REFERENCES users (id),
    status             VARCHAR(50) NOT NULL DEFAULT 'APPLIED',
    check_in_date      DATE        NOT NULL,
    check_out_date     DATE        NOT NULL,
    applied_at         TIMESTAMP   NOT NULL,
    check_in_at        TIMESTAMP,
    check_out_at       TIMESTAMP,
    version            BIGINT,
    created_by         VARCHAR(255),
    created_date       TIMESTAMP,
    last_modified_by   VARCHAR(255),
    last_modified_date TIMESTAMP
);

CREATE TABLE IF NOT EXISTS resolution_windows (
    id                    BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    booking_id            BIGINT         NOT NULL REFERENCES bookings (id),
    status                VARCHAR(50)    NOT NULL DEFAULT 'OPEN',
    opened_at             TIMESTAMP      NOT NULL,
    closed_at             TIMESTAMP,
    money_requested_at    TIMESTAMP,
    refused_at            TIMESTAMP,
    amount_requested      NUMERIC(12, 2),
    complaint_description VARCHAR(2000),
    version               BIGINT,
    created_by            VARCHAR(255),
    created_date          TIMESTAMP,
    last_modified_by      VARCHAR(255),
    last_modified_date    TIMESTAMP,
    CONSTRAINT uq_resolution_windows_booking_id UNIQUE (booking_id)
);

CREATE TABLE IF NOT EXISTS notifications (
    id                    BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id               BIGINT       NOT NULL REFERENCES users (id),
    type                  VARCHAR(80)  NOT NULL,
    title                 VARCHAR(500) NOT NULL,
    body                  VARCHAR(2000),
    related_booking_id    BIGINT REFERENCES bookings (id),
    related_resolution_id BIGINT REFERENCES resolution_windows (id),
    read                  BOOLEAN      NOT NULL DEFAULT FALSE,
    read_at               TIMESTAMP,
    created_at            TIMESTAMP
);
