/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.gson.impl;

import java.util.Optional;
import java.util.function.Supplier;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * A {@link JsonAccessor} whose purpose is to check type information
 * on the node being accessed.
 * <p>
 * The node being accessed is provided by the "parent" accessor.
 *
 * @author Yoann Rodiere
 */
abstract class TypingJsonAccessor<T> extends NonRootJsonAccessor<JsonElement, T> {

	public TypingJsonAccessor(JsonAccessor<JsonElement> parentAccessor) {
		super( parentAccessor );
	}

	@Override
	public Optional<T> get(JsonObject root) throws UnexpectedJsonElementTypeException {
		return getParentAccessor().get( root ).map( this::fromElement );
	}

	@Override
	public void set(JsonObject root, T newValue) throws UnexpectedJsonElementTypeException {
		getParentAccessor().set( root, toElement( newValue ) );
	}

	@Override
	public void add(JsonObject root, T newValue) throws UnexpectedJsonElementTypeException {
		getParentAccessor().add( root, toElement( newValue ) );
	}

	@Override
	public T getOrCreate(JsonObject root, Supplier<? extends T> newValueSupplier) throws UnexpectedJsonElementTypeException {
		return fromElement( getParentAccessor().getOrCreate( root, () -> toElement( newValueSupplier.get() ) ) );
	}

	private T fromElement(JsonElement parent) throws UnexpectedJsonElementTypeException {
		JsonElementType<T> expectedType = getExpectedElementType();
		if ( parent == null || parent.isJsonNull() ) {
			return null;
		}
		else if ( !expectedType.isInstance( parent ) ) {
			throw new UnexpectedJsonElementTypeException( this, expectedType, parent );
		}
		else {
			return expectedType.fromElement( parent );
		}
	}

	protected JsonElement toElement(T value) {
		return getExpectedElementType().toElement( value );
	}

	protected abstract JsonElementType<T> getExpectedElementType();

	@Override
	protected void appendRuntimeRelativePath(StringBuilder path) {
		path.append( '(' ).append( getExpectedElementType() ).append( ')' );
	}

	@Override
	protected void appendStaticRelativePath(StringBuilder path, boolean first) {
		// No-op: the type doesn't show up in this path
	}

}
