UPDATE ST_Group
SET
    state = 'VALID',
    stateSaveDate = CURRENT_TIMESTAMP;

ALTER TABLE ST_Group ALTER COLUMN state SET NOT NULL;
ALTER TABLE ST_Group ALTER COLUMN stateSaveDate SET NOT NULL;