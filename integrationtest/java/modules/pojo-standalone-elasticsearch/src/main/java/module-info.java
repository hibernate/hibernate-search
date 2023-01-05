module org.hibernate.search.integrationtest.java.modules.pojo.standalone.elasticsearch {
	exports org.hibernate.search.integrationtest.java.modules.pojo.standalone.elasticsearch.service;
	opens org.hibernate.search.integrationtest.java.modules.pojo.standalone.elasticsearch.entity to
			org.hibernate.search.mapper.pojo.standalone;
	opens org.hibernate.search.integrationtest.java.modules.pojo.standalone.elasticsearch.config to
			org.hibernate.search.engine; // For reflective instantiation of the analysis configurer

	requires org.hibernate.search.backend.elasticsearch;
	requires org.hibernate.search.mapper.pojo.standalone;

}
