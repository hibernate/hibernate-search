/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.impl;

import java.lang.invoke.MethodHandles;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.hibernate.search.engine.backend.document.model.ObjectFieldStorage;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneFieldFormatter;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneFieldQueryFactory;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexModel;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaFieldNode;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaObjectNode;
import org.hibernate.search.backend.lucene.index.spi.ReaderProvider;
import org.hibernate.search.backend.lucene.logging.impl.Log;
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

	public Set<LuceneIndexModel> getIndexModels() {
		return indexModels;
	}

	public Set<ReaderProvider> getReaderProviders() {
		return readerProviders;
	}

	public LuceneFieldQueryFactory getFieldQueryBuilder(String absoluteFieldPath) {
		return getFieldElement( LuceneIndexSchemaFieldNode::getQueryBuilder, absoluteFieldPath );
	}

	public LuceneFieldFormatter<?> getFieldFormatter(String absoluteFieldPath) {
		return getFieldElement( LuceneIndexSchemaFieldNode::getFormatter, absoluteFieldPath );
	}

	private <R> R getFieldElement(Function<LuceneIndexSchemaFieldNode<?>, R> getElementFunction, String absoluteFieldPath) {
		LuceneIndexModel indexModelForSelectedFormatter = null;
		R selectedElement = null;
		for ( LuceneIndexModel indexModel : indexModels ) {
			LuceneIndexSchemaFieldNode<?> schemaNode = indexModel.getFieldNode( absoluteFieldPath );
			if ( schemaNode != null ) {
				R element = getElementFunction.apply( schemaNode );
				if ( selectedElement == null ) {
					selectedElement = element;
					indexModelForSelectedFormatter = indexModel;
				}
				else if ( !selectedElement.equals( element ) ) {
					throw log.conflictingFieldTypesForSearch(
							absoluteFieldPath,
							selectedElement, indexModelForSelectedFormatter.getIndexName(),
							element, indexModel.getIndexName()
					);
				}
			}
		}
		if ( selectedElement == null ) {
			throw log.unknownFieldForSearch( absoluteFieldPath, getIndexNames() );
		}
		return selectedElement;
	}

	public void checkNestedField(String absoluteFieldPath) {
		boolean found = false;

		for ( LuceneIndexModel indexModel : indexModels ) {
			LuceneIndexSchemaObjectNode schemaNode = indexModel.getObjectNode( absoluteFieldPath );
			if ( schemaNode != null ) {
				found = true;
				if ( !ObjectFieldStorage.NESTED.equals( schemaNode.getStorage() ) ) {
					throw log.nonNestedFieldForNestedQuery( indexModel.getIndexName(), absoluteFieldPath );
				}
			}
		}
		if ( !found ) {
			for ( LuceneIndexModel indexModel : indexModels ) {
				LuceneIndexSchemaFieldNode<?> schemaNode = indexModel.getFieldNode( absoluteFieldPath );
				if ( schemaNode != null ) {
					throw log.nonObjectFieldForNestedQuery( indexModel.getIndexName(), absoluteFieldPath );
				}
			}
			throw log.unknownFieldForSearch( absoluteFieldPath, getIndexNames() );
		}
	}
}
