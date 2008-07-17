Hibernate Search dependencies
=============================

Core
====
hibernate-commons-annotations.jar: required
hibernate-core.jar: required
hibernate core dependencies: required (see Hibernate Core for more information)
lucene-core-*.jar: required (used version 2.3.2)
jta.jar: required   
jms.jar: optional (needed for JMS based clustering strategy, usually available with your application server)
jsr-250-api.jar: optional (needed for JMS based clustering strategy, usually available with your application server)
apache-solr-analyzer.jar: optional (used version 1.2.0), needed if @AnalyzerDef is used
slf4j-api: required (a slf4j-[impl].ar is required too - eg. slf4j-log4j12.jar) 

Test
====
hibernate-annotations.jar: required
hibernate-entitymanager.jar: required