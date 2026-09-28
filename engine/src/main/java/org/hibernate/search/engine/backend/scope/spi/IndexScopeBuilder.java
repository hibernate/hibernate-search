package org.hibernate.search.engine.backend.scope.spi;

public interface IndexScopeBuilder<SR> {

	IndexScope<SR> build();

}
