/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engineperformance.elasticsearch;

import org.hibernate.search.engineperformance.elasticsearch.datasets.Dataset;
import org.hibernate.search.engineperformance.elasticsearch.model.AbstractBookEntity;
import org.hibernate.search.spi.IndexedTypeIdentifier;
import org.hibernate.search.spi.impl.PojoIndexedTypeIdentifier;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.ThreadParams;

/**
 * @author Yoann Rodiere
 */
@State(Scope.Benchmark)
public class NonStreamQueryParams {

	@Param( { "100" } )
	private int maxResults;

	private Class<?> entityType;

	@Setup(Level.Trial)
	public void setup(NonStreamDatasetHolder dh, ThreadParams threadParams) {
		int threadIndex = threadParams.getSubgroupThreadIndex();
		Dataset<? extends AbstractBookEntity> dataset = dh.getDataset( threadIndex );
		IndexedTypeIdentifier typeId = dataset.getTypeId();
		this.entityType = PojoIndexedTypeIdentifier.convertToLegacy( typeId );
	}

	public int getQueryMaxResults() {
		return maxResults;
	}

	public Class<?> getEntityType() {
		return entityType;
	}
}
