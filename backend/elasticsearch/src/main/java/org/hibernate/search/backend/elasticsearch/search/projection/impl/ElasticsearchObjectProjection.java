/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.projection.impl;

import java.util.Arrays;

import org.hibernate.search.backend.elasticsearch.search.common.impl.AbstractElasticsearchCompositeNodeSearchQueryElementFactory;
import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexCompositeNodeContext;
import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexScope;
import org.hibernate.search.engine.search.loading.spi.LoadingResult;
import org.hibernate.search.engine.search.loading.spi.ProjectionHitMapper;
import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.projection.spi.CompositeProjectionBuilder;
import org.hibernate.search.engine.search.projection.spi.ProjectionAccumulator;
import org.hibernate.search.engine.search.projection.spi.ProjectionCompositor;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

/**
 * A projection that yields one composite value per object in a given object field.
 * <p>
 * Not to be confused with {@link ElasticsearchCompositeProjection}.
 *
 * @param <E> The type of the temporary storage for component values.
 * @param <V> The type of a single composed value.
 * @param <P> The type of the final projection result representing an accumulation of composed values of type {@code V}.
 */
public class ElasticsearchObjectProjection<E, V, P>
		extends AbstractElasticsearchProjection<P> {

	private final String absoluteFieldPath;
	private final String[] absoluteFieldPathComponents;
	private final String requiredContextAbsoluteFieldPath;
	private final ElasticsearchSearchProjection<?>[] inners;
	private final ProjectionCompositor<E, V> compositor;
	private final ProjectionAccumulator.Provider<V, P> accumulatorProvider;

	public ElasticsearchObjectProjection(Builder builder, ElasticsearchSearchProjection<?>[] inners,
			ProjectionCompositor<E, V> compositor, ProjectionAccumulator.Provider<V, P> accumulatorProvider) {
		super( builder.scope );
		this.absoluteFieldPath = builder.objectField.absolutePath();
		this.absoluteFieldPathComponents = builder.objectField.absolutePathComponents();
		this.requiredContextAbsoluteFieldPath = accumulatorProvider.isSingleValued()
				? builder.objectField.closestMultiValuedParentAbsolutePath() : null;
		this.inners = inners;
		this.compositor = compositor;
		this.accumulatorProvider = accumulatorProvider;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "["
				+ "inners=" + Arrays.toString( inners )
				+ ", compositor=" + compositor
				+ ", accumulatorProvider=" + accumulatorProvider
				+ "]";
	}

	@Override
	public Extractor<?, P> request(JsonObject requestBody, ProjectionRequestContext context) {
		ProjectionRequestContext innerContext = context.forField( absoluteFieldPath, absoluteFieldPathComponents );
		if ( requiredContextAbsoluteFieldPath != null
				&& !requiredContextAbsoluteFieldPath.equals( context.absoluteCurrentFieldPath() ) ) {
			throw log.invalidSingleValuedProjectionOnValueFieldInMultiValuedObjectField(
					absoluteFieldPath, requiredContextAbsoluteFieldPath );
		}
		String[] extractorFieldPathComponents = innerContext.relativeCurrentFieldPathComponents();
		JsonPrimitive fieldPathJson = new JsonPrimitive( absoluteFieldPath );
		AccumulatingSourceExtractor.REQUEST_SOURCE_ACCESSOR.addElementIfAbsent( requestBody, fieldPathJson );
		Extractor<?, ?>[] innerExtractors = new Extractor[inners.length];
		for ( int i = 0; i < inners.length; i++ ) {
			innerExtractors[i] = inners[i].request( requestBody, innerContext );
		}
		return new ObjectFieldExtractor<>( extractorFieldPathComponents, accumulatorProvider.get(), innerExtractors );
	}

	/**
	 * @param <A> The type of the temporary storage for accumulated values, before and after being composed.
	 */
	private class ObjectFieldExtractor<A> extends AccumulatingSourceExtractor<E, V, A, P> {
		private final Extractor<?, ?>[] inners;

		private ObjectFieldExtractor(String[] fieldPathComponents, ProjectionAccumulator<E, V, A, P> accumulator,
				Extractor<?, ?>[] inners) {
			super( fieldPathComponents, accumulator );
			this.inners = inners;
		}

		@Override
		public String toString() {
			return getClass().getSimpleName() + "["
					+ "inners=" + Arrays.toString( inners )
					+ ", compositor=" + compositor
					+ ", accumulator=" + accumulator
					+ "]";
		}

		@Override
		protected E extract(ProjectionHitMapper<?> projectionHitMapper, JsonObject hit, JsonElement sourceElement,
				ProjectionExtractContext context) {
			if ( sourceElement == null || sourceElement.isJsonNull() ) {
				return null;
			}
			JsonObject sourceObject = sourceElement.getAsJsonObject();
			E components = compositor.createInitial();
			for ( int i = 0; i < inners.length; i++ ) {
				Object extractedDataForInner = inners[i].extract( projectionHitMapper, hit, sourceObject, context );
				components = compositor.set( components, i, extractedDataForInner );
			}
			return components;
		}

		@Override
		public final P transform(LoadingResult<?> loadingResult, A accumulated,
				ProjectionTransformContext context) {
			for ( int i = 0; i < accumulator.size( accumulated ); i++ ) {
				E transformedData = accumulator.get( accumulated, i );
				if ( transformedData == null ) {
					continue;
				}
				// Transform in-place
				for ( int j = 0; j < inners.length; j++ ) {
					Object extractedDataForInner = compositor.get( transformedData, j );
					Object transformedDataForInner = Extractor.transformUnsafe( inners[j],
							loadingResult, extractedDataForInner, context );
					transformedData = compositor.set( transformedData, j, transformedDataForInner );
				}

				accumulated = accumulator.transform( accumulated, i, compositor.finish( transformedData ) );
			}
			return accumulator.finish( accumulated );
		}
	}

	public static class Factory
			extends AbstractElasticsearchCompositeNodeSearchQueryElementFactory<Builder> {
		@Override
		public Builder create(ElasticsearchSearchIndexScope<?> scope,
				ElasticsearchSearchIndexCompositeNodeContext objectField) {
			return new Builder( scope, objectField );
		}
	}

	static class Builder implements CompositeProjectionBuilder {

		private final ElasticsearchSearchIndexScope<?> scope;
		private final ElasticsearchSearchIndexCompositeNodeContext objectField;

		Builder(ElasticsearchSearchIndexScope<?> scope, ElasticsearchSearchIndexCompositeNodeContext objectField) {
			this.scope = scope;
			this.objectField = objectField;
		}

		@Override
		public <E, V, P> SearchProjection<P> build(SearchProjection<?>[] inners, ProjectionCompositor<E, V> compositor,
				ProjectionAccumulator.Provider<V, P> accumulatorProvider) {
			if ( accumulatorProvider.isSingleValued() && objectField.multiValued() ) {
				throw log.invalidSingleValuedProjectionOnMultiValuedField( objectField.absolutePath(),
						objectField.eventContext() );
			}
			ElasticsearchSearchProjection<?>[] typedInners =
					new ElasticsearchSearchProjection<?>[ inners.length ];
			for ( int i = 0; i < inners.length; i++ ) {
				typedInners[i] = ElasticsearchSearchProjection.from( scope, inners[i] );
			}
			return new ElasticsearchObjectProjection<>( this, typedInners,
					compositor, accumulatorProvider );
		}
	}
}
