package cn.iocoder.yudao.framework.test.core.ut;

import cn.hutool.extra.spring.SpringUtil;
import cn.iocoder.yudao.framework.datasource.config.YudaoDataSourceAutoConfiguration;
import cn.iocoder.yudao.framework.mybatis.config.YudaoMybatisAutoConfiguration;
import cn.iocoder.yudao.framework.test.config.SqlInitializationTestConfiguration;
import com.alibaba.druid.spring.boot3.autoconfigure.DruidDataSourceAutoConfigure;
import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import com.github.yulichang.autoconfigure.MybatisPlusJoinAutoConfiguration;
import org.junit.jupiter.api.Tag;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * MySQL-backed DB integration test base.
 *
 * <p>Use this for final evidence of Mapper, SQL dialect, transaction,
 * pagination, logical delete, and schema behavior. Keep {@link BaseDbUnitTest}
 * for legacy fast H2 tests.
 *
 * <p>Subclasses must use {@code @Sql} to create required tables before tests
 * and clean up after tests, since the default H2 {@code create_tables.sql}
 * is not MySQL-compatible.
 */
@Tag("db")
@Tag("integration")
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, classes = BaseDbIntegrationTest.Application.class)
@ActiveProfiles("unit-test")
@TestPropertySource(properties = {
        "spring.sql.init.schema-locations=", // disable H2-specific create_tables.sql
        "mybatis-plus.global-config.db-config.id-type=auto" // MySQL AUTO_INCREMENT
})
public abstract class BaseDbIntegrationTest {

    @Container
    @ServiceConnection
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>(DockerImageName.parse("mysql:8.0"))
            .withDatabaseName("ruoyi-vue-pro")
            .withUsername("test")
            .withPassword("test");

    @Import({
            YudaoDataSourceAutoConfiguration.class,
            DataSourceAutoConfiguration.class,
            DataSourceTransactionManagerAutoConfiguration.class,
            DruidDataSourceAutoConfigure.class,
            SqlInitializationTestConfiguration.class,
            YudaoMybatisAutoConfiguration.class,
            MybatisPlusAutoConfiguration.class,
            MybatisPlusJoinAutoConfiguration.class,
            SpringUtil.class
    })
    public static class Application {
    }

}
