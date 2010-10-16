package org.hibernate.search.spi.internals;

import org.hibernate.search.engine.SearchFactoryImplementor;

/**
 * Search Factory implementor exposing its sharable state.
 * The state can then be extracted and used to mutate factories.
 *
 * @author Emmanuel Bernard
 */
public interface SearchFactoryImplementorWithShareableState extends SearchFactoryImplementor, SearchFactoryState {
}
