/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.document.model.impl;

import org.hibernate.search.backend.elasticsearch.document.model.impl.esnative.DataType;
import org.hibernate.search.backend.elasticsearch.document.model.impl.esnative.PropertyMapping;
import org.hibernate.search.backend.elasticsearch.gson.impl.JsonObjectAccessor;

public class IndexSchemaObjectPropertyNodeBuilder extends AbstractIndexSchemaCompositeNodeBuilder<PropertyMapping> {

	public IndexSchemaObjectPropertyNodeBuilder(JsonObjectAccessor accessor) {
		super( accessor );
	}

	@Override
	protected PropertyMapping createMapping() {
		PropertyMapping mapping = new PropertyMapping();
		mapping.setType( DataType.OBJECT );
		return mapping;
	}
}
