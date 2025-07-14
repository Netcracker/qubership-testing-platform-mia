DO
$$
BEGIN
    -- Add columns if they don't already exist
    ALTER TABLE project_processes_configuration
        ADD COLUMN IF NOT EXISTS in_sections text,
        ADD COLUMN IF NOT EXISTS in_compounds text;

    -- Fill in compounds names of a process : allow duplicate compound names
    UPDATE project_processes_configuration ppc
    SET in_compounds = sub.compound_names
    FROM (
        SELECT
            pcpc.process_id,
            STRING_AGG(pcc.compound_name, ', ' ORDER BY pcc.compound_name) AS compound_names
        FROM project_compound_process_configuration AS pcpc
        JOIN project_compounds_configuration pcc ON pcpc.compound_id = pcc.id
        GROUP BY pcpc.process_id
        ) sub
    WHERE ppc.id = sub.process_id;

    -- Fill in sections names of a process : distinct section names
    UPDATE project_processes_configuration ppc
    SET in_sections = sub.section_names
    FROM (
        SELECT
            psps.process_id,
            STRING_AGG(DISTINCT psc.section_name, ', ' ORDER BY psc.section_name) AS section_names
        FROM project_section_process_configuration psps
        JOIN project_section_configuration psc ON psps.section_id = psc.id
        GROUP BY psps.process_id
    ) sub
    WHERE ppc.id = sub.process_id;
END
$$;