/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.types.dsl.impl;

import org.hibernate.search.engine.backend.types.dsl.StringIndexFieldTypeContext;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.model.StubIndexSchemaNode;

class StubStringIndexFieldTypeContext
		extends AbstractStubStandardIndexFieldTypeContext<StubStringIndexFieldTypeContext, String>
		implements StringIndexFieldTypeContext<StubStringIndexFieldTypeContext> {

	StubStringIndexFieldTypeContext(StubIndexSchemaNode.Builder builder, boolean included) {
		super( builder, String.class, included );
	}

	@Override
	StubStringIndexFieldTypeContext thisAsS() {
		return this;
	}

	@Override
	public StubStringIndexFieldTypeContext analyzer(String analyzerName) {
		builder.analyzerName( analyzerName );
		return this;
	}

	@Override
	public StubStringIndexFieldTypeContext normalizer(String normalizerName) {
		builder.normalizerName( normalizerName );
		return this;
	}

}
