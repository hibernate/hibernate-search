/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge.builtin.spatial;

import java.lang.invoke.MethodHandles;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collector;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldAccessor;
import org.hibernate.search.engine.backend.document.model.IndexSchemaElement;
import org.hibernate.search.engine.backend.document.model.Store;
import org.hibernate.search.engine.backend.spatial.GeoPoint;
import org.hibernate.search.engine.backend.spatial.ImmutableGeoPoint;
import org.hibernate.search.engine.common.spi.BuildContext;
import org.hibernate.search.engine.mapper.model.SearchModel;
import org.hibernate.search.mapper.pojo.bridge.PropertyBridge;
import org.hibernate.search.mapper.pojo.bridge.TypeBridge;
import org.hibernate.search.mapper.pojo.bridge.mapping.AnnotationBridgeBuilder;
import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.mapper.pojo.model.PojoElement;
import org.hibernate.search.mapper.pojo.model.PojoModelElement;
import org.hibernate.search.mapper.pojo.model.PojoModelElementAccessor;
import org.hibernate.search.mapper.pojo.model.PojoModelProperty;
import org.hibernate.search.mapper.pojo.model.PojoModelType;
import org.hibernate.search.util.SearchException;
import org.hibernate.search.util.impl.common.StreamHelper;
import org.hibernate.search.util.impl.common.LoggerFactory;

/**
 * @author Yoann Rodiere
 */
public class GeoPointBridge implements TypeBridge, PropertyBridge {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	public static class Builder implements
			AnnotationBridgeBuilder<GeoPointBridge, org.hibernate.search.mapper.pojo.bridge.builtin.spatial.annotation.GeoPointBridge> {

		private String fieldName;
		private Store store = Store.DEFAULT;
		private String markerSet;

		@Override
		public void initialize(
				org.hibernate.search.mapper.pojo.bridge.builtin.spatial.annotation.GeoPointBridge annotation) {
			fieldName( annotation.fieldName() );
			markerSet( annotation.markerSet() );
			store( annotation.store() );
		}

		public Builder fieldName(String fieldName) {
			this.fieldName = fieldName;
			return this;
		}

		public Builder store(Store store) {
			this.store = store;
			return this;
		}

		public Builder markerSet(String markerSet) {
			this.markerSet = markerSet;
			return this;
		}

		@Override
		public GeoPointBridge build(BuildContext buildContext) {
			return new GeoPointBridge( fieldName, store, markerSet );
		}

	}

	private final String fieldName;
	private final Store store;
	private final String markerSet;

	private IndexFieldAccessor<GeoPoint> fieldAccessor;
	private Function<PojoElement, GeoPoint> coordinatesExtractor;

	/*
	 * Private constructor, use the Builder instead.
	 */
	private GeoPointBridge(String fieldName, Store store, String markerSet) {
		this.fieldName = fieldName;
		this.store = store;
		this.markerSet = markerSet;
	}

	@Override
	public void bind(IndexSchemaElement indexSchemaElement, PojoModelType bridgedPojoModelType,
			SearchModel searchModel) {
		if ( fieldName == null || fieldName.isEmpty() ) {
			throw log.missingFieldNameForGeoPointBridgeOnType( bridgedPojoModelType.toString() );
		}

		bind( fieldName, indexSchemaElement, bridgedPojoModelType );
	}

	@Override
	public void bind(IndexSchemaElement indexSchemaElement, PojoModelProperty bridgedPojoModelProperty,
			SearchModel searchModel) {
		String defaultedFieldName;
		if ( fieldName != null && !fieldName.isEmpty() ) {
			defaultedFieldName = fieldName;
		}
		else {
			defaultedFieldName = bridgedPojoModelProperty.getName();
		}

		bind( defaultedFieldName, indexSchemaElement, bridgedPojoModelProperty );
	}

	private void bind(String defaultedFieldName, IndexSchemaElement indexSchemaElement,
			PojoModelElement bridgedPojoModelElement) {
		fieldAccessor = indexSchemaElement.field( defaultedFieldName ).asGeoPoint().store( store ).createAccessor();

		if ( bridgedPojoModelElement.isAssignableTo( GeoPoint.class ) ) {
			PojoModelElementAccessor<GeoPoint> sourceAccessor = bridgedPojoModelElement.createAccessor( GeoPoint.class );
			coordinatesExtractor = sourceAccessor::read;
		}
		else {
			PojoModelElementAccessor<Double> latitudeAccessor = bridgedPojoModelElement.properties()
					.filter( model -> model.markers( LatitudeMarker.class )
							.anyMatch( m -> Objects.equals( markerSet, m.getMarkerSet() ) ) )
					.collect( singleMarkedProperty( "latitude", defaultedFieldName, markerSet ) )
					.createAccessor( Double.class );
			PojoModelElementAccessor<Double> longitudeAccessor = bridgedPojoModelElement.properties()
					.filter( model -> model.markers( LongitudeMarker.class )
							.anyMatch( m -> Objects.equals( markerSet, m.getMarkerSet() ) ) )
					.collect( singleMarkedProperty( "longitude", defaultedFieldName, markerSet ) )
					.createAccessor( Double.class );

			coordinatesExtractor = bridgedElement -> {
				Double latitude = latitudeAccessor.read( bridgedElement );
				Double longitude = longitudeAccessor.read( bridgedElement );

				if ( latitude == null || longitude == null ) {
					return null;
				}

				return new ImmutableGeoPoint( latitude, longitude );
			};
		}
	}

	private static Collector<PojoModelElement, ?, PojoModelElement> singleMarkedProperty(
			String markerName, String fieldName, String markerSet) {
		return StreamHelper.singleElement(
				() -> new SearchException( "Could not find a property with the " + markerName
						+ " marker for field '" + fieldName + "' (marker set: '" + markerSet + "')" ),
				() -> new SearchException( "Found multiple properties with the " + markerName
						+ " marker for field '" + fieldName + "' (marker set: '" + markerSet + "')" )
				);
	}

	@Override
	public void write(DocumentElement target, PojoElement source) {
		GeoPoint coordinates = coordinatesExtractor.apply( source );
		fieldAccessor.write( target, coordinates );
	}

	@Override
	public void close() {
		// Nothing to do
	}
}
