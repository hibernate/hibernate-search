/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.mapper.mapping.building.impl;

import java.util.Optional;

import org.hibernate.search.engine.backend.document.model.dsl.impl.IndexSchemaNestingContext;
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
	public <T> T nest(String relativeName, LeafFactory<T> factoryIfIncluded, LeafFactory<T> factoryIfExcluded) {
		String nameRelativeToFilter = prefixFromFilter + relativeName;
		String prefixedRelativeName = unconsumedPrefix + relativeName;
		if ( filter.isPathIncluded( nameRelativeToFilter ) ) {
			return factoryIfIncluded.create( prefixedRelativeName );
		}
		else {
			return factoryIfExcluded.create( prefixedRelativeName );
		}
	}

	@Override
	public <T> T nest(String relativeName, CompositeFactory<T> factoryIfIncluded,
			CompositeFactory<T> factoryIfExcluded) {
		String nameRelativeToFilter = prefixFromFilter + relativeName;
		String prefixedRelativeName = unconsumedPrefix + relativeName;
		if ( filter.isPathIncluded( nameRelativeToFilter ) ) {
			ConfiguredIndexSchemaNestingContext nestedFilter =
					new ConfiguredIndexSchemaNestingContext( filter, nameRelativeToFilter + ".", "" );
			return factoryIfIncluded.create( prefixedRelativeName, nestedFilter );
		}
		else {
			return factoryIfExcluded.create( prefixedRelativeName, IndexSchemaNestingContext.excludeAll() );
		}
	}

	public <T> Optional<T> addIndexedEmbeddedIfIncluded(
			IndexedEmbeddedDefinition definition,
			IndexedEmbeddedPathTracker pathTracker,
			NestedContextBuilder<T> contextBuilder) {
		IndexSchemaFilter composedFilter = filter.compose( definition, pathTracker );
		if ( !composedFilter.isEveryPathExcluded() ) {
			String prefixToParse = unconsumedPrefix + definition.getRelativePrefix();
			int afterPreviousDotIndex = 0;
			int nextDotIndex = prefixToParse.indexOf( '.', afterPreviousDotIndex );
			while ( nextDotIndex >= 0 ) {
				String objectName = prefixToParse.substring( afterPreviousDotIndex, nextDotIndex );
				contextBuilder.appendObject( objectName );

				// Make sure to mark the paths as encountered in the filter
				String objectNameRelativeToFilter = prefixToParse.substring( 0, nextDotIndex );
				// We only use isPathIncluded for its side effect: it marks the path as encountered
				filter.isPathIncluded( objectNameRelativeToFilter );

				afterPreviousDotIndex = nextDotIndex + 1;
				nextDotIndex = prefixToParse.indexOf( '.', afterPreviousDotIndex );
			}
			String unconsumedPrefix = prefixToParse.substring( afterPreviousDotIndex );

			ConfiguredIndexSchemaNestingContext nestedContext =
					new ConfiguredIndexSchemaNestingContext( composedFilter, "", unconsumedPrefix );
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