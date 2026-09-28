package org.hibernate.search.util.common.reflect.impl;

import java.io.Serializable;

@SuppressWarnings("unused")
class CustomBoundedGenericType<T extends Number & Cloneable & Serializable> implements CustomBoundedGenericInterface<T> {
}
