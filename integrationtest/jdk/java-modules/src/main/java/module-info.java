module org.hibernate.search.integrationtest.java.module {
	exports org.hibernate.search.integrationtest.java.module.service;
	opens org.hibernate.search.integrationtest.java.module.entity;
	requires java.naming;
	requires org.hibernate.search.engine;
	requires org.hibernate.search.mapper.pojo;
	requires org.hibernate.search.mapper.orm;
	requires org.hibernate.search.backend.elasticsearch;
	requires java.persistence;
	requires org.hibernate.orm.core;
}