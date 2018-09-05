/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.gson.impl;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.Supplier;

import org.hibernate.search.elasticsearch.impl.JsonBuilder;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * A {@link JsonAccessor} whose purpose is to get access to a new node
 * in the data structure (object property, array element, ...).
 *
 * @author Yoann Rodiere
 */
abstract class CrawlingJsonAccessor<P extends JsonElement> extends NonRootJsonAccessor<P, JsonElement>
		implements UnknownTypeJsonAccessor {

	public CrawlingJsonAccessor(JsonCompositeAccessor<P> parentAccessor) {
		super( parentAccessor );
	}

	@Override
	protected JsonCompositeAccessor<P> getParentAccessor() {
		return (JsonCompositeAccessor<P>) super.getParentAccessor();
	}
	@Override
	public Optional<JsonElement> get(JsonObject root) throws UnexpectedJsonElementTypeException {
		return getParentAccessor().get( root ).map( this::doGet );
	}

	protected abstract JsonElement doGet(P parent);

	@Override
	public void set(JsonObject root, JsonElement newValue) throws UnexpectedJsonElementTypeException {
		P parent = getParentAccessor().getOrCreate( root );
		doSet( parent, newValue );
	}

	protected abstract void doSet(P parent, JsonElement newValue);

	@Override
	public JsonElement getOrCreate(JsonObject root, Supplier<? extends JsonElement> newValueSupplier) throws UnexpectedJsonElementTypeException {
		P parent = getParentAccessor().getOrCreate( root );
		JsonElement currentValue = doGet( parent );
		if ( currentValue == null || currentValue.isJsonNull() ) {
			JsonElement result = newValueSupplier.get();
			doSet( parent, result );
			return result;
		}
		else {
			return currentValue;
		}
	}

	@Override
	public void add(JsonObject root, JsonElement newValue) throws UnexpectedJsonElementTypeException {
		P parent = getParentAccessor().getOrCreate( root );
		JsonElement currentValue = doGet( parent );
		if ( currentValue == null ) { // Do not overwrite JsonNull, because it might be there on purpose
			doSet( parent, newValue );
		}
		else if ( JsonElementType.ARRAY.isInstance( currentValue ) ) {
			JsonElementType.ARRAY.fromElement( currentValue ).add( newValue );
		}
		else if ( JsonElementType.PRIMITIVE.isInstance( currentValue )
				|| JsonElementType.NULL.isInstance( currentValue ) ) {
			doSet( parent, JsonBuilder.array().add( currentValue ).add( newValue ).build() );
		}
		else {
			throw new UnexpectedJsonElementTypeException(
					this,
					Arrays.asList( JsonElementType.ARRAY, JsonElementType.PRIMITIVE, JsonElementType.NULL ),
					currentValue );
		}
	}

	@Override
	public JsonObjectAccessor asObject() {
		return new JsonObjectAccessorImpl( this );
	}

	@Override
	public JsonArrayAccessor asArray() {
		return new JsonArrayAccessorImpl( this );
	}

	@Override
	public JsonAccessor<String> asString() {
		return new JsonStringAccessorImpl( this );
	}

	@Override
	public JsonAccessor<Boolean> asBoolean() {
		return new JsonBooleanAccessorImpl( this );
	}

	@Override
	public JsonAccessor<Integer> asInteger() {
		return new JsonIntegerAccessorImpl( this );
	}

	@Override
	public JsonAccessor<Float> asFloat() {
		return new JsonFloatAccessorImpl( this );
	}

	@Override
	public JsonAccessor<Double> asDouble() {
		return new JsonDoubleAccessorImpl( this );
	}

	@Override
	public UnknownTypeJsonAccessor element(int index) {
		return asArray().element( index );
	}

	@Override
	public UnknownTypeJsonAccessor property(String propertyName) {
		return asObject().property( propertyName );
	}


}
