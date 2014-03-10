/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010-2014, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.search.bridge.impl;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.URL;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.hibernate.search.engine.service.classloading.spi.ClassLoadingException;
import org.hibernate.search.engine.service.spi.ServiceManager;
import org.hibernate.search.exception.AssertionFailure;
import org.hibernate.annotations.common.reflection.ReflectionManager;
import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.annotations.common.reflection.XMember;
import org.hibernate.search.annotations.ClassBridge;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.NumericField;
import org.hibernate.search.annotations.Parameter;
import org.hibernate.search.annotations.Resolution;
import org.hibernate.search.annotations.Spatial;
import org.hibernate.search.annotations.SpatialMode;
import org.hibernate.search.bridge.AppliedOnTypeAwareBridge;
import org.hibernate.search.bridge.FieldBridge;
import org.hibernate.search.bridge.ParameterizedBridge;
import org.hibernate.search.bridge.TwoWayFieldBridge;
import org.hibernate.search.bridge.TwoWayStringBridge;
import org.hibernate.search.bridge.builtin.BigDecimalBridge;
import org.hibernate.search.bridge.builtin.BigIntegerBridge;
import org.hibernate.search.bridge.builtin.BooleanBridge;
import org.hibernate.search.bridge.builtin.CalendarBridge;
import org.hibernate.search.bridge.builtin.CharacterBridge;
import org.hibernate.search.bridge.builtin.DateBridge;
import org.hibernate.search.bridge.builtin.DoubleBridge;
import org.hibernate.search.bridge.builtin.EnumBridge;
import org.hibernate.search.bridge.builtin.FloatBridge;
import org.hibernate.search.bridge.builtin.IntegerBridge;
import org.hibernate.search.bridge.builtin.LongBridge;
import org.hibernate.search.bridge.builtin.NumericFieldBridge;
import org.hibernate.search.bridge.builtin.ShortBridge;
import org.hibernate.search.bridge.builtin.StringBridge;
import org.hibernate.search.bridge.builtin.UUIDBridge;
import org.hibernate.search.bridge.builtin.UriBridge;
import org.hibernate.search.bridge.builtin.UrlBridge;
import org.hibernate.search.bridge.builtin.impl.BuiltinArrayBridge;
import org.hibernate.search.bridge.builtin.impl.BuiltinIterableBridge;
import org.hibernate.search.bridge.builtin.impl.BuiltinMapBridge;
import org.hibernate.search.bridge.builtin.impl.String2FieldBridgeAdaptor;
import org.hibernate.search.bridge.builtin.impl.TwoWayString2FieldBridgeAdaptor;
import org.hibernate.search.spatial.SpatialFieldBridgeByHash;
import org.hibernate.search.spatial.SpatialFieldBridgeByRange;
import org.hibernate.search.util.impl.ClassLoaderHelper;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * This factory is responsible for creating and initializing build-in and custom <i>FieldBridges</i>.
 *
 * @author Emmanuel Bernard
 * @author John Griffin
 */
public final class BridgeFactory {
	private static final Log LOG = LoggerFactory.make();

	public final TwoWayFieldBridge CHARACTER;
	public final TwoWayFieldBridge DOUBLE;
	public final TwoWayFieldBridge FLOAT;
	public final TwoWayFieldBridge SHORT;
	public final TwoWayFieldBridge INTEGER;
	public final TwoWayFieldBridge LONG;
	public final TwoWayFieldBridge BIG_INTEGER;
	public final TwoWayFieldBridge BIG_DECIMAL;
	public final TwoWayFieldBridge STRING;
	public final TwoWayFieldBridge BOOLEAN;
	public final TwoWayFieldBridge CLAZZ;
	public final TwoWayFieldBridge Url;
	public final TwoWayFieldBridge Uri;
	public final TwoWayFieldBridge UUID;

	public final FieldBridge DATE_YEAR;
	public final FieldBridge DATE_MONTH;
	public final FieldBridge DATE_DAY;
	public final FieldBridge DATE_HOUR;
	public final FieldBridge DATE_MINUTE;
	public final FieldBridge DATE_SECOND;

	public final FieldBridge ITERABLE_DATE_YEAR;
	public final FieldBridge ITERABLE_DATE_MONTH;
	public final FieldBridge ITERABLE_DATE_DAY;
	public final FieldBridge ITERABLE_DATE_HOUR;
	public final FieldBridge ITERABLE_DATE_MINUTE;
	public final FieldBridge ITERABLE_DATE_SECOND;

	public final FieldBridge MAP_DATE_YEAR;
	public final FieldBridge MAP_DATE_MONTH;
	public final FieldBridge MAP_DATE_DAY;
	public final FieldBridge MAP_DATE_HOUR;
	public final FieldBridge MAP_DATE_MINUTE;
	public final FieldBridge MAP_DATE_SECOND;

	public final FieldBridge ARRAY_DATE_YEAR;
	public final FieldBridge ARRAY_DATE_MONTH;
	public final FieldBridge ARRAY_DATE_DAY;
	public final FieldBridge ARRAY_DATE_HOUR;
	public final FieldBridge ARRAY_DATE_MINUTE;
	public final FieldBridge ARRAY_DATE_SECOND;

	public final FieldBridge CALENDAR_YEAR;
	public final FieldBridge CALENDAR_MONTH;
	public final FieldBridge CALENDAR_DAY;
	public final FieldBridge CALENDAR_HOUR;
	public final FieldBridge CALENDAR_MINUTE;
	public final FieldBridge CALENDAR_SECOND;

	public final FieldBridge ITERABLE_CALENDAR_YEAR;
	public final FieldBridge ITERABLE_CALENDAR_MONTH;
	public final FieldBridge ITERABLE_CALENDAR_DAY;
	public final FieldBridge ITERABLE_CALENDAR_HOUR;
	public final FieldBridge ITERABLE_CALENDAR_MINUTE;
	public final FieldBridge ITERABLE_CALENDAR_SECOND;

