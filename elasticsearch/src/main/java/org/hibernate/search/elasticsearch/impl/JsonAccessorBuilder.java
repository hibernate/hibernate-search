/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.impl;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

import org.hibernate.search.elasticsearch.gson.impl.JsonAccessor;
import org.hibernate.search.elasticsearch.gson.impl.UnknownTypeJsonAccessor;
import org.hibernate.search.elasticsearch.impl.NestingMarker.NestingPathComponent;
import org.hibernate.search.elasticsearch.util.impl.ParentPathMismatchException;
import org.hibernate.search.elasticsearch.util.impl.PathComponentExtractor;
import org.hibernate.search.elasticsearch.util.impl.PathComponentExtractor.ConsumptionLimit;
import org.hibernate.search.engine.metadata.impl.EmbeddedTypeMetadata;

/**
 * A stateful helper to create JSON accessors from a {@link NestingPathComponent nesting path}.
 *
 * <p>The code below may seem overly complicated, but the complexity lies in the requirements:
 * the mapping between the path components (Java attributes) and field path components
 * (nested objects in the index) is not at all 1-1:
 *
 * <ul>
 * <li>if the field prefix is something like "someprefix_" (no final dot), the nesting
 *    path component will only define *part* (the beginning) of a field path component.
 *    The actual field, or an embedded, will define the next part of the field path component.
 * <li>if the field prefix is something like "some.chain.of.identifiers.", the nesting path
 *    component will define *multiple* field path components.
 * <li>and of course, the field prefix may be some combination of the above, such as
 *    "some.chain.of.identifiers.someprefix_".
 * </ul>
 *
 * @author Yoann Rodiere
 */
final class JsonAccessorBuilder {

	private UnknownTypeJsonAccessor currentAccessor = null;

	/*
	 * This variable allows to keep in memory the fact that a previous path component could not have its index handled,
	 * and thus must have it handled later.
	 */
	private final Deque<Integer> indexes = new ArrayDeque<>();

	/*
	 * This variable allows to keep in memory the prefix from the last embeddable, if it
	 * did not end with a dot.
	 */
	private final PathComponentExtractor pathComponentExtractor = new PathComponentExtractor();

	public void reset() {
		this.indexes.clear();
		this.pathComponentExtractor.reset();
		this.currentAccessor = null;
	}

	/**
	 * Nest accessors as necessary to match the given nesting path.
	 *
	 * @param component The new path component.
	 */
	public void append(List<NestingPathComponent> nestingPath) {
		if ( nestingPath == null ) {
			return;
		}

		for ( NestingPathComponent pathComponent : nestingPath ) {
			EmbeddedTypeMetadata embeddedTypeMetadata = pathComponent.getEmbeddedTypeMetadata();

			pathComponentExtractor.append( embeddedTypeMetadata.getEmbeddedFieldPrefix() );

			Integer currentComponentArrayIndex = pathComponent.getIndex();
			if ( currentComponentArrayIndex != null ) {
				indexes.addLast( currentComponentArrayIndex );
			}

			UnknownTypeJsonAccessor newAccessor = consumePath( pathComponentExtractor, ConsumptionLimit.SECOND_BUT_LAST );
			if ( newAccessor != currentAccessor ) {
				currentAccessor = newAccessor;
				indexes.clear();
			}
		}
	}

	/**
	 * Nest accessors as many times as possible by creating at least one element for
	 * each fully-defined "field path component", i.e. each non-null result of
	 * {@link PathComponentExtractor#next()}.
	 * <p>Multiple accessors may get created for one of those field path components,
	 * if {@code indexes} is non-empty (requiring the creation of arrays).
	 *
	 * @param extractor The field path builder to extract field path components from.
	 * @param consumptionLimit The consumption limit to pass to {@link PathComponentExtractor#next(ConsumptionLimit)).
	 * @return The resulting accessor.
	 */
	private UnknownTypeJsonAccessor consumePath(PathComponentExtractor extractor, ConsumptionLimit consumptionLimit ) {
		String childName = extractor.next( consumptionLimit );
		UnknownTypeJsonAccessor newAccessor = currentAccessor;
		boolean consumeIndexes = !indexes.isEmpty();

		while ( childName != null ) {
			if ( newAccessor == null ) {
				newAccessor = JsonAccessor.root().property( childName );
			}
			else {
				newAccessor = newAccessor.property( childName );
			}

			if ( consumeIndexes ) {
				for ( Integer index : indexes ) {
					newAccessor = newAccessor.element( index );
				}
				consumeIndexes = false; // Only consume indexes once
			}

			// Iterate
			childName = extractor.next( consumptionLimit );
		}

		return newAccessor;
	}

	public UnknownTypeJsonAccessor buildForPath(String absolutePath) throws ParentPathMismatchException {
		/*
		 * We must run the path through a field path builder again to handle cases
		 * where the field name contains dots (and therefore requires creating containing
		 * properties independently of the nesting context).
		 */
		PathComponentExtractor newExtractor = this.pathComponentExtractor.clone();
		newExtractor.appendRelativePart( absolutePath );

		return consumePath( newExtractor, ConsumptionLimit.LAST );
	}

}