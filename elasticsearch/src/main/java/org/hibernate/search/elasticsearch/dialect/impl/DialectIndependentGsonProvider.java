/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.dialect.impl;

import org.hibernate.search.elasticsearch.gson.impl.GsonProvider;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * @author Guillaume Smet
 */
public class DialectIndependentGsonProvider implements GsonProvider {

	private static final String ELASTIC_SEARCH_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ssZ";

	public static final DialectIndependentGsonProvider INSTANCE = new DialectIndependentGsonProvider();

	private DialectIndependentGsonProvider() {
		// Use INSTANCE
	}

	private static GsonBuilder builderBase() {
		return new GsonBuilder().setDateFormat( ELASTIC_SEARCH_DATE_FORMAT );
	}

	// Null serialization needs to be enabled to index null fields
	private final Gson gson = builderBase()
			.serializeNulls()
			.create();

	private final Gson gsonPrettyPrinting = builderBase()
			.setPrettyPrinting()
			.create();

	private final Gson gsonNoSerializeNulls = builderBase()
			.create();

	@Override
	public Gson getGson() {
		return gson;
	}

	@Override
	public Gson getGsonPrettyPrinting() {
		return gsonPrettyPrinting;
	}

	@Override
	public Gson getGsonNoSerializeNulls() {
		return gsonNoSerializeNulls;
	}

}
