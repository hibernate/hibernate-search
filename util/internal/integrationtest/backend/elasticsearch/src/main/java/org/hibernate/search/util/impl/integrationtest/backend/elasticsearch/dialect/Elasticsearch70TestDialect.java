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

import org.hibernate.search.backend.elasticsearch.client.impl.Paths;
import org.hibernate.search.backend.elasticsearch.util.spi.URLEncodedString;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class Elasticsearch70TestDialect implements ElasticsearchTestDialect {

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
	public List<String> getAllLocalDateDefaultMappingFormats() {
		return Collections.singletonList( "uuuu-MM-dd" );
	}

	@Override
	public void setTemplatePattern(JsonObject object, String pattern) {
		JsonArray array = new JsonArray();
		array.add( pattern );
		object.add( "index_patterns", array );
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
	public boolean hasBugForBigIntegerValuesForDynamicField() {
		return true;
	}
}
