--   Copyright 2024-2025 NetCracker Technology Corporation
--
--   Licensed under the Apache License, Version 2.0 (the "License");
--   you may not use this file except in compliance with the License.
--   You may obtain a copy of the License at
--
--        http://www.apache.org/licenses/LICENSE-2.0
--
--   Unless required by applicable law or agreed to in writing, software
--   distributed under the License is distributed on an "AS IS" BASIS,
--   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
--   See the License for the specific language governing permissions and
--   limitations under the License.
--

BEGIN;

-- Remove temporary table if it exists from previous runs
DROP TABLE IF EXISTS tmp_project_section_resequence_count;

-- Identify sections with duplicate positions under the same parent
WITH numbered_rows AS (
    SELECT id, ROW_NUMBER() OVER (PARTITION BY project_id, parent_id ORDER BY place) - 1 AS new_place
    FROM project_section_configuration
)
SELECT COUNT(*) INTO TEMPORARY TABLE tmp_project_section_resequence_count FROM numbered_rows nr
JOIN project_section_configuration psc ON psc.id = nr.id WHERE psc.place != nr.new_place;

-- Execute updates only if duplicates are found
DO $$
DECLARE
    affected_rows INTEGER;
    rows_to_update_count INTEGER;
BEGIN
    SELECT count INTO rows_to_update_count FROM tmp_project_section_resequence_count;
    RAISE NOTICE 'Identified % sections requiring position resequencing within their parent groups',
          rows_to_update_count;

    IF rows_to_update_count > 0 THEN
        -- Resequence positions to ensure consecutive ordering within each parent
        WITH numbered_rows AS (
            SELECT id, section_name, project_id, parent_id, place,
            ROW_NUMBER() OVER (PARTITION BY project_id, parent_id ORDER BY place) - 1 AS new_place
            FROM project_section_configuration
        )
        UPDATE project_section_configuration psc
        SET place = nr.new_place FROM numbered_rows nr
        WHERE psc.id = nr.id AND psc.place != nr.new_place;

        GET DIAGNOSTICS affected_rows = ROW_COUNT;

        IF affected_rows != rows_to_update_count THEN
            RAISE NOTICE 'Rolling back: Update affected % sections instead of expected % under their respective parents',
                affected_rows, rows_to_update_count;
            ROLLBACK;
            -- Clean up temporary table even on rollback
            DROP TABLE IF EXISTS tmp_project_section_resequence_count;
            RETURN;
        END IF;
        RAISE NOTICE 'Successfully resequenced % section positions', affected_rows;
    END IF;
END $$;

-- Clean up temporary table before committing
DROP TABLE IF EXISTS tmp_project_section_resequence_count;

COMMIT;
