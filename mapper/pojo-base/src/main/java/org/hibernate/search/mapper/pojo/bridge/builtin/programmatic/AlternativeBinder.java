/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.bridge.builtin.programmatic;

import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.mapper.pojo.bridge.builtin.alternative.impl.AlternativeBinderImpl;
import org.hibernate.search.mapper.pojo.bridge.builtin.alternative.impl.AlternativeDiscriminatorBinderImpl;
import org.hibernate.search.mapper.pojo.bridge.builtin.annotation.AlternativeDiscriminator;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.MarkerBinder;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.TypeBinder;
import org.hibernate.search.mapper.pojo.model.PojoModelProperty;
import org.hibernate.search.util.common.annotation.Incubating;

/**
 * The binder that sets up {@link AlternativeValueBridge}s.
 * <p>
 * Alternative field bridges solve the problem of mapping one property to multiple fields
 * depending on the value of another property.
 * <p>
 * One use case is when an entity has text properties whose content
 * is in a different language depending on the value of another property, say {@code language}.
 * In that case, you probably want to analyze the text differently depending on the language.
 * This binder solves the problem this way:
 * <ul>
 * <li>at bootstrap, declare one index field per language ({@code title_en}, {@code title_fr}, etc.),
 * assigning a different analyzer to each field;</li>
 * <li>at runtime, put the content of the text property in a different field based on the language.
 * </ul>
 * <p>
 * In order to use this binder, you will need to:
 * <ul>
 *     <li>annotate a property with {@link AlternativeDiscriminator} (e.g. the {@code language} property)</li>
 *     <li>implement an {@link AlternativeBinderDelegate} that will declare the index fields
 *     (e.g. one field per language) and create an {@link AlternativeValueBridge}.
 *     This bridge is responsible for passing the property value to the relevant field at runtime</li>
 *     <li>apply the {@link AlternativeBinder} to the type hosting the properties
 *     (e.g. the type declaring the {@code language} property and the multi-language text properties).
 *     Generally you will want to create your own annotation for that.</li>
 * </ul>
 *
 * @see AlternativeBinder
 * @see AlternativeBinderDelegate#bind(IndexSchemaElement, PojoModelProperty)
 */
@Incubating
public interface AlternativeBinder extends TypeBinder {

	/**
	 * @param id The identifier of the alternative.
	 * This is used to differentiate between multiple alternative discriminators:
	 * {@link AlternativeDiscriminatorBinder#id(String) assign an id when building each discriminator marker},
	 * then select the same id here.
	 * @return {@code this}, for method chaining.
	 */
	AlternativeBinder alternativeId(String id);

	/**
	 * @param discriminatorType The expected type of alternative discriminator values.
	 * The alternative discriminator is designated through the {@link #alternativeDiscriminator()} marker.
	 * @param fieldValueSourcePropertyName The expected type of the field value source,
	 * i.e. the property bound to a different field based on the discriminator.
	 * @param fieldValueSourcePropertyType The expected type of the field value source.
	 * @param delegateRef A reference to the {@link AlternativeBinderDelegate},
	 * responsible for binding one field per alternative and creating an {@link AlternativeValueBridge}.
	 * @return An {@link AlternativeBinder}.
	 * @param <D> The expected type of alternative discriminator values.
	 * @param <P> The expected type of the field value source.
	 */
	static <D, P> AlternativeBinder create(Class<D> discriminatorType,
			String fieldValueSourcePropertyName, Class<P> fieldValueSourcePropertyType,
			BeanReference<? extends AlternativeBinderDelegate<D, P>> delegateRef) {
		return new AlternativeBinderImpl<>( discriminatorType, fieldValueSourcePropertyName,
				fieldValueSourcePropertyType, delegateRef );
	}

	/**
	 * @return A {@link MarkerBinder} for the alternative discriminator, to be applied on a property.
	 * @see LatitudeLongitudeMarkerBinder
	 */
	static AlternativeDiscriminatorBinder alternativeDiscriminator() {
		return new AlternativeDiscriminatorBinderImpl();
	}

}
