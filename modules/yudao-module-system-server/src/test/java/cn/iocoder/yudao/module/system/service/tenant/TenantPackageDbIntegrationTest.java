package cn.iocoder.yudao.module.system.service.tenant;

import cn.iocoder.yudao.module.system.dal.dataobject.tenant.TenantPackageDO;
import cn.iocoder.yudao.module.system.dal.mysql.tenant.TenantPackageMapper;
import cn.iocoder.yudao.module.system.framework.test.BaseSystemDbIntegrationTest;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class TenantPackageDbIntegrationTest extends BaseSystemDbIntegrationTest {

    @Resource
    private TenantPackageMapper tenantPackageMapper;
    @Resource
    private DataSource dataSource;

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

    @Test
    void should_filter_logically_deleted_tenant_package() {
        TenantPackageDO tenantPackage = TenantPackageTestData.tenantPackage()
                .name("逻辑删除套餐")
                .build();
        tenantPackageMapper.insert(tenantPackage);

        tenantPackageMapper.deleteById(tenantPackage.getId());

        assertNull(tenantPackageMapper.selectById(tenantPackage.getId()));
        Integer deletedRowCount = new JdbcTemplate(dataSource).queryForObject(
                "SELECT COUNT(*) FROM system_tenant_package WHERE id = ? AND deleted = b'1'",
                Integer.class,
                tenantPackage.getId());
        assertEquals(1, deletedRowCount);
    }

}
