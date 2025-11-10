CREATE TABLE github_events
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_type VARCHAR(255),
    repo_name  VARCHAR(255),
    created_at VARCHAR(255)
);