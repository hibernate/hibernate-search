/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.scope.model.impl;

import java.lang.invoke.MethodHandles;
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
import org.hibernate.search.backend.elasticsearch.util.spi.URLEncodedString;
import org.hibernate.search.engine.backend.document.model.spi.IndexFieldFilter;
import org.hibernate.search.engine.backend.types.converter.spi.ToDocumentIdentifierValueConverter;
import org.hibernate.search.engine.backend.document.model.dsl.ObjectFieldStorage;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.util.common.reporting.EventContext;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public class ElasticsearchScopeModel {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final Set<ElasticsearchIndexModel> indexModels;
	private final Set<String> hibernateSearchIndexNames;
	private final Map<String, URLEncodedString> mappedTypeToElasticsearchIndexNames;

	public ElasticsearchScopeModel(Set<ElasticsearchIndexModel> indexModels) {
		this.indexModels = indexModels;
		// Use LinkedHashMap/LinkedHashSet to ensure stable order when generating requests
		this.hibernateSearchIndexNames = new LinkedHashSet<>();
		this.mappedTypeToElasticsearchIndexNames = new LinkedHashMap<>();
		for ( ElasticsearchIndexModel model : indexModels ) {
			hibernateSearchIndexNames.add( model.hibernateSearchName() );
			mappedTypeToElasticsearchIndexNames.put( model.getMappedTypeName(), model.getNames().getRead() );
		}
	}

	public Set<String> mappedTypeNames() {
		return mappedTypeToElasticsearchIndexNames.keySet();
	}

	public Set<String> hibernateSearchIndexNames() {
		return hibernateSearchIndexNames;
	}

	public Collection<URLEncodedString> elasticsearchIndexNames() {
		return mappedTypeToElasticsearchIndexNames.values();
	}

	public Map<String, URLEncodedString> mappedTypeToElasticsearchIndexNames() {
		return mappedTypeToElasticsearchIndexNames;
	}

	public EventContext indexesEventContext() {
		return EventContexts.fromIndexNames( hibernateSearchIndexNames() );
	}

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

	public <T> ElasticsearchScopedIndexFieldComponent<T> schemaNodeComponent(String absoluteFieldPath,
			IndexSchemaFieldNodeComponentRetrievalStrategy<T> componentRetrievalStrategy) {
		ElasticsearchIndexModel indexModelForSelectedSchemaNode = null;
		ElasticsearchIndexSchemaFieldNode<?> selectedSchemaNode = null;
		ElasticsearchScopedIndexFieldComponent<T> scopedIndexFieldComponent = new ElasticsearchScopedIndexFieldComponent<>();

		for ( ElasticsearchIndexModel indexModel : indexModels ) {
			ElasticsearchIndexSchemaFieldNode<?> schemaNode =
					indexModel.getFieldNode( absoluteFieldPath, IndexFieldFilter.INCLUDED_ONLY );
			if ( schemaNode == null ) {
				continue;
			}

			T component = componentRetrievalStrategy.extractComponent( schemaNode );
			if ( selectedSchemaNode == null ) {
				selectedSchemaNode = schemaNode;
				indexModelForSelectedSchemaNode = indexModel;
				scopedIndexFieldComponent.setComponent( component );
				scopedIndexFieldComponent.setMultiValuedFieldInRoot( schemaNode.multiValuedInRoot() );
				continue;
			}

			if ( !componentRetrievalStrategy.hasCompatibleCodec( scopedIndexFieldComponent.getComponent(), component ) ) {
				throw componentRetrievalStrategy.createCompatibilityException(
						absoluteFieldPath,
						scopedIndexFieldComponent.getComponent(),
						component,
						EventContexts.fromIndexNames(
								indexModelForSelectedSchemaNode.hibernateSearchName(),
								indexModel.hibernateSearchName()
						)
				);
			}
			scopedIndexFieldComponent.setMultiValuedFieldInRoot(
					scopedIndexFieldComponent.isMultiValuedFieldInRoot() || schemaNode.multiValued()
			);
			ElasticsearchFailingFieldCompatibilityChecker<T> failingCompatibilityChecker = new ElasticsearchFailingFieldCompatibilityChecker<>(
					absoluteFieldPath, scopedIndexFieldComponent.getComponent(), component, EventContexts.fromIndexNames(
					indexModelForSelectedSchemaNode.hibernateSearchName(), indexModel.hibernateSearchName()
			), componentRetrievalStrategy );

			if ( !componentRetrievalStrategy.hasCompatibleConverter( scopedIndexFieldComponent.getComponent(), component ) ) {
				scopedIndexFieldComponent.setConverterCompatibilityChecker( failingCompatibilityChecker );
			}
			if ( !componentRetrievalStrategy.hasCompatibleAnalyzer( scopedIndexFieldComponent.getComponent(), component ) ) {
				scopedIndexFieldComponent.setAnalyzerCompatibilityChecker( failingCompatibilityChecker );
			}
		}
		if ( selectedSchemaNode == null ) {
			throw log.unknownFieldForSearch( absoluteFieldPath, indexesEventContext() );
		}

		return scopedIndexFieldComponent;
	}

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

	public String nestedDocumentPath(String absoluteFieldPath) {
		Optional<String> nestedDocumentPath = indexModels.stream()
				.map( indexModel -> indexModel.getFieldNode( absoluteFieldPath, IndexFieldFilter.INCLUDED_ONLY ) )
				.filter( Objects::nonNull )
				.map( fieldNode -> Optional.ofNullable( fieldNode.nestedPath() ) )
				.reduce( (nestedDocumentPath1, nestedDocumentPath2) -> {
					if ( Objects.equals( nestedDocumentPath1, nestedDocumentPath2 ) ) {
						return nestedDocumentPath1;
					}

					throw log.conflictingNestedDocumentPaths(
							absoluteFieldPath, nestedDocumentPath1.orElse( null ), nestedDocumentPath2.orElse( null ), indexesEventContext() );
				} )
				.orElse( Optional.empty() );

		return nestedDocumentPath.orElse( null );
	}

	public List<String> nestedPathHierarchyForField(String absoluteFieldPath) {
		Optional<List<String>> nestedDocumentPath = indexModels.stream()
				.map( indexModel -> indexModel.getFieldNode( absoluteFieldPath, IndexFieldFilter.INCLUDED_ONLY ) )
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
}
