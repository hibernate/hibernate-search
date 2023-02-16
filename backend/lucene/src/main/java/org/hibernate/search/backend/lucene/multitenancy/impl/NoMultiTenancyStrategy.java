/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.multitenancy.impl;

import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.Set;

import org.apache.lucene.document.Document;
import org.apache.lucene.search.Query;
import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.util.common.reporting.EventContext;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public class NoMultiTenancyStrategy implements MultiTenancyStrategy {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	@Override
	public boolean isMultiTenancySupported() {
		return false;
	}

	@Override
	public void contributeToIndexedDocument(Document document, String tenantId) {
		// No need to add anything to documents, the ID field (already added elsewhere) is enough
	}

	@Override
	public Query filterOrNull(String tenantId) {
		return null;
	}

	@Override
	public Query filterOrNull(Set<String> tenantIds) {
		return null;
	}

	@Override
	public void checkTenantId(String tenantId, EventContext backendContext) {
		if ( tenantId != null ) {
			throw log.tenantIdProvidedButMultiTenancyDisabled( Collections.singleton( tenantId ), backendContext );
		}
	}

	@Override
	public void checkTenantId(Set<String> tenantIds, EventContext context) {
		if ( tenantIds != null && !tenantIds.isEmpty() ) {
			throw log.tenantIdProvidedButMultiTenancyDisabled( tenantIds, context );
		}
	}
}
