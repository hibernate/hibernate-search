//$Id$
package org.hibernate.search.backend.impl.lucene;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.List;

import org.hibernate.search.backend.BackendQueueProcessorFactory;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.backend.Workspace;
import org.hibernate.search.backend.impl.lucene.works.LuceneWorkVisitor;
import org.hibernate.search.engine.SearchFactoryImplementor;
import org.hibernate.search.store.DirectoryProvider;

/**
 * This will actually contain the Workspace and LuceneWork visitor implementation,
 * reused per-DirectoryProvider.
 * Both Workspace(s) and LuceneWorkVisitor(s) lifecycle are linked to the backend
 * lifecycle (reused and shared by all transactions).
 * The LuceneWorkVisitor(s) are stateless, the Workspace(s) are threadsafe.
 * 
 * @author Emmanuel Bernard
 * @author Sanne Grinovero
 */
public class LuceneBackendQueueProcessorFactory implements BackendQueueProcessorFactory {

	private SearchFactoryImplementor searchFactoryImp;
	
	/**
	 * Contains the Workspace and LuceneWork visitor implementation,
	 * reused per-DirectoryProvider.
	 * Both Workspace(s) and LuceneWorkVisitor(s) lifecycle are linked to the backend
	 * lifecycle (reused and shared by all transactions);
	 * the LuceneWorkVisitor(s) are stateless, the Workspace(s) are threadsafe.
	 */
	private final Map<DirectoryProvider,LuceneWorkVisitor> visitorsMap = new HashMap<DirectoryProvider,LuceneWorkVisitor>();

	public void initialize(Properties props, SearchFactoryImplementor searchFactoryImplementor) {
		this.searchFactoryImp = searchFactoryImplementor;
		for (DirectoryProvider dp : searchFactoryImplementor.getDirectoryProviders() ) {
			Workspace w = new Workspace( searchFactoryImplementor, dp );
			LuceneWorkVisitor visitor = new LuceneWorkVisitor( w );
			visitorsMap.put( dp, visitor );
		}
	}

	public Runnable getProcessor(List<LuceneWork> queue) {
		return new LuceneBackendQueueProcessor( queue, searchFactoryImp, visitorsMap );
	}

	public void close() {
		// no need to release anything
	}
	
}
