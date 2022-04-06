/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.dialect;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.hibernate.search.backend.elasticsearch.client.impl.Paths;
import org.hibernate.search.backend.elasticsearch.util.spi.URLEncodedString;
import org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.ElasticsearchTestHostConnectionConfiguration;

@SuppressWarnings("deprecation") // We use Paths.DOC on purpose
public class Elasticsearch68TestDialect extends Elasticsearch70TestDialect {

	@Override
	public boolean isEmptyMappingPossible() {
		return true;
	}

	@Override
	public URLEncodedString getTypeKeywordForNonMappingApi() {
		return Paths.DOC;
	}

	@Override
	public Optional<URLEncodedString> getTypeNameForMappingAndBulkApi() {
		return Optional.of( Paths.DOC );
	}

	@Override
	public Boolean getIncludeTypeNameParameterForMappingApi() {
		return true;
	}

	@Override
	public List<String> getAllLocalDateDefaultMappingFormats() {
		return Arrays.asList( "yyyy-MM-dd", "yyyyyyyyy-MM-dd" );
	}

	@Override
	public boolean zonedDateTimeDocValueHasUTCZoneId() {
		return true;
	}

	@Override
	public boolean hasBugForBigDecimalValuesForDynamicField() {
		// See https://hibernate.atlassian.net/browse/HSEARCH-4310,
		// https://hibernate.atlassian.net/browse/HSEARCH-4310
		return true;
	}

	@Override
	public boolean supportsSkipOrLimitingTotalHitCount() {
		return false;
	}

	@Override
	public boolean ignoresFieldSortWhenNestedFieldMissing() {
		// AWS apparently didn't apply this patch, which solves the problem in 6.8.1/7.1.2,
		// to their 6.8 branch:
		// https://github.com/elastic/elasticsearch/pull/42451
		return !ElasticsearchTestHostConnectionConfiguration.get().isAws()
				&& super.ignoresFieldSortWhenNestedFieldMissing();
	}
}
