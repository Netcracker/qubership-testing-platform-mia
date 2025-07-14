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
        DROP INDEX IF EXISTS projectIdIndexOnProjectConfiguration;
        CREATE INDEX projectIdIndexOnProjectConfiguration ON project_configuration (project_id);

        DROP INDEX IF EXISTS projectIdIndexOnProjectCommonConfiguration;
        CREATE INDEX projectIdIndexOnProjectCommonConfiguration ON project_common_configuration (project_id);

        DROP INDEX IF EXISTS projectIdIndexOnProjectHeaderConfiguration;
        CREATE INDEX projectIdIndexOnProjectHeaderConfiguration ON project_header_configuration (project_id);

        DROP INDEX IF EXISTS projectIdIndexOnProjectPotHeaderConfiguration;
        CREATE INDEX projectIdIndexOnProjectPotHeaderConfiguration ON project_pot_header_configuration (project_id);

        DROP INDEX IF EXISTS projectIdIndexOnProjectSectionConfiguration;
        CREATE INDEX projectIdIndexOnProjectSectionConfiguration ON project_section_configuration (project_id);

        DROP INDEX IF EXISTS projectIdIndexOnProjectCompoundsConfiguration;
        CREATE INDEX projectIdIndexOnProjectCompoundsConfiguration ON project_compounds_configuration (project_id);

        DROP INDEX IF EXISTS projectIdIndexOnProjectProcessesConfiguration;
        CREATE INDEX projectIdIndexOnProjectProcessesConfiguration ON project_processes_configuration (project_id);

        DROP INDEX IF EXISTS projectIdIndexOnProjectDirectory;
        CREATE INDEX projectIdIndexOnProjectDirectory ON project_directory (project_id);

        DROP INDEX IF EXISTS projectIdIndexOnProjectFile;
        CREATE INDEX projectIdIndexOnProjectFile ON project_file (project_id);
    END
$$;
