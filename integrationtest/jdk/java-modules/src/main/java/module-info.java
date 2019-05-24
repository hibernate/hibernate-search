module org.hibernate.search.integrationtest.java.module {
	exports org.hibernate.search.integrationtest.java.module.service;
	opens org.hibernate.search.integrationtest.java.module.entity to
			org.hibernate.orm.core,
			/*
			 * TODO HSEARCH-3274 This part of the "opens" directive ideally should not be necessary.
			 *  Hopefully we should be able to ask for a MethodHandles.Lookup instance from Hibernate ORM
			 *  and take advantage of the fact the package is already open to Hibernate ORM?
			 */
			org.hibernate.search.mapper.orm;
	opens org.hibernate.search.integrationtest.java.module.config to
			org.hibernate.search.engine; // For reflective instantiation of the analysis configurer

	requires java.persistence;
	requires org.hibernate.orm.core;
	requires org.hibernate.search.mapper.orm;
	requires org.hibernate.search.backend.elasticsearch;

	// TODO HSEARCH-3274 This should be re-exported transitively by org.hibernate.search.mapper.orm
	requires org.hibernate.search.engine;
	requires org.hibernate.search.mapper.pojo;

	/*
	 * This is necessary in order to use SessionFactory,
	 * which extends "javax.naming.Referenceable".
	 * Without this, compilation as a Java module fails.
	 */
	requires java.naming;
}