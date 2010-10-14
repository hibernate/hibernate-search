package org.hibernate.search.spi.internals;

import java.util.Map;
import java.util.Properties;

import org.apache.lucene.analysis.Analyzer;

import org.hibernate.search.backend.BackendQueueProcessorFactory;
import org.hibernate.search.backend.LuceneIndexingParameters;
import org.hibernate.search.backend.Worker;
import org.hibernate.search.engine.DocumentBuilderContainedEntity;
import org.hibernate.search.engine.DocumentBuilderIndexedEntity;
import org.hibernate.search.engine.FilterDef;
import org.hibernate.search.engine.SearchFactoryImplementor;
import org.hibernate.search.exception.ErrorHandler;
import org.hibernate.search.filter.FilterCachingStrategy;
import org.hibernate.search.reader.ReaderProvider;
import org.hibernate.search.store.DirectoryProvider;

/**
 * Search Factory implementor exposing its sharable state.
 * The state can then be extracted and used to mutate factories.
 *
 * @author Emmanuel Bernard
 */
public interface SearchFactoryImplementorWithShareableState extends SearchFactoryImplementor, SearchFactoryState {
}
