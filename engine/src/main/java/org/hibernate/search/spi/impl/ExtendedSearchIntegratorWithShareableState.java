/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.spi.impl;

import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;

/**
 * Contract for {@code ExtendedSearchintegrator} implementors exposing its shareable state.
 * The state can then be extracted and used to mutate factories.
 *
 * @author Emmanuel Bernard
 */
public interface ExtendedSearchIntegratorWithShareableState extends ExtendedSearchIntegrator, SearchFactoryState {
}
