/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.schema.management.impl;

import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchRequest;
import org.hibernate.search.backend.elasticsearch.index.layout.IndexLayoutStrategy;
import org.hibernate.search.backend.elasticsearch.index.layout.impl.IndexNames;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.impl.IndexMetadata;
import org.hibernate.search.backend.elasticsearch.schema.management.ElasticsearchIndexSchemaExport;
import org.hibernate.search.backend.elasticsearch.util.spi.URLEncodedString;
import org.hibernate.search.backend.elasticsearch.work.factory.impl.ElasticsearchWorkFactory;

import com.google.gson.Gson;

final class ElasticsearchSchemaExporter {

	private final Gson userFacingGson;
	private final ElasticsearchWorkFactory workFactory;
	private final IndexLayoutStrategy indexLayoutStrategy;

	public ElasticsearchSchemaExporter(Gson userFacingGson, ElasticsearchWorkFactory workFactory,
			IndexLayoutStrategy indexLayoutStrategy) {
		this.userFacingGson = userFacingGson;
		this.workFactory = workFactory;
		this.indexLayoutStrategy = indexLayoutStrategy;
	}

	public ElasticsearchIndexSchemaExport export(IndexMetadata indexMetadata, IndexNames indexNames) {
		URLEncodedString primaryIndexName = createPrimaryIndexName( indexNames );
		ElasticsearchRequest request = workFactory.createIndex( primaryIndexName )
				.aliases( indexMetadata.getAliases() )
				.mapping( indexMetadata.getMapping() )
				.settings( indexMetadata.getSettings() )
				.build().request();

		return new ElasticsearchIndexSchemaExportImpl(
				userFacingGson,
				indexNames.hibernateSearchIndex(),
				request
		);
	}

	private URLEncodedString createPrimaryIndexName(IndexNames indexNames) {
		return IndexNames.encodeName(
				indexLayoutStrategy.createInitialElasticsearchIndexName( indexNames.hibernateSearchIndex() )
		);
	}
}
