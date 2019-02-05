/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.predicate.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.backend.elasticsearch.gson.impl.JsonAccessor;
import org.hibernate.search.backend.elasticsearch.gson.impl.JsonObjectAccessor;
import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchContext;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchFieldCodec;
import org.hibernate.search.engine.backend.types.converter.ToDocumentFieldValueConverter;
import org.hibernate.search.engine.logging.spi.EventContexts;
import org.hibernate.search.engine.search.predicate.spi.MatchPredicateBuilder;
import org.hibernate.search.util.impl.common.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * @author Yoann Rodiere
 */
public class ElasticsearchMatchPredicateBuilder<F> extends AbstractElasticsearchSearchPredicateBuilder
		implements MatchPredicateBuilder<ElasticsearchSearchPredicateBuilder> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private static final JsonAccessor<JsonElement> QUERY_ACCESSOR = JsonAccessor.root().property( "query" );

	private static final JsonObjectAccessor MATCH_ACCESSOR = JsonAccessor.root().property( "match" ).asObject();

	private final ElasticsearchSearchContext searchContext;

	private final String absoluteFieldPath;
	private final ToDocumentFieldValueConverter<?, ? extends F> dslToIndexConverter;
	private final ElasticsearchFieldCodec<F> codec;

	private JsonElement value;

	public ElasticsearchMatchPredicateBuilder(ElasticsearchSearchContext searchContext,
			String absoluteFieldPath,
			ToDocumentFieldValueConverter<?, ? extends F> dslToIndexConverter,
			ElasticsearchFieldCodec<F> codec) {
		this.searchContext = searchContext;
		this.absoluteFieldPath = absoluteFieldPath;
		this.dslToIndexConverter = dslToIndexConverter;
		this.codec = codec;
	}

	@Override
	public void value(Object value) {
		try {
			F converted = dslToIndexConverter.convertUnknown( value, searchContext.getToDocumentFieldValueConvertContext() );
			this.value = codec.encode( converted );
		}
		catch (RuntimeException e) {
			throw log.cannotConvertDslParameter(
					e.getMessage(), e, EventContexts.fromIndexFieldAbsolutePath( absoluteFieldPath )
			);
		}
	}

	@Override
	protected JsonObject doBuild(ElasticsearchSearchPredicateContext context,
			JsonObject outerObject, JsonObject innerObject) {
		QUERY_ACCESSOR.set( innerObject, value );

		JsonObject middleObject = new JsonObject();
		middleObject.add( absoluteFieldPath, innerObject );

		MATCH_ACCESSOR.set( outerObject, middleObject );
		return outerObject;
	}

}
