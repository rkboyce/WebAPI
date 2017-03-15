compile:
	mvn clean
	mvn compile -Pwebapi-postgresql-laertes

package: compile
	mvn package -Pwebapi-postgresql-laertes

deploy: package 
	#sudo service tomcat7 stop
	/var/lib/tomcat7/bin/shutdown.sh
	sleep 4
	sudo rm -rf /var/lib/tomcat7/webapps/WebAPI*
	sudo cp -r target/WebAPI.war  /var/lib/tomcat7/webapps/
	#sudo chown tomcat7 /var/lib/tomcat7/webapps/WebAPI.war
	#sudo chgrp tomcat7 /var/lib/tomcat7/webapps/WebAPI.war
	#sudo service tomcat7 start
	/var/lib/tomcat7/bin/startup.sh

git-push:
	git push myfork master

test:
	wget -O tests/test-general-evidence.json "http://localhost:8080/WebAPI/evidence/LAERTES_CDM/752061"
	wget -O tests/test-drug-hoi.json "http://localhost:8080/WebAPI/evidence/LAERTES_CDM/drughoi/752061-374013"
	wget -O tests/test-drug.json "http://localhost:8080/WebAPI/evidence/LAERTES_CDM/drug/752061"
	wget -O tests/test-hoi.json "http://localhost:8080/WebAPI/evidence/LAERTES_CDM/hoi/320073"
	wget -O tests/test-info.json "http://localhost:8080/WebAPI/evidence/LAERTES_CDM/info"
	wget -O tests/test-drug-hoi-eu-spc.json "http://localhost:8080/WebAPI/evidence/LAERTES_CDM/drughoi/904351-4190045" 
	wget -O tests/test-drug-hoi-splicer.json "http://localhost:8080/WebAPI/evidence/LAERTES_CDM/drughoi/19133853-195588"
	wget -O tests/test-drug-hoi-faers-counts-and-signals.json "http://localhost:8080/WebAPI/evidence/LAERTES_CDM/drughoi/1154343-433031"
	wget -O tests/test-drug-hoi-pubmed-mesh-cr.json "http://localhost:8080/WebAPI/evidence/LAERTES_CDM/drughoi/1154343-433031" 
	wget -O tests/test-drug-hoi-pubmed-mesh-clin-trial.json "http://localhost:8080/WebAPI/evidence/LAERTES_CDM/drughoi/789578-378144"
	wget -O tests/test-drug-hoi-pubmed-mesh-other.json "http://localhost:8080/WebAPI/evidence/LAERTES_CDM/drughoi/19010482-316866"
	wget -O tests/test-drug-hoi-semmed-cr.json "http://localhost:8080/WebAPI/evidence/LAERTES_CDM/drughoi/1112807-441202"
	wget -O tests/test-drug-hoi-semmed-clin-trial.json "http://localhost:8080/WebAPI/evidence/LAERTES_CDM/drughoi/19059744-381591"
	wget -O tests/test-drug-rollup-ingredient.json "http://localhost:8080/WebAPI/evidence/LAERTES_CDM/drugrollup/ingredient/1000632"
	wget -O tests/test-drug-rollup-clin-drug.json "http://localhost:8080/WebAPI/evidence/LAERTES_CDM/drugrollup/clinicaldrug/19074181"
	wget -O tests/test-drug-rollup-branded-drug.json "http://localhost:8080/WebAPI/evidence/LAERTES_CDM/drugrollup/brandeddrug/1000640"
	wget -O tests/test-rdf-evidencesummary.json "http://localhost:8080/WebAPI/evidence/LAERTES_CDM/evidencesummary?conditionID=139900&drugID=1115008&evidenceGroup=Literature"
	wget -O tests/test-rdf-evidencedetails.json "http://localhost:8080/WebAPI/evidence/LAERTES_CDM/evidencedetails?conditionID=24134&drugID=1115008&evidenceType=SPL_SPLICER_ADR"

test-sparql:
	wget -O tests/test-rdf-info.json "http://localhost:8080/WebAPI/evidence/rdfinfo"
	wget -O tests/test-rdf-commandlist.json "http://localhost:8080/WebAPI/evidence/?"
	wget -O tests/test-rdf-linkoutdata.json "http://localhost:8080/WebAPI/evidence/LAERTES_CDM/linkoutdata/http%3A%252F%252Flocalhost%3A8080%252Fl%252Findex.php%3Fid%3Dsplicer-splicer-210"


test-public:
	wget -O tests/test-general-evidence.json "http://api.ohdsi.org/WebAPI/evidence/CS1/752061"
	wget -O tests/test-drug-hoi.json "http://api.ohdsi.org/WebAPI/evidence/CS1/drughoi/752061-374013"
	wget -O tests/test-drug.json "http://api.ohdsi.org/WebAPI/evidence/CS1/drug/752061"
	wget -O tests/test-hoi.json "http://api.ohdsi.org/WebAPI/evidence/CS1/hoi/320073"
	wget -O tests/test-info.json "http://api.ohdsi.org/WebAPI/evidence/CS1/info"
	wget -O tests/test-drug-hoi-eu-spc.json "http://api.ohdsi.org/WebAPI/evidence/CS1/drughoi/904351-4190045" 
	wget -O tests/test-drug-hoi-splicer.json "http://api.ohdsi.org/WebAPI/evidence/CS1/drughoi/19133853-195588"
	wget -O tests/test-drug-hoi-faers-counts-and-signals.json "http://api.ohdsi.org/WebAPI/evidence/CS1/drughoi/1154343-433031"
	wget -O tests/test-drug-hoi-pubmed-mesh-cr.json "http://api.ohdsi.org/WebAPI/evidence/CS1/drughoi/1154343-433031" 
	wget -O tests/test-drug-hoi-pubmed-mesh-clin-trial.json "http://api.ohdsi.org/WebAPI/evidence/CS1/drughoi/789578-378144"
	wget -O tests/test-drug-hoi-pubmed-mesh-other.json "http://api.ohdsi.org/WebAPI/evidence/CS1/drughoi/19010482-316866"
	wget -O tests/test-drug-hoi-semmed-cr.json "http://api.ohdsi.org/WebAPI/evidence/CS1/drughoi/1112807-441202"
	wget -O tests/test-drug-hoi-semmed-clin-trial.json "http://api.ohdsi.org/WebAPI/evidence/CS1/drughoi/19059744-381591"
	wget -O tests/test-drug-rollup-ingredient.json "http://api.ohdsi.org/WebAPI/evidence/CS1/drugrollup/ingredient/1000632"
	wget -O tests/test-drug-rollup-clin-drug.json "http://api.ohdsi.org/WebAPI/evidence/CS1/drugrollup/clinicaldrug/19074181"
	wget -O tests/test-drug-rollup-branded-drug.json "http://api.ohdsi.org/WebAPI/evidence/CS1/drugrollup/brandeddrug/1000640"
	wget -O tests/test-rdf-evidencesummary.json "http://api.ohdsi.org/WebAPI/evidence/CS1/evidencesummary?conditionID=139900&drugID=1115008&evidenceGroup=Literature"
	wget -O tests/test-rdf-evidencedetails.json "http://api.ohdsi.org/WebAPI/evidence/CS1/evidencedetails?conditionID=24134&drugID=1115008&evidenceType=SPL_SPLICER_ADR"
