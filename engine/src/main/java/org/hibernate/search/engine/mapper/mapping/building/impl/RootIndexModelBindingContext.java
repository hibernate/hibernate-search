/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.mapper.mapping.building.impl;

import java.util.Collection;
import java.util.Collections;

import org.hibernate.search.engine.backend.document.IndexObjectFieldAccessor;
import org.hibernate.search.engine.backend.types.converter.spi.ToDocumentIdentifierValueConverter;
import org.hibernate.search.engine.backend.document.model.dsl.spi.IndexSchemaRootNodeBuilder;

public class RootIndexModelBindingContext extends AbstractIndexModelBindingContext<IndexSchemaRootNodeBuilder> {

	public RootIndexModelBindingContext(IndexSchemaRootNodeBuilder indexSchemaObjectNodeBuilder) {
		super( indexSchemaObjectNodeBuilder, indexSchemaObjectNodeBuilder, ConfiguredIndexSchemaNestingContext.root() );
	}

	@Override
	public Collection<IndexObjectFieldAccessor> getParentIndexObjectAccessors() {
		return Collections.emptyList();
	}

	@Override
	public void explicitRouting() {
		indexSchemaObjectNodeBuilder.explicitRouting();
	}

	@Override
	public void idDslConverter(ToDocumentIdentifierValueConverter<?> idConverter) {
		indexSchemaObjectNodeBuilder.idDslConverter( idConverter );
	}
}
