package org.hibernate.search.spatial;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.apache.lucene.document.Document;

import org.hibernate.search.bridge.FieldBridge;
import org.hibernate.search.bridge.LuceneOptions;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

import static java.util.Locale.ENGLISH;

public abstract class SpatialFieldBridge implements FieldBridge {

	private static final Log LOG = LoggerFactory.make();

	protected boolean fieldMode;
	protected String latitudeField;
	protected String longitudeField;

	@Override
	public abstract void set(String name, Object value, Document document, LuceneOptions luceneOptions);

	Double getLatitude( Object value ) {
		if ( fieldMode ) {
			Class<?> clazz = value.getClass();
			try {
				Field latitude = clazz.getField( latitudeField );
				return (Double)latitude.get( value );
			} catch ( NoSuchFieldException e )  {
				try {
					PropertyDescriptor propertyDescriptor =  new PropertyDescriptor(
							latitudeField,
							clazz,
							"get" + capitalize( latitudeField ),
							null);
					Method latitudeGetter = propertyDescriptor.getReadMethod();
					if ( latitudeGetter != null ) {
						return (Double)latitudeGetter.invoke( value, null );
					} else {
						throw LOG.cannotReadFieldForClass( latitudeField, clazz.getName() );
					}
				} catch ( IllegalAccessException ex ) {
					throw LOG.cannotReadFieldForClass( latitudeField, clazz.getName() );
				} catch ( InvocationTargetException ex ) {
					throw LOG.cannotReadFieldForClass( latitudeField, clazz.getName() );
				} catch ( IntrospectionException ex ) {
					throw LOG.cannotReadFieldForClass( latitudeField, clazz.getName() );
				}
			} catch ( IllegalAccessException e ) {
				throw LOG.cannotReadFieldForClass( latitudeField, clazz.getName() );
			}
		} else {
			try {
				Coordinates coordinates = (Coordinates) value;
				return coordinates.getLatitude();
			} catch ( ClassCastException e ) {
				throw LOG.cannotExtractCoordinateFromObject( value.getClass().getName() );
			}
		}
	}

	Double getLongitude( Object value ) {
		if ( fieldMode ) {
			Class<?> clazz = value.getClass();
			try {
				Field longitude = clazz.getField( longitudeField );
				return (Double)longitude.get( value );
			} catch ( NoSuchFieldException e )  {
				try {
					PropertyDescriptor propertyDescriptor =  new PropertyDescriptor(
							longitudeField,
							clazz,
							"get" + capitalize( longitudeField ),
							null);
					Method longitudeGetter = propertyDescriptor.getReadMethod();
					if ( longitudeGetter != null ) {
						return (Double)longitudeGetter.invoke( value, null );
					} else {
						throw LOG.cannotReadFieldForClass( latitudeField, clazz.getName() );
					}
				} catch ( IntrospectionException ex ) {
					throw LOG.cannotReadFieldForClass( latitudeField, clazz.getName() );
				} catch ( IllegalAccessException ex ) {
					throw LOG.cannotReadFieldForClass( latitudeField, clazz.getName() );
				} catch ( InvocationTargetException ex ) {
					throw LOG.cannotReadFieldForClass( latitudeField, clazz.getName() );
				}
			} catch ( IllegalAccessException e ) {
				throw LOG.cannotReadFieldForClass( latitudeField, clazz.getName() );
			}
		} else {
			try {
				Coordinates coordinates = (Coordinates) value;
				return coordinates.getLongitude();
			} catch ( ClassCastException e ) {
				throw LOG.cannotExtractCoordinateFromObject( value.getClass().getName() );
			}
		}
	}

	public static String capitalize(String name) {
		if (name == null || name.length() == 0) {
			return name;
		}
		return name.substring(0, 1).toUpperCase(ENGLISH) + name.substring(1);
	}
}
