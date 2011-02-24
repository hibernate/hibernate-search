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
package org.hibernate.search.bridge;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.URL;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.search.SearchException;
import org.hibernate.annotations.common.AssertionFailure;
import org.hibernate.annotations.common.reflection.ReflectionManager;
import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.annotations.common.reflection.XMember;
import org.hibernate.search.annotations.ClassBridge;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.NumericField;
import org.hibernate.search.annotations.Parameter;
import org.hibernate.search.annotations.Resolution;
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
import org.hibernate.search.bridge.builtin.UriBridge;
import org.hibernate.search.bridge.builtin.UrlBridge;

/**
 * This factory is responsible for creating and initializing build-in and custom <i>FieldBridges</i>.
 *
 * @author Emmanuel Bernard
 * @author John Griffin
 */
public final class BridgeFactory {
	private static Map<String, FieldBridge> builtInBridges = new HashMap<String, FieldBridge>();
	private static Map<String, NumericFieldBridge> numericBridges = new HashMap<String, NumericFieldBridge>();

	private BridgeFactory() {
	}

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

	public static final FieldBridge DATE_YEAR = new TwoWayString2FieldBridgeAdaptor( DateBridge.DATE_YEAR );
	public static final FieldBridge DATE_MONTH = new TwoWayString2FieldBridgeAdaptor( DateBridge.DATE_MONTH );
	public static final FieldBridge DATE_DAY = new TwoWayString2FieldBridgeAdaptor( DateBridge.DATE_DAY );
	public static final FieldBridge DATE_HOUR = new TwoWayString2FieldBridgeAdaptor( DateBridge.DATE_HOUR );
	public static final FieldBridge DATE_MINUTE = new TwoWayString2FieldBridgeAdaptor( DateBridge.DATE_MINUTE );
	public static final FieldBridge DATE_SECOND = new TwoWayString2FieldBridgeAdaptor( DateBridge.DATE_SECOND );

	public static final FieldBridge CALENDAR_YEAR = new TwoWayString2FieldBridgeAdaptor( CalendarBridge.CALENDAR_YEAR );
	public static final FieldBridge CALENDAR_MONTH = new TwoWayString2FieldBridgeAdaptor( CalendarBridge.CALENDAR_MONTH );
	public static final FieldBridge CALENDAR_DAY = new TwoWayString2FieldBridgeAdaptor( CalendarBridge.CALENDAR_DAY );
	public static final FieldBridge CALENDAR_HOUR = new TwoWayString2FieldBridgeAdaptor( CalendarBridge.CALENDAR_HOUR );
	public static final FieldBridge CALENDAR_MINUTE = new TwoWayString2FieldBridgeAdaptor( CalendarBridge.CALENDAR_MINUTE );
	public static final FieldBridge CALENDAR_SECOND = new TwoWayString2FieldBridgeAdaptor( CalendarBridge.CALENDAR_SECOND );

	public static final NumericFieldBridge INT_NUMERIC = new IntegerNumericFieldBridge();
	public static final NumericFieldBridge LONG_NUMERIC = new LongNumericFieldBridge();
	public static final NumericFieldBridge FLOAT_NUMERIC = new FloatNumericFieldBridge();
	public static final NumericFieldBridge DOUBLE_NUMERIC = new DoubleNumericFieldBridge();

	public static final TwoWayFieldBridge DATE_MILLISECOND =
			new TwoWayString2FieldBridgeAdaptor( DateBridge.DATE_MILLISECOND );

	public static final TwoWayFieldBridge CALENDAR_MILLISECOND =
			new TwoWayString2FieldBridgeAdaptor( CalendarBridge.CALENDAR_MILLISECOND );


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

