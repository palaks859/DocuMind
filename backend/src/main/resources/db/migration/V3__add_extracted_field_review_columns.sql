ALTER TABLE extracted_fields
    ADD COLUMN IF NOT EXISTS review_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    ADD COLUMN IF NOT EXISTS reviewed_by UUID REFERENCES users(id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS reviewed_at TIMESTAMPTZ;

ALTER TABLE extracted_fields
    DROP CONSTRAINT IF EXISTS ck_extracted_fields_review_status;

ALTER TABLE extracted_fields
    ADD CONSTRAINT ck_extracted_fields_review_status
    CHECK (review_status IN ('PENDING', 'APPROVED', 'REJECTED', 'EDITED'));
