/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.search.projection.impl;

import java.util.Optional;
import java.util.regex.Pattern;

import org.hibernate.search.backend.elasticsearch.gson.impl.JsonAccessor;
import org.hibernate.search.backend.elasticsearch.gson.impl.JsonArrayAccessor;
import org.hibernate.search.backend.elasticsearch.gson.impl.JsonObjectAccessor;
import org.hibernate.search.backend.elasticsearch.search.common.impl.AbstractElasticsearchValueFieldSearchQueryElementFactory;
import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexScope;
import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexValueFieldContext;
import org.hibernate.search.backend.elasticsearch.search.projection.util.impl.SloppyMath;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchGeoPointFieldCodec;
import org.hibernate.search.engine.backend.types.converter.spi.ProjectionConverter;
import org.hibernate.search.engine.search.loading.spi.LoadingResult;
import org.hibernate.search.engine.search.loading.spi.ProjectionHitMapper;
import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.projection.spi.DistanceToFieldProjectionBuilder;
import org.hibernate.search.engine.search.projection.spi.ProjectionAccumulator;
import org.hibernate.search.engine.spatial.DistanceUnit;
import org.hibernate.search.engine.spatial.GeoPoint;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * A projection on the distance from a given center to the GeoPoint defined in an index field.
 *
 * @param <A> The type of the temporary storage for accumulated values, before and after being transformed.
 * @param <P> The type of the final projection result representing accumulated distance values.
 */
