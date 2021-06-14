/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.backend.scope.spi;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.search.engine.backend.document.model.spi.AbstractIndexModel;
import org.hibernate.search.engine.backend.mapping.spi.BackendMappingContext;
import org.hibernate.search.engine.backend.types.converter.runtime.ToDocumentFieldValueConvertContext;
import org.hibernate.search.engine.backend.types.converter.runtime.spi.ToDocumentIdentifierValueConvertContext;
import org.hibernate.search.engine.backend.types.converter.spi.DocumentIdentifierValueConverter;
import org.hibernate.search.engine.backend.types.converter.spi.StringDocumentIdentifierValueConverter;
import org.hibernate.search.engine.logging.impl.Log;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.engine.search.common.ValueConvert;
import org.hibernate.search.engine.search.common.spi.SearchIndexCompositeNodeContext;
import org.hibernate.search.engine.search.common.spi.SearchIndexNodeContext;
import org.hibernate.search.engine.search.common.spi.SearchIndexScope;
import org.hibernate.search.engine.search.common.spi.SearchQueryElementTypeKey;
import org.hibernate.search.engine.search.query.spi.SearchQueryIndexScope;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.reporting.EventContext;

public abstract class AbstractSearchIndexScope<
				S extends SearchIndexScope,
				M extends AbstractIndexModel<?, ? extends C, ? extends N>,
				N extends SearchIndexNodeContext<S>,
				C extends SearchIndexCompositeNodeContext<S>
		>
		implements SearchIndexScope, SearchQueryIndexScope {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private static final StringDocumentIdentifierValueConverter RAW_ID_CONVERTER =
			new StringDocumentIdentifierValueConverter();

	// Mapping context
	private final BackendMappingContext mappingContext;

	// Targeted indexes
	private final Set<String> hibernateSearchIndexNames;
	private final Set<? extends M> indexModels;

	public AbstractSearchIndexScope(BackendMappingContext mappingContext, Set<? extends M> indexModels) {
		this.mappingContext = mappingContext;

		// Use LinkedHashMap/LinkedHashSet to ensure stable order when generating requests
		this.hibernateSearchIndexNames = new LinkedHashSet<>();
		for ( M model : indexModels ) {
			hibernateSearchIndexNames.add( model.hibernateSearchName() );
		}
		this.indexModels = indexModels;
	}

	protected final EventContext indexesEventContext() {
		return EventContexts.fromIndexNames( hibernateSearchIndexNames );
	}

	protected abstract S self();

	@Override
	public final ToDocumentIdentifierValueConvertContext toDocumentIdentifierValueConvertContext() {
		return mappingContext.toDocumentIdentifierValueConvertContext();
	}

	@Override
	public final ToDocumentFieldValueConvertContext toDocumentFieldValueConvertContext() {
		return mappingContext.toDocumentFieldValueConvertContext();
	}

	@Override
	public final Set<String> hibernateSearchIndexNames() {
		return hibernateSearchIndexNames;
	}

	@Override
	public final DocumentIdentifierValueConverter<?> idDslConverter(ValueConvert valueConvert) {
		if ( ValueConvert.NO.equals( valueConvert ) ) {
			return RAW_ID_CONVERTER;
		}
		DocumentIdentifierValueConverter<?> converter = null;
		for ( M model : indexModels ) {
			DocumentIdentifierValueConverter<?> converterForIndex = model.idDslConverter();
			if ( converter == null ) {
				converter = converterForIndex;
			}
			else if ( !converter.isCompatibleWith( converterForIndex ) ) {
				throw log.inconsistentConfigurationForIdentifierForSearch( converter, converterForIndex,
						indexesEventContext() );
			}
		}
		return converter;
	}

	private C root() {
		if ( indexModels.size() == 1 ) {
			return indexModels.iterator().next().root();
		}
		else {
			List<C> rootForEachIndex = new ArrayList<>();
			for ( M model : indexModels ) {
				rootForEachIndex.add( model.root() );
			}
			return createMultiIndexSearchRootContext( rootForEachIndex );
		}
	}

	private N field(String absoluteFieldPath) {
		N resultOrNull;
		if ( indexModels.size() == 1 ) {
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
	public final N child(SearchIndexCompositeNodeContext<?> parent, String name) {
		return field( parent.absolutePath( name ) );
	}

	@SuppressWarnings({"rawtypes", "unchecked"}) // We check types using reflection
	private N createMultiIndexFieldContext(String absoluteFieldPath) {
		List<N> fieldForEachIndex = new ArrayList<>();
		M modelOfFirstField = null;
		N firstField = null;

		for ( M model : indexModels ) {
			N fieldForCurrentIndex = model.fieldOrNull( absoluteFieldPath );
			if ( fieldForCurrentIndex == null ) {
				continue;
			}
			if ( firstField == null ) {
				modelOfFirstField = model;
				firstField = fieldForCurrentIndex;
			}
			else if ( firstField.isComposite() != fieldForCurrentIndex.isComposite() ) {
				SearchException cause = log.conflictingFieldModel();
				throw log.inconsistentConfigurationForIndexElementForSearch(
						EventContexts.fromIndexFieldAbsolutePath( absoluteFieldPath ), cause.getMessage(),
						EventContexts.fromIndexNames( modelOfFirstField.hibernateSearchName(),
								model.hibernateSearchName() ),
						cause );
			}
			fieldForEachIndex.add( fieldForCurrentIndex );
		}

		if ( firstField == null ) {
			return null;
		}

		if ( firstField.isComposite() ) {
			return createMultiIndexSearchObjectFieldContext( absoluteFieldPath, fieldForEachIndex );
		}
		else {
			return createMultiIndexSearchValueFieldContext( absoluteFieldPath, fieldForEachIndex );
		}
	}

	@Override
	public final <T> T rootQueryElement(SearchQueryElementTypeKey<T> key) {
		return root().queryElement( key, self() );
	}

	@Override
	public final <T> T fieldQueryElement(String absoluteFieldPath, SearchQueryElementTypeKey<T> key) {
		return field( absoluteFieldPath ).queryElement( key, self() );
	}

	protected abstract C createMultiIndexSearchRootContext(List<C> rootForEachIndex);

	protected abstract N createMultiIndexSearchValueFieldContext(String absolutePath, List<N> fieldForEachIndex);

	protected abstract N createMultiIndexSearchObjectFieldContext(String absolutePath, List<N> fieldForEachIndex);

}