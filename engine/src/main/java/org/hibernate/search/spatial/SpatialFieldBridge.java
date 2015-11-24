/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.spatial;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.apache.lucene.document.Document;
import org.hibernate.search.bridge.LuceneOptions;
import org.hibernate.search.bridge.MetadataProvidingFieldBridge;
import org.hibernate.search.bridge.spi.FieldMetadataBuilder;
import org.hibernate.search.bridge.spi.FieldType;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

import static java.util.Locale.ENGLISH;

public abstract class SpatialFieldBridge implements MetadataProvidingFieldBridge {

	private static final Log LOG = LoggerFactory.make();

	protected String latitudeField;
	protected String longitudeField;

	@Override
	public abstract void set(String name, Object value, Document document, LuceneOptions luceneOptions);

	protected Double getLatitude(final Object value ) {
		if ( useFieldMode() ) {
			return getCoordinateFromField( latitudeField, value );
		}
		else {
			try {
				Coordinates coordinates = (Coordinates) value;
				return coordinates.getLatitude();
			}
			catch (ClassCastException e) {
				throw LOG.cannotExtractCoordinateFromObject( value.getClass().getName() );
			}
		}
	}

	@Override
	public void configureFieldMetadata(String name, FieldMetadataBuilder builder) {
		builder.field( name, FieldType.DOUBLE )
			.sortable( true );
	}

	private Double getCoordinateFromField(String coordinateField, Object value) {
		Class<?> clazz = value.getClass();
		try {
			Field latitude = clazz.getField( coordinateField );
			return (Double) latitude.get( value );
		}
		catch (NoSuchFieldException e) {
			try {
				PropertyDescriptor propertyDescriptor = new PropertyDescriptor(
						coordinateField,
						clazz,
						"get" + capitalize( coordinateField ),
						null);
				Method latitudeGetter = propertyDescriptor.getReadMethod();
				if ( latitudeGetter != null ) {
					return (Double) latitudeGetter.invoke( value );
				}
				else {
					throw LOG.cannotReadFieldForClass( coordinateField, clazz.getName() );
				}
			}
			catch (IllegalAccessException ex) {
				throw LOG.cannotReadFieldForClass( coordinateField, clazz.getName() );
			}
			catch (InvocationTargetException ex) {
				throw LOG.cannotReadFieldForClass( coordinateField, clazz.getName() );
			}
			catch (IntrospectionException ex) {
				throw LOG.cannotReadFieldForClass( coordinateField, clazz.getName() );
			}
		}
		catch (IllegalAccessException e) {
			throw LOG.cannotReadFieldForClass( coordinateField, clazz.getName() );
		}
	}

	protected Double getLongitude(final Object value) {
		if ( useFieldMode() ) {
			return getCoordinateFromField( longitudeField, value );
		}
		else {
			try {
				Coordinates coordinates = (Coordinates) value;
				return coordinates.getLongitude();
			}
			catch (ClassCastException e) {
				throw LOG.cannotExtractCoordinateFromObject( value.getClass().getName() );
			}
		}
	}

	private boolean useFieldMode() {
		return latitudeField != null && longitudeField != null;
	}

	public static String capitalize(final String name) {
		if ( name == null || name.length() == 0 ) {
			return name;
		}
		return name.substring( 0, 1 ).toUpperCase( ENGLISH ) + name.substring( 1 );
	}
}
