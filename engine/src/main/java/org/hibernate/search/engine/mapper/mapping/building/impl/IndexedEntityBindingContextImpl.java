/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.mapper.mapping.building.impl;

import org.hibernate.search.engine.backend.document.model.dsl.spi.IndexRootBuilder;
import org.hibernate.search.engine.backend.types.converter.FromDocumentValueConverter;
import org.hibernate.search.engine.backend.types.converter.ToDocumentValueConverter;
import org.hibernate.search.engine.common.tree.spi.TreeNestingContext;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexedEntityBindingContext;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexedEntityBindingMapperContext;

public class IndexedEntityBindingContextImpl extends AbstractIndexBindingContext<IndexRootBuilder>
		implements IndexedEntityBindingContext {

	public IndexedEntityBindingContextImpl(IndexedEntityBindingMapperContext mapperContext,
			IndexRootBuilder indexRootBuilder) {
		super( mapperContext, indexRootBuilder, indexRootBuilder, TreeNestingContext.root() );
	}

	@Override
	public void explicitRouting() {
		indexSchemaObjectNodeBuilder.explicitRouting();
	}

	@Override
	public <I> void idDslConverter(Class<I> valueType, ToDocumentValueConverter<I, String> converter) {
		indexSchemaObjectNodeBuilder.idDslConverter( valueType, converter );
	}

	@Override
	public <I> void idProjectionConverter(Class<I> valueType, FromDocumentValueConverter<String, I> converter) {
		indexSchemaObjectNodeBuilder.idProjectionConverter( valueType, converter );
	}

	@Override
	boolean isParentMultivaluedAndWithoutObjectField() {
		// The root has no parent
		return false;
	}
}
