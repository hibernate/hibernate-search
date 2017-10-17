/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.dsl.impl;

import java.util.Optional;
import java.util.function.Consumer;

import org.hibernate.search.backend.elasticsearch.ElasticsearchExtension;
import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.backend.elasticsearch.search.ElasticsearchQueryClauseContainerContext;
import org.hibernate.search.backend.elasticsearch.search.clause.impl.ClauseBuilder;
import org.hibernate.search.engine.search.dsl.BooleanJunctionContext;
import org.hibernate.search.engine.search.dsl.MatchClauseContext;
import org.hibernate.search.engine.search.dsl.QueryClauseContainerContext;
import org.hibernate.search.engine.search.dsl.QueryClauseExtension;
import org.hibernate.search.engine.search.dsl.RangeClauseContext;
import org.hibernate.search.util.spi.LoggerFactory;

import com.google.gson.JsonObject;


/**
 * @author Yoann Rodiere
 */
abstract class AbstractClauseContainerContext<N> implements ElasticsearchQueryClauseContainerContext<N> {

	private static final Log log = LoggerFactory.make( Log.class );

	private final QueryTargetContext targetContext;

	public AbstractClauseContainerContext(QueryTargetContext targetContext) {
		this.targetContext = targetContext;
	}

	protected AbstractClauseContainerContext(AbstractClauseContainerContext<?> other) {
		this( other.targetContext );
	}

	@Override
	public BooleanJunctionContext<N> bool() {
		BooleanJunctionContextImpl<N> child = new BooleanJunctionContextImpl<>( targetContext, this::getNext );
		add( child );
		return child;
	}

	@Override
	public MatchClauseContext<N> match() {
		MatchClauseContextImpl<N> child = new MatchClauseContextImpl<>( targetContext, this::getNext );
		add( child );
		return child;
	}

	@Override
	public RangeClauseContext<N> range() {
		RangeClauseContextImpl<N> child = new RangeClauseContextImpl<>( targetContext, this::getNext );
		add( child );
		return child;
	}

	@Override
	public N fromJsonString(String jsonString) {
		add( new UserProvidedJsonClauseBuilder<>( jsonString ) );
		return getNext();
	}

	protected abstract void add(ClauseBuilder<JsonObject> child);

	protected abstract N getNext();

	@Override
	public <T> T withExtension(QueryClauseExtension<N, T> extension) {
		return extension.extendOrFail( this );
	}

	@Override
	public <T> N withExtensionOptional(
			QueryClauseExtension<N, T> extension, Consumer<T> clauseContributor) {
		extension.extendOptional( this ).ifPresent( clauseContributor );
		return getNext();
	}

	@Override
	public <T> N withExtensionOptional(
			QueryClauseExtension<N, T> extension,
			Consumer<T> clauseContributor,
			Consumer<QueryClauseContainerContext<N>> fallbackClauseContributor) {
		Optional<T> optional = extension.extendOptional( this );
		if ( optional.isPresent() ) {
			clauseContributor.accept( optional.get() );
		}
		else {
			fallbackClauseContributor.accept( this );
		}
		return getNext();
	}

	private <T extends QueryClauseContainerContext<N>> boolean supportsExtension(QueryClauseExtension<N, T> extension) {
		return extension == ElasticsearchExtension.get();
	}

}
