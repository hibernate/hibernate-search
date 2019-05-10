/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.document.impl;

import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexSchemaFieldNode;
import org.hibernate.search.backend.elasticsearch.gson.impl.JsonAccessor;
import org.hibernate.search.engine.backend.document.IndexFieldReference;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;


public class ElasticsearchIndexFieldReference<F> implements IndexFieldReference<F> {

	private final JsonAccessor<JsonElement> relativeAccessor;

	private ElasticsearchIndexSchemaFieldNode<F> schemaNode;

	public ElasticsearchIndexFieldReference(JsonAccessor<JsonElement> relativeAccessor) {
		this.relativeAccessor = relativeAccessor;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[schemaNode=" + schemaNode + ", relativeAccessor=" + relativeAccessor + "]";
	}

	public void enable(ElasticsearchIndexSchemaFieldNode<F> schemaNode) {
		this.schemaNode = schemaNode;
	}

	boolean isEnabled() {
		return schemaNode != null;
	}

	ElasticsearchIndexSchemaFieldNode<F> getSchemaNode() {
		return schemaNode;
	}

	void addTo(JsonObject parent, F value) {
		relativeAccessor.add( parent, schemaNode.getCodec().encode( value ) );
	}

	boolean hasValueIn(JsonObject parent) {
		return relativeAccessor.hasExplicitValue( parent );
	}
}
