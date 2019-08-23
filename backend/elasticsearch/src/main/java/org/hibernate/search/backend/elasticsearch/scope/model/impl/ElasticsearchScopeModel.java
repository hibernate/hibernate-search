/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.scope.model.impl;

import java.lang.invoke.MethodHandles;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

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
	private final Set<String> hibernateSearchIndexNames;
	private final Set<URLEncodedString> elasticsearchIndexNames;

	public ElasticsearchScopeModel(Set<ElasticsearchIndexModel> indexModels) {
		this.indexModels = indexModels;
		this.hibernateSearchIndexNames = indexModels.stream()
				.map( ElasticsearchIndexModel::getHibernateSearchIndexName )
				.collect( Collectors.toSet() );
		// Use LinkedHashSet to ensure stable order when generating requests
		this.elasticsearchIndexNames = indexModels.stream()
				.map( ElasticsearchIndexModel::getElasticsearchIndexName )
				.collect( Collectors.toCollection( LinkedHashSet::new ) );
	}

	public Set<String> getHibernateSearchIndexNames() {
		return hibernateSearchIndexNames;
	}

	public Set<URLEncodedString> getElasticsearchIndexNames() {
		return elasticsearchIndexNames;
	}

	public EventContext getIndexesEventContext() {
		return EventContexts.fromIndexNames( hibernateSearchIndexNames );
	}

	public ToDocumentIdentifierValueConverter<?> getIdDslConverter() {
		Iterator<ElasticsearchIndexModel> iterator = indexModels.iterator();
		ElasticsearchIndexModel indexModelForSelectedIdConverter = iterator.next();
		ToDocumentIdentifierValueConverter<?> selectedIdConverter = indexModelForSelectedIdConverter.getIdDslConverter();

		while ( iterator.hasNext() ) {
			ElasticsearchIndexModel indexModel = iterator.next();
			ToDocumentIdentifierValueConverter<?> idConverter = indexModel.getIdDslConverter();
			if ( !selectedIdConverter.isCompatibleWith( idConverter ) ) {
				throw log.conflictingIdentifierTypesForPredicate(
						selectedIdConverter, idConverter,
						EventContexts.fromIndexNames(
								indexModelForSelectedIdConverter.getHibernateSearchIndexName(),
								indexModel.getHibernateSearchIndexName()
						)
				);
			}
		}

		return selectedIdConverter;
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
			ElasticsearchFailingCompatibilityChecker<T> failingCompatibilityChecker = new ElasticsearchFailingCompatibilityChecker<>(
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

					throw log.conflictingNestedDocumentPathsForProjection(
							absoluteFieldPath, nestedDocumentPath1.orElse( null ), nestedDocumentPath2.orElse( null ), getIndexesEventContext() );
				} )
				.orElse( Optional.empty() );

		return nestedDocumentPath.orElse( null );
	}

	public List<String> getNestedPathHierarchy(String absoluteFieldPath) {
		Optional<List<String>> nestedDocumentPath = indexModels.stream()
				.map( indexModel -> indexModel.getFieldNode( absoluteFieldPath ) )
				.filter( Objects::nonNull )
				.map( fieldNode -> Optional.ofNullable( fieldNode.getNestedPathHierarchy() ) )
				.reduce( (nestedDocumentPath1, nestedDocumentPath2) -> {
					if ( Objects.equals( nestedDocumentPath1, nestedDocumentPath2 ) ) {
						return nestedDocumentPath1;
					}

					throw log.conflictingNestedDocumentPathHierarchyForProjection(
							absoluteFieldPath, nestedDocumentPath1.orElse( null ), nestedDocumentPath2.orElse( null ), getIndexesEventContext() );
				} )
				.orElse( Optional.empty() );

		return nestedDocumentPath.orElse( null );
	}
}
