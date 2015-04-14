/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.impl.lucene.works;

import org.hibernate.search.backend.AddLuceneWork;
import org.hibernate.search.backend.DeleteByQueryLuceneWork;
import org.hibernate.search.backend.DeleteLuceneWork;
import org.hibernate.search.backend.FlushLuceneWork;
import org.hibernate.search.backend.IndexWorkVisitor;
import org.hibernate.search.backend.OptimizeLuceneWork;
import org.hibernate.search.backend.PurgeAllLuceneWork;
import org.hibernate.search.backend.UpdateLuceneWork;
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
public class IndexUpdateVisitor implements IndexWorkVisitor<Void, LuceneWorkDelegate> {

	private static final Log log = LoggerFactory.make();

	private final AddWorkDelegate addDelegate;
	private final DeleteWorkDelegate deleteDelegate;
	private final UpdateWorkDelegate updateDelegate;
	private final OptimizeWorkDelegate optimizeDelegate;
	private final PurgeAllWorkDelegate purgeAllDelegate;
	private final FlushWorkDelegate flushDelegate;
	private final DeleteByQueryWorkDelegate deleteByQueryDelegate;

	public IndexUpdateVisitor(Workspace workspace) {
		this.addDelegate = new AddWorkDelegate( workspace );
		if ( workspace.areSingleTermDeletesSafe() ) {
			this.deleteDelegate = new DeleteExtWorkDelegate( workspace );
			this.updateDelegate = new UpdateExtWorkDelegate( workspace, addDelegate );
		}
		else if ( workspace.isDeleteByTermEnforced() ) {
			//TODO Cleanup: with upcoming enhancements of the DocumentBuilder we should be able
			//to extrapolate some constant methods in there, and avoid needing so many different visitors.
			//The difference with the visitors of the previous block is that these are not coupled to a
			//specific type, allowing still dynamic discovery et al
			this.deleteDelegate = new ByTermDeleteWorkDelegate( workspace );
			this.updateDelegate = new ByTermUpdateWorkDelegate( workspace, addDelegate );
		}
		else {
			this.deleteDelegate = new DeleteWorkDelegate( workspace );
			this.updateDelegate = new UpdateWorkDelegate( deleteDelegate, addDelegate );
			log.singleTermDeleteDisabled( workspace.getIndexName() );
		}
		this.purgeAllDelegate = new PurgeAllWorkDelegate( workspace );
		this.optimizeDelegate = new OptimizeWorkDelegate( workspace );
		this.flushDelegate = new FlushWorkDelegate( workspace );
		this.deleteByQueryDelegate = new DeleteByQueryWorkDelegate( workspace );
	}

	@Override
	public LuceneWorkDelegate visitAddWork(AddLuceneWork addLuceneWork, Void p) {
		return addDelegate;
	}

	@Override
	public LuceneWorkDelegate visitDeleteWork(DeleteLuceneWork deleteLuceneWork, Void p) {
		return deleteDelegate;
	}

	@Override
	public LuceneWorkDelegate visitOptimizeWork(OptimizeLuceneWork optimizeLuceneWork, Void p) {
		return optimizeDelegate;
	}

	@Override
	public LuceneWorkDelegate visitPurgeAllWork(PurgeAllLuceneWork purgeAllLuceneWork, Void p) {
		return purgeAllDelegate;
	}

	@Override
	public LuceneWorkDelegate visitUpdateWork(UpdateLuceneWork updateLuceneWork, Void p) {
		return updateDelegate;
	}

	@Override
	public LuceneWorkDelegate visitFlushWork(FlushLuceneWork flushLuceneWork, Void p) {
		return flushDelegate;
	}

	@Override
	public LuceneWorkDelegate visitDeleteByQueryWork(DeleteByQueryLuceneWork deleteByQueryLuceneWork, Void p) {
		return deleteByQueryDelegate;
	}
}
