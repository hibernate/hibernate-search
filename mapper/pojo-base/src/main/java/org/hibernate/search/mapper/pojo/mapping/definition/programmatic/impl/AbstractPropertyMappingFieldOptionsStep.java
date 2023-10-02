/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.mapping.definition.programmatic.impl;

import java.lang.invoke.MethodHandles;
import java.util.Map;

import org.hibernate.search.engine.backend.common.spi.FieldPaths;
import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.mapper.pojo.bridge.ValueBridge;
import org.hibernate.search.mapper.pojo.bridge.mapping.impl.BeanBinder;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.ValueBinder;
import org.hibernate.search.mapper.pojo.extractor.mapping.programmatic.ContainerExtractorPath;
import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoIndexMappingCollectorPropertyNode;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoPropertyMetadataContributor;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingFieldOptionsStep;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingStep;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

abstract class AbstractPropertyMappingFieldOptionsStep<S extends PropertyMappingFieldOptionsStep<?>>
		extends DelegatingPropertyMappingStep
		implements PojoPropertyMetadataContributor, PropertyMappingFieldOptionsStep<S> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	protected final String relativeFieldName;
	final PojoCompositeFieldModelContributor fieldModelContributor;

	private ValueBinder binder;
	private Map<String, Object> params;

	private ContainerExtractorPath extractorPath = ContainerExtractorPath.defaultExtractors();

	AbstractPropertyMappingFieldOptionsStep(PropertyMappingStep delegate, String relativeFieldName,
			PojoCompositeFieldModelContributor.DefaultInitiator fieldTypeDefaultInitiator,
			PojoCompositeFieldModelContributor.Contributor fieldTypeChecker) {
		super( delegate );
		this.relativeFieldName = relativeFieldName;
		if ( relativeFieldName != null && relativeFieldName.contains( FieldPaths.PATH_SEPARATOR_STRING ) ) {
			throw log.invalidFieldNameDotNotAllowed( relativeFieldName );
		}
		this.fieldModelContributor = new PojoCompositeFieldModelContributor( fieldTypeDefaultInitiator );
		// The very first field type contributor will just check that the field type is appropriate.
		// It is only useful if no option is set, since setting an option will perform that check too.
		this.fieldModelContributor.add( fieldTypeChecker );
	}

	@Override
	public void contributeIndexMapping(PojoIndexMappingCollectorPropertyNode collector) {
		collector.value( extractorPath )
				.valueBinder( binder, params, relativeFieldName, fieldModelContributor );
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
	public S valueBinder(ValueBinder binder, Map<String, Object> params) {
		this.binder = binder;
		this.params = params;
		return thisAsS();
	}

	@Override
	public S extractors(ContainerExtractorPath extractorPath) {
		this.extractorPath = extractorPath;
		return thisAsS();
	}

	abstract S thisAsS();
}
