//$Id$
package org.hibernate.search.bridge;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.AssertionFailure;
import org.hibernate.search.bridge.builtin.DateBridge;
import org.hibernate.search.bridge.builtin.FloatBridge;
import org.hibernate.search.bridge.builtin.LongBridge;
import org.hibernate.search.bridge.builtin.BigIntegerBridge;
import org.hibernate.search.bridge.builtin.StringBridge;
import org.hibernate.search.bridge.builtin.IntegerBridge;
import org.hibernate.search.bridge.builtin.BigDecimalBridge;
import org.hibernate.search.bridge.builtin.DoubleBridge;
import org.hibernate.search.bridge.builtin.ShortBridge;
import org.hibernate.search.bridge.builtin.EnumBridge;
import org.hibernate.search.bridge.builtin.BooleanBridge;
import org.hibernate.search.annotations.Resolution;
import org.hibernate.search.annotations.Parameter;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.ClassBridge;
import org.hibernate.search.SearchException;
import org.hibernate.search.util.BinderHelper;
import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.annotations.common.reflection.XMember;
import org.hibernate.annotations.common.reflection.ReflectionManager;

/**
 * @author Emmanuel Bernard
 * @author John Griffin
 */
public class BridgeFactory {
	private static Map<String, FieldBridge> builtInBridges = new HashMap<String, FieldBridge>();

	private BridgeFactory() {
	}

	public static final TwoWayFieldBridge DOUBLE = new TwoWayString2FieldBridgeAdaptor( new DoubleBridge() );

	public static final TwoWayFieldBridge FLOAT = new TwoWayString2FieldBridgeAdaptor( new FloatBridge() );

	public static final TwoWayFieldBridge SHORT = new TwoWayString2FieldBridgeAdaptor( new ShortBridge() );

	public static final TwoWayFieldBridge INTEGER = new TwoWayString2FieldBridgeAdaptor( new IntegerBridge() );

	public static final TwoWayFieldBridge LONG = new TwoWayString2FieldBridgeAdaptor( new LongBridge() );

	public static final TwoWayFieldBridge BIG_INTEGER = new TwoWayString2FieldBridgeAdaptor( new BigIntegerBridge() );

	public static final TwoWayFieldBridge BIG_DECIMAL = new TwoWayString2FieldBridgeAdaptor( new BigDecimalBridge() );

	public static final TwoWayFieldBridge STRING = new TwoWayString2FieldBridgeAdaptor( new StringBridge() );

	public static final TwoWayFieldBridge BOOLEAN = new TwoWayString2FieldBridgeAdaptor( new BooleanBridge() );

	public static final FieldBridge DATE_YEAR = new String2FieldBridgeAdaptor( DateBridge.DATE_YEAR );
	public static final FieldBridge DATE_MONTH = new String2FieldBridgeAdaptor( DateBridge.DATE_MONTH );
	public static final FieldBridge DATE_DAY = new String2FieldBridgeAdaptor( DateBridge.DATE_DAY );
	public static final FieldBridge DATE_HOUR = new String2FieldBridgeAdaptor( DateBridge.DATE_HOUR );
	public static final FieldBridge DATE_MINUTE = new String2FieldBridgeAdaptor( DateBridge.DATE_MINUTE );
	public static final FieldBridge DATE_SECOND = new String2FieldBridgeAdaptor( DateBridge.DATE_SECOND );
	public static final TwoWayFieldBridge DATE_MILLISECOND =
			new TwoWayString2FieldBridgeAdaptor( DateBridge.DATE_MILLISECOND );

	static {
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

		builtInBridges.put( Date.class.getName(), DATE_MILLISECOND );
	}
	
