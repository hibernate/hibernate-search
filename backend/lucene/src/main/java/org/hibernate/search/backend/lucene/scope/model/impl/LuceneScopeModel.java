/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.scope.model.impl;

import java.lang.invoke.MethodHandles;
import java.util.Iterator;
import java.util.Set;
import java.util.stream.Collectors;

import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexModel;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaFieldNode;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaObjectNode;
import org.hibernate.search.backend.lucene.index.spi.ReaderProvider;
import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.engine.backend.document.model.dsl.ObjectFieldStorage;
import org.hibernate.search.engine.backend.types.converter.spi.ToDocumentIdentifierValueConverter;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.reporting.EventContext;

public class LuceneScopeModel {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final Set<LuceneIndexModel> indexModels;
	private final Set<String> indexNames;
	private final Set<ReaderProvider> readerProviders;

	public LuceneScopeModel(Set<LuceneIndexModel> indexModels, Set<ReaderProvider> readerProviders) {
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

	public <T> LuceneScopedIndexFieldComponent<T> getSchemaNodeComponent(String absoluteFieldPath,
			IndexSchemaFieldNodeComponentRetrievalStrategy<T> componentRetrievalStrategy) {
		LuceneIndexModel indexModelForSelectedSchemaNode = null;
		LuceneIndexSchemaFieldNode<?> selectedSchemaNode = null;
		LuceneScopedIndexFieldComponent<T> scopedIndexFieldComponent = new LuceneScopedIndexFieldComponent<>();

		for ( LuceneIndexModel indexModel : indexModels ) {
			LuceneIndexSchemaFieldNode<?> schemaNode = indexModel.getFieldNode( absoluteFieldPath );
			if ( schemaNode == null ) {
				continue;
			}

			T component = componentRetrievalStrategy.extractComponent( schemaNode );
			if ( selectedSchemaNode == null ) {
				selectedSchemaNode = schemaNode;
				indexModelForSelectedSchemaNode = indexModel;
				scopedIndexFieldComponent.setComponent( component );
				continue;
			}

			if ( !componentRetrievalStrategy.hasCompatibleCodec( scopedIndexFieldComponent.getComponent(), component ) ) {
				throw componentRetrievalStrategy.createCompatibilityException(
						absoluteFieldPath,
						scopedIndexFieldComponent.getComponent(),
						component,
						EventContexts.fromIndexNames(
								indexModelForSelectedSchemaNode.getIndexName(),
								indexModel.getIndexName()
						)
				);
			}

			LuceneFailingCompatibilityChecker<T> failingCompatibilityChecker = new LuceneFailingCompatibilityChecker<>(
					absoluteFieldPath, scopedIndexFieldComponent.getComponent(), component, EventContexts.fromIndexNames(
					indexModelForSelectedSchemaNode.getIndexName(), indexModel.getIndexName()
			), componentRetrievalStrategy );

			if ( !componentRetrievalStrategy.hasCompatibleConverter( scopedIndexFieldComponent.getComponent(), component ) ) {
				scopedIndexFieldComponent.setConverterCompatibilityChecker( failingCompatibilityChecker );
			}
			if ( !componentRetrievalStrategy.hasCompatibleAnalyzer( scopedIndexFieldComponent.getComponent(), component ) ) {
				scopedIndexFieldComponent.setAnalyzerCompatibilityChecker( failingCompatibilityChecker );
			}
		}
		if ( selectedSchemaNode == null ) {
			throw log.unknownFieldForSearch( absoluteFieldPath, getIndexesEventContext() );
		}
		return scopedIndexFieldComponent;
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
