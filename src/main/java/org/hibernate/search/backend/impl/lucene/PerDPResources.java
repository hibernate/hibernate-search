package org.hibernate.search.backend.impl.lucene;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.hibernate.search.backend.Workspace;
import org.hibernate.search.backend.impl.lucene.works.LuceneWorkVisitor;
import org.hibernate.search.engine.SearchFactoryImplementor;
import org.hibernate.search.store.DirectoryProvider;

/**
 * Collects all resources needed to apply changes to one index,
 * and are reused across several WorkQueues.
 *
 * @author Sanne Grinovero
 */
class PerDPResources {
	
	private final ExecutorService executor;
	private final LuceneWorkVisitor visitor;
	private final Workspace workspace;
	
	PerDPResources(SearchFactoryImplementor searchFactoryImp, DirectoryProvider dp) {
		workspace = new Workspace( searchFactoryImp, dp );
		visitor = new LuceneWorkVisitor( workspace );
		executor = Executors.newFixedThreadPool( 1 );
	}

	public ExecutorService getExecutor() {
		return executor;
	}

	public LuceneWorkVisitor getVisitor() {
		return visitor;
	}

	public Workspace getWorkspace() {
		return workspace;
	}
	
}
