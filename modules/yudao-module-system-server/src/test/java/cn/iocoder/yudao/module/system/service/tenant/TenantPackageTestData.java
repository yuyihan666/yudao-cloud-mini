package cn.iocoder.yudao.module.system.service.tenant;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.test.core.builder.TestDataBuilder;
import cn.iocoder.yudao.module.system.controller.admin.tenant.vo.packages.TenantPackageSaveReqVO;
import cn.iocoder.yudao.module.system.dal.dataobject.tenant.TenantPackageDO;

import java.util.Set;

public final class TenantPackageTestData {

    private TenantPackageTestData() {
    }

    public static TenantPackageDOBuilder tenantPackage() {
        return new TenantPackageDOBuilder();
    }

    public static TenantPackageSaveReqVOBuilder createTenantPackageReq() {
        return new TenantPackageSaveReqVOBuilder().withoutId();
    }

    public static class TenantPackageDOBuilder extends TestDataBuilder<TenantPackageDO, TenantPackageDOBuilder> {

        TenantPackageDOBuilder() {
            super(new TenantPackageDO()
                    .setName("标准套餐")
                    .setStatus(CommonStatusEnum.ENABLE.getStatus())
                    .setMenuIds(Set.of(1L, 2L, 3L))
                    .setRemark("标准套餐备注"));
        }

        public TenantPackageDOBuilder disabled() {
            data.setStatus(CommonStatusEnum.DISABLE.getStatus());
            return self();
        }

        public TenantPackageDOBuilder name(String name) {
            data.setName(name);
            return self();
        }

        public TenantPackageDOBuilder menuIds(Set<Long> menuIds) {
            data.setMenuIds(menuIds);
            return self();
        }

    }

    public static class TenantPackageSaveReqVOBuilder
            extends TestDataBuilder<TenantPackageSaveReqVO, TenantPackageSaveReqVOBuilder> {

        TenantPackageSaveReqVOBuilder() {
            super(new TenantPackageSaveReqVO()
                    .setName("标准套餐")
                    .setStatus(CommonStatusEnum.ENABLE.getStatus())
                    .setMenuIds(Set.of(1L, 2L, 3L))
                    .setRemark("标准套餐备注"));
        }

        public TenantPackageSaveReqVOBuilder withoutId() {
            data.setId(null);
            return self();
        }

        public TenantPackageSaveReqVOBuilder id(Long id) {
            data.setId(id);
            return self();
        }

        public TenantPackageSaveReqVOBuilder name(String name) {
            data.setName(name);
            return self();
        }

    }

}
