/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.model.impl;

import org.hibernate.search.engine.backend.types.ObjectStructure;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.model.StubIndexSchemaDataNode;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.common.impl.StubSearchIndexNodeContext;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.common.impl.StubSingleIndexSearchIndexCompositeNodeContext;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.common.impl.StubSingleIndexSearchIndexValueFieldContext;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.types.impl.StubIndexValueFieldType;

public class StubIndexNode {

	private final StubIndexValueFieldType<?> type;
	private final ObjectStructure objectStructure;
	private final StubIndexSchemaDataNode schemaData;

	public StubIndexNode(StubIndexSchemaDataNode schemaData, StubIndexValueFieldType<?> type, ObjectStructure objectStructure) {
		this.schemaData = schemaData;
		this.type = type;
		this.objectStructure = objectStructure;
	}

	public StubIndexValueFieldType<?> type() {
		return type;
	}

	public ObjectStructure objectStructure() {
		return objectStructure;
	}

	public StubIndexSchemaDataNode schemaData() {
		return schemaData;
	}

	public StubSearchIndexNodeContext toSearchContext() {
		StubIndexSchemaDataNode.Kind kind = schemaData.kind();
		switch ( kind ) {
			case ROOT:
			case OBJECT_FIELD:
				return new StubSingleIndexSearchIndexCompositeNodeContext( schemaData.absolutePath(), objectStructure );
			case VALUE_FIELD:
				return new StubSingleIndexSearchIndexValueFieldContext<>( schemaData.absolutePath(), type );
			case NAMED_PREDICATE:
			case OBJECT_FIELD_TEMPLATE:
			case VALUE_FIELD_TEMPLATE:
			default:
				throw new SearchException( "Cannot create a search context for index schema element of kind " + kind );
		}
	}

}
