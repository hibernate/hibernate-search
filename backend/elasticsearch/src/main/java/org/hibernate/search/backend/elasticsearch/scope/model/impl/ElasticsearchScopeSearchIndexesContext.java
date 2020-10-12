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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.hibernate.search.backend.elasticsearch.document.model.impl.AbstractElasticsearchIndexSchemaFieldNode;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexModel;
import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchMultiIndexSearchObjectFieldContext;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchMultiIndexSearchValueFieldContext;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchFieldContext;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchIndexContext;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchIndexesContext;
import org.hibernate.search.engine.backend.types.ObjectStructure;
import org.hibernate.search.engine.backend.types.converter.spi.StringToDocumentIdentifierValueConverter;
import org.hibernate.search.engine.backend.types.converter.spi.ToDocumentIdentifierValueConverter;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.engine.search.common.ValueConvert;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.reporting.EventContext;

public class ElasticsearchScopeSearchIndexesContext implements ElasticsearchSearchIndexesContext {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private static final StringToDocumentIdentifierValueConverter RAW_ID_CONVERTER =
			new StringToDocumentIdentifierValueConverter();

	private final Set<ElasticsearchIndexModel> indexModels;
	private final Set<String> hibernateSearchIndexNames;
	private final Map<String, ElasticsearchSearchIndexContext> mappedTypeNameToIndex;

	public ElasticsearchScopeSearchIndexesContext(Set<ElasticsearchIndexModel> indexModels) {
		this.indexModels = indexModels;
		// Use LinkedHashMap/LinkedHashSet to ensure stable order when generating requests
		this.hibernateSearchIndexNames = new LinkedHashSet<>();
		this.mappedTypeNameToIndex = new LinkedHashMap<>();
		for ( ElasticsearchIndexModel model : indexModels ) {
			hibernateSearchIndexNames.add( model.hibernateSearchName() );
			mappedTypeNameToIndex.put( model.mappedTypeName(), model );
		}
	}

	@Override
	public Collection<ElasticsearchSearchIndexContext> elements() {
		return mappedTypeNameToIndex.values();
	}

	@Override
	public Set<String> hibernateSearchIndexNames() {
		return hibernateSearchIndexNames;
	}

	@Override
	public Map<String, ElasticsearchSearchIndexContext> mappedTypeNameToIndex() {
		return mappedTypeNameToIndex;
	}

	@Override
	public ToDocumentIdentifierValueConverter<?> idDslConverter(ValueConvert valueConvert) {
		if ( ValueConvert.NO.equals( valueConvert ) ) {
			return RAW_ID_CONVERTER;
		}
		ToDocumentIdentifierValueConverter<?> converter = null;
		for ( ElasticsearchIndexModel indexModel : indexModels ) {
			ToDocumentIdentifierValueConverter<?> converterForIndex = indexModel.idDslConverter();
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
	@SuppressWarnings("unchecked") // We check types using reflection (see calls to type().valueType())
	public ElasticsearchSearchFieldContext field(String absoluteFieldPath) {
		ElasticsearchSearchFieldContext resultOrNull = null;
		if ( elements().size() == 1 ) {
			// Single-index search
			resultOrNull = indexModels.iterator().next().fieldOrNull( absoluteFieldPath );
		}
		else {
			// Multi-index search
			List<ElasticsearchSearchFieldContext> fieldForEachIndex = new ArrayList<>();
			ElasticsearchSearchIndexContext indexModelOfFirstField = null;
			AbstractElasticsearchIndexSchemaFieldNode firstField = null;

			for ( ElasticsearchIndexModel indexModel : indexModels ) {
				AbstractElasticsearchIndexSchemaFieldNode fieldForCurrentIndex =
						indexModel.fieldOrNull( absoluteFieldPath );
				if ( fieldForCurrentIndex == null ) {
					continue;
				}
				if ( firstField == null ) {
					indexModelOfFirstField = indexModel;
					firstField = fieldForCurrentIndex;
				}
				else {
					if ( firstField.isObjectField() != fieldForCurrentIndex.isObjectField() ) {
						throw log.conflictingFieldModel( absoluteFieldPath, firstField, fieldForCurrentIndex,
								EventContexts.fromIndexNames( indexModelOfFirstField.names().getHibernateSearch(),
										indexModel.names().getHibernateSearch() ) );
					}
				}
				fieldForEachIndex.add( fieldForCurrentIndex );
			}

			if ( !fieldForEachIndex.isEmpty() ) {
				if ( firstField.isObjectField() ) {
					resultOrNull = new ElasticsearchMultiIndexSearchObjectFieldContext(
							hibernateSearchIndexNames, absoluteFieldPath, (List) fieldForEachIndex
					);
				}
				else {
					resultOrNull = new ElasticsearchMultiIndexSearchValueFieldContext<>(
							hibernateSearchIndexNames, absoluteFieldPath, (List) fieldForEachIndex
					);
				}
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
			AbstractElasticsearchIndexSchemaFieldNode field = indexModel.fieldOrNull( absoluteFieldPath );
			// Even if we have an inconsistency with the Lucene backend,
			// we decide to be very lenient here,
			// allowing ALL the model incompatibilities Elasticsearch allows.
			if ( field != null && field.isObjectField() ) {
				return true;
			}
		}

		return false;
	}

	@Override
	public void checkNestedField(String absoluteFieldPath) {
		boolean found = false;

		for ( ElasticsearchIndexModel indexModel : indexModels ) {
			AbstractElasticsearchIndexSchemaFieldNode schemaNode = indexModel.fieldOrNull( absoluteFieldPath );
			if ( schemaNode == null ) {
				continue;
			}
			found = true;
			if ( !schemaNode.isObjectField() ) {
				throw log.nonObjectFieldForNestedQuery(
						absoluteFieldPath, indexModel.getEventContext()
				);
			}
			if ( !ObjectStructure.NESTED.equals( schemaNode.toObjectField().structure() ) ) {
				throw log.nonNestedFieldForNestedQuery(
						absoluteFieldPath, indexModel.getEventContext()
				);
			}
		}
		if ( !found ) {
			throw log.unknownFieldForSearch( absoluteFieldPath, indexesEventContext() );
		}
	}

	@Override
	public List<String> nestedPathHierarchyForObject(String absoluteObjectPath) {
		Optional<List<String>> nestedDocumentPath = indexModels.stream()
				.map( indexModel -> indexModel.fieldOrNull( absoluteObjectPath ) )
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
