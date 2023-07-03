module org.hibernate.search.integrationtest.java.module.orm.elasticsearch.coordination.outboxpolling {
	exports org.hibernate.search.integrationtest.java.modules.orm.elasticsearch.coordination.outboxpolling.service;
	opens org.hibernate.search.integrationtest.java.modules.orm.elasticsearch.coordination.outboxpolling.entity to
			org.hibernate.orm.core,
			/*
			 * TODO HSEARCH-4302 This part of the "opens" directive ideally should not be necessary.
			 *  Hopefully we should be able to ask for a MethodHandles.Lookup instance from Hibernate ORM
			 *  and take advantage of the fact the package is already open to Hibernate ORM?
			 */
			org.hibernate.search.mapper.orm;
	opens org.hibernate.search.integrationtest.java.modules.orm.elasticsearch.coordination.outboxpolling.config to
			org.hibernate.search.engine; // For reflective instantiation of the analysis configurer

	requires jakarta.persistence;
	requires org.hibernate.orm.core;
	requires org.hibernate.search.mapper.orm;
	requires org.hibernate.search.backend.elasticsearch;
	requires org.hibernate.search.mapper.orm.coordination.outboxpolling;

	// This should be re-exported transitively by org.hibernate.search.mapper.orm
	// but currently isn't, because org.hibernate.search.mapper.orm
	// is still an automatic module
	requires org.hibernate.search.engine;
	requires org.hibernate.search.mapper.pojo;

	/*
	 * This is necessary in order to use SessionFactory,
	 * which extends "javax.naming.Referenceable".
	 * Without this, compilation as a Java module fails.
	 */
	requires java.naming;

	// Since Avro is using log4j we need to explicitly require it to make things work:
	requires org.apache.logging.log4j;
}
