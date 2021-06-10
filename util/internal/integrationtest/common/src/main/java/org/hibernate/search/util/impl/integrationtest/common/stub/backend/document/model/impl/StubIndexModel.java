/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.model.impl;

import java.util.Collections;
import java.util.Map;

import org.hibernate.search.engine.backend.document.model.spi.AbstractIndexModel;
import org.hibernate.search.engine.backend.types.converter.spi.DocumentIdentifierValueConverter;

public class StubIndexModel extends AbstractIndexModel<StubIndexModel, StubIndexRoot, StubIndexField> {

	public StubIndexModel(String hibernateSearchIndexName, String mappedTypeName,
			DocumentIdentifierValueConverter<?> idDslConverter,
			StubIndexRoot root, Map<String, StubIndexField> fields) {
		super( hibernateSearchIndexName, mappedTypeName, idDslConverter, root, fields, Collections.emptyList() );
	}

	@Override
	protected StubIndexModel self() {
		return this;
	}

}
