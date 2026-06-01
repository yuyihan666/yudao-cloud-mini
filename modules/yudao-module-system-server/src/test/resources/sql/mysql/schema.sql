-- MySQL-compatible schema for system_tenant_package
CREATE TABLE IF NOT EXISTS system_tenant_package (
    id bigint NOT NULL AUTO_INCREMENT,
    name varchar(30) NOT NULL,
    status tinyint NOT NULL,
    remark varchar(256),
    menu_ids varchar(2048) NOT NULL,
    creator varchar(64) DEFAULT '',
    create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater varchar(64) DEFAULT '',
    update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted bit(1) NOT NULL DEFAULT b'0',
    tenant_id bigint NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
) COMMENT '租户套餐表';
