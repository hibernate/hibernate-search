/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.sort.impl;

import java.lang.invoke.MethodHandles;
import java.util.List;

import org.hibernate.search.backend.elasticsearch.gson.impl.JsonAccessor;
import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.backend.elasticsearch.scope.model.impl.ElasticsearchCompatibilityChecker;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchContext;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchFieldCodec;
import org.hibernate.search.engine.backend.types.converter.ToDocumentFieldValueConverter;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.engine.search.common.ValueConvert;
import org.hibernate.search.engine.search.sort.spi.FieldSortBuilder;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

public class ElasticsearchFieldSortBuilder<F> extends AbstractElasticsearchSearchNestedSortBuilder
		implements FieldSortBuilder<ElasticsearchSearchSortBuilder> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private static final JsonAccessor<JsonElement> MISSING_ACCESSOR = JsonAccessor.root().property( "missing" );
	private static final JsonPrimitive MISSING_FIRST_KEYWORD_JSON = new JsonPrimitive( "_first" );
	private static final JsonPrimitive MISSING_LAST_KEYWORD_JSON = new JsonPrimitive( "_last" );

	private final ElasticsearchSearchContext searchContext;

	private final String absoluteFieldPath;

	private final ToDocumentFieldValueConverter<?, ? extends F> converter;
	private final ToDocumentFieldValueConverter<F, ? extends F> rawConverter;
	private final ElasticsearchCompatibilityChecker converterChecker;

	private final ElasticsearchFieldCodec<F> codec;

	private JsonElement missing;

	public ElasticsearchFieldSortBuilder(ElasticsearchSearchContext searchContext,
			String absoluteFieldPath, List<String> nestedPathHierarchy,
			ToDocumentFieldValueConverter<?, ? extends F> converter, ToDocumentFieldValueConverter<F, ? extends F> rawConverter,
			ElasticsearchCompatibilityChecker converterChecker, ElasticsearchFieldCodec<F> codec) {
		super( nestedPathHierarchy );
		this.searchContext = searchContext;
		this.absoluteFieldPath = absoluteFieldPath;
		this.converter = converter;
		this.rawConverter = rawConverter;
		this.converterChecker = converterChecker;
		this.codec = codec;
	}

	@Override
	public void missingFirst() {
		this.missing = MISSING_FIRST_KEYWORD_JSON;
	}

	@Override
	public void missingLast() {
		this.missing = MISSING_LAST_KEYWORD_JSON;
	}

	@Override
	public void missingAs(Object value, ValueConvert convert) {
		ToDocumentFieldValueConverter<?, ? extends F> dslToIndexConverter = getDslToIndexConverter( convert );
		try {
			F converted = dslToIndexConverter.convertUnknown( value, searchContext.getToDocumentFieldValueConvertContext() );
			this.missing = codec.encodeForMissing( converted );
		}
		catch (RuntimeException e) {
			throw log.cannotConvertDslParameter(
					e.getMessage(), e, EventContexts.fromIndexFieldAbsolutePath( absoluteFieldPath )
			);
		}
	}

	@Override
	public void doBuildAndAddTo(ElasticsearchSearchSortCollector collector, JsonObject innerObject) {
		if ( missing != null ) {
			MISSING_ACCESSOR.set( innerObject, missing );
		}

		if ( innerObject.size() == 0 ) {
			collector.collectSort( new JsonPrimitive( absoluteFieldPath ) );
		}
		else {
			JsonObject outerObject = new JsonObject();
			outerObject.add( absoluteFieldPath, innerObject );
			collector.collectSort( outerObject );
		}
	}

	private ToDocumentFieldValueConverter<?, ? extends F> getDslToIndexConverter(ValueConvert convert) {
		switch ( convert ) {
			case NO:
				return rawConverter;
			case YES:
			default:
				converterChecker.failIfNotCompatible();
				return converter;
		}
	}
}
