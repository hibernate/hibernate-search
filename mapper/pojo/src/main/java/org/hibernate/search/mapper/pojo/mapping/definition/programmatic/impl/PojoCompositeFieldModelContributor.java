/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.definition.programmatic.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.hibernate.search.engine.backend.types.dsl.StandardIndexFieldTypeContext;
import org.hibernate.search.mapper.pojo.mapping.building.spi.FieldModelContributor;
import org.hibernate.search.mapper.pojo.mapping.building.spi.FieldModelContributorIndirectContext;

class PojoCompositeFieldModelContributor<C extends StandardIndexFieldTypeContext<?, ?>>
		implements FieldModelContributor {
	private final Function<StandardIndexFieldTypeContext<?, ?>, C> contextConverter;
	private final List<BiConsumer<C, FieldModelContributorIndirectContext>> delegates = new ArrayList<>();

	PojoCompositeFieldModelContributor(Function<StandardIndexFieldTypeContext<?, ?>, C> contextConverter) {
		this.contextConverter = contextConverter;
	}

	public void add(BiConsumer<C, FieldModelContributorIndirectContext> delegate) {
		delegates.add( delegate );
	}

	@Override
	public void defaultDecimalScale(int defaultScale) {
		delegates.add( (c, b) -> b.defaultDecimalScale( defaultScale ) );
	}

	@Override
	public void contribute(StandardIndexFieldTypeContext<?, ?> context, FieldModelContributorIndirectContext bridgeContext) {
		C convertedContext = contextConverter.apply( context );
		delegates.forEach( c -> c.accept( convertedContext, bridgeContext ) );
	}
}
