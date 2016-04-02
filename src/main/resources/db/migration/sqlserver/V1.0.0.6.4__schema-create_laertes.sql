-- Authors: Richard D Boyce, Erica Voss, Lee Evans
-- 2014/2015
-- sql server script

ALTER TABLE ${ohdsiSchema}.DRUG_HOI_EVIDENCE RENAME MODALITY TO SUPPORTS;

ALTER TABLE ${ohdsiSchema}.LAERTES_SUMMARY RENAME CT_COUNT TO MEDLINE_CT_COUNT;
ALTER TABLE ${ohdsiSchema}.LAERTES_SUMMARY RENAME CASE_COUNT TO MEDLINE_CASE_COUNT;
ALTER TABLE ${ohdsiSchema}.LAERTES_SUMMARY RENAME OTHER_COUNT TO MEDLINE_OTHER_COUNT;

ALTER TABLE ${ohdsiSchema}.LAERTES_SUMMARY ADD CTD_CHEMICAL_DISEASE_COUNT   INTEGER;
COMMENT ON COLUMN ${ohdsiSchema}.LAERTES_SUMMARY.CTD_CHEMICAL_DISEASE_COUNT IS '';
ALTER TABLE ${ohdsiSchema}.LAERTES_SUMMARY ADD SEMMEDDB_OTHER_COUNT INTEGER;
COMMENT ON COLUMN ${ohdsiSchema}.LAERTES_SUMMARY.SEMMEDDB_OTHER_COUNT IS '';
ALTER TABLE ${ohdsiSchema}.LAERTES_SUMMARY ADD SEMMEDDB_NEG_OTHER_COUNT INTEGER;
COMMENT ON COLUMN ${ohdsiSchema}.LAERTES_SUMMARY.SEMMEDDB_NEG_OTHER_COUNT IS '';
