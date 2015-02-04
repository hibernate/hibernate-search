/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.indexes.serialization.avro.impl;

import org.apache.avro.Protocol;
import org.hibernate.search.indexes.serialization.avro.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * Keeps a reference to each known Avro Protocol revision we can support.
 * Only a single Major version is supported, but for minor revisions we
 * should always be able to deserialize an older encoding scheme.
 * For minor revisions beyond the last known version a best effort is
 * applied and a warning is logged.
 * Each Protocol is only created at first use, so to not waste parsing
 * time nor memory for unused revisions.
 *
 * @author Sanne Grinovero
 * @since 5.0
 */
public final class KnownProtocols {

	/**
	 * Latest protocol version is 1.2
	 */
	public static final int MAJOR_VERSION = 1;
	public static final int LATEST_MINOR_VERSION = 2;

	private static final Log log = LoggerFactory.make( Log.class );
	private volatile Protocol v1_0 = null;
	private volatile Protocol v1_1 = null;
	private volatile Protocol v1_2 = null;
	private volatile boolean warned = false;

	Protocol getProtocol(int majorVersion, int minorVersion) {
		if ( MAJOR_VERSION != majorVersion ) {
			throw log.incompatibleProtocolVersion(
					majorVersion,
					minorVersion,
					MAJOR_VERSION,
					LATEST_MINOR_VERSION
			);
		}
		if ( minorVersion == 2 ) {
			return getV1_2();
		}
		else if ( minorVersion == 1 ) {
			return getV1_1();
		}
		else if ( minorVersion == 0 ) {
			return getV1_0();
		}
		else {
			if ( ! warned ) {
				warned = true;
				log.unexpectedMinorProtocolVersion( majorVersion, minorVersion, LATEST_MINOR_VERSION );
			}
			return getV1_2();
		}
	}

	public Protocol getLatestProtocol() {
		return getProtocol( MAJOR_VERSION, LATEST_MINOR_VERSION );
	}

	private Protocol getV1_0() {
		Protocol p = v1_0;
		if ( p == null ) {
			synchronized (this) {
				p = v1_0;
				if ( p != null ) {
					return p;
				}
				p = new ProtocolBuilderV1_0().build();
				v1_0 = p;
			}
		}
		return p;
	}

	private Protocol getV1_1() {
		Protocol p = v1_1;
		if ( p == null ) {
			synchronized (this) {
				p = v1_1;
				if ( p != null ) {
					return p;
				}
				p = new ProtocolBuilderV1_1().build();
				v1_1 = p;
			}
		}
		return p;
	}

	private Protocol getV1_2() {
		Protocol p = v1_2;
		if ( p == null ) {
			synchronized (this) {
				p = v1_2;
				if ( p != null ) {
					return p;
				}
				p = new ProtocolBuilderV1_2().build();
				v1_2 = p;
			}
		}
		return p;
	}

}
