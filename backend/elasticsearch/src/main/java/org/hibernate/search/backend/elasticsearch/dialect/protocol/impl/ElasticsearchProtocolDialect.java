/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.dialect.protocol.impl;

import org.hibernate.search.backend.elasticsearch.dialect.model.impl.ElasticsearchModelDialect;
import org.hibernate.search.backend.elasticsearch.gson.spi.GsonProvider;
import org.hibernate.search.backend.elasticsearch.lowlevel.syntax.metadata.impl.ElasticsearchIndexMetadataSyntax;
import org.hibernate.search.backend.elasticsearch.lowlevel.syntax.search.impl.ElasticsearchSearchSyntax;
import org.hibernate.search.backend.elasticsearch.search.query.impl.ElasticsearchSearchResultExtractorFactory;
import org.hibernate.search.backend.elasticsearch.work.factory.impl.ElasticsearchWorkFactory;

/**
 * An entry point to all dialect-sensitive operations that only affect the protocol when talking to the Elasticsearch cluster,
 * i.e. the paths and parameters to use in requests.
 * <p>
 * Add more methods here as necessary to implement dialect-specific behavior.
 * <p>
 * This "protocol dialect" should not affect the index mapping or the analysis definitions:
 * it only affects how we transmit such information to the Elasticsearch cluster.
 * It allows to address slight variations in the way Elasticsearch expects clients to send requests,
 * such as the "include_type_name" parameter introduced in 6.7 that triggers warnings if it's not present in 6.7,
 * but triggers failure if it's present before 6.7.
 * <p>
 * Similarly to {@link ElasticsearchModelDialect},
 * this interface should only expose methods to be called during bootstrap,
 * and should not be depended upon in every part of the code.
 * Thus, most methods defined here should be about creating an instance of an interface defined in another package,
 * that will be passed to the part of the code that needs it.
 */
public interface ElasticsearchProtocolDialect {

	ElasticsearchIndexMetadataSyntax createIndexMetadataSyntax();

	ElasticsearchSearchSyntax createSearchSyntax();

	ElasticsearchWorkFactory createWorkFactory(GsonProvider gsonProvider, Boolean ignoreShardFailures);

	ElasticsearchSearchResultExtractorFactory createSearchResultExtractorFactory();

}
