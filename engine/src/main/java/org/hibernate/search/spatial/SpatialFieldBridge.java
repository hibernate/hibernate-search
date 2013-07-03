/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
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
