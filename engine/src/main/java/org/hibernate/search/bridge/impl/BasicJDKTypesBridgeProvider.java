/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.bridge.impl;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.URL;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.hibernate.search.bridge.FieldBridge;
import org.hibernate.search.bridge.TwoWayFieldBridge;
import org.hibernate.search.bridge.builtin.BigDecimalBridge;
import org.hibernate.search.bridge.builtin.BigIntegerBridge;
import org.hibernate.search.bridge.builtin.BooleanBridge;
import org.hibernate.search.bridge.builtin.CharacterBridge;
import org.hibernate.search.bridge.builtin.DoubleBridge;
import org.hibernate.search.bridge.builtin.FloatBridge;
import org.hibernate.search.bridge.builtin.IntegerBridge;
import org.hibernate.search.bridge.builtin.LongBridge;
import org.hibernate.search.bridge.builtin.ShortBridge;
import org.hibernate.search.bridge.builtin.StringBridge;
import org.hibernate.search.bridge.builtin.UUIDBridge;
import org.hibernate.search.bridge.builtin.UriBridge;
import org.hibernate.search.bridge.builtin.UrlBridge;
import org.hibernate.search.bridge.builtin.impl.TwoWayString2FieldBridgeAdaptor;
import org.hibernate.search.bridge.spi.BridgeProvider;
import org.hibernate.search.engine.service.spi.ServiceManager;

/**
 * Support all the default types of the JDK.
 *
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public class BasicJDKTypesBridgeProvider implements BridgeProvider {


	public static final TwoWayFieldBridge CHARACTER;
	public static final TwoWayFieldBridge DOUBLE;
	public static final TwoWayFieldBridge FLOAT;
	public static final TwoWayFieldBridge SHORT;
	public static final TwoWayFieldBridge INTEGER;
	public static final TwoWayFieldBridge LONG;
	public static final TwoWayFieldBridge BIG_INTEGER;
	public static final TwoWayFieldBridge BIG_DECIMAL;
	public static final TwoWayFieldBridge STRING;
	public static final TwoWayFieldBridge BOOLEAN;
	public static final TwoWayFieldBridge Url;
	public static final TwoWayFieldBridge Uri;
	public static final TwoWayFieldBridge UUID;

	//volatile necessary due to the lazy initialization nature of CLAZZ which requires a service manager
	public volatile TwoWayFieldBridge clazz;
	private volatile Map<String, FieldBridge> builtInBridges = new HashMap<String, FieldBridge>();

	static {
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
		Url = new TwoWayString2FieldBridgeAdaptor( new UrlBridge() );
		Uri = new TwoWayString2FieldBridgeAdaptor( new UriBridge() );
		UUID = new TwoWayString2FieldBridgeAdaptor( new UUIDBridge() );
	}

	public BasicJDKTypesBridgeProvider(ServiceManager serviceManager) {
		clazz = new TwoWayString2FieldBridgeAdaptor( new org.hibernate.search.bridge.builtin.ClassBridge( serviceManager ) );
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
		builtInBridges.put( URL.class.getName(), Url );
		builtInBridges.put( URI.class.getName(), Uri );
		builtInBridges.put( UUID.class.getName(), UUID );
		builtInBridges.put( Date.class.getName(), DateBridgeProvider.DATE_MILLISECOND );
		builtInBridges.put( Calendar.class.getName(), CalendarBridgeProvider.CALENDAR_MILLISECOND );
		builtInBridges.put( Class.class.getName(), clazz );
	}

	@Override
	public FieldBridge provideFieldBridge(BridgeProviderContext bridgeProviderContext) {
		return builtInBridges.get( bridgeProviderContext.getReturnType().getName() );
	}
}
