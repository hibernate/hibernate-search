package org.hibernate.search.backend.elasticsearch.json.impl;

import java.util.Optional;
import java.util.function.Supplier;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;


/**
 * A {@link JsonAccessor} whose purpose is to check type information
 * on the node being accessed.
 * <p>
 * The node being accessed is provided by the "parent" accessor.
 *
 */
abstract class AbstractTypingJsonAccessor<T> extends AbstractNonRootJsonAccessor<JsonNode, T> {

	public AbstractTypingJsonAccessor(JsonAccessor<JsonNode> parentAccessor) {
		super( parentAccessor );
	}

	@Override
	public Optional<T> get(ObjectNode root) {
		return getParentAccessor().get( root ).map( this::fromElement );
	}

	@Override
	public void set(ObjectNode root, T newValue) {
		getParentAccessor().set( root, toElement( newValue ) );
	}

	@Override
	public T getOrCreate(ObjectNode root, Supplier<? extends T> newValueSupplier) {
		return getOrCreateNode( root, () -> toElement( newValueSupplier.get() ) );
	}

	protected T getOrCreateNode(ObjectNode root, Supplier<? extends JsonNode> newNodeSupplier) {
		return fromElement( getParentAccessor().getOrCreate( root, newNodeSupplier ) );
	}

	private T fromElement(JsonNode parent) {
		JsonElementType<T> expectedType = getExpectedElementType();
		if ( parent == null || parent.isNull() ) {
			return null;
		}
		else if ( !expectedType.isInstance( parent ) ) {
			throw new UnexpectedJsonElementTypeException( this, expectedType, parent );
		}
		else {
			return expectedType.fromElement( parent );
		}
	}

	protected JsonNode toElement(T value) {
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
