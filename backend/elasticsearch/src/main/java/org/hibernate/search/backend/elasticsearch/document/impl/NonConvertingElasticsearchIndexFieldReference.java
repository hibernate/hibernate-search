/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.document.impl;

import org.hibernate.search.backend.elasticsearch.gson.impl.JsonAccessor;


/**
 * @author Yoann Rodiere
 */
public final class NonConvertingElasticsearchIndexFieldReference<T> extends ElasticsearchIndexFieldReference<T, T> {

	public NonConvertingElasticsearchIndexFieldReference(JsonAccessor<T> accessor) {
		super( accessor );
	}

	@Override
	protected T convert(T value) {
		return value;
	}

}