	public final FieldBridge MAP_CALENDAR_YEAR;
	public final FieldBridge MAP_CALENDAR_MONTH;
	public final FieldBridge MAP_CALENDAR_DAY;
	public final FieldBridge MAP_CALENDAR_HOUR;
	public final FieldBridge MAP_CALENDAR_MINUTE;
	public final FieldBridge MAP_CALENDAR_SECOND;

	public final FieldBridge ARRAY_CALENDAR_YEAR;
	public final FieldBridge ARRAY_CALENDAR_MONTH;
	public final FieldBridge ARRAY_CALENDAR_DAY;
	public final FieldBridge ARRAY_CALENDAR_HOUR;
	public final FieldBridge ARRAY_CALENDAR_MINUTE;
	public final FieldBridge ARRAY_CALENDAR_SECOND;

	public final FieldBridge ITERABLE_BRIDGE;
	public final BuiltinIterableBridge ITERABLE_BRIDGE_DOUBLE;
	public final BuiltinIterableBridge ITERABLE_BRIDGE_FLOAT;
	public final BuiltinIterableBridge ITERABLE_BRIDGE_INT;
	public final BuiltinIterableBridge ITERABLE_BRIDGE_LONG;

	public final FieldBridge ARRAY_BRIDGE;
	public final BuiltinArrayBridge ARRAY_BRIDGE_DOUBLE;
	public final BuiltinArrayBridge ARRAY_BRIDGE_FLOAT;
	public final BuiltinArrayBridge ARRAY_BRIDGE_INT;
	public final BuiltinArrayBridge ARRAY_BRIDGE_LONG;

	public final FieldBridge MAP_BRIDGE;
	public final BuiltinMapBridge MAP_BRIDGE_DOUBLE;
	public final BuiltinMapBridge MAP_BRIDGE_FLOAT;
	public final BuiltinMapBridge MAP_BRIDGE_INT;
	public final BuiltinMapBridge MAP_BRIDGE_LONG;

	public final TwoWayFieldBridge DATE_MILLISECOND;

	public final FieldBridge ARRAY_DATE_MILLISECOND;
	public final FieldBridge ITERABLE_DATE_MILLISECOND;
	public final FieldBridge MAP_DATE_MILLISECOND;

	public final TwoWayFieldBridge CALENDAR_MILLISECOND;

	public final FieldBridge ARRAY_CALENDAR_MILLISECOND;
	public final FieldBridge ITERABLE_CALENDAR_MILLISECOND;
	public final FieldBridge MAP_CALENDAR_MILLISECOND;

	public final String TIKA_BRIDGE_NAME = "org.hibernate.search.bridge.builtin.TikaBridge";
	public final String TIKA_BRIDGE_METADATA_PROCESSOR_SETTER = "setMetadataProcessorClass";
	public final String TIKA_BRIDGE_PARSE_CONTEXT_SETTER = "setParseContextProviderClass";

	private final Map<String, FieldBridge> builtInBridges;
	private final Map<String, NumericFieldBridge> numericBridges;
	private final Map<String, BuiltinMapBridge> numericMapBridges;
	private final Map<String, BuiltinArrayBridge> numericArrayBridges;
	private final Map<String, BuiltinIterableBridge> numericIterableBridges;

