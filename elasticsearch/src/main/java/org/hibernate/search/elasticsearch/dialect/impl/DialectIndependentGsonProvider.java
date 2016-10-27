/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.dialect.impl;

import org.hibernate.search.elasticsearch.gson.impl.DefaultGsonProvider;
import org.hibernate.search.elasticsearch.gson.impl.GsonProvider;

import com.google.gson.GsonBuilder;

/**
 * @author Guillaume Smet
 */
public class DialectIndependentGsonProvider {

	public static final GsonProvider INSTANCE =
			DefaultGsonProvider.create( () -> { return new GsonBuilder(); } );

	private DialectIndependentGsonProvider() {
		// Use INSTANCE
	}

}
