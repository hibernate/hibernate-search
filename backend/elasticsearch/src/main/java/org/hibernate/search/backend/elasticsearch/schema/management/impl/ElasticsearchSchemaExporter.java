package org.hibernate.search.backend.elasticsearch.schema.management.impl;

import org.hibernate.search.backend.elasticsearch.client.common.impl.ElasticsearchJsonRequest;
import org.hibernate.search.backend.elasticsearch.client.common.util.spi.URLEncodedString;
import org.hibernate.search.backend.elasticsearch.index.layout.IndexLayoutStrategy;
import org.hibernate.search.backend.elasticsearch.index.layout.impl.IndexNames;
import org.hibernate.search.backend.elasticsearch.json.internal.spi.JsonCodecService;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.impl.IndexMetadata;
import org.hibernate.search.backend.elasticsearch.schema.management.ElasticsearchIndexSchemaExport;
import org.hibernate.search.backend.elasticsearch.work.factory.impl.ElasticsearchWorkFactory;

final class ElasticsearchSchemaExporter {

	private final JsonCodecService userFacingCodec;
	private final ElasticsearchWorkFactory workFactory;
	private final IndexLayoutStrategy indexLayoutStrategy;

	public ElasticsearchSchemaExporter(JsonCodecService userFacingCodec, ElasticsearchWorkFactory workFactory,
			IndexLayoutStrategy indexLayoutStrategy) {
		this.userFacingCodec = userFacingCodec;
		this.workFactory = workFactory;
		this.indexLayoutStrategy = indexLayoutStrategy;
	}

	public ElasticsearchIndexSchemaExport export(IndexMetadata indexMetadata, IndexNames indexNames) {
		URLEncodedString primaryIndexName = createPrimaryIndexName( indexNames );
		ElasticsearchJsonRequest request = workFactory.createIndex( primaryIndexName )
				.aliases( indexMetadata.getAliases() )
				.mapping( indexMetadata.getMapping() )
				.settings( indexMetadata.getSettings() )
				.build().request();

		return new ElasticsearchIndexSchemaExportImpl(
				userFacingCodec,
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