	public BridgeFactory(ServiceManager serviceManager) {
		CHARACTER = new TwoWayString2FieldBridgeAdaptor( new CharacterBridge() );
		DOUBLE = new TwoWayString2FieldBridgeAdaptor( new DoubleBridge() );
		FLOAT = new TwoWayString2FieldBridgeAdaptor( new FloatBridge() );
		SHORT = new TwoWayString2FieldBridgeAdaptor( new ShortBridge() );
		INTEGER = new TwoWayString2FieldBridgeAdaptor( new IntegerBridge() );
		LONG = new TwoWayString2FieldBridgeAdaptor( new LongBridge() );
		BIG_INTEGER = new TwoWayString2FieldBridgeAdaptor( new BigIntegerBridge() );
		BIG_DECIMAL = new TwoWayString2FieldBridgeAdaptor( new BigDecimalBridge() );
		STRING = new TwoWayString2FieldBridgeAdaptor( new StringBridge() );
		BOOLEAN = new TwoWayString2FieldBridgeAdaptor( new BooleanBridge() );
		CLAZZ = new TwoWayString2FieldBridgeAdaptor( new org.hibernate.search.bridge.builtin.ClassBridge(serviceManager) );
		Url = new TwoWayString2FieldBridgeAdaptor( new UrlBridge() );
		Uri = new TwoWayString2FieldBridgeAdaptor( new UriBridge() );
		UUID = new TwoWayString2FieldBridgeAdaptor( new UUIDBridge() );

		DATE_YEAR = new TwoWayString2FieldBridgeAdaptor( DateBridge.DATE_YEAR );
		DATE_MONTH = new TwoWayString2FieldBridgeAdaptor( DateBridge.DATE_MONTH );
		DATE_DAY = new TwoWayString2FieldBridgeAdaptor( DateBridge.DATE_DAY );
		DATE_HOUR = new TwoWayString2FieldBridgeAdaptor( DateBridge.DATE_HOUR );
		DATE_MINUTE = new TwoWayString2FieldBridgeAdaptor( DateBridge.DATE_MINUTE );
		DATE_SECOND = new TwoWayString2FieldBridgeAdaptor( DateBridge.DATE_SECOND );

		ITERABLE_DATE_YEAR = new BuiltinIterableBridge( DATE_YEAR );
		ITERABLE_DATE_MONTH = new BuiltinIterableBridge( DATE_MONTH );
		ITERABLE_DATE_DAY = new BuiltinIterableBridge( DATE_DAY );
		ITERABLE_DATE_HOUR = new BuiltinIterableBridge( DATE_HOUR );
		ITERABLE_DATE_MINUTE = new BuiltinIterableBridge( DATE_MINUTE );
		ITERABLE_DATE_SECOND = new BuiltinIterableBridge( DATE_SECOND );

		MAP_DATE_YEAR = new BuiltinMapBridge( DATE_YEAR );
		MAP_DATE_MONTH = new BuiltinMapBridge( DATE_MONTH );
		MAP_DATE_DAY = new BuiltinMapBridge( DATE_DAY );
		MAP_DATE_HOUR = new BuiltinMapBridge( DATE_HOUR );
		MAP_DATE_MINUTE = new BuiltinMapBridge( DATE_MINUTE );
		MAP_DATE_SECOND = new BuiltinMapBridge( DATE_SECOND );

		ARRAY_DATE_YEAR = new BuiltinArrayBridge( DATE_YEAR );
		ARRAY_DATE_MONTH = new BuiltinArrayBridge( DATE_MONTH );
		ARRAY_DATE_DAY = new BuiltinArrayBridge( DATE_DAY );
		ARRAY_DATE_HOUR = new BuiltinArrayBridge( DATE_HOUR );
		ARRAY_DATE_MINUTE = new BuiltinArrayBridge( DATE_MINUTE );
		ARRAY_DATE_SECOND = new BuiltinArrayBridge( DATE_SECOND );

		CALENDAR_YEAR = new TwoWayString2FieldBridgeAdaptor( CalendarBridge.CALENDAR_YEAR );
		CALENDAR_MONTH = new TwoWayString2FieldBridgeAdaptor( CalendarBridge.CALENDAR_MONTH );
		CALENDAR_DAY = new TwoWayString2FieldBridgeAdaptor( CalendarBridge.CALENDAR_DAY );
		CALENDAR_HOUR = new TwoWayString2FieldBridgeAdaptor( CalendarBridge.CALENDAR_HOUR );
		CALENDAR_MINUTE = new TwoWayString2FieldBridgeAdaptor( CalendarBridge.CALENDAR_MINUTE );
		CALENDAR_SECOND = new TwoWayString2FieldBridgeAdaptor( CalendarBridge.CALENDAR_SECOND );

		ITERABLE_CALENDAR_YEAR = new BuiltinIterableBridge( CALENDAR_YEAR );
		ITERABLE_CALENDAR_MONTH = new BuiltinIterableBridge( CALENDAR_MONTH );
		ITERABLE_CALENDAR_DAY = new BuiltinIterableBridge( CALENDAR_DAY );
		ITERABLE_CALENDAR_HOUR = new BuiltinIterableBridge( CALENDAR_HOUR );
		ITERABLE_CALENDAR_MINUTE = new BuiltinIterableBridge( CALENDAR_MINUTE );
		ITERABLE_CALENDAR_SECOND = new BuiltinIterableBridge( CALENDAR_SECOND );

		MAP_CALENDAR_YEAR = new BuiltinMapBridge( CALENDAR_YEAR );
		MAP_CALENDAR_MONTH = new BuiltinMapBridge( CALENDAR_MONTH );
		MAP_CALENDAR_DAY = new BuiltinMapBridge( CALENDAR_DAY );
		MAP_CALENDAR_HOUR = new BuiltinMapBridge( CALENDAR_HOUR );
		MAP_CALENDAR_MINUTE = new BuiltinMapBridge( CALENDAR_MINUTE );
		MAP_CALENDAR_SECOND = new BuiltinMapBridge( CALENDAR_SECOND );

		ARRAY_CALENDAR_YEAR = new BuiltinArrayBridge( CALENDAR_YEAR );
		ARRAY_CALENDAR_MONTH = new BuiltinArrayBridge( CALENDAR_MONTH );
		ARRAY_CALENDAR_DAY = new BuiltinArrayBridge( CALENDAR_DAY );
		ARRAY_CALENDAR_HOUR = new BuiltinArrayBridge( CALENDAR_HOUR );
		ARRAY_CALENDAR_MINUTE = new BuiltinArrayBridge( CALENDAR_MINUTE );
		ARRAY_CALENDAR_SECOND = new BuiltinArrayBridge( CALENDAR_SECOND );

		ITERABLE_BRIDGE = new BuiltinIterableBridge();
		ITERABLE_BRIDGE_DOUBLE = new BuiltinIterableBridge( NumericFieldBridge.DOUBLE_FIELD_BRIDGE );
		ITERABLE_BRIDGE_FLOAT = new BuiltinIterableBridge( NumericFieldBridge.FLOAT_FIELD_BRIDGE );
		ITERABLE_BRIDGE_INT = new BuiltinIterableBridge( NumericFieldBridge.INT_FIELD_BRIDGE );
		ITERABLE_BRIDGE_LONG = new BuiltinIterableBridge( NumericFieldBridge.LONG_FIELD_BRIDGE );

		ARRAY_BRIDGE = new BuiltinArrayBridge();
		ARRAY_BRIDGE_DOUBLE = new BuiltinArrayBridge( NumericFieldBridge.DOUBLE_FIELD_BRIDGE );
		ARRAY_BRIDGE_FLOAT = new BuiltinArrayBridge( NumericFieldBridge.FLOAT_FIELD_BRIDGE );
		ARRAY_BRIDGE_INT = new BuiltinArrayBridge( NumericFieldBridge.INT_FIELD_BRIDGE );
		ARRAY_BRIDGE_LONG = new BuiltinArrayBridge( NumericFieldBridge.LONG_FIELD_BRIDGE );

		MAP_BRIDGE = new BuiltinMapBridge();
		MAP_BRIDGE_DOUBLE = new BuiltinMapBridge( NumericFieldBridge.DOUBLE_FIELD_BRIDGE );
		MAP_BRIDGE_FLOAT = new BuiltinMapBridge( NumericFieldBridge.FLOAT_FIELD_BRIDGE );
		MAP_BRIDGE_INT = new BuiltinMapBridge( NumericFieldBridge.INT_FIELD_BRIDGE );
		MAP_BRIDGE_LONG = new BuiltinMapBridge( NumericFieldBridge.LONG_FIELD_BRIDGE );

		DATE_MILLISECOND = new TwoWayString2FieldBridgeAdaptor(DateBridge.DATE_MILLISECOND );

		ARRAY_DATE_MILLISECOND = new BuiltinArrayBridge( DATE_MILLISECOND );
		ITERABLE_DATE_MILLISECOND = new BuiltinIterableBridge( DATE_MILLISECOND );
		MAP_DATE_MILLISECOND = new BuiltinMapBridge( DATE_MILLISECOND );

		CALENDAR_MILLISECOND = new TwoWayString2FieldBridgeAdaptor(CalendarBridge.CALENDAR_MILLISECOND );

		ARRAY_CALENDAR_MILLISECOND = new BuiltinArrayBridge( CALENDAR_MILLISECOND );
		ITERABLE_CALENDAR_MILLISECOND = new BuiltinIterableBridge( CALENDAR_MILLISECOND );
		MAP_CALENDAR_MILLISECOND = new BuiltinMapBridge( CALENDAR_MILLISECOND );

		builtInBridges = new HashMap<String, FieldBridge>();
		builtInBridges.put( Character.class.getName(), CHARACTER );
		builtInBridges.put( char.class.getName(), CHARACTER );
		builtInBridges.put( Double.class.getName(), DOUBLE );
		builtInBridges.put( double.class.getName(), DOUBLE );
		builtInBridges.put( Float.class.getName(), FLOAT );
		builtInBridges.put( float.class.getName(), FLOAT );
		builtInBridges.put( Short.class.getName(), SHORT );
		builtInBridges.put( short.class.getName(), SHORT );
		builtInBridges.put( Integer.class.getName(), INTEGER );
		builtInBridges.put( int.class.getName(), INTEGER );
		builtInBridges.put( Long.class.getName(), LONG );
		builtInBridges.put( long.class.getName(), LONG );
		builtInBridges.put( BigInteger.class.getName(), BIG_INTEGER );
		builtInBridges.put( BigDecimal.class.getName(), BIG_DECIMAL );
		builtInBridges.put( String.class.getName(), STRING );
		builtInBridges.put( Boolean.class.getName(), BOOLEAN );
		builtInBridges.put( boolean.class.getName(), BOOLEAN );
		builtInBridges.put( Class.class.getName(), CLAZZ );
		builtInBridges.put( URL.class.getName(), Url );
		builtInBridges.put( URI.class.getName(), Uri );
		builtInBridges.put( UUID.class.getName(), UUID );

		builtInBridges.put( Date.class.getName(), DATE_MILLISECOND );
		builtInBridges.put( Calendar.class.getName(), CALENDAR_MILLISECOND );

		numericBridges = new HashMap<String, NumericFieldBridge>();
		numericBridges.put( Integer.class.getName(), NumericFieldBridge.INT_FIELD_BRIDGE );
		numericBridges.put( int.class.getName(), NumericFieldBridge.INT_FIELD_BRIDGE );
		numericBridges.put( Long.class.getName(), NumericFieldBridge.LONG_FIELD_BRIDGE );
		numericBridges.put( long.class.getName(), NumericFieldBridge.LONG_FIELD_BRIDGE );
		numericBridges.put( Double.class.getName(), NumericFieldBridge.DOUBLE_FIELD_BRIDGE );
		numericBridges.put( double.class.getName(), NumericFieldBridge.DOUBLE_FIELD_BRIDGE );
		numericBridges.put( Float.class.getName(), NumericFieldBridge.FLOAT_FIELD_BRIDGE );
		numericBridges.put( float.class.getName(), NumericFieldBridge.FLOAT_FIELD_BRIDGE );

		numericMapBridges = new HashMap<String, BuiltinMapBridge>();
		numericMapBridges.put( Integer.class.getName(), MAP_BRIDGE_INT );
		numericMapBridges.put( int.class.getName(), MAP_BRIDGE_INT );
		numericMapBridges.put( Long.class.getName(), MAP_BRIDGE_LONG );
		numericMapBridges.put( long.class.getName(), MAP_BRIDGE_LONG );
		numericMapBridges.put( Double.class.getName(), MAP_BRIDGE_DOUBLE );
		numericMapBridges.put( double.class.getName(), MAP_BRIDGE_DOUBLE );
		numericMapBridges.put( Float.class.getName(), MAP_BRIDGE_FLOAT );
		numericMapBridges.put( float.class.getName(), MAP_BRIDGE_FLOAT );

		numericArrayBridges = new HashMap<String, BuiltinArrayBridge>();
		numericArrayBridges.put( Integer.class.getName(), ARRAY_BRIDGE_INT );
		numericArrayBridges.put( int.class.getName(), ARRAY_BRIDGE_INT );
		numericArrayBridges.put( Long.class.getName(), ARRAY_BRIDGE_LONG );
		numericArrayBridges.put( long.class.getName(), ARRAY_BRIDGE_LONG );
		numericArrayBridges.put( Double.class.getName(), ARRAY_BRIDGE_DOUBLE );
		numericArrayBridges.put( double.class.getName(), ARRAY_BRIDGE_DOUBLE );
		numericArrayBridges.put( Float.class.getName(), ARRAY_BRIDGE_FLOAT );
		numericArrayBridges.put( float.class.getName(), ARRAY_BRIDGE_FLOAT );

		numericIterableBridges = new HashMap<String, BuiltinIterableBridge>();
		numericIterableBridges.put( Integer.class.getName(), ITERABLE_BRIDGE_INT );
		numericIterableBridges.put( int.class.getName(), ITERABLE_BRIDGE_INT );
		numericIterableBridges.put( Long.class.getName(), ITERABLE_BRIDGE_LONG );
		numericIterableBridges.put( long.class.getName(), ITERABLE_BRIDGE_LONG );
		numericIterableBridges.put( Double.class.getName(), ITERABLE_BRIDGE_DOUBLE );
		numericIterableBridges.put( double.class.getName(), ITERABLE_BRIDGE_DOUBLE );
		numericIterableBridges.put( Float.class.getName(), ITERABLE_BRIDGE_FLOAT );
		numericIterableBridges.put( float.class.getName(), ITERABLE_BRIDGE_FLOAT );
	}

