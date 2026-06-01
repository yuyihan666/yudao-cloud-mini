package cn.iocoder.yudao.module.system.framework.test;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbIntegrationTest;
import org.springframework.test.context.jdbc.Sql;

/**
 * System module base class for real MySQL integration tests.
 *
 * <p>The shared framework base starts MySQL. This module base owns system-module schema
 * setup and cleanup so individual DB tests do not duplicate SQL annotations.</p>
 */
@Sql(scripts = "/sql/mysql/schema.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "/sql/mysql/clean.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
public abstract class BaseSystemDbIntegrationTest extends BaseDbIntegrationTest {
}
