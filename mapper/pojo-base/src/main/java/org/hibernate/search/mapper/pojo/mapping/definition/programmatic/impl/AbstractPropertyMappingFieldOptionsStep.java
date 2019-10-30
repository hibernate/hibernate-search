/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.definition.programmatic.impl;

import org.hibernate.search.engine.backend.types.Aggregable;
import org.hibernate.search.engine.backend.types.Searchable;
import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.mapper.pojo.bridge.ValueBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.spi.FieldModelContributor;
import org.hibernate.search.mapper.pojo.bridge.mapping.impl.BeanBinder;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.ValueBinder;
import org.hibernate.search.mapper.pojo.extractor.mapping.programmatic.ContainerExtractorPath;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoMappingCollectorPropertyNode;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoPropertyMetadataContributor;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingFieldOptionsStep;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingStep;
import org.hibernate.search.mapper.pojo.model.additionalmetadata.building.spi.PojoAdditionalMetadataCollectorPropertyNode;


abstract class AbstractPropertyMappingFieldOptionsStep<S extends PropertyMappingFieldOptionsStep<?>>
		extends DelegatingPropertyMappingStep
		implements PojoPropertyMetadataContributor, PropertyMappingFieldOptionsStep<S> {

	private final String relativeFieldName;

	final PojoCompositeFieldModelContributor fieldModelContributor;

	private ValueBinder binder;

	private ContainerExtractorPath extractorPath = ContainerExtractorPath.defaultExtractors();

	AbstractPropertyMappingFieldOptionsStep(PropertyMappingStep parent, String relativeFieldName,
			FieldModelContributor fieldTypeChecker) {
		super( parent );
		this.relativeFieldName = relativeFieldName;
		this.fieldModelContributor = new PojoCompositeFieldModelContributor();
		// The very first field contributor will just check that the field type is appropriate.
		// It is only useful if no option is set, since setting an option will perform that check too.
		this.fieldModelContributor.add( fieldTypeChecker );
	}

	@Override
	public void contributeAdditionalMetadata(PojoAdditionalMetadataCollectorPropertyNode collector) {
		// Nothing to do
	}

	@Override
	public void contributeMapping(PojoMappingCollectorPropertyNode collector) {
		collector.value( extractorPath )
				.valueBinder( binder, relativeFieldName, fieldModelContributor );
	}

	abstract S thisAsS();

	@Override
	public S projectable(Projectable projectable) {
		fieldModelContributor.add( c -> c.getStandardTypeOptionsStep().projectable( projectable ) );
		return thisAsS();
	}

	@Override
	public S searchable(Searchable searchable) {
		fieldModelContributor.add( c -> c.getStandardTypeOptionsStep().searchable( searchable ) );
		return thisAsS();
	}

	@Override
	public S aggregable(Aggregable aggregable) {
		fieldModelContributor.add( c -> c.getStandardTypeOptionsStep().aggregable( aggregable ) );
		return thisAsS();
	}

	@Override
	public S valueBridge(Class<? extends ValueBridge<?, ?>> bridgeClass) {
		return valueBridge( BeanReference.of( bridgeClass ) );
	}

	@Override
	public S valueBridge(BeanReference<? extends ValueBridge<?, ?>> bridgeReference) {
		return valueBinder( new BeanBinder( bridgeReference ) );
	}

	@Override
	public S valueBinder(ValueBinder binder) {
		this.binder = binder;
		return thisAsS();
	}

	@Override
	public S extractors(ContainerExtractorPath extractorPath) {
		this.extractorPath = extractorPath;
		return thisAsS();
	}
}
