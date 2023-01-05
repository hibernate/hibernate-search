module org.hibernate.search.integrationtest.java.modules.pojo.standalone.lucene {
	exports org.hibernate.search.integrationtest.java.modules.pojo.standalone.lucene.service;
	opens org.hibernate.search.integrationtest.java.modules.pojo.standalone.lucene.entity to
			org.hibernate.search.mapper.pojo.standalone;
	opens org.hibernate.search.integrationtest.java.modules.pojo.standalone.lucene.config to
			org.hibernate.search.engine; // For reflective instantiation of the analysis configurer

	requires org.hibernate.search.backend.lucene;
	requires org.hibernate.search.mapper.pojo.standalone;

}
