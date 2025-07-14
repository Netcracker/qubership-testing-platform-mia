BEGIN;
-- Batch insert missing "Single Processes" sections for all eligible projects
INSERT INTO project_section_configuration (project_id, section_name, place, created_when, modified_when)
SELECT DISTINCT ppc.project_id, 'Single Processes',
                COALESCE((SELECT MAX(place) + 1 FROM project_section_configuration
                          WHERE project_id = ppc.project_id AND parent_id IS NULL), 0),
                NOW(), NOW()
FROM project_processes_configuration ppc
         LEFT JOIN project_section_process_configuration pspc ON ppc.id = pspc.process_id
WHERE pspc.process_id IS NULL
  AND NOT EXISTS (
    SELECT 1 FROM project_section_configuration
    WHERE project_id = ppc.project_id AND section_name = 'Single Processes' AND parent_id IS NULL
);
COMMIT;
