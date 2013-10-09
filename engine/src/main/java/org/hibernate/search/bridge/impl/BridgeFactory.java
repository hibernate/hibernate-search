/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat, Inc. and/or its affiliates or third-party contributors as
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
import org.hibernate.search.bridge.builtin.DoubleNumericFieldBridge;
import org.hibernate.search.bridge.builtin.EnumBridge;
import org.hibernate.search.bridge.builtin.FloatBridge;
import org.hibernate.search.bridge.builtin.FloatNumericFieldBridge;
import org.hibernate.search.bridge.builtin.IntegerBridge;
import org.hibernate.search.bridge.builtin.IntegerNumericFieldBridge;
import org.hibernate.search.bridge.builtin.LongBridge;
import org.hibernate.search.bridge.builtin.LongNumericFieldBridge;
import org.hibernate.search.bridge.builtin.NumericFieldBridge;
import org.hibernate.search.bridge.builtin.ShortBridge;
import org.hibernate.search.bridge.builtin.StringBridge;
import org.hibernate.search.bridge.builtin.UUIDBridge;
import org.hibernate.search.bridge.builtin.UriBridge;
import org.hibernate.search.bridge.builtin.UrlBridge;
import org.hibernate.search.bridge.builtin.impl.BuiltinArrayBridge;
import org.hibernate.search.bridge.builtin.impl.BuiltinIterableBridge;
import org.hibernate.search.bridge.builtin.impl.BuiltinMapBridge;
import org.hibernate.search.bridge.builtin.impl.BuiltinNumericArrayBridge;
import org.hibernate.search.bridge.builtin.impl.BuiltinNumericIterableBridge;
import org.hibernate.search.bridge.builtin.impl.BuiltinNumericMapBridge;
import org.hibernate.search.bridge.builtin.impl.String2FieldBridgeAdaptor;
import org.hibernate.search.bridge.builtin.impl.TwoWayString2FieldBridgeAdaptor;
import org.hibernate.search.spatial.SpatialFieldBridgeByQuadTree;
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

	public static final TwoWayFieldBridge CHARACTER = new TwoWayString2FieldBridgeAdaptor( new CharacterBridge() );
	public static final TwoWayFieldBridge DOUBLE = new TwoWayString2FieldBridgeAdaptor( new DoubleBridge() );
	public static final TwoWayFieldBridge FLOAT = new TwoWayString2FieldBridgeAdaptor( new FloatBridge() );
	public static final TwoWayFieldBridge SHORT = new TwoWayString2FieldBridgeAdaptor( new ShortBridge() );
	public static final TwoWayFieldBridge INTEGER = new TwoWayString2FieldBridgeAdaptor( new IntegerBridge() );
	public static final TwoWayFieldBridge LONG = new TwoWayString2FieldBridgeAdaptor( new LongBridge() );
	public static final TwoWayFieldBridge BIG_INTEGER = new TwoWayString2FieldBridgeAdaptor( new BigIntegerBridge() );
	public static final TwoWayFieldBridge BIG_DECIMAL = new TwoWayString2FieldBridgeAdaptor( new BigDecimalBridge() );
	public static final TwoWayFieldBridge STRING = new TwoWayString2FieldBridgeAdaptor( new StringBridge() );
	public static final TwoWayFieldBridge BOOLEAN = new TwoWayString2FieldBridgeAdaptor( new BooleanBridge() );
	public static final TwoWayFieldBridge CLAZZ = new TwoWayString2FieldBridgeAdaptor( new org.hibernate.search.bridge.builtin.ClassBridge() );
	public static final TwoWayFieldBridge Url = new TwoWayString2FieldBridgeAdaptor( new UrlBridge() );
	public static final TwoWayFieldBridge Uri = new TwoWayString2FieldBridgeAdaptor( new UriBridge() );
	public static final TwoWayFieldBridge UUID = new TwoWayString2FieldBridgeAdaptor( new UUIDBridge() );

	public static final FieldBridge DATE_YEAR = new TwoWayString2FieldBridgeAdaptor( DateBridge.DATE_YEAR );
	public static final FieldBridge DATE_MONTH = new TwoWayString2FieldBridgeAdaptor( DateBridge.DATE_MONTH );
	public static final FieldBridge DATE_DAY = new TwoWayString2FieldBridgeAdaptor( DateBridge.DATE_DAY );
	public static final FieldBridge DATE_HOUR = new TwoWayString2FieldBridgeAdaptor( DateBridge.DATE_HOUR );
	public static final FieldBridge DATE_MINUTE = new TwoWayString2FieldBridgeAdaptor( DateBridge.DATE_MINUTE );
	public static final FieldBridge DATE_SECOND = new TwoWayString2FieldBridgeAdaptor( DateBridge.DATE_SECOND );

	public static final FieldBridge ITERABLE_DATE_YEAR = new BuiltinIterableBridge( DATE_YEAR );
	public static final FieldBridge ITERABLE_DATE_MONTH = new BuiltinIterableBridge( DATE_MONTH );
	public static final FieldBridge ITERABLE_DATE_DAY = new BuiltinIterableBridge( DATE_DAY );
	public static final FieldBridge ITERABLE_DATE_HOUR = new BuiltinIterableBridge( DATE_HOUR );
	public static final FieldBridge ITERABLE_DATE_MINUTE = new BuiltinIterableBridge( DATE_MINUTE );
	public static final FieldBridge ITERABLE_DATE_SECOND = new BuiltinIterableBridge( DATE_SECOND );

	public static final FieldBridge MAP_DATE_YEAR = new BuiltinMapBridge( DATE_YEAR );
	public static final FieldBridge MAP_DATE_MONTH = new BuiltinMapBridge( DATE_MONTH );
	public static final FieldBridge MAP_DATE_DAY = new BuiltinMapBridge( DATE_DAY );
	public static final FieldBridge MAP_DATE_HOUR = new BuiltinMapBridge( DATE_HOUR );
	public static final FieldBridge MAP_DATE_MINUTE = new BuiltinMapBridge( DATE_MINUTE );
	public static final FieldBridge MAP_DATE_SECOND = new BuiltinMapBridge( DATE_SECOND );

	public static final FieldBridge ARRAY_DATE_YEAR = new BuiltinArrayBridge( DATE_YEAR );
	public static final FieldBridge ARRAY_DATE_MONTH = new BuiltinArrayBridge( DATE_MONTH );
	public static final FieldBridge ARRAY_DATE_DAY = new BuiltinArrayBridge( DATE_DAY );
	public static final FieldBridge ARRAY_DATE_HOUR = new BuiltinArrayBridge( DATE_HOUR );
	public static final FieldBridge ARRAY_DATE_MINUTE = new BuiltinArrayBridge( DATE_MINUTE );
	public static final FieldBridge ARRAY_DATE_SECOND = new BuiltinArrayBridge( DATE_SECOND );

	public static final FieldBridge CALENDAR_YEAR = new TwoWayString2FieldBridgeAdaptor( CalendarBridge.CALENDAR_YEAR );
	public static final FieldBridge CALENDAR_MONTH = new TwoWayString2FieldBridgeAdaptor( CalendarBridge.CALENDAR_MONTH );
	public static final FieldBridge CALENDAR_DAY = new TwoWayString2FieldBridgeAdaptor( CalendarBridge.CALENDAR_DAY );
	public static final FieldBridge CALENDAR_HOUR = new TwoWayString2FieldBridgeAdaptor( CalendarBridge.CALENDAR_HOUR );
	public static final FieldBridge CALENDAR_MINUTE = new TwoWayString2FieldBridgeAdaptor( CalendarBridge.CALENDAR_MINUTE );
	public static final FieldBridge CALENDAR_SECOND = new TwoWayString2FieldBridgeAdaptor( CalendarBridge.CALENDAR_SECOND );

	public static final FieldBridge ITERABLE_CALENDAR_YEAR = new BuiltinIterableBridge( CALENDAR_YEAR );
	public static final FieldBridge ITERABLE_CALENDAR_MONTH = new BuiltinIterableBridge( CALENDAR_MONTH );
	public static final FieldBridge ITERABLE_CALENDAR_DAY = new BuiltinIterableBridge( CALENDAR_DAY );
	public static final FieldBridge ITERABLE_CALENDAR_HOUR = new BuiltinIterableBridge( CALENDAR_HOUR );
	public static final FieldBridge ITERABLE_CALENDAR_MINUTE = new BuiltinIterableBridge( CALENDAR_MINUTE );
	public static final FieldBridge ITERABLE_CALENDAR_SECOND = new BuiltinIterableBridge( CALENDAR_SECOND );

	public static final FieldBridge MAP_CALENDAR_YEAR = new BuiltinMapBridge( CALENDAR_YEAR );
	public static final FieldBridge MAP_CALENDAR_MONTH = new BuiltinMapBridge( CALENDAR_MONTH );
	public static final FieldBridge MAP_CALENDAR_DAY = new BuiltinMapBridge( CALENDAR_DAY );
	public static final FieldBridge MAP_CALENDAR_HOUR = new BuiltinMapBridge( CALENDAR_HOUR );
	public static final FieldBridge MAP_CALENDAR_MINUTE = new BuiltinMapBridge( CALENDAR_MINUTE );
	public static final FieldBridge MAP_CALENDAR_SECOND = new BuiltinMapBridge( CALENDAR_SECOND );

	public static final FieldBridge ARRAY_CALENDAR_YEAR = new BuiltinArrayBridge( CALENDAR_YEAR );
	public static final FieldBridge ARRAY_CALENDAR_MONTH = new BuiltinArrayBridge( CALENDAR_MONTH );
	public static final FieldBridge ARRAY_CALENDAR_DAY = new BuiltinArrayBridge( CALENDAR_DAY );
	public static final FieldBridge ARRAY_CALENDAR_HOUR = new BuiltinArrayBridge( CALENDAR_HOUR );
	public static final FieldBridge ARRAY_CALENDAR_MINUTE = new BuiltinArrayBridge( CALENDAR_MINUTE );
	public static final FieldBridge ARRAY_CALENDAR_SECOND = new BuiltinArrayBridge( CALENDAR_SECOND );

	public static final FieldBridge ITERABLE_BRIDGE = new BuiltinIterableBridge();
	public static final FieldBridge NUMERIC_ITERABLE_BRIDGE = new BuiltinNumericIterableBridge();

	public static final FieldBridge ARRAY_BRIDGE = new BuiltinArrayBridge();
	public static final FieldBridge NUMERIC_ARRAY_BRIDGE = new BuiltinNumericArrayBridge();

	public static final FieldBridge MAP_BRIDGE = new BuiltinMapBridge();
	public static final FieldBridge NUMERIC_MAP_BRIDGE = new BuiltinNumericMapBridge();

	public static final NumericFieldBridge INT_NUMERIC = new IntegerNumericFieldBridge();
	public static final NumericFieldBridge LONG_NUMERIC = new LongNumericFieldBridge();
	public static final NumericFieldBridge FLOAT_NUMERIC = new FloatNumericFieldBridge();
	public static final NumericFieldBridge DOUBLE_NUMERIC = new DoubleNumericFieldBridge();

	public static final TwoWayFieldBridge DATE_MILLISECOND = new TwoWayString2FieldBridgeAdaptor(
			DateBridge.DATE_MILLISECOND );

	public static final FieldBridge ARRAY_DATE_MILLISECOND = new BuiltinArrayBridge( DATE_MILLISECOND );
	public static final FieldBridge ITERABLE_DATE_MILLISECOND = new BuiltinIterableBridge( DATE_MILLISECOND );
	public static final FieldBridge MAP_DATE_MILLISECOND = new BuiltinMapBridge( DATE_MILLISECOND );

	public static final TwoWayFieldBridge CALENDAR_MILLISECOND = new TwoWayString2FieldBridgeAdaptor(
			CalendarBridge.CALENDAR_MILLISECOND );

	public static final FieldBridge ARRAY_CALENDAR_MILLISECOND = new BuiltinArrayBridge( CALENDAR_MILLISECOND );
	public static final FieldBridge ITERABLE_CALENDAR_MILLISECOND = new BuiltinIterableBridge( CALENDAR_MILLISECOND );
	public static final FieldBridge MAP_CALENDAR_MILLISECOND = new BuiltinMapBridge( CALENDAR_MILLISECOND );

	public static final String TIKA_BRIDGE_NAME = "org.hibernate.search.bridge.builtin.TikaBridge";
	public static final String TIKA_BRIDGE_METADATA_PROCESSOR_SETTER = "setMetadataProcessorClass";
	public static final String TIKA_BRIDGE_PARSE_CONTEXT_SETTER = "setParseContextProviderClass";

	private static final Log LOG = LoggerFactory.make();

	private static Map<String, FieldBridge> builtInBridges = new HashMap<String, FieldBridge>();
	private static Map<String, NumericFieldBridge> numericBridges = new HashMap<String, NumericFieldBridge>();

	static {
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

		numericBridges.put( Integer.class.getName(), INT_NUMERIC );
		numericBridges.put( int.class.getName(), INT_NUMERIC );
		numericBridges.put( Long.class.getName(), LONG_NUMERIC );
		numericBridges.put( long.class.getName(), LONG_NUMERIC );
		numericBridges.put( Double.class.getName(), DOUBLE_NUMERIC );
		numericBridges.put( double.class.getName(), DOUBLE_NUMERIC );
		numericBridges.put( Float.class.getName(), FLOAT_NUMERIC );
		numericBridges.put( float.class.getName(), FLOAT_NUMERIC );
	}

	private BridgeFactory() {
	}

	/**
	 * This extracts and instantiates the implementation class from a {@code ClassBridge} annotation.
	 *
	 * @param cb the class bridge annotation
	 * @param clazz the {@code Class} on which the annotation is defined on
	 * @return Returns the specified {@code FieldBridge} instance
	 */
	public static FieldBridge extractType(ClassBridge cb, Class<?> clazz) {
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
	public static void injectParameters(ClassBridge classBridgeConfiguration, Object classBridge) {
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
	public static FieldBridge buildSpatialBridge(Spatial spatial, XClass clazz, String latitudeField, String longitudeField) {
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
	public static FieldBridge buildSpatialBridge(Spatial spatial, XMember member) {
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
	public static FieldBridge buildSpatialBridge(Spatial spatial, String latitudeField, String longitudeField) {
		FieldBridge bridge = null;
		if ( spatial != null ) {
			if ( spatial.spatialMode() == SpatialMode.GRID ) {
				if ( latitudeField != null && longitudeField != null ) {
					bridge = new SpatialFieldBridgeByQuadTree( spatial.topQuadTreeLevel(), spatial.bottomQuadTreeLevel(), latitudeField, longitudeField );
				}
				else {
					bridge = new SpatialFieldBridgeByQuadTree( spatial.topQuadTreeLevel(), spatial.bottomQuadTreeLevel() );
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

	public static FieldBridge guessType(Field field, NumericField numericField, XMember member, ReflectionManager reflectionManager) {
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
			bridge = createTikaBridge( annotation );
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

	private static FieldBridge createTikaBridge(org.hibernate.search.annotations.TikaBridge annotation) {
		Class<?> tikaBridgeClass;
		FieldBridge tikaBridge;
		try {
			tikaBridgeClass = ClassLoaderHelper.classForName( TIKA_BRIDGE_NAME, BridgeFactory.class.getClassLoader() );
			tikaBridge = ClassLoaderHelper.instanceFromClass( FieldBridge.class, tikaBridgeClass, "Tika bridge" );
		}
		catch (ClassNotFoundException e) {
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

	private static void configureTikaBridgeParameters(Class<?> tikaBridgeClass, String setter, Object tikaBridge, Class<?> clazz) {
		try {
			Method m = tikaBridgeClass.getMethod( setter, Class.class );
			m.invoke( tikaBridge, clazz );
		}
		catch (Exception e) {
			throw LOG.unableToConfigureTikaBridge( TIKA_BRIDGE_NAME, e );
		}
	}

	private static FieldBridge guessEmbeddedFieldBridge(XMember member, ReflectionManager reflectionManager) {
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

	private static FieldBridge guessNumericFieldBridge(XMember member, ReflectionManager reflectionManager) {
		if ( isNotAnnotatedWithIndexEmbedded( member ) ) {
			return numericBridges.get( member.getType().getName() );
		}

		if ( isIterable( reflectionManager, member ) ) {
			return NUMERIC_ITERABLE_BRIDGE;
		}

		if ( member.isArray() ) {
			return NUMERIC_ARRAY_BRIDGE;
		}

		if ( isMap( member ) ) {
			return NUMERIC_MAP_BRIDGE;
		}

		return null;
	}

	private static FieldBridge guessCalendarFieldBridge(XMember member, ReflectionManager reflectionManager, Resolution resolution) {
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

	private static FieldBridge guessDateFieldBridge(XMember member, ReflectionManager reflectionManager, Resolution resolution) {
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

	private static boolean isNotAnnotatedWithIndexEmbedded(XMember member) {
		return !isAnnotatedWithIndexEmbedded( member );
	}

	private static boolean isAnnotatedWithIndexEmbedded(XMember member) {
		return member.isAnnotationPresent( org.hibernate.search.annotations.IndexedEmbedded.class );
	}

	private static boolean isIterable(ReflectionManager reflectionManager, XMember member) {
		Class<?> typeClass = reflectionManager.toClass( member.getType() );
		return Iterable.class.isAssignableFrom( typeClass );
	}

	private static boolean isMap(XMember member) {
		return member.isCollection() && Map.class.equals( member.getCollectionClass() );
	}

	private static FieldBridge doExtractType(
			org.hibernate.search.annotations.FieldBridge bridgeAnn,
			XMember member,
			ReflectionManager reflectionManager) {
		return doExtractType( bridgeAnn, member.getName(), reflectionManager.toClass( member.getType() ) );
	}

	private static FieldBridge doExtractType(
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

	private static void populateReturnType(Class<?> appliedOnType, Class<?> bridgeType, Object bridgeInstance) {
		if ( AppliedOnTypeAwareBridge.class.isAssignableFrom( bridgeType ) ) {
			( (AppliedOnTypeAwareBridge) bridgeInstance ).setAppliedOnType( appliedOnType );
		}
	}

	public static FieldBridge getDateField(Resolution resolution) {
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

	public static FieldBridge getArrayDateField(Resolution resolution) {
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

	public static FieldBridge getMapDateField(Resolution resolution) {
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

	public static FieldBridge getIterableDateField(Resolution resolution) {
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

	public static FieldBridge getCalendarField(Resolution resolution) {
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

	public static FieldBridge getArrayCalendarField(Resolution resolution) {
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

	public static FieldBridge getMapCalendarField(Resolution resolution) {
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

	public static FieldBridge getIterableCalendarField(Resolution resolution) {
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
	public static TwoWayFieldBridge extractTwoWayType(org.hibernate.search.annotations.FieldBridge fieldBridge,
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
	public static FieldBridge extractType(org.hibernate.search.annotations.FieldBridge fieldBridgeAnnotation,
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
