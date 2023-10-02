/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.gson.impl;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

class ObjectPropertyJsonAccessor extends AbstractCrawlingJsonAccessor<JsonObject> {
	private final String propertyName;

	public ObjectPropertyJsonAccessor(JsonCompositeAccessor<JsonObject> parentAccessor, String propertyName) {
		super( parentAccessor );
		this.propertyName = propertyName;
	}

	@Override
	protected JsonElement doGet(JsonObject parent) {
		return parent.get( propertyName );
	}

	@Override
	protected void doSet(JsonObject parent, JsonElement newValue) {
		parent.add( propertyName, newValue );
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
