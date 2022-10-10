/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.dialect;

import static org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.dialect.ElasticsearchVersionUtils.isActualVersionBetweenIncluding;
import static org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.dialect.ElasticsearchVersionUtils.isActualVersionLess;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.hibernate.search.backend.elasticsearch.ElasticsearchVersion;
import org.hibernate.search.backend.elasticsearch.client.impl.Paths;
import org.hibernate.search.backend.elasticsearch.util.spi.URLEncodedString;

public class ElasticsearchTestDialectImpl implements ElasticsearchTestDialect {

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
	public List<String> getAllLocalDateDefaultMappingFormats() {
		if ( isActualVersionLess( ACTUAL_VERSION, "elastic:6.9.0" ) ) {
			return Arrays.asList( "yyyy-MM-dd", "yyyyyyyyy-MM-dd" );
		}
		return Collections.singletonList( "uuuu-MM-dd" );
	}

	@Override
	public boolean supportsIsWriteIndex() {
		if ( isActualVersionLess( ACTUAL_VERSION, "elastic:6.4.0" ) ) {
			return false;
		}
		return true;
	}
}
