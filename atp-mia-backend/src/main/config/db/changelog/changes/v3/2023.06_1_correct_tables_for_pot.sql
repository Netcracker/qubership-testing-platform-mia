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

        CREATE TABLE IF NOT EXISTS pot_session
        (
            id         uuid         NOT NULL,
            project_id uuid         NOT NULL,
            created_at TIMESTAMP    NOT NULL DEFAULT now(),
            created_by varchar(255) NOT NULL,
            CONSTRAINT POT_SESSION_PKEY PRIMARY KEY (id)
        );

        CREATE TABLE IF NOT EXISTS pot_execution_step
        (
            id               uuid         NOT NULL,
            executed_command text         NULL,
            step_name        varchar(255) NULL,
            pot_session_id   uuid         NULL,
            environment_name text         NULL,
            CONSTRAINT POT_EXECUTION_STEP_PKEY PRIMARY KEY (id),
            CONSTRAINT POT_EXECUTION_STEP_TO_RECORDING_SESSION_FK FOREIGN KEY (pot_session_id)
                REFERENCES pot_session (id) ON DELETE CASCADE ON UPDATE CASCADE
        );

        CREATE TABLE IF NOT EXISTS pot_sql_response
        (
            id                    uuid         NOT NULL,
            connection_info       jsonb        NULL,
            description           text         NULL,
            internal_path_to_file varchar(255) NULL,
            query                 text         NULL,
            records               int4         NULL,
            save_to_word_file     bool         NULL,
            save_to_zip_file      bool         NULL,
            table_name            varchar(255) NULL,
            pot_execution_step_id uuid         NULL,
            CONSTRAINT POT_SQL_RESPONSE_PKEY PRIMARY KEY (id),
            CONSTRAINT POT_SQL_RESPONSE_TO_EXECUTION_STEP_FK FOREIGN KEY (pot_execution_step_id)
                REFERENCES pot_execution_step (id) ON DELETE CASCADE ON UPDATE CASCADE
        );

        CREATE TABLE IF NOT EXISTS pot_process_status
        (
            id                    uuid NOT NULL,
            status                int2 NULL,
            pot_execution_step_id uuid NULL,
            CONSTRAINT POT_PROCESS_STATUS_PKEY PRIMARY KEY (id),
            CONSTRAINT POT_PROCESS_STATUS_TO_EXECUTION_STEP_FK FOREIGN KEY (pot_execution_step_id)
                REFERENCES pot_execution_step (id) ON DELETE CASCADE ON UPDATE CASCADE
        );

        CREATE TABLE IF NOT EXISTS pot_execution_error
        (
            id                    uuid NOT NULL,
            error_code            text NULL,
            http_status           int4 NULL,
            message               text NULL,
            pot_execution_step_id uuid NULL,
            CONSTRAINT POT_EXECUTION_ERROR_PKEY PRIMARY KEY (id),
            CONSTRAINT POT_EXECUTION_ERROR_TO_EXECUTION_STEP_FK FOREIGN KEY (pot_execution_step_id)
                REFERENCES pot_execution_step (id) ON DELETE CASCADE ON UPDATE CASCADE
        );

        CREATE TABLE IF NOT EXISTS pot_link
        (
            id                    uuid         NOT NULL,
            "name"                varchar(255) NULL,
            "path"                varchar(255) NULL,
            pot_execution_step_id uuid         NULL,
            pot_sql_response_id   uuid         NULL,
            CONSTRAINT POT_LINK_PKEY PRIMARY KEY (id),
            CONSTRAINT POT_LINK_TO_POT_EXECUTION_STEP_FK FOREIGN KEY (pot_execution_step_id)
                REFERENCES pot_execution_step (id) ON DELETE CASCADE ON UPDATE CASCADE,
            CONSTRAINT POT_LINK_TO_POT_SQL_RESPONSE_FK FOREIGN KEY (pot_sql_response_id)
                REFERENCES pot_sql_response (id) ON DELETE CASCADE ON UPDATE CASCADE
        );

        CREATE TABLE IF NOT EXISTS pot_marker
        (
            id                    uuid NOT NULL,
            fail_when_no_passed   bool NULL,
            pot_process_status_id uuid NULL,
            CONSTRAINT POT_MARKER_PKEY PRIMARY KEY (id),
            CONSTRAINT POT_MARKER_TO_POT_PROCESS_STATUS_FK FOREIGN KEY (pot_process_status_id)
                REFERENCES pot_process_status (id) ON DELETE CASCADE ON UPDATE CASCADE
        );

        CREATE TABLE IF NOT EXISTS pot_failed_marker
        (
            pot_marker_id uuid NOT NULL,
            failed_marker text NULL,
            CONSTRAINT POT_FAILED_MARKER_TO_POT_MARKER_FK FOREIGN KEY (pot_marker_id)
                REFERENCES pot_marker (id) ON DELETE CASCADE ON UPDATE CASCADE
        );

        CREATE TABLE IF NOT EXISTS pot_passed_marker
        (
            pot_marker_id uuid NOT NULL,
            passed_marker text NULL,
            CONSTRAINT POT_PASSED_MARKER_TO_POT_MARKER_FK FOREIGN KEY (pot_marker_id)
                REFERENCES pot_marker (id) ON DELETE CASCADE ON UPDATE CASCADE
        );

        CREATE TABLE IF NOT EXISTS pot_warn_marker
        (
            pot_marker_id uuid NOT NULL,
            warn_marker   text NULL,
            CONSTRAINT POT_WARNMARKER_TO_POT_MARKER_FK FOREIGN KEY (pot_marker_id)
                REFERENCES pot_marker (id) ON DELETE CASCADE ON UPDATE CASCADE
        );

        CREATE TABLE IF NOT EXISTS pot_table_marker_result
        (
            id                  uuid NOT NULL,
            pot_sql_response_id uuid NULL,
            CONSTRAINT POT_TABLE_MARKER_RESULT_PKEY PRIMARY KEY (id),
            CONSTRAINT POT_TABLE_MARKER_RESULT_TO_POT_SQL_RESPONSE_FK FOREIGN KEY (pot_sql_response_id)
                REFERENCES pot_sql_response (id) ON DELETE CASCADE ON UPDATE CASCADE
        );

        CREATE TABLE IF NOT EXISTS pot_table_marker_column_status
        (
            id                         uuid         NOT NULL,
            actual_result              text         NULL,
            column_name                varchar(255) NULL,
            expected_result            text         NULL,
            status                     int4         NULL,
            pot_table_marker_result_id uuid         NULL,
            CONSTRAINT POT_TABLE_MARKER_COLUMN_STATUS_PKEY PRIMARY KEY (id),
            CONSTRAINT POT_TABLE_MARKER_COLUMN_STATUS_TO_POT_TABLE_MARKER_RESULT_FK
                FOREIGN KEY (pot_table_marker_result_id)
                    REFERENCES pot_table_marker_result (id) ON DELETE CASCADE ON UPDATE CASCADE
        );

        CREATE TABLE IF NOT EXISTS pot_table_marker_row_count
        (
            id                         uuid         NOT NULL,
            actual_result              varchar(255) NULL,
            expected_result            varchar(255) NULL,
            status                     int4         NULL,
            pot_table_marker_result_id uuid         NULL,
            CONSTRAINT POT_TABLE_MARKER_ROW_COUNT_PKEY PRIMARY KEY (id),
            CONSTRAINT POT_TABLE_MARKER_ROW_COUNT_TO_POT_TABLE_MARKER_RESULT_FK FOREIGN KEY (pot_table_marker_result_id)
                REFERENCES pot_table_marker_result (id) ON DELETE CASCADE ON UPDATE CASCADE
        );

        CREATE TABLE IF NOT EXISTS pot_db_table
        (
            id                  uuid NOT NULL,
            pot_sql_response_id uuid NULL,
            CONSTRAINT POT_DB_TABLE_PKEY PRIMARY KEY (id),
            CONSTRAINT POT_DB_TABLE_TO_POT_SQL_RESPONSE_FK FOREIGN KEY (pot_sql_response_id)
                REFERENCES pot_sql_response (id) ON DELETE CASCADE ON UPDATE CASCADE
        );

        CREATE TABLE IF NOT EXISTS pot_db_table_row
        (
            id              uuid NOT NULL,
            pot_db_table_id uuid NULL,
            CONSTRAINT POT_DB_TABLE_ROW_PKEY PRIMARY KEY (id),
            CONSTRAINT POT_DB_TABLE_ROW_TO_POT_DB_TABLE_FK FOREIGN KEY (pot_db_table_id)
                REFERENCES pot_db_table (id) ON DELETE CASCADE ON UPDATE CASCADE
        );

        CREATE TABLE IF NOT EXISTS pot_db_columns
        (
            pot_db_table_row_id uuid NOT NULL,
            db_columns          text NOT NULL,
            CONSTRAINT POT_DB_COLUMNS_TO_POT_DB_TABLE_ROW_FK FOREIGN KEY (pot_db_table_row_id)
                REFERENCES pot_db_table_row (id) ON DELETE CASCADE ON UPDATE CASCADE
        );

        CREATE TABLE IF NOT EXISTS pot_db_column_names
        (
            pot_db_table_id uuid         NOT NULL,
            db_column_names varchar(255) NOT NULL,
            CONSTRAINT POT_DB_COLUMN_NAMES_TO_POT_DB_TABLE_FK FOREIGN KEY (pot_db_table_id)
                REFERENCES pot_db_table (id) ON DELETE CASCADE ON UPDATE CASCADE
        );

        CREATE TABLE IF NOT EXISTS recording_session
        (
            id uuid NOT NULL UNIQUE
        );
        TRUNCATE TABLE recording_session CASCADE;
        DROP TABLE IF EXISTS db_column_names CASCADE;
        DROP TABLE IF EXISTS db_columns CASCADE;
        DROP TABLE IF EXISTS db_table_row CASCADE;
        DROP TABLE IF EXISTS db_table CASCADE;
        DROP TABLE IF EXISTS table_marker_row_count CASCADE;
        DROP TABLE IF EXISTS table_marker_column_status CASCADE;
        DROP TABLE IF EXISTS table_marker_result CASCADE;
        DROP TABLE IF EXISTS warn_marker CASCADE;
        DROP TABLE IF EXISTS passed_marker CASCADE;
        DROP TABLE IF EXISTS failed_marker CASCADE;
        DROP TABLE IF EXISTS marker CASCADE;
        DROP TABLE IF EXISTS link CASCADE;
        DROP TABLE IF EXISTS pot_error CASCADE;
        DROP TABLE IF EXISTS process_status CASCADE;
        DROP TABLE IF EXISTS sql_response CASCADE;
        DROP TABLE IF EXISTS execution_step CASCADE;
        DROP TABLE IF EXISTS recording_session CASCADE;
    END
$$;
