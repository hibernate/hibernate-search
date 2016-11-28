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
	 * @return The bridge that is directly wrapped by this adaptor.
	 */
	Object unwrap();

}
