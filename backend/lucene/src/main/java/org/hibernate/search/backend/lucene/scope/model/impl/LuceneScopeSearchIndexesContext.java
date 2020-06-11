/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.scope.model.impl;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexModel;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaFieldNode;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaObjectFieldNode;
import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.search.impl.LuceneMultiIndexSearchFieldContext;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchFieldContext;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchIndexesContext;
import org.hibernate.search.backend.lucene.types.predicate.impl.LuceneObjectPredicateBuilderFactory;
import org.hibernate.search.backend.lucene.types.predicate.impl.LuceneObjectPredicateBuilderFactoryImpl;
import org.hibernate.search.engine.backend.document.model.dsl.ObjectFieldStorage;
import org.hibernate.search.engine.backend.document.model.spi.IndexFieldFilter;
import org.hibernate.search.engine.backend.types.converter.spi.ToDocumentIdentifierValueConverter;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.reporting.EventContext;

public class LuceneScopeSearchIndexesContext implements LuceneSearchIndexesContext {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final Set<LuceneIndexModel> indexModels;
	private final Set<String> typeNames;
	private final Set<String> indexNames;
	private final Set<LuceneScopeIndexManagerContext> indexManagerContexts;

	public LuceneScopeSearchIndexesContext(Set<LuceneIndexModel> indexModels,
			Set<LuceneScopeIndexManagerContext> indexManagerContexts) {
		this.indexModels = indexModels;
		// Use LinkedHashSet to ensure stable order when generating requests
		this.typeNames = new LinkedHashSet<>();
		this.indexNames = new LinkedHashSet<>();
		this.indexManagerContexts = indexManagerContexts;

		for ( LuceneIndexModel model : indexModels ) {
			this.typeNames.add( model.getMappedTypeName() );
			this.indexNames.add( model.hibernateSearchName() );
		}
	}

	@Override
	public Set<String> typeNames() {
		return typeNames;
	}

	@Override
	public Set<String> indexNames() {
		return indexNames;
	}

	@Override
	public Set<LuceneScopeIndexManagerContext> indexManagerContexts() {
		return indexManagerContexts;
	}

	@Override
	public LuceneScopedIndexRootComponent<ToDocumentIdentifierValueConverter<?>> idDslConverter() {
		Iterator<LuceneIndexModel> iterator = indexModels.iterator();
		LuceneIndexModel indexModelForSelectedIdConverter = null;
		ToDocumentIdentifierValueConverter<?> selectedIdConverter = null;
		LuceneScopedIndexRootComponent<ToDocumentIdentifierValueConverter<?>> scopedIndexFieldComponent =
				new LuceneScopedIndexRootComponent<>();

		while ( iterator.hasNext() ) {
			LuceneIndexModel indexModel = iterator.next();
			ToDocumentIdentifierValueConverter<?> idConverter = indexModel.getIdDslConverter();

			if ( selectedIdConverter == null ) {
				indexModelForSelectedIdConverter = indexModel;
				selectedIdConverter = idConverter;
				scopedIndexFieldComponent.setComponent( selectedIdConverter );
				continue;
			}

			if ( !selectedIdConverter.isCompatibleWith( idConverter ) ) {
				LuceneFailingIdCompatibilityChecker failingCompatibilityChecker =
						new LuceneFailingIdCompatibilityChecker(
								selectedIdConverter, idConverter,
								EventContexts.fromIndexNames(
										indexModelForSelectedIdConverter.hibernateSearchName(),
										indexModel.hibernateSearchName()
								)
						);
				scopedIndexFieldComponent.setIdConverterCompatibilityChecker( failingCompatibilityChecker );
			}
		}

		return scopedIndexFieldComponent;
	}

	@Override
	public LuceneObjectPredicateBuilderFactory objectPredicateBuilderFactory(String absoluteFieldPath) {
		LuceneObjectPredicateBuilderFactory result = null;

		LuceneIndexSchemaObjectFieldNode objectNode = null;
		String objectNodeIndexName = null;
		LuceneIndexSchemaFieldNode<?> fieldNode = null;
		String fieldNodeIndexName = null;

		for ( LuceneIndexModel indexModel : indexModels ) {
			String indexName = indexModel.hibernateSearchName();

			LuceneIndexSchemaFieldNode<?> currentFieldNode =
					indexModel.getFieldNode( absoluteFieldPath, IndexFieldFilter.INCLUDED_ONLY );
			if ( currentFieldNode != null ) {
				fieldNode = currentFieldNode;
				fieldNodeIndexName = indexName;
				if ( objectNode != null ) {
					throw log.conflictingFieldModel( absoluteFieldPath, objectNode, fieldNode,
							EventContexts.fromIndexNames( objectNodeIndexName, indexName )
					);
				}
				continue;
			}

			LuceneIndexSchemaObjectFieldNode currentObjectNode =
					indexModel.getObjectFieldNode( absoluteFieldPath, IndexFieldFilter.INCLUDED_ONLY );
			if ( currentObjectNode == null ) {
				continue;
			}

			if ( fieldNode != null ) {
				throw log.conflictingFieldModel( absoluteFieldPath, currentObjectNode, fieldNode,
						EventContexts.fromIndexNames( fieldNodeIndexName, indexName )
				);
			}

			LuceneObjectPredicateBuilderFactoryImpl predicateBuilderFactory =
					new LuceneObjectPredicateBuilderFactoryImpl( currentObjectNode );
			if ( result == null ) {
				result = predicateBuilderFactory;
				objectNode = currentObjectNode;
				objectNodeIndexName = indexName;
				continue;
			}

			if ( !result.isCompatibleWith( predicateBuilderFactory ) ) {
				throw log.conflictingObjectFieldModel( absoluteFieldPath, objectNode, currentObjectNode,
						EventContexts.fromIndexNames( objectNodeIndexName, indexName )
				);
			}
		}
		return result;
	}

