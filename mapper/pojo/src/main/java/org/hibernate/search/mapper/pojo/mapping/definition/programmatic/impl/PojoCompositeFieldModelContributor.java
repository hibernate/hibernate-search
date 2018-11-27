/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.definition.programmatic.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import org.hibernate.search.engine.backend.document.model.dsl.StandardIndexSchemaFieldTypedContext;
import org.hibernate.search.engine.mapper.mapping.building.spi.FieldModelContributor;

class PojoCompositeFieldModelContributor<C extends StandardIndexSchemaFieldTypedContext<?, ?>>
		implements FieldModelContributor {
	private final Function<StandardIndexSchemaFieldTypedContext<?, ?>, C> contextConverter;
	private final List<Consumer<C>> delegates = new ArrayList<>();

	PojoCompositeFieldModelContributor(Function<StandardIndexSchemaFieldTypedContext<?, ?>, C> contextConverter) {
		this.contextConverter = contextConverter;
	}

	public void add(Consumer<C> delegate) {
		delegates.add( delegate );
	}

	@Override
	public void contribute(StandardIndexSchemaFieldTypedContext<?, ?> context) {
		C convertedContext = contextConverter.apply( context );
		delegates.forEach( c -> c.accept( convertedContext ) );
	}
}
