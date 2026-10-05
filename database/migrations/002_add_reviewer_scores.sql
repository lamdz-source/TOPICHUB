-- Run once against student_topic_management after applying the base SQL schema.
-- This stores the review score/comment required by registration_periods.review_deadline.
ALTER TABLE topic_reviewers
    ADD COLUMN review_score DECIMAL(4,2) NULL,
    ADD COLUMN review_comment TEXT NULL,
    ADD COLUMN review_submitted_at DATETIME NULL,
    ADD CONSTRAINT chk_topic_reviewers_review_score_0_10
        CHECK (review_score IS NULL OR (review_score >= 0 AND review_score <= 10));
