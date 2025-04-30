/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.processor.annotation.processing.impl;

import java.util.HashSet;
import java.util.Set;

import javax.annotation.processing.Messager;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.ProgrammaticMappingConfigurationContext;

public record ProcessorAnnotationProcessorContext(  Elements elements, Types types, Messager messager,
													ProgrammaticMappingConfigurationContext programmaticMapping,
													Set<TypeElement> processedTypes) {
	public ProcessorAnnotationProcessorContext(Elements elements, Types types, Messager messager,
			ProgrammaticMappingConfigurationContext programmaticMapping) {
		this( elements, types, messager, programmaticMapping, new HashSet<>() );
	}
}
