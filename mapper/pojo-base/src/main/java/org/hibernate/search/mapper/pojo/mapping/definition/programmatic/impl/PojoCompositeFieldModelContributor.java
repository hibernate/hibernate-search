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

import org.hibernate.search.engine.backend.types.dsl.StandardIndexFieldTypeOptionsStep;
import org.hibernate.search.mapper.pojo.bridge.binding.spi.FieldModelContributor;
import org.hibernate.search.mapper.pojo.bridge.binding.spi.FieldModelContributorBridgeContext;

class PojoCompositeFieldModelContributor<C extends StandardIndexFieldTypeOptionsStep<?, ?>>
		implements FieldModelContributor {
	private final Function<StandardIndexFieldTypeOptionsStep<?, ?>, C> typeOptionsStepCaster;
	private final List<BiConsumer<C, FieldModelContributorBridgeContext>> delegates = new ArrayList<>();

	PojoCompositeFieldModelContributor(Function<StandardIndexFieldTypeOptionsStep<?, ?>, C> typeOptionsStepCaster) {
		this.typeOptionsStepCaster = typeOptionsStepCaster;
	}

	public void add(BiConsumer<C, FieldModelContributorBridgeContext> delegate) {
		delegates.add( delegate );
	}

	@Override
	public void contribute(StandardIndexFieldTypeOptionsStep<?, ?> optionsStep,
			FieldModelContributorBridgeContext bridgeContext) {
		C castedOptionsStep = typeOptionsStepCaster.apply( optionsStep );
		delegates.forEach( c -> c.accept( castedOptionsStep, bridgeContext ) );
	}
}
