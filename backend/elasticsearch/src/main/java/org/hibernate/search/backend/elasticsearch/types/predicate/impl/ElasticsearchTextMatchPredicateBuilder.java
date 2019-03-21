/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.types.predicate.impl;

import org.hibernate.search.backend.elasticsearch.gson.impl.JsonAccessor;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchConverterCompatibilityChecker;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchContext;
import org.hibernate.search.backend.elasticsearch.search.predicate.impl.ElasticsearchSearchPredicateContext;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchFieldCodec;
import org.hibernate.search.engine.backend.types.converter.ToDocumentFieldValueConverter;

import com.google.gson.JsonObject;

class ElasticsearchTextMatchPredicateBuilder extends ElasticsearchStandardMatchPredicateBuilder<String> {

	private static final JsonAccessor<Integer> FUZZINESS_ACCESSOR = JsonAccessor.root().property( "fuzziness" ).asInteger();
	private static final JsonAccessor<Integer> PREFIX_LENGTH_ACCESSOR = JsonAccessor.root().property( "prefix_length" ).asInteger();

	private Integer fuzziness;
	private Integer prefixLength;

	ElasticsearchTextMatchPredicateBuilder(
			ElasticsearchSearchContext searchContext,
			String absoluteFieldPath,
			ToDocumentFieldValueConverter<?, ? extends String> converter, ToDocumentFieldValueConverter<String, ? extends String> rawConverter,
			ElasticsearchConverterCompatibilityChecker converterChecker, ElasticsearchFieldCodec<String> codec) {
		super( searchContext, absoluteFieldPath, converter, rawConverter, converterChecker, codec );
	}

	@Override
	public void fuzzy(int maxEditDistance, int exactPrefixLength) {
		this.fuzziness = maxEditDistance;
		this.prefixLength = exactPrefixLength;
	}

	@Override
	protected JsonObject doBuild(ElasticsearchSearchPredicateContext context, JsonObject outerObject,
			JsonObject innerObject) {
		if ( fuzziness != null ) {
			FUZZINESS_ACCESSOR.set( innerObject, fuzziness );
		}
		if ( prefixLength != null ) {
			PREFIX_LENGTH_ACCESSOR.set( innerObject, prefixLength );
		}
		return super.doBuild( context, outerObject, innerObject );
	}
}
