/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.massindexing;

import org.hibernate.search.mapper.pojo.massindexing.impl.PojoMassIndexingLoggingMonitor;
import org.hibernate.search.util.common.annotation.Incubating;

/**
 * A simple builder class that allows configuring the built-in logging mass indexer monitor.
 * <p>
 * To customize the monitor pass it to the mass indexer as:
 * <pre>{@code
 * massIndexer.monitor( DefaultMassIndexingMonitor.builder()
 *     .countOnStart( true )
 *     .countOnBeforeType( false )
 *     .build()
 * );
 * }
 * </pre>
 */
@Incubating
public final class DefaultMassIndexingMonitor {

	public static DefaultMassIndexingMonitor builder() {
		return new DefaultMassIndexingMonitor();
	}

	private boolean countOnStart = false;
	private boolean countOnBeforeType = true;

	private DefaultMassIndexingMonitor() {
	}

	public MassIndexingMonitor build() {
		return new PojoMassIndexingLoggingMonitor( countOnStart, countOnBeforeType );
	}

	/**
	 * Allows specifying whether the mass indexer should try obtaining the total number of <b>all</b> entities to index before the indexing even starts.
	 * <p>
	 * This means that the default monitor will make an attempt to get the counts in the main thread and only then start the indexing.
	 * Then, at index time, the mass indexer may attempt to recalculate the total for a currently indexed type (see {@link #countOnBeforeType(boolean)}.
	 * <p>
	 * Defaults to {@code false}.
	 * @param countOnStart If {@code true}, the mass indexer will try determining the total number of all entities to index
	 * before the actual indexing starts.
	 *
	 * @return {@code this} for method chaining
	 */
	public DefaultMassIndexingMonitor countOnStart(boolean countOnStart) {
		this.countOnStart = countOnStart;
		return this;
	}

	/**
	 * Allows specifying whether to try determining the total number of entities of the particular type to index
	 * and logging that information.
	 * <p>
	 * This count attempt happens right before fetching the IDs to index, and should provide the
	 * number of entities to fetch.
	 * <p>
	 * It may be helpful to skip the counting of entities and start the ID fetching right away to save some time.
	 * <p>
	 * Defaults to {@code true}.
	 * @param countOnBeforeType If {@code true}, the mass indexer will try determining the total number of entities,
	 * otherwise the mass indexer will not try obtaining the total count.
	 *
	 * @return {@code this} for method chaining
	 */
	public DefaultMassIndexingMonitor countOnBeforeType(boolean countOnBeforeType) {
		this.countOnBeforeType = countOnBeforeType;
		return this;
	}
}
