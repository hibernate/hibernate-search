/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.impl;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.hibernate.search.elasticsearch.impl.NestingMarker.NestingPathComponent;
import org.hibernate.search.engine.metadata.impl.EmbeddedTypeMetadata;
import org.hibernate.search.exception.AssertionFailure;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;

/**
 * A stateful helper to create JSON trees from {@link NestingPathComponent}s.
 *
 * <p>The code below may seem overly complicated, but the complexity lies in the requirements:
 * the mapping between the nesting path components (Java attributes) and field path components
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
final class JsonTreeBuilder {

	private final JsonObject root;

	/*
	 * This variable allows to keep in memory the fact that a previous path component
	 * could not have its index handled, and thus must have it handled later.
	 */
	private final Deque<Integer> indexes = new ArrayDeque<>();

	private JsonObject parent;

	private final PathComponentExtractor pathComponentExtractor = new PathComponentExtractor();

	public JsonTreeBuilder(JsonObject root) {
		super();
		this.root = root;
		this.parent = root;
	}

	private void reset() {
		indexes.clear();

		parent = root;
		pathComponentExtractor.reset();
	}

	/**
	 * Gets the parent JSON object for a field having the given nesting marker
	 *
	 * @param nestingPath The nesting path.
	 * @return The JSON object representing the parent for any property in the
	 * given nesting path.
	 */
	public JsonObject getOrCreateParent(List<NestingPathComponent> nestingPath) {
		if ( nestingPath == null ) {
			return root;
		}

		reset();

		for ( NestingPathComponent pathComponent : nestingPath ) {
			EmbeddedTypeMetadata embeddedTypeMetadata = pathComponent.getEmbeddedTypeMetadata();

			pathComponentExtractor.append( embeddedTypeMetadata.getEmbeddedFieldPrefix() );

			Integer currentComponentArrayIndex = pathComponent.getIndex();
			if ( currentComponentArrayIndex != null ) {
				indexes.addLast( currentComponentArrayIndex );
			}

			advanceInPath();
		}
		return parent;
	}

	/**
	 * Updates the parent as many times as possible by creating at least one element for
	 * each fully-defined "field path component", i.e. each dot-separated substring of
	 * {@code path} between {@code currentIndexInPath} and the last dot.
	 * <p>Multiple elements may get created for one of those field path components,
	 * if {@code indexes} is non-empty (requiring the creation of arrays).
	 */
	private void advanceInPath() {
		String childName = pathComponentExtractor.next();
		while ( childName != null ) {
			JsonObject newParent;

			if ( !indexes.isEmpty() ) {
				/*
				 * There were indexes in previous path components.
				 * We'll have to create an array, or multiple nested arrays, to reflect
				 * those indexes in the resulting object tree.
				 */
				JsonArray array = getOrCreate( parent, childName, JsonElementType.ARRAY );

				newParent = getOrCreate( array, indexes, JsonElementType.OBJECT );
				indexes.clear();
			}
			else {
				newParent = getOrCreate( parent, childName, JsonElementType.OBJECT );
			}

			parent = newParent;
			childName = pathComponentExtractor.next();
		}
	}

	/**
	 * Gets the element at path {@code parent.childName}, creating and adding it if necessary.
	 * <p>This method ensures that the element at path {@code parent.childname} is of type
	 * {@code type}.
	 * @param array The containing array
	 * @param indexes The index of the returned element
	 * @param type The type of the returned element
	 * @return The element at path {@code parent.childname}
	 * @throws AssertionFailure if the element at path {@code parent.childName} already exists
	 * and has incorrect type.
	 */
	private <T extends JsonElement> T getOrCreate(JsonObject parent, String childName, JsonElementType<T> type) {
		final JsonElement existingChild = parent.get( childName );
		final T childAsT;
		if ( existingChild == null ) {
			childAsT = type.newInstance();
			parent.add( childName, childAsT );
		}
		else if ( !type.isInstance( existingChild ) ) {
			throw new AssertionFailure(
					"Incorrect type for element '" + childName + "'. Expected '"
					+ type + "', got '" + existingChild.getClass() + "'"
					);
		}
		else {
			childAsT = type.cast( existingChild );
		}

		return childAsT;
	}

	/**
	 * Gets the element at path {@code array[index]}, creating and adding it if necessary.
	 * <p>The array is populated with as many null elements as necessary to fill in the gap
	 * between its current size and the requested index.
	 * <p>This method ensures that the element at path {@code array[index]} is of type
	 * {@code type}.
	 * @param array The containing array
	 * @param indexes The index of the returned element
	 * @param type The type of the returned element
	 * @return The element at path {@code array[index]}
	 * @throws AssertionFailure if the element at path {@code array[index]} already exists
	 * and has incorrect type.
	 */
	private <T extends JsonElement> T getOrCreate(JsonArray array, int index, JsonElementType<T> type) {
		final JsonElement existingArrayElement = array.size() <= index ? null : array.get( index );
		final T arrayElementAsT;
		if ( existingArrayElement == null ) {
			arrayElementAsT = type.newInstance();
			// Fill in the gaps, if any
			for ( int i = array.size(); i <= index; ++i ) {
				array.add( JsonNull.INSTANCE );
			}
			array.set( index, arrayElementAsT );
		}
		else if ( !type.isInstance( existingArrayElement ) ) {
			throw new AssertionFailure(
					"Incorrect type for element at index '" + index + "'. Expected '"
					+ type + "', got '" + existingArrayElement.getClass() + "'"
					);
		}
		else {
			arrayElementAsT = type.cast( existingArrayElement );
		}

		return arrayElementAsT;
	}

	/**
	 * Gets the element at path {@code array[index1][index2]...[indexN]}, creating and
	 * adding every component in the path as necessary
	 * <p>The arrays are populated with as many null elements as necessary to fill in
	 * the gap between their current size and the requested indexes.
	 * <p>This method ensures that the last component in the path is of type {@code type}
	 * and the previous ones of array type.
	 * @param array The root array
	 * @param indexes The indexes (must be non-empty)
	 * @param type The type of the last component in the path
	 * @return The last component in the path
	 * @throws AssertionFailure if any component in the path already exists and has
	 * incorrect type.
	 * @throws NoSuchElementException if {@code indexes} is empty.
	 */
	private <T extends JsonElement> T getOrCreate(JsonArray array, Iterable<Integer> indexes, JsonElementType<T> type) {
		Iterator<Integer> iterator = indexes.iterator();
		Integer currentIndex = iterator.next();
		while ( iterator.hasNext() ) {
			array = getOrCreate( array, currentIndex, JsonElementType.ARRAY );
			currentIndex = iterator.next();
		}

		return getOrCreate( array, currentIndex, type );
	}
}