/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.dialect;

import java.util.List;
import java.util.Optional;

import org.hibernate.search.backend.elasticsearch.ElasticsearchVersion;
import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchRequest;
import org.hibernate.search.backend.elasticsearch.util.spi.URLEncodedString;

import com.google.gson.JsonObject;

public interface ElasticsearchTestDialect {

	static ElasticsearchTestDialect get() {
		String dialectClassName = System.getProperty( "org.hibernate.search.integrationtest.backend.elasticsearch.testdialect" );
		try {
			@SuppressWarnings("unchecked")
			Class<? extends ElasticsearchTestDialect> dialectClass =
					(Class<? extends ElasticsearchTestDialect>) Class.forName( dialectClassName );
			return dialectClass.getConstructor().newInstance();
		}
		catch (Exception | LinkageError e) {
			throw new IllegalStateException(
					"Unexpected error while initializing the ElasticsearchTestDialect with name '" + dialectClassName + "'."
							+ " Did you properly set the appropriate elasticsearch-x.x/opensearch-x.x profile?",
					e
			);
		}
	}

	static ElasticsearchVersion getActualVersion() {
		return ElasticsearchVersion.of( System.getProperty( "org.hibernate.search.integrationtest.backend.elasticsearch.version" ) );
	}

	boolean isEmptyMappingPossible();

	URLEncodedString getTypeKeywordForNonMappingApi();

	Optional<URLEncodedString> getTypeNameForMappingAndBulkApi();

	Boolean getIncludeTypeNameParameterForMappingApi();

	ElasticsearchRequest createTemplatePutRequest(String templateName, String pattern, int priority, JsonObject settings);

	ElasticsearchRequest createTemplateDeleteRequest(String templateName);

	List<String> getAllLocalDateDefaultMappingFormats();

	default String getFirstLocalDateDefaultMappingFormat() {
		return getAllLocalDateDefaultMappingFormats().get( 0 );
	}

	default String getConcatenatedLocalDateDefaultMappingFormats() {
		return String.join( "||", getAllLocalDateDefaultMappingFormats() );
	}

	boolean supportsGeoPointIndexNullAs();

	boolean supportsStrictGreaterThanRangedQueriesOnScaledFloatField();

	boolean zonedDateTimeDocValueHasUTCZoneId();

	boolean supportsIsWriteIndex();

	boolean hasBugForSortMaxOnNegativeFloats();

	boolean hasBugForBigIntegerValuesForDynamicField();

	boolean hasBugForBigDecimalValuesForDynamicField();

	boolean normalizesStringArgumentToWildcardPredicateForAnalyzedStringField();

	boolean supportsSkipOrLimitingTotalHitCount();

	boolean hasBugForExistsOnNullGeoPointFieldWithoutDocValues();

	boolean supportMoreThan1024Terms();

	boolean supportsIgnoreUnmappedForGeoPointField();

	boolean ignoresFieldSortWhenNestedFieldMissing();

	boolean hasBugForDateFormattedAsYear();

}
