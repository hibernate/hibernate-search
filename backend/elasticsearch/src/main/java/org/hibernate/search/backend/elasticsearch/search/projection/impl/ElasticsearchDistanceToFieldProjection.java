/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.projection.impl;

import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

import org.hibernate.search.backend.elasticsearch.gson.impl.JsonAccessor;
import org.hibernate.search.backend.elasticsearch.gson.impl.JsonArrayAccessor;
import org.hibernate.search.backend.elasticsearch.gson.impl.JsonObjectAccessor;
import org.hibernate.search.backend.elasticsearch.search.projection.util.impl.SloppyMath;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchGeoPointFieldCodec;
import org.hibernate.search.engine.backend.types.converter.runtime.FromDocumentFieldValueConvertContext;
import org.hibernate.search.engine.backend.types.converter.spi.ProjectionConverter;
import org.hibernate.search.engine.search.loading.spi.LoadingResult;
import org.hibernate.search.engine.search.loading.spi.ProjectionHitMapper;
import org.hibernate.search.engine.search.projection.spi.ProjectionAccumulator;
import org.hibernate.search.engine.spatial.DistanceUnit;
import org.hibernate.search.engine.spatial.GeoPoint;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * A projection on the distance from a given center to the GeoPoint defined in an index field.
 *
 * @param <E> The type of aggregated values extracted from the backend response (before conversion).
 * @param <P> The type of aggregated values returned by the projection (after conversion).
 */
class ElasticsearchDistanceToFieldProjection<E, P> implements ElasticsearchSearchProjection<E, P> {

	private static final JsonObjectAccessor SCRIPT_FIELDS_ACCESSOR = JsonAccessor.root().property( "script_fields" ).asObject();
	private static final JsonObjectAccessor FIELDS_ACCESSOR = JsonAccessor.root().property( "fields" ).asObject();
	private static final JsonArrayAccessor SORT_ACCESSOR = JsonAccessor.root().property( "sort" ).asArray();
	private static final ElasticsearchGeoPointFieldCodec CODEC = ElasticsearchGeoPointFieldCodec.INSTANCE;

	private static final ProjectionConverter<Double, Double> NO_OP_DOUBLE_CONVERTER = new ProjectionConverter<>(
			Double.class,
			(value, context) -> value
	);

	private static final Pattern NON_DIGITS_PATTERN = Pattern.compile( "\\D" );

	private static final String DISTANCE_PROJECTION_SCRIPT =
		// Use ".size() != 0" to check whether this field has a value. ".value != null" won't work on ES7+
		" if (doc[params.fieldPath].size() != 0) {" +
			" return doc[params.fieldPath].arcDistance(params.lat, params.lon);" +
		" } else {" +
			" return null;" +
		" }";

	private final Set<String> indexNames;
	private final String absoluteFieldPath;
	private final boolean multiValued;

	private final GeoPoint center;
	private final DistanceUnit unit;

	private final ProjectionAccumulator<Double, Double, E, P> accumulator;

	private final String scriptFieldName;
	private final ElasticsearchFieldProjection<E, P, ?, Double> sourceProjection;

	ElasticsearchDistanceToFieldProjection(Set<String> indexNames, String absoluteFieldPath,
			String[] absoluteFieldPathComponents, boolean nested, boolean multiValued,
			GeoPoint center, DistanceUnit unit,
			ProjectionAccumulator<Double, Double, E, P> accumulator) {
		this.indexNames = indexNames;
		this.absoluteFieldPath = absoluteFieldPath;
		this.multiValued = multiValued;
		this.center = center;
		this.unit = unit;
		this.accumulator = accumulator;
		if ( !multiValued && !nested ) {
			// Rely on docValues when there is no sort to extract the distance from.
			scriptFieldName = createScriptFieldName( absoluteFieldPath, center );
			sourceProjection = null;
		}
		else {
			// Rely on _source when there is no sort to extract the distance from.
			scriptFieldName = null;
			this.sourceProjection = new ElasticsearchFieldProjection<>(
					indexNames, absoluteFieldPath, absoluteFieldPathComponents,
					this::computeDistanceWithUnit,
					NO_OP_DOUBLE_CONVERTER,
					accumulator
			);
		}
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder( getClass().getSimpleName() )
				.append( "[" )
				.append( "absoluteFieldPath=" ).append( absoluteFieldPath )
				.append( ", center=" ).append( center )
				.append( ", unit=" ).append( unit )
				.append( ", accumulator=" ).append( accumulator )
				.append( "]" );
		return sb.toString();
	}

	@Override
	public Set<String> getIndexNames() {
		return indexNames;
	}

	@Override
	public void request(JsonObject requestBody, SearchProjectionRequestContext context) {
		if ( !multiValued && context.getDistanceSortIndex( absoluteFieldPath, center ) != null ) {
			// Nothing to do, we'll rely on the sort key
		}
		else if ( scriptFieldName != null ) {
			// we rely on a script to compute the distance
			SCRIPT_FIELDS_ACCESSOR
					.property( scriptFieldName ).asObject()
					.property( "script" ).asObject()
					.set( requestBody, createScript( absoluteFieldPath, center ) );
		}
		else {
			// we rely on the _source to compute the distance
			sourceProjection.request( requestBody, context );
		}
	}

	@Override
	public E extract(ProjectionHitMapper<?, ?> projectionHitMapper, JsonObject hit,
			SearchProjectionExtractContext context) {
		Integer distanceSortIndex = multiValued ? null : context.getDistanceSortIndex( absoluteFieldPath, center );

		if ( distanceSortIndex != null ) {
			E accumulated = accumulator.createInitial();
			accumulated = accumulator.accumulate( accumulated, extractDistanceFromSortKey( hit, distanceSortIndex ) );
			return accumulated;
		}
		else if ( scriptFieldName != null ) {
			E accumulated = accumulator.createInitial();
			accumulated = accumulator.accumulate( accumulated, extractDistanceFromScriptField( hit ) );
			return accumulated;
		}
		else {
			return sourceProjection.extract( projectionHitMapper, hit, context );
		}
	}

	@Override
	public P transform(LoadingResult<?> loadingResult, E extractedData,
			SearchProjectionTransformContext context) {
		FromDocumentFieldValueConvertContext convertContext = context.getFromDocumentFieldValueConvertContext();
		return accumulator.finish( extractedData, NO_OP_DOUBLE_CONVERTER, convertContext );
	}

	private Double extractDistanceFromScriptField(JsonObject hit) {
		Optional<JsonElement> projectedFieldElement = FIELDS_ACCESSOR.property( scriptFieldName ).asArray().element( 0 ).get( hit );
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
}
