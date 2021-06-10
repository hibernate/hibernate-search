/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.scope.model.impl;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.hibernate.search.backend.lucene.analysis.model.impl.LuceneAnalysisDefinitionRegistry;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexField;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexModel;
import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.multitenancy.impl.MultiTenancyStrategy;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneMultiIndexSearchIndexCompositeNodeContext;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneMultiIndexSearchIndexValueFieldContext;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexCompositeNodeContext;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexScope;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexContext;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexNodeContext;
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

import org.apache.lucene.search.Query;

public final class LuceneSearchIndexScopeImpl implements LuceneSearchIndexScope {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private static final StringDocumentIdentifierValueConverter RAW_ID_CONVERTER =
			new StringDocumentIdentifierValueConverter();

	// Mapping context
	private final ToDocumentIdentifierValueConvertContext toDocumentIdentifierValueConvertContext;
	private final ToDocumentFieldValueConvertContext toDocumentFieldValueConvertContext;

	// Backend context
	private final LuceneAnalysisDefinitionRegistry analysisDefinitionRegistry;
	private final MultiTenancyStrategy multiTenancyStrategy;

	// Global timing source
	private final TimingSource timingSource;

	// Targeted indexes
	private final Map<String, LuceneScopeIndexManagerContext> mappedTypeNameToIndex;
	private final Set<String> indexNames;

	public LuceneSearchIndexScopeImpl(BackendMappingContext mappingContext,
			LuceneAnalysisDefinitionRegistry analysisDefinitionRegistry,
			MultiTenancyStrategy multiTenancyStrategy,
			TimingSource timingSource,
			Set<? extends LuceneScopeIndexManagerContext> indexManagerContexts) {
		this.toDocumentIdentifierValueConvertContext = new ToDocumentIdentifierValueConvertContextImpl( mappingContext );
		this.toDocumentFieldValueConvertContext = new ToDocumentFieldValueConvertContextImpl( mappingContext );
		this.analysisDefinitionRegistry = analysisDefinitionRegistry;
		this.multiTenancyStrategy = multiTenancyStrategy;
		this.timingSource = timingSource;
		// Use LinkedHashMap/LinkedHashSet to ensure stable order when generating requests
		this.mappedTypeNameToIndex = new LinkedHashMap<>();
		this.indexNames = new LinkedHashSet<>();
		for ( LuceneScopeIndexManagerContext indexManager : indexManagerContexts ) {
			this.mappedTypeNameToIndex.put( indexManager.model().mappedTypeName(), indexManager );
			this.indexNames.add( indexManager.model().hibernateSearchName() );
		}
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
	public LuceneAnalysisDefinitionRegistry analysisDefinitionRegistry() {
		return analysisDefinitionRegistry;
	}

	@Override
	public Query filterOrNull(String tenantId) {
		return multiTenancyStrategy.filterOrNull( tenantId );
	}

	@Override
	public TimeoutManager createTimeoutManager(Long timeout, TimeUnit timeUnit, boolean exceptionOnTimeout) {
		return TimeoutManager.of( timingSource, timeout, timeUnit, exceptionOnTimeout );
	}

	@Override
	public Collection<LuceneScopeIndexManagerContext> indexes() {
		return mappedTypeNameToIndex.values();
	}

	@Override
	public Map<String, ? extends LuceneSearchIndexContext> mappedTypeNameToIndex() {
		return mappedTypeNameToIndex;
	}

	@Override
	public Set<String> hibernateSearchIndexNames() {
		return indexNames;
	}

	@Override
	public DocumentIdentifierValueConverter<?> idDslConverter(ValueConvert valueConvert) {
		if ( ValueConvert.NO.equals( valueConvert ) ) {
			return RAW_ID_CONVERTER;
		}
		DocumentIdentifierValueConverter<?> converter = null;
		for ( LuceneScopeIndexManagerContext index : indexes() ) {
			DocumentIdentifierValueConverter<?> converterForIndex = index.model().idDslConverter();
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
	public LuceneSearchIndexCompositeNodeContext root() {
		if ( indexes().size() == 1 ) {
			return indexes().iterator().next().model().root();
		}
		else {
			List<LuceneSearchIndexCompositeNodeContext> rootForEachIndex = new ArrayList<>();
			for ( LuceneScopeIndexManagerContext index : indexes() ) {
				rootForEachIndex.add( index.model().root() );
			}
			return new LuceneMultiIndexSearchIndexCompositeNodeContext( this, null, rootForEachIndex );
		}
	}

	@Override
	public LuceneSearchIndexNodeContext field(String absoluteFieldPath) {
		LuceneSearchIndexNodeContext resultOrNull;
		if ( indexes().size() == 1 ) {
			resultOrNull = indexes().iterator().next().model().fieldOrNull( absoluteFieldPath );
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
	public boolean hasNestedDocuments() {
		for ( LuceneScopeIndexManagerContext element : indexes() ) {
			if ( element.model().hasNestedDocuments() ) {
				return true;
			}
		}
		return false;
	}

	private EventContext indexesEventContext() {
		return EventContexts.fromIndexNames( indexNames );
	}

	@SuppressWarnings({"rawtypes", "unchecked"}) // We check types using reflection
	private LuceneSearchIndexNodeContext createMultiIndexFieldContext(String absoluteFieldPath) {
		List<LuceneSearchIndexNodeContext> fieldForEachIndex = new ArrayList<>();
		LuceneScopeIndexManagerContext indexOfFirstField = null;
		LuceneIndexField firstField = null;

		for ( LuceneScopeIndexManagerContext index : indexes() ) {
			LuceneIndexModel indexModel = index.model();
			LuceneIndexField fieldForCurrentIndex = indexModel.fieldOrNull( absoluteFieldPath );
			if ( fieldForCurrentIndex == null ) {
				continue;
			}
			if ( firstField == null ) {
				indexOfFirstField = index;
				firstField = fieldForCurrentIndex;
			}
			else if ( firstField.isComposite() != fieldForCurrentIndex.isComposite() ) {
				SearchException cause = log.conflictingFieldModel();
				throw log.inconsistentConfigurationForIndexElementForSearch(
						EventContexts.fromIndexFieldAbsolutePath( absoluteFieldPath ), cause.getMessage(),
						EventContexts.fromIndexNames( indexOfFirstField.model().hibernateSearchName(),
								index.model().hibernateSearchName() ),
						cause );
			}
			fieldForEachIndex.add( fieldForCurrentIndex );
		}

		if ( fieldForEachIndex.isEmpty() ) {
			return null;
		}

		if ( firstField.isComposite() ) {
			return new LuceneMultiIndexSearchIndexCompositeNodeContext( this, absoluteFieldPath,
					(List) fieldForEachIndex );
		}
		else {
			return new LuceneMultiIndexSearchIndexValueFieldContext<>( this, absoluteFieldPath,
					(List) fieldForEachIndex );
		}
	}

}
