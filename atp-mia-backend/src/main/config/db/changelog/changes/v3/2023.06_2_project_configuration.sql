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
        CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

        CREATE TABLE IF NOT EXISTS project_configuration
        (
            project_id             uuid    NOT NULL UNIQUE,
            project_name           text    NOT NULL,
            git_url                text    NULL,
            validation_result      text    NULL,
            primary_migration_done boolean NULL DEFAULT FALSE,
            CONSTRAINT project_configuration_pk PRIMARY KEY (project_id)
        );

        CREATE TABLE IF NOT EXISTS project_common_configuration
        (
            project_id                    uuid  NOT NULL UNIQUE,
            default_system                text  NOT NULL DEFAULT 'Billing System',
            use_variables_inside_variable boolean        DEFAULT FALSE,
            variable_format               text  NULL     DEFAULT ':VARIABLE_NAME',
            save_files_to_working_dir     boolean        DEFAULT FALSE,
            save_sql_tables_to_file       boolean        DEFAULT TRUE,
            common_variables              jsonb NOT NULL DEFAULT '{}',
            next_bill_date_sql            text  NOT NULL DEFAULT 'SELECT NEXT_BILL_DTM FROM ACCOUNT WHERE ACCOUNT_NUM ='':accountNumber''',
            reset_cache_sql               text  NOT NULL DEFAULT '{call gnvsessiongparams.clearcache()}',
            ethalon_files_path            text  NOT NULL DEFAULT 'etalon_files',
            external_environment_prefix   text  NOT NULL DEFAULT '',
            command_shell_separator       text  NOT NULL DEFAULT '\n',
            geneva_date_mask              text,
            ssh_rsa_file_path             text  NULL,
            lines_amount                  int4           DEFAULT 3,
            CONSTRAINT PROJECT_COMMON_CONFIGURATION_PK PRIMARY KEY (project_id),
            CONSTRAINT PROJECT_COMMON_CONFIGURATION_FK
                FOREIGN KEY (project_id)
                    REFERENCES project_configuration (project_id) ON DELETE CASCADE ON UPDATE CASCADE
        );

        CREATE TABLE IF NOT EXISTS project_command_prefix
        (
            project_id  uuid  NOT NULL,
            system_name text  NOT NULL,
            prefixes    jsonb NOT NULL DEFAULT '{}',
            CONSTRAINT PROJECT_COMMAND_PREFIX_PK PRIMARY KEY (project_id, system_name),
            CONSTRAINT PROJECT_COMMON_CONFIGURATION_FK
                FOREIGN KEY (project_id)
                    REFERENCES project_common_configuration (project_id) ON DELETE CASCADE
        );

        CREATE TABLE IF NOT EXISTS project_header_configuration
        (
            project_id                     uuid  NOT NULL UNIQUE,
            show_geneva_date_block         boolean        DEFAULT TRUE,
            show_working_directory         boolean        DEFAULT TRUE,
            show_reset_db_cache            boolean        DEFAULT FALSE,
            show_update_config             boolean        DEFAULT TRUE,
            show_test_data                 boolean        DEFAULT FALSE,
            show_time_shifting             boolean        DEFAULT FALSE,
            export_toggle_default_position boolean        DEFAULT TRUE,
            enable_update_flow_json_config boolean        DEFAULT TRUE,
            working_directory              text  NOT NULL DEFAULT '/tmp/TA/',
            system_switchers               jsonb NOT NULL DEFAULT '[
              {
                "value": false,
                "actionType": "SSH",
                "name": "fullTraceMode",
                "display": "Full Trace On/Off",
                "actionTrue": "export TRACE_LEVEL=FULL\nexport TRACE_ALL=ON"
              },
              {
                "value": false,
                "name": "needDos2Unix",
                "display": "needDos2Unix"
              },
              {
                "value": true,
                "name": "replaceFileOnUploadAttachment",
                "display": "Replace/Add timestamp on upload attachment",
                "actionTrue": "replace",
                "actionFalse": "add timestamp"
              },
              {
                "value": true,
                "name": "stopOnFail",
                "display": "Stop compound if one of processes is fail"
              }
            ]',
            switchers                      jsonb NOT NULL DEFAULT '[]',
            CONSTRAINT project_header_configuration_pk PRIMARY KEY (project_id),
            CONSTRAINT HEADER_CONFIGURATION_FK
                FOREIGN KEY (project_id)
                    REFERENCES project_configuration (project_id) ON DELETE CASCADE ON UPDATE CASCADE
        );

        CREATE TABLE IF NOT EXISTS project_pot_header_configuration
        (
            project_id uuid  NOT NULL UNIQUE,
            headers    jsonb NOT NULL DEFAULT '[]',
            CONSTRAINT project_pot_header_configuration_pk PRIMARY KEY (project_id),
            CONSTRAINT POT_HEADER_CONFIGURATION_FK
                FOREIGN KEY (project_id)
                    REFERENCES project_configuration (project_id) ON DELETE CASCADE ON UPDATE CASCADE
        );

        CREATE TABLE IF NOT EXISTS project_section_configuration
        (
            id           uuid NOT NULL DEFAULT uuid_generate_v4() PRIMARY KEY,
            project_id   uuid NOT NULL REFERENCES project_configuration (project_id),
            section_name text NOT NULL,
            parent_id    uuid NULL REFERENCES project_section_configuration (id) ON DELETE CASCADE ON UPDATE CASCADE,
            place        int4 NULL

        );
        CREATE UNIQUE INDEX IF NOT EXISTS PROJECT_SECTION_UNIQUE_PATH_1
            ON project_section_configuration (project_id, section_name, parent_id)
            WHERE parent_id IS NOT NULL;

        CREATE UNIQUE INDEX IF NOT EXISTS PROJECT_SECTION_UNIQUE_PATH_2
            ON project_section_configuration (project_id, section_name)
            WHERE parent_id IS NULL;

        CREATE TABLE IF NOT EXISTS project_compounds_configuration
        (
            id             uuid NOT NULL DEFAULT uuid_generate_v4() UNIQUE,
            project_id     uuid NOT NULL,
            compound_name  text NOT NULL,
            refer_to_input text NULL,
            CONSTRAINT PROJECT_COMPOUNDS_CONFIGURATION_PK PRIMARY KEY (project_id, compound_name),
            CONSTRAINT COMPOUND_CONFIGURATION_FK
                FOREIGN KEY (project_id)
                    REFERENCES project_configuration (project_id) ON DELETE CASCADE ON UPDATE CASCADE
        );

        CREATE TABLE IF NOT EXISTS project_section_compound_configuration
        (
            section_id  uuid NOT NULL,
            compound_id uuid NOT NULL,
            place       int4 NOT NULL,
            CONSTRAINT PROJECT_SECTION_COMPOUND_CONFIGURATION_PK PRIMARY KEY (section_id, compound_id),
            CONSTRAINT COMPOUNDS_FK
                FOREIGN KEY (compound_id)
                    REFERENCES project_compounds_configuration (id) ON DELETE CASCADE ON UPDATE CASCADE,
            CONSTRAINT SECTION_FOR_COMPOUND_FK
                FOREIGN KEY (section_id)
                    REFERENCES project_section_configuration (id) ON DELETE CASCADE ON UPDATE CASCADE
        );

        CREATE TABLE IF NOT EXISTS project_processes_configuration
        (
            id               uuid  NOT NULL DEFAULT uuid_generate_v4() UNIQUE,
            project_id       uuid  NOT NULL,
            file_name        text  NULL,
            process_name     text  NOT NULL,
            process_settings jsonb NOT NULL,
            CONSTRAINT PROJECT_PROCESSES_CONFIGURATION_PK PRIMARY KEY (project_id, process_name),
            CONSTRAINT PROCESSES_CONFIGURATION_FK
                FOREIGN KEY (project_id)
                    REFERENCES project_configuration (project_id) ON DELETE CASCADE ON UPDATE CASCADE
        );

        CREATE TABLE IF NOT EXISTS project_section_process_configuration
        (
            section_id uuid NOT NULL,
            process_id uuid NOT NULL,
            place      int4 NOT NULL,
            CONSTRAINT PROJECT_SECTION_PROCESS_CONFIGURATION_PK PRIMARY KEY (section_id, process_id),
            CONSTRAINT PROCESSES_IN_SECTION_FK
                FOREIGN KEY (process_id)
                    REFERENCES project_processes_configuration (id) ON DELETE CASCADE ON UPDATE CASCADE,
            CONSTRAINT SECTION_FOR_PROCESS_FK
                FOREIGN KEY (section_id)
                    REFERENCES project_section_configuration (id) ON DELETE CASCADE ON UPDATE CASCADE
        );

        CREATE TABLE IF NOT EXISTS project_compound_process_configuration
        (
            compound_id uuid NOT NULL,
            process_id  uuid NOT NULL,
            place       int4 NOT NULL,
            CONSTRAINT PROJECT_COMPOUND_PROCESS_CONFIGURATION_PK PRIMARY KEY (compound_id, process_id, place),
            CONSTRAINT PROCESSES_IN_COMPOUND_FK
                FOREIGN KEY (process_id)
                    REFERENCES project_processes_configuration (id) ON DELETE CASCADE ON UPDATE CASCADE,
            CONSTRAINT COMPOUND_FOR_PROCESS_FK
                FOREIGN KEY (compound_id)
                    REFERENCES project_compounds_configuration (id) ON DELETE CASCADE ON UPDATE CASCADE
        );


        CREATE TABLE IF NOT EXISTS project_directory
        (
            id             uuid NOT NULL DEFAULT uuid_generate_v4(),
            project_id     uuid NOT NULL,
            directory_name text NOT NULL,
            parent_id      uuid NULL,
            CONSTRAINT PROJECT_DIRECTORY_CONFIGURATION_PK PRIMARY KEY (id),
            CONSTRAINT PROJECT_DIRECTORY_UNIQUE_PATH UNIQUE (project_id, directory_name, parent_id),
            CONSTRAINT PROJECT_DIRECTORY_FK
                FOREIGN KEY (project_id)
                    REFERENCES project_configuration (project_id) ON DELETE CASCADE ON UPDATE CASCADE
        );

        CREATE OR REPLACE FUNCTION check_unique_project_directory() RETURNS trigger AS
        $func$
        BEGIN
            IF EXISTS(SELECT *
                      FROM project_directory d
                      WHERE d.project_id IS NOT DISTINCT FROM NEW.project_id
                        AND d.directory_name IS NOT DISTINCT FROM NEW.directory_name
                        AND d.parent_id IS NOT DISTINCT FROM NEW.parent_id
                        AND d.id <> NEW.id) THEN
                RAISE EXCEPTION 'Directory with the same name already exist in current directory';
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

        ALTER TABLE project_directory
            DROP CONSTRAINT IF EXISTS DIRECTORY_PARENT_FK;
        ALTER TABLE project_directory
            ADD CONSTRAINT DIRECTORY_PARENT_FK
                FOREIGN KEY (parent_id) REFERENCES project_directory (id) ON DELETE CASCADE ON UPDATE CASCADE;

        CREATE TABLE IF NOT EXISTS project_file
        (
            id               uuid NOT NULL DEFAULT uuid_generate_v4() UNIQUE,
            project_id       uuid NOT NULL,
            file_name        text NOT NULL,
            directory_id     uuid,
            gridfs_object_id text NOT NULL,
            CONSTRAINT PROJECT_FILE_PK PRIMARY KEY (id),
            CONSTRAINT PROJECT_FILE_UNIQUE_PATH UNIQUE (project_id, file_name, directory_id),
            CONSTRAINT PROJECT_FILE_FK
                FOREIGN KEY (project_id)
                    REFERENCES project_configuration (project_id) ON DELETE CASCADE ON UPDATE CASCADE,
            CONSTRAINT PROJECT_FILE_TO_DIRECTORY_FK
                FOREIGN KEY (directory_id)
                    REFERENCES project_directory (id) ON DELETE CASCADE ON UPDATE CASCADE
        );

        CREATE OR REPLACE FUNCTION check_unique_project_file() RETURNS trigger AS
        $func$
        BEGIN
            IF EXISTS(SELECT *
                      FROM project_file f
                      WHERE f.project_id IS NOT DISTINCT FROM NEW.project_id
                        AND f.file_name IS NOT DISTINCT FROM NEW.file_name
                        AND f.directory_id IS NOT DISTINCT FROM NEW.directory_id
                        AND f.id <> NEW.id) THEN
                RAISE EXCEPTION 'File with the same name already exist in current directory';
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

        ALTER TABLE pot_session
            DROP CONSTRAINT IF EXISTS RECORDING_SESSION_PARENT_FK;
        ALTER TABLE pot_session
            DROP CONSTRAINT IF EXISTS "RECORDING_SESSION_PARENT_FK";
        ALTER TABLE pot_session
            ADD CONSTRAINT RECORDING_SESSION_PARENT_FK
                FOREIGN KEY (project_id)
                    REFERENCES project_configuration (project_id) ON DELETE CASCADE ON UPDATE CASCADE;
    END
$$;
