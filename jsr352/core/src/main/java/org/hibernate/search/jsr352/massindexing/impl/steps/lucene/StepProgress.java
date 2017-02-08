/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jsr352.massindexing.impl.steps.lucene;

import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.jboss.logging.Logger;

/**
 * Step level progress. It contains the indexing progress of the step level. In another word, it is the sum of all the
 * elementary, partition-local level progress. The progress is initialized at the first start of the indexing job and
 * reused after the restart.
 *
 * @author Mincong Huang
 */
public class StepProgress implements Serializable {

	private static final Logger LOGGER = Logger.getLogger( StepProgress.class );
	private static final long serialVersionUID = 7808926033388850340L;

	/**
	 * A map of the total number of rows having already been indexed across all the partitions. Key: the partition id;
	 * Value: the number of rows indexed.
	 */
	private Map<Integer, Long> partitionProgress;

	/**
	 * A map of the total number of rows to index across all the partitions. Key: the partition id; Value: the number of
	 * rows to index.
	 */
	private Map<Integer, Long> partitionTotal;

	/**
	 * A map of the total number of rows having already been indexed across all the entity types. Key: the entity name
	 * in string; Value: the number of rows indexed.
	 */
	private Map<String, Long> entityProgress;

	/**
	 * A map of the total number of rows to index across all the entity types. Key: the entity name in string; Value:
	 * the number of rows to index.
	 */
	private Map<String, Long> entityTotal;

	public StepProgress() {
		partitionProgress = new HashMap<>();
		partitionTotal = new HashMap<>();
		entityProgress = new HashMap<>();
		entityTotal = new HashMap<>();
	}

	/**
	 * Update the step-level indexing progress using the partition-level indexing progress. (step-level is higher, one
	 * step contains multiple partitions)
	 *
	 * @param pp partition-level indexing progress
	 */
	public void updateProgress(PartitionProgress pp) {
		long prevDone = partitionProgress.getOrDefault( pp.getPartitionId(), 0L );
		long currDone = pp.getWorkDone();
		if ( currDone < prevDone ) {
			throw new ArithmeticException( "Current indexed works (" + currDone
					+ " indexed) is smaller than previous indexed works ("
					+ prevDone + " indexed)." );
		}
		increment( pp.getEntityName(), currDone - prevDone );
		increment( pp.getPartitionId(), currDone - prevDone );
	}

	private void increment(String entityName, long increment) {
		long prevDone = entityProgress.getOrDefault( entityName, 0L );
		entityProgress.put( entityName, prevDone + increment );
	}

	private void increment(int pid, long increment) {
		long prevDone = partitionProgress.getOrDefault( pid, 0L );
		partitionProgress.put( pid, prevDone + increment );
	}

	/**
	 * Get the progress of a given partition ID.
	 *
	 * @param pid partition ID
	 * @return a progress value varies between {@literal [0.0, 1.0]}.
	 */
	public double getProgress(int pid) {
		if ( !partitionProgress.containsKey( pid )
				|| !partitionTotal.containsKey( pid ) ) {
			throw new NullPointerException( "partitionId=" + pid + " not found." );
		}
		return partitionProgress.get( pid ) * 1.0 / partitionTotal.get( pid );
	}

	/**
	 * Get the progress of a given entity.
	 *
	 * @param entityName the name of entity
	 * @return a progress value varies between {@literal [0.0, 1.0]}.
	 */
	public double getProgress(String entityName) {
		if ( !entityProgress.containsKey( entityName )
				|| !entityTotal.containsKey( entityName ) ) {
			throw new NullPointerException( "entityName=" + entityName + " not found." );
		}
		return entityProgress.get( entityName ) * 1.0 / entityTotal.get( entityName );
	}

	/**
	 * Get progresses of each entity at step-level.
	 *
	 * @return an iterable results in string format.
	 */
	public Iterable<String> getProgresses() {
		List<String> results = new LinkedList<>();
		for ( String entity : entityTotal.keySet() ) {
			String msg = String.format(
					Locale.ROOT,
					"%s: %d/%d works processed (%.2f%%).",
					entity,
					entityProgress.get( entity ),
					entityTotal.get( entity ),
					entityProgress.get( entity ) * 100F / entityTotal.get( entity ) );
			results.add( msg );
		}
		return results;
	}

	public long getRowsToIndex(String entityName) {
		return entityTotal.get( entityName );
	}

	public void setRowsToIndex(String entityName, long rowsToIndex) {
		LOGGER.infof( "{key: \"%s\", value: %d}", entityName, rowsToIndex );
		entityProgress.put( entityName, 0L );
		entityTotal.put( entityName, rowsToIndex );
	}
}
