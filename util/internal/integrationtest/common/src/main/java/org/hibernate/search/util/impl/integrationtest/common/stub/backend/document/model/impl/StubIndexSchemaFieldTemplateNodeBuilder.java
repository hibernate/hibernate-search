/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.model.impl;

import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaFieldTemplateOptionsStep;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.model.StubIndexSchemaNode;

class StubIndexSchemaFieldTemplateNodeBuilder
		implements IndexSchemaFieldTemplateOptionsStep<StubIndexSchemaFieldTemplateNodeBuilder> {

	private final StubIndexSchemaNode.Builder builder;

	StubIndexSchemaFieldTemplateNodeBuilder(StubIndexSchemaNode.Builder builder) {
		this.builder = builder;
	}

	@Override
	public StubIndexSchemaFieldTemplateNodeBuilder matchingPathGlob(String pathGlob) {
		builder.matchingPathGlob( pathGlob );
		return this;
	}

	@Override
	public StubIndexSchemaFieldTemplateNodeBuilder multiValued() {
		builder.multiValued( true );
		return this;
	}
}
