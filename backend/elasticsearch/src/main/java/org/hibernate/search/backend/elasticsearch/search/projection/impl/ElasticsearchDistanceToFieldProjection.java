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
import org.hibernate.search.engine.search.loading.spi.LoadingResult;
import org.hibernate.search.engine.search.loading.spi.ProjectionHitMapper;
import org.hibernate.search.engine.spatial.DistanceUnit;
import org.hibernate.search.engine.spatial.GeoPoint;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

class ElasticsearchDistanceToFieldProjection implements ElasticsearchSearchProjection<Double, Double> {

	private static final JsonObjectAccessor SCRIPT_FIELDS_ACCESSOR = JsonAccessor.root().property( "script_fields" ).asObject();
	private static final JsonObjectAccessor FIELDS_ACCESSOR = JsonAccessor.root().property( "fields" ).asObject();
	private static final JsonArrayAccessor SCORE_ACCESSOR = JsonAccessor.root().property( "sort" ).asArray();

	private static final Pattern NON_DIGITS_PATTERN = Pattern.compile( "\\D" );

	private static final String DISTANCE_PROJECTION_SCRIPT =
		// Use ".size() != 0" to check whether this field has a value. ".value != null" won't work on ES7+
		" Object result;" +
		" if (doc[params.fieldPath].size() != 0) {" +
			" result = doc[params.fieldPath].arcDistance(params.lat, params.lon);" +
		" } else {" +
			" for (int i=0; i<params.nestedPaths.length; i++) {" +
				" String nestedPath = params.nestedPaths[i];" +
				" String relativeFieldPath = params.relativeFieldPaths[i];" +
				" if (params['_source'][nestedPath] == null) continue;" +
				" if (params['_source'][nestedPath][relativeFieldPath] == null) continue;" +
				" return params['_source'][nestedPath][relativeFieldPath]" +
			" }" +
		" }" +
		" return result;";

	private final Set<String> indexNames;
	private final String absoluteFieldPath;
	private final Set<String> nestedPaths;

	private final GeoPoint center;

	private final DistanceUnit unit;

	private final String scriptFieldName;

	ElasticsearchDistanceToFieldProjection(Set<String> indexNames, String absoluteFieldPath, Set<String> nestedPaths, GeoPoint center, DistanceUnit unit) {
		this.indexNames = indexNames;
		this.absoluteFieldPath = absoluteFieldPath;
		this.nestedPaths = nestedPaths;
		this.center = center;
		this.unit = unit;
		this.scriptFieldName = createScriptFieldName( absoluteFieldPath, center, unit );
	}

	@Override
	public void contributeRequest(JsonObject requestBody, SearchProjectionExtractContext context) {
		if ( context.getDistanceSortIndex( absoluteFieldPath, center ) == null ) {
			// we rely on a script to compute the distance
			SCRIPT_FIELDS_ACCESSOR
					.property( scriptFieldName ).asObject()
					.property( "script" ).asObject()
					.set( requestBody, createScript( absoluteFieldPath, nestedPaths, center ) );
		}
	}

	@Override
	public Double extract(ProjectionHitMapper<?, ?> projectionHitMapper, JsonObject responseBody, JsonObject hit,
			SearchProjectionExtractContext context) {
		Optional<Double> distance;
		Integer distanceSortIndex = context.getDistanceSortIndex( absoluteFieldPath, center );

		if ( distanceSortIndex == null ) {
			// we extract the value from the fields computed by the script_fields
			distance = extractDistance( hit );
		}
		else {
			// we extract the value from the score element
			Optional<JsonElement> scoreDistanceElement = SCORE_ACCESSOR.element( distanceSortIndex ).get( hit );

			if ( !scoreDistanceElement.isPresent() ) {
				distance = Optional.empty();
			}
			else if ( !scoreDistanceElement.get().getAsJsonPrimitive().isNumber() ) {
				// Elasticsearch will return "Infinity" if the distance has not been computed.
				// Usually, it's because the indexed object doesn't have a location defined for this field.
				distance = Optional.empty();
			}
			else {
				distance = Optional.of( scoreDistanceElement.get().getAsJsonPrimitive().getAsDouble() );
			}
		}

		return distance.isPresent() && Double.isFinite( distance.get() ) ?
				unit.fromMeters( distance.get() ) : null;
	}

	public Optional<Double> extractDistance(JsonObject hit) {
		Optional<JsonElement> projectedFieldElement = FIELDS_ACCESSOR.property( scriptFieldName ).asArray().element( 0 ).get( hit );
		if ( !projectedFieldElement.isPresent() || projectedFieldElement.get().isJsonNull() ) {
			return Optional.empty();
		}

		if ( projectedFieldElement.get().isJsonPrimitive() ) {
			return Optional.of( projectedFieldElement.get().getAsDouble() );
		}

		JsonObject geoPoint = projectedFieldElement.get().getAsJsonObject();
		double distanceInMeters = SloppyMath.haversinMeters(
				center.getLatitude(), center.getLongitude(), geoPoint.get( "lat" ).getAsDouble(), geoPoint.get( "lon" ).getAsDouble() );

		return Optional.of( unit.fromMeters( distanceInMeters ) );
	}

	@Override
	public Double transform(LoadingResult<?> loadingResult, Double extractedData,
			SearchProjectionTransformContext context) {
		return extractedData;
	}

	@Override
	public Set<String> getIndexNames() {
		return indexNames;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder( getClass().getSimpleName() )
				.append( "[" )
				.append( "absoluteFieldPath=" ).append( absoluteFieldPath )
				.append( ", center=" ).append( center )
				.append( ", unit=" ).append( unit )
				.append( "]" );
		return sb.toString();
	}

	private static String createScriptFieldName(String absoluteFieldPath, GeoPoint center, DistanceUnit unit) {
		StringBuilder sb = new StringBuilder();
		sb.append( "distance_" )
				.append( absoluteFieldPath )
				.append( "_" )
				.append( NON_DIGITS_PATTERN.matcher( Double.toString( center.getLatitude() ) ).replaceAll( "_" ) )
				.append( "_" )
				.append( NON_DIGITS_PATTERN.matcher( Double.toString( center.getLongitude() ) ).replaceAll( "_" ) )
				.append( "_" )
				.append( unit.name() );
		return sb.toString();
	}

	private static JsonObject createScript(String absoluteFieldPath, Set<String> nestedPaths, GeoPoint center) {
		JsonArray nestedPathsProperty = new JsonArray();
		JsonArray relativeFieldPaths = new JsonArray();

		for ( String nestedPath : nestedPaths ) {
			if ( !absoluteFieldPath.startsWith( nestedPath ) ) {
				continue;
			}
			nestedPathsProperty.add( nestedPath );
			relativeFieldPaths.add( absoluteFieldPath.substring( nestedPath.length() + 1 ) );
		}

		JsonObject params = new JsonObject();
		params.addProperty( "lat", center.getLatitude() );
		params.addProperty( "lon", center.getLongitude() );
		params.addProperty( "fieldPath", absoluteFieldPath );
		params.add( "nestedPaths", nestedPathsProperty );
		params.add( "relativeFieldPaths", relativeFieldPaths );

		JsonObject scriptContent = new JsonObject();
		scriptContent.addProperty( "lang", "painless" );
		scriptContent.add( "params", params );
		scriptContent.addProperty( "source", DISTANCE_PROJECTION_SCRIPT );

		return scriptContent;
	}
}
