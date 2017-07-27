/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engineperformance.elasticsearch;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import java.util.Random;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.hibernate.search.engineperformance.elasticsearch.datasets.Dataset;
import org.hibernate.search.engineperformance.elasticsearch.model.AbstractBookEntity;
import org.hibernate.search.engineperformance.elasticsearch.setuputilities.SearchIntegratorHelper;
import org.hibernate.search.exception.AssertionFailure;
import org.hibernate.search.spi.IndexedTypeIdentifier;
import org.hibernate.search.util.impl.CollectionHelper;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.infra.ThreadParams;

/**
 * Holds the list of documents currently present in the index
 * and generates changesets accordingly.
 * <p>
 * Note that we cannot reuse IDs during an iteration, for instance
 * adding a document for an ID that just got deleted,
 * because changesets may be executed asynchronously and out of order.
 * That's why we store consumed IDs, and we flip the add and delete sets
 * from one iteration to another.
 */
@State(Scope.Thread)
public class ChangesetGenerator {

	private static final long SEED = 3210140441369L;

	@Param( { "100" } )
	private int changesetsPerFlush;

	/**
	 * Format: (number of add/remove) + "," + (number of updates)
	 * <p>
	 * The two values are squeezed into one parameter so as to
	 * give more control over which combinations will be executed.
	 * For instance you may want to test 5;5 then 1;1,
	 * but 1;5 and 5;1 may not be of interest.
	 */
	@Param( { "3;6" } )
	private String worksPerChangeset;

	private int addDeletesPerChangeset;
	private int updatesPerChangeset;
	private int threadIdIntervalFirst;

	private int addDeletesPerInvocation;
	private int updatesPerInvocation;

	private BitSet toAdd;
	private BitSet consumedToAdd;

	private BitSet toDelete;
	private BitSet consumedToDelete;

	private BitSet toUpdate;
	private BitSet consumedToUpdate;

	private Dataset<? extends AbstractBookEntity> dataset;

	@Setup(Level.Trial)
	public void setup(NonStreamWriteEngineHolder eh, NonStreamDatasetHolder dh, ThreadParams threadParams) {
		int initialIndexSize = eh.getInitialIndexSize();

		String[] worksPerChangesetSplit = worksPerChangeset.split( ";" );
		addDeletesPerChangeset = Integer.parseInt( worksPerChangesetSplit[0] );
		updatesPerChangeset = Integer.parseInt( worksPerChangesetSplit[1] );

		int threadIndex = threadParams.getThreadIndex();

		dataset = dh.getDataset( threadIndex );

		addDeletesPerInvocation = changesetsPerFlush * addDeletesPerChangeset;
		updatesPerInvocation = changesetsPerFlush * updatesPerChangeset;
		int threadIdIntervalSize = addDeletesPerInvocation + addDeletesPerInvocation + updatesPerInvocation;
		threadIdIntervalFirst = initialIndexSize + threadIndex * threadIdIntervalSize;

		toAdd = new BitSet( threadIdIntervalSize );
		consumedToAdd = new BitSet( threadIdIntervalSize );
		toDelete = new BitSet( threadIdIntervalSize );
		consumedToDelete = new BitSet( threadIdIntervalSize );
		toUpdate = new BitSet( threadIdIntervalSize );
		consumedToUpdate = new BitSet( threadIdIntervalSize );

		// Initialize the ID sets
		List<Integer> shuffledIds = createShuffledIndexList( threadIdIntervalSize );
		int deletesShuffledIdsOffset = addDeletesPerInvocation;
		int updatesShuffledIdsOffset = deletesShuffledIdsOffset + addDeletesPerInvocation;
		for ( int i = 0 ; i < addDeletesPerInvocation ; ++i ) {
			toAdd.set( shuffledIds.get( i ) );
		}
		for ( int i = deletesShuffledIdsOffset ; i < updatesShuffledIdsOffset ; ++i ) {
			toDelete.set( shuffledIds.get( i ) );
		}
		for ( int i = updatesShuffledIdsOffset ; i < threadIdIntervalSize ; ++i ) {
			toUpdate.set( shuffledIds.get( i ) );
		}

		SearchIntegratorHelper.preindexEntities(
				eh.getSearchIntegrator(),
				dataset,
				IntStream.concat( toDelete.stream(), toUpdate.stream() )
				);
	}

