ALTER TABLE saged.demands
    ADD COLUMN equipment_name  VARCHAR(255),
    ADD COLUMN equipment_model VARCHAR(128),
    ADD COLUMN is_rented       BOOLEAN;
