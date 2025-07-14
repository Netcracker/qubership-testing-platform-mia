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
    ALTER TABLE warn_marker ALTER COLUMN warn_marker TYPE text;
    ALTER TABLE table_marker_column_status ALTER COLUMN actual_result TYPE text;
    ALTER TABLE table_marker_column_status ALTER COLUMN expected_result TYPE text;
    ALTER TABLE pot_error ALTER COLUMN error_code TYPE text;
    ALTER TABLE passed_marker ALTER COLUMN passed_marker TYPE text;
    ALTER TABLE failed_marker ALTER COLUMN failed_marker TYPE text;
    ALTER TABLE db_columns ALTER COLUMN db_columns TYPE text;
END
$$;
