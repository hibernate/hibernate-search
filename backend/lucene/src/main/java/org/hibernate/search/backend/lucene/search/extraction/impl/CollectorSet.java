/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.extraction.impl;

import java.util.Map;

import org.hibernate.search.backend.lucene.lowlevel.collector.impl.CollectorKey;

import org.apache.lucene.search.Collector;

public class CollectorSet {

	private final Collector composed;
	private final Map<CollectorKey<?>, Collector> components;

	public CollectorSet(Collector composed, Map<CollectorKey<?>, Collector> components) {
		this.composed = composed;
		this.components = components;
	}

	public Collector getComposed() {
		return composed;
	}

	@SuppressWarnings("unchecked")
	public <C extends Collector> C get(CollectorKey<C> key) {
		return (C) components.get( key );
	}

}
