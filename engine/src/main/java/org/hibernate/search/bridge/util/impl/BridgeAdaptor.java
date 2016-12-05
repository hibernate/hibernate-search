/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.bridge.util.impl;

import org.hibernate.search.bridge.TwoWayStringBridge;
import org.hibernate.search.bridge.spi.IgnoreAnalyzerBridge;

/**
 * An interface for bridge adaptors, i.e. types exposing one bridge interface and using
 * another bridge instance under the hood.
 *
 * <p>Implementing this interface allows {@link BridgeAdaptorUtils} to detect
 * specific bridge interfaces ({@link TwoWayStringBridge}, {@link IgnoreAnalyzerBridge},
 * ...) even when the adaptor does no re-implement the interface.
 *
 * @author Yoann Rodiere
 */
public interface BridgeAdaptor {

	/**
	 * Return an object of the specified type to allow access to specific
	 * bridge interfaces.
	 *
	 * <p>If no adapted bridge matches this type, {@code null} is returned.

	 * @return An instance of the specified class, or {@code null} if there is none.
	 */
	<T> T unwrap(Class<T> bridgeClass);

}
