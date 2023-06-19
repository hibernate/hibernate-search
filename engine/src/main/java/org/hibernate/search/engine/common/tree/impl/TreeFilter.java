/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.common.tree.impl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
	 * The idea is to find the longest cycle so far possible.
	 * Then we'll check if that cycle is broken by some parent exclude. If not,
	 * then we are giving it a last chance and are checking if any node in the cycle itself can break the cycle permutation at some point.
	 *
	 * If we found something that breaks the cycle we are going to return `null`.
	 *
	 * If nothing breaks the cycle -- we are going to return the "shortest" cycle found to be reported to the user.
	 */
	private String findCycle(MappingElement mappingElement, String relativePrefix, TreeFilterDefinition definitionToBeAdded) {
		// we will use these lists for step 2, see below:
		List<String> paths = new ArrayList<>();
		List<TreeFilter> nodes = new ArrayList<>();

		String shortestCycle = null;
		String longestCycle = null;
		TreeFilter longestCycleNode = null;
		TreeFilter localParent = this;
		StringBuilder path = new StringBuilder();
		while ( localParent != null && localParent.definition != null ) {

			path.insert( 0, localParent.relativePrefix );
			paths.add( 0, path.toString() );
			nodes.add( 0, localParent );

			if ( localParent.isSame( mappingElement, relativePrefix, definitionToBeAdded ) ) {
				longestCycle = path.toString();
				longestCycleNode = localParent;
				if ( shortestCycle == null ) {
					// set the first found cycle and keep it so till the end of the loop
					shortestCycle = path.toString();
				}
			}

			localParent = localParent.parent;
		}

		if ( longestCycle == null ) {
			return null;
		}

		// We found the longest cycle now let's see if it is included in any path:
		// 1. First let's check if any node before the one that caused a cycle is going to break that cycle.
		// To check the node where we've encountered the longest cycle we will first check the node itself, and then
		// go to its parents till the root prepending the corresponding relative node path.
		if ( isPotentiallyExcludedPath(
				// when we check the node we don't need the node's relative path at the beginning of the path we are checking:
				longestCycle.substring( longestCycleNode.relativePrefix.length() ),
				longestCycleNode
		) ) {
			return null;
		}

		// 2. maybe there's a node following the one causing the cycle that would break it at some point.
		// If so we want to go one node at a time, and only check the "new nodes" i.e. if we encounter a same node for the second time
		// it means even if that node at the current step may break the cycle - we've already checked it before and it didn't
		// in the previous steps, meaning that if that node breaks a cycle, it wouldn't be the one we are currently in:
		Set<TreeFilter> encounteredNodesSoFar = new HashSet<>();
		// we don't want to start from the beginning but from the next node from the one at which we've found the longest cycle:
		for ( int index = paths.indexOf( longestCycle ); index < nodes.size(); index++ ) {
			TreeFilter node = nodes.get( index );
			// so we know that we are in the cycle, and we know what the cycle looks like, if the current node can break a cycle
			// it would mean that if we make a cycle permutation that starts with the "current node ^" and this same node can
			// potentially break the cycle -- then we are ok to break it:

			if ( encounteredNodesSoFar.add( node )
					&& node.pathFilter.isPotentiallyExcluded(
					removeDot( pathPermutation( longestCycle, paths.get( index ), node.relativePrefix ) )
			) ) {
				return null;
			}
		}

		// 3. we haven't found an exclude that would break the cycle - so we return the shortest cycle
		return shortestCycle;
	}

	private boolean isSame(MappingElement mappingElement, String relativePrefix, TreeFilterDefinition definition) {
		return this.mappingElement.equals( mappingElement )
				// Technically we shouldn't have to check these,
				// but we'll check just to be safe, and to avoid regressions.
				// We can consider dropping these checks in the next major,
				// where behavior changes are more acceptable.
				&& this.relativePrefix.equals( relativePrefix )
				&& this.definition.equals( definition );
	}

	private String pathPermutation(String cycle, String path, String currentNodeRelativePath) {
		// `a.b.c. + a.` -- cycle
		// let's assume we are at `b.`
		// first we want to get `b.c.a.`
		// then we want to drop the current node (`b.`) from the beginning of the permuted path, since our filter wouldn't start with the node itself,
		// but with the following node:
		return ( cycle.substring( path.length() ) + cycle.substring( 0, path.length() ) ) // <-- making a permutation
				.substring( currentNodeRelativePath.length() ); // <-- dropping the current node
	}

	private String removeDot(String string) {
		return string != null && string.endsWith( "." ) ? string.substring( 0, string.length() - 1 ) : string;
	}

	private boolean isPotentiallyExcludedPath(String path, TreeFilter node) {
		return node.definition != null && (
				node.pathFilter.isPotentiallyExcluded( removeDot( path ) )
						|| ( node.parent != null && isPotentiallyExcludedPath(
						node.relativePrefix + path, node.parent
				) ) );
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
		return depthFilter.hasDepthLimit() || pathFilter.isAnyPathExplicitlyIncluded()
				|| parent != null && parent.hasCompositionLimits();
	}
}
