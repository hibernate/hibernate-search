/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.mapper.mapping.building.spi;

import java.util.function.Consumer;

import org.hibernate.search.engine.mapper.model.spi.MappableTypeModel;

/**
 * @param <C> The type of contributors
 */
public interface TypeMetadataContributorProvider<C> {

	/**
	 * @param typeModel The model of a type to retrieve contributors for.
	 * @param contributorConsumer The consumer that will be applied to each contributor.
	 */
	void forEach(MappableTypeModel typeModel, Consumer<C> contributorConsumer);

}
