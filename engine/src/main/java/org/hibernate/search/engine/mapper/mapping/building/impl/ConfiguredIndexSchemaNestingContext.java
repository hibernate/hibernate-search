/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.mapper.mapping.building.impl;

import java.util.Optional;

import org.hibernate.search.engine.backend.document.model.dsl.impl.IndexSchemaNestingContext;
import org.hibernate.search.engine.backend.document.model.spi.IndexFieldInclusion;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexedEmbeddedDefinition;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexedEmbeddedPathTracker;


class ConfiguredIndexSchemaNestingContext implements IndexSchemaNestingContext {

	private static final ConfiguredIndexSchemaNestingContext ROOT =
			new ConfiguredIndexSchemaNestingContext( IndexSchemaFilter.root(), "", "" );

	public static ConfiguredIndexSchemaNestingContext root() {
		return ROOT;
	}

	private final IndexSchemaFilter filter;
	private final String prefixFromFilter;
	private final String unconsumedPrefix;

	private ConfiguredIndexSchemaNestingContext(IndexSchemaFilter filter, String prefixFromFilter,
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
	public <T> T nest(String relativeName, LeafFactory<T> factory) {
		String nameRelativeToFilter = prefixFromFilter + relativeName;
		String prefixedRelativeName = unconsumedPrefix + relativeName;
		boolean included = filter.isPathIncluded( nameRelativeToFilter );
		return factory.create( prefixedRelativeName,
				included ? IndexFieldInclusion.INCLUDED : IndexFieldInclusion.EXCLUDED );
	}

	@Override
	public <T> T nest(String relativeName, CompositeFactory<T> factory) {
		String nameRelativeToFilter = prefixFromFilter + relativeName;
		String prefixedRelativeName = unconsumedPrefix + relativeName;
		boolean included = filter.isPathIncluded( nameRelativeToFilter );
		if ( included ) {
			ConfiguredIndexSchemaNestingContext nestedFilter =
					new ConfiguredIndexSchemaNestingContext( filter, nameRelativeToFilter + ".", "" );
			return factory.create( prefixedRelativeName, IndexFieldInclusion.INCLUDED, nestedFilter );
		}
		else {
			return factory.create( prefixedRelativeName, IndexFieldInclusion.EXCLUDED,
					IndexSchemaNestingContext.excludeAll() );
		}
	}

	@Override
	public <T> T nestUnfiltered(UnfilteredFactory<T> factory) {
		return factory.create( IndexFieldInclusion.INCLUDED, unconsumedPrefix );
	}

	public <T> Optional<T> addIndexedEmbeddedIfIncluded(
			IndexedEmbeddedDefinition definition,
			IndexedEmbeddedPathTracker pathTracker,
			NestedContextBuilder<T> contextBuilder) {
		IndexSchemaFilter composedFilter = filter.compose( definition, pathTracker );

		if ( !composedFilter.isEveryPathExcluded() ) {
			String prefixToParse = unconsumedPrefix + definition.relativePrefix();
			int afterPreviousDotIndex = 0;
			int nextDotIndex = prefixToParse.indexOf( '.', afterPreviousDotIndex );
			while ( nextDotIndex >= 0 ) {
				// Make sure to mark the paths as encountered in the filter
				String objectNameRelativeToFilter = prefixToParse.substring( unconsumedPrefix.length(), nextDotIndex );

				// we don't want to proceed if a subpath is already excluded:
				if ( !filter.isPathIncluded( objectNameRelativeToFilter ) ) {
					return Optional.empty();
				}

				String objectName = prefixToParse.substring( afterPreviousDotIndex, nextDotIndex );
				contextBuilder.appendObject( objectName );

				afterPreviousDotIndex = nextDotIndex + 1;
				nextDotIndex = prefixToParse.indexOf( '.', afterPreviousDotIndex );
			}
			String composedUnconsumedPrefix = prefixToParse.substring( afterPreviousDotIndex );

			ConfiguredIndexSchemaNestingContext nestedContext =
					new ConfiguredIndexSchemaNestingContext( composedFilter, "", composedUnconsumedPrefix );
			return Optional.of( contextBuilder.build( nestedContext ) );
		}
		else {
			return Optional.empty();
		}
	}

	public interface NestedContextBuilder<T> {

		void appendObject(String objectName);

		T build(ConfiguredIndexSchemaNestingContext nestingContext);

	}
}
