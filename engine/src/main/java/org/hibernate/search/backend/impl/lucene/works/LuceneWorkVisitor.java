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
import org.hibernate.search.backend.OptimizeLuceneWork;
import org.hibernate.search.backend.PurgeAllLuceneWork;
import org.hibernate.search.backend.UpdateLuceneWork;
import org.hibernate.search.backend.impl.WorkVisitor;
import org.hibernate.search.store.Workspace;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * @author Sanne Grinovero
 */
public class LuceneWorkVisitor implements WorkVisitor<LuceneWorkDelegate> {

	private static final Log log = LoggerFactory.make();

	private final AddWorkDelegate addDelegate;
	private final DeleteWorkDelegate deleteDelegate;
	private final UpdateWorkDelegate updateDelegate;
	private final OptimizeWorkDelegate optimizeDelegate;
	private final PurgeAllWorkDelegate purgeAllDelegate;
	private final FlushWorkDelegate flushDelegate;

	public LuceneWorkVisitor(Workspace workspace) {
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
	}

	@Override
	public LuceneWorkDelegate getDelegate(AddLuceneWork addLuceneWork) {
		return addDelegate;
	}

	@Override
	public LuceneWorkDelegate getDelegate(DeleteLuceneWork deleteLuceneWork) {
		return deleteDelegate;
	}

	@Override
	public LuceneWorkDelegate getDelegate(OptimizeLuceneWork optimizeLuceneWork) {
		return optimizeDelegate;
	}

	@Override
	public LuceneWorkDelegate getDelegate(PurgeAllLuceneWork purgeAllLuceneWork) {
		return purgeAllDelegate;
	}

	@Override
	public LuceneWorkDelegate getDelegate(UpdateLuceneWork updateLuceneWork) {
		return updateDelegate;
	}

	@Override
	public LuceneWorkDelegate getDelegate(FlushLuceneWork flushLuceneWork) {
		return flushDelegate;
	}

}
