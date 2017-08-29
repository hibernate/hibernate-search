/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.logging.impl;

import org.hibernate.search.elasticsearch.util.impl.JsonLogHelper;

import com.google.gson.JsonObject;

/**
 * Used with JBoss Logging's {@link org.jboss.logging.annotations.FormatWith}
 * to display {@link JsonObject}s in log messages.
 *
 * @author Yoann Rodiere
 */
public class ElasticsearchJsonObjectFormatter {

	private final String stringRepresentation;

	public ElasticsearchJsonObjectFormatter(JsonObject object) {
		this.stringRepresentation = JsonLogHelper.get().toString( object );
	}

	@Override
	public String toString() {
		return stringRepresentation;
	}
}
