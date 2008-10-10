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
	
	/**
	 * The Workspace this visitor has been created for;
	 * different workspaces could use different Delegates for specific
	 * needs basing on workspace or DirectoryProvider configuration.
	 */
	private final Workspace linkedWorkspace;

	public LuceneWorkVisitor(Workspace workspace) {
		this.addDelegate = new AddWorkDelegate( workspace );
		this.deleteDelegate = new DeleteWorkDelegate( workspace );
		this.optimizeDelegate = new OptimizeWorkDelegate( workspace );
		this.purgeAllDelegate = new PurgeAllWorkDelegate();
		this.linkedWorkspace = workspace;
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
	
	public Workspace getWorkspace(){
		return linkedWorkspace;
	}

}
