-- Standardize project_processes_configuration
-- First update any NULL values
UPDATE project_processes_configuration SET created_when = now() WHERE created_when IS NULL;
UPDATE project_processes_configuration SET modified_when = now() WHERE modified_when IS NULL;
-- Then alter the columns
ALTER TABLE project_processes_configuration
    ALTER COLUMN created_when SET DEFAULT now(),
    ALTER COLUMN created_when SET NOT NULL,
    ALTER COLUMN modified_when SET DEFAULT now(),
    ALTER COLUMN modified_when SET NOT NULL;

-- Drop the unnecessary columns
ALTER TABLE project_processes_configuration
    DROP COLUMN IF EXISTS in_sections,
    DROP COLUMN IF EXISTS in_compounds;

-- Standardize project_compounds_configuration
-- First update any NULL values
UPDATE project_compounds_configuration SET created_when = now() WHERE created_when IS NULL;
UPDATE project_compounds_configuration SET modified_when = now() WHERE modified_when IS NULL;
-- Then alter the columns
ALTER TABLE project_compounds_configuration
    ALTER COLUMN created_when SET DEFAULT now(),
    ALTER COLUMN created_when SET NOT NULL,
    ALTER COLUMN modified_when SET DEFAULT now(),
    ALTER COLUMN modified_when SET NOT NULL;

-- Standardize project_section_configuration
-- First update any NULL values
UPDATE project_section_configuration SET created_when = now() WHERE created_when IS NULL;
UPDATE project_section_configuration SET modified_when = now() WHERE modified_when IS NULL;
-- Then alter the columns
ALTER TABLE project_section_configuration
    ALTER COLUMN created_when SET DEFAULT now(),
    ALTER COLUMN created_when SET NOT NULL,
    ALTER COLUMN modified_when SET DEFAULT now(),
    ALTER COLUMN modified_when SET NOT NULL;

-- Standardize project_configuration
-- First update any NULL values
UPDATE project_configuration SET created_when = now() WHERE created_when IS NULL;
UPDATE project_configuration SET modified_when = now() WHERE modified_when IS NULL;
-- Then alter the columns
ALTER TABLE project_configuration
    ALTER COLUMN created_when SET DEFAULT now(),
    ALTER COLUMN created_when SET NOT NULL,
    ALTER COLUMN modified_when SET DEFAULT now(),
    ALTER COLUMN modified_when SET NOT NULL;

-- Standardize project_directory
-- First update any NULL values
UPDATE project_directory SET created_when = now() WHERE created_when IS NULL;
UPDATE project_directory SET modified_when = now() WHERE modified_when IS NULL;
-- Then alter the columns
ALTER TABLE project_directory
    ALTER COLUMN created_when SET DEFAULT now(),
    ALTER COLUMN created_when SET NOT NULL,
    ALTER COLUMN modified_when SET DEFAULT now(),
    ALTER COLUMN modified_when SET NOT NULL;

-- Standardize project_file
-- First update any NULL values
UPDATE project_file SET created_when = now() WHERE created_when IS NULL;
UPDATE project_file SET modified_when = now() WHERE modified_when IS NULL;
-- Then alter the columns
ALTER TABLE project_file
    ALTER COLUMN created_when SET DEFAULT now(),
    ALTER COLUMN created_when SET NOT NULL,
    ALTER COLUMN modified_when SET DEFAULT now(),
    ALTER COLUMN modified_when SET NOT NULL;
