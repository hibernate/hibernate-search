package org.hibernate.search.backend.impl.lucene.works;

import org.hibernate.search.backend.AddLuceneWork;
import org.hibernate.search.backend.DeleteLuceneWork;
import org.hibernate.search.backend.OptimizeLuceneWork;
import org.hibernate.search.backend.PurgeAllLuceneWork;
import org.hibernate.search.backend.WorkVisitor;
import org.hibernate.search.backend.Workspace;

/**
 * @author Sanne Grinovero
 */
public class LuceneWorkVisitor implements WorkVisitor<LuceneWorkDelegate> {
	
	private final AddWorkDelegate addDelegate;
	private final DeleteWorkDelegate deleteDelegate;
	private final OptimizeWorkDelegate optimizeDelegate;
	private final PurgeAllWorkDelegate purgeAllDelegate;
	
	public LuceneWorkVisitor(Workspace workspace) {
		if ( workspace.getEntitiesInDirectory().size() == 1 ) {
			this.deleteDelegate = new DeleteExtWorkDelegate( workspace );
		}
		else {
			this.deleteDelegate = new DeleteWorkDelegate( workspace );
		}
		this.purgeAllDelegate = new PurgeAllWorkDelegate();
		this.addDelegate = new AddWorkDelegate( workspace );
		this.optimizeDelegate = new OptimizeWorkDelegate( workspace );
	}

	public LuceneWorkDelegate getDelegate(AddLuceneWork addLuceneWork) {
		return addDelegate;
	}

	public LuceneWorkDelegate getDelegate(DeleteLuceneWork deleteLuceneWork) {
		return deleteDelegate;
	}

	public LuceneWorkDelegate getDelegate(OptimizeLuceneWork optimizeLuceneWork) {
		return optimizeDelegate;
	}

	public LuceneWorkDelegate getDelegate(PurgeAllLuceneWork purgeAllLuceneWork) {
		return purgeAllDelegate;
	}
	
}
