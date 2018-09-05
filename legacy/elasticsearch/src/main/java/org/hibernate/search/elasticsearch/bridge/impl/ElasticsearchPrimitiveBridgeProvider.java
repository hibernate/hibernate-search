/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.elasticsearch.bridge.impl;

import org.hibernate.search.bridge.FieldBridge;
import org.hibernate.search.bridge.spi.BridgeProvider;
import org.hibernate.search.bridge.util.impl.TwoWayString2FieldBridgeIgnoreAnalyzerAdaptor;
import org.hibernate.search.elasticsearch.bridge.builtin.impl.ElasticsearchBooleanBridge;

/**
 * Creates bridges specific to Elasticsearch for primitive types.
 *
 * @author Yoann Rodiere
 */
class ElasticsearchPrimitiveBridgeProvider implements BridgeProvider {

	@Override
	public FieldBridge provideFieldBridge(BridgeProviderContext context) {
		Class<?> returnType = context.getReturnType();
		if ( Boolean.class.equals( returnType ) || boolean.class.equals( returnType ) ) {
			return new TwoWayString2FieldBridgeIgnoreAnalyzerAdaptor( ElasticsearchBooleanBridge.INSTANCE );
		}

		return null;
	}
}
