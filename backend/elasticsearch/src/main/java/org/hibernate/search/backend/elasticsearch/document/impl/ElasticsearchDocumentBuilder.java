/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.document.impl;

import org.hibernate.search.engine.backend.document.spi.DocumentState;
import org.hibernate.search.backend.elasticsearch.gson.impl.JsonAccessor;

import com.google.gson.JsonObject;

/**
 * @author Yoann Rodiere
 */
public class ElasticsearchDocumentBuilder implements DocumentState {

	private final JsonObject content = new JsonObject();

	public <T> void add(JsonAccessor<T> accessor, T value) {
		accessor.add( content, value );
	}

	public JsonObject build() {
		return content;
	}

}
