/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.document.model.impl;

import org.hibernate.search.backend.elasticsearch.gson.impl.JsonObjectAccessor;
import org.hibernate.search.util.AssertionFailure;

public class ElasticsearchObjectFieldIndexSchemaCollectorImpl
		extends AbstractElasticsearchIndexSchemaCollector<IndexSchemaObjectPropertyNodeBuilder> {
	ElasticsearchObjectFieldIndexSchemaCollectorImpl(JsonObjectAccessor accessor,
			IndexSchemaObjectPropertyNodeBuilder nodeBuilder) {
		super( accessor, nodeBuilder );
	}

	@Override
	public void explicitRouting() {
		throw new AssertionFailure( "explicitRouting() was called on a non-root schema collector; this should never happen." );
	}
}
