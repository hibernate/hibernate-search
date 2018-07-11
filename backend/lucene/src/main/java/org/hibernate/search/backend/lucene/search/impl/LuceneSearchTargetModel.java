/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.impl;

import java.lang.invoke.MethodHandles;
import java.util.Set;
import java.util.stream.Collectors;

import org.hibernate.search.engine.backend.document.model.dsl.ObjectFieldStorage;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexModel;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaFieldNode;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaObjectNode;
import org.hibernate.search.backend.lucene.index.spi.ReaderProvider;
import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.engine.logging.spi.FailureContext;
import org.hibernate.search.engine.logging.spi.FailureContexts;
import org.hibernate.search.util.impl.common.LoggerFactory;

public class LuceneSearchTargetModel {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final Set<LuceneIndexModel> indexModels;
	private final Set<String> indexNames;
	private final Set<ReaderProvider> readerProviders;

	public LuceneSearchTargetModel(Set<LuceneIndexModel> indexModels, Set<ReaderProvider> readerProviders) {
		this.indexModels = indexModels;
		this.indexNames = indexModels.stream()
				.map( LuceneIndexModel::getIndexName )
				.collect( Collectors.toSet() );
		this.readerProviders = readerProviders;
	}

	public Set<String> getIndexNames() {
		return indexNames;
	}

	public FailureContext getIndexesFailureContext() {
		return FailureContexts.fromIndexNames( indexNames );
	}

	public Set<LuceneIndexModel> getIndexModels() {
		return indexModels;
	}

	public Set<ReaderProvider> getReaderProviders() {
		return readerProviders;
	}

	public LuceneIndexSchemaFieldNode<?> getSchemaNode(String absoluteFieldPath) {
		LuceneIndexModel indexModelForSelectedSchemaNode = null;
		LuceneIndexSchemaFieldNode<?> selectedSchemaNode = null;

		for ( LuceneIndexModel indexModel : indexModels ) {
			LuceneIndexSchemaFieldNode<?> schemaNode = indexModel.getFieldNode( absoluteFieldPath );
			if ( schemaNode != null ) {
				if ( selectedSchemaNode == null ) {
					selectedSchemaNode = schemaNode;
					indexModelForSelectedSchemaNode = indexModel;
				}
				else if ( !selectedSchemaNode.isCompatibleWith( schemaNode ) ) {
					throw log.conflictingFieldTypesForSearch(
							absoluteFieldPath,
							selectedSchemaNode, schemaNode,
							FailureContexts.fromIndexNames(
									indexModelForSelectedSchemaNode.getIndexName(),
									indexModel.getIndexName()
							)
					);
				}
			}
		}
		if ( selectedSchemaNode == null ) {
			throw log.unknownFieldForSearch( absoluteFieldPath, getIndexesFailureContext() );
		}
		return selectedSchemaNode;
	}

	public void checkNestedField(String absoluteFieldPath) {
		boolean found = false;

		for ( LuceneIndexModel indexModel : indexModels ) {
			LuceneIndexSchemaObjectNode schemaNode = indexModel.getObjectNode( absoluteFieldPath );
			if ( schemaNode != null ) {
				found = true;
				if ( !ObjectFieldStorage.NESTED.equals( schemaNode.getStorage() ) ) {
					throw log.nonNestedFieldForNestedQuery(
							absoluteFieldPath, indexModel.getFailureContext()
					);
				}
			}
		}
		if ( !found ) {
			for ( LuceneIndexModel indexModel : indexModels ) {
				LuceneIndexSchemaFieldNode<?> schemaNode = indexModel.getFieldNode( absoluteFieldPath );
				if ( schemaNode != null ) {
					throw log.nonObjectFieldForNestedQuery(
							absoluteFieldPath, indexModel.getFailureContext()
					);
				}
			}
			throw log.unknownFieldForSearch( absoluteFieldPath, getIndexesFailureContext() );
		}
	}
}
