/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.mapper.mapping.building.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.hibernate.search.engine.backend.document.IndexObjectFieldAccessor;
import org.hibernate.search.engine.backend.document.model.ObjectFieldStorage;
import org.hibernate.search.engine.backend.document.model.spi.IndexSchemaCollector;
import org.hibernate.search.engine.backend.document.model.spi.IndexSchemaNestingContext;
import org.hibernate.search.engine.backend.document.model.spi.ObjectFieldIndexSchemaCollector;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexModelBindingContext;
import org.hibernate.search.engine.mapper.model.spi.IndexableTypeOrdering;

/**
 * @author Yoann Rodiere
 */
class IndexSchemaNestingContextImpl implements IndexSchemaNestingContext {

	private final IndexedEmbeddedFilter filter;
	private final String prefixFromFilter;
	private final String unconsumedPrefix;

	public IndexSchemaNestingContextImpl(IndexableTypeOrdering typeOrdering) {
		this( new IndexedEmbeddedFilter( typeOrdering ), "", "" );
	}

	private IndexSchemaNestingContextImpl(IndexedEmbeddedFilter filter, String prefixFromFilter,
			String unconsumedPrefix) {
		this.filter = filter;
		this.prefixFromFilter = prefixFromFilter;
		this.unconsumedPrefix = unconsumedPrefix;
	}

	@Override
	public String toString() {
		return new StringBuilder( getClass().getSimpleName() )
				.append( "[" )
				.append( "filter=" ).append( filter )
				.append( ",prefixFromFilter=" ).append( prefixFromFilter )
				.append( ",unconsumedPrefix=" ).append( unconsumedPrefix )
				.append( "]" )
				.toString();
	}

	@Override
	public <T> T nest(String relativeName, Function<String, T> nestedElementFactoryIfIncluded,
			Function<String, T> nestedElementFactoryIfExcluded) {
		String nameRelativeToFilter = prefixFromFilter + relativeName;
		String prefixedRelativeName = unconsumedPrefix + relativeName;
		if ( filter.isPathIncluded( nameRelativeToFilter ) ) {
			return nestedElementFactoryIfIncluded.apply( prefixedRelativeName );
		}
		else {
			return nestedElementFactoryIfExcluded.apply( prefixedRelativeName );
		}
	}

	@Override
	public <T> T nest(String relativeName,
			BiFunction<String, IndexSchemaNestingContext, T> nestedElementFactoryIfIncluded,
			BiFunction<String, IndexSchemaNestingContext, T> nestedElementFactoryIfExcluded) {
		String nameRelativeToFilter = prefixFromFilter + relativeName;
		String prefixedRelativeName = unconsumedPrefix + relativeName;
		if ( filter.isPathIncluded( nameRelativeToFilter ) ) {
			IndexSchemaNestingContextImpl nestedFilter =
					new IndexSchemaNestingContextImpl( filter, nameRelativeToFilter + ".", "" );
			return nestedElementFactoryIfIncluded.apply( prefixedRelativeName, nestedFilter );
		}
		else {
			return nestedElementFactoryIfExcluded.apply( prefixedRelativeName, IndexSchemaNestingContext.excludeAll() );
		}
	}

	public Optional<IndexModelBindingContext> addIndexedEmbeddedIfIncluded(
			String relativePrefix, ObjectFieldStorage storage,
			Function<IndexedEmbeddedFilter, IndexedEmbeddedFilter> filterCompositionFunction,
			IndexSchemaCollector indexModelNode) {
		IndexedEmbeddedFilter composedFilter = filterCompositionFunction.apply( filter );
		if ( !composedFilter.isTerminal() ) {
			String prefixToParse = unconsumedPrefix + relativePrefix;
			IndexSchemaCollector currentNode = indexModelNode;
			int afterPreviousDotIndex = 0;
			int nextDotIndex = prefixToParse.indexOf( '.', afterPreviousDotIndex );
			List<IndexObjectFieldAccessor> parentObjectAccessors = new ArrayList<>();
			while ( nextDotIndex >= 0 ) {
				String objectName = prefixToParse.substring( afterPreviousDotIndex, nextDotIndex );
				ObjectFieldIndexSchemaCollector nextNode = currentNode.objectField( objectName, storage );
				parentObjectAccessors.add( nextNode.withContext( IndexSchemaNestingContext.includeAll() ).createAccessor() );
				afterPreviousDotIndex = nextDotIndex + 1;
				nextDotIndex = prefixToParse.indexOf( '.', afterPreviousDotIndex );
				currentNode = nextNode;
			}
			String unconsumedPrefix = prefixToParse.substring( afterPreviousDotIndex );

			IndexSchemaNestingContextImpl nestedContext =
					new IndexSchemaNestingContextImpl( composedFilter, relativePrefix, unconsumedPrefix );
			return Optional.of( new IndexModelBindingContextImpl( currentNode, parentObjectAccessors, nestedContext ) );
		}
		else {
			return Optional.empty();
		}
	}
}