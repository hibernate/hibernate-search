/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.projection.impl;

import java.util.Optional;
import java.util.regex.Pattern;

import org.hibernate.search.backend.elasticsearch.gson.impl.JsonAccessor;
import org.hibernate.search.backend.elasticsearch.gson.impl.JsonObjectAccessor;
import org.hibernate.search.engine.search.query.spi.ProjectionHitCollector;
import org.hibernate.search.engine.spatial.DistanceUnit;
import org.hibernate.search.engine.spatial.GeoPoint;

import com.google.gson.JsonObject;

public class DistanceFieldSearchProjectionImpl implements ElasticsearchSearchProjection<Double> {

	private static final JsonObjectAccessor SCRIPT_FIELDS_ACCESSOR = JsonAccessor.root().property( "script_fields" ).asObject();
	private static final JsonObjectAccessor FIELDS_ACCESSOR = JsonAccessor.root().property( "fields" ).asObject();

	private static final Pattern NON_DIGITS_PATTERN = Pattern.compile( "\\D" );

	private final String absoluteFieldPath;

	private final GeoPoint center;

	private final DistanceUnit unit;

	private final String scriptFieldName;

	DistanceFieldSearchProjectionImpl(String absoluteFieldPath, GeoPoint center, DistanceUnit unit) {
		this.absoluteFieldPath = absoluteFieldPath;
		this.center = center;
		this.unit = unit;
		this.scriptFieldName = createScriptFieldName( absoluteFieldPath, center, unit );
	}

	@Override
	public void contributeRequest(JsonObject requestBody) {
		SCRIPT_FIELDS_ACCESSOR
				.property( scriptFieldName ).asObject()
				.property( "script" ).asObject()
				.set( requestBody, createScript( absoluteFieldPath, center ) );
	}

	@Override
	public void extract(ProjectionHitCollector collector, JsonObject responseBody, JsonObject hit) {
		Optional<Double> distance = FIELDS_ACCESSOR.property( scriptFieldName ).asArray()
				.element( 0 )
				.asDouble().get( hit );

		collector.collectProjection( distance.isPresent() && Double.isFinite( distance.get() ) ?
				unit.fromMeters( distance.get() ) : null );
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

	private static JsonObject createScript(String absoluteFieldPath, GeoPoint center) {
		JsonObject jsonCenter = new JsonObject();
		jsonCenter.addProperty( "lat", center.getLatitude() );
		jsonCenter.addProperty( "lon", center.getLongitude() );

		JsonObject scriptContent = new JsonObject();
		scriptContent.addProperty( "lang", "painless" );
		scriptContent.add( "params", jsonCenter );
		scriptContent.addProperty( "inline", "doc['" + absoluteFieldPath + "'].value !== null ?"
				+ " doc['" + absoluteFieldPath + "'].arcDistance(params.lat, params.lon)"
				+ " : null" );

		return scriptContent;
	}
}
