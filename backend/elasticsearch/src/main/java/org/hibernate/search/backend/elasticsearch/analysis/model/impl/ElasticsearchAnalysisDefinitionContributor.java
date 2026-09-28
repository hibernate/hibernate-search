package org.hibernate.search.backend.elasticsearch.analysis.model.impl;


public interface ElasticsearchAnalysisDefinitionContributor {

	void contribute(ElasticsearchAnalysisDefinitionCollector collector);

}
