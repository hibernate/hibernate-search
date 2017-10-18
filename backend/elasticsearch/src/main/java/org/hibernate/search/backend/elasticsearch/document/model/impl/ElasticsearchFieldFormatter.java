/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.document.model.impl;

import com.google.gson.JsonElement;

/**
 * @author Yoann Rodiere
 */
public interface ElasticsearchFieldFormatter {

	JsonElement format(Object object);

	Object parse(JsonElement element);

	/**
	 * @param obj An object to compare
	 * @return {@code true} if {@code obj} is a formatter whose {@link #format(Object)} method is
	 * guaranteed to always return the exact same output value as this formatter for any input value,
	 * {@code false} otherwise.
	 */
	@Override
	boolean equals(Object obj);

	@Override
	int hashCode();

}
