/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend;

import org.hibernate.search.engine.backend.types.dsl.IndexFieldTypeFactory;
import org.hibernate.search.engine.backend.types.dsl.IndexFieldTypeFactoryExtension;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.types.dsl.impl.StubIndexFieldTypeFactory;

public class StubBackendExtension implements IndexFieldTypeFactoryExtension<StubIndexFieldTypeFactory> {

	private static final StubBackendExtension INSTANCE = new StubBackendExtension();

	public static StubBackendExtension get() {
		return INSTANCE;
	}

	private StubBackendExtension() {
	}

	@Override
	public StubIndexFieldTypeFactory extendOrFail(IndexFieldTypeFactory original) {
		if ( original instanceof StubIndexFieldTypeFactory ) {
			return (StubIndexFieldTypeFactory) original;
		}
		else {
			throw new SearchException( original + " cannot be extended by " + this );
		}
	}
}
