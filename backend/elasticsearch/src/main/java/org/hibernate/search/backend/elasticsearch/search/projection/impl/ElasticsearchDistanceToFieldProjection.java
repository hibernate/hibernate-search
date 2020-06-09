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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

class ElasticsearchDistanceToFieldProjection implements ElasticsearchSearchProjection<Double, Double> {

	private static final JsonObjectAccessor SCRIPT_FIELDS_ACCESSOR = JsonAccessor.root().property( "script_fields" ).asObject();
	private static final JsonObjectAccessor FIELDS_ACCESSOR = JsonAccessor.root().property( "fields" ).asObject();
	private static final JsonArrayAccessor SORT_ACCESSOR = JsonAccessor.root().property( "sort" ).asArray();

	private static final Pattern NON_DIGITS_PATTERN = Pattern.compile( "\\D" );

	private static final String DISTANCE_PROJECTION_SCRIPT =
		// Use ".size() != 0" to check whether this field has a value. ".value != null" won't work on ES7+
		" Object result;" +
		" if (doc[params.fieldPath].size() != 0) {" +
			" result = doc[params.fieldPath].arcDistance(params.lat, params.lon);" +
		" } else {" +
			// At the moment it seems that there is no way to apply #arcDistance on a nested object field.
			// To workaround this limit we extract the geo point JSON source
			// and we will compute the distance on client side.
			" String nestedPath = params.nestedPath;" +
			" String relativeFieldPath = params.relativeFieldPath;" +
			" if (params['_source'][nestedPath] == null) return result;" +
			" if (params['_source'][nestedPath][relativeFieldPath] == null) return result;" +
			" return params['_source'][nestedPath][relativeFieldPath]" +
		" }" +
		" return result;";

	private final Set<String> indexNames;
	private final String absoluteFieldPath;
	private final String nestedPath;

	private final GeoPoint center;
	private final DistanceUnit unit;

	private final String scriptFieldName;

	ElasticsearchDistanceToFieldProjection(Set<String> indexNames, String absoluteFieldPath, String nestedPath,
			GeoPoint center, DistanceUnit unit) {
		this.indexNames = indexNames;
		this.absoluteFieldPath = absoluteFieldPath;
		this.nestedPath = nestedPath;
		this.center = center;
		this.unit = unit;
		this.scriptFieldName = createScriptFieldName( absoluteFieldPath, center, unit );
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

	@Override
	public Set<String> getIndexNames() {
		return indexNames;
	}

	@Override
	public void request(JsonObject requestBody, SearchProjectionRequestContext context) {
		if ( context.getDistanceSortIndex( absoluteFieldPath, center ) == null ) {
			// we rely on a script to compute the distance
			SCRIPT_FIELDS_ACCESSOR
					.property( scriptFieldName ).asObject()
					.property( "script" ).asObject()
					.set( requestBody, createScript( absoluteFieldPath, nestedPath, center ) );
		}
	}

	@Override
	public Double extract(ProjectionHitMapper<?, ?> projectionHitMapper, JsonObject hit,
			SearchProjectionExtractContext context) {
		Double distance;
		Integer distanceSortIndex = context.getDistanceSortIndex( absoluteFieldPath, center );

		if ( distanceSortIndex == null ) {
			distance = extractDistanceFromScriptField( hit );
		}
		else {
			distance = extractDistanceFromSortKey( hit, distanceSortIndex );
		}

		return distance != null ? unit.fromMeters( distance ) : null;
	}

	@Override
	public Double transform(LoadingResult<?> loadingResult, Double extractedData,
			SearchProjectionTransformContext context) {
		return extractedData;
	}

	private Double extractDistanceFromScriptField(JsonObject hit) {
		Optional<JsonElement> projectedFieldElement = FIELDS_ACCESSOR.property( scriptFieldName ).asArray().element( 0 ).get( hit );
		if ( !projectedFieldElement.isPresent() || projectedFieldElement.get().isJsonNull() ) {
			return null;
		}

		if ( projectedFieldElement.get().isJsonPrimitive() ) {
			return projectedFieldElement.get().getAsDouble();
		}

		JsonObject geoPoint = projectedFieldElement.get().getAsJsonObject();
		double distanceInMeters = SloppyMath.haversinMeters(
				center.latitude(), center.longitude(), geoPoint.get( "lat" ).getAsDouble(), geoPoint.get( "lon" ).getAsDouble() );

		return unit.fromMeters( distanceInMeters );
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
		else {
			return sortKeyDistanceElement.get().getAsJsonPrimitive().getAsDouble();
		}
	}

	private static String createScriptFieldName(String absoluteFieldPath, GeoPoint center, DistanceUnit unit) {
		StringBuilder sb = new StringBuilder();
		sb.append( "distance_" )
				.append( absoluteFieldPath )
				.append( "_" )
				.append( NON_DIGITS_PATTERN.matcher( Double.toString( center.latitude() ) ).replaceAll( "_" ) )
				.append( "_" )
				.append( NON_DIGITS_PATTERN.matcher( Double.toString( center.longitude() ) ).replaceAll( "_" ) )
				.append( "_" )
				.append( unit.name() );
		return sb.toString();
	}

	private static JsonObject createScript(String absoluteFieldPath, String nestedPath, GeoPoint center) {
		String relativeFieldPath = ( nestedPath != null && absoluteFieldPath.startsWith( nestedPath ) ) ?
			absoluteFieldPath.substring( nestedPath.length() + 1 ) : null;

		JsonObject params = new JsonObject();
		params.addProperty( "lat", center.latitude() );
		params.addProperty( "lon", center.longitude() );
		params.addProperty( "fieldPath", absoluteFieldPath );
		params.addProperty( "nestedPath", nestedPath );
		params.addProperty( "relativeFieldPath", relativeFieldPath );

		JsonObject scriptContent = new JsonObject();
		scriptContent.addProperty( "lang", "painless" );
		scriptContent.add( "params", params );
		scriptContent.addProperty( "source", DISTANCE_PROJECTION_SCRIPT );

		return scriptContent;
	}
}
