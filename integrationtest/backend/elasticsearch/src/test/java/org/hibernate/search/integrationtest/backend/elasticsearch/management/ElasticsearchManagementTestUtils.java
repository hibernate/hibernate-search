/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.elasticsearch.management;

import static org.hibernate.search.integrationtest.backend.elasticsearch.mapping.ElasticsearchTypeNameMappingTestUtils.discriminatorMappingComplete;
import static org.hibernate.search.integrationtest.backend.elasticsearch.mapping.ElasticsearchTypeNameMappingTestUtils.discriminatorMappingOmitDefaults;

class ElasticsearchManagementTestUtils {

	private ElasticsearchManagementTestUtils() {
	}

	static String simpleMappingForInitialization(String properties) {
		return simpleMapping( "'__HSEARCH_type': " + discriminatorMappingComplete(), properties );
	}

	static String simpleMappingForExpectations(String properties) {
		return simpleMapping( "'__HSEARCH_type': " + discriminatorMappingOmitDefaults(), properties );
	}

	private static String simpleMapping(String metadataMapping, String otherProperties) {
		StringBuilder builder = new StringBuilder();
		builder.append( "{"
				+ "'dynamic': 'strict',"
				+ "'properties': {" );
		builder.append( metadataMapping );
		if ( !metadataMapping.isEmpty() && !otherProperties.isEmpty() ) {
			builder.append( "," );
		}
		builder.append( otherProperties );
		builder.append( "}"
				+ "}" );
		return builder.toString();
	}

	static String defaultMetadataMappingForInitialization() {
		return "'__HSEARCH_type': " + discriminatorMappingComplete().toString();
	}

	static String defaultMetadataMappingAndCommaForInitialization() {
		String mapping = defaultMetadataMappingForInitialization();
		return mapping.isEmpty() ? "" : mapping + ", ";
	}
}
