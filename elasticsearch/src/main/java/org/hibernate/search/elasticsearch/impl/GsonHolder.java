/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.impl;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;

import io.searchbox.client.AbstractJestClient;

/**
 * Centralizes the configuration of the Gson object.
 *
 * @author Guillaume Smet
 */
public class GsonHolder {

	private GsonHolder() {
	}

	public static final Gson GSON = new GsonBuilder()
			.setDateFormat( AbstractJestClient.ELASTIC_SEARCH_DATE_FORMAT )
			.serializeNulls()
			.create();

	public static final JsonParser PARSER = new JsonParser();

}
