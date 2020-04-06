/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.scope.model.impl;

import java.lang.invoke.MethodHandles;
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
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexSchemaObjectNode;
import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.backend.elasticsearch.util.spi.URLEncodedString;
import org.hibernate.search.engine.backend.types.converter.spi.ToDocumentIdentifierValueConverter;
import org.hibernate.search.engine.backend.document.model.dsl.ObjectFieldStorage;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.util.common.reporting.EventContext;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public class ElasticsearchScopeModel {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final Set<ElasticsearchIndexModel> indexModels;
	private final Map<String, URLEncodedString> hibernateSearchIndexNamesToIndexReadNames;
	private final Set<String> mappedTypeNames;

	public ElasticsearchScopeModel(Set<ElasticsearchIndexModel> indexModels) {
		this.indexModels = indexModels;
		// Use LinkedHashMap/LinkedHashSet to ensure stable order when generating requests
		this.hibernateSearchIndexNamesToIndexReadNames = new LinkedHashMap<>();
		this.mappedTypeNames = new LinkedHashSet<>();
		for ( ElasticsearchIndexModel model : indexModels ) {
			hibernateSearchIndexNamesToIndexReadNames.put( model.getHibernateSearchIndexName(), model.getNames().getRead() );
			mappedTypeNames.add( model.getMappedTypeName() );
		}
	}

	public Set<String> getHibernateSearchIndexNames() {
		return hibernateSearchIndexNamesToIndexReadNames.keySet();
	}

	public Map<String, URLEncodedString> getHibernateSearchIndexNamesToIndexReadNames() {
		return hibernateSearchIndexNamesToIndexReadNames;
	}

	public Set<String> getMappedTypeNames() {
		return mappedTypeNames;
	}

	public EventContext getIndexesEventContext() {
		return EventContexts.fromIndexNames( getHibernateSearchIndexNames() );
	}

	public ElasticsearchScopedIndexRootComponent<ToDocumentIdentifierValueConverter<?>> getIdDslConverter() {
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
										indexModelForSelectedIdConverter.getHibernateSearchIndexName(),
										indexModel.getHibernateSearchIndexName()
								)
						);
				scopedIndexFieldComponent.setIdConverterCompatibilityChecker( failingCompatibilityChecker );
			}
		}

		return scopedIndexFieldComponent;
	}

	public <T> ElasticsearchScopedIndexFieldComponent<T> getSchemaNodeComponent(String absoluteFieldPath,
			IndexSchemaFieldNodeComponentRetrievalStrategy<T> componentRetrievalStrategy) {
		ElasticsearchIndexModel indexModelForSelectedSchemaNode = null;
		ElasticsearchIndexSchemaFieldNode<?> selectedSchemaNode = null;
		ElasticsearchScopedIndexFieldComponent<T> scopedIndexFieldComponent = new ElasticsearchScopedIndexFieldComponent<>();

		for ( ElasticsearchIndexModel indexModel : indexModels ) {
			ElasticsearchIndexSchemaFieldNode<?> schemaNode = indexModel.getFieldNode( absoluteFieldPath );
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
								indexModelForSelectedSchemaNode.getHibernateSearchIndexName(),
								indexModel.getHibernateSearchIndexName()
						)
				);
			}
			ElasticsearchFailingFieldCompatibilityChecker<T> failingCompatibilityChecker = new ElasticsearchFailingFieldCompatibilityChecker<>(
					absoluteFieldPath, scopedIndexFieldComponent.getComponent(), component, EventContexts.fromIndexNames(
					indexModelForSelectedSchemaNode.getHibernateSearchIndexName(), indexModel.getHibernateSearchIndexName()
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

	public boolean hasSchemaObjectNodeComponent(String absoluteFieldPath) {
		for ( ElasticsearchIndexModel indexModel : indexModels ) {
			ElasticsearchIndexSchemaObjectNode objectNode = indexModel.getObjectNode( absoluteFieldPath );
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
			ElasticsearchIndexSchemaObjectNode schemaNode = indexModel.getObjectNode( absoluteFieldPath );
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
			for ( ElasticsearchIndexModel indexModel : indexModels ) {
				ElasticsearchIndexSchemaFieldNode<?> schemaNode = indexModel.getFieldNode( absoluteFieldPath );
				if ( schemaNode != null ) {
					throw log.nonObjectFieldForNestedQuery(
							absoluteFieldPath, indexModel.getEventContext()
					);
				}
			}
			throw log.unknownFieldForSearch( absoluteFieldPath, getIndexesEventContext() );
		}
	}

	public String getNestedDocumentPath(String absoluteFieldPath) {
		Optional<String> nestedDocumentPath = indexModels.stream()
				.map( indexModel -> indexModel.getFieldNode( absoluteFieldPath ) )
				.filter( Objects::nonNull )
				.map( fieldNode -> Optional.ofNullable( fieldNode.getNestedPath() ) )
				.reduce( (nestedDocumentPath1, nestedDocumentPath2) -> {
					if ( Objects.equals( nestedDocumentPath1, nestedDocumentPath2 ) ) {
						return nestedDocumentPath1;
					}

					throw log.conflictingNestedDocumentPaths(
							absoluteFieldPath, nestedDocumentPath1.orElse( null ), nestedDocumentPath2.orElse( null ), getIndexesEventContext() );
				} )
				.orElse( Optional.empty() );

		return nestedDocumentPath.orElse( null );
	}

	public List<String> getNestedPathHierarchyForField(String absoluteFieldPath) {
		Optional<List<String>> nestedDocumentPath = indexModels.stream()
				.map( indexModel -> indexModel.getFieldNode( absoluteFieldPath ) )
				.filter( Objects::nonNull )
				.map( node -> Optional.ofNullable( node.getNestedPathHierarchy() ) )
				.reduce( (nestedDocumentPath1, nestedDocumentPath2) -> {
					if ( Objects.equals( nestedDocumentPath1, nestedDocumentPath2 ) ) {
						return nestedDocumentPath1;
					}

					throw log.conflictingNestedDocumentPathHierarchy(
							absoluteFieldPath, nestedDocumentPath1.orElse( null ), nestedDocumentPath2.orElse( null ), getIndexesEventContext() );
				} )
				.orElse( Optional.empty() );

		return nestedDocumentPath.orElse( Collections.emptyList() );
	}

	public List<String> getNestedPathHierarchyForObject(String absoluteObjectPath) {
		Optional<List<String>> nestedDocumentPath = indexModels.stream()
				.map( indexModel -> indexModel.getObjectNode( absoluteObjectPath ) )
				.filter( Objects::nonNull )
				.map( node -> Optional.ofNullable( node.getNestedPathHierarchy() ) )
				.reduce( (nestedDocumentPath1, nestedDocumentPath2) -> {
					if ( Objects.equals( nestedDocumentPath1, nestedDocumentPath2 ) ) {
						return nestedDocumentPath1;
					}

					throw log.conflictingNestedDocumentPathHierarchy(
							absoluteObjectPath, nestedDocumentPath1.orElse( null ), nestedDocumentPath2.orElse( null ), getIndexesEventContext() );
				} )
				.orElse( Optional.empty() );

		return nestedDocumentPath.orElse( Collections.emptyList() );
	}
}
