INSERT INTO tb_users (id, name, whatsapp_number, active)
VALUES (
    gen_random_uuid(),
    '${brother-name}',
    '${brother-whatsapp}',
    TRUE
);

WITH usuario AS (
    SELECT id FROM tb_users WHERE whatsapp_number = '${brother-whatsapp}'
),
medicamento AS (
    INSERT INTO tb_medications (id, user_id, name, active)
    SELECT gen_random_uuid(), id, '${medication-name}', TRUE
    FROM usuario
    RETURNING id
)
INSERT INTO tb_medication_scheduled_times (medication_id, scheduled_time)
SELECT id, CAST('08:00:00' AS TIME) FROM medicamento
UNION ALL
SELECT id, CAST('20:00:00' AS TIME) FROM medicamento;