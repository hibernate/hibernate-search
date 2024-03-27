/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.common.tree.impl;

import java.util.function.BiFunction;

import org.hibernate.search.engine.common.tree.TreeFilterDefinition;
import org.hibernate.search.engine.common.tree.spi.TreeFilterPathTracker;
import org.hibernate.search.engine.mapper.model.spi.MappingElement;
import org.hibernate.search.util.common.SearchException;

/**
 * A tree filter, responsible for deciding which parts of a (potentially cyclic) graph will be retained as a tree,
 * e.g. in an index schema.
 * <p>
 * A tree filter that accepts everything is created at the root of tree-like mapping structures (e.g. a Pojo mapping).
 * Then, each time specific nesting features (e.g. index embedding with {@code @IndexedEmbedded}) are used,
 * another filter is created with the constraints defined through that feature
 * (e.g. {@code @IndexedEmbedded(includePaths = ...)}).
 * <p>
 * <h3 id="filter-usage">Filter usage in index schemas</h3>
 * <p>
 * A tree filter is asked to provide advice about whether to trim down the index schema in two cases:
 * <ul>
 *     <li>When a field is added by a bridge, the filter decides whether to include this field or not
 *     through its {@link #isPathIncluded(String)} method</li>
 *     <li>When a nested filter (e.g. {@code @IndexedEmbedded}) is requested, a new filter is created through the
 *     {@link #compose(MappingElement, String, TreeFilterDefinition, TreeFilterPathTracker, BiFunction)}  method,
 *     which may return an empty optional,
 *     meaning that the nested filter {@link #isEveryPathExcluded() excludes every path}.</li>
 * </ul>
 *
 * <h3 id="filter-properties">Filter properties</h3>
 * <p>
 * A tree filter decides whether to include a path or not according to its two main properties:
 * <ul>
 *     <li>the {@link #depthFilter depth filter}</li>
 *     <li>the {@link #pathFilter path filter}</li>
 * </ul>
 * <p>
 * The path filter, as its name suggests, define which paths
 * should be explicitly included or excluded by this filter.
 * <p>
 * The depth filter defines
 * how paths that are not explicitly included or excluded by the path filter should be treated:
 * <ul>
 *     <li>as long as the remaining depth is unlimited (null) or strictly positive, paths are included by default</li>
 *     <li>as soon as the remaining depth is zero or negative, paths are excluded by default</li>
 * </ul>
 *
 * <h3 id="filter-composition">Filter composition</h3>
 * <p>
 * Composed filters are created whenever a nested filter (e.g. {@code @IndexedEmbedded}) is encountered.
 * A composed filter will always enforce the restrictions of its parent filter,
 * plus some added restrictions depending on the properties of the nested filter.
 * <p>
 * For more information about how filters are composed, see
 * {@link #compose(MappingElement, String, TreeFilterDefinition, TreeFilterPathTracker, BiFunction)}.
 *
 */
public class TreeFilter {

	private static final TreeFilter ROOT = new TreeFilter(
			null, null, null,
			TreeFilterDefinition.includeAll(),
			null,
			DepthFilter.unconstrained(), PathFilter.unconstrained()
	);

	public static TreeFilter root() {
		return ROOT;
	}

	private final TreeFilter parent;
	private final MappingElement mappingElement;
	private final String relativePrefix;
	private final TreeFilterDefinition definition;
	private final TreeFilterPathTracker pathTracker;

	private final DepthFilter depthFilter;

	private final PathFilter pathFilter;

	private TreeFilter(TreeFilter parent,
			MappingElement mappingElement, String relativePrefix,
			TreeFilterDefinition definition, TreeFilterPathTracker pathTracker,
			DepthFilter depthFilter, PathFilter pathFilter) {
		this.parent = parent;
		this.mappingElement = mappingElement;
		this.relativePrefix = relativePrefix;
		this.definition = definition;
		this.pathTracker = pathTracker;
		this.depthFilter = depthFilter;
		this.pathFilter = pathFilter;
	}

	@Override
	public String toString() {
		return new StringBuilder( getClass().getSimpleName() )
				.append( "[" )
				.append( "depthFilter=" ).append( depthFilter )
				.append( ",pathFilter=" ).append( pathFilter )
				.append( ",parent=" ).append( parent )
				.append( "]" )
				.toString();
	}

	public boolean isPathIncluded(String relativePath) {
		return isPathIncludedInternal( 0, relativePath, true );
	}

