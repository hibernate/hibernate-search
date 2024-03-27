/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.model.dsl.impl;

import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaFieldTemplateOptionsStep;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.model.StubIndexSchemaDataNode;

class StubIndexFieldTemplateNodeBuilder
		implements IndexSchemaFieldTemplateOptionsStep<StubIndexFieldTemplateNodeBuilder> {

	private final StubIndexSchemaDataNode.Builder schemaDataNodeBuilder;

	StubIndexFieldTemplateNodeBuilder(StubIndexSchemaDataNode.Builder schemaDataNodeBuilder) {
		this.schemaDataNodeBuilder = schemaDataNodeBuilder;
	}

	@Override
	public StubIndexFieldTemplateNodeBuilder matchingPathGlob(String pathGlob) {
		schemaDataNodeBuilder.matchingPathGlob( pathGlob );
		return this;
	}

	@Override
	public StubIndexFieldTemplateNodeBuilder multiValued() {
		schemaDataNodeBuilder.multiValued( true );
		return this;
	}
}
