/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.model.impl;

import org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.model.StubIndexSchemaNode;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaNamedPredicateOptionsStep;

class StubIndexSchemaNamedPredicateBuilder
	implements IndexSchemaNamedPredicateOptionsStep {

	private final StubIndexSchemaNode.Builder builder;

	StubIndexSchemaNamedPredicateBuilder(StubIndexSchemaNode.Builder builder, boolean included) {
		this.builder = builder;
	}

}
