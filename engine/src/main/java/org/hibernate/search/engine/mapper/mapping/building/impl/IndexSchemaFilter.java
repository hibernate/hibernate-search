/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.mapper.mapping.building.impl;

import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
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
 *     {@link #composeWithNested(MappableTypeModel, String, Integer, Set)} method, which may return a filter
 *     that {@link #isEveryPathExcluded() excludes every path}, meaning the {@code @IndexedEmbedded} will
 *     be ignored</li>
 * </ul>
 *
 * <h3 id="filter-properties">Filter properties</h3>
 * <p>
 * A filter decides whether to include a path or not according to its two main properties:
 * <ul>
 *     <li>the {@link #remainingCompositionDepth remaining composition depth}</li>
 *     <li>the {@link #explicitlyIncludedPaths explicitly included paths}</li>
 * </ul>
 * <p>
 * The explicitly included paths, as their name suggests, define which paths
 * should be accepted by this filter no matter what.
 * <p>
 * The composition depth defines
 * {@link #isEveryPathIncludedByDefault(Integer) how paths that do not appear in the explicitly included paths should be treated}:
 * <ul>
 *     <li>if {@code <= 0}, paths are excluded by default</li>
 *     <li>if {@code null} or {@code > 0}, paths are included by default</li>
 * </ul>
 *
 * <h3 id="filter-composition">Filter composition</h3>
 * <p>
 * Composed filters are created whenever a nested {@code @IndexedEmbedded} is encountered.
 * A composed filter will always enforce the restrictions of its parent filter,
 * plus some added restrictions depending on the properties of the nested {@code IndexedEmbedded}.
 * <p>
 * For more information about how filters are composed, see
 * {@link #composeWithNested(MappableTypeModel, String, Integer, Set)}.
 *
 */
class IndexSchemaFilter {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private static final IndexSchemaFilter ROOT = new IndexSchemaFilter(
			null, null, null,
			null, Collections.emptySet(), Collections.emptySet()
	);

	public static IndexSchemaFilter root() {
		return ROOT;
	}

	private final IndexSchemaFilter parent;
	private final MappableTypeModel parentTypeModel;
	private final String relativePrefix;

	/**
	 * Defines how deep indexed embedded are allowed to be composed.
	 *
	 * Note that composition depth only relates to IndexedEmbedded composition;
	 * bridge-declared fields are only affected by path filtering,
	 * whose default behavior (include or exclude) is determined by {@link #isEveryPathIncludedByDefault(Integer)}.
	 */
	private final Integer remainingCompositionDepth;

	/**
	 * Defines paths to be included even when the default behavior is to exclude paths.
	 */
	private final Set<String> explicitlyIncludedPaths;

	/**
	 * The {@code explicitlyIncludedPaths} that were included by this filter explicitly (not a parent filter).
	 */
	private final Set<String> localExplicitlyIncludedPaths;

	/**
	 * The {@code paths} that were encountered, i.e. passed to {@link #isPathIncluded(String)}
	 * or to the same method of a child filter.
	 */
	// Use a LinkedHashSet, since the set will be exposed through a getter and may be iterated on
	private final Map<String, Boolean> encounteredFieldPaths = new LinkedHashMap<>();

	private IndexSchemaFilter(IndexSchemaFilter parent, MappableTypeModel parentTypeModel, String relativePrefix,
			Integer remainingCompositionDepth, Set<String> explicitlyIncludedPaths,
			Set<String> localExplicitlyIncludedPaths) {
		this.parent = parent;
		this.parentTypeModel = parentTypeModel;
		this.relativePrefix = relativePrefix;
		this.remainingCompositionDepth = remainingCompositionDepth;
		this.explicitlyIncludedPaths = explicitlyIncludedPaths;
		this.localExplicitlyIncludedPaths = localExplicitlyIncludedPaths;
	}

	@Override
	public String toString() {
		return new StringBuilder( getClass().getSimpleName() )
				.append( "[" )
				.append( "parentTypeModel=" ).append( parentTypeModel )
				.append( ",relativePrefix=" ).append( relativePrefix )
				.append( ",remainingCompositionDepth=" ).append( remainingCompositionDepth )
				.append( ",explicitlyIncludedPaths=" ).append( explicitlyIncludedPaths )
				.append( "]" )
				.toString();
	}

	public boolean isPathIncluded(String relativePath) {
		boolean included = isPathIncluded( remainingCompositionDepth, explicitlyIncludedPaths, relativePath );
		markAsEncountered( relativePath, included );
		return included;
	}

	private void markAsEncountered(String relativePath, boolean included) {
		encounteredFieldPaths.put( relativePath, included );
		if ( parent != null ) {
			parent.markAsEncountered( relativePrefix + relativePath, included );
		}
	}

	public boolean isEveryPathExcluded() {
		return !isEveryPathIncludedByDefault( remainingCompositionDepth ) && !isAnyPathExplicitlyIncluded();
	}

	public Set<String> getLocalExplicitlyIncludedPaths() {
		return localExplicitlyIncludedPaths;
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

	public IndexSchemaFilter composeWithNested(MappableTypeModel parentTypeModel, String relativePrefix,
			Integer maxDepth, Set<String> includePaths) {
		String cyclicRecursionPath = getPathFromSameIndexedEmbeddedSinceNoCompositionLimits( parentTypeModel, relativePrefix );
		if ( cyclicRecursionPath != null ) {
			cyclicRecursionPath += relativePrefix;
			throw log.indexedEmbeddedCyclicRecursion( cyclicRecursionPath, parentTypeModel );
		}

		Set<String> nullSafeIncludePaths = includePaths == null ? Collections.emptySet() : includePaths;

		// The remaining composition depth according to "this" only
		Integer currentRemainingDepth = remainingCompositionDepth == null ? null : remainingCompositionDepth - 1;

		// The remaining composition depth according to the nested IndexedEmbedded only
		Integer nestedRemainingDepth = maxDepth;
		if ( maxDepth == null && !nullSafeIncludePaths.isEmpty() ) {
			/*
			 * If no max depth was provided and "includePaths" was provided,
			 * the remaining composition depth is implicitly set to 0,
			 * meaning no composition is allowed and paths are excluded unless
			 * explicitly listed in "includePaths".
			 */
			nestedRemainingDepth = 0;
		}

		/*
		 * By default, a composed filters' remaining composition depth is its parent's minus one
		 * (or null if the remaining composition depth was not set in the parent)...
		 */
		Integer composedRemainingDepth = currentRemainingDepth;
		if ( composedRemainingDepth == null
				|| nestedRemainingDepth != null && composedRemainingDepth > nestedRemainingDepth ) {
			/*
			 * ... but the nested filter can reduce it.
			 */
			composedRemainingDepth = nestedRemainingDepth;
		}

		// The included paths that will be used to determine inclusion
		Set<String> composedFilterExplicitlyIncludedPaths = new HashSet<>();
		// The subset of these included paths that were added by the nested filter (and not a parent filter)
		// Use a LinkedHashSet, since the set will be exposed through a getter and may be iterated on
		Set<String> composedFilterLocalExplicitlyIncludedPaths = new LinkedHashSet<>();

		/*
		 * Add the nested filter's explicitly included paths to the composed filter's "explicitlyIncludedPaths",
		 * provided they are not filtered out by the current filter.
		 */
		for ( String path : nullSafeIncludePaths ) {
			if ( isPathIncluded( currentRemainingDepth, explicitlyIncludedPaths, relativePrefix + path ) ) {
				composedFilterExplicitlyIncludedPaths.add( path );
				composedFilterLocalExplicitlyIncludedPaths.add( path );
				// Also add paths leading to this path (so that object nodes are not excluded)
				int afterPreviousDotIndex = 0;
				int nextDotIndex = path.indexOf( '.', afterPreviousDotIndex );
				while ( nextDotIndex >= 0 ) {
					String subPath = path.substring( 0, nextDotIndex );
					composedFilterExplicitlyIncludedPaths.add( subPath );
					afterPreviousDotIndex = nextDotIndex + 1;
					nextDotIndex = path.indexOf( '.', afterPreviousDotIndex );
				}
			}
		}
		/*
		 * Add the current filter's explicitly included paths to the composed filter's "explicitlyIncludedPaths",
		 * provided they start with the nested filter's prefix and are not filtered out by the nested filter.
		 */
		int relativePrefixLength = relativePrefix.length();
		for ( String path : explicitlyIncludedPaths ) {
			if ( path.startsWith( relativePrefix ) ) {
				String pathRelativeToNestedFilter = path.substring( relativePrefixLength );
				if ( isPathIncluded( nestedRemainingDepth, nullSafeIncludePaths, pathRelativeToNestedFilter ) ) {
					composedFilterExplicitlyIncludedPaths.add( pathRelativeToNestedFilter );
				}
			}
		}

		return new IndexSchemaFilter(
				this, parentTypeModel, relativePrefix,
				composedRemainingDepth, composedFilterExplicitlyIncludedPaths,
				composedFilterLocalExplicitlyIncludedPaths
		);
	}

	private static boolean isPathIncluded(Integer remainingDepth, Set<String> explicitlyIncludedPaths, String relativePath) {
		return isEveryPathIncludedByDefault( remainingDepth )
				|| explicitlyIncludedPaths.contains( relativePath );
	}

	private static boolean isEveryPathIncludedByDefault(Integer remainingDepth) {
		/*
		 * A remaining composition depth of 0 or below means
		 * paths should be excluded when filtering unless mentioned in explicitlyIncludedPaths.
		 */
		return remainingDepth == null || remainingDepth > 0;
	}

	private boolean isAnyPathExplicitlyIncluded() {
		return !explicitlyIncludedPaths.isEmpty();
	}

	private boolean hasCompositionLimits() {
		return remainingCompositionDepth != null || !explicitlyIncludedPaths.isEmpty();
	}
}