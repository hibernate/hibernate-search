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
import java.util.concurrent.TimeUnit;

import org.hibernate.search.backend.elasticsearch.common.impl.DocumentIdHelper;
import org.hibernate.search.backend.elasticsearch.document.model.impl.AbstractElasticsearchIndexSchemaFieldNode;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexModel;
import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.backend.elasticsearch.lowlevel.syntax.search.impl.ElasticsearchSearchSyntax;
import org.hibernate.search.backend.elasticsearch.multitenancy.impl.MultiTenancyStrategy;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchMultiIndexSearchCompositeIndexSchemaElementContext;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchMultiIndexSearchValueFieldContext;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchCompositeIndexSchemaElementContext;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchIndexScope;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchIndexContext;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchIndexSchemaElementContext;
import org.hibernate.search.engine.backend.mapping.spi.BackendMappingContext;
import org.hibernate.search.engine.backend.types.converter.runtime.ToDocumentFieldValueConvertContext;
import org.hibernate.search.engine.backend.types.converter.runtime.spi.ToDocumentFieldValueConvertContextImpl;
import org.hibernate.search.engine.backend.types.converter.runtime.spi.ToDocumentIdentifierValueConvertContext;
import org.hibernate.search.engine.backend.types.converter.runtime.spi.ToDocumentIdentifierValueConvertContextImpl;
import org.hibernate.search.engine.backend.types.converter.spi.DocumentIdentifierValueConverter;
import org.hibernate.search.engine.backend.types.converter.spi.StringDocumentIdentifierValueConverter;
import org.hibernate.search.engine.common.timing.spi.TimingSource;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.engine.search.common.ValueConvert;
import org.hibernate.search.engine.search.timeout.spi.TimeoutManager;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.reporting.EventContext;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

public final class ElasticsearchSearchIndexScopeImpl implements ElasticsearchSearchIndexScope {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private static final StringDocumentIdentifierValueConverter RAW_ID_CONVERTER =
			new StringDocumentIdentifierValueConverter();

	// Mapping context
	private final ToDocumentIdentifierValueConvertContext toDocumentIdentifierValueConvertContext;
	private final ToDocumentFieldValueConvertContext toDocumentFieldValueConvertContext;

	// Backend context
	private final Gson userFacingGson;
	private final ElasticsearchSearchSyntax searchSyntax;
	private final MultiTenancyStrategy multiTenancyStrategy;
	private final TimingSource timingSource;

	// Targeted indexes
	private final Set<ElasticsearchIndexModel> indexModels;
	private final Set<String> hibernateSearchIndexNames;
	private final Map<String, ElasticsearchSearchIndexContext> mappedTypeNameToIndex;
	private final int maxResultWindow;

	public ElasticsearchSearchIndexScopeImpl(BackendMappingContext mappingContext,
			Gson userFacingGson, ElasticsearchSearchSyntax searchSyntax,
			MultiTenancyStrategy multiTenancyStrategy,
			TimingSource timingSource,
			Set<ElasticsearchIndexModel> indexModels) {
		this.toDocumentIdentifierValueConvertContext = new ToDocumentIdentifierValueConvertContextImpl(
				mappingContext );
		this.toDocumentFieldValueConvertContext = new ToDocumentFieldValueConvertContextImpl( mappingContext );
		this.userFacingGson = userFacingGson;
		this.searchSyntax = searchSyntax;
		this.multiTenancyStrategy = multiTenancyStrategy;
		this.timingSource = timingSource;

		this.indexModels = indexModels;
		// Use LinkedHashMap/LinkedHashSet to ensure stable order when generating requests
		this.hibernateSearchIndexNames = new LinkedHashSet<>();
		this.mappedTypeNameToIndex = new LinkedHashMap<>();
		for ( ElasticsearchIndexModel model : indexModels ) {
			hibernateSearchIndexNames.add( model.hibernateSearchName() );
			mappedTypeNameToIndex.put( model.mappedTypeName(), model );
		}

		int currentMaxResultWindow = Integer.MAX_VALUE;
		for ( ElasticsearchIndexModel index : indexModels ) {
			if ( index.maxResultWindow() < currentMaxResultWindow ) {
				// take the minimum
				currentMaxResultWindow = index.maxResultWindow();
			}
		}
		this.maxResultWindow = currentMaxResultWindow;
	}

