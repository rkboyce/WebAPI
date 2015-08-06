SELECT EV.EVIDENCE_TYPE EV_TYPE, EV.MODALITY EV_MODALITY, EV.EVIDENCE_LINKOUT EV_LINKOUT, REL.HOI EV_HOI, EV.STATISTIC_VALUE EV_STAT_VAL, EV.STATISTIC_TYPE EV_STAT_TYPE
FROM @OHDSI_schema.DRUG_HOI_RELATIONSHIP AS REL INNER JOIN @OHDSI_schema.DRUG_HOI_EVIDENCE AS EV 
ON REL.ID = EV.DRUG_HOI_RELATIONSHIP 
WHERE REL.DRUG = @id
