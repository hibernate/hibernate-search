/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.client.impl;

import java.io.IOException;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.apache.http.HttpEntity;

/**
 * @author Yoann Rodiere
 */
public class ElasticsearchClientUtils {

	private ElasticsearchClientUtils() {
		// Private constructor
	}

	public static boolean isSuccessCode(int code) {
		return 200 <= code && code < 300;
	}

	public static HttpEntity toEntity(Gson gson, ElasticsearchRequest request) throws IOException {
		final List<JsonObject> bodyParts = request.getBodyParts();
		if ( bodyParts.isEmpty() ) {
			return null;
		}
		return new GsonHttpEntity( gson, bodyParts );
	}

}
