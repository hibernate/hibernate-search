/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.types.dsl.impl;

import org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.model.StubIndexSchemaNode;

class StubGenericIndexFieldTypeContext<F>
		extends AbstractStubStandardIndexFieldTypeContext<StubGenericIndexFieldTypeContext<F>, F> {

	StubGenericIndexFieldTypeContext(StubIndexSchemaNode.Builder builder, Class<F> inputType, boolean included) {
		super( builder, inputType, included );
	}

	@Override
	StubGenericIndexFieldTypeContext<F> thisAsS() {
		return this;
	}

}
