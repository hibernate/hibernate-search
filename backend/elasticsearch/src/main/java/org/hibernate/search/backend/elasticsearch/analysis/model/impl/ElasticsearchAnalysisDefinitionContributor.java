/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.analysis.model.impl;


public interface ElasticsearchAnalysisDefinitionContributor {

	void contribute(ElasticsearchAnalysisDefinitionCollector collector);

}
