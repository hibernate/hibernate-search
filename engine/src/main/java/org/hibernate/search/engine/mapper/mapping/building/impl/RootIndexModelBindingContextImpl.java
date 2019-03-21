/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.mapper.mapping.building.impl;

import org.hibernate.search.engine.backend.document.model.dsl.spi.IndexSchemaRootNodeBuilder;
import org.hibernate.search.engine.backend.types.converter.spi.ToDocumentIdentifierValueConverter;
import org.hibernate.search.engine.mapper.mapping.building.spi.RootIndexModelBindingContext;

public class RootIndexModelBindingContextImpl extends AbstractIndexModelBindingContext<IndexSchemaRootNodeBuilder>
		implements RootIndexModelBindingContext {

	public RootIndexModelBindingContextImpl(IndexSchemaRootNodeBuilder indexSchemaObjectNodeBuilder) {
		super( indexSchemaObjectNodeBuilder, indexSchemaObjectNodeBuilder, ConfiguredIndexSchemaNestingContext.root() );
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
