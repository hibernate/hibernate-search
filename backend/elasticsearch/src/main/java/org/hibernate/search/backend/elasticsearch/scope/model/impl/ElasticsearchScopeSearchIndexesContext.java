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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.search.backend.elasticsearch.document.model.impl.AbstractElasticsearchIndexSchemaFieldNode;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexModel;
import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchMultiIndexSearchObjectFieldContext;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchMultiIndexSearchValueFieldContext;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchFieldContext;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchIndexContext;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchIndexesContext;
import org.hibernate.search.engine.backend.types.converter.spi.StringToDocumentIdentifierValueConverter;
import org.hibernate.search.engine.backend.types.converter.spi.ToDocumentIdentifierValueConverter;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.engine.search.common.ValueConvert;
import org.hibernate.search.util.common.SearchException;
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
	public ElasticsearchSearchFieldContext field(String absoluteFieldPath) {
		ElasticsearchSearchFieldContext resultOrNull;
		if ( elements().size() == 1 ) {
			resultOrNull = indexModels.iterator().next().fieldOrNull( absoluteFieldPath );
		}
		else {
			resultOrNull = createMultiIndexFieldContext( absoluteFieldPath );
		}
		if ( resultOrNull == null ) {
			throw log.unknownFieldForSearch( absoluteFieldPath, indexesEventContext() );
		}
		return resultOrNull;
	}

	private EventContext indexesEventContext() {
		return EventContexts.fromIndexNames( hibernateSearchIndexNames() );
	}

	@SuppressWarnings({"rawtypes", "unchecked"}) // We check types using reflection
	private ElasticsearchSearchFieldContext createMultiIndexFieldContext(String absoluteFieldPath) {
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
			else if ( firstField.isObjectField() != fieldForCurrentIndex.isObjectField() ) {
				SearchException cause = log.conflictingFieldModel();
				throw log.inconsistentConfigurationForFieldForSearch( absoluteFieldPath, cause.getMessage(),
						EventContexts.fromIndexNames( indexModelOfFirstField.names().hibernateSearchIndex(),
								indexModel.names().hibernateSearchIndex() ),
						cause );
			}
			fieldForEachIndex.add( fieldForCurrentIndex );
		}

		if ( fieldForEachIndex.isEmpty() ) {
			return null;
		}

		if ( fieldForEachIndex.get( 0 ).isObjectField() ) {
			return new ElasticsearchMultiIndexSearchObjectFieldContext( hibernateSearchIndexNames, absoluteFieldPath,
					(List) fieldForEachIndex );
		}
		else {
			return new ElasticsearchMultiIndexSearchValueFieldContext<>( hibernateSearchIndexNames, absoluteFieldPath,
					(List) fieldForEachIndex );
		}
	}
}
