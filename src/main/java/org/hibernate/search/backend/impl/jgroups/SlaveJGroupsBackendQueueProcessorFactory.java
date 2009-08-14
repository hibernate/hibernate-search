// $Id$
package org.hibernate.search.backend.impl.jgroups;

import java.util.List;

import org.hibernate.search.backend.LuceneWork;

/**
 * @author Lukasz Moren
 */
public class SlaveJGroupsBackendQueueProcessorFactory extends JGroupsBackendQueueProcessorFactory {

	public Runnable getProcessor(List<LuceneWork> queue) {
		return new JGroupsBackendQueueProcessor( queue, this );
	}
}
