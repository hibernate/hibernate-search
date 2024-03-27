/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.common.tree.impl;

import java.util.Optional;
import java.util.function.BiFunction;

import org.hibernate.search.engine.common.tree.TreeFilterDefinition;
import org.hibernate.search.engine.common.tree.spi.TreeFilterPathTracker;
import org.hibernate.search.engine.common.tree.spi.TreeNestingContext;
import org.hibernate.search.engine.common.tree.spi.TreeNodeInclusion;
import org.hibernate.search.engine.mapper.model.spi.MappingElement;
import org.hibernate.search.util.common.SearchException;

public final class ConfiguredTreeNestingContext implements TreeNestingContext {

	public static final ConfiguredTreeNestingContext ROOT =
			new ConfiguredTreeNestingContext( TreeFilter.root(), "", "" );

	private final TreeFilter filter;
	private final String prefixFromFilter;
	private final String unconsumedPrefix;

	private ConfiguredTreeNestingContext(TreeFilter filter, String prefixFromFilter,
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
				included ? TreeNodeInclusion.INCLUDED : TreeNodeInclusion.EXCLUDED );
	}

	@Override
	public <T> T nest(String relativeName, CompositeFactory<T> factory) {
		String nameRelativeToFilter = prefixFromFilter + relativeName;
		String prefixedRelativeName = unconsumedPrefix + relativeName;
		boolean included = filter.isPathIncluded( nameRelativeToFilter );
		if ( included ) {
			ConfiguredTreeNestingContext nestedFilter =
					new ConfiguredTreeNestingContext( filter, nameRelativeToFilter + ".", "" );
			return factory.create( prefixedRelativeName, TreeNodeInclusion.INCLUDED, nestedFilter );
		}
		else {
			return factory.create( prefixedRelativeName, TreeNodeInclusion.EXCLUDED,
					TreeNestingContext.excludeAll() );
		}
	}

	@Override
	public <T> T nestUnfiltered(UnfilteredFactory<T> factory) {
		return factory.create( TreeNodeInclusion.INCLUDED, unconsumedPrefix );
	}

	@Override
	public <T> Optional<T> nestComposed(MappingElement mappingElement,
			String relativePrefix, TreeFilterDefinition definition,
			TreeFilterPathTracker pathTracker, NestedContextBuilder<T> contextBuilder,
			BiFunction<MappingElement, String, SearchException> cyclicRecursionExceptionFactory) {
		TreeFilter composedFilter = filter.compose( mappingElement, relativePrefix, definition, pathTracker,
				cyclicRecursionExceptionFactory );

		if ( !composedFilter.isEveryPathExcluded() ) {
			String prefixToParse = unconsumedPrefix + relativePrefix;
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

			ConfiguredTreeNestingContext nestedContext =
					new ConfiguredTreeNestingContext( composedFilter, "", composedUnconsumedPrefix );
			return Optional.of( contextBuilder.build( nestedContext ) );
		}
		else {
			return Optional.empty();
		}
	}
}
