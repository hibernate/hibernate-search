/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.predicate.impl;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Array;

import org.hibernate.search.backend.elasticsearch.gson.impl.JsonAccessor;
import org.hibernate.search.backend.elasticsearch.gson.impl.JsonArrayAccessor;
import org.hibernate.search.backend.elasticsearch.gson.impl.JsonObjectAccessor;
import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.backend.elasticsearch.search.common.impl.AbstractElasticsearchCodecAwareSearchQueryElementFactory;
import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexScope;
import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexValueFieldContext;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchFieldCodec;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchVectorFieldCodec;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.spi.KnnPredicateBuilder;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class ElasticsearchKnnPredicate extends AbstractElasticsearchSingleFieldPredicate {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private static final JsonAccessor<String> FIELD_ACCESSOR = JsonAccessor.root().property( "field" ).asString();
	private static final JsonArrayAccessor QUERY_VECTOR_ACCESSOR = JsonAccessor.root().property( "query_vector" ).asArray();
	private static final JsonAccessor<Integer> K_ACCESSOR = JsonAccessor.root().property( "k" ).asInteger();

	private static final JsonObjectAccessor FILTER_ACCESSOR = JsonAccessor.root().property( "filter" ).asObject();
	private static final JsonAccessor<Integer> NUM_CANDIDATES_ACCESSOR =
			JsonAccessor.root().property( "num_candidates" ).asInteger();

	private final ElasticsearchSearchPredicate filter;
	private final int k;
	private final JsonArray vector;
	private final Integer numberOfCandidates;

	private ElasticsearchKnnPredicate(Builder<?> builder) {
		super( builder );
		this.filter = builder.filter;
		this.k = builder.k;
		this.vector = builder.vector;
		this.numberOfCandidates = builder.numberOfCandidates;
		builder.filter = null;
		builder.vector = null;
	}

	@Override
	protected JsonObject doToJsonKnn(PredicateRequestContext context) {
		JsonObject object = new JsonObject();
		FIELD_ACCESSOR.set( object, absoluteFieldPath );
		K_ACCESSOR.set( object, k );
		if ( filter != null ) {
			FILTER_ACCESSOR.set( object, filter.toJsonQuery( context ) );
		}
		if ( numberOfCandidates != null ) {
			NUM_CANDIDATES_ACCESSOR.set( object, numberOfCandidates );
		}
		QUERY_VECTOR_ACCESSOR.set( object, vector );

		return object;
	}

	@Override
	public JsonObject toJsonQuery(PredicateRequestContext context) {
		return null;
	}

	@Override
	protected JsonObject doToJsonQuery(PredicateRequestContext context, JsonObject outerObject, JsonObject innerObject) {
		throw new AssertionFailure( "Shouldn't be reached since we've overridden the toJsonQuery" );
	}

	@Override
	public ElasticsearchSearchPredicate checkAcceptableAsBoolPredicateClause(String clauseType) {
		if ( !ElasticsearchBooleanPredicate.SHOULD_PROPERTY_NAME.equals( clauseType ) ) {
			throw log.knnPredicateCanOnlyBeShouldClause();
		}
		return super.checkAcceptableAsBoolPredicateClause( clauseType );
	}

	@Override
	public void checkNestableWithin(String expectedParentNestedPath) {
		if ( expectedParentNestedPath != null ) {
			throw log.cannotBeNestedPredicate();
		}
	}

	public static class Factory<F>
			extends AbstractElasticsearchCodecAwareSearchQueryElementFactory<KnnPredicateBuilder, F> {
		public Factory(ElasticsearchFieldCodec<F> codec) {
			super( codec );
		}

		@Override
		public KnnPredicateBuilder create(ElasticsearchSearchIndexScope<?> scope,
				ElasticsearchSearchIndexValueFieldContext<F> field) {
			return new Builder<>( codec, scope, field );
		}
	}

	private static class Builder<F> extends AbstractBuilder implements KnnPredicateBuilder {

		private final Class<?> vectorElementsType;
		private final int indexedVectorsDimension;
		private int k;
		private JsonArray vector;
		private Integer numberOfCandidates;
		private ElasticsearchSearchPredicate filter;

		private Builder(ElasticsearchFieldCodec<F> codec, ElasticsearchSearchIndexScope<?> scope,
				ElasticsearchSearchIndexValueFieldContext<F> field) {
			super( scope, field );
			if ( codec instanceof ElasticsearchVectorFieldCodec ) {
				ElasticsearchVectorFieldCodec<F> vectorCodec = (ElasticsearchVectorFieldCodec<F>) codec;
				vectorElementsType = vectorCodec.vectorElementsType();
				indexedVectorsDimension = vectorCodec.getConfiguredDimensions();
			}
			else {
				// shouldn't really happen as if someone tries this it should fail on `queryElementFactory` lookup.
				throw new AssertionFailure( "Attempting to use a knn predicate on a non-vector field." );
			}
		}

		@Override
		public void k(int k) {
			this.k = k;
		}

		@Override
		public void vector(Object vector) {
			if ( !vector.getClass().isArray() ) {
				throw new IllegalArgumentException( "Vector can only be either a float or a byte array (float[], byte[])." );
			}
			if ( !vectorElementsType.equals( vector.getClass().getComponentType() ) ) {
				throw log.vectorKnnMatchVectorTypeDiffersFromField( absoluteFieldPath, vectorElementsType,
						vector.getClass().getComponentType() );
			}
			if ( Array.getLength( vector ) != indexedVectorsDimension ) {
				throw log.vectorKnnMatchVectorDimensionDiffersFromField( absoluteFieldPath, indexedVectorsDimension,
						Array.getLength( vector )
				);
			}
			this.vector = vectorToJsonArray( vector, vectorElementsType );
		}

		@Override
		public void filter(SearchPredicate filter) {
			this.filter = ElasticsearchSearchPredicate.from( scope, filter );
		}

		@Override
		public void numberOfCandidates(int numberOfCandidates) {
			this.numberOfCandidates = numberOfCandidates;
		}

		@Override
		public SearchPredicate build() {
			return new ElasticsearchKnnPredicate( this );
		}
	}

	private static JsonArray vectorToJsonArray(Object vector, Class<?> vectorElementsType) {
		// we know it is an array since we've checked it when we got the vector
		int length = Array.getLength( vector );
		JsonArray array = new JsonArray( length );
		for ( int i = 0; i < length; i++ ) {
			if ( byte.class.equals( vectorElementsType ) ) {
				array.add( Array.getByte( vector, i ) );
			}
			else {
				array.add( Array.getFloat( vector, i ) );
			}
		}
		return array;
	}
}
