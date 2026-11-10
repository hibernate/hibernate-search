package org.hibernate.search.backend.elasticsearch.json.impl;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.NullNode;

class ArrayElementJsonAccessor extends AbstractCrawlingJsonAccessor<ArrayNode> {

	private final int index;

	public ArrayElementJsonAccessor(JsonCompositeAccessor<ArrayNode> parentAccessor, int index) {
		super( parentAccessor );
		this.index = index;
	}

	@Override
	protected JsonNode doGet(ArrayNode parent) {
		if ( index < 0 ) {
			throw new IndexOutOfBoundsException( "Negative array index: " + index );
		}
		if ( parent != null && index < parent.size() ) {
			return parent.get( index );
		}
		else {
			return null;
		}
	}

	@Override
	protected void doSet(ArrayNode parent, JsonNode newValue) {
		fillTo( parent, index );
		parent.set( index, newValue );
	}

	private static void fillTo(ArrayNode array, int index) {
		for ( int i = array.size(); i <= index; ++i ) {
			array.add( NullNode.instance );
		}
	}

	@Override
	protected void appendRuntimeRelativePath(StringBuilder path) {
		path.append( "[" ).append( index ).append( "]" );
	}

	@Override
	protected void appendStaticRelativePath(StringBuilder path, boolean first) {
		// Nothing to do
	}
}