	/**
	 * This extracts and instantiates the implementation class from a {@code ClassBridge} annotation.
	 *
	 * @param cb the class bridge annotation
	 * @param clazz the {@code Class} on which the annotation is defined on
	 * @return Returns the specified {@code FieldBridge} instance
	 */
	public FieldBridge extractType(ClassBridge cb, Class<?> clazz) {
		FieldBridge bridge = null;
		Class<?> bridgeType = null;

		if ( cb != null ) {
			bridgeType = cb.impl();
			if ( bridgeType != null ) {
				try {
					Object instance = bridgeType.newInstance();
					if ( FieldBridge.class.isAssignableFrom( bridgeType ) ) {
						bridge = (FieldBridge) instance;
					}
					else if ( org.hibernate.search.bridge.TwoWayStringBridge.class.isAssignableFrom( bridgeType ) ) {
						bridge = new TwoWayString2FieldBridgeAdaptor(
								(org.hibernate.search.bridge.TwoWayStringBridge) instance
						);
					}
					else if ( org.hibernate.search.bridge.StringBridge.class.isAssignableFrom( bridgeType ) ) {
						bridge = new String2FieldBridgeAdaptor( (org.hibernate.search.bridge.StringBridge) instance );
					}
					else {
						throw LOG.noFieldBridgeInterfaceImplementedByClassBridge( bridgeType.getName() );
					}
				}
				catch (Exception e) {
					throw LOG.cannotInstantiateClassBridgeOfType( bridgeType.getName(), clazz.getName(), e );
				}
			}
		}
		if ( bridge == null ) {
			throw LOG.unableToDetermineClassBridge( ClassBridge.class.getName() );
		}

		populateReturnType( clazz, bridgeType, bridge );

		return bridge;
	}

