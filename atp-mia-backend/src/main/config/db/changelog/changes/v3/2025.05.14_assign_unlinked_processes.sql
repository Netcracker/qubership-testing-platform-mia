BEGIN;
    -- Batch insert process assignments using a temporary table
    CREATE UNLOGGED TABLE temp_process_assignment AS
SELECT DISTINCT ppc.id AS process_id, psc.id AS section_id,
                ROW_NUMBER() OVER (PARTITION BY psc.id ORDER BY ppc.id) - 1 AS place
FROM project_processes_configuration ppc
         LEFT JOIN project_section_process_configuration pspc ON ppc.id = pspc.process_id
         JOIN project_section_configuration psc ON psc.project_id = ppc.project_id
WHERE psc.section_name = 'Single Processes' AND psc.parent_id IS NULL
  AND pspc.process_id IS NULL;

-- Bulk insert assignments from temporary table
INSERT INTO project_section_process_configuration (process_id, section_id, place)
SELECT process_id, section_id, place FROM temp_process_assignment;

-- Cleanup temporary table
DROP TABLE temp_process_assignment;
COMMIT;
