/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.dialect;

import static org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.dialect.ElasticsearchVersionUtils.isAtMost;
import static org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.dialect.ElasticsearchVersionUtils.isBetween;
import static org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.dialect.ElasticsearchVersionUtils.isLessThan;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.hibernate.search.backend.elasticsearch.ElasticsearchVersion;
import org.hibernate.search.backend.elasticsearch.client.impl.Paths;
import org.hibernate.search.backend.elasticsearch.util.spi.URLEncodedString;

public class ElasticsearchTestDialect {

	private static final ElasticsearchVersion ACTUAL_VERSION = ElasticsearchVersion.of(
			System.getProperty( "org.hibernate.search.integrationtest.backend.elasticsearch.version" )
	);

	private static final ElasticsearchTestDialect INSTANCE = new ElasticsearchTestDialect();

	public static ElasticsearchTestDialect get() {
		return INSTANCE;
	}

	public static ElasticsearchVersion getActualVersion() {
		return ACTUAL_VERSION;
	}

	public boolean isEmptyMappingPossible() {
		return isAtMost( ACTUAL_VERSION, "elastic:6.8" );
	}

	@SuppressWarnings("deprecation")
	public Optional<URLEncodedString> getTypeNameForMappingAndBulkApi() {
		if ( isAtMost( ACTUAL_VERSION, "elastic:6.8" ) ) {
			return Optional.of( Paths.DOC );
		}
		return Optional.empty();
	}

	public Boolean getIncludeTypeNameParameterForMappingApi() {
		if ( isBetween( ACTUAL_VERSION, "elastic:6.7", "elastic:6.8" ) ) {
			return Boolean.TRUE;
		}
		return null;
	}

	public List<String> getAllLocalDateDefaultMappingFormats() {
		if ( isAtMost( ACTUAL_VERSION, "elastic:6.8" ) ) {
			return Arrays.asList( "yyyy-MM-dd", "yyyyyyyyy-MM-dd" );
		}
		return Collections.singletonList( "uuuu-MM-dd" );
	}

	public boolean supportsIsWriteIndex() {
		return !isLessThan( ACTUAL_VERSION, "elastic:6.4.0" );
	}

	public String getFirstLocalDateDefaultMappingFormat() {
		return getAllLocalDateDefaultMappingFormats().get( 0 );
	}

	public String getConcatenatedLocalDateDefaultMappingFormats() {
		return String.join( "||", getAllLocalDateDefaultMappingFormats() );
	}
}
