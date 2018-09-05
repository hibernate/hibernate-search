/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.bridge.util.impl;

import org.hibernate.search.bridge.ContainerBridge;
import org.hibernate.search.bridge.TwoWayStringBridge;
import org.hibernate.search.bridge.spi.IgnoreAnalyzerBridge;

/**
 * Utilities allowing to take into account bridge adaptors when detecting implemented tagging
 * interface (e.g. {@link IgnoreAnalyzerBridge}) or when trying to make use of specific field
 * interfaces (e.g. {@link TwoWayStringBridge}).
 *
 * @author Yoann Rodiere
 */
public final class BridgeAdaptorUtils {

	private BridgeAdaptorUtils() {
		// private constructor
	}

	/**
	 * Return a bridge of the specified type extracted from the given bridge,
	 * going through {@link BridgeAdaptor adaptors} and {@link ContainerBridge container bridges}
	 * as necessary.
	 *
	 * <p>The returned object may be the adaptor itself, or its delegate, or a delegate of
	 * its delegate, and so on, but it may also be a element bridge of the given bridge if the
	 * given bridge is a {@link ContainerBridge}, or an element bridge of a delegate of the given
	 * bridge, and so on.
	 * <p>If neither the adaptor nor one of its delegates can provide an instance of the
	 * specified type, {@code null} is returned.
	 *
	 * @param bridge The bridge to use as a starting point.
	 * @param clazz The expected bridge type.
	 * @return A unwrapped bridge implementing the expected bridge type, or {@code null}
	 * if none could be found.
	 */
	public static <T> T unwrapAdaptorAndContainer(Object bridge, Class<T> clazz) {
		T resultFromAdaptor = unwrapAdaptorOnly( bridge, clazz );
		if ( resultFromAdaptor != null ) {
			return resultFromAdaptor;
		}

		Object elementBridge = getElementBridge( bridge );
		if ( elementBridge != null ) {
			return unwrapAdaptorAndContainer( elementBridge, clazz );
		}

		return null;
	}

	/**
	 * Return the element bridge for the given bridge, if any, taking adaptors into account.
	 *
	 * <p>The returned object may be the element bridge for the given bridge itself, or its delegate,
	 * or a delegate of its delegate, and so on.
	 * <p>If neither the given bridge nor one of its delegates is a {@link ContainerBridge},
	 * {@code null} is returned.
	 *
	 * @param bridge The bridge to use as a starting point.
	 * @param clazz The expected bridge type.
	 * @return A unwrapped bridge implementing the expected bridge type, or {@code null}
	 * if none could be found.
	 */
	private static Object getElementBridge(Object bridge) {
		ContainerBridge containerBridge = null;
		if ( bridge instanceof BridgeAdaptor ) {
			containerBridge = ( (BridgeAdaptor) bridge ).unwrap( ContainerBridge.class );
		}
		else if ( bridge instanceof ContainerBridge ) {
			containerBridge = (ContainerBridge) bridge;
		}

		return containerBridge == null ? null : containerBridge.getElementBridge();
	}

	/**
	 * Return a bridge of the specified type extracted from the given bridge,
	 * going through {@link BridgeAdaptor adaptors} as necessary, never going
	 * through {@link ContainerBridge container bridges}.
	 *
	 * <p>The returned object may be the adaptor itself, or its delegate, or a delegate of
	 * its delegate, and so on.
	 * <p>If neither the adaptor or one of its delegates is an instance of the specified type,
	 * {@code null} is returned.
	 *
	 * @param bridge The bridge to use as a starting point.
	 * @param clazz The expected bridge type.
	 * @return A unwrapped bridge implementing the expected bridge type, or {@code null}
	 * if none could be found.
	 */
	public static <T> T unwrapAdaptorOnly(Object bridge, Class<T> clazz) {
		if ( clazz.isInstance( bridge ) ) {
			return clazz.cast( bridge );
		}
		else if ( bridge instanceof BridgeAdaptor ) {
			return ( (BridgeAdaptor) bridge ).unwrap( clazz );
		}
		else {
			return null;
		}
	}

}
