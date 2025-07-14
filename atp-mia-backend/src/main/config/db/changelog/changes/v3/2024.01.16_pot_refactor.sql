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
        ALTER TABLE pot_execution_step
            DROP CONSTRAINT IF EXISTS POT_SQL_RESPONSE_TO_EXECUTION_STEP_FK;
        ALTER TABLE pot_execution_step
            DROP CONSTRAINT IF EXISTS POT_PROCESS_STATUS_TO_EXECUTION_STEP_FK;
        ALTER TABLE pot_execution_step
            DROP CONSTRAINT IF EXISTS POT_EXECUTION_ERROR_TO_EXECUTION_STEP_FK;
        ALTER TABLE pot_execution_step
            ADD COLUMN IF NOT EXISTS process_status json,
            ADD COLUMN IF NOT EXISTS links          json,
            ADD COLUMN IF NOT EXISTS validations    json,
            ADD COLUMN IF NOT EXISTS errors         json;
        DROP INDEX IF EXISTS potSessionIndexOnPotExecutionStep;
        CREATE INDEX potSessionIndexOnPotExecutionStep ON pot_execution_step (pot_session_id);
        DROP INDEX IF EXISTS projectIdIndexOnPotSession;
        CREATE INDEX projectIdIndexOnPotSession ON pot_session (project_id);
        DROP INDEX IF EXISTS createdAtIndexOnPotSession;
        CREATE INDEX createdAtIndexOnPotSession ON pot_session (created_at);

        TRUNCATE TABLE pot_sql_response CASCADE;
        TRUNCATE TABLE pot_process_status CASCADE;
        TRUNCATE TABLE pot_execution_error CASCADE;
        TRUNCATE TABLE pot_link CASCADE;
    END
$$;
