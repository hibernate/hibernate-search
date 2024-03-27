/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.types.dsl.impl;

import java.util.Collection;

import org.hibernate.search.engine.backend.types.Highlightable;
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
		builder.modifier( b -> b.analyzerName( analyzerName ) );
		return this;
	}

	@Override
	public StubStringIndexFieldTypeOptionsStep searchAnalyzer(String searchAnalyzerName) {
		builder.modifier( b -> b.searchAnalyzerName( searchAnalyzerName ) );
		return this;
	}

	@Override
	public StubStringIndexFieldTypeOptionsStep normalizer(String normalizerName) {
		builder.modifier( b -> b.normalizerName( normalizerName ) );
		return this;
	}

	@Override
	public StubStringIndexFieldTypeOptionsStep norms(Norms norms) {
		builder.modifier( b -> b.norms( norms ) );
		return this;
	}

	@Override
	public StubStringIndexFieldTypeOptionsStep termVector(TermVector termVector) {
		builder.modifier( b -> b.termVector( termVector ) );
		return this;
	}

	@Override
	public StubStringIndexFieldTypeOptionsStep highlightable(Collection<Highlightable> highlightable) {
		builder.modifier( b -> b.highlightable( highlightable ) );
		return this;
	}
}
