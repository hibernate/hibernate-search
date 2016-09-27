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
 * @author Yoann Rodiere
 */
public class TwoWayString2FieldBridgeIgnoreAnalyzerAdaptor extends TwoWayString2FieldBridgeAdaptor implements IgnoreAnalyzerBridge {

	public <T extends TwoWayStringBridge & IgnoreAnalyzerBridge>
			TwoWayString2FieldBridgeIgnoreAnalyzerAdaptor(T stringBridge) {
		super( stringBridge );
	}

}
