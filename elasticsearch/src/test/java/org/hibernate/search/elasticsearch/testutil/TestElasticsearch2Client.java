/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.testutil;

import org.hibernate.search.elasticsearch.client.impl.ElasticsearchRequest;
import org.hibernate.search.elasticsearch.client.impl.ElasticsearchResponse;
import org.hibernate.search.elasticsearch.client.impl.URLEncodedString;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * @author Yoann Rodiere
 */
public class TestElasticsearch2Client extends TestElasticsearchClient {

	@Override
	protected JsonElement getDocumentField(URLEncodedString indexName, URLEncodedString typeName, URLEncodedString id, String fieldName) {
		ElasticsearchResponse response = performRequest( ElasticsearchRequest.get()
				.pathComponent( indexName ).pathComponent( typeName ).pathComponent( id )
				.param( "fields", fieldName )
				.build() );
		JsonObject result = response.getBody();
		return result.get( "fields" ).getAsJsonObject().get( fieldName );
	}

}
