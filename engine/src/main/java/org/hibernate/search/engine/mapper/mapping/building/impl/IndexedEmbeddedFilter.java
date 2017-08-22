/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.mapper.mapping.building.impl;

import java.util.HashSet;
import java.util.Set;

import org.hibernate.search.engine.mapper.model.spi.IndexableTypeOrdering;
import org.hibernate.search.engine.mapper.model.spi.IndexedTypeIdentifier;
import org.hibernate.search.util.SearchException;

/**
 * @author Yoann Rodiere
 */
/*
 * TODO discuss IndexedEmbedded behavior
 *
 * Historically, the recursion context's "depth" has always been the *embedding* depth,
 * not the entity property nesting depth or the index field nesting depth.
 * As a result, bridges are free to use properties up to any depth,
 * as long as the property the bridge was defined on is at the right depth.
 * On the other hand, path filters have always been about *fields*,
 * not embedding nor properties.
 */
public class IndexedEmbeddedFilter {

	private final IndexableTypeOrdering typeOrdering;

	private final IndexedEmbeddedFilter parent;
	private final IndexedTypeIdentifier parentTypeId;
	private final String relativePrefix;

	private final Integer remainingDepth;
	private final Set<String> pathFilters;

	public IndexedEmbeddedFilter(IndexableTypeOrdering typeOrdering) {
		this( typeOrdering, null, null, null, null, null );
	}

	private IndexedEmbeddedFilter(IndexableTypeOrdering typeOrdering,
			IndexedEmbeddedFilter parent, IndexedTypeIdentifier parentTypeId, String relativePrefix,
			Integer remainingDepth, Set<String> pathFilters) {
		this.typeOrdering = typeOrdering;
		this.parent = parent;
		this.parentTypeId = parentTypeId;
		this.relativePrefix = relativePrefix;
		this.remainingDepth = remainingDepth;
		this.pathFilters = pathFilters;
	}

	@Override
	public String toString() {
		return new StringBuilder( getClass().getSimpleName() )
				.append( "[" )
				.append( "parentTypeId=" ).append( parentTypeId )
				.append( ",relativePrefix=" ).append( relativePrefix )
				.append( ",remainingDepth=" ).append( remainingDepth )
				.append( ",pathFilters=" ).append( pathFilters )
				.append( "]" )
				.toString();
	}

	public boolean isPathIncluded(String relativePath) {
		return remainingDepth == null || remainingDepth > 0
				|| pathFilters != null && pathFilters.contains( relativePath );
	}

	private boolean hasRecursionLimits() {
		return remainingDepth != null || pathFilters != null;
	}

	private String getPathFromIndexedEmbeddedSinceNoRecursionLimits(IndexedTypeIdentifier parentTypeId, String relativePrefix) {
		if ( hasRecursionLimits() ) {
			return null;
		}
		else if ( parent != null ) {
			if ( typeOrdering.isSubType( parentTypeId, this.parentTypeId )
					&& this.relativePrefix.equals( relativePrefix ) ) {
				// Same IndexedEmbedded as the one passed as a parameter
				return this.relativePrefix;
			}
			else {
				String path = parent.getPathFromIndexedEmbeddedSinceNoRecursionLimits( parentTypeId, relativePrefix );
				return path == null ? null : path + relativePrefix;
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

	public boolean isTerminal() {
		return remainingDepth != null && remainingDepth <= 0 && pathFilters == null
				|| pathFilters != null && pathFilters.isEmpty();
	}

	public IndexedEmbeddedFilter composeWithNested(IndexedTypeIdentifier parentTypeId, String relativePrefix,
			Integer nestedMaxDepth, Set<String> nestedPathFilters) {
		String cyclicRecursionPath = getPathFromIndexedEmbeddedSinceNoRecursionLimits( parentTypeId, relativePrefix );
		if ( cyclicRecursionPath != null ) {
			throw new SearchException( "Found an infinite IndexedEmbedded recursion involving path '"
					+ cyclicRecursionPath + "' on type '" + parentTypeId + "'" );
		}

		Integer defaultedNestedMaxDepth = nestedMaxDepth;
		if ( defaultedNestedMaxDepth == null && nestedPathFilters != null && !nestedPathFilters.isEmpty() ) {
			// The max depth is implicitly set to 0 when not provided and when there are path filters
			defaultedNestedMaxDepth = 0;
		}

		Integer newRemainingDepth = remainingDepth == null ? null : remainingDepth - 1;
		if ( defaultedNestedMaxDepth != null && ( remainingDepth == null || remainingDepth > defaultedNestedMaxDepth ) ) {
			newRemainingDepth = defaultedNestedMaxDepth;
		}

		Set<String> newPathFilters = null;
		if ( nestedPathFilters != null && !nestedPathFilters.isEmpty() ) {
			/*
			 * Explicit path filtering on the nested indexedEmbedded:
			 * compose filters with the current ones and use the result.
			 */
			newPathFilters = new HashSet<>();
			for ( String nestedPathFilter : nestedPathFilters ) {
				if ( isPathIncluded( relativePrefix + nestedPathFilter ) ) {
					newPathFilters.add( nestedPathFilter );
					// Also add paths leading to this path (so that object nodes are not excluded)
					int afterPreviousDotIndex = 0;
					int nextDotIndex = nestedPathFilter.indexOf( '.', afterPreviousDotIndex );
					while ( nextDotIndex >= 0 ) {
						String subPath = nestedPathFilter.substring( 0, nextDotIndex );
						newPathFilters.add( subPath );
						afterPreviousDotIndex = nextDotIndex + 1;
						nextDotIndex = nestedPathFilter.indexOf( '.', afterPreviousDotIndex );
					}
				}
			}
		}
		else if ( pathFilters != null ) {
			/*
			 * No explicit path filtering on the nested indexedEmbedded
			 * (meaning all path are included as far as it's concerned)
			 * but the parent indexedEmbedded has path filtering.
			 * Keep only filters matching the nested indexedEmbedded's prefix.
			 */
			newPathFilters = new HashSet<>();
			int relativePrefixLength = relativePrefix.length();
			for ( String pathFilter : pathFilters ) {
				if ( pathFilter.startsWith( relativePrefix ) ) {
					newPathFilters.add( pathFilter.substring( relativePrefixLength ) );
				}
			}
		}

		return new IndexedEmbeddedFilter( typeOrdering, this, parentTypeId, relativePrefix,
				newRemainingDepth, newPathFilters );
	}
}