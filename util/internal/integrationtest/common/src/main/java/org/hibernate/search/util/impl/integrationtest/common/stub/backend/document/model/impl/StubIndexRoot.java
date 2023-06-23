/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.model.impl;

import java.util.Map;

import org.hibernate.search.engine.backend.document.model.spi.AbstractIndexRoot;
import org.hibernate.search.engine.search.common.spi.SearchIndexSchemaElementContextHelper;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.model.StubIndexSchemaDataNode;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.common.impl.StubSearchIndexScope;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.types.impl.StubIndexCompositeNodeType;

public final class StubIndexRoot
		extends AbstractIndexRoot<
				StubIndexRoot,
				StubSearchIndexScope,
				StubIndexCompositeNodeType,
				StubIndexField>
		implements StubIndexCompositeNode {

	private final StubIndexSchemaDataNode schemaData;

	public StubIndexRoot(StubIndexCompositeNodeType type, Map<String, StubIndexField> notYetInitializedStaticChildren,
			StubIndexSchemaDataNode schemaData) {
		super( type, notYetInitializedStaticChildren );
		this.schemaData = schemaData;
	}

	@Override
	protected StubIndexRoot self() {
		return this;
	}

	@Override
	public StubIndexObjectField toObjectField() {
		return SearchIndexSchemaElementContextHelper.throwingToObjectField( this );
	}

	@Override
	public StubIndexValueField<?> toValueField() {
		return SearchIndexSchemaElementContextHelper.throwingToValueField( this );
	}

	@Override
	public StubIndexSchemaDataNode schemaData() {
		return schemaData;
	}
}
