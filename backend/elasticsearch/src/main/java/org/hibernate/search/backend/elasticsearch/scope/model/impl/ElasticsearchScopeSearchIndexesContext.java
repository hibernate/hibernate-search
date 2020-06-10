/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.scope.model.impl;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexModel;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexSchemaFieldNode;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexSchemaObjectFieldNode;
import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchMultiIndexSearchFieldContext;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchFieldContext;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchIndexesContext;
import org.hibernate.search.backend.elasticsearch.util.spi.URLEncodedString;
import org.hibernate.search.engine.backend.document.model.spi.IndexFieldFilter;
import org.hibernate.search.engine.backend.types.converter.spi.ToDocumentIdentifierValueConverter;
import org.hibernate.search.engine.backend.document.model.dsl.ObjectFieldStorage;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.util.common.reporting.EventContext;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public class ElasticsearchScopeSearchIndexesContext implements ElasticsearchSearchIndexesContext {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final Set<ElasticsearchIndexModel> indexModels;
	private final Set<String> hibernateSearchIndexNames;
	private final Map<String, URLEncodedString> mappedTypeToElasticsearchIndexNames;

	public ElasticsearchScopeSearchIndexesContext(Set<ElasticsearchIndexModel> indexModels) {
		this.indexModels = indexModels;
		// Use LinkedHashMap/LinkedHashSet to ensure stable order when generating requests
		this.hibernateSearchIndexNames = new LinkedHashSet<>();
		this.mappedTypeToElasticsearchIndexNames = new LinkedHashMap<>();
		for ( ElasticsearchIndexModel model : indexModels ) {
			hibernateSearchIndexNames.add( model.hibernateSearchName() );
			mappedTypeToElasticsearchIndexNames.put( model.getMappedTypeName(), model.getNames().getRead() );
		}
	}

	@Override
	public Set<String> mappedTypeNames() {
		return mappedTypeToElasticsearchIndexNames.keySet();
	}

	@Override
	public Set<String> hibernateSearchIndexNames() {
		return hibernateSearchIndexNames;
	}

	@Override
	public Collection<URLEncodedString> elasticsearchIndexNames() {
		return mappedTypeToElasticsearchIndexNames.values();
	}

	@Override
	public Map<String, URLEncodedString> mappedTypeToElasticsearchIndexNames() {
		return mappedTypeToElasticsearchIndexNames;
	}

	@Override
	public ElasticsearchScopedIndexRootComponent<ToDocumentIdentifierValueConverter<?>> idDslConverter() {
		Iterator<ElasticsearchIndexModel> iterator = indexModels.iterator();
		ElasticsearchIndexModel indexModelForSelectedIdConverter = null;
		ToDocumentIdentifierValueConverter<?> selectedIdConverter = null;
		ElasticsearchScopedIndexRootComponent<ToDocumentIdentifierValueConverter<?>> scopedIndexFieldComponent =
				new ElasticsearchScopedIndexRootComponent<>();

		while ( iterator.hasNext() ) {
			ElasticsearchIndexModel indexModel = iterator.next();
			ToDocumentIdentifierValueConverter<?> idConverter = indexModel.getIdDslConverter();

			if ( selectedIdConverter == null ) {
				indexModelForSelectedIdConverter = indexModel;
				selectedIdConverter = idConverter;
				scopedIndexFieldComponent.setComponent( selectedIdConverter );
				continue;
			}

			if ( !selectedIdConverter.isCompatibleWith( idConverter ) ) {
				ElasticsearchFailingIdCompatibilityChecker failingCompatibilityChecker =
						new ElasticsearchFailingIdCompatibilityChecker(
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
	@SuppressWarnings("unchecked") // We check types using reflection (see calls to type().valueType())
	public ElasticsearchSearchFieldContext<?> field(String absoluteFieldPath) {
		ElasticsearchSearchFieldContext<?> resultOrNull = null;
		if ( indexModels.size() == 1 ) {
			// Single-index search
			resultOrNull = indexModels.iterator().next().getFieldNode( absoluteFieldPath, IndexFieldFilter.INCLUDED_ONLY );
		}
		else {
			// Multi-index search
			Class<?> fieldValueTypeForAllIndexes = null;
			List<ElasticsearchSearchFieldContext<?>> fieldForEachIndex = new ArrayList<>();

			for ( ElasticsearchIndexModel indexModel : indexModels ) {
				ElasticsearchIndexSchemaFieldNode<?> fieldForCurrentIndex =
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
				resultOrNull = new ElasticsearchMultiIndexSearchFieldContext<>(
						hibernateSearchIndexNames(), absoluteFieldPath, (List) fieldForEachIndex
				);
			}
		}
		if ( resultOrNull == null ) {
			throw log.unknownFieldForSearch( absoluteFieldPath, indexesEventContext() );
		}
		return resultOrNull;
	}

	@Override
	public boolean hasSchemaObjectNodeComponent(String absoluteFieldPath) {
		for ( ElasticsearchIndexModel indexModel : indexModels ) {
			ElasticsearchIndexSchemaObjectFieldNode objectNode =
					indexModel.getObjectFieldNode( absoluteFieldPath, IndexFieldFilter.INCLUDED_ONLY );
			// Even if we have an inconsistency with the Lucene backend,
			// we decide to be very lenient here,
			// allowing ALL the model incompatibilities Elasticsearch allows.
			if ( objectNode != null ) {
				return true;
			}
		}

		return false;
	}

	@Override
	public void checkNestedField(String absoluteFieldPath) {
		boolean found = false;

		for ( ElasticsearchIndexModel indexModel : indexModels ) {
			ElasticsearchIndexSchemaObjectFieldNode schemaNode =
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
			for ( ElasticsearchIndexModel indexModel : indexModels ) {
				ElasticsearchIndexSchemaFieldNode<?> schemaNode =
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
	public List<String> nestedPathHierarchyForObject(String absoluteObjectPath) {
		Optional<List<String>> nestedDocumentPath = indexModels.stream()
				.map( indexModel -> indexModel.getObjectFieldNode( absoluteObjectPath, IndexFieldFilter.INCLUDED_ONLY ) )
				.filter( Objects::nonNull )
				.map( node -> Optional.ofNullable( node.nestedPathHierarchy() ) )
				.reduce( (nestedDocumentPath1, nestedDocumentPath2) -> {
					if ( Objects.equals( nestedDocumentPath1, nestedDocumentPath2 ) ) {
						return nestedDocumentPath1;
					}

					throw log.conflictingNestedDocumentPathHierarchy(
							absoluteObjectPath, nestedDocumentPath1.orElse( null ), nestedDocumentPath2.orElse( null ), indexesEventContext() );
				} )
				.orElse( Optional.empty() );

		return nestedDocumentPath.orElse( Collections.emptyList() );
	}

	private EventContext indexesEventContext() {
		return EventContexts.fromIndexNames( hibernateSearchIndexNames() );
	}
}
