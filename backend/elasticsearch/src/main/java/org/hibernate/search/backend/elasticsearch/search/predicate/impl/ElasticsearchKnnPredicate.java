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
import org.hibernate.search.backend.elasticsearch.search.predicate.spi.ElasticsearchKnnPredicateBuilder;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchFieldCodec;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchVectorFieldCodec;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.spi.KnnPredicateBuilder;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public abstract class ElasticsearchKnnPredicate extends AbstractElasticsearchSingleFieldPredicate {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	protected final ElasticsearchSearchPredicate filter;
	protected final int k;
	protected final JsonArray vector;
	protected final Integer numberOfCandidates;

	private ElasticsearchKnnPredicate(AbstractKnnBuilder<?> builder) {
		super( builder );
		this.filter = builder.filter;
		this.k = builder.k;
		this.vector = builder.vector;
		this.numberOfCandidates = builder.numberOfCandidates;
		builder.filter = null;
		builder.vector = null;
	}

	public static class ElasticsearchFactory<F>
			extends AbstractElasticsearchCodecAwareSearchQueryElementFactory<KnnPredicateBuilder, F> {
		public ElasticsearchFactory(ElasticsearchFieldCodec<F> codec) {
			super( codec );
		}

		@Override
		public KnnPredicateBuilder create(ElasticsearchSearchIndexScope<?> scope,
				ElasticsearchSearchIndexValueFieldContext<F> field) {
			return new ElasticsearchImpl.Builder<>( codec, scope, field );
		}
	}

	public static class OpenSearchFactory<F>
			extends AbstractElasticsearchCodecAwareSearchQueryElementFactory<KnnPredicateBuilder, F> {
		public OpenSearchFactory(ElasticsearchFieldCodec<F> codec) {
			super( codec );
		}

		@Override
		public KnnPredicateBuilder create(ElasticsearchSearchIndexScope<?> scope,
				ElasticsearchSearchIndexValueFieldContext<F> field) {
			return new OpenSearchImpl.Builder<>( codec, scope, field );
		}
	}

	private abstract static class AbstractKnnBuilder<F> extends AbstractBuilder implements ElasticsearchKnnPredicateBuilder {

		private final Class<?> vectorElementsType;
		private final int indexedVectorsDimension;
		private int k;
		private JsonArray vector;
		private ElasticsearchSearchPredicate filter;
		protected Integer numberOfCandidates;

		private AbstractKnnBuilder(ElasticsearchFieldCodec<F> codec, ElasticsearchSearchIndexScope<?> scope,
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

	private static class ElasticsearchImpl extends ElasticsearchKnnPredicate {

		private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

		private static final JsonAccessor<String> FIELD_ACCESSOR = JsonAccessor.root().property( "field" ).asString();
		private static final JsonArrayAccessor QUERY_VECTOR_ACCESSOR = JsonAccessor.root().property( "query_vector" ).asArray();
		private static final JsonAccessor<Integer> K_ACCESSOR = JsonAccessor.root().property( "k" ).asInteger();

		private static final JsonObjectAccessor FILTER_ACCESSOR = JsonAccessor.root().property( "filter" ).asObject();
		private static final JsonAccessor<Integer> NUM_CANDIDATES_ACCESSOR =
				JsonAccessor.root().property( "num_candidates" ).asInteger();


		private ElasticsearchImpl(Builder<?> builder) {
			super( builder );
		}

		@Override
		public JsonObject buildJsonQuery(PredicateRequestContext context) {
			// we want the query to get created and passed to the request context
			context.contributeKnnClause( ( super.buildJsonQuery( context ) ) );
			// but we don't want it to be an actual query so we return `null`:
			return null;
		}

		@Override
		protected JsonObject doToJsonQuery(PredicateRequestContext context, JsonObject outerObject, JsonObject innerObject) {
			FIELD_ACCESSOR.set( innerObject, absoluteFieldPath );
			K_ACCESSOR.set( innerObject, k );
			if ( filter != null ) {
				JsonObject query = filter.toJsonQuery( context );
				// we shouldn't get a null query here, since that's only possible if a filter was a knn predicate,
				//   and in that case we are failing much faster for am Elasticsearch distribution...
				FILTER_ACCESSOR.set( innerObject, query );
			}
			NUM_CANDIDATES_ACCESSOR.set( innerObject, numberOfCandidates != null ? numberOfCandidates : k );
			QUERY_VECTOR_ACCESSOR.set( innerObject, vector );

			return innerObject;
		}

		@Override
		public void checkNestableWithin(String expectedParentNestedPath) {
			if ( expectedParentNestedPath != null ) {
				throw log.cannotAddKnnClauseAtThisStep();
			}
		}

		private static class Builder<F> extends AbstractKnnBuilder<F> {

			private Builder(ElasticsearchFieldCodec<F> codec, ElasticsearchSearchIndexScope<?> scope,
					ElasticsearchSearchIndexValueFieldContext<F> field) {
				super( codec, scope, field );
			}

			@Override
			public void numberOfCandidates(int numberOfCandidates) {
				this.numberOfCandidates = numberOfCandidates;
			}

			@Override
			public void constantScore() {
				log.elasticsearchKnnIgnoresConstantScore();
			}

			@Override
			public SearchPredicate build() {
				return new ElasticsearchImpl( this );
			}
		}
	}

	private static class OpenSearchImpl extends ElasticsearchKnnPredicate {

		private static final JsonObjectAccessor KNN_ACCESSOR = JsonAccessor.root().property( "knn" ).asObject();
		private static final JsonArrayAccessor VECTOR_ACCESSOR = JsonAccessor.root().property( "vector" ).asArray();
		private static final JsonAccessor<Integer> K_ACCESSOR = JsonAccessor.root().property( "k" ).asInteger();

		private static final JsonObjectAccessor FILTER_ACCESSOR = JsonAccessor.root().property( "filter" ).asObject();

		private OpenSearchImpl(Builder<?> builder) {
			super( builder );
		}

		@Override
		protected JsonObject doToJsonQuery(PredicateRequestContext context, JsonObject outerObject, JsonObject innerObject) {
			JsonObject field = new JsonObject();
			KNN_ACCESSOR.set( outerObject, field );

			field.add( absoluteFieldPath, innerObject );
			if ( filter != null ) {
				FILTER_ACCESSOR.set( innerObject, filter.toJsonQuery( context ) );
			}
			K_ACCESSOR.set( innerObject, k );
			VECTOR_ACCESSOR.set( innerObject, vector );

			return outerObject;
		}

		protected static class Builder<F> extends AbstractKnnBuilder<F> {
			protected Builder(ElasticsearchFieldCodec<F> codec, ElasticsearchSearchIndexScope<?> scope,
					ElasticsearchSearchIndexValueFieldContext<F> field) {
				super( codec, scope, field );
			}

			@Override
			public void numberOfCandidates(int numberOfCandidates) {
				throw log.knnNumberOfCandidatesUnsupportedOption();
			}

			@Override
			public SearchPredicate build() {
				return new OpenSearchImpl( this );
			}
		}
	}
}
