/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.types.dsl.impl;

import org.hibernate.search.engine.backend.types.Norms;
import org.hibernate.search.engine.backend.types.dsl.StringIndexFieldTypeContext;

class StubStringIndexFieldTypeContext
		extends AbstractStubStandardIndexFieldTypeContext<StubStringIndexFieldTypeContext, String>
		implements StringIndexFieldTypeContext<StubStringIndexFieldTypeContext> {

	StubStringIndexFieldTypeContext() {
		super( String.class );
	}

	@Override
	StubStringIndexFieldTypeContext thisAsS() {
		return this;
	}

	@Override
	public StubStringIndexFieldTypeContext analyzer(String analyzerName) {
		modifiers.add( b -> b.analyzerName( analyzerName ) );
		return this;
	}

	@Override
	public StubStringIndexFieldTypeContext normalizer(String normalizerName) {
		modifiers.add( b -> b.normalizerName( normalizerName ) );
		return this;
	}

	@Override
	public StubStringIndexFieldTypeContext norms(Norms norms) {
		modifiers.add( b -> b.norms( norms ) );
		return this;
	}
}
