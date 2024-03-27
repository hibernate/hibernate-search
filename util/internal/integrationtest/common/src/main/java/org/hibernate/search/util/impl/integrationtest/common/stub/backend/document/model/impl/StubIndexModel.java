/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.model.impl;

import java.util.Collections;
import java.util.Map;

import org.hibernate.search.engine.backend.analysis.spi.AnalysisDescriptorRegistry;
import org.hibernate.search.engine.backend.document.model.spi.AbstractIndexModel;
import org.hibernate.search.engine.backend.document.model.spi.IndexIdentifier;

public class StubIndexModel extends AbstractIndexModel<StubIndexModel, StubIndexRoot, StubIndexField> {

	public StubIndexModel(AnalysisDescriptorRegistry analysisDescriptorRegistry, String hibernateSearchIndexName,
			String mappedTypeName,
			IndexIdentifier identifier, StubIndexRoot root, Map<String, StubIndexField> fields) {
		super( analysisDescriptorRegistry, hibernateSearchIndexName, mappedTypeName, identifier, root, fields,
				Collections.emptyList() );
	}

	@Override
	protected StubIndexModel self() {
		return this;
	}

}
