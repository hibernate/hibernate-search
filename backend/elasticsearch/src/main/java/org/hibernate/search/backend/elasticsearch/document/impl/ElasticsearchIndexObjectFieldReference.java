/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.document.impl;

import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexSchemaObjectNode;
import org.hibernate.search.backend.elasticsearch.gson.impl.JsonAccessor;
import org.hibernate.search.engine.backend.document.IndexObjectFieldReference;

import com.google.gson.JsonObject;


public class ElasticsearchIndexObjectFieldReference implements IndexObjectFieldReference {

	private final JsonAccessor<JsonObject> relativeAccessor;

	private ElasticsearchIndexSchemaObjectNode schemaNode;

	public ElasticsearchIndexObjectFieldReference(JsonAccessor<JsonObject> relativeAccessor) {
		this.relativeAccessor = relativeAccessor;
	}

	public void enable(ElasticsearchIndexSchemaObjectNode schemaNode) {
		this.schemaNode = schemaNode;
	}

	boolean isEnabled() {
		return schemaNode != null;
	}

	ElasticsearchIndexSchemaObjectNode getSchemaNode() {
		return schemaNode;
	}

	void addTo(JsonObject parent, JsonObject value) {
		relativeAccessor.add( parent, value );
	}

	boolean hasValueIn(JsonObject parent) {
		return relativeAccessor.hasExplicitValue( parent );
	}
}
