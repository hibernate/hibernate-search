/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.indexes.spi;

import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.search.similarities.Similarity;

import org.hibernate.search.backend.IndexingMonitor;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.indexes.serialization.spi.LuceneWorkSerializer;
import org.hibernate.search.spi.WorkerBuildContext;

/**
 * An IndexManager abstracts the specific configuration and implementations being used on a single index.
 * For each index a different implementation can be used, or different configurations.
 *
 * While in previous versions of Hibernate Search the backend could be sync or async, this fact is now
 * considered a detail of the concrete IndexManager implementations. This makes it possible to configure each index
 * manager (and hence index) differently. A concrete implementation can also decide to only support a specific mode
 * of operation. It can ignore some configuration properties or expect additional properties.
 *
 * @author Sanne Grinovero (C) 2011 Red Hat Inc.
 */
public interface IndexManager {

	/**
	 * Useful for labeling and logging resources from this instance.
	 *
	 * @return the name of the index maintained by this manager.
	 */
	String getIndexName();

	/**
	 * Provide access to {@code IndexReader}s.
	 *
	 * @return a {@code ReaderProvider} instance for the index managed by this instance
	 */
	ReaderProvider getReaderProvider();

	/**
	 * Used to apply update operations to the index.
	 * Operations can be applied in sync or async, depending on the IndexManager implementation and configuration.
	 *
	 * @param monitor no be notified of indexing events
	 * @param queue the list of write operations to apply.
	 */
	void performOperations(List<LuceneWork> queue, IndexingMonitor monitor);

	/**
	 * Perform a single non-transactional operation, best to stream large amounts of operations.
	 * Operations might be applied out-of-order! To mark two series of operations which need to be applied
	 * in order, use a transactional operation between them: a transactional operation will always flush
	 * all streaming operations first, and be applied before subsequent streaming operations.
	 *
	 * @param singleOperation the operation to perform
	 * @param monitor no be notified of indexing events
	 * @param forceAsync if true, the invocation will not block to wait for it being applied.
	 * When false this will depend on the backend configuration.
	 */
	void performStreamOperation(LuceneWork singleOperation, IndexingMonitor monitor, boolean forceAsync);

	/**
	 * Initialize this {@code IndexManager} before its use.
	 *
	 * @param indexName the unique name of the index (manager). Can be used to retrieve a {@code IndexManager} instance
	 * via the search factory and {@link org.hibernate.search.indexes.impl.IndexManagerHolder}.
	 * @param properties the configuration properties
	 * @param similarity defines the component of Lucene scoring
	 * @param context context information needed to initialize this index manager
	 */
	void initialize(String indexName, Properties properties, Similarity similarity, WorkerBuildContext context);

	/**
	 * Called when a {@code SearchFactory} is stopped. This method typically releases resources.
	 */
	void destroy();

	/**
	 * @return the set of classes being indexed in this manager
	 */
	Set<Class<?>> getContainedTypes();

	/**
	 * @return the {@code Similarity} applied to this index. Note, only a single {@code Similarity} can be applied to
	 *         a given index.
	 */
	Similarity getSimilarity();

	/**
	 * @param name the name of the analyzer to retrieve.
	 *
	 * @return Returns the {@code Analyzer} with the given name (see also {@link org.hibernate.search.annotations.AnalyzerDef})
	 * @throws org.hibernate.search.exception.SearchException in case the analyzer name is unknown.
	 */
	Analyzer getAnalyzer(String name);

	/**
	 * Connects this {@code IndexManager} to a new {@code ExtendedSearchintegrator}.
	 *
	 * @param boundSearchIntegrator the existing search factory to which to associate this index manager with
	 */
	void setSearchFactory(ExtendedSearchIntegrator boundSearchIntegrator);

	/**
	 * @param entity Adds the specified entity type to this index manager, making it responsible for manging this type.
	 */
	void addContainedEntity(Class<?> entity);

	/**
	 * To optimize the underlying index. Some implementations might ignore the request, if it doesn't apply to them.
	 */
	void optimize();

	/**
	 * @return the Serializer implementation used for this IndexManager
	 */
	LuceneWorkSerializer getSerializer();

}
