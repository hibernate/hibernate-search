/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.model.dsl.impl;

import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaNamedPredicateOptionsStep;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.model.StubIndexSchemaDataNode;

class StubIndexNamedPredicateBuilder
		implements IndexSchemaNamedPredicateOptionsStep {

	@SuppressWarnings("unused")
	StubIndexNamedPredicateBuilder(StubIndexSchemaDataNode.Builder schemaDataNodeBuilder) {
	}

}