	/**
	 * @param relativeDepth The number of filters between this filter and the end of the path.
	 * @param relativePath The path relative to this filter.
	 * @param markIncludedAsEncountered Whether the included path should be marked as encountered
	 * ({@code true}), or not ({@code false}).
	 * Useful when processing "theoretical" paths, such as the includePaths of a child filter, in particular.
	 * @return {@code true} if the path is included, {@code false} otherwise.
	 */
	private boolean isPathIncludedInternal(int relativeDepth, String relativePath,
			boolean markIncludedAsEncountered) {
		boolean includedByThis = ( depthFilter.isEveryPathIncludedAtDepth( relativeDepth )
				|| pathFilter.isExplicitlyIncluded( relativePath ) )
				&& !pathFilter.isExplicitlyExcluded( relativePath );

		boolean includedByParent = true;
		/*
		 * The parent can filter out paths that are considered as included by a child,
		 * by reducing the includeDepth in particular,
		 * but it cannot include paths that are filtered out by a child.
		 */
		if ( includedByThis && parent != null ) {
			includedByParent = parent.isPathIncludedInternal(
					relativeDepth + 1,
					relativePrefix + relativePath,
					markIncludedAsEncountered
			);
		}

		boolean included = includedByParent && includedByThis;

		// when we are checking if any path is included we want to make sure we record the paths we've checked, but they are excluded:
		// - if we end up excluding the entire object because *none* of the "includePaths" are actually included
		// we want to record the path so that the exclude filter will know it wasn't useless,
		// - if we end up including the object because some of the "includePaths" are actually included we'll mark those
		// paths later when we'll be considering adding them.
		if ( ( markIncludedAsEncountered || !included ) && pathTracker != null ) {
			pathTracker.markAsEncountered( relativePath, includedByThis );
		}

		return included;
	}

	public boolean isEveryPathExcluded() {
		return !isEveryPathIncludedAtDepth( 0 )
				&& !isAnyPathExplicitlyIncluded( "", this );
	}

	private boolean isAnyPathExplicitlyIncluded(String prefixToRemove, TreeFilter filter) {
		if ( definition == null ) {
			// Root: return early to avoid annoying null checks
			return false;
		}

		int prefixLength = prefixToRemove.length();
		/*
		 * First check the explicitly included paths from this.
		 * They may be excluded by the given filter (which is either this or a descendant).
		 */
		for ( String path : definition.includePaths() ) {
			if ( !path.startsWith( prefixToRemove ) ) {
				continue;
			}
			String pathWithoutPrefix = path.substring( prefixLength );
			if ( filter.isPathIncludedInternal( 0, pathWithoutPrefix, false ) ) {
				return true;
			}
		}
		/*
		 * Then the explicitly included paths from ancestors.
		 * They may be excluded by the given filter (which is either this or a descendant),
		 * but they may also be included.
		 * This can happen for example
		 * if @IndexedEmbedded(includeDepth = 0, includePaths = "embedded.foo")
		 * embeds @IndexedEmbedded(includeDepth = 42):
		 * the paths explicitly included by the parent will be included
		 * even though they are not explicitly included by the child
		 */
		return parent != null
				&& parent.isAnyPathExplicitlyIncluded( this.relativePrefix + prefixToRemove, filter );
	}

	private boolean isEveryPathIncludedAtDepth(int depth) {
		return depthFilter.isEveryPathIncludedAtDepth( depth )
				// The parent can reduce the max depth of a child.
				&& ( parent == null || parent.isEveryPathIncludedAtDepth( depth + 1 ) );
	}

	private String getPathFromSameFilterSinceNoCompositionLimits(MappingElement mappingElement,
			String relativePrefix, TreeFilterDefinition definition) {
		if ( hasCompositionLimits() ) {
			return null;
		}
		if ( this.definition == null ) {
			// we are at root
			return null;
		}

		return findCycle( mappingElement, relativePrefix, definition );
	}

