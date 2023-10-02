/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.bridge.builtin.impl;

import static java.util.function.Predicate.isEqual;

import java.lang.invoke.MethodHandles;
import java.util.function.Function;
import java.util.stream.Collector;

import org.hibernate.search.annotations.Spatial;
import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.engine.backend.types.converter.FromDocumentValueConverter;
import org.hibernate.search.engine.backend.types.converter.ToDocumentValueConverter;
import org.hibernate.search.engine.backend.types.converter.runtime.FromDocumentValueConvertContext;
import org.hibernate.search.engine.backend.types.converter.runtime.ToDocumentValueConvertContext;
import org.hibernate.search.engine.backend.types.dsl.IndexFieldTypeFactory;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.mapper.pojo.bridge.PropertyBridge;
import org.hibernate.search.mapper.pojo.bridge.TypeBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.PropertyBindingContext;
import org.hibernate.search.mapper.pojo.bridge.binding.TypeBindingContext;
import org.hibernate.search.mapper.pojo.bridge.builtin.programmatic.GeoPointBinder;
import org.hibernate.search.mapper.pojo.bridge.builtin.spatial.impl.LatitudeMarker;
import org.hibernate.search.mapper.pojo.bridge.builtin.spatial.impl.LongitudeMarker;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.PropertyBinder;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.TypeBinder;
import org.hibernate.search.mapper.pojo.bridge.runtime.PropertyBridgeWriteContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.TypeBridgeWriteContext;
import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.mapper.pojo.model.PojoElementAccessor;
import org.hibernate.search.mapper.pojo.model.PojoModelCompositeElement;
import org.hibernate.search.mapper.pojo.model.PojoModelType;
import org.hibernate.search.spatial.Coordinates;
import org.hibernate.search.spatial.impl.Point;
import org.hibernate.search.util.common.impl.StreamHelper;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public class CoordinatesBridge implements TypeBridge<Object>, PropertyBridge<Object> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final Function<Object, GeoPoint> coordinatesExtractor;
	private final IndexFieldReference<GeoPoint> indexFieldReference;

	/**
	 * Private constructor, use {@link GeoPointBinder#create()} instead.
	 */
	private CoordinatesBridge(Function<Object, GeoPoint> coordinatesExtractor,
			IndexFieldReference<GeoPoint> indexFieldReference) {
		this.coordinatesExtractor = coordinatesExtractor;
		this.indexFieldReference = indexFieldReference;
	}

	@Override
	public void write(DocumentElement target, Object bridgedElement, TypeBridgeWriteContext context) {
		doWrite( target, bridgedElement );
	}

	@Override
	public void write(DocumentElement target, Object bridgedElement, PropertyBridgeWriteContext context) {
		doWrite( target, bridgedElement );
	}

	private void doWrite(DocumentElement target, Object bridgedElement) {
		GeoPoint coordinates = coordinatesExtractor.apply( bridgedElement );
		if ( coordinates != null ) {
			target.addValue( indexFieldReference, coordinates );
		}
	}

	@Override
	public void close() {
		// Nothing to do
	}

	public static class Binder implements TypeBinder, PropertyBinder {

		private String fieldName;
		private Projectable projectable = Projectable.DEFAULT;
		private String markerSet;

		public Binder fieldName(String fieldName) {
			this.fieldName = fieldName;
			return this;
		}

		public Binder projectable(Projectable projectable) {
			this.projectable = projectable;
			return this;
		}

		public Binder markerSet(String markerSet) {
			this.markerSet = markerSet;
			return this;
		}

		@Override
		public void bind(TypeBindingContext context) {
			String defaultedFieldName;
			if ( fieldName != null && !fieldName.isEmpty() ) {
				defaultedFieldName = fieldName;
			}
			else {
				defaultedFieldName = Spatial.COORDINATES_DEFAULT_FIELD;
			}

			PojoModelType bridgedElement = context.bridgedElement();
			Function<Object, GeoPoint> coordinatesExtractor;
			if ( bridgedElement.isAssignableTo( Coordinates.class ) ) {
				// Search 5 behavior: ignore @Latitude/@Longitude in this case
				coordinatesExtractor = coord -> Coordinates.toGeoPoint( (Coordinates) coord );
				context.dependencies()
						.use( "latitude" )
						.use( "longitude" );
			}
			else {
				coordinatesExtractor = createCoordinatesExtractorUsingMarkers( defaultedFieldName, bridgedElement );
			}

			CoordinatesBridge bridge = doBind(
					defaultedFieldName,
					context.typeFactory(),
					context.indexSchemaElement(),
					coordinatesExtractor
			);
			context.bridge( bridge );
		}

		@Override
		public void bind(PropertyBindingContext context) {
			String defaultedFieldName;
			if ( fieldName != null && !fieldName.isEmpty() ) {
				defaultedFieldName = fieldName;
			}
			else {
				defaultedFieldName = context.bridgedElement().name();
			}

			Function<Object, GeoPoint> coordinatesExtractor = createCoordinatesExtractorUsingMarkers(
					defaultedFieldName, context.bridgedElement() );

			CoordinatesBridge bridge = doBind(
					defaultedFieldName,
					context.typeFactory(),
					context.indexSchemaElement(),
					coordinatesExtractor
			);
			context.bridge( bridge );
		}

		private CoordinatesBridge doBind(String defaultedFieldName, IndexFieldTypeFactory typeFactory,
				IndexSchemaElement indexSchemaElement,
				Function<Object, GeoPoint> coordinatesExtractor) {
			IndexFieldReference<GeoPoint> indexFieldReference = indexSchemaElement.field(
					defaultedFieldName,
					typeFactory.asGeoPoint().projectable( projectable )
							.sortable( Sortable.YES )
							.dslConverter( Coordinates.class, CoordinatesConverter.INSTANCE )
							.projectionConverter( Coordinates.class, CoordinatesConverter.INSTANCE )
							.toIndexFieldType()
			)
					.toReference();

			return new CoordinatesBridge(
					coordinatesExtractor,
					indexFieldReference
			);
		}

		private Function<Object, GeoPoint> createCoordinatesExtractorUsingMarkers(
				String defaultedFieldName, PojoModelCompositeElement bridgedElement) {
			PojoElementAccessor<Double> latitudeAccessor = bridgedElement.properties().stream()
					.filter( model -> model.markers( LatitudeMarker.class ).stream()
							.map( LatitudeMarker::getMarkerSet )
							.anyMatch( isEqual( markerSet ) ) )
					.collect( singleMarkedProperty( "latitude", defaultedFieldName, markerSet ) )
					.createAccessor( Double.class );
			PojoElementAccessor<Double> longitudeAccessor = bridgedElement.properties().stream()
					.filter( model -> model.markers( LongitudeMarker.class ).stream()
							.map( LongitudeMarker::getMarkerSet )
							.anyMatch( isEqual( markerSet ) ) )
					.collect( singleMarkedProperty( "longitude", defaultedFieldName, markerSet ) )
					.createAccessor( Double.class );

			return source -> {
				Double latitude = latitudeAccessor.read( source );
				Double longitude = longitudeAccessor.read( source );

				if ( latitude == null || longitude == null ) {
					return null;
				}

				return GeoPoint.of( latitude, longitude );
			};
		}

		private static Collector<PojoModelCompositeElement, ?, PojoModelCompositeElement> singleMarkedProperty(
				String markerName, String fieldName, String markerSet) {
			return StreamHelper.singleElement(
					() -> log.unableToFindLongitudeOrLatitudeProperty( markerName, fieldName, markerSet ),
					() -> log.multipleLatitudeOrLongitudeProperties( markerName, fieldName, markerSet )
			);
		}
	}

	private static class CoordinatesConverter
			implements ToDocumentValueConverter<Coordinates, GeoPoint>,
			FromDocumentValueConverter<GeoPoint, Coordinates> {
		static final CoordinatesConverter INSTANCE = new CoordinatesConverter();

		private CoordinatesConverter() {
		}

		@Override
		public Coordinates fromDocumentValue(GeoPoint value, FromDocumentValueConvertContext context) {
			return value == null ? null : Point.fromDegrees( value.latitude(), value.longitude() );
		}

		@Override
		public GeoPoint toDocumentValue(Coordinates value, ToDocumentValueConvertContext context) {
			return Coordinates.toGeoPoint( value );
		}
	}
}
