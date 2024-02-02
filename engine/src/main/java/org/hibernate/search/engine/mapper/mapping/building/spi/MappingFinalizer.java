/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.mapper.mapping.building.spi;

import org.hibernate.search.engine.common.spi.SearchIntegrationFinalizer;
import org.hibernate.search.engine.mapper.mapping.spi.MappingImplementor;

/**
 * An object responsible for finalizing a mapping.
 *
 * @param <PBM> The type of pre-built mappings
 * @param <M> The type of fully-built mappings
 * @see SearchIntegrationFinalizer#finalizeMapping(MappingKey, MappingFinalizer)
 */
public interface MappingFinalizer<PBM, M> {

	/**
	 * @param context The context, including configuration properties.
	 * @param partiallyBuiltMapping The partially built mapping.
	 * @return The fully-built mapping.
	 * @throws RuntimeException If something went wrong when finalizing the mapping.
	 * @throws MappingAbortedException If something went wrong when finalizing the mapping.
	 */
	MappingImplementor<M> finalizeMapping(MappingFinalizationContext context, PBM partiallyBuiltMapping)
			throws MappingAbortedException;

}
