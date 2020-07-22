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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexModel;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaValueFieldNode;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaObjectFieldNode;
import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.search.impl.LuceneMultiIndexSearchValueFieldContext;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchValueFieldContext;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchIndexesContext;
import org.hibernate.search.backend.lucene.types.predicate.impl.LuceneObjectPredicateBuilderFactory;
import org.hibernate.search.backend.lucene.types.predicate.impl.LuceneObjectPredicateBuilderFactoryImpl;
import org.hibernate.search.engine.backend.types.ObjectStructure;
import org.hibernate.search.engine.backend.document.model.spi.IndexFieldFilter;
import org.hibernate.search.engine.backend.types.converter.spi.StringToDocumentIdentifierValueConverter;
import org.hibernate.search.engine.backend.types.converter.spi.ToDocumentIdentifierValueConverter;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.engine.search.common.ValueConvert;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.reporting.EventContext;

public class LuceneScopeSearchIndexesContext implements LuceneSearchIndexesContext {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private static final StringToDocumentIdentifierValueConverter RAW_ID_CONVERTER =
			new StringToDocumentIdentifierValueConverter();

	private final Set<LuceneScopeIndexManagerContext> indexManagerContexts;
	private final Set<String> typeNames;
	private final Set<String> indexNames;

	public LuceneScopeSearchIndexesContext(Set<LuceneScopeIndexManagerContext> indexManagerContexts) {
		this.indexManagerContexts = indexManagerContexts;
		// Use LinkedHashSet to ensure stable order when generating requests
		this.typeNames = new LinkedHashSet<>();
		this.indexNames = new LinkedHashSet<>();
		for ( LuceneScopeIndexManagerContext indexManager : indexManagerContexts ) {
			this.typeNames.add( indexManager.model().mappedTypeName() );
			this.indexNames.add( indexManager.model().hibernateSearchName() );
		}
	}

	@Override
	public Set<LuceneScopeIndexManagerContext> indexManagerContexts() {
		return indexManagerContexts;
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
	public ToDocumentIdentifierValueConverter<?> idDslConverter(ValueConvert valueConvert) {
		if ( ValueConvert.NO.equals( valueConvert ) ) {
			return RAW_ID_CONVERTER;
		}
		ToDocumentIdentifierValueConverter<?> converter = null;
		for ( LuceneScopeIndexManagerContext index : indexManagerContexts ) {
			ToDocumentIdentifierValueConverter<?> converterForIndex = index.model().getIdDslConverter();
			if ( converter == null ) {
				converter = converterForIndex;
			}
			else if ( !converter.isCompatibleWith( converterForIndex ) ) {
				throw log.inconsistentConfigurationForIdentifierForSearch( converter, converterForIndex, indexesEventContext() );
			}
		}
		return converter;
	}

	@Override
	public LuceneObjectPredicateBuilderFactory objectPredicateBuilderFactory(String absoluteFieldPath) {
		LuceneObjectPredicateBuilderFactory result = null;

		LuceneIndexSchemaObjectFieldNode objectNode = null;
		String objectNodeIndexName = null;
		LuceneIndexSchemaValueFieldNode<?> fieldNode = null;
		String fieldNodeIndexName = null;

		for ( LuceneScopeIndexManagerContext index : indexManagerContexts ) {
			LuceneIndexModel indexModel = index.model();
			String indexName = indexModel.hibernateSearchName();

			LuceneIndexSchemaValueFieldNode<?> currentFieldNode =
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
	public LuceneSearchValueFieldContext<?> field(String absoluteFieldPath) {
		LuceneSearchValueFieldContext<?> resultOrNull = null;
		if ( indexManagerContexts.size() == 1 ) {
			// Single-index search
			resultOrNull = indexManagerContexts.iterator().next().model()
					.getFieldNode( absoluteFieldPath, IndexFieldFilter.INCLUDED_ONLY );
		}
		else {
			// Multi-index search
			List<LuceneSearchValueFieldContext<?>> fieldForEachIndex = new ArrayList<>();

			for ( LuceneScopeIndexManagerContext index : indexManagerContexts ) {
				LuceneIndexModel indexModel = index.model();
				LuceneIndexSchemaValueFieldNode<?> fieldForCurrentIndex =
						indexModel.getFieldNode( absoluteFieldPath, IndexFieldFilter.INCLUDED_ONLY );
				if ( fieldForCurrentIndex == null ) {
					continue;
				}
				fieldForEachIndex.add( fieldForCurrentIndex );
			}

			if ( !fieldForEachIndex.isEmpty() ) {
				resultOrNull = new LuceneMultiIndexSearchValueFieldContext<>(
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

		for ( LuceneScopeIndexManagerContext index : indexManagerContexts ) {
			LuceneIndexModel indexModel = index.model();
			LuceneIndexSchemaObjectFieldNode schemaNode =
					indexModel.getObjectFieldNode( absoluteFieldPath, IndexFieldFilter.INCLUDED_ONLY );
			if ( schemaNode != null ) {
				found = true;
				if ( !ObjectStructure.NESTED.equals( schemaNode.structure() ) ) {
					throw log.nonNestedFieldForNestedQuery(
							absoluteFieldPath, indexModel.getEventContext()
					);
				}
			}
		}
		if ( !found ) {
			for ( LuceneScopeIndexManagerContext index : indexManagerContexts ) {
				LuceneIndexModel indexModel = index.model();
				LuceneIndexSchemaValueFieldNode<?> schemaNode =
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
		Optional<List<String>> nestedDocumentPath = indexManagerContexts.stream()
				.map( index -> index.model().getObjectFieldNode( absoluteFieldPath, IndexFieldFilter.INCLUDED_ONLY ) )
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
