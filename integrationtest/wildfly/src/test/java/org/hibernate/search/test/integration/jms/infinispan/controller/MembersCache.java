/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.integration.jms.infinispan.controller;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.Stateless;

import org.infinispan.manager.CacheContainer;

/**
 * A simple cache to store some information about a newly registered member.
 *
 * @author Davide D'Alto
 */
@Stateless
public class MembersCache {

	@Resource(lookup = "java:jboss/infinispan/container/membersCache")
	private CacheContainer container;

	private org.infinispan.Cache<String, String> cache;

	@PostConstruct
	public void initCache() {
		this.cache = container.getCache();
	}

	public String get(String key) {
		return this.cache.get( key );
	}

	public void put(String key, String value) {
		this.cache.put( key, value );
	}
}