	@Override
	public ToDocumentIdentifierValueConvertContext toDocumentIdentifierValueConvertContext() {
		return toDocumentIdentifierValueConvertContext;
	}

	@Override
	public ToDocumentFieldValueConvertContext toDocumentFieldValueConvertContext() {
		return toDocumentFieldValueConvertContext;
	}

	@Override
	public Gson userFacingGson() {
		return userFacingGson;
	}

	@Override
	public ElasticsearchSearchSyntax searchSyntax() {
		return searchSyntax;
	}

	@Override
	public DocumentIdHelper documentIdHelper() {
		return multiTenancyStrategy.documentIdHelper();
	}

	@Override
	public JsonObject filterOrNull(String tenantId) {
		return multiTenancyStrategy.filterOrNull( tenantId );
	}

	@Override
	public TimeoutManager createTimeoutManager(Long timeout,
			TimeUnit timeUnit, boolean exceptionOnTimeout) {
		if ( timeout != null && timeUnit != null ) {
			if ( exceptionOnTimeout ) {
				return TimeoutManager.hardTimeout( timingSource, timeout, timeUnit );
			}
			else {
				return TimeoutManager.softTimeout( timingSource, timeout, timeUnit );
			}
		}
		return TimeoutManager.noTimeout( timingSource );
	}

	@Override
	public Collection<ElasticsearchSearchIndexContext> indexes() {
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
	public DocumentIdentifierValueConverter<?> idDslConverter(ValueConvert valueConvert) {
		if ( ValueConvert.NO.equals( valueConvert ) ) {
			return RAW_ID_CONVERTER;
		}
		DocumentIdentifierValueConverter<?> converter = null;
		for ( ElasticsearchIndexModel indexModel : indexModels ) {
			DocumentIdentifierValueConverter<?> converterForIndex = indexModel.idDslConverter();
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
	public ElasticsearchSearchCompositeIndexSchemaElementContext root() {
		if ( indexes().size() == 1 ) {
			return indexModels.iterator().next().root();
		}
		else {
			List<ElasticsearchSearchCompositeIndexSchemaElementContext> rootForEachIndex = new ArrayList<>();
			for ( ElasticsearchIndexModel indexModel : indexModels ) {
				rootForEachIndex.add( indexModel.root() );
			}
			return new ElasticsearchMultiIndexSearchCompositeIndexSchemaElementContext( hibernateSearchIndexNames,
					null, rootForEachIndex );
		}
	}

	@Override
	public ElasticsearchSearchIndexSchemaElementContext field(String absoluteFieldPath) {
		ElasticsearchSearchIndexSchemaElementContext resultOrNull;
		if ( indexes().size() == 1 ) {
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

	@Override
	public int maxResultWindow() {
		return maxResultWindow;
	}

	private EventContext indexesEventContext() {
		return EventContexts.fromIndexNames( hibernateSearchIndexNames() );
	}

	@SuppressWarnings({"rawtypes", "unchecked"}) // We check types using reflection
	private ElasticsearchSearchIndexSchemaElementContext createMultiIndexFieldContext(String absoluteFieldPath) {
		List<ElasticsearchSearchIndexSchemaElementContext> fieldForEachIndex = new ArrayList<>();
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
			else if ( firstField.isComposite() != fieldForCurrentIndex.isComposite() ) {
				SearchException cause = log.conflictingFieldModel();
				throw log.inconsistentConfigurationForIndexElementForSearch(
						EventContexts.fromIndexFieldAbsolutePath( absoluteFieldPath ), cause.getMessage(),
						EventContexts.fromIndexNames( indexModelOfFirstField.names().hibernateSearchIndex(),
								indexModel.names().hibernateSearchIndex() ),
						cause );
			}
			fieldForEachIndex.add( fieldForCurrentIndex );
		}

		if ( fieldForEachIndex.isEmpty() ) {
			return null;
		}

		if ( fieldForEachIndex.get( 0 ).isComposite() ) {
			return new ElasticsearchMultiIndexSearchCompositeIndexSchemaElementContext( hibernateSearchIndexNames,
					absoluteFieldPath, (List) fieldForEachIndex );
		}
		else {
			return new ElasticsearchMultiIndexSearchValueFieldContext<>( hibernateSearchIndexNames, absoluteFieldPath,
					(List) fieldForEachIndex );
		}
	}
}
