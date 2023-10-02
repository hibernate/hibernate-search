/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.model.path.impl;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.hibernate.search.mapper.pojo.model.path.PojoModelPathPropertyNode;
import org.hibernate.search.mapper.pojo.model.path.PojoModelPathValueNode;
import org.hibernate.search.mapper.pojo.model.path.spi.PojoPathDefinition;
import org.hibernate.search.mapper.pojo.model.path.spi.PojoPathDefinitionProvider;

/**
 * A {@link PojoPathDefinitionProvider} using a simple string representation of dirty paths,
 * in the form "propertyA.propertyB.propertyC".
 * <p>
 * See {@link PojoModelPathPropertyNode#toPropertyString()}.
 */
public class SimplePojoPathsDefinitionProvider implements PojoPathDefinitionProvider {

	public static final SimplePojoPathsDefinitionProvider INSTANCE = new SimplePojoPathsDefinitionProvider();

	private SimplePojoPathsDefinitionProvider() {
	}

	@Override
	public List<String> preDefinedOrdinals() {
		return Collections.emptyList(); // No pre-defined ordinals
	}

	@Override
	public PojoPathDefinition interpretPath(PojoModelPathValueNode source) {
		Set<String> stringRepresentations = new LinkedHashSet<>();
		stringRepresentations.add( source.parent().toPropertyString() );
		return new PojoPathDefinition( stringRepresentations, Optional.empty() );
	}
}
