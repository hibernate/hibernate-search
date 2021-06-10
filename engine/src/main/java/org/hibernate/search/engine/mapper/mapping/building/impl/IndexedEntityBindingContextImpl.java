/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.mapper.mapping.building.impl;

import org.hibernate.search.engine.backend.document.model.dsl.spi.IndexRootBuilder;
import org.hibernate.search.engine.backend.types.converter.spi.DocumentIdentifierValueConverter;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexedEntityBindingContext;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexedEntityBindingMapperContext;

public class IndexedEntityBindingContextImpl extends AbstractIndexBindingContext<IndexRootBuilder>
		implements IndexedEntityBindingContext {

	public IndexedEntityBindingContextImpl(IndexedEntityBindingMapperContext mapperContext,
			IndexRootBuilder indexRootBuilder) {
		super( mapperContext, indexRootBuilder, indexRootBuilder, ConfiguredIndexSchemaNestingContext.root() );
	}

	@Override
	public void explicitRouting() {
		indexSchemaObjectNodeBuilder.explicitRouting();
	}

	@Override
	public void idDslConverter(DocumentIdentifierValueConverter<?> idConverter) {
		indexSchemaObjectNodeBuilder.idDslConverter( idConverter );
	}

	@Override
	boolean isParentMultivaluedAndWithoutObjectField() {
		// The root has no parent
		return false;
	}
}
