/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.mapping.definition.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.hibernate.search.engine.environment.bean.BeanRetrieval;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.IdentifierBinderRef;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.IdentifierBridgeRef;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.PropertyMapping;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.PropertyMappingAnnotationProcessorRef;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.impl.DocumentIdProcessor;

/**
 * Maps a property to the identifier of documents in the index.
 * <p>
 * This annotation is only taken into account on {@link Indexed} types.
 */
@Documented
@Target({ ElementType.METHOD, ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
@PropertyMapping(processor = @PropertyMappingAnnotationProcessorRef(type = DocumentIdProcessor.class,
		retrieval = BeanRetrieval.CONSTRUCTOR))
public @interface DocumentId {

	/**
	 * @return A reference to the identifier bridge to use for document IDs.
	 * Must not be set if {@link #identifierBinder()} is set.
	 * @see IdentifierBridgeRef
	 */
	IdentifierBridgeRef identifierBridge() default @IdentifierBridgeRef;

	/**
	 * @return A reference to the identifier binder to use for document IDs.
	 * Must not be set if {@link #identifierBridge()} is set.
	 * @see IdentifierBinderRef
	 */
	IdentifierBinderRef identifierBinder() default @IdentifierBinderRef;

}
