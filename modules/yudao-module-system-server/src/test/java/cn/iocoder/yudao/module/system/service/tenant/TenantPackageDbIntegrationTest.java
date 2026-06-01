package cn.iocoder.yudao.module.system.service.tenant;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbIntegrationTest;
import cn.iocoder.yudao.module.system.dal.dataobject.tenant.TenantPackageDO;
import cn.iocoder.yudao.module.system.dal.mysql.tenant.TenantPackageMapper;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.jdbc.Sql;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Sql(scripts = "/sql/mysql/schema.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "/sql/mysql/clean.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class TenantPackageDbIntegrationTest extends BaseDbIntegrationTest {

    @Resource
    private TenantPackageMapper tenantPackageMapper;

    @Test
    void should_insert_and_select_with_real_mysql() {
        TenantPackageDO tenantPackage = new TenantPackageDO()
                .setName("标准套餐")
                .setStatus(0)
                .setMenuIds(Set.of(1L, 2L, 3L))
                .setRemark("mysql integration");

        tenantPackageMapper.insert(tenantPackage);

        assertNotNull(tenantPackage.getId());
        TenantPackageDO result = tenantPackageMapper.selectById(tenantPackage.getId());
        assertNotNull(result);
        assertEquals("标准套餐", result.getName());
        assertEquals(Set.of(1L, 2L, 3L), result.getMenuIds());
    }

}
