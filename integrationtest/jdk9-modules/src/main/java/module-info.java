module hibernate.search.integrationtest.jdk9.modules.client {
	exports org.hibernate.search.test.integration.jdk9_modules.client.service;
	requires hibernate.search.engine;
	requires hibernate.search.orm;
	requires java.persistence;
	requires org.hibernate.orm.core;
	requires lucene.analyzers.common;
	requires lucene.core;
}