	/**
	 * This extracts and instantiates the implementation class from a {@code ClassBridge} annotation.
	 *
	 * @param cb the class bridge annotation
	 * @param clazz the {@code XClass} on which the annotation is defined on
	 *
	 * @return Returns the specified {@code FieldBridge} instance
	 */
	public static FieldBridge extractType(ClassBridge cb, XClass clazz) {
		FieldBridge bridge = null;

		if ( cb != null ) {
			Class<?> impl = cb.impl();
			if ( impl != null ) {
				try {
					Object instance = impl.newInstance();
					if ( FieldBridge.class.isAssignableFrom( impl ) ) {
						bridge = (FieldBridge) instance;
					}
					else if ( org.hibernate.search.bridge.TwoWayStringBridge.class.isAssignableFrom( impl ) ) {
						bridge = new TwoWayString2FieldBridgeAdaptor(
								(org.hibernate.search.bridge.TwoWayStringBridge) instance
						);
					}
					else if ( org.hibernate.search.bridge.StringBridge.class.isAssignableFrom( impl ) ) {
						bridge = new String2FieldBridgeAdaptor( (org.hibernate.search.bridge.StringBridge) instance );
					}
					else {
						throw new SearchException(
								"@ClassBridge implementation implements none of the field bridge interfaces: "
										+ impl
						);
					}
					if ( cb.params().length > 0 && ParameterizedBridge.class.isAssignableFrom( impl ) ) {
						Map<String, String> params = new HashMap<String, String>( cb.params().length );
						for ( Parameter param : cb.params() ) {
							params.put( param.name(), param.value() );
						}
						( (ParameterizedBridge) instance ).setParameterValues( params );
					}
				}
				catch ( Exception e ) {
					final String msg = "Unable to instantiate ClassBridge of type " + impl.getName() + " defined on "
							+ clazz.getName();
					throw new SearchException( msg, e );
				}
			}
		}
		if ( bridge == null ) {
			throw new SearchException( "Unable to guess FieldBridge for " + ClassBridge.class.getName() );
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
			bridge = getDateField( resolution );
		}
		else if ( member.isAnnotationPresent( org.hibernate.search.annotations.CalendarBridge.class ) ) {
			Resolution resolution = member.getAnnotation( org.hibernate.search.annotations.CalendarBridge.class )
					.resolution();
			bridge = getCalendarField( resolution );
		}
		else if ( numericField != null ) {
			bridge = numericBridges.get( member.getType().getName() );
		}
		else {
			//find in built-ins
			XClass returnType = member.getType();
			bridge = builtInBridges.get( returnType.getName() );
			if ( bridge == null && returnType.isEnum() ) {
				//we return one enum type bridge instance per property as it is customized per ReturnType
				@SuppressWarnings("unchecked")
				final EnumBridge enumBridge = new EnumBridge();
				populateReturnType( reflectionManager.toClass( member.getType() ), EnumBridge.class, enumBridge );
				bridge = new TwoWayString2FieldBridgeAdaptor( enumBridge );
			}
		}
		//TODO add classname
		if ( bridge == null ) {
			throw new SearchException( "Unable to guess FieldBridge for " + member.getName() );
		}
		return bridge;
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
		Class impl = bridgeAnn.impl();
		if ( impl == void.class ) {
			throw new SearchException( "@FieldBridge with no implementation class defined in: " + appliedOnName );
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
				throw new SearchException(
						"@FieldBridge implementation implements none of the field bridge interfaces: "
								+ impl + " in " + appliedOnName
				);
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
		catch ( Exception e ) {
			//TODO add classname
			throw new SearchException( "Unable to instantiate FieldBridge for " + appliedOnName, e );
		}
		return bridge;
	}

	private static void populateReturnType(Class<?> appliedOnType, Class<?> bridgeType, Object bridgeInstance) {
		if ( AppliedOnTypeAwareBridge.class.isAssignableFrom( bridgeType ) ) {
			( ( AppliedOnTypeAwareBridge ) bridgeInstance ).setAppliedOnType( appliedOnType );
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
				throw new AssertionFailure( "Unknown Resolution: " + resolution );
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
				throw new AssertionFailure( "Unknown Resolution: " + resolution );
		}
	}

	/**
	 * Takes in a fieldBridge and will return you a TwoWayFieldBridge instance.
	 *
	 * @param fieldBridge the field bridge annotation
	 * @param appliedOnType the type the bridge is applied on
	 * @param reflectionManager The reflection manager instance
	 *
	 * @return a TwoWayFieldBridge instance if the Field Bridge is an instance of a TwoWayFieldBridge.
	 *
	 * @throws SearchException if the FieldBridge passed in is not an instance of a TwoWayFieldBridge.
	 */

	public static TwoWayFieldBridge extractTwoWayType(org.hibernate.search.annotations.FieldBridge fieldBridge,
													  XClass appliedOnType,
													  ReflectionManager reflectionManager) {
		FieldBridge fb = extractType( fieldBridge, appliedOnType, reflectionManager );
		if ( fb instanceof TwoWayFieldBridge ) {
			return (TwoWayFieldBridge) fb;
		}
		else {
			throw new SearchException( "FieldBridge passed in is not an instance of " + TwoWayFieldBridge.class.getSimpleName() );
		}
	}

	/**
	 * This extracts and instantiates the implementation class from a ClassBridge
	 * annotation.
	 *
	 * @param fieldBridgeAnnotation the FieldBridge annotation
	 * @param appliedOnType the type the bridge is applied on
	 * @param reflectionManager The reflection manager instance
	 *
	 * @return FieldBridge
	 */
	public static FieldBridge extractType(org.hibernate.search.annotations.FieldBridge fieldBridgeAnnotation,
										  XClass appliedOnType,
										  ReflectionManager reflectionManager) {
		FieldBridge bridge = null;

		if ( fieldBridgeAnnotation != null ) {
			bridge = doExtractType( fieldBridgeAnnotation, appliedOnType.getName(), reflectionManager.toClass( appliedOnType ) );
		}

		if ( bridge == null ) {
			throw new SearchException(
					"Unable to guess FieldBridge for " + org.hibernate.search.annotations.FieldBridge.class.getName()
			);
		}

		return bridge;
	}
}
