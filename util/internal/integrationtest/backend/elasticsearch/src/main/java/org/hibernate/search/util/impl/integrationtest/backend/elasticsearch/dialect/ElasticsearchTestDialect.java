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
import org.hibernate.search.backend.elasticsearch.util.spi.URLEncodedString;

public interface ElasticsearchTestDialect {

	static ElasticsearchTestDialect get() {
		return new ElasticsearchTestDialectImpl();
	}

	static ElasticsearchVersion getActualVersion() {
		return ElasticsearchVersion.of( System.getProperty( "org.hibernate.search.integrationtest.backend.elasticsearch.version" ) );
	}

	boolean isEmptyMappingPossible();

	Optional<URLEncodedString> getTypeNameForMappingAndBulkApi();

	Boolean getIncludeTypeNameParameterForMappingApi();

	List<String> getAllLocalDateDefaultMappingFormats();

	default String getFirstLocalDateDefaultMappingFormat() {
		return getAllLocalDateDefaultMappingFormats().get( 0 );
	}

	default String getConcatenatedLocalDateDefaultMappingFormats() {
		return String.join( "||", getAllLocalDateDefaultMappingFormats() );
	}

	boolean supportsIsWriteIndex();

}
