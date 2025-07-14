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

DO
$$
    BEGIN
        ALTER TABLE pot_session
            DROP CONSTRAINT IF EXISTS RECORDING_SESSION_PARENT_FK;

        DROP TABLE IF EXISTS pot_db_column_names CASCADE;
        DROP TABLE IF EXISTS pot_db_columns CASCADE;
        DROP TABLE IF EXISTS pot_db_table_row CASCADE;
        DROP TABLE IF EXISTS pot_db_table CASCADE;
        DROP TABLE IF EXISTS pot_table_marker_row_count CASCADE;
        DROP TABLE IF EXISTS pot_table_marker_column_status CASCADE;
        DROP TABLE IF EXISTS pot_table_marker_result CASCADE;
        DROP TABLE IF EXISTS pot_warn_marker CASCADE;
        DROP TABLE IF EXISTS pot_passed_marker CASCADE;
        DROP TABLE IF EXISTS pot_failed_marker CASCADE;
        DROP TABLE IF EXISTS pot_marker CASCADE;
        DROP TABLE IF EXISTS pot_link CASCADE;
        DROP TABLE IF EXISTS pot_execution_error CASCADE;
        DROP TABLE IF EXISTS pot_process_status CASCADE;
        DROP TABLE IF EXISTS pot_sql_response CASCADE;

        CREATE OR REPLACE FUNCTION check_unique_project_file() RETURNS trigger AS
        $func$
        BEGIN
            IF NEW.directory_id IS NULL THEN
                IF NEW.id IS NULL THEN
                    IF EXISTS(SELECT *
                              FROM project_file f
                              WHERE f.project_id IS NOT DISTINCT FROM NEW.project_id
                                AND f.file_name IS NOT DISTINCT FROM NEW.file_name
                                AND f.directory_id IS NULL) THEN
                        RAISE EXCEPTION 'New file with the name "%" already exist in root directory', NEW.file_name;
                    END IF;
                ELSE
                    IF EXISTS(SELECT *
                              FROM project_file f
                              WHERE f.project_id IS NOT DISTINCT FROM NEW.project_id
                                AND f.file_name IS NOT DISTINCT FROM NEW.file_name
                                AND f.directory_id IS NULL
                                AND f.id <> NEW.id) THEN
                        RAISE EXCEPTION 'File with the name "%" already exist in root directory', NEW.file_name;
                    END IF;
                END IF;
            ELSE
                IF NEW.id IS NULL THEN
                    IF EXISTS(SELECT *
                              FROM project_file f
                              WHERE f.project_id IS NOT DISTINCT FROM NEW.project_id
                                AND f.file_name IS NOT DISTINCT FROM NEW.file_name
                                AND f.directory_id IS NOT NULL
                                AND f.directory_id IS NOT DISTINCT FROM NEW.directory_id) THEN
                        RAISE EXCEPTION 'New file with the name "%" already exist in current directory "%"', NEW.file_name, NEW.directory_id;
                    END IF;
                ELSE
                    IF EXISTS(SELECT *
                              FROM project_file f
                              WHERE f.project_id IS NOT DISTINCT FROM NEW.project_id
                                AND f.file_name IS NOT DISTINCT FROM NEW.file_name
                                AND f.directory_id IS NOT NULL
                                AND f.directory_id IS NOT DISTINCT FROM NEW.directory_id
                                AND f.id <> NEW.id) THEN
                        RAISE EXCEPTION 'File with the name "%" already exist in current directory "%"', NEW.file_name, NEW.directory_id;
                    END IF;
                END IF;
            END IF;
            RETURN NEW;
        END;
        $func$ LANGUAGE plpgsql;

        DROP TRIGGER IF EXISTS PROJECT_FILE_UNIQUE_PATH ON project_file;
        CREATE TRIGGER PROJECT_FILE_UNIQUE_PATH
            BEFORE INSERT OR UPDATE
            ON project_file
            FOR EACH ROW
        EXECUTE PROCEDURE check_unique_project_file();

        CREATE OR REPLACE FUNCTION check_unique_project_directory() RETURNS trigger AS
        $func$
        BEGIN
            IF NEW.parent_id IS NULL THEN
                IF NEW.id IS NULL THEN
                    IF EXISTS(SELECT *
                              FROM project_directory d
                              WHERE d.project_id IS NOT DISTINCT FROM NEW.project_id
                                AND d.directory_name IS NOT DISTINCT FROM NEW.directory_name
                                AND d.parent_id IS NULL) THEN
                        RAISE EXCEPTION 'New directory with the name "%" already exist in root directory', NEW.directory_name;
                    END IF;
                ELSE
                    IF EXISTS(SELECT *
                              FROM project_directory d
                              WHERE d.project_id IS NOT DISTINCT FROM NEW.project_id
                                AND d.directory_name IS NOT DISTINCT FROM NEW.directory_name
                                AND d.parent_id IS NULL
                                AND d.id <> NEW.id) THEN
                        RAISE EXCEPTION 'Directory with the name "%" already exist in root directory', NEW.directory_name;
                    END IF;
                END IF;
            ELSE
                IF NEW.id IS NULL THEN
                    IF EXISTS(SELECT *
                              FROM project_directory d
                              WHERE d.project_id IS NOT DISTINCT FROM NEW.project_id
                                AND d.directory_name IS NOT DISTINCT FROM NEW.directory_name
                                AND d.parent_id IS NOT NULL
                                AND d.parent_id IS NOT DISTINCT FROM NEW.parent_id) THEN
                        RAISE EXCEPTION 'New directory with the name "%" already exist in current directory "%"', NEW.directory_name, NEW.parent_id;
                    END IF;
                ELSE
                    IF EXISTS(SELECT *
                              FROM project_directory d
                              WHERE d.project_id IS NOT DISTINCT FROM NEW.project_id
                                AND d.directory_name IS NOT DISTINCT FROM NEW.directory_name
                                AND d.parent_id IS NOT NULL
                                AND d.parent_id IS NOT DISTINCT FROM NEW.parent_id
                                AND d.id <> NEW.id) THEN
                        RAISE EXCEPTION 'Directory with the name "%" already exist in current directory "%"', NEW.directory_name, NEW.parent_id;
                    END IF;
                END IF;
            END IF;
            RETURN NEW;
        END;
        $func$ LANGUAGE plpgsql;

        DROP TRIGGER IF EXISTS PROJECT_DIRECTORY_UNIQUE_PATH ON project_directory;
        CREATE TRIGGER PROJECT_DIRECTORY_UNIQUE_PATH
            BEFORE INSERT OR UPDATE
            ON project_directory
            FOR EACH ROW
        EXECUTE PROCEDURE check_unique_project_directory();

        CREATE OR REPLACE FUNCTION check_unique_project_section() RETURNS trigger AS
        $func$
        BEGIN
            IF NEW.parent_id IS NULL THEN
                IF NEW.id IS NULL THEN
                    IF EXISTS(SELECT *
                              FROM project_section_configuration s
                              WHERE s.project_id IS NOT DISTINCT FROM NEW.project_id
                                AND s.section_name IS NOT DISTINCT FROM NEW.section_name
                                AND s.parent_id IS NULL) THEN
                        RAISE EXCEPTION 'New section with the name "%" already exist in root section', NEW.section_name;
                    END IF;
                ELSE
                    IF EXISTS(SELECT *
                              FROM project_section_configuration s
                              WHERE s.project_id IS NOT DISTINCT FROM NEW.project_id
                                AND s.section_name IS NOT DISTINCT FROM NEW.section_name
                                AND s.parent_id IS NULL
                                AND s.id <> NEW.id) THEN
                        RAISE EXCEPTION 'Section with the name "%" already exist in root section', NEW.section_name;
                    END IF;
                END IF;
            ELSE
                IF NEW.id IS NULL THEN
                    IF EXISTS(SELECT *
                              FROM project_section_configuration s
                              WHERE s.project_id IS NOT DISTINCT FROM NEW.project_id
                                AND s.section_name IS NOT DISTINCT FROM NEW.section_name
                                AND s.parent_id IS NOT NULL
                                AND s.parent_id IS NOT DISTINCT FROM NEW.parent_id) THEN
                        RAISE EXCEPTION 'New section with the name "%" already exist in current section "%"', NEW.section_name, NEW.parent_id;
                    END IF;
                ELSE
                    IF EXISTS(SELECT *
                              FROM project_section_configuration s
                              WHERE s.project_id IS NOT DISTINCT FROM NEW.project_id
                                AND s.section_name IS NOT DISTINCT FROM NEW.section_name
                                AND s.parent_id IS NOT NULL
                                AND s.parent_id IS NOT DISTINCT FROM NEW.parent_id
                                AND s.id <> NEW.id) THEN
                        RAISE EXCEPTION 'Section with the name "%" already exist in current section "%"', NEW.section_name, NEW.parent_id;
                    END IF;
                END IF;
            END IF;
            RETURN NEW;
        END;
        $func$ LANGUAGE plpgsql;


        DROP TRIGGER IF EXISTS PROJECT_SECTION_UNIQUE_PATH ON project_section_configuration;
        CREATE TRIGGER PROJECT_SECTION_UNIQUE_PATH
            BEFORE INSERT OR UPDATE
            ON project_section_configuration
            FOR EACH ROW
        EXECUTE PROCEDURE check_unique_project_section();

        DROP INDEX IF EXISTS PROJECT_SECTION_UNIQUE_PATH_1;
        DROP INDEX IF EXISTS PROJECT_SECTION_UNIQUE_PATH_2;

        ALTER TABLE project_section_configuration
            DROP CONSTRAINT IF EXISTS project_section_configuration_project_id_fkey,
            DROP CONSTRAINT IF EXISTS SECTION_CONFIGURATION_FK;

        ALTER TABLE project_section_configuration
            ADD CONSTRAINT SECTION_CONFIGURATION_FK
                FOREIGN KEY (project_id) REFERENCES project_configuration (project_id) ON DELETE CASCADE ON UPDATE CASCADE;
    END
$$;
