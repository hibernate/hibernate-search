/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.elasticsearch.mapping;

import com.google.gson.JsonObject;

class ElasticsearchTypeNameMappingTestUtils {

	private ElasticsearchTypeNameMappingTestUtils() {
	}

	public static JsonObject mappingWithoutAnyProperty() {
		JsonObject result = new JsonObject();
		result.addProperty( "dynamic", "strict" );
		return result;
	}

	public static JsonObject mappingWithDiscriminatorProperty(String propertyName) {
		JsonObject result = new JsonObject();
		result.addProperty( "dynamic", "strict" );

		JsonObject properties = new JsonObject();
		result.add( "properties", properties );

		properties.add( propertyName, discriminatorMapping() );

		return result;
	}

	private static JsonObject discriminatorMapping() {
		JsonObject discriminatorMapping = new JsonObject();
		discriminatorMapping.addProperty( "type", "keyword" );
		discriminatorMapping.addProperty( "index", false );
		discriminatorMapping.addProperty( "store", false );
		discriminatorMapping.addProperty( "doc_values", true );
		return discriminatorMapping;
	}

}
