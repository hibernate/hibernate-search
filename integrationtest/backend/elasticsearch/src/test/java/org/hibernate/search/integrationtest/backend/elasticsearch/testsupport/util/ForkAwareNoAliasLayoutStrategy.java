package org.hibernate.search.integrationtest.backend.elasticsearch.testsupport.util;

import org.hibernate.search.backend.elasticsearch.index.layout.IndexLayoutStrategy;
import org.hibernate.search.util.impl.integrationtest.common.TestForkPrefix;

public class ForkAwareNoAliasLayoutStrategy implements IndexLayoutStrategy {
    @Override
    public String createInitialElasticsearchIndexName(String hibernateSearchIndexName) {
        return TestForkPrefix.PREFIX + hibernateSearchIndexName;
    }

    @Override
    public String createWriteAlias(String hibernateSearchIndexName) {
        return null;
    }

    @Override
    public String createReadAlias(String hibernateSearchIndexName) {
        return null;
    }
}
