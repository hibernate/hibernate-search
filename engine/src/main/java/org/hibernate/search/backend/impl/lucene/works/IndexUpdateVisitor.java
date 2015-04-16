/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.impl.lucene.works;

import org.hibernate.search.backend.AddLuceneWork;
import org.hibernate.search.backend.DeleteLuceneWork;
import org.hibernate.search.backend.FlushLuceneWork;
import org.hibernate.search.backend.IndexWorkVisitor;
import org.hibernate.search.backend.OptimizeLuceneWork;
import org.hibernate.search.backend.PurgeAllLuceneWork;
import org.hibernate.search.backend.UpdateLuceneWork;
import org.hibernate.search.backend.spi.DeleteByQueryLuceneWork;
import org.hibernate.search.store.Workspace;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * A {@link IndexWorkVisitor} which applies updates to the underlying Lucene index.
 * <p>
 * Implementation note: This {@link IndexWorkVisitor} implementation intentionally does not perform the actual logic
 * within the individual visit methods themselves but rather returns a delegate class for that purpose. This is to avoid
 * the need for the allocation of a parameter object with the required input data, instead a method with the required
 * parameters is exposed on said delegate.
 *
 * @author Sanne Grinovero
 * @author Gunnar Morling
 */
public class IndexUpdateVisitor implements IndexWorkVisitor<Void, LuceneWorkExecutor> {

	private static final Log log = LoggerFactory.make();

	private final AddWorkExecutor addExecutor;
	private final DeleteWorkExecutor deleteExecutor;
	private final UpdateWorkExecutor updateExecutor;
	private final OptimizeWorkExecutor optimizeExecutor;
	private final PurgeAllWorkExecutor purgeAllExecutor;
	private final FlushWorkExecutor flushExecutor;
	private final DeleteByQueryWorkExecutor deleteByQueryExecutor;

	public IndexUpdateVisitor(Workspace workspace) {
		this.addExecutor = new AddWorkExecutor( workspace );
		if ( workspace.areSingleTermDeletesSafe() ) {
			this.deleteExecutor = new DeleteExtWorkExecutor( workspace );
			this.updateExecutor = new UpdateExtWorkExecutor( workspace, addExecutor );
		}
		else if ( workspace.isDeleteByTermEnforced() ) {
			//TODO Cleanup: with upcoming enhancements of the DocumentBuilder we should be able
			//to extrapolate some constant methods in there, and avoid needing so many different visitors.
			//The difference with the visitors of the previous block is that these are not coupled to a
			//specific type, allowing still dynamic discovery et al
			this.deleteExecutor = new ByTermDeleteWorkExecutor( workspace );
			this.updateExecutor = new ByTermUpdateWorkExecutor( workspace, addExecutor );
		}
		else {
			this.deleteExecutor = new DeleteWorkExecutor( workspace );
			this.updateExecutor = new UpdateWorkExecutor( deleteExecutor, addExecutor );
			log.singleTermDeleteDisabled( workspace.getIndexName() );
		}
		this.purgeAllExecutor = new PurgeAllWorkExecutor( workspace );
		this.optimizeExecutor = new OptimizeWorkExecutor( workspace );
		this.flushExecutor = new FlushWorkExecutor( workspace );
		this.deleteByQueryExecutor = new DeleteByQueryWorkExecutor( workspace );
	}

	@Override
	public LuceneWorkExecutor visitAddWork(AddLuceneWork addLuceneWork, Void p) {
		return addExecutor;
	}

	@Override
	public LuceneWorkExecutor visitDeleteWork(DeleteLuceneWork deleteLuceneWork, Void p) {
		return deleteExecutor;
	}

	@Override
	public LuceneWorkExecutor visitOptimizeWork(OptimizeLuceneWork optimizeLuceneWork, Void p) {
		return optimizeExecutor;
	}

	@Override
	public LuceneWorkExecutor visitPurgeAllWork(PurgeAllLuceneWork purgeAllLuceneWork, Void p) {
		return purgeAllExecutor;
	}

	@Override
	public LuceneWorkExecutor visitUpdateWork(UpdateLuceneWork updateLuceneWork, Void p) {
		return updateExecutor;
	}

	@Override
	public LuceneWorkExecutor visitFlushWork(FlushLuceneWork flushLuceneWork, Void p) {
		return flushExecutor;
	}

	@Override
	public LuceneWorkExecutor visitDeleteByQueryWork(DeleteByQueryLuceneWork deleteByQueryLuceneWork, Void p) {
		return deleteByQueryExecutor;
	}
}
