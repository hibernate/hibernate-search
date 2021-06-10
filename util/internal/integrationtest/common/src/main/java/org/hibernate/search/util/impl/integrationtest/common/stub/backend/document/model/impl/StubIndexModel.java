/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.model.impl;

import java.util.Map;

import org.hibernate.search.engine.backend.types.converter.spi.DocumentIdentifierValueConverter;

public class StubIndexModel {

	private final String hibernateSearchIndexName;
	private final DocumentIdentifierValueConverter<?> idDslConverter;
	private final StubIndexRoot root;
	private final Map<String, StubIndexField> fields;

	public StubIndexModel(String hibernateSearchIndexName, DocumentIdentifierValueConverter<?> idDslConverter,
			StubIndexRoot root, Map<String, StubIndexField> fields) {
		this.hibernateSearchIndexName = hibernateSearchIndexName;
		this.idDslConverter = idDslConverter;
		this.root = root;
		this.fields = fields;
	}

	public String hibernateSearchName() {
		return hibernateSearchIndexName;
	}

	public DocumentIdentifierValueConverter<?> idDslConverter() {
		return idDslConverter;
	}

	public StubIndexRoot root() {
		return root;
	}

	public StubIndexField fieldOrNull(String name) {
		return fields.get( name );
	}
}
