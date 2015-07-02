/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.impl.lucene;

import java.util.Collections;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.IndexWriter;

import org.hibernate.search.cfg.spi.IdUniquenessResolver;
import org.hibernate.search.engine.service.spi.ServiceManager;
import org.hibernate.search.engine.spi.DocumentBuilderIndexedEntity;
import org.hibernate.search.exception.impl.ErrorContextBuilder;
import org.hibernate.search.indexes.impl.PropertiesParseHelper;
import org.hibernate.search.indexes.spi.DirectoryBasedIndexManager;
import org.hibernate.search.spi.WorkerBuildContext;
import org.hibernate.search.store.Workspace;
import org.hibernate.search.store.optimization.OptimizerStrategy;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * Lucene workspace for an IndexManager
 *
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 * @author Sanne Grinovero
 */
public abstract class AbstractWorkspaceImpl implements Workspace {

	private static final Log log = LoggerFactory.make();

	private final OptimizerStrategy optimizerStrategy;
	private final DirectoryBasedIndexManager indexManager;
	private final ServiceManager serviceManager;

	protected final IndexWriterHolder writerHolder;
	private final boolean deleteByTermEnforced;
	private boolean indexMetadataIsComplete;

	/**
	 * Keeps a count of modification operations done on the index.
	 */
	private final AtomicLong operations = new AtomicLong( 0L );

	public AbstractWorkspaceImpl(DirectoryBasedIndexManager indexManager, WorkerBuildContext context, Properties cfg) {
		this.indexManager = indexManager;
		this.optimizerStrategy = indexManager.getOptimizerStrategy();
		this.writerHolder = new IndexWriterHolder( context.getErrorHandler(), indexManager );
		this.indexMetadataIsComplete = PropertiesParseHelper.isIndexMetadataComplete( cfg, context );
		this.deleteByTermEnforced = context.isDeleteByTermEnforced();
		this.serviceManager = context.getServiceManager();
	}

	@Override
	public DocumentBuilderIndexedEntity getDocumentBuilder(Class<?> entity) {
		return indexManager.getIndexBinding( entity ).getDocumentBuilder();
	}

	@Override
	public Analyzer getAnalyzer(String name) {
		return indexManager.getAnalyzer( name );
	}

	@Override
	public void optimizerPhase() {
		optimizerStrategy.addOperationWithinTransactionCount( operations.getAndSet( 0L ) );
		optimizerStrategy.optimize( this );
	}

	@Override
	public void performOptimization(IndexWriter writer) {
		optimizerStrategy.performOptimization( writer );
	}

	protected void incrementModificationCounter() {
		operations.addAndGet( 1 );
	}

	@Override
	public Set<Class<?>> getEntitiesInIndexManager() {
		// Do not cache it as an IndexManager receiving a new type should return an updated list
		// and will trigger a LuceneBackendResources rebuild and by side effect
		// a new LuceneWorkVisitor which will need the new list
		return Collections.unmodifiableSet( indexManager.getContainedTypes() );
	}

	@Override
	public void afterTransactionApplied(boolean someFailureHappened, boolean streaming) {
		getCommitPolicy().onChangeSetApplied( someFailureHappened, streaming );
	}

	public void shutDownNow() {
		getCommitPolicy().onClose();
		log.shuttingDownBackend( indexManager.getIndexName() );
		writerHolder.closeIndexWriter();
	}

	@Override
	public IndexWriter getIndexWriter() {
		return writerHolder.getIndexWriter();
	}

	public IndexWriter getIndexWriter(ErrorContextBuilder errorContextBuilder) {
		return writerHolder.getIndexWriter( errorContextBuilder );
	}

	@Override
	public boolean areSingleTermDeletesSafe() {
		return indexMetadataIsComplete && getEntitiesInIndexManager().size() == 1;
	}

	@Override
	public boolean isDeleteByTermEnforced() {
		// if artificially forced: go ahead
		if ( deleteByTermEnforced ) {
			return true;
		}
		// Optimize only if we have all the metadata
		if ( indexMetadataIsComplete ) {
			Set<Class<?>> entitiesInvolved = getEntitiesInIndexManager();
			// a single entity is always safe
			if ( entitiesInvolved.size() == 1 ) {
				return true;
			}
			// ask the source of data for some extra info to be sure it's safe
			IdUniquenessResolver idUniquenessResolver = this.serviceManager.requestService( IdUniquenessResolver.class );
			try {
				// Check all tuple of entities with one another with the following rules:
				//
				// if they use JPA ids, the range of ids is shared for both entity types
				// i.e. two instances of either types are unique if and only if they have the same id value

				// Note that we cannot apply this alternative following rule:
				// "The id field name of one is not used as field name of the second"
				// because we don't know for sure all the fields involved as FiedBridges can do a lot of things
				// behind our back
				// Once we have some new metadata, we can revisit
				for ( Class<?> firstEntity : entitiesInvolved ) {
					boolean firstEntityIsUsingJPAId =
							indexManager.getIndexBinding( firstEntity )
									.getDocumentBuilder()
									.getTypeMetadata()
									.isJpaIdUsedAsDocumentId();
					boolean followingEntities = false;
					for ( Class<?> secondEntity : entitiesInvolved ) {
						// Skip all entities already processed and the same entity
						if ( firstEntity == secondEntity ) {
							followingEntities = true;
						}
						else if ( followingEntities ) {
							//core of the validation rules
							boolean secondEntityIsUsingJPAId =
									indexManager.getIndexBinding( secondEntity )
											.getDocumentBuilder()
											.getTypeMetadata()
											.isJpaIdUsedAsDocumentId();
							// both use JPA id and they share the same id uniqueness set
							// the boolean evaluation in important: only call areIdsUniqueForClasses if absolutely necessary
							boolean uniqueIdEqualityMeansEntityEquality =
									firstEntityIsUsingJPAId && secondEntityIsUsingJPAId &&
									idUniquenessResolver.areIdsUniqueForClasses( firstEntity, secondEntity );
							if ( !uniqueIdEqualityMeansEntityEquality ) {
								return false;
							}
						}
					}
				}
			}
			finally {
				this.serviceManager.releaseService( IdUniquenessResolver.class );
			}
			return true;
		}
		else {
			return false;
		}
	}

	@Override
	public void flush() {
		getCommitPolicy().onFlush();
	}

	@Override
	public String getIndexName() {
		return this.indexManager.getIndexName();
	}

	public IndexWriterDelegate getIndexWriterDelegate(ErrorContextBuilder errorContextBuilder) {
		IndexWriter indexWriter = getIndexWriter( errorContextBuilder );
		//This to respect the existing semantics of returning null on failure of IW opening
		if ( indexWriter != null ) {
			return new IndexWriterDelegate( indexWriter );
		}
		else {
			return null;
		}
	}

	public IndexWriterDelegate getIndexWriterDelegate() {
		IndexWriter indexWriter = getIndexWriter();
		//This to respect the existing semantics of returning null on failure of IW opening
		if ( indexWriter != null ) {
			return new IndexWriterDelegate( indexWriter );
		}
		else {
			return null;
		}
	}

}
