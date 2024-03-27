/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.impl.integrationtest.mapper.stub;

import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.common.EntityReference;
import org.hibernate.search.engine.mapper.scope.spi.MappedIndexScope;

/**
 * A wrapper around {@link MappedIndexScope} providing some syntactic sugar,
 * such as methods that do not force to provide a session context.
 * <p>
 * This is a simpler version of {@link GenericStubMappingScope} that allows user to skip the generic parameters.
 */
public class StubMappingScope extends GenericStubMappingScope<EntityReference, DocumentReference> {

	StubMappingScope(StubMapping mapping, MappedIndexScope<EntityReference, DocumentReference> delegate) {
		super( mapping, delegate, new StubSearchLoadingContext() );
	}
}
