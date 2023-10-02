/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.backend.scope.spi;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.hibernate.search.engine.backend.document.model.spi.AbstractIndexModel;
import org.hibernate.search.engine.backend.mapping.spi.BackendMappingContext;
import org.hibernate.search.engine.backend.types.converter.runtime.ToDocumentValueConvertContext;
import org.hibernate.search.engine.logging.impl.Log;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.engine.search.common.spi.MultiIndexSearchIndexIdentifierContext;
import org.hibernate.search.engine.search.common.spi.SearchIndexCompositeNodeContext;
import org.hibernate.search.engine.search.common.spi.SearchIndexIdentifierContext;
import org.hibernate.search.engine.search.common.spi.SearchIndexNodeContext;
import org.hibernate.search.engine.search.common.spi.SearchIndexScope;
import org.hibernate.search.engine.search.common.spi.SearchQueryElementTypeKey;
import org.hibernate.search.engine.search.projection.definition.spi.ProjectionRegistry;
import org.hibernate.search.engine.search.projection.spi.ProjectionMappedTypeContext;
import org.hibernate.search.engine.search.query.spi.SearchQueryIndexScope;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.impl.Contracts;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.reporting.EventContext;

public abstract class AbstractSearchIndexScope<
		S extends SearchQueryIndexScope<?>,
		M extends AbstractIndexModel<?, ? extends C, ? extends N>,
		N extends SearchIndexNodeContext<? super S>,
		C extends SearchIndexCompositeNodeContext<? super S>>
		implements SearchIndexScope<S>, SearchQueryIndexScope<S> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	// Mapping context
	protected final BackendMappingContext mappingContext;

	// Targeted indexes
	private final Set<String> hibernateSearchIndexNames;
	private final Set<? extends M> indexModels;
	private final List<ProjectionMappedTypeContext> mappedTypeContexts;

	// withRoot(...)
	private final C overriddenRoot;


	public AbstractSearchIndexScope(BackendMappingContext mappingContext, Set<? extends M> indexModels) {
		this.mappingContext = mappingContext;

		// Use LinkedHashMap/LinkedHashSet to ensure stable order when generating requests
		this.hibernateSearchIndexNames = new TreeSet<>();
		this.mappedTypeContexts = new ArrayList<>();
		for ( M model : indexModels ) {
			hibernateSearchIndexNames.add( model.hibernateSearchName() );
			mappedTypeContexts.add( mappingContext.mappedTypeContext( model.mappedTypeName() ) );
		}
		this.indexModels = indexModels;

		this.overriddenRoot = null;
	}

	protected AbstractSearchIndexScope(AbstractSearchIndexScope<S, M, N, C> parentScope, C overriddenRoot) {
		this.mappingContext = parentScope.mappingContext;
		this.hibernateSearchIndexNames = parentScope.hibernateSearchIndexNames;
		this.indexModels = parentScope.indexModels;
		this.mappedTypeContexts = parentScope.mappedTypeContexts;
		this.overriddenRoot = overriddenRoot;
	}

	@Override
	public BackendMappingContext mappingContext() {
		return mappingContext;
	}

	@Override
	public EventContext eventContext() {
		return EventContexts.fromIndexNames( hibernateSearchIndexNames );
	}

	protected final EventContext indexesAndRootEventContext() {
		if ( overriddenRoot == null ) {
			return eventContext();
		}
		else {
			return eventContext().append( overriddenRoot.relativeEventContext() );
		}
	}

	protected abstract S self();

	@Override
	public final ToDocumentValueConvertContext toDocumentValueConvertContext() {
		return mappingContext.toDocumentValueConvertContext();
	}

	@Override
	public final Set<String> hibernateSearchIndexNames() {
		return hibernateSearchIndexNames;
	}

	@Override
	public String toAbsolutePath(String relativeFieldPath) {
		Contracts.assertNotNull( relativeFieldPath, "relativeFieldPath" );
		return overriddenRoot == null ? relativeFieldPath : overriddenRoot.absolutePath( relativeFieldPath );
	}

	@Override
	public SearchIndexIdentifierContext identifier() {
		if ( indexModels.size() == 1 ) {
			return indexModels.iterator().next().identifier();
		}
		else {
			List<SearchIndexIdentifierContext> identifierForEachIndex = new ArrayList<>();
			for ( M model : indexModels ) {
				identifierForEachIndex.add( model.identifier() );
			}
			return new MultiIndexSearchIndexIdentifierContext( this, identifierForEachIndex );
		}
	}

	protected C root() {
		if ( overriddenRoot != null ) {
			return overriddenRoot;
		}
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

	protected N field(String fieldPath) {
		if ( overriddenRoot != null ) {
			return fieldInternal( overriddenRoot.absolutePath( fieldPath ) );
		}
		return fieldInternal( fieldPath );
	}

	private N fieldInternal(String absoluteFieldPath) {
		N resultOrNull;
		if ( indexModels.size() == 1 ) {
			resultOrNull = indexModels.iterator().next().fieldOrNull( absoluteFieldPath );
		}
		else {
			resultOrNull = createMultiIndexFieldContext( absoluteFieldPath );
		}
		if ( resultOrNull == null ) {
			throw log.unknownFieldForSearch( absoluteFieldPath, indexesAndRootEventContext() );
		}
		return resultOrNull;
	}

	@Override
	public final N child(SearchIndexCompositeNodeContext<?> parent, String name) {
		return fieldInternal( parent.absolutePath( name ) );
	}

	@SuppressWarnings("unchecked")
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
				throw log.inconsistentConfigurationInContextForSearch(
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
	public final <T> T fieldQueryElement(String fieldPath, SearchQueryElementTypeKey<T> key) {
		return field( fieldPath ).queryElement( key, self() );
	}

	@Override
	public ProjectionRegistry projectionRegistry() {
		return mappingContext.projectionRegistry();
	}

	@Override
	public List<? extends ProjectionMappedTypeContext> mappedTypeContexts() {
		return mappedTypeContexts;
	}

	protected abstract C createMultiIndexSearchRootContext(List<C> rootForEachIndex);

	protected abstract N createMultiIndexSearchValueFieldContext(String absolutePath, List<N> fieldForEachIndex);

	protected abstract N createMultiIndexSearchObjectFieldContext(String absolutePath, List<N> fieldForEachIndex);

}
