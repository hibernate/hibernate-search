/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.mapper.mapping.building.impl;

import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.hibernate.search.engine.backend.document.model.dsl.spi.IndexSchemaNestingContext;
import org.hibernate.search.engine.mapper.model.spi.MappableTypeModel;

/**
 * @author Yoann Rodiere
 */
class IndexSchemaNestingContextImpl implements IndexSchemaNestingContext {

	private static final IndexSchemaNestingContextImpl ROOT =
			new IndexSchemaNestingContextImpl( IndexSchemaFilter.root(), "", "" );

	public static IndexSchemaNestingContextImpl root() {
		return ROOT;
	}

	private final IndexSchemaFilter filter;
	private final String prefixFromFilter;
	private final String unconsumedPrefix;

	private IndexSchemaNestingContextImpl(IndexSchemaFilter filter, String prefixFromFilter,
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

	public <T> Optional<T> addIndexedEmbeddedIfIncluded(
			MappableTypeModel parentTypeModel, String relativePrefix,
			Integer nestedMaxDepth, Set<String> nestedPathFilters,
			NestedContextBuilder<T> contextBuilder) {
		IndexSchemaFilter composedFilter = filter.composeWithNested(
				parentTypeModel, relativePrefix, nestedMaxDepth, nestedPathFilters
		);
		if ( !composedFilter.isEveryPathExcluded() ) {
			String prefixToParse = unconsumedPrefix + relativePrefix;
			int afterPreviousDotIndex = 0;
			int nextDotIndex = prefixToParse.indexOf( '.', afterPreviousDotIndex );
			while ( nextDotIndex >= 0 ) {
				String objectName = prefixToParse.substring( afterPreviousDotIndex, nextDotIndex );
				contextBuilder.appendObject( objectName );
				afterPreviousDotIndex = nextDotIndex + 1;
				nextDotIndex = prefixToParse.indexOf( '.', afterPreviousDotIndex );
			}
			String unconsumedPrefix = prefixToParse.substring( afterPreviousDotIndex );

			IndexSchemaNestingContextImpl nestedContext =
					new IndexSchemaNestingContextImpl( composedFilter, "", unconsumedPrefix );
			return Optional.of( contextBuilder.build( nestedContext ) );
		}
		else {
			return Optional.empty();
		}
	}

	public interface NestedContextBuilder<T> {

		void appendObject(String objectName);

		T build(IndexSchemaNestingContextImpl nestingContext);

	}
}