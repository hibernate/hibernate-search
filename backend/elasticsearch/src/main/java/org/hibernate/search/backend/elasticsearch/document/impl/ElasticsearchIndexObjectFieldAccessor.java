/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.document.impl;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexObjectFieldAccessor;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexSchemaObjectNode;
import org.hibernate.search.backend.elasticsearch.gson.impl.JsonAccessor;

import com.google.gson.JsonObject;


/**
 * @author Yoann Rodiere
 */
public class ElasticsearchIndexObjectFieldAccessor implements IndexObjectFieldAccessor {

	private final JsonAccessor<JsonObject> relativeAccessor;

	private final ElasticsearchIndexSchemaObjectNode node;

	public ElasticsearchIndexObjectFieldAccessor(JsonAccessor<JsonObject> relativeAccessor,
			ElasticsearchIndexSchemaObjectNode node) {
		this.relativeAccessor = relativeAccessor;
		this.node = node;
	}

	@Override
	public DocumentElement add(DocumentElement target) {
		JsonObject jsonObject = new JsonObject();
		((ElasticsearchDocumentObjectBuilder) target).add( node.getParent(), relativeAccessor, jsonObject );
		return new ElasticsearchDocumentObjectBuilder( node, jsonObject );
	}

	@Override
	public void addMissing(DocumentElement target) {
		((ElasticsearchDocumentObjectBuilder) target).add( node.getParent(), relativeAccessor, null );
	}
}
