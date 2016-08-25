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

	private final Gson gson = new GsonBuilder()
			.setDateFormat( AbstractJestClient.ELASTIC_SEARCH_DATE_FORMAT )
			.serializeNulls()
			.create();

	public Gson getGson() {
		return gson;
	}

}
