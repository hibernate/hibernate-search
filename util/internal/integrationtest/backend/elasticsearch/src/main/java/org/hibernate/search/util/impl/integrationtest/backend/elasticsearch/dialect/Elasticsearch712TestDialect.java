/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.dialect;

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

public class Elasticsearch712TestDialect implements ElasticsearchTestDialect {

	private static final URLEncodedString PATH_TEMPLATE = URLEncodedString.fromString( "_template" );
	private static final URLEncodedString PATH_INDEX_TEMPLATE = URLEncodedString.fromString( "_index_template" );

	@Override
	public boolean isEmptyMappingPossible() {
		return false;
	}

	@Override
	public URLEncodedString getTypeKeywordForNonMappingApi() {
		return Paths._DOC;
	}

	@Override
	public Optional<URLEncodedString> getTypeNameForMappingAndBulkApi() {
		return Optional.empty();
	}

	@Override
	public Boolean getIncludeTypeNameParameterForMappingApi() {
		return null;
	}

	@Override
	public ElasticsearchRequest createTemplatePutRequest(String templateName, String pattern, int priority,
			JsonObject settings) {
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
		return Collections.singletonList( "uuuu-MM-dd" );
	}

	@Override
	public boolean supportsGeoPointIndexNullAs() {
		return true;
	}

	@Override
	public boolean supportsStrictGreaterThanRangedQueriesOnScaledFloatField() {
		return true;
	}

	@Override
	public boolean zonedDateTimeDocValueHasUTCZoneId() {
		return false;
	}

	@Override
	public boolean supportsIsWriteIndex() {
		return true;
	}

	@Override
	public boolean hasBugForSortMaxOnNegativeFloats() {
		return false;
	}

	@Override
	public boolean hasBugForBigIntegerValuesForDynamicField() {
		return false;
	}

	@Override
	public boolean hasBugForBigDecimalValuesForDynamicField() {
		return false;
	}

	@Override
	public boolean normalizesStringArgumentToWildcardPredicateForAnalyzedStringField() {
		// In ES 7.7 through 7.11, wildcard predicates on analyzed fields get their pattern normalized,
		// but that was deemed a bug and fixed in 7.12.2+: https://github.com/elastic/elasticsearch/pull/53127
		return false;
	}

	@Override
	public boolean supportsSkipOrLimitingTotalHitCount() {
		return true;
	}

	@Override
	public boolean hasBugForExistsOnNullGeoPointFieldWithoutDocValues() {
		return true;
	}

	@Override
	public boolean supportMoreThan1024Terms() {
		return true;
	}

	@Override
	public boolean supportsIgnoreUnmappedForGeoPointField() {
		return true;
	}

	@Override
	public boolean ignoresFieldSortWhenNestedFieldMissing() {
		return true;
	}

	@Override
	public boolean hasBugForDateFormattedAsYear() {
		// https://github.com/elastic/elasticsearch/issues/90187
		// Seems like this was fixed in 8.5.1 and they won't backport to 8.4:
		// https://github.com/elastic/elasticsearch/pull/90458
		return ElasticsearchVersion.of( "elastic:8.4.2" ).matches( ElasticsearchTestDialect.getActualVersion() )
				|| ElasticsearchVersion.of( "elastic:8.4.3" ).matches( ElasticsearchTestDialect.getActualVersion() );
	}
}
