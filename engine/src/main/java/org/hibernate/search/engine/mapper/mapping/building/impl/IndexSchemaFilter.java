/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.mapper.mapping.building.impl;

import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.hibernate.search.engine.logging.impl.Log;
import org.hibernate.search.engine.mapper.model.spi.MappableTypeModel;
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
 *     {@link #compose(MappableTypeModel, String, Integer, Set)} method, which may return a filter
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
 * {@link #compose(MappableTypeModel, String, Integer, Set)}.
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
	private final MappableTypeModel parentTypeModel;
	private final String relativePrefix;

	private final DepthFilter depthFilter;

	private final PathFilter pathFilter;

	/**
	 * The {@code paths} that were encountered, i.e. passed to {@link #isPathIncluded(String)}
	 * or to the same method of a child filter.
	 */
	// Use a LinkedHashSet, since the set will be exposed through a getter and may be iterated on
	private final Map<String, Boolean> encounteredFieldPaths = new LinkedHashMap<>();

	private IndexSchemaFilter(IndexSchemaFilter parent, MappableTypeModel parentTypeModel, String relativePrefix,
			DepthFilter depthFilter, PathFilter pathFilter) {
		this.parent = parent;
		this.parentTypeModel = parentTypeModel;
		this.relativePrefix = relativePrefix;
		this.depthFilter = depthFilter;
		this.pathFilter = pathFilter;
	}

	@Override
	public String toString() {
		return new StringBuilder( getClass().getSimpleName() )
				.append( "[" )
				.append( ",parentTypeModel=" ).append( parentTypeModel )
				.append( ",relativePrefix=" ).append( relativePrefix )
				.append( ",depthFilter=" ).append( depthFilter )
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
		boolean includedByThis = depthFilter.isEveryPathIncludedAtDepth( relativeDepth )
				|| pathFilter.isExplicitlyIncluded( relativePath );

		boolean includedByParent = true;
		/*
		 * The parent can filter out paths that are considered as included by a child,
		 * by reducing the maxDepth in particular,
		 * but it cannot include paths that are filtered out by a child.
		 */
		if ( parent != null ) {
			includedByParent = parent.isPathIncludedInternal(
					relativeDepth + 1,
					relativePrefix + relativePath,
					markAsEncountered, includedByThis
			);
		}

		boolean included = includedByParent && includedByThis;

		if ( markAsEncountered ) {
			encounteredFieldPaths.merge(
					relativePath, includedByThis && includedByChild,
					(included1, included2) -> included1 || included2
			);
		}

		return included;
	}

	boolean isEveryPathExcluded() {
		return !isEveryPathIncludedAtDepth( 0 )
				&& !isAnyPathExplicitlyIncluded( "", this );
	}

	private boolean isAnyPathExplicitlyIncluded(String prefixToRemove, IndexSchemaFilter filter) {
		int prefixLength = prefixToRemove.length();
		/*
		 * First check the explicitly included paths from this.
		 * They may be excluded by the given filter (which is either this or a descendant).
		 */
		for ( String path : pathFilter.getConfiguredIncludedPaths() ) {
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
		 * if @IndexedEmbedded(maxDepth = 0, includePaths = "embedded.foo")
		 * embeds @IndexedEmbedded(maxDepth = 42):
		 * the paths explicitly included by the parent will be included
		 * even though they are not explicitly included by the child
		 */
		return parent != null
				&& parent.isAnyPathExplicitlyIncluded( relativePrefix + prefixToRemove, filter );
	}

	private boolean isEveryPathIncludedAtDepth(int depth) {
		return depthFilter.isEveryPathIncludedAtDepth( depth )
				// The parent can reduce the max depth of a child.
				&& ( parent == null || parent.isEveryPathIncludedAtDepth( depth + 1 ) );
	}

	public Set<String> getConfiguredIncludedPaths() {
		return pathFilter.getConfiguredIncludedPaths();
	}

	public Map<String, Boolean> getEncounteredFieldPaths() {
		return encounteredFieldPaths;
	}

	private String getPathFromSameIndexedEmbeddedSinceNoCompositionLimits(MappableTypeModel parentTypeModel, String relativePrefix) {
		if ( hasCompositionLimits() ) {
			return null;
		}
		else if ( parent != null ) {
			if ( this.relativePrefix.equals( relativePrefix )
					&& this.parentTypeModel.isSubTypeOf( parentTypeModel ) ) {
				// Same IndexedEmbedded as the one passed as a parameter
				return this.relativePrefix;
			}
			else {
				String path = parent.getPathFromSameIndexedEmbeddedSinceNoCompositionLimits( parentTypeModel, relativePrefix );
				return path == null ? null : path + this.relativePrefix;
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

	public IndexSchemaFilter compose(MappableTypeModel parentTypeModel, String relativePrefix,
			Integer maxDepthToCompose, Set<String> includedPathsToCompose) {
		String cyclicRecursionPath = getPathFromSameIndexedEmbeddedSinceNoCompositionLimits( parentTypeModel, relativePrefix );
		if ( cyclicRecursionPath != null ) {
			cyclicRecursionPath += relativePrefix;
			throw log.indexedEmbeddedCyclicRecursion( cyclicRecursionPath, parentTypeModel );
		}

		Set<String> nullSafeIncludedPathsToCompose = includedPathsToCompose == null ? Collections.emptySet() : includedPathsToCompose;

		// The new depth filter according to the new max depth
		DepthFilter newDepthFilter;
		if ( maxDepthToCompose == null && !nullSafeIncludedPathsToCompose.isEmpty() ) {
			/*
			 * If no max depth was provided and included paths were provided,
			 * the remaining composition depth is implicitly set to 0,
			 * meaning no composition is allowed and paths are excluded unless
			 * explicitly listed in "includePaths".
			 */
			newDepthFilter = DepthFilter.of( 0 );
		}
		else {
			newDepthFilter = DepthFilter.of( maxDepthToCompose );
		}

		// The new path filter according to the given includedPaths
		PathFilter newPathFilter = PathFilter.of( includedPathsToCompose );

		return new IndexSchemaFilter(
				this, parentTypeModel, relativePrefix,
				newDepthFilter, newPathFilter
		);
	}

	private boolean hasCompositionLimits() {
		return depthFilter.hasDepthLimit() || pathFilter.isAnyPathExplicitlyIncluded()
				|| parent != null && parent.hasCompositionLimits();
	}
}