/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.logging.impl;

import static org.hibernate.search.elasticsearch.util.impl.JsonLogHelper.prettyPrint;

import com.google.gson.JsonObject;

/**
 * Used with JBoss Logging's {@link org.jboss.logging.annotations.FormatWith}
 * to display iterables of {@link JsonObject}s in log messages.
 *
 * @author Yoann Rodiere
 */
public final class ElasticsearchJsonObjectIterableFormatter {

	private final String stringRepresentation;

	public ElasticsearchJsonObjectIterableFormatter(Iterable<JsonObject> objects) {
		this.stringRepresentation = prettyPrint( objects );
	}

	@Override
	public String toString() {
		return stringRepresentation;
	}
}
