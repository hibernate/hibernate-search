/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge.builtin.programmatic;

import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.MarkerBinder;
import org.hibernate.search.util.common.annotation.Incubating;

/**
 * A binder for markers that mark a property as the discriminator
 * for alternatives for an {@link AlternativeBinder Alternative bridge}.
 *
 * @see AlternativeBinder#alternativeDiscriminator()
 */
@Incubating
public interface AlternativeDiscriminatorBinder extends MarkerBinder {

	/**
	 * @param id The identifier of the alternative.
	 * This is used to differentiate between multiple alternative discriminators.
	 * @return {@code this}, for method chaining.
	 * @see AlternativeBinder#alternativeId(String)
	 */
	AlternativeDiscriminatorBinder id(String id);

}
