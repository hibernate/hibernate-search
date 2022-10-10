/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.dialect;

import static org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.dialect.ElasticsearchVersionUtils.isActualVersionBetween;
import static org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.dialect.ElasticsearchVersionUtils.isActualVersionBetweenIncluding;
import static org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.dialect.ElasticsearchVersionUtils.isActualVersionEquals;
import static org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.dialect.ElasticsearchVersionUtils.isActualVersionLess;
import static org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.dialect.ElasticsearchVersionUtils.isActualVersionLessOrEquals;
import static org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.dialect.ElasticsearchVersionUtils.isOpensearchDistribution;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.hibernate.search.backend.elasticsearch.ElasticsearchVersion;
import org.hibernate.search.backend.elasticsearch.client.impl.Paths;
import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchRequest;
import org.hibernate.search.backend.elasticsearch.util.spi.URLEncodedString;
import org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.ElasticsearchTestHostConnectionConfiguration;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class ElasticsearchTestDialectImpl implements ElasticsearchTestDialect {

	private static final URLEncodedString PATH_TEMPLATE = URLEncodedString.fromString( "_template" );
	private static final URLEncodedString PATH_INDEX_TEMPLATE = URLEncodedString.fromString( "_index_template" );

	private static final ElasticsearchVersion ACTUAL_VERSION = ElasticsearchTestDialect.getActualVersion();

	@Override
	public boolean isEmptyMappingPossible() {
		if ( isActualVersionLess( ACTUAL_VERSION, "elastic:6.9.0" ) ) {
			return true;
		}
		return false;
	}

	@Override
	@SuppressWarnings("deprecation")
	public URLEncodedString getTypeKeywordForNonMappingApi() {
		if ( isActualVersionLess( ACTUAL_VERSION, "elastic:6.9.0" ) ) {
			return Paths.DOC;
		}
		return Paths._DOC;
	}

	@Override
	@SuppressWarnings("deprecation")
	public Optional<URLEncodedString> getTypeNameForMappingAndBulkApi() {
		if ( isActualVersionLess( ACTUAL_VERSION, "elastic:6.9.0" ) ) {
			return Optional.of( Paths.DOC );
		}
		return Optional.empty();
	}

	@Override
	public Boolean getIncludeTypeNameParameterForMappingApi() {
		if ( isActualVersionBetweenIncluding( ACTUAL_VERSION, "elastic:6.4.0", "elastic:6.9.0" ) ) {
			return Boolean.TRUE;
		}
		return null;
	}

	@Override
	public ElasticsearchRequest createTemplatePutRequest(String templateName, String pattern, int priority,
			JsonObject settings) {
		if ( isActualVersionLess( ACTUAL_VERSION, "elastic:7.8.0" ) ) {
			return createLegacyTemplatePutRequest( templateName, pattern, priority, settings );
		}
		if ( ElasticsearchTestHostConnectionConfiguration.get().isAws() ) {
			// New template APIs are not allowed on AWS (getting 401 'Unauthorized')
			return createLegacyTemplatePutRequest( templateName, pattern, priority, settings );
		}
		JsonObject source = new JsonObject();

		JsonArray indexPatterns = new JsonArray();
		indexPatterns.add( pattern );
		source.add( "index_patterns", indexPatterns );

		source.addProperty( "priority", priority );

		JsonObject template = new JsonObject();
		source.add( "template", template );
		template.add( "settings", settings );

		return ElasticsearchRequest.put()
				.pathComponent( PATH_INDEX_TEMPLATE ).pathComponent( URLEncodedString.fromString( templateName ) )
				.body( source )
				.build();
	}

	protected ElasticsearchRequest createLegacyTemplatePutRequest(String templateName, String pattern, int priority,
			JsonObject settings) {
		JsonObject source = new JsonObject();

		setLegacyTemplatePattern( source, pattern );

		source.addProperty( "order", priority );
		source.add( "settings", settings );

		ElasticsearchRequest.Builder builder = ElasticsearchRequest.put()
				.pathComponent( PATH_TEMPLATE ).pathComponent( URLEncodedString.fromString( templateName ) )
				.body( source );

		Boolean includeTypeName = getIncludeTypeNameParameterForMappingApi();
		if ( includeTypeName != null ) {
			builder.param( "include_type_name", includeTypeName );
		}

		return builder.build();
	}

	@Override
	public ElasticsearchRequest createTemplateDeleteRequest(String templateName) {
		if ( isActualVersionLess( ACTUAL_VERSION, "elastic:7.8.0" ) ) {
			return createLegacyTemplateDeleteRequest( templateName );
		}
		if ( ElasticsearchTestHostConnectionConfiguration.get().isAws() ) {
			// New template APIs are not allowed on AWS (getting 401 'Unauthorized')
			return createLegacyTemplateDeleteRequest( templateName );
		}
		return ElasticsearchRequest.delete()
				.pathComponent( PATH_INDEX_TEMPLATE ).pathComponent( URLEncodedString.fromString( templateName ) )
				.build();
	}

	protected ElasticsearchRequest createLegacyTemplateDeleteRequest(String templateName) {
		return ElasticsearchRequest.delete()
				.pathComponent( PATH_TEMPLATE ).pathComponent( URLEncodedString.fromString( templateName ) )
				.build();
	}

	protected void setLegacyTemplatePattern(JsonObject object, String pattern) {
		JsonArray indexPatterns = new JsonArray();
		indexPatterns.add( pattern );
		object.add( "index_patterns", indexPatterns );
	}

	@Override
	public List<String> getAllLocalDateDefaultMappingFormats() {
		if ( isActualVersionLess( ACTUAL_VERSION, "elastic:6.9.0" ) ) {
			return Arrays.asList( "yyyy-MM-dd", "yyyyyyyyy-MM-dd" );
		}
		return Collections.singletonList( "uuuu-MM-dd" );
	}

	@Override
	public boolean supportsGeoPointIndexNullAs() {
		if ( isActualVersionLess( ACTUAL_VERSION, "elastic:6.3.0" ) ) {
			return false;
		}
		return true;
	}

	@Override
	public boolean supportsStrictGreaterThanRangedQueriesOnScaledFloatField() {
		if ( isActualVersionLess( ACTUAL_VERSION, "elastic:6.0.0" ) ) {
			return false;
		}
		return true;
	}

	@Override
	public boolean zonedDateTimeDocValueHasUTCZoneId() {
		if ( isActualVersionLess( ACTUAL_VERSION, "elastic:6.9.0" ) ) {
			return true;
		}
		return false;
	}

	@Override
	public boolean supportsIsWriteIndex() {
		if ( isActualVersionLess( ACTUAL_VERSION, "elastic:6.4.0" ) ) {
			return false;
		}
		return true;
	}

	@Override
	public boolean hasBugForSortMaxOnNegativeFloats() {
		if ( isActualVersionLess( ACTUAL_VERSION, "elastic:6.0.0" ) ) {
			return true;
		}
		return false;
	}

	@Override
	public boolean hasBugForBigIntegerValuesForDynamicField() {
		if ( isActualVersionLess( ACTUAL_VERSION, "elastic:7.3.0" ) ) {
			return true;
		}
		return false;
	}

	@Override
	public boolean hasBugForBigDecimalValuesForDynamicField() {
		// See https://hibernate.atlassian.net/browse/HSEARCH-4310,
		// https://hibernate.atlassian.net/browse/HSEARCH-4310
		if ( isActualVersionLess( ACTUAL_VERSION, "elastic:6.9.0" ) ) {
			return true;
		}
		return false;
	}

	@Override
	public boolean normalizesStringArgumentToWildcardPredicateForAnalyzedStringField() {
		// In ES 7.7 through 7.11, wildcard predicates on analyzed fields get their pattern normalized,
		// but that was deemed a bug and fixed in 7.12.2+: https://github.com/elastic/elasticsearch/pull/53127
		if ( isActualVersionBetweenIncluding( ACTUAL_VERSION, "elastic:7.7", "elastic:7.12.1" ) ||
				isOpensearchDistribution( ACTUAL_VERSION ) ) {
			return true;
		}
		return false;
	}

	@Override
	public boolean supportsSkipOrLimitingTotalHitCount() {
		if ( isActualVersionLess( ACTUAL_VERSION, "elastic:6.9.0" ) ) {
			return false;
		}
		return true;
	}

	@Override
	public boolean hasBugForExistsOnNullGeoPointFieldWithoutDocValues() {
		if ( isActualVersionLess( ACTUAL_VERSION, "elastic:7.9.0" ) ) {
			return false;
		}
		return true;
	}

	@Override
	public boolean supportMoreThan1024Terms() {
		if ( isActualVersionLess( ACTUAL_VERSION, "elastic:6.0.0" ) ) {
			return false;
		}
		return true;
	}

	@Override
	public boolean supportsIgnoreUnmappedForGeoPointField() {
		// Support for ignore_unmapped in geo_distance sorts added in 6.4:
		// https://github.com/elastic/elasticsearch/pull/31153
		// In 6.3 and below, we just can't ignore unmapped fields,
		// which means sorts will fail when the geo_point field is not present in all indexes.
		if ( isActualVersionLess( ACTUAL_VERSION, "elastic:6.4.0" ) ) {
			return false;
		}
		return true;
	}

	@Override
	public boolean ignoresFieldSortWhenNestedFieldMissing() {
		// Support for ignoring field sorts when a nested field is missing was added in 6.8.1/7.1.2:
		// https://github.com/elastic/elasticsearch/pull/42451
		// In 6.8.0 and below, we just can't ignore unmapped nested fields in field sorts,
		// which means sorts will fail when the nested field is not present in all indexes.
		// -----------------------------------------------------------------------------------------
		// AWS apparently didn't apply this patch, which solves the problem in 6.8.1/7.1.2,
		// to their 6.8 branch:
		// https://github.com/elastic/elasticsearch/pull/42451

		if ( isActualVersionLessOrEquals( ACTUAL_VERSION, "elastic:6.7.0" ) ||
				ElasticsearchTestHostConnectionConfiguration.get().isAws() &&
						isActualVersionEquals( ACTUAL_VERSION, "elastic:6.8" ) ) {
			return false;
		}
		return true;
	}

	@Override
	public boolean hasBugForDateFormattedAsYear() {
		// https://github.com/elastic/elasticsearch/issues/90187
		// Seems like this was fixed in 8.5.1 and they won't backport to 8.4:
		// https://github.com/elastic/elasticsearch/pull/90458
		if ( isActualVersionBetween( ACTUAL_VERSION, "elastic:8.4.1", "elastic:8.5.1" ) ) {
			// TODO: should we just go with isActualVersionOneOf( ACTUAL_VERSION, "elastic:8.4.2", "elastic:8.4.3") ?
			return true;
		}
		return false;
	}

}
