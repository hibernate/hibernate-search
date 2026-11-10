package org.hibernate.search.backend.elasticsearch.json.impl;

import java.util.Optional;
import java.util.function.Supplier;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;


/**
 * A {@link JsonAccessor} whose purpose is to get access to a new node
 * in the data structure (object property, array element, ...).
 *
 */
abstract class AbstractCrawlingJsonAccessor<P extends JsonNode> extends AbstractNonRootJsonAccessor<P, JsonNode>
		implements UnknownTypeJsonAccessor {

	public AbstractCrawlingJsonAccessor(JsonCompositeAccessor<P> parentAccessor) {
		super( parentAccessor );
	}

	@Override
	protected JsonCompositeAccessor<P> getParentAccessor() {
		return (JsonCompositeAccessor<P>) super.getParentAccessor();
	}

	@Override
	public Optional<JsonNode> get(ObjectNode root) {
		return getParentAccessor().get( root ).map( this::doGet );
	}

	protected abstract JsonNode doGet(P parent);

	@Override
	public void set(ObjectNode root, JsonNode newValue) {
		P parent = getParentAccessor().getOrCreate( root );
		if ( parent == null ) {
			throw new IllegalStateException( "Cannot traverse an absent adapted parent at " + getParentAccessor() );
		}
		doSet( parent, newValue );
	}

	protected abstract void doSet(P parent, JsonNode newValue);

	@Override
	public JsonNode getOrCreate(ObjectNode root, Supplier<? extends JsonNode> newValueSupplier) {
		P parent = getParentAccessor().getOrCreate( root );
		if ( parent == null ) {
			throw new IllegalStateException( "Cannot traverse an absent adapted parent at " + getParentAccessor() );
		}
		JsonNode currentValue = doGet( parent );
		if ( currentValue == null || currentValue.isNull() ) {
			JsonNode result = newValueSupplier.get();
			doSet( parent, result );
			return result;
		}
		else {
			return currentValue;
		}
	}

	@Override
	public UnknownTypeJsonAccessor unwrapSingletonArrays() {
		return new SingletonArrayJsonAccessor( this );
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
