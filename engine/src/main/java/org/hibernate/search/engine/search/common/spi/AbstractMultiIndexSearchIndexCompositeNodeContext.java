/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.common.spi;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;

import org.hibernate.search.engine.backend.common.spi.FieldPaths;
import org.hibernate.search.engine.logging.impl.Log;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public abstract class AbstractMultiIndexSearchIndexCompositeNodeContext<
		S extends SearchIndexCompositeNodeContext<SC>,
		SC extends SearchIndexScope<?>,
		NT extends SearchIndexCompositeNodeTypeContext<SC, S>,
		F extends SearchIndexNodeContext<SC>>
		extends AbstractMultiIndexSearchIndexNodeContext<S, SC, NT>
		implements SearchIndexCompositeNodeContext<SC>, SearchIndexCompositeNodeTypeContext<SC, S> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private Map<String, F> staticChildrenByName;

	public AbstractMultiIndexSearchIndexCompositeNodeContext(SC scope, String absolutePath,
			List<? extends S> nodeForEachIndex) {
		super( scope, absolutePath, nodeForEachIndex );
	}

	@Override
	public final NT type() {
		return selfAsNodeType();
	}

	@Override
	public final boolean isComposite() {
		return true;
	}

	@Override
	public boolean isObjectField() {
		return absolutePath != null;
	}

	@Override
	public final boolean isValueField() {
		return false;
	}

	@Override
	public final S toComposite() {
		return self();
	}

	@Override
	public S toObjectField() {
		if ( isObjectField() ) {
			return self();
		}
		else {
			return SearchIndexSchemaElementContextHelper.throwingToObjectField( this );
		}
	}

	@Override
	public SearchIndexValueFieldContext<SC> toValueField() {
		return SearchIndexSchemaElementContextHelper.throwingToValueField( this );
	}

	@Override
	public final String absolutePath(String relativeFieldName) {
		return FieldPaths.compose( absolutePath(), relativeFieldName );
	}

	@Override
	public final boolean nested() {
		return fromTypeIfCompatible( SearchIndexCompositeNodeTypeContext::nested, Object::equals,
				"nested" );
	}

	@Override
	public final Map<String, F> staticChildrenByName() {
		if ( staticChildrenByName != null ) {
			return staticChildrenByName;
		}

		Map<String, F> result = new TreeMap<>();
		Function<String, F> createChildFieldContext = this::childInScope;
		for ( S nodeForIndex : nodeForEachIndex ) {
			for ( String childRelativeName : nodeForIndex.staticChildrenByName().keySet() ) {
				try {
					result.computeIfAbsent( childRelativeName, createChildFieldContext );
				}
				catch (SearchException e) {
					throw log.inconsistentConfigurationInContextForSearch( relativeEventContext(), e.getMessage(),
							indexesEventContext(), e );
				}
			}
		}
		// Only set this field to a non-null value at the end,
		// so that if there was a conflict and we threw an exception,
		// the next call to this method will go through the loop again and throw an exception again.
		staticChildrenByName = result;
		return staticChildrenByName;
	}

	protected abstract F childInScope(String childRelativeName);

	@Override
	final SearchIndexSchemaElementContextHelper helper() {
		return SearchIndexSchemaElementContextHelper.COMPOSITE;
	}
}
