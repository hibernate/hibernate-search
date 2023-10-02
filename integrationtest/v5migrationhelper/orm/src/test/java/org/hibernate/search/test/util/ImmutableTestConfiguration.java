/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.test.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Use this custom TestConfiguration to create a DefaultTestResourceManager when inheritance
 * of our base test classes is not pratical.
 *
 * @author Sanne Grinovero
 * @since 5.3
 */
public class ImmutableTestConfiguration implements TestConfiguration {

	private final Set<String> tenantIds;
	private final ArrayList<Class<?>> annotatedClasses;
	private final Map<String, Object> settings;

	public ImmutableTestConfiguration(Map<String, Object> settings, Class<?>[] annotatedClasses) {
		this( settings, annotatedClasses, Collections.<String>emptySet() );
	}

	public ImmutableTestConfiguration(Map<String, Object> settings, Class<?>[] annotatedClasses, Set<String> tenantIds) {
		this.settings = settings == Collections.EMPTY_MAP ? settings : new HashMap<String, Object>( settings );
		this.tenantIds = tenantIds == Collections.EMPTY_SET
				? tenantIds
				: Collections.unmodifiableSet( new HashSet<String>( tenantIds ) );
		this.annotatedClasses = new ArrayList<>( annotatedClasses.length );
		Collections.addAll( this.annotatedClasses, annotatedClasses );
	}

	@Override
	public void configure(Map<String, Object> settings) {
		settings.putAll( this.settings );
	}

	@Override
	public Set<String> multiTenantIds() {
		return tenantIds;
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return annotatedClasses.toArray( new Class<?>[annotatedClasses.size()] );
	}

}
