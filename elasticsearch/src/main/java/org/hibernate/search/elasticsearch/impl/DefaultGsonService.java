/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.impl;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import io.searchbox.client.AbstractJestClient;

/**
 * @author Guillaume Smet
 */
public class DefaultGsonService implements GsonService {

	private static GsonBuilder builderBase() {
		return new GsonBuilder()
				.setDateFormat( AbstractJestClient.ELASTIC_SEARCH_DATE_FORMAT );
	}

	// TODO find out and document why null serialization needs to be turned on...
	private final Gson gson = builderBase()
			.serializeNulls()
			.create();

	private final Gson gsonNoSerializeNulls = builderBase()
			.create();

	public Gson getGson() {
		return gson;
	}

	@Override
	public Gson getGsonNoSerializeNulls() {
		return gsonNoSerializeNulls;
	}

}
