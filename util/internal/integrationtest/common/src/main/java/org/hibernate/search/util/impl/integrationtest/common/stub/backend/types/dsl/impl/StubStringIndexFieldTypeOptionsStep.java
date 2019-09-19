/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.types.dsl.impl;

import org.hibernate.search.engine.backend.types.Norms;
import org.hibernate.search.engine.backend.types.TermVector;
import org.hibernate.search.engine.backend.types.dsl.StringIndexFieldTypeOptionsStep;

class StubStringIndexFieldTypeOptionsStep
		extends AbstractStubStandardIndexFieldTypeOptionsStep<StubStringIndexFieldTypeOptionsStep, String>
		implements StringIndexFieldTypeOptionsStep<StubStringIndexFieldTypeOptionsStep> {

	StubStringIndexFieldTypeOptionsStep() {
		super( String.class );
	}

	@Override
	StubStringIndexFieldTypeOptionsStep thisAsS() {
		return this;
	}

	@Override
	public StubStringIndexFieldTypeOptionsStep analyzer(String analyzerName) {
		modifiers.add( b -> b.analyzerName( analyzerName ) );
		return this;
	}

	@Override
	public StubStringIndexFieldTypeOptionsStep searchAnalyzer(String searchAnalyzerName) {
		modifiers.add( b -> b.searchAnalyzerName( searchAnalyzerName ) );
		return this;
	}

	@Override
	public StubStringIndexFieldTypeOptionsStep normalizer(String normalizerName) {
		modifiers.add( b -> b.normalizerName( normalizerName ) );
		return this;
	}

	@Override
	public StubStringIndexFieldTypeOptionsStep norms(Norms norms) {
		modifiers.add( b -> b.norms( norms ) );
		return this;
	}

	@Override
	public StubStringIndexFieldTypeOptionsStep termVector(TermVector termVector) {
		modifiers.add( b -> b.termVector( termVector ) );
		return this;
	}
}