public class ElasticsearchDistanceToFieldProjection<A, P> extends AbstractElasticsearchProjection<P>
		implements ElasticsearchSearchProjection.Extractor<A, P> {

	private static final JsonObjectAccessor SCRIPT_FIELDS_ACCESSOR = JsonAccessor.root().property( "script_fields" ).asObject();
	private static final JsonObjectAccessor FIELDS_ACCESSOR = JsonAccessor.root().property( "fields" ).asObject();
	private static final JsonArrayAccessor SORT_ACCESSOR = JsonAccessor.root().property( "sort" ).asArray();
	private static final ElasticsearchGeoPointFieldCodec CODEC = ElasticsearchGeoPointFieldCodec.INSTANCE;

	private static final ProjectionConverter<Double, Double> NO_OP_DOUBLE_CONVERTER =
			ProjectionConverter.passThrough( Double.class );

	private static final Pattern NON_DIGITS_PATTERN = Pattern.compile( "\\D" );

	private static final String DISTANCE_PROJECTION_SCRIPT =
			// Check whether the field exists first with "containsKey";
			// in a multi-index search, it may not exist for all indexes.
			// Use ".size() != 0" to check whether this field has a value. ".value != null" won't work on ES7+
			" if (doc.containsKey(params.fieldPath) && doc[params.fieldPath].size() != 0) {" +
					" return doc[params.fieldPath].arcDistance(params.lat, params.lon);" +
					" } else {" +
					" return null;" +
					" }";

	private final String absoluteFieldPath;
	private final boolean singleValuedInRoot;

	private final GeoPoint center;
	private final DistanceUnit unit;

	private final ProjectionAccumulator<Double, Double, A, P> accumulator;

	private final String scriptFieldName;
	private final ElasticsearchFieldProjection<?, Double, P> sourceProjection;

	private ElasticsearchDistanceToFieldProjection(Builder builder,
			ProjectionAccumulator.Provider<Double, P> accumulatorProvider,
			ProjectionAccumulator<Double, Double, A, P> accumulator) {
		super( builder );
		this.absoluteFieldPath = builder.field.absolutePath();
		this.singleValuedInRoot = !builder.field.multiValuedInRoot();
		this.center = builder.center;
		this.unit = builder.unit;
		this.accumulator = accumulator;
		if ( singleValuedInRoot && builder.field.nestedPathHierarchy().isEmpty() ) {
			// Rely on docValues when there is no sort to extract the distance from.
			scriptFieldName = createScriptFieldName( absoluteFieldPath, center );
			sourceProjection = null;
		}
		else {
			// Rely on _source when there is no sort to extract the distance from.
			scriptFieldName = null;
			this.sourceProjection = new ElasticsearchFieldProjection<>(
					builder.scope, builder.field,
					this::computeDistanceWithUnit, false, NO_OP_DOUBLE_CONVERTER, accumulatorProvider
			);
		}
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "["
				+ "absoluteFieldPath=" + absoluteFieldPath
				+ ", center=" + center
				+ ", unit=" + unit
				+ ", accumulator=" + accumulator
				+ "]";
	}

	@Override
	public Extractor<?, P> request(JsonObject requestBody, ProjectionRequestContext context) {
		context.checkValidField( absoluteFieldPath );
		if ( singleValuedInRoot && context.root().getDistanceSortIndex( absoluteFieldPath, center ) != null ) {
			// Nothing to do, we'll rely on the sort key
			return this;
		}
		else if ( scriptFieldName != null ) {
			// we rely on a script to compute the distance
			SCRIPT_FIELDS_ACCESSOR
					.property( scriptFieldName ).asObject()
					.property( "script" ).asObject()
					.set( requestBody, createScript( absoluteFieldPath, center ) );
			return this;
		}
		else {
			// we rely on the _source to compute the distance
			return sourceProjection.request( requestBody, context );
		}
	}

	@Override
	public A extract(ProjectionHitMapper<?> projectionHitMapper, JsonObject hit,
			JsonObject source, ProjectionExtractContext context) {
		Integer distanceSortIndex = singleValuedInRoot ? context.getDistanceSortIndex( absoluteFieldPath, center ) : null;

		if ( distanceSortIndex != null ) {
			A accumulated = accumulator.createInitial();
			accumulated = accumulator.accumulate( accumulated, extractDistanceFromSortKey( hit, distanceSortIndex ) );
			return accumulated;
		}
		else {
			A accumulated = accumulator.createInitial();
			accumulated = accumulator.accumulate( accumulated, extractDistanceFromScriptField( hit ) );
			return accumulated;
		}
	}

	@Override
	public P transform(LoadingResult<?> loadingResult, A extractedData,
			ProjectionTransformContext context) {
		// Nothing to transform: we take the values as they are.
		return accumulator.finish( extractedData );
	}

	private Double extractDistanceFromScriptField(JsonObject hit) {
		Optional<JsonElement> projectedFieldElement =
				FIELDS_ACCESSOR.property( scriptFieldName ).asArray().element( 0 ).get( hit );
		if ( !projectedFieldElement.isPresent() || projectedFieldElement.get().isJsonNull() ) {
			return null;
		}

		if ( projectedFieldElement.get().isJsonPrimitive() ) {
			return unit.fromMeters( projectedFieldElement.get().getAsDouble() );
		}
		else {
			JsonObject geoPoint = projectedFieldElement.get().getAsJsonObject();
			return computeDistanceWithUnit( geoPoint );
		}
	}

	private Double extractDistanceFromSortKey(JsonObject hit, int distanceSortIndex) {
		// we extract the value from the sort key
		Optional<JsonElement> sortKeyDistanceElement = SORT_ACCESSOR.element( distanceSortIndex ).get( hit );

		if ( !sortKeyDistanceElement.isPresent() ) {
			return null;
		}
		else if ( !sortKeyDistanceElement.get().getAsJsonPrimitive().isNumber() ) {
			// Elasticsearch will return "Infinity" if the distance has not been computed.
			// Usually, it's because the indexed object doesn't have a location defined for this field.
			return null;
		}

		double distanceInMeters = sortKeyDistanceElement.get().getAsJsonPrimitive().getAsDouble();

		return unit.fromMeters( distanceInMeters );
	}

	private Double computeDistanceWithUnit(JsonElement geoPoint) {
		GeoPoint decoded = CODEC.decode( geoPoint );
		if ( decoded == null ) {
			return null;
		}
		double distanceInMeters = SloppyMath.haversinMeters(
				center.latitude(), center.longitude(),
				decoded.latitude(), decoded.longitude()
		);
		return unit.fromMeters( distanceInMeters );
	}

	private static String createScriptFieldName(String absoluteFieldPath, GeoPoint center) {
		StringBuilder sb = new StringBuilder();
		sb.append( "distance_" )
				.append( absoluteFieldPath )
				.append( "_" )
				.append( NON_DIGITS_PATTERN.matcher( Double.toString( center.latitude() ) ).replaceAll( "_" ) )
				.append( "_" )
				.append( NON_DIGITS_PATTERN.matcher( Double.toString( center.longitude() ) ).replaceAll( "_" ) );
		return sb.toString();
	}

	private static JsonObject createScript(String absoluteFieldPath, GeoPoint center) {
		JsonObject params = new JsonObject();
		params.addProperty( "lat", center.latitude() );
		params.addProperty( "lon", center.longitude() );
		params.addProperty( "fieldPath", absoluteFieldPath );

		JsonObject scriptContent = new JsonObject();
		scriptContent.addProperty( "lang", "painless" );
		scriptContent.add( "params", params );
		scriptContent.addProperty( "source", DISTANCE_PROJECTION_SCRIPT );

		return scriptContent;
	}

	public static class Factory
			extends
			AbstractElasticsearchValueFieldSearchQueryElementFactory<DistanceToFieldProjectionBuilder, GeoPoint> {
		@Override
		public Builder create(ElasticsearchSearchIndexScope<?> scope,
				ElasticsearchSearchIndexValueFieldContext<GeoPoint> field) {
			// Check the compatibility of nested structure in the case of multi-index search.
			field.nestedPathHierarchy();
			return new Builder( scope, field );
		}
	}

	public static class Builder extends AbstractElasticsearchProjection.AbstractBuilder<Double>
			implements DistanceToFieldProjectionBuilder {

		private final ElasticsearchSearchIndexValueFieldContext<GeoPoint> field;

		private GeoPoint center;
		private DistanceUnit unit = DistanceUnit.METERS;

		private Builder(ElasticsearchSearchIndexScope<?> scope, ElasticsearchSearchIndexValueFieldContext<GeoPoint> field) {
			super( scope );
			this.field = field;
		}

		@Override
		public void center(GeoPoint center) {
			this.center = center;
		}

		@Override
		public void unit(DistanceUnit unit) {
			this.unit = unit;
		}

		@Override
		public <P> SearchProjection<P> build(ProjectionAccumulator.Provider<Double, P> accumulatorProvider) {
			return new ElasticsearchDistanceToFieldProjection<>( this, accumulatorProvider,
					accumulatorProvider.get() );
		}
	}
}
