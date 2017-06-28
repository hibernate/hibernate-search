/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.util.impl;

import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.hibernate.search.elasticsearch.client.impl.ElasticsearchRequest;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

/**
 * @author Yoann Rodiere
 */
public class ElasticsearchClientUtils {

	private static final ContentType JSON_CONTENT_TYPE = ContentType.APPLICATION_JSON.withCharset("utf-8");

	private ElasticsearchClientUtils() {
		// Private constructor
	}

	public static boolean isSuccessCode(int code) {
		return 200 <= code && code < 300;
	}

	public static HttpEntity toEntity(Gson gson, ElasticsearchRequest request) {
		List<JsonObject> bodyParts = request.getBodyParts();
		if ( bodyParts.isEmpty() ) {
			return null;
		}
		StringBuilder builder = new StringBuilder();
		for ( JsonObject bodyPart : bodyParts ) {
			gson.toJson( bodyPart, builder );
			builder.append("\n");
		}
		return new StringEntity( builder.toString(), JSON_CONTENT_TYPE );
	}

}
