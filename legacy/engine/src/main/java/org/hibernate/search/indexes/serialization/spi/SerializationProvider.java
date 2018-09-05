/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.indexes.serialization.spi;

import org.hibernate.search.engine.service.spi.Service;

/**
 * Provides access to a serializer and deserializer to send the necessary work load for remote backends over the wire.
 * <p>
 * Note: Providers are encouraged to offer a backward and forward compatible protocol.
 * </p>
 * <p>
 * Implementors are encouraged to implement a descriptive {@code toString()}
 * method for logging purposes.
 * </p>
 *
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 */
public interface SerializationProvider extends Service {

	/**
	 * This method will be invoked when any thread needs a Serializer:
	 * implementors of this Service can return the same instance multiple
	 * times if the Serializer implementation is threadsafe.
	 * In all other cases return a new instance.
	 * An obtained Serializer should not be shared across threads.
	 * @return the initialized Serializer ready for usage
	 */
	Serializer getSerializer();

	/**
	 * This method will be invoked when any thread needs a Deserializer:
	 * implementors of this Service can return the same instance multiple
	 * times if the Deserializer implementation is threadsafe.
	 * In all other cases return a new instance.
	 * An obtained Deserializer should not be shared across threads.
	 * @return the initialized Deserializer ready for usage
	 */
	Deserializer getDeserializer();

}
