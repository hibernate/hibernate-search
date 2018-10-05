/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.model.impl;

import org.hibernate.search.engine.backend.document.model.dsl.StringIndexSchemaFieldTypedContext;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.model.StubIndexSchemaNode;

class StubStringIndexSchemaFieldTypedContext
		extends StubStandardIndexSchemaFieldTypedContext<StubStringIndexSchemaFieldTypedContext, String>
		implements StringIndexSchemaFieldTypedContext<StubStringIndexSchemaFieldTypedContext> {

	StubStringIndexSchemaFieldTypedContext(StubIndexSchemaNode.Builder builder, boolean included) {
		super( builder, String.class, included );
	}

	@Override
	StubStringIndexSchemaFieldTypedContext thisAsS() {
		return this;
	}

	@Override
	public StubStringIndexSchemaFieldTypedContext analyzer(String analyzerName) {
		builder.analyzerName( analyzerName );
		return this;
	}

	@Override
	public StubStringIndexSchemaFieldTypedContext normalizer(String normalizerName) {
		builder.normalizerName( normalizerName );
		return this;
	}

}