	/**
	 * This extracts and instantiates the implementation class from a ClassBridge
	 * annotation.
	 *
	 * @param cb the ClassBridge
	 * @return FieldBridge
	 */
	public static FieldBridge extractType(ClassBridge cb)
	{
		FieldBridge bridge = null;

		if ( cb != null ) {
			Class impl = cb.impl();

			if (impl != null) {
				try {
					Object instance = impl.newInstance();
					if ( FieldBridge.class.isAssignableFrom( impl ) ) {
						bridge = (FieldBridge) instance;
					}
					if ( cb.params().length > 0 && ParameterizedBridge.class.isAssignableFrom( impl ) ) {
						Map params = new HashMap( cb.params().length );
						for ( Parameter param : cb.params() ) {
							params.put( param.name(), param.value() );
						}
						( (ParameterizedBridge) instance ).setParameterValues( params );
					}
				}
				catch (Exception e) {
					throw new HibernateException( "Unable to instantiate FieldBridge for " + ClassBridge.class.getName(), e );
				}
			}
		}
		if ( bridge == null ) throw new SearchException( "Unable to guess FieldBridge for " + ClassBridge.class.getName() );

		return bridge;
	}

	public static FieldBridge guessType(Field field, XMember member, ReflectionManager reflectionManager) {
		FieldBridge bridge = null;
		org.hibernate.search.annotations.FieldBridge bridgeAnn;
		//@Field bridge has priority over @FieldBridge
		if ( field != null && void.class != field.bridge().impl() ) {
			bridgeAnn = field.bridge();
		}
		else {
			bridgeAnn = member.getAnnotation( org.hibernate.search.annotations.FieldBridge.class );
		}
		if ( bridgeAnn != null ) {
			Class impl = bridgeAnn.impl();
			if (impl == void.class)
				throw new SearchException("@FieldBridge with no implementation class defined in: " + member.getName() );
			try {
				Object instance = impl.newInstance();
				if ( FieldBridge.class.isAssignableFrom( impl ) ) {
					bridge = (FieldBridge) instance;
				}
				else if ( org.hibernate.search.bridge.TwoWayStringBridge.class.isAssignableFrom( impl ) ) {
					bridge = new TwoWayString2FieldBridgeAdaptor(
							(org.hibernate.search.bridge.TwoWayStringBridge) instance );
				}
				else if ( org.hibernate.search.bridge.StringBridge.class.isAssignableFrom( impl ) ) {
					bridge = new String2FieldBridgeAdaptor( (org.hibernate.search.bridge.StringBridge) instance );
				}
				else {
					throw new SearchException("@FieldBridge implementation implements none of the field bridge interfaces: "
							+ impl + " in " + member.getName() );
				}
				if ( bridgeAnn.params().length > 0 && ParameterizedBridge.class.isAssignableFrom( impl ) ) {
					Map params = new HashMap( bridgeAnn.params().length );
					for ( Parameter param : bridgeAnn.params() ) {
						params.put( param.name(), param.value() );
					}
					( (ParameterizedBridge) instance ).setParameterValues( params );
				}
			}
			catch (Exception e) {
				//TODO add classname
				throw new HibernateException( "Unable to instanciate FieldBridge for " + member.getName(), e );
			}
		}
		else if ( member.isAnnotationPresent( org.hibernate.search.annotations.DateBridge.class ) ) {
			Resolution resolution =
					member.getAnnotation( org.hibernate.search.annotations.DateBridge.class ).resolution();
			bridge = getDateField( resolution );
		}
		else {
			//find in built-ins
			XClass returnType = member.getType();
			bridge = builtInBridges.get( returnType.getName() );
			if ( bridge == null && returnType.isEnum() ) {
				bridge = new TwoWayString2FieldBridgeAdaptor(
						new EnumBridge( (Class<? extends Enum>) reflectionManager.toClass( returnType ) )
				);
			}
		}
		//TODO add classname
		if ( bridge == null ) throw new SearchException( "Unable to guess FieldBridge for " + member.getName() );
		return bridge;
	}

	public static FieldBridge getDateField(Resolution resolution) {
		switch (resolution) {
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
}
