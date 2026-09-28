package org.hibernate.search.backend.lucene.lowlevel.collector.impl;

import org.apache.lucene.search.Collector;
import org.apache.lucene.search.CollectorManager;

public interface BaseTermsCollector {

	CollectorKey<?, ?>[] keys();

	CollectorManager<Collector, ?>[] managers();

}
