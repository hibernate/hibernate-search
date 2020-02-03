/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.index.admin.impl;

import java.util.concurrent.CompletableFuture;

import org.hibernate.search.backend.elasticsearch.index.naming.impl.IndexNames;
import org.hibernate.search.backend.elasticsearch.index.naming.IndexNamingStrategy;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.impl.IndexMetadata;
import org.hibernate.search.backend.elasticsearch.util.spi.URLEncodedString;
import org.hibernate.search.backend.elasticsearch.work.result.impl.ExistingIndexMetadata;

/**
 * The default {@link ElasticsearchSchemaCreator} implementation.
 * @author Gunnar Morling
 */
public class ElasticsearchSchemaCreatorImpl implements ElasticsearchSchemaCreator {

	private final ElasticsearchSchemaAccessor schemaAccessor;

	private final IndexNamingStrategy indexNamingStrategy;

	public ElasticsearchSchemaCreatorImpl(ElasticsearchSchemaAccessor schemaAccessor,
			IndexNamingStrategy indexNamingStrategy) {
		this.schemaAccessor = schemaAccessor;
		this.indexNamingStrategy = indexNamingStrategy;
	}

	@Override
	public CompletableFuture<?> createIndexAssumeNonExisting(IndexNames indexNames, IndexMetadata indexMetadata) {
		return schemaAccessor.createIndexAssumeNonExisting(
				createPrimaryIndexName( indexNames ),
				indexMetadata.getAliases(),
				indexMetadata.getSettings(),
				indexMetadata.getMapping()
		);
	}

	@Override
	public CompletableFuture<ExistingIndexMetadata> createIndexIfAbsent(IndexNames indexNames, IndexMetadata indexMetadata) {
		return schemaAccessor.getCurrentIndexMetadataOrNull( indexNames )
				.thenCompose( existingIndexMetadata -> {
					if ( existingIndexMetadata != null ) {
						return CompletableFuture.completedFuture( existingIndexMetadata );
					}
					else {
						return schemaAccessor.createIndexIgnoreExisting(
								createPrimaryIndexName( indexNames ),
								indexMetadata.getAliases(),
								indexMetadata.getSettings(),
								indexMetadata.getMapping()
						)
								.thenApply( ignored -> null );
					}
				} );
	}

	private URLEncodedString createPrimaryIndexName(IndexNames indexNames) {
		return IndexNames.encodeName(
				indexNamingStrategy.createInitialElasticsearchIndexName( indexNames.getHibernateSearch() )
		);
	}

}