	/**
	 * Injects any parameters configured via the given {@code ClassBridge} annotation into the given object, in case
	 * this is a {@link ParameterizedBridge}.
	 *
	 * @param classBridgeConfiguration the parameter source
	 * @param classBridge the object to inject the parameters into
	 */
	public void injectParameters(ClassBridge classBridgeConfiguration, Object classBridge) {
		if ( classBridgeConfiguration.params().length > 0 && ParameterizedBridge.class.isAssignableFrom( classBridge.getClass() ) ) {
			Map<String, String> params = new HashMap<String, String>( classBridgeConfiguration.params().length );
			for ( Parameter param : classBridgeConfiguration.params() ) {
				params.put( param.name(), param.value() );
			}
			( (ParameterizedBridge) classBridge ).setParameterValues( params );
		}
	}

	/**
	 * This instantiates the SpatialFieldBridge from a {@code Spatial} annotation.
	 *
	 * @param spatial the {@code Spatial} annotation
	 * @param clazz the {@code XClass} on which the annotation is defined on
	 * @return Returns the {@code SpatialFieldBridge} instance
	 * @param latitudeField a {@link java.lang.String} object.
	 * @param longitudeField a {@link java.lang.String} object.
	 */
	public FieldBridge buildSpatialBridge(Spatial spatial, XClass clazz, String latitudeField, String longitudeField) {
		FieldBridge bridge;
		try {
			bridge = buildSpatialBridge( spatial, latitudeField, longitudeField );
		}
		catch (Exception e) {
			throw LOG.unableToInstantiateSpatial( clazz.getName(), e );
		}
		if ( bridge == null ) {
			throw LOG.unableToInstantiateSpatial( clazz.getName(), null );
		}

		return bridge;
	}

	/**
	 * This instantiates the SpatialFieldBridge from a {@code Spatial} annotation.
	 *
	 * @param spatial the {@code Spatial} annotation
	 * @param member the {@code XMember} on which the annotation is defined on
	 * @return Returns the {@code SpatialFieldBridge} instance
	 */
	public FieldBridge buildSpatialBridge(Spatial spatial, XMember member) {
		FieldBridge bridge;
		try {
			bridge = buildSpatialBridge( spatial, null, null );
		}
		catch (Exception e) {
			throw LOG.unableToInstantiateSpatial( member.getName(), e );
		}
		if ( bridge == null ) {
			throw LOG.unableToInstantiateSpatial( member.getName(), null );
		}

		return bridge;
	}

	/**
	 * This instantiates the SpatialFieldBridge from a {@code Spatial} annotation.
	 *
	 * @param spatial the {@code Spatial} annotation
	 * @return Returns the {@code SpatialFieldBridge} instance
	 * @param latitudeField a {@link java.lang.String} object.
	 * @param longitudeField a {@link java.lang.String} object.
	 */
	public FieldBridge buildSpatialBridge(Spatial spatial, String latitudeField, String longitudeField) {
		FieldBridge bridge = null;
		if ( spatial != null ) {
			if ( spatial.spatialMode() == SpatialMode.HASH ) {
				if ( latitudeField != null && longitudeField != null ) {
					bridge = new SpatialFieldBridgeByHash( spatial.topSpatialHashLevel(), spatial.bottomSpatialHashLevel(), latitudeField, longitudeField );
				}
				else {
					bridge = new SpatialFieldBridgeByHash( spatial.topSpatialHashLevel(), spatial.bottomSpatialHashLevel() );
				}
			}
			else {
				if ( latitudeField != null && longitudeField != null ) {
					bridge = new SpatialFieldBridgeByRange( latitudeField, longitudeField );
				}
				else {
					bridge = new SpatialFieldBridgeByRange();
				}
			}
		}

		return bridge;
	}

