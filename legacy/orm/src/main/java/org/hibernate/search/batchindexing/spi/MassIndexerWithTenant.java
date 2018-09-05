/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.batchindexing.spi;

import org.hibernate.search.MassIndexer;

/**
 * A {@link MassIndexer} that can be assigned to a tenant in architectures with multi-tenancy.
 *
 * @author Davide D'Alto
 */
public interface MassIndexerWithTenant extends MassIndexer {

	/**
	 * Set the tenant that is associated to this {@link MassIndexer}.
	 *
	 * @param tenantIdentifier the identifier of the tenant associated this {@link MassIndexer}
	 * @return <tt>this</tt> for method chaining
	 */
	MassIndexerWithTenant tenantIdentifier(String tenantIdentifier);
}
