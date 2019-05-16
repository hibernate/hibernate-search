/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.elasticsearch.testsupport.dialect;

import java.util.List;
import java.util.Optional;

import org.hibernate.search.backend.elasticsearch.util.spi.URLEncodedString;

import com.google.gson.JsonObject;

public interface ElasticsearchTestDialect {

	boolean isEmptyMappingPossible();

	URLEncodedString getTypeKeywordForNonMappingApi();

	Optional<URLEncodedString> getTypeNameForMappingApi();

	Boolean getIncludeTypeNameParameterForMappingApi();

	List<String> getAllLocalDateDefaultMappingFormats();

	default String getFirstLocalDateDefaultMappingFormat() {
		return getAllLocalDateDefaultMappingFormats().get( 0 );
	}

	default String getConcatenatedLocalDateDefaultMappingFormats() {
		return String.join( "||", getAllLocalDateDefaultMappingFormats() );
	}

	void setTemplatePattern(JsonObject object, String pattern);

	default boolean isGeoPointIndexNullAsPossible() {
		return true;
	}

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
							+ " Did you properly set the appropriate elasticsearch-x.x profile?",
					e
			);
		}
	}

	static String getClusterVersion() {
		return System.getProperty( "org.hibernate.search.integrationtest.backend.elasticsearch.version" );
	}

	default boolean worksFineWithStrictGraterThanRangedQueriesOnScaledFloatField() {
		return true;
	}
}
