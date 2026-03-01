CREATE TABLE tb_medication_scheduled_times (
    medication_id UUID NOT NULL,
    scheduled_time TIME NOT NULL,
    CONSTRAINT fk_scheduled_time_medication FOREIGN KEY (medication_id) REFERENCES tb_medications(id)
);