	public FieldBridge guessType(Field field,
			NumericField numericField,
			XMember member,
			ReflectionManager reflectionManager,
			ServiceManager serviceManager
	) {
		FieldBridge bridge;
		org.hibernate.search.annotations.FieldBridge bridgeAnn;
		//@Field bridge has priority over @FieldBridge
		if ( field != null && void.class != field.bridge().impl() ) {
			bridgeAnn = field.bridge();
		}
		else {
			bridgeAnn = member.getAnnotation( org.hibernate.search.annotations.FieldBridge.class );
		}
		if ( bridgeAnn != null ) {
			bridge = doExtractType( bridgeAnn, member, reflectionManager );
		}
		else if ( member.isAnnotationPresent( org.hibernate.search.annotations.DateBridge.class ) ) {
			Resolution resolution = member.getAnnotation( org.hibernate.search.annotations.DateBridge.class )
					.resolution();
			bridge = guessDateFieldBridge( member, reflectionManager, resolution );
		}
		else if ( member.isAnnotationPresent( org.hibernate.search.annotations.CalendarBridge.class ) ) {
			Resolution resolution = member.getAnnotation( org.hibernate.search.annotations.CalendarBridge.class )
					.resolution();
			bridge = guessCalendarFieldBridge( member, reflectionManager, resolution );
		}
		else if ( member.isAnnotationPresent( org.hibernate.search.annotations.TikaBridge.class ) ) {
			org.hibernate.search.annotations.TikaBridge annotation = member.getAnnotation( org.hibernate.search.annotations.TikaBridge.class );
			bridge = createTikaBridge( annotation, serviceManager );
		}
		else if ( numericField != null ) {
			bridge = guessNumericFieldBridge( member, reflectionManager );
		}
		else if ( member.isAnnotationPresent( org.hibernate.search.annotations.Spatial.class ) ) {
			Spatial spatialAnn = member.getAnnotation( org.hibernate.search.annotations.Spatial.class );
			bridge = buildSpatialBridge( spatialAnn, member );
		}
		else {
			//find in built-ins
			XClass returnType = member.getType();
			bridge = builtInBridges.get( returnType.getName() );
			if ( bridge == null && returnType.isEnum() ) {
				//we return one enum type bridge instance per property as it is customized per ReturnType
				final EnumBridge enumBridge = new EnumBridge();
				populateReturnType( reflectionManager.toClass( member.getType() ), EnumBridge.class, enumBridge );
				bridge = new TwoWayString2FieldBridgeAdaptor( enumBridge );
			}
			if ( bridge == null && isAnnotatedWithIndexEmbedded( member ) ) {
				bridge = guessEmbeddedFieldBridge( member, reflectionManager );
			}
		}
		if ( bridge == null ) {
			throw LOG.unableToGuessFieldBridge( member.getType().getName(), member.getName() );
		}
		return bridge;
	}

	private FieldBridge createTikaBridge(org.hibernate.search.annotations.TikaBridge annotation, ServiceManager serviceManager) {
		Class<?> tikaBridgeClass;
		FieldBridge tikaBridge;
		try {
			tikaBridgeClass = ClassLoaderHelper.classForName( TIKA_BRIDGE_NAME, serviceManager);
			tikaBridge = ClassLoaderHelper.instanceFromClass( FieldBridge.class, tikaBridgeClass, "Tika bridge" );
		}
		catch (ClassLoadingException e) {
			throw new AssertionFailure( "Unable to find Tika bridge class: " + TIKA_BRIDGE_NAME );
		}

		Class<?> tikaMetadataProcessorClass = annotation.metadataProcessor();
		if ( tikaMetadataProcessorClass != void.class ) {
			configureTikaBridgeParameters(
					tikaBridgeClass,
					TIKA_BRIDGE_METADATA_PROCESSOR_SETTER,
					tikaBridge,
					tikaMetadataProcessorClass
			);
		}

		Class<?> tikaParseContextProviderClass = annotation.parseContextProvider();
		if ( tikaParseContextProviderClass != void.class ) {
			configureTikaBridgeParameters(
					tikaBridgeClass,
					TIKA_BRIDGE_PARSE_CONTEXT_SETTER,
					tikaBridge,
					tikaParseContextProviderClass
			);
		}

		return tikaBridge;
	}

	private void configureTikaBridgeParameters(Class<?> tikaBridgeClass, String setter, Object tikaBridge, Class<?> clazz) {
		try {
			Method m = tikaBridgeClass.getMethod( setter, Class.class );
			m.invoke( tikaBridge, clazz );
		}
		catch (Exception e) {
			throw LOG.unableToConfigureTikaBridge( TIKA_BRIDGE_NAME, e );
		}
	}

	private FieldBridge guessEmbeddedFieldBridge(XMember member, ReflectionManager reflectionManager) {
		if ( isIterable( reflectionManager, member ) ) {
			return ITERABLE_BRIDGE;
		}

		if ( member.isArray() ) {
			return ARRAY_BRIDGE;
		}

		if ( isMap( member ) ) {
			return MAP_BRIDGE;
		}

		return null;
	}

	private FieldBridge guessNumericFieldBridge(XMember member, ReflectionManager reflectionManager) {
		if ( isNotAnnotatedWithIndexEmbedded( member ) ) {
			return numericBridges.get( member.getType().getName() );
		}

		if ( isIterable( reflectionManager, member ) ) {
			return numericIterableBridges.get( member.getElementClass().getName() );
		}

		if ( member.isArray() ) {
			return numericArrayBridges.get( member.getElementClass().getName() );
		}

		if ( isMap( member ) ) {
			return numericMapBridges.get( member.getElementClass().getName() );
		}

		return null;
	}

