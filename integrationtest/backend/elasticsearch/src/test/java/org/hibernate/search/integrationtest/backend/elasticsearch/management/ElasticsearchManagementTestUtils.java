/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.elasticsearch.management;

class ElasticsearchManagementTestUtils {

	private ElasticsearchManagementTestUtils() {
	}

	static String simpleMappingForInitialization(String properties) {
		// TODO HSEARCH-3765 add type metadata
		return simpleMapping( "", properties );
	}

	static String simpleMappingForExpectations(String properties) {
		// TODO HSEARCH-3765 add type metadata
		return simpleMapping( "", properties );
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
		// TODO HSEARCH-3765 add type metadata
		return "";
	}

	static String defaultMetadataMappingAndCommaForInitialization() {
		String mapping = defaultMetadataMappingForInitialization();
		return mapping.isEmpty() ? "" : mapping + ", ";
	}
}
