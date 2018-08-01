/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.document.impl;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldAccessor;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexSchemaFieldNode;
import org.hibernate.search.backend.elasticsearch.gson.impl.JsonAccessor;

import com.google.gson.JsonElement;


/**
 * @author Yoann Rodiere
 * @author Guillaume Smet
 */
public class ElasticsearchIndexFieldAccessor<F> implements IndexFieldAccessor<F> {

	private final JsonAccessor<JsonElement> accessor;

	private final ElasticsearchIndexSchemaFieldNode<F> schemaNode;

	public ElasticsearchIndexFieldAccessor(JsonAccessor<JsonElement> accessor,
			ElasticsearchIndexSchemaFieldNode<F> schemaNode) {
		this.accessor = accessor;
		this.schemaNode = schemaNode;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[schemaNode=" + schemaNode + ", accessor=" + accessor + "]";
	}

	@Override
	public void write(DocumentElement target, F value) {
		ElasticsearchDocumentObjectBuilder builder = (ElasticsearchDocumentObjectBuilder) target;
		builder.checkTreeConsistency( schemaNode.getParent() );
		builder.add( accessor, schemaNode.getCodec().encode( value ) );
	}

}