	/*
	 * The idea is to find the farthest cycle, i.e. the cycle closest to the root.
	 * There is a chance that the cycle may repeat a few times if there are exclude paths that may break the cycle at some point.
	 * Let's assume we have a cycle where we are trying to add next `a.` relative path with the same mapping element and filter definition
	 * as the already added ones before:
	 * a.b.c.a.b.c.a.b.c.a.b.c. + a.
	 * 1   ^ 2     3     4        5
	 *
	 * - we'll be looking for a relative path `a.` with index `1` and that would define our "farthest cycle".
	 * - we want to know the filter representing the relative path `c.` (marked with `^`) that is right before the `a.` with index `2` as well as the
	 * path (and prefix) leading to it from `5` -- "a.b.c.a.b.c.a.b.c."
	 * that will be the filter and the path we'll pass to the "isPotentiallyExcludedPathAndPrefix" check.
	 * These will be our corresponding `previousCycleFilter` and `pathAndPrefixFromPreviousCycleFilter`
	 *
	 * Doing so will go through any potential exclude filters defined on indexed-embedded only once; going from that relative path `c.` (^)
	 * and through its parents will cover the filters from the cycle itself as well as any filters out of the cycle that are in the path leading to the cycle.
	 *
	 * In case we don't have a repeating cycle yet - we'll be starting from the "current filter"
	 * ( previousCycleFilter == this, pathAndPrefixFromPreviousCycleFilter == this.relativePrefix )
	 */
	private String findCycle(MappingElement mappingElement, String relativePrefix, TreeFilterDefinition definitionToBeAdded) {
		String closestCyclePathAndPrefix = null;
		String pathAndPrefixFromCurrentCycleFilter = null;
		String pathAndPrefixFromPreviousCycleFilter = relativePrefix;
		TreeFilter currentFilter = this;
		TreeFilter currentCycleFilter = null;
		TreeFilter previousCycleFilter = this;
		StringBuilder path = new StringBuilder();
		while ( currentFilter != null && currentFilter.definition != null ) {

			path.insert( 0, currentFilter.relativePrefix );

			if ( currentFilter.isSame( mappingElement, relativePrefix, definitionToBeAdded ) ) {
				if ( currentCycleFilter != null ) {
					previousCycleFilter = currentCycleFilter.parent;
					pathAndPrefixFromPreviousCycleFilter = pathAndPrefixFromCurrentCycleFilter;
				}

				pathAndPrefixFromCurrentCycleFilter = path.toString();
				currentCycleFilter = currentFilter;

				if ( closestCyclePathAndPrefix == null ) {
					// Set the first found cycle and keep it unchanged till the end of the loop. We'll report it to the user.
					closestCyclePathAndPrefix = pathAndPrefixFromCurrentCycleFilter;
				}
			}

			currentFilter = currentFilter.parent;
		}

		if ( pathAndPrefixFromCurrentCycleFilter == null ) {
			return null;
		}

		// We have a cycle and the path doesn't contain dots, i.e. we are building a cycle from prefixes without dots.
		// Which means that this cycle will never be terminated by an exclude path. So it is "safe to just fail" at this point
		if ( !closestCyclePathAndPrefix.contains( "." ) ) {
			return closestCyclePathAndPrefix;
		}

		// If we reached this point -- we've found a cycle that might be broken by an exclude path,
		// and we need to check if any exclude paths will do so:
		if ( previousCycleFilter.isPotentiallyExcludedPathAndPrefix( pathAndPrefixFromPreviousCycleFilter ) ) {
			return null;
		}

		// We haven't found an exclude that would break the cycle - so we return the closest cycle
		return closestCyclePathAndPrefix;
	}

	private boolean isPotentiallyExcludedPathAndPrefix(String pathAndPrefix) {
		if ( parent == null ) {
			// The root node doesn't exclude any path.
			return false;
		}

		// Only "pure" paths can be excluded, paths with trailing prefixes cannot.
		// See @IndexedEmbedded.excludePaths: those are expected to be "pure" paths, without any trailing prefix.
		int lastDotIndex = pathAndPrefix.lastIndexOf( '.' );
		if ( lastDotIndex > 0 ) {
			// "pathAndPrefix" starts with an actual path, which could possibly be excluded.
			String pathWithoutTrailingPrefix = pathAndPrefix.substring( 0, lastDotIndex );
			if ( pathFilter.isPotentiallyExcluded( pathWithoutTrailingPrefix ) ) {
				return true;
			}
		}
		// else: "pathAndPrefix" is really just a prefix without a path, it cannot possibly be excluded by this filter.
		// However, the prefix of a parent filter may still start with an actual path which may be excluded,
		// so we'll proceed.

		// If this filter cannot exclude the given "path and prefix", maybe a parent can.
		// We're not the root, so there's necessarily a parent.
		return parent.isPotentiallyExcludedPathAndPrefix( relativePrefix + pathAndPrefix );
	}

	private boolean isSame(MappingElement mappingElement, String relativePrefix, TreeFilterDefinition definition) {
		return this.mappingElement != null // no need to check the root filter any further
				&& this.mappingElement.equals( mappingElement )
				// Technically we shouldn't have to check these,
				// but we'll check just to be safe, and to avoid regressions.
				// We can consider dropping these checks in the next major,
				// where behavior changes are more acceptable.
				&& this.relativePrefix.equals( relativePrefix )
				&& this.definition.equals( definition );
	}

	public TreeFilter compose(MappingElement mappingElement, String relativePrefix,
			TreeFilterDefinition definition, TreeFilterPathTracker pathTracker,
			BiFunction<MappingElement, String, SearchException> cyclicRecursionExceptionFactory) {
		String cyclicRecursionPath = getPathFromSameFilterSinceNoCompositionLimits(
				mappingElement, relativePrefix, definition );
		if ( cyclicRecursionPath != null ) {
			cyclicRecursionPath += relativePrefix;
			throw cyclicRecursionExceptionFactory.apply( mappingElement, cyclicRecursionPath );
		}

		// The new depth filter according to the given includeDepth
		DepthFilter newDepthFilter = DepthFilter.of( definition.includeDepth() );

		// The new path filter according to the given includedPaths
		PathFilter newPathFilter = PathFilter.of( definition.includePaths(), definition.excludePaths() );

		return new TreeFilter(
				this, mappingElement, relativePrefix, definition, pathTracker,
				newDepthFilter, newPathFilter
		);
	}

	private boolean hasCompositionLimits() {
		return depthFilter.hasDepthLimit()
				|| pathFilter.isAnyPathExplicitlyIncluded()
				|| parent != null && parent.hasCompositionLimits();
	}
}
