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
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.hibernate.search.bridge.FieldBridge;
import org.hibernate.search.bridge.TwoWayFieldBridge;
import org.hibernate.search.bridge.builtin.BigDecimalBridge;
import org.hibernate.search.bridge.builtin.BigIntegerBridge;
import org.hibernate.search.bridge.builtin.BooleanBridge;
import org.hibernate.search.bridge.builtin.ByteBridge;
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
 * @author Emmanuel Bernard
 */
class BasicJDKTypesBridgeProvider implements BridgeProvider {

	private static final TwoWayFieldBridge CHARACTER = new TwoWayString2FieldBridgeAdaptor( new CharacterBridge() );
	private static final TwoWayFieldBridge DOUBLE = new TwoWayString2FieldBridgeAdaptor( new DoubleBridge() );
	private static final TwoWayFieldBridge FLOAT = new TwoWayString2FieldBridgeAdaptor( new FloatBridge() );
	private static final TwoWayFieldBridge BYTE = new TwoWayString2FieldBridgeAdaptor( new ByteBridge() );
	private static final TwoWayFieldBridge SHORT = new TwoWayString2FieldBridgeAdaptor( new ShortBridge() );
	private static final TwoWayFieldBridge INTEGER = new TwoWayString2FieldBridgeAdaptor( new IntegerBridge() );
	private static final TwoWayFieldBridge LONG = new TwoWayString2FieldBridgeAdaptor( new LongBridge() );
	private static final TwoWayFieldBridge BIG_INTEGER = new TwoWayString2FieldBridgeAdaptor( new BigIntegerBridge() );
	private static final TwoWayFieldBridge BIG_DECIMAL = new TwoWayString2FieldBridgeAdaptor( new BigDecimalBridge() );
	private static final TwoWayFieldBridge STRING = new TwoWayString2FieldBridgeAdaptor( new StringBridge() );
	private static final TwoWayFieldBridge BOOLEAN = new TwoWayString2FieldBridgeAdaptor( new BooleanBridge() );
	private static final TwoWayFieldBridge Url = new TwoWayString2FieldBridgeAdaptor( new UrlBridge() );
	private static final TwoWayFieldBridge Uri = new TwoWayString2FieldBridgeAdaptor( new UriBridge() );
	private static final TwoWayFieldBridge UUID = new TwoWayString2FieldBridgeAdaptor( new UUIDBridge() );

	//Not static as it depends on the application's classloader
	private final TwoWayFieldBridge clazz;

	private final Map<String, FieldBridge> builtInBridges;

	BasicJDKTypesBridgeProvider(ServiceManager serviceManager) {
		this.clazz = new TwoWayString2FieldBridgeAdaptor( new org.hibernate.search.bridge.builtin.ClassBridge( serviceManager ) );
		Map<String, FieldBridge> bridges = new HashMap<String, FieldBridge>();
		bridges.put( Character.class.getName(), CHARACTER );
		bridges.put( char.class.getName(), CHARACTER );
		bridges.put( Double.class.getName(), DOUBLE );
		bridges.put( double.class.getName(), DOUBLE );
		bridges.put( Float.class.getName(), FLOAT );
		bridges.put( float.class.getName(), FLOAT );
		bridges.put( Byte.class.getName(), BYTE );
		bridges.put( byte.class.getName(), BYTE );
		bridges.put( Short.class.getName(), SHORT );
		bridges.put( short.class.getName(), SHORT );
		bridges.put( Integer.class.getName(), INTEGER );
		bridges.put( int.class.getName(), INTEGER );
		bridges.put( Long.class.getName(), LONG );
		bridges.put( long.class.getName(), LONG );
		bridges.put( BigInteger.class.getName(), BIG_INTEGER );
		bridges.put( BigDecimal.class.getName(), BIG_DECIMAL );
		bridges.put( String.class.getName(), STRING );
		bridges.put( Boolean.class.getName(), BOOLEAN );
		bridges.put( boolean.class.getName(), BOOLEAN );
		bridges.put( URL.class.getName(), Url );
		bridges.put( URI.class.getName(), Uri );
		bridges.put( UUID.class.getName(), UUID );
		bridges.put( Class.class.getName(), clazz );
		this.builtInBridges = bridges;
	}

	@Override
	public FieldBridge provideFieldBridge(BridgeProviderContext bridgeProviderContext) {
		return builtInBridges.get( bridgeProviderContext.getReturnType().getName() );
	}
}