	private List<Integer> createShuffledIndexList(int size) {
		/*
		 * We just want a sequence of numbers that spreads uniformly over a large interval,
		 * but we don't need cryptographically secure randomness,
		 * and we want the sequence to be the same from one test run to another.
		 * That's why we simply use {@link Random} and that's why we set the seed to
		 * a hard-coded value.
		 * Also, we use one random generator per thread to avoid contention.
		 */
		Random random = new Random( SEED );

		int afterLast = size;
		List<Integer> result = CollectionHelper.newArrayList( size );
		for ( int i = 0 ; i < afterLast ; ++i ) {
			result.add( i );
		}
		Collections.shuffle( result, random );
		return result;
	}

	@TearDown(Level.Invocation)
	public void updateBitSets() {
		if ( addDeletesPerInvocation != consumedToAdd.cardinality() ) {
			throw new AssertionFailure( "Wrong number of adds: expected " + addDeletesPerInvocation + ", got " + consumedToAdd.cardinality() );
		}
		if ( addDeletesPerInvocation != consumedToDelete.cardinality() ) {
			throw new AssertionFailure( "Wrong number of deletes: expected " + addDeletesPerInvocation + ", got " + consumedToDelete.cardinality() );
		}
		if ( updatesPerInvocation != consumedToUpdate.cardinality() ) {
			throw new AssertionFailure( "Wrong number of updates: expected " + updatesPerInvocation + ", got " + consumedToUpdate.cardinality() );
		}

		toAdd.clear();
		toAdd.or( consumedToDelete );
		consumedToDelete.clear();

		toDelete.clear();
		toDelete.or( consumedToAdd );
		consumedToAdd.clear();

		toUpdate.clear();
		toUpdate.or( consumedToUpdate );
		consumedToUpdate.clear();
	}

	public class Changeset {
		private final List<Integer> toAdd = new ArrayList<>();
		private final List<Integer> toDelete = new ArrayList<>();
		private final List<Integer> toUpdate = new ArrayList<>();

		public Stream<AbstractBookEntity> toAdd() {
			return toAdd.stream().map( dataset::create );
		}

		public Stream<Integer> toDelete() {
			return toDelete.stream();
		}

		public Stream<AbstractBookEntity> toUpdate() {
			return toUpdate.stream().map( dataset::create );
		}
	}

	public Stream<Changeset> stream() {
		/*
		 * Since each changeset is only used once,
		 * and this stream is used in a single thread,
		 * we re-use the sample between calls.
		 */
		Changeset sample = new Changeset();
		consume( sample );
		return Stream.iterate( sample, this::consume ).limit( changesetsPerFlush );
	}

	public IndexedTypeIdentifier getTypeId() {
		return dataset.getTypeId();
	}

	private Changeset consume(Changeset target) {
		consumeIds( target.toAdd, addDeletesPerChangeset, toAdd, consumedToAdd );
		consumeIds( target.toDelete, addDeletesPerChangeset, toDelete, consumedToDelete );
		consumeIds( target.toUpdate, updatesPerChangeset, toUpdate, consumedToUpdate);
		return target;
	}

	private void consumeIds(List<Integer> target, int size, BitSet source, BitSet consumed) {
		target.clear();
		source.stream().limit( size ).forEach( target::add );
		ListIterator<Integer> iterator = target.listIterator();
		while ( iterator.hasNext() ) {
			int index = iterator.next();
			iterator.set( threadIdIntervalFirst + index );
			source.clear( index );
			consumed.set( index );
		}
	}

}