	@Override
	@SuppressWarnings("unchecked") // We check types using reflection (see calls to type().valueType())
	public LuceneSearchFieldContext<?> field(String absoluteFieldPath) {
		LuceneSearchFieldContext<?> resultOrNull = null;
		if ( indexModels.size() == 1 ) {
			// Single-index search
			resultOrNull = indexModels.iterator().next().getFieldNode( absoluteFieldPath, IndexFieldFilter.INCLUDED_ONLY );
		}
		else {
			// Multi-index search
			Class<?> fieldValueTypeForAllIndexes = null;
			List<LuceneSearchFieldContext<?>> fieldForEachIndex = new ArrayList<>();

			for ( LuceneIndexModel indexModel : indexModels ) {
				LuceneIndexSchemaFieldNode<?> fieldForCurrentIndex =
						indexModel.getFieldNode( absoluteFieldPath, IndexFieldFilter.INCLUDED_ONLY );
				if ( fieldForCurrentIndex == null ) {
					continue;
				}
				Class<?> fieldValueTypeForCurrentIndex = fieldForCurrentIndex.type().valueClass();
				if ( fieldValueTypeForAllIndexes == null ) {
					fieldValueTypeForAllIndexes = fieldValueTypeForCurrentIndex;
				}
				else {
					if ( !fieldValueTypeForAllIndexes.equals( fieldValueTypeForCurrentIndex ) ) {
						throw log.conflictingFieldTypesForSearch( absoluteFieldPath, "valueType",
								fieldValueTypeForAllIndexes, fieldValueTypeForCurrentIndex, indexesEventContext() );
					}
				}
				fieldForEachIndex.add( fieldForCurrentIndex );
			}

			if ( !fieldForEachIndex.isEmpty() ) {
				resultOrNull = new LuceneMultiIndexSearchFieldContext<>(
						indexNames, absoluteFieldPath, (List) fieldForEachIndex
				);
			}
		}
		if ( resultOrNull == null ) {
			throw log.unknownFieldForSearch( absoluteFieldPath, indexesEventContext() );
		}
		return resultOrNull;
	}

	@Override
	public void checkNestedField(String absoluteFieldPath) {
		boolean found = false;

		for ( LuceneIndexModel indexModel : indexModels ) {
			LuceneIndexSchemaObjectFieldNode schemaNode =
					indexModel.getObjectFieldNode( absoluteFieldPath, IndexFieldFilter.INCLUDED_ONLY );
			if ( schemaNode != null ) {
				found = true;
				if ( !ObjectFieldStorage.NESTED.equals( schemaNode.storage() ) ) {
					throw log.nonNestedFieldForNestedQuery(
							absoluteFieldPath, indexModel.getEventContext()
					);
				}
			}
		}
		if ( !found ) {
			for ( LuceneIndexModel indexModel : indexModels ) {
				LuceneIndexSchemaFieldNode<?> schemaNode =
						indexModel.getFieldNode( absoluteFieldPath, IndexFieldFilter.INCLUDED_ONLY );
				if ( schemaNode != null ) {
					throw log.nonObjectFieldForNestedQuery(
							absoluteFieldPath, indexModel.getEventContext()
					);
				}
			}
			throw log.unknownFieldForSearch( absoluteFieldPath, indexesEventContext() );
		}
	}

	@Override
	public List<String> nestedPathHierarchyForObject(String absoluteFieldPath) {
		Optional<List<String>> nestedDocumentPath = indexModels.stream()
				.map( indexModel -> indexModel.getObjectFieldNode( absoluteFieldPath, IndexFieldFilter.INCLUDED_ONLY ) )
				.filter( Objects::nonNull )
				.map( node -> Optional.ofNullable( node.nestedPathHierarchy() ) )
				.reduce( (nestedDocumentPath1, nestedDocumentPath2) -> {
					if ( Objects.equals( nestedDocumentPath1, nestedDocumentPath2 ) ) {
						return nestedDocumentPath1;
					}

					throw log.conflictingNestedDocumentPathHierarchy(
							absoluteFieldPath, nestedDocumentPath1.orElse( null ), nestedDocumentPath2.orElse( null ), indexesEventContext() );
				} )
				.orElse( Optional.empty() );

		return nestedDocumentPath.orElse( Collections.emptyList() );
	}

	private EventContext indexesEventContext() {
		return EventContexts.fromIndexNames( indexNames );
	}
}
