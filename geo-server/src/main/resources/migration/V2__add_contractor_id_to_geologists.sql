-- V2__add_contractor_id_to_geologists.sql

-- ALTER TABLE ref_geologists
--     ADD COLUMN contractor_id BIGINT;

-- ALTER TABLE ref_geologists
--     ADD CONSTRAINT fk_geologist_contractor
--         FOREIGN KEY (contractor_id)
--             REFERENCES ref_contractors(id)
--             ON DELETE CASCADE;

-- Если хочешь сделать поле NOT NULL, сначала заполни существующие записи
-- UPDATE ref_geologists SET contractor_id = 1 WHERE contractor_id IS NULL;
-- Потом:
-- ALTER TABLE ref_geologists ALTER COLUMN contractor_id SET NOT NULL;