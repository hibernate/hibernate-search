/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.types.sort.impl;

import java.lang.invoke.MethodHandles;
import java.util.List;

import org.hibernate.search.backend.elasticsearch.gson.impl.JsonAccessor;
import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.backend.elasticsearch.scope.model.impl.ElasticsearchCompatibilityChecker;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchContext;
import org.hibernate.search.backend.elasticsearch.search.sort.impl.AbstractElasticsearchSearchNestedSortBuilder;
import org.hibernate.search.backend.elasticsearch.search.sort.impl.ElasticsearchSearchSortBuilder;
import org.hibernate.search.backend.elasticsearch.search.sort.impl.ElasticsearchSearchSortCollector;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchFieldCodec;
import org.hibernate.search.engine.backend.types.converter.spi.DslConverter;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.engine.search.common.SortMode;
import org.hibernate.search.engine.search.common.ValueConvert;
import org.hibernate.search.engine.search.sort.spi.FieldSortBuilder;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.reporting.EventContext;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

public class ElasticsearchStandardFieldSortBuilder<F> extends AbstractElasticsearchSearchNestedSortBuilder
		implements FieldSortBuilder<ElasticsearchSearchSortBuilder> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private static final JsonAccessor<JsonElement> MISSING_ACCESSOR = JsonAccessor.root().property( "missing" );
	private static final JsonPrimitive MISSING_FIRST_KEYWORD_JSON = new JsonPrimitive( "_first" );
	private static final JsonPrimitive MISSING_LAST_KEYWORD_JSON = new JsonPrimitive( "_last" );

	private static final JsonAccessor<JsonElement> MODE_ACCESSOR = JsonAccessor.root().property( "mode" );
	private static final JsonPrimitive SUM_KEYWORD_JSON = new JsonPrimitive( "sum" );
	private static final JsonPrimitive AVG_KEYWORD_JSON = new JsonPrimitive( "avg" );
	private static final JsonPrimitive MIN_KEYWORD_JSON = new JsonPrimitive( "min" );
	private static final JsonPrimitive MAX_KEYWORD_JSON = new JsonPrimitive( "max" );
	private static final JsonPrimitive MEDIAN_KEYWORD_JSON = new JsonPrimitive( "median" );

	private final ElasticsearchSearchContext searchContext;

	private final String absoluteFieldPath;

	private final DslConverter<?, ? extends F> converter;
	private final DslConverter<F, ? extends F> rawConverter;
	private final ElasticsearchCompatibilityChecker converterChecker;

	private final ElasticsearchFieldCodec<F> codec;

	private JsonElement missing;

	private JsonPrimitive mode;

	public ElasticsearchStandardFieldSortBuilder(ElasticsearchSearchContext searchContext,
			String absoluteFieldPath, List<String> nestedPathHierarchy,
			DslConverter<?, ? extends F> converter, DslConverter<F, ? extends F> rawConverter,
			ElasticsearchCompatibilityChecker converterChecker, ElasticsearchFieldCodec<F> codec) {
		super( nestedPathHierarchy, searchContext.getSearchSyntax() );
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
		DslConverter<?, ? extends F> dslToIndexConverter = getDslToIndexConverter( convert );
		try {
			F converted = dslToIndexConverter.convertUnknown( value, searchContext.getToDocumentFieldValueConvertContext() );
			this.missing = codec.encodeForMissing( converted );
		}
		catch (RuntimeException e) {
			throw log.cannotConvertDslParameter( e.getMessage(), e, getEventContext() );
		}
	}

	@Override
	public void mode(SortMode mode) {
		if ( !nestedPathHierarchy.isEmpty() && SortMode.MEDIAN.equals( mode ) ) {
			throw log.cannotComputeMedianAcrossNested( getEventContext() );
		}
		if ( mode != null ) {
			switch ( mode ) {
				case SUM:
					this.mode = SUM_KEYWORD_JSON;
					break;
				case AVG:
					this.mode = AVG_KEYWORD_JSON;
					break;
				case MIN:
					this.mode = MIN_KEYWORD_JSON;
					break;
				case MAX:
					this.mode = MAX_KEYWORD_JSON;
					break;
				case MEDIAN:
					this.mode = MEDIAN_KEYWORD_JSON;
					break;
				default:
					throw new AssertionFailure( "Unexpected sort mode: " + mode );
			}
		}
	}

	@Override
	public void doBuildAndAddTo(ElasticsearchSearchSortCollector collector, JsonObject innerObject) {
		if ( missing != null ) {
			MISSING_ACCESSOR.set( innerObject, missing );
		}
		if ( mode != null ) {
			MODE_ACCESSOR.set( innerObject, mode );
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

	protected final EventContext getEventContext() {
		return EventContexts.fromIndexFieldAbsolutePath( absoluteFieldPath );
	}

	private DslConverter<?, ? extends F> getDslToIndexConverter(ValueConvert convert) {
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
