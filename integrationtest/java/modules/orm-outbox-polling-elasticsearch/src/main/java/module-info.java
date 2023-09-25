module org.hibernate.search.integrationtest.java.module.orm.elasticsearch.outboxpolling {
	exports org.hibernate.search.integrationtest.java.modules.orm.elasticsearch.outboxpolling.service;
	opens org.hibernate.search.integrationtest.java.modules.orm.elasticsearch.outboxpolling.entity to
			org.hibernate.orm.core,
			/*
			 * TODO HSEARCH-4302 This part of the "opens" directive ideally should not be necessary.
			 *  Hopefully we should be able to ask for a MethodHandles.Lookup instance from Hibernate ORM
			 *  and take advantage of the fact the package is already open to Hibernate ORM?
			 */
			org.hibernate.search.mapper.orm;
	opens org.hibernate.search.integrationtest.java.modules.orm.elasticsearch.outboxpolling.config to
			org.hibernate.search.engine; // For reflective instantiation of the analysis configurer

	requires jakarta.persistence;
	requires org.hibernate.orm.core;
	requires org.hibernate.search.mapper.orm;
	requires org.hibernate.search.backend.elasticsearch;
	requires org.hibernate.search.mapper.orm.outboxpolling;

	/*
	 * This is necessary in order to use SessionFactory,
	 * which extends "javax.naming.Referenceable".
	 * Without this, compilation as a Java module fails.
	 */
	requires java.naming;

	// Since Avro is using log4j we need to explicitly require it to make things work:
	requires org.apache.logging.log4j;

	/*
	 * This is necessary in order to put ByteBuddy in the modulepath and make module exports effective.
	 * I do not know why ByteBuddy doesn't end up in the modulepath without this.
	 */
	requires net.bytebuddy;
}
