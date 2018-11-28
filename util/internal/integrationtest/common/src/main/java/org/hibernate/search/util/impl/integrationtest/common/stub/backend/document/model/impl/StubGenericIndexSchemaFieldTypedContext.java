/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.model.impl;

import org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.model.StubIndexSchemaNode;

class StubGenericIndexSchemaFieldTypedContext<F>
		extends AbstractStubStandardIndexSchemaFieldTypedContext<StubGenericIndexSchemaFieldTypedContext<F>, F> {

	StubGenericIndexSchemaFieldTypedContext(StubIndexSchemaNode.Builder builder, Class<F> inputType, boolean included) {
		super( builder, inputType, included );
	}

	@Override
	StubGenericIndexSchemaFieldTypedContext<F> thisAsS() {
		return this;
	}

}
