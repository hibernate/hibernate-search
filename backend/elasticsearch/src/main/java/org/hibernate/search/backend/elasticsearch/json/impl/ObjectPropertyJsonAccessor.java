package org.hibernate.search.backend.elasticsearch.json.impl;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;


class ObjectPropertyJsonAccessor extends AbstractCrawlingJsonAccessor<ObjectNode> {
	private final String propertyName;

	public ObjectPropertyJsonAccessor(JsonCompositeAccessor<ObjectNode> parentAccessor, String propertyName) {
		super( parentAccessor );
		this.propertyName = propertyName;
	}

	@Override
	protected JsonNode doGet(ObjectNode parent) {
		return parent.get( propertyName );
	}

	@Override
	protected void doSet(ObjectNode parent, JsonNode newValue) {
		parent.set( propertyName, newValue );
	}

	@Override
	protected void appendRuntimeRelativePath(StringBuilder path) {
		path.append( "." ).append( propertyName );
	}

	@Override
	protected void appendStaticRelativePath(StringBuilder path, boolean isFirst) {
		if ( !isFirst ) {
			path.append( "." );
		}
		path.append( propertyName );
	}
}
