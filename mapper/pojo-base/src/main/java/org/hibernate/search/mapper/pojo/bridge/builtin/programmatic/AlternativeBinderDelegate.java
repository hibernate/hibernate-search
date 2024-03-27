/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.bridge.builtin.programmatic;

import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.mapper.pojo.model.PojoModelProperty;
import org.hibernate.search.util.common.annotation.Incubating;

/**
 * The component responsible for binding one field per alternative,
 * and creating an {@link AlternativeValueBridge}.
 *
 * @param <D> The expected type of alternative discriminator values.
 * The alternative discriminator is designated through the {@link AlternativeBinder#alternativeDiscriminator()} marker.
 * @param <P> The expected type of the field value source, i.e. the property bound to multiple index fields.
 * The field value source is designated through the property name and type passed to
 * {@link AlternativeBinder#create(Class, String, Class, BeanReference)}.
 *
 * @see AlternativeBinder
 */
@Incubating
public interface AlternativeBinderDelegate<D, P> {

	/**
	 * Binds the given field value source to multiple field, i.e.:
	 * <ul>
	 *     <li>Declares one field per alternative.</li>
	 *     <li>Creates a bridge that will route field values to the appropriate field based on a discriminator.</li>
	 * </ul>
	 * @param indexSchemaElement The index schema element to add fields to.
	 * @param fieldValueSource The source of field values, passed to allow looking up metadata (name, exact type, ...).
	 * @return The {@link AlternativeValueBridge} bound to the given property.
	 * @see AlternativeValueBridge
	 */
	AlternativeValueBridge<D, P> bind(IndexSchemaElement indexSchemaElement, PojoModelProperty fieldValueSource);

}
