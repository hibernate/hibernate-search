/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.mapper.mapping.building.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.engine.logging.impl.Log;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexedEmbeddedDefinition;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexedEmbeddedPathTracker;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

/**
 * A schema filter, responsible for deciding which parts of a mapping will actually make it to the index schema.
 * <p>
 * A schema filter is created at the root of a Pojo mapping (it accepts everything),
 * and also each time index embedding ({@code @IndexedEmbedded}) is used.
 *
 * <h3 id="filter-usage">Filter usage</h3>
 * <p>
 * A schema filter is asked to provide advice about whether or not to trim down the schema in two cases:
 * <ul>
 *     <li>When a field is added by a bridge, the filter decides whether to include this field or not
 *     through its {@link #isPathIncluded(String)} method</li>
 *     <li>When a nested {@code @IndexedEmbedded} is requested, a new filter is created through the
 *     {@link #compose(IndexedEmbeddedDefinition, IndexedEmbeddedPathTracker)} method, which may return a filter
 *     that {@link #isEveryPathExcluded() excludes every path}, meaning the {@code @IndexedEmbedded} will
 *     be ignored</li>
 * </ul>
 *
 * <h3 id="filter-properties">Filter properties</h3>
 * <p>
 * A filter decides whether to include a path or not according to its two main properties:
 * <ul>
 *     <li>the {@link #depthFilter depth filter}</li>
 *     <li>the {@link #pathFilter path filter}</li>
 * </ul>
 * <p>
 * The path filter, as its name suggests, define which paths
 * should be accepted by this filter.
 * <p>
 * The depth filter defines
 * how paths that are not included by the path filter should be treated:
 * <ul>
 *     <li>as long as the remaining depth is unlimited (null) or strictly positive, paths are included by default</li>
 *     <li>as soon as the remaining depth is zero or negative, paths are excluded by default</li>
 * </ul>
 *
 * <h3 id="filter-composition">Filter composition</h3>
 * <p>
 * Composed filters are created whenever a nested {@code @IndexedEmbedded} is encountered.
 * A composed filter will always enforce the restrictions of its parent filter,
 * plus some added restrictions depending on the properties of the nested {@code IndexedEmbedded}.
 * <p>
 * For more information about how filters are composed, see
 * {@link #compose(IndexedEmbeddedDefinition, IndexedEmbeddedPathTracker)}.
 *
 */
class IndexSchemaFilter {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private static final IndexSchemaFilter ROOT = new IndexSchemaFilter(
			null, null, null,
			DepthFilter.unconstrained(), PathFilter.unconstrained()
	);

	public static IndexSchemaFilter root() {
		return ROOT;
	}

	private final IndexSchemaFilter parent;
	private final IndexedEmbeddedDefinition definition;
	private final IndexedEmbeddedPathTracker pathTracker;

	private final DepthFilter depthFilter;

	private final PathFilter pathFilter;

	private IndexSchemaFilter(IndexSchemaFilter parent,
			IndexedEmbeddedDefinition definition, IndexedEmbeddedPathTracker pathTracker,
			DepthFilter depthFilter, PathFilter pathFilter) {
		this.parent = parent;
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
		return isPathIncludedInternal( 0, relativePath, true, true );
	}

	/**
	 * @param relativeDepth The number of indexed-embeddeds between this filter and the end of the path.
	 * @param relativePath The path relative to this filter.
	 * @param markAsEncountered Whether the path should be marked as encountered
	 * ({@code true}), or not ({@code false}).
	 * Useful when processing "theoretical" paths, such as the includePaths of a child filter, in particular.
	 * @param includedByChild Whether a child filter included this path.
	 * Useful when marking a path as encountered: if it is included by this filter,
	 * but not by any children, the corresponding includePaths is not effective,
	 * and thus we should consider the path as excluded for the purpose of detecting ineffective includePaths.
	 * @return {@code true} if the path is included, {@code false} otherwise.
	 */
	private boolean isPathIncludedInternal(int relativeDepth, String relativePath,
			boolean markAsEncountered, boolean includedByChild) {
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
					definition.relativePrefix() + relativePath,
					markAsEncountered, includedByThis
			);
		}

		boolean included = includedByParent && includedByThis;

		if ( markAsEncountered && pathTracker != null ) {
			pathTracker.markAsEncountered(
					relativePath, includedByThis && includedByChild
			);
		}

		return included;
	}

	boolean isEveryPathExcluded() {
		return !isEveryPathIncludedAtDepth( 0 )
				&& !isAnyPathExplicitlyIncluded( "", this );
	}

	private boolean isAnyPathExplicitlyIncluded(String prefixToRemove, IndexSchemaFilter filter) {
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
			if ( filter.isPathIncludedInternal( 0, pathWithoutPrefix, false, true ) ) {
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
				&& parent.isAnyPathExplicitlyIncluded( definition.relativePrefix() + prefixToRemove, filter );
	}

	private boolean isEveryPathIncludedAtDepth(int depth) {
		return depthFilter.isEveryPathIncludedAtDepth( depth )
				// The parent can reduce the max depth of a child.
				&& ( parent == null || parent.isEveryPathIncludedAtDepth( depth + 1 ) );
	}

	private String getPathFromSameIndexedEmbeddedSinceNoCompositionLimits(IndexedEmbeddedDefinition definition) {
		if ( hasCompositionLimits() ) {
			return null;
		}
		else if ( parent != null ) {
			if ( this.definition.equals( definition ) ) {
				// Same IndexedEmbedded as the one passed as a parameter
				return this.definition.relativePrefix();
			}
			else {
				String path = parent.getPathFromSameIndexedEmbeddedSinceNoCompositionLimits( definition );
				return path == null ? null : path + this.definition.relativePrefix();
			}
		}
		else {
			/*
			 * No recursion limits, no parent: this is the root.
			 * I we reach this point, it means there was no recursion limit at all,
			 * but we did not encounter the IndexedEmbedded we were looking for.
			 */
			return null;
		}
	}

	public IndexSchemaFilter compose(IndexedEmbeddedDefinition definition, IndexedEmbeddedPathTracker pathTracker) {
		String cyclicRecursionPath = getPathFromSameIndexedEmbeddedSinceNoCompositionLimits( definition );
		if ( cyclicRecursionPath != null ) {
			cyclicRecursionPath += definition.relativePrefix();
			throw log.indexedEmbeddedCyclicRecursion( cyclicRecursionPath, definition.definingTypeModel() );
		}

		// The new depth filter according to the given includeDepth
		DepthFilter newDepthFilter = DepthFilter.of( definition.includeDepth() );

		// The new path filter according to the given includedPaths
		PathFilter newPathFilter = PathFilter.of( definition.includePaths(), definition.excludePaths() );

		return new IndexSchemaFilter(
				this, definition, pathTracker,
				newDepthFilter, newPathFilter
		);
	}

	private boolean hasCompositionLimits() {
		return depthFilter.hasDepthLimit() || pathFilter.isAnyPathExplicitlyIncluded()
				|| parent != null && parent.hasCompositionLimits();
	}
}