	private FieldBridge guessCalendarFieldBridge(XMember member, ReflectionManager reflectionManager, Resolution resolution) {
		if ( isNotAnnotatedWithIndexEmbedded( member ) ) {
			return getCalendarField( resolution );
		}

		if ( isIterable( reflectionManager, member ) ) {
			return getIterableCalendarField( resolution );
		}

		if ( member.isArray() ) {
			return getArrayCalendarField( resolution );
		}

		if ( isMap( member ) ) {
			return getMapCalendarField( resolution );
		}

		return null;
	}

	private FieldBridge guessDateFieldBridge(XMember member, ReflectionManager reflectionManager, Resolution resolution) {
		if ( isNotAnnotatedWithIndexEmbedded( member ) ) {
			return getDateField( resolution );
		}

		if ( isIterable( reflectionManager, member ) ) {
			return getIterableDateField( resolution );
		}

		if ( member.isArray() ) {
			return getArrayDateField( resolution );
		}

		if ( isMap( member ) ) {
			return getMapDateField( resolution );
		}

		return null;
	}

	private boolean isNotAnnotatedWithIndexEmbedded(XMember member) {
		return !isAnnotatedWithIndexEmbedded( member );
	}

	private boolean isAnnotatedWithIndexEmbedded(XMember member) {
		return member.isAnnotationPresent( org.hibernate.search.annotations.IndexedEmbedded.class );
	}

	private boolean isIterable(ReflectionManager reflectionManager, XMember member) {
		Class<?> typeClass = reflectionManager.toClass( member.getType() );
		return Iterable.class.isAssignableFrom( typeClass );
	}

	private boolean isMap(XMember member) {
		return member.isCollection() && Map.class.equals( member.getCollectionClass() );
	}

	private FieldBridge doExtractType(
			org.hibernate.search.annotations.FieldBridge bridgeAnn,
			XMember member,
			ReflectionManager reflectionManager) {
		return doExtractType( bridgeAnn, member.getName(), reflectionManager.toClass( member.getType() ) );
	}

	private FieldBridge doExtractType(
			org.hibernate.search.annotations.FieldBridge bridgeAnn,
			String appliedOnName,
			Class<?> appliedOnType) {
		assert bridgeAnn != null : "@FieldBridge instance cannot be null";
		FieldBridge bridge;
		Class<?> impl = bridgeAnn.impl();
		if ( impl == void.class ) {
			throw LOG.noImplementationClassInFieldBridge( appliedOnName );
		}
		try {
			Object instance = impl.newInstance();
			if ( FieldBridge.class.isAssignableFrom( impl ) ) {
				bridge = (FieldBridge) instance;
			}
			else if ( TwoWayStringBridge.class.isAssignableFrom( impl ) ) {
				bridge = new TwoWayString2FieldBridgeAdaptor(
						(TwoWayStringBridge) instance
				);
			}
			else if ( org.hibernate.search.bridge.StringBridge.class.isAssignableFrom( impl ) ) {
				bridge = new String2FieldBridgeAdaptor( (org.hibernate.search.bridge.StringBridge) instance );
			}
			else {
				throw LOG.noFieldBridgeInterfaceImplementedByFieldBridge( impl.getName(), appliedOnName );
			}
			if ( bridgeAnn.params().length > 0 && ParameterizedBridge.class.isAssignableFrom( impl ) ) {
				Map<String, String> params = new HashMap<String, String>( bridgeAnn.params().length );
				for ( Parameter param : bridgeAnn.params() ) {
					params.put( param.name(), param.value() );
				}
				( (ParameterizedBridge) instance ).setParameterValues( params );
			}
			populateReturnType( appliedOnType, impl, instance );
		}
		catch (Exception e) {
			throw LOG.unableToInstantiateFieldBridge( appliedOnName, appliedOnType.getName(), e );
		}
		return bridge;
	}

	private void populateReturnType(Class<?> appliedOnType, Class<?> bridgeType, Object bridgeInstance) {
		if ( AppliedOnTypeAwareBridge.class.isAssignableFrom( bridgeType ) ) {
			( (AppliedOnTypeAwareBridge) bridgeInstance ).setAppliedOnType( appliedOnType );
		}
	}

	public FieldBridge getDateField(Resolution resolution) {
		switch ( resolution ) {
			case YEAR:
				return DATE_YEAR;
			case MONTH:
				return DATE_MONTH;
			case DAY:
				return DATE_DAY;
			case HOUR:
				return DATE_HOUR;
			case MINUTE:
				return DATE_MINUTE;
			case SECOND:
				return DATE_SECOND;
			case MILLISECOND:
				return DATE_MILLISECOND;
			default:
				throw LOG.unknownResolution( resolution.toString() );
		}
	}

	public FieldBridge getArrayDateField(Resolution resolution) {
		switch ( resolution ) {
			case YEAR:
				return ARRAY_DATE_YEAR;
			case MONTH:
				return ARRAY_DATE_MONTH;
			case DAY:
				return ARRAY_DATE_DAY;
			case HOUR:
				return ARRAY_DATE_HOUR;
			case MINUTE:
				return ARRAY_DATE_MINUTE;
			case SECOND:
				return ARRAY_DATE_SECOND;
			case MILLISECOND:
				return ARRAY_DATE_MILLISECOND;
			default:
				throw LOG.unknownArrayBridgeForResolution( resolution.toString() );
		}
	}

