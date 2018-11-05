/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.mapper;

import org.hibernate.search.engine.mapper.mapping.spi.MappedIndexSearchTarget;
import org.hibernate.search.engine.search.DocumentReference;
import org.hibernate.search.engine.search.loading.spi.ObjectLoader;
import org.hibernate.search.util.impl.integrationtest.common.stub.StubSessionContext;

/**
 * A wrapper around {@link MappedIndexSearchTarget} providing some syntactic sugar,
 * such as methods that do not force to provide a session context.
 * <p>
 * This is a simpler version of {@link GenericStubMappingSearchTarget} that allows user to skip the generic parameters.
 */
public class StubMappingSearchTarget extends GenericStubMappingSearchTarget<DocumentReference, DocumentReference> {

	StubMappingSearchTarget(MappedIndexSearchTarget<DocumentReference, DocumentReference> indexSearchTarget) {
		super( indexSearchTarget );
	}

	public StubMappingQueryResultDefinitionContext<DocumentReference, DocumentReference> query() {
		return query( ObjectLoader.identity() );
	}

	public StubMappingQueryResultDefinitionContext<DocumentReference, DocumentReference> query(
			StubSessionContext sessionContext) {
		return query( sessionContext, ObjectLoader.identity() );
	}
}
