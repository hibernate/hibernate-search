Hibernate Search dependencies
=============================

Core
====
hibernate-commons-annotations.jar: required
hibernate-core.jar: required + hibernate core dependencies - see Hibernate Core for more information  
lucene-core.jar: required (used version 2.4.0)
jta.jar: required 
slf4j-api: required together with a slf4j-[impl].jar eg slf4j-log4j12.jar  

jms.jar: optional, needed for JMS based clustering strategy, usually available with your application server
jsr-250-api.jar: optional, needed for JMS based clustering strategy, usually available with your application server
solr-core.jar, solr-common.jar: optional (used version 1.3.0), needed if @AnalyzerDef is used
solr-lucenen-snowball.jar: optional, needed if snowball stemmer is used

Test
====
hibernate-annotations.jar: required
hibernate-entitymanager.jar: required
