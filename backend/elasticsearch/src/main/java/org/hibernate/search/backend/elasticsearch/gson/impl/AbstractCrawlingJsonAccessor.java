/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.gson.impl;

import java.util.Optional;
import java.util.function.Supplier;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * A {@link JsonAccessor} whose purpose is to get access to a new node
 * in the data structure (object property, array element, ...).
 *
 */
abstract class AbstractCrawlingJsonAccessor<P extends JsonElement> extends AbstractNonRootJsonAccessor<P, JsonElement>
		implements UnknownTypeJsonAccessor {

	public AbstractCrawlingJsonAccessor(JsonCompositeAccessor<P> parentAccessor) {
		super( parentAccessor );
	}

	@Override
	protected JsonCompositeAccessor<P> getParentAccessor() {
		return (JsonCompositeAccessor<P>) super.getParentAccessor();
	}

	@Override
	public Optional<JsonElement> get(JsonObject root) {
		return getParentAccessor().get( root ).map( this::doGet );
	}

	protected abstract JsonElement doGet(P parent);

	@Override
	public void set(JsonObject root, JsonElement newValue) {
		P parent = getParentAccessor().getOrCreate( root );
		doSet( parent, newValue );
	}

	protected abstract void doSet(P parent, JsonElement newValue);

	@Override
	public JsonElement getOrCreate(JsonObject root, Supplier<? extends JsonElement> newValueSupplier) {
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
	public JsonObjectAccessor asObject() {
		return new JsonObjectAccessorImpl( this );
	}

	@Override
	public JsonArrayAccessor asArray() {
		return new JsonArrayAccessorImpl( this );
	}

	@Override
	public JsonAccessor<String> asString() {
		return new JsonStringAccessor( this );
	}

	@Override
	public JsonAccessor<Boolean> asBoolean() {
		return new JsonBooleanAccessor( this );
	}

	@Override
	public JsonAccessor<Integer> asInteger() {
		return new JsonIntegerAccessor( this );
	}

	@Override
	public JsonAccessor<Long> asLong() {
		return new JsonLongAccessor( this );
	}

	@Override
	public JsonAccessor<Float> asFloat() {
		return new JsonFloatAccessor( this );
	}

	@Override
	public JsonAccessor<Double> asDouble() {
		return new JsonDoubleAccessor( this );
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
