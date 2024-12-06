/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.multitenancy.impl;

import java.util.Collections;
import java.util.Set;

import org.hibernate.search.backend.lucene.logging.impl.ConfigurationLog;
import org.hibernate.search.util.common.reporting.EventContext;

import org.apache.lucene.document.Document;
import org.apache.lucene.search.Query;

public class NoMultiTenancyStrategy implements MultiTenancyStrategy {

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
			throw ConfigurationLog.INSTANCE.tenantIdProvidedButMultiTenancyDisabled( Collections.singleton( tenantId ),
					backendContext );
		}
	}

	@Override
	public void checkTenantId(Set<String> tenantIds, EventContext context) {
		if ( tenantIds != null && !tenantIds.isEmpty() ) {
			throw ConfigurationLog.INSTANCE.tenantIdProvidedButMultiTenancyDisabled( tenantIds, context );
		}
	}
}
