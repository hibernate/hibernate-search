/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.model.impl;

import java.util.Map;

import org.hibernate.search.engine.backend.document.model.spi.AbstractIndexObjectField;
import org.hibernate.search.engine.backend.document.model.spi.IndexFieldInclusion;
import org.hibernate.search.engine.search.common.spi.SearchIndexSchemaElementContextHelper;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.model.StubIndexSchemaDataNode;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.common.impl.StubSearchIndexScope;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.types.impl.StubIndexCompositeNodeType;

public final class StubIndexObjectField
		extends AbstractIndexObjectField<
						StubIndexObjectField,
						StubSearchIndexScope,
						StubIndexCompositeNodeType,
						StubIndexCompositeNode,
						StubIndexField
				>
		implements StubIndexCompositeNode, StubIndexField {

	private final StubIndexSchemaDataNode schemaData;

	public StubIndexObjectField(StubIndexCompositeNode parent, String relativeFieldName,
			StubIndexCompositeNodeType type, IndexFieldInclusion inclusion, boolean multiValued,
			Map<String, StubIndexField> notYetInitializedStaticChildren,
			StubIndexSchemaDataNode schemaData) {
		super( parent, relativeFieldName, type, inclusion, multiValued, notYetInitializedStaticChildren );
		this.schemaData = schemaData;
	}

	@Override
	protected StubIndexObjectField self() {
		return this;
	}

	@Override
	public StubIndexCompositeNode toComposite() {
		return this;
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
