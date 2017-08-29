/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.document.model.impl;

import org.hibernate.search.backend.elasticsearch.document.impl.DeferredInitializationIndexFieldReference;
import org.hibernate.search.backend.elasticsearch.document.impl.ElasticsearchIndexFieldReference;
import org.hibernate.search.backend.elasticsearch.document.model.impl.esnative.DataType;
import org.hibernate.search.backend.elasticsearch.document.model.impl.esnative.PropertyMapping;
import org.hibernate.search.backend.elasticsearch.gson.impl.JsonAccessor;
import org.hibernate.search.engine.bridge.builtin.spatial.GeoPoint;

import com.google.gson.JsonObject;

/**
 * @author Yoann Rodiere
 */
class CoordinatesFieldModelContext extends AbstractScalarFieldModelContext<GeoPoint> {

	private final JsonAccessor<JsonObject> accessor;

	public CoordinatesFieldModelContext(JsonAccessor<JsonObject> accessor) {
		this.accessor = accessor;
	}

	@Override
	protected void build(DeferredInitializationIndexFieldReference<GeoPoint> reference, PropertyMapping mapping) {
		super.build( reference, mapping );
		reference.initialize( new GeoPointElasticsearchIndexFieldReference( accessor ) );
		mapping.setType( DataType.GEO_POINT );
	}

	private static class GeoPointElasticsearchIndexFieldReference extends ElasticsearchIndexFieldReference<GeoPoint, JsonObject> {

		protected GeoPointElasticsearchIndexFieldReference(JsonAccessor<JsonObject> accessor) {
			super( accessor );
		}

		@Override
		protected JsonObject convert(GeoPoint value) {
			if ( value == null ) {
				return null;
			}
			JsonObject result = new JsonObject();
			result.addProperty( "lat", value.getLatitude() );
			result.addProperty( "lon", value.getLongitude() );
			return result;
		}

	}
}
