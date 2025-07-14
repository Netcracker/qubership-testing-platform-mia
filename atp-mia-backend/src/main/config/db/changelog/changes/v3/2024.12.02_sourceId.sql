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
        ALTER TABLE IF EXISTS project_processes_configuration ADD COLUMN IF NOT EXISTS source_id uuid;
        ALTER TABLE IF EXISTS project_compounds_configuration ADD COLUMN IF NOT EXISTS source_id uuid;
        ALTER TABLE IF EXISTS project_section_configuration   ADD COLUMN IF NOT EXISTS source_id uuid;
        ALTER TABLE IF EXISTS project_directory               ADD COLUMN IF NOT EXISTS source_id uuid;
        ALTER TABLE IF EXISTS project_file                    ADD COLUMN IF NOT EXISTS source_id uuid;
    END
$$;
