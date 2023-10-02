/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.spi;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.jboss.jandex.DotName;

public final class BuiltinAnnotations {

	private BuiltinAnnotations() {
	}

	public static final DotName ROOT_MAPPING =
			DotName.createSimple( "org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.RootMapping" );

	public static final List<DotName> ROOT_MAPPING_ANNOTATIONS = Collections.unmodifiableList( Arrays.asList(
			DotName.createSimple( "org.hibernate.search.mapper.pojo.mapping.definition.annotation.ProjectionConstructor" ),
			DotName.createSimple( "org.hibernate.search.mapper.pojo.mapping.definition.annotation.SearchEntity" )
	) );

}
