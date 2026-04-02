CREATE DATABASE `pms-auth`;

USE `pms-auth`;

CREATE TABLE users
(
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    email         VARCHAR(255) NOT NULL,
    password_hash VARCHAR(60)  NOT NULL, -- BCrypt always 60 chars
    enabled       BOOLEAN      NOT NULL DEFAULT TRUE,
    domain_id     BIGINT       NULL,     -- links to doctor.id or patient.id; NULL for admin
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uq_email UNIQUE (email)
);

CREATE TABLE roles
(
    id   BIGINT      NOT NULL AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL,
    CONSTRAINT pk_roles PRIMARY KEY (id),
    CONSTRAINT uq_rolename UNIQUE (name)
);

CREATE TABLE user_roles
(
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    CONSTRAINT pk_user_roles PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_ur_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_ur_role FOREIGN KEY (role_id) REFERENCES roles (id) ON DELETE CASCADE
);

-- Refresh tokens: stored as SHA-256 hash, never plain text
CREATE TABLE refresh_tokens
(
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    user_id    BIGINT      NOT NULL,
    token_hash VARCHAR(64) NOT NULL,
    issued_at  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at DATETIME    NOT NULL,
    revoked    BOOLEAN     NOT NULL DEFAULT FALSE,
    CONSTRAINT pk_rt PRIMARY KEY (id),
    CONSTRAINT uq_rt_hash UNIQUE (token_hash),
    CONSTRAINT fk_rt_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);
CREATE INDEX idx_rt_user_id ON refresh_tokens (user_id);

-- ── Seed roles ─────────────────────────────────────────────────
INSERT INTO roles (name)
VALUES ('ROLE_HEALTHCARE_ADMIN'),
       ('ROLE_DOCTOR'),
       ('ROLE_PATIENT');

-- ── Seed users ─────────────────────────────────────────────────
-- Passwords are BCrypt(cost=12). Values shown in comments — change before production.

-- admin@pms.ie  /  Admin@1234!
INSERT INTO users (email, password_hash, domain_id)
VALUES ('admin@pms.ie',
        '$2a$12$Dl.NsFefxVo0cKPtWqMmGe8eAeMMeU6UfwJNpHKQc.mQML.rpV80u',
        NULL);

-- dr.smith@pms.ie  /  Doctor@1234!   (domain_id = 1 → ms-doctor row id=1)
INSERT INTO users (email, password_hash, domain_id)
VALUES ('dr.smith@pms.ie',
        '$2a$12$XtSuMK8k3bCyqQ6lW8fYhOJ0mOiMVvVlGqB8Z7pRXeAAyXEOhnZrC',
        1);

-- john.doe@pms.ie  /  Patient@1234!  (domain_id = 1 → ms-patient row id=1)
INSERT INTO users (email, password_hash, domain_id)
VALUES ('john.doe@pms.ie',
        '$2a$12$L9kP2YtVqB3mNxJ8dFgHuOEwZs4iKlRvXcA7nWM6eTbCpQjYaDIK.',
        1);

-- ── Assign roles ───────────────────────────────────────────────
INSERT INTO user_roles (user_id, role_id)
VALUES (1, (SELECT id FROM roles WHERE name = 'ROLE_HEALTHCARE_ADMIN')),
       (2, (SELECT id FROM roles WHERE name = 'ROLE_DOCTOR')),
       (3, (SELECT id FROM roles WHERE name = 'ROLE_PATIENT'));


CREATE DATABASE `pms-doctor`;

USE `pms-doctor`;

-- Create 'speciality' table if it doesn't exist
CREATE TABLE IF NOT EXISTS `speciality`
(
    `id`          INT(11)      NOT NULL AUTO_INCREMENT,
    `description` VARCHAR(100) NOT NULL UNIQUE,
    `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = latin1;

-- Create 'doctor' table if it doesn't exist
CREATE TABLE IF NOT EXISTS `doctor`
(
    `id`            INT          NOT NULL AUTO_INCREMENT,
    `speciality_id` INT(11)      NOT NULL,
    `first_name`    VARCHAR(50)  NOT NULL,
    `last_name`     VARCHAR(50)  NOT NULL,
    `title`         VARCHAR(45)  NOT NULL,
    `email`         VARCHAR(150) NOT NULL,
    `phone`         VARCHAR(30)  NOT NULL,
    `department`    VARCHAR(100) NOT NULL,
    `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_doctor_1` (`speciality_id`),
    INDEX `idx_doctor_2` (`last_name`),
    INDEX `idx_doctor_3` (`email`),
    INDEX `idx_doctor_4` (`department`),
    CONSTRAINT `fk_doctor_1`
        FOREIGN KEY (`speciality_id`)
            REFERENCES `speciality` (`id`)
            ON DELETE NO ACTION
            ON UPDATE NO ACTION
) ENGINE = InnoDB
  DEFAULT CHARSET = latin1;

INSERT IGNORE INTO `speciality` (`description`)
VALUES ('Primary Care'),
       ('Internal Medicine'),
       ('Pediatrics'),
       ('Geriatrics'),
       ('Cardiology'),
       ('Dermatology'),
       ('Endocrinology'),
       ('Gastroenterology'),
       ('Hematology'),
       ('Oncology'),
       ('Nephrology'),
       ('Pulmonology'),
       ('Rheumatology'),
       ('Neurology'),
       ('Obstetrics & Gynecology'),
       ('Ophthalmology'),
       ('Psychiatry'),
       ('Urology');

CREATE DATABASE `pms-appointment`;

USE `pms-appointment`;

CREATE TABLE `appointment`
(
    `id`                  int(11)      NOT NULL AUTO_INCREMENT,
    `patient_id`          int(11)      NOT NULL,
    `doctor_id`           int(11)      NOT NULL,
    `start_time`          datetime     NOT NULL,
    `end_time`            datetime     NOT NULL,
    `duration`            int(11)      NOT NULL,
    `title`               varchar(100) NOT NULL,
    `description`         text,
    `notes`               varchar(100) DEFAULT NULL,
    `follow_up_required`  tinyint(4)   DEFAULT NULL,
    `cancellation_time`   datetime     DEFAULT NULL,
    `cancellation_reason` varchar(100) DEFAULT NULL,
    `type`                varchar(50)  NOT NULL,
    `status`              varchar(50)  NOT NULL,
    `created_at`          datetime     NOT NULL,
    `last_updated`        datetime     DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_appointment_1` (`patient_id`),
    KEY `idx_appointment_2` (`start_time`),
    KEY `idx_appointment_3` (`status`),
    KEY `idx_appointment_4` (`doctor_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = latin1;

CREATE DATABASE `pms-patient`;

USE `pms-patient`;

-- Create 'patient' table if it doesn't exist
CREATE TABLE IF NOT EXISTS `patient`
(
    `id`            int(11)      NOT NULL AUTO_INCREMENT,
    `first_name`    varchar(50)  NOT NULL,
    `last_name`     varchar(50)  NOT NULL,
    `email`         varchar(100) NOT NULL,
    `date_of_birth` date         NOT NULL,
    `address`       varchar(150) DEFAULT NULL,
    `created_at`    datetime     NOT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_patient_1` (`first_name`),
    KEY `idx_patient_2` (`email`),
    KEY `idx_patient_3` (`last_name`)
) ENGINE = InnoDB
  DEFAULT CHARSET = latin1;