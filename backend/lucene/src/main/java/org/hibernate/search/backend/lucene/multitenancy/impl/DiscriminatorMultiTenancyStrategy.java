/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.multitenancy.impl;

import java.lang.invoke.MethodHandles;
import java.util.Set;

import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.lowlevel.common.impl.MetadataFields;
import org.hibernate.search.backend.lucene.lowlevel.query.impl.Queries;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.reporting.EventContext;

import org.apache.lucene.document.Document;
import org.apache.lucene.search.Query;

public class DiscriminatorMultiTenancyStrategy implements MultiTenancyStrategy {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	@Override
	public void contributeToIndexedDocument(Document document, String tenantId) {
		document.add( MetadataFields.searchableMetadataField( MetadataFields.tenantIdFieldName(), tenantId ) );
	}

	@Override
	public Query filterOrNull(String tenantId) {
		return Queries.term( MetadataFields.tenantIdFieldName(), tenantId );
	}

	@Override
	public Query filterOrNull(Set<String> tenantIds) {
		return Queries.anyTerm( MetadataFields.tenantIdFieldName(), tenantIds );
	}

	@Override
	public void checkTenantId(String tenantId, EventContext backendContext) {
		if ( tenantId == null ) {
			throw log.multiTenancyEnabledButNoTenantIdProvided( backendContext );
		}
	}

	@Override
	public void checkTenantId(Set<String> tenantIds, EventContext context) {
		if ( tenantIds == null || tenantIds.isEmpty() ) {
			throw log.multiTenancyEnabledButNoTenantIdProvided( context );
		}
	}
}
