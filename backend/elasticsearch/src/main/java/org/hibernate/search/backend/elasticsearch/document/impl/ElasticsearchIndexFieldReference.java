/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.document.impl;

import org.hibernate.search.engine.backend.document.spi.DocumentState;
import org.hibernate.search.engine.backend.document.spi.IndexFieldReference;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchFieldModel;
import org.hibernate.search.backend.elasticsearch.gson.impl.JsonAccessor;

import com.google.gson.JsonElement;


/**
 * @author Yoann Rodiere
 * @author Guillaume Smet
 */
public class ElasticsearchIndexFieldReference<T> implements IndexFieldReference<T> {

	private final JsonAccessor<JsonElement> accessor;

	private final ElasticsearchFieldModel model;

	public ElasticsearchIndexFieldReference(JsonAccessor<JsonElement> accessor,
			ElasticsearchFieldModel model) {
		this.accessor = accessor;
		this.model = model;
	}

	@Override
	public void add(DocumentState state, T value) {
		((ElasticsearchDocumentBuilder) state).add( accessor, model.getFormatter().format( value ) );
	}

}
