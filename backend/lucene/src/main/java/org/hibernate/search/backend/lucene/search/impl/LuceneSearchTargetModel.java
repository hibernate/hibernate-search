/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.impl;

import java.lang.invoke.MethodHandles;
import java.util.Iterator;
import java.util.Set;
import java.util.stream.Collectors;

import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexModel;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaFieldNode;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaObjectNode;
import org.hibernate.search.backend.lucene.index.spi.ReaderProvider;
import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.engine.backend.types.converter.spi.ToDocumentIdentifierValueConverter;
import org.hibernate.search.engine.backend.document.model.dsl.ObjectFieldStorage;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.engine.search.predicate.spi.DslConverter;
import org.hibernate.search.util.common.reporting.EventContext;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

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

	public EventContext getIndexesEventContext() {
		return EventContexts.fromIndexNames( indexNames );
	}

	public Set<ReaderProvider> getReaderProviders() {
		return readerProviders;
	}

	public ToDocumentIdentifierValueConverter<?> getIdDslConverter() {
		Iterator<LuceneIndexModel> iterator = indexModels.iterator();
		LuceneIndexModel indexModelForSelectedIdConverter = iterator.next();
		ToDocumentIdentifierValueConverter<?> selectedIdConverter = indexModelForSelectedIdConverter.getIdDslConverter();

		while ( iterator.hasNext() ) {
			LuceneIndexModel indexModel = iterator.next();
			ToDocumentIdentifierValueConverter<?> idConverter = indexModel.getIdDslConverter();
			if ( !selectedIdConverter.isCompatibleWith( idConverter ) ) {
				throw log.conflictingIdentifierTypesForPredicate(
						selectedIdConverter, idConverter,
						EventContexts.fromIndexNames(
								indexModelForSelectedIdConverter.getIndexName(),
								indexModel.getIndexName()
						)
				);
			}
		}

		return selectedIdConverter;
	}

	public <T> T getSchemaNodeComponent(String absoluteFieldPath,
			IndexSchemaFieldNodeComponentRetrievalStrategy<T> componentRetrievalStrategy) {
		// by default dsl converter check is enabled
		return getSchemaNodeComponent( absoluteFieldPath, componentRetrievalStrategy, DslConverter.ENABLED );
	}

	public <T> T getSchemaNodeComponent(String absoluteFieldPath,
			IndexSchemaFieldNodeComponentRetrievalStrategy<T> componentRetrievalStrategy, DslConverter dslConverter) {
		LuceneIndexModel indexModelForSelectedSchemaNode = null;
		LuceneIndexSchemaFieldNode<?> selectedSchemaNode = null;
		T selectedComponent = null;

		for ( LuceneIndexModel indexModel : indexModels ) {
			LuceneIndexSchemaFieldNode<?> schemaNode = indexModel.getFieldNode( absoluteFieldPath );

			if ( schemaNode != null ) {
				T component = componentRetrievalStrategy.extractComponent( schemaNode );

				if ( selectedSchemaNode == null ) {
					selectedSchemaNode = schemaNode;
					indexModelForSelectedSchemaNode = indexModel;
					selectedComponent = component;
				}
				else if ( !componentRetrievalStrategy.areCompatible( selectedComponent, component, dslConverter ) ) {
					throw componentRetrievalStrategy.createCompatibilityException(
							absoluteFieldPath,
							selectedComponent,
							component,
							EventContexts.fromIndexNames(
									indexModelForSelectedSchemaNode.getIndexName(),
									indexModel.getIndexName()
							)
					);
				}
			}
		}
		if ( selectedSchemaNode == null ) {
			throw log.unknownFieldForSearch( absoluteFieldPath, getIndexesEventContext() );
		}
		return selectedComponent;
	}

	public void checkNestedField(String absoluteFieldPath) {
		boolean found = false;

		for ( LuceneIndexModel indexModel : indexModels ) {
			LuceneIndexSchemaObjectNode schemaNode = indexModel.getObjectNode( absoluteFieldPath );
			if ( schemaNode != null ) {
				found = true;
				if ( !ObjectFieldStorage.NESTED.equals( schemaNode.getStorage() ) ) {
					throw log.nonNestedFieldForNestedQuery(
							absoluteFieldPath, indexModel.getEventContext()
					);
				}
			}
		}
		if ( !found ) {
			for ( LuceneIndexModel indexModel : indexModels ) {
				LuceneIndexSchemaFieldNode<?> schemaNode = indexModel.getFieldNode( absoluteFieldPath );
				if ( schemaNode != null ) {
					throw log.nonObjectFieldForNestedQuery(
							absoluteFieldPath, indexModel.getEventContext()
					);
				}
			}
			throw log.unknownFieldForSearch( absoluteFieldPath, getIndexesEventContext() );
		}
	}
}
