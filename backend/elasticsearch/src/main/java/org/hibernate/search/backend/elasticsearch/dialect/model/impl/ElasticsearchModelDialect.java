/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.dialect.model.impl;

import org.hibernate.search.backend.elasticsearch.types.dsl.provider.impl.ElasticsearchIndexFieldTypeFactoryProvider;
import org.hibernate.search.backend.elasticsearch.validation.impl.ElasticsearchPropertyMappingValidatorProvider;

import com.google.gson.Gson;

/**
 * An entry point to all operations that may be implemented differently depending
 * on the Elasticsearch version running on the Elasticsearch cluster.
 * <p>
 * Add more methods here as necessary to implement dialect-specific behavior.
 * <p>
 * This interface should only expose methods to be called during bootstrap,
 * and should not be depended upon in every part of the code.
 * Thus, most methods defined here should be about creating an instance of an interface defined in another package,
 * that will be passed to the part of the code that needs it.
 * <p>
 * For example, if a particular predicate has a different syntax in its JSON form depending on the
 * Elasticsearch version, we could have a createPredicateFormattingStrategy() methods that returns
 * a strategy to be plugged into the predicate builder factory.
 */
public interface ElasticsearchModelDialect {

	ElasticsearchIndexFieldTypeFactoryProvider createIndexTypeFieldFactoryProvider(Gson userFacingGson);

	ElasticsearchPropertyMappingValidatorProvider createElasticsearchPropertyMappingValidatorProvider();

}
