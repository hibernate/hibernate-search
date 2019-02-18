/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.indexes.serialization.avro.impl;

import org.hibernate.search.indexes.serialization.avro.logging.impl.Log;
import org.hibernate.search.indexes.serialization.spi.Deserializer;
import org.hibernate.search.indexes.serialization.spi.SerializationProvider;
import org.hibernate.search.indexes.serialization.spi.Serializer;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import java.lang.invoke.MethodHandles;

/**
 * Avro based implementation of {@code SerializationProvider}.
 * <p>
 * Parsing code inspired by http://www.infoq.com/articles/ApacheAvro
 * from Boris Lublinsky
 * <p>
 * Before the actual serialized flux, two bytes are reserved:
 * <ul>
 * <li>majorVersion</li>
 * <li>minorVersion</li>
 * </ul>
 *
 * A major version increase implies an incompatible protocol change.
 * Messages of a {@code majorVersion > current version} should be refused.
 * <p>
 * A minor version increase implies a compatible protocol change.
 * Messages of a {@code minorVersion > current version} are parsed, but new
 * operation will be ignored or rejected.
 * <p>
 * If message's {@code major version is < current version}, then the
 * implementation is strongly encouraged to parse and process them.
 * It is mandatory if only message's {@code code minor version is < current version}.
 *
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 * @author Hardy Ferentschik
 */
public class AvroSerializationProvider implements SerializationProvider {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final KnownProtocols protocols;

	public static int getMajorVersion() {
		return KnownProtocols.MAJOR_VERSION;
	}

	public static int getMinorVersion() {
		return KnownProtocols.LATEST_MINOR_VERSION;
	}

	public AvroSerializationProvider() {
		log.serializationProtocol( getMajorVersion(), getMinorVersion() );
		this.protocols = new KnownProtocols();
	}

	@Override
	public Serializer getSerializer() {
		return new AvroSerializer( protocols.getLatestProtocol() );
	}

	@Override
	public Deserializer getDeserializer() {
		return new AvroDeserializer( protocols );
	}

	@Override
	public String toString() {
		return "Avro SerializationProvider v" + getMajorVersion() + "." + getMinorVersion();
	}
}
