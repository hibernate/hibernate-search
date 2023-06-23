/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.elasticsearch.schema.management;

import static org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.ElasticsearchIndexMetadataTestUtils.discriminatorMappingComplete;
import static org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.ElasticsearchIndexMetadataTestUtils.discriminatorMappingOmitDefaults;

import org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.ElasticsearchIndexMetadataTestUtils;
import org.hibernate.search.util.impl.integrationtest.common.reporting.FailureReportChecker;
import org.hibernate.search.util.impl.integrationtest.common.reporting.FailureReportUtils;

class ElasticsearchIndexSchemaManagerTestUtils {

	static final String STUB_CONTEXT_LITERAL = "Stub context";

	private ElasticsearchIndexSchemaManagerTestUtils() {
	}

	public static String simpleWriteAliasDefinition() {
		return simpleAliasDefinition( true, "" );
	}

	public static String simpleReadAliasDefinition() {
		return simpleAliasDefinition( false, "" );
	}

	public static String simpleAliasDefinition(boolean isWriteIndex, String otherAttributes) {
		return ElasticsearchIndexMetadataTestUtils.aliasDefinition( isWriteIndex, otherAttributes ).toString();
	}

	static String simpleMappingForInitialization(String properties) {
		return simpleMapping( "'_entity_type': " + discriminatorMappingComplete(), properties );
	}

	static String simpleMappingForExpectations(String properties) {
		return simpleMapping( "'_entity_type': " + discriminatorMappingOmitDefaults(), properties );
	}

	private static String simpleMapping(String metadataMapping, String otherProperties) {
		StringBuilder builder = new StringBuilder();
		builder.append( "{"
				+ "  'dynamic': 'strict',"
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
		return "'_entity_type': " + discriminatorMappingComplete().toString();
	}

	static String defaultMetadataMappingAndCommaForInitialization() {
		String mapping = defaultMetadataMappingForInitialization();
		return mapping.isEmpty() ? "" : mapping + ", ";
	}

	static String defaultMetadataMappingForExpectations() {
		return "'_entity_type': " + discriminatorMappingOmitDefaults().toString();
	}

	static String defaultMetadataMappingAndCommaForExpectations() {
		String mapping = defaultMetadataMappingForExpectations();
		return mapping.isEmpty() ? "" : mapping + ", ";
	}

	static FailureReportChecker hasValidationFailureReport() {
		return FailureReportUtils.hasFailureReport()
				.contextLiteral( STUB_CONTEXT_LITERAL )
				.failure( "Validation of the existing index in the Elasticsearch cluster failed. See below for details." );
	}
}
