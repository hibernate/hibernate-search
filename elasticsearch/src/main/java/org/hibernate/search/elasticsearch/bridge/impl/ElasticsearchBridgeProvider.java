/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.bridge.impl;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.search.bridge.FieldBridge;
import org.hibernate.search.bridge.spi.BridgeProvider;
import org.hibernate.search.bridge.spi.IndexManagerTypeSpecificBridgeProvider;
import org.hibernate.search.elasticsearch.spi.ElasticsearchIndexManagerType;
import org.hibernate.search.indexes.spi.IndexManagerType;

/**
 * Creates bridges specific to ES.
 *
 * @author Gunnar Morling
 * @author Guillaume Smet
 * @author Yoann Rodiere
 */
public class ElasticsearchBridgeProvider implements IndexManagerTypeSpecificBridgeProvider {

	private final List<BridgeProvider> delegates = new ArrayList<>();

	public ElasticsearchBridgeProvider() {
		super();
		delegates.add( new ElasticsearchPrimitiveBridgeProvider() );
		delegates.add( new ElasticsearchJavaUtilTimeBridgeProvider() );
		delegates.add( new ElasticsearchJavaTimeBridgeProvider() );
	}

	@Override
	public IndexManagerType getIndexManagerType() {
		return ElasticsearchIndexManagerType.INSTANCE;
	}

	@Override
	public FieldBridge provideFieldBridge(BridgeProviderContext context) {
		for ( BridgeProvider delegate : delegates ) {
			FieldBridge delegateResult = delegate.provideFieldBridge( context );
			if ( delegateResult != null ) {
				return delegateResult;
			}
		}

		return null;
	}
}
