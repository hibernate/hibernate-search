/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge.builtin.programmatic;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.mapper.pojo.model.PojoModelProperty;
import org.hibernate.search.util.common.annotation.Incubating;

/**
 * A component that routes field values to one of multiple index fields
 * based on a discriminator.
 *
 * @param <D> The expected type of alternative discriminator values.
 * The alternative discriminator is designated through the {@link AlternativeBinder#alternativeDiscriminator()} marker.
 * @param <P> The expected type of the field value source, i.e. the property bound to multiple index fields.
 * The field value source is designated through the property name and type passed to
 * {@link AlternativeBinder#create(Class, String, Class, BeanReference)}.
 *
 * @see AlternativeBinder
 * @see AlternativeBinderDelegate#bind(IndexSchemaElement, PojoModelProperty)
 */
@Incubating
public interface AlternativeValueBridge<D, P> {

	void write(DocumentElement target, D discriminator, P bridgedElement);

}