	public FieldBridge getMapDateField(Resolution resolution) {
		switch ( resolution ) {
			case YEAR:
				return MAP_DATE_YEAR;
			case MONTH:
				return MAP_DATE_MONTH;
			case DAY:
				return MAP_DATE_DAY;
			case HOUR:
				return MAP_DATE_HOUR;
			case MINUTE:
				return MAP_DATE_MINUTE;
			case SECOND:
				return MAP_DATE_SECOND;
			case MILLISECOND:
				return MAP_DATE_MILLISECOND;
			default:
				throw LOG.unknownMapBridgeForResolution( resolution.toString() );
		}
	}

	public FieldBridge getIterableDateField(Resolution resolution) {
		switch ( resolution ) {
			case YEAR:
				return ITERABLE_DATE_YEAR;
			case MONTH:
				return ITERABLE_DATE_MONTH;
			case DAY:
				return ITERABLE_DATE_DAY;
			case HOUR:
				return ITERABLE_DATE_HOUR;
			case MINUTE:
				return ITERABLE_DATE_MINUTE;
			case SECOND:
				return ITERABLE_DATE_SECOND;
			case MILLISECOND:
				return ITERABLE_DATE_MILLISECOND;
			default:
				throw LOG.unknownIterableBridgeForResolution( resolution.toString() );
		}
	}

	public FieldBridge getCalendarField(Resolution resolution) {
		switch ( resolution ) {
			case YEAR:
				return CALENDAR_YEAR;
			case MONTH:
				return CALENDAR_MONTH;
			case DAY:
				return CALENDAR_DAY;
			case HOUR:
				return CALENDAR_HOUR;
			case MINUTE:
				return CALENDAR_MINUTE;
			case SECOND:
				return CALENDAR_SECOND;
			case MILLISECOND:
				return CALENDAR_MILLISECOND;
			default:
				throw LOG.unknownResolution( resolution.toString() );
		}
	}

	public FieldBridge getArrayCalendarField(Resolution resolution) {
		switch ( resolution ) {
			case YEAR:
				return ARRAY_CALENDAR_YEAR;
			case MONTH:
				return ARRAY_CALENDAR_MONTH;
			case DAY:
				return ARRAY_CALENDAR_DAY;
			case HOUR:
				return ARRAY_CALENDAR_HOUR;
			case MINUTE:
				return ARRAY_CALENDAR_MINUTE;
			case SECOND:
				return ARRAY_CALENDAR_SECOND;
			case MILLISECOND:
				return ARRAY_CALENDAR_MILLISECOND;
			default:
				throw LOG.unknownArrayBridgeForResolution( resolution.toString() );
		}
	}

	public FieldBridge getMapCalendarField(Resolution resolution) {
		switch ( resolution ) {
			case YEAR:
				return MAP_CALENDAR_YEAR;
			case MONTH:
				return MAP_CALENDAR_MONTH;
			case DAY:
				return MAP_CALENDAR_DAY;
			case HOUR:
				return MAP_CALENDAR_HOUR;
			case MINUTE:
				return MAP_CALENDAR_MINUTE;
			case SECOND:
				return MAP_CALENDAR_SECOND;
			case MILLISECOND:
				return MAP_CALENDAR_MILLISECOND;
			default:
				throw LOG.unknownMapBridgeForResolution( resolution.toString() );
		}
	}

	public FieldBridge getIterableCalendarField(Resolution resolution) {
		switch ( resolution ) {
			case YEAR:
				return ITERABLE_CALENDAR_YEAR;
			case MONTH:
				return ITERABLE_CALENDAR_MONTH;
			case DAY:
				return ITERABLE_CALENDAR_DAY;
			case HOUR:
				return ITERABLE_CALENDAR_HOUR;
			case MINUTE:
				return ITERABLE_CALENDAR_MINUTE;
			case SECOND:
				return ITERABLE_CALENDAR_SECOND;
			case MILLISECOND:
				return ITERABLE_CALENDAR_MILLISECOND;
			default:
				throw LOG.unknownIterableBridgeForResolution( resolution.toString() );
		}
	}

	/**
	 * Takes in a fieldBridge and will return you a TwoWayFieldBridge instance.
	 *
	 * @param fieldBridge the field bridge annotation
	 * @param appliedOnType the type the bridge is applied on
	 * @param reflectionManager The reflection manager instance
	 * @return a TwoWayFieldBridge instance if the Field Bridge is an instance of a TwoWayFieldBridge.
	 * @throws org.hibernate.search.SearchException if the FieldBridge passed in is not an instance of a TwoWayFieldBridge.
	 */
	public TwoWayFieldBridge extractTwoWayType(org.hibernate.search.annotations.FieldBridge fieldBridge,
													XClass appliedOnType,
													ReflectionManager reflectionManager) {
		FieldBridge fb = extractType( fieldBridge, appliedOnType, reflectionManager );
		if ( fb instanceof TwoWayFieldBridge ) {
			return (TwoWayFieldBridge) fb;
		}
		else {
			throw LOG.fieldBridgeNotAnInstanceof( TwoWayFieldBridge.class.getSimpleName() );
		}
	}

	/**
	 * This extracts and instantiates the implementation class from a ClassBridge
	 * annotation.
	 *
	 * @param fieldBridgeAnnotation the FieldBridge annotation
	 * @param appliedOnType the type the bridge is applied on
	 * @param reflectionManager The reflection manager instance
	 * @return FieldBridge
	 */
	public FieldBridge extractType(org.hibernate.search.annotations.FieldBridge fieldBridgeAnnotation,
										XClass appliedOnType,
										ReflectionManager reflectionManager) {
		FieldBridge bridge = null;

		if ( fieldBridgeAnnotation != null ) {
			bridge = doExtractType(
					fieldBridgeAnnotation,
					appliedOnType.getName(),
					reflectionManager.toClass( appliedOnType )
			);
		}

		if ( bridge == null ) {
			throw LOG.unableToDetermineClassBridge( appliedOnType.getName() );
		}

		return bridge;
	}
}
