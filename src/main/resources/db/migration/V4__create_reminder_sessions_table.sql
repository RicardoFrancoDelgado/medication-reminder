CREATE TABLE tb_reminder_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    medication_id UUID NOT NULL,
    scheduled_time TIME NOT NULL,
    sent_at TIMESTAMP,
    responded_at TIMESTAMP,
    status VARCHAR(20) NOT NULL,
    retry_count INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT fk_session_medication FOREIGN KEY (medication_id) REFERENCES tb_medications(id)
);