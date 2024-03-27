/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.backend.document.model.spi;

import org.hibernate.search.engine.backend.common.spi.FieldPaths;
import org.hibernate.search.engine.backend.types.spi.AbstractIndexNodeType;
import org.hibernate.search.engine.common.tree.spi.TreeNodeInclusion;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.engine.search.common.spi.SearchIndexScope;
import org.hibernate.search.util.common.reporting.EventContext;

public abstract class AbstractIndexField<
		S extends AbstractIndexField<S, SC, ?, C>,
		SC extends SearchIndexScope<?>,
		NT extends AbstractIndexNodeType<SC, ? super S>,
		C extends IndexCompositeNode<SC, ?, ?>>
		extends AbstractIndexNode<S, SC, NT>
		implements IndexField<SC, C> {
	protected final C parent;
	protected final String absolutePath;
	protected final String[] absolutePathComponents;
	protected final String relativeName;
	protected final TreeNodeInclusion inclusion;
	protected final boolean multiValued;
	private final String closestMultiValuedParentAbsolutePath;

	public AbstractIndexField(C parent, String relativeFieldName, NT type, TreeNodeInclusion inclusion,
			boolean multiValued) {
		super( type );
		this.parent = parent;
		this.absolutePath = parent.absolutePath( relativeFieldName );
		this.absolutePathComponents = FieldPaths.split( absolutePath );
		this.relativeName = relativeFieldName;
		this.inclusion = inclusion;
		this.multiValued = multiValued;
		this.closestMultiValuedParentAbsolutePath = parent.multiValued()
				? parent.absolutePath()
				: parent.closestMultiValuedParentAbsolutePath();
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[absolutePath=" + absolutePath + ", type=" + type + "]";
	}

	@Override
	public final EventContext relativeEventContext() {
		return EventContexts.fromIndexFieldAbsolutePath( absolutePath );
	}

	@Override
	public final C parent() {
		return parent;
	}

	@Override
	public final String absolutePath() {
		return absolutePath;
	}

	@Override
	public final String[] absolutePathComponents() {
		return absolutePathComponents;
	}

	@Override
	public final String relativeName() {
		return relativeName;
	}

	@Override
	public final TreeNodeInclusion inclusion() {
		return inclusion;
	}

	@Override
	public final boolean multiValued() {
		return multiValued;
	}

	@Override
	public boolean multiValuedInRoot() {
		return multiValued || closestMultiValuedParentAbsolutePath != null;
	}

	@Override
	public String closestMultiValuedParentAbsolutePath() {
		return closestMultiValuedParentAbsolutePath;
	}

}
