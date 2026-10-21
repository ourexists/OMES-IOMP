-- 在线客服同步状态。消息正文在 WuKongIM，不在这两张表。
-- 客服工作台菜单不自动授权，在角色管理里勾给当班账号。

CREATE TABLE IF NOT EXISTS "public"."t_ucenter_im_account" (
  "id" varchar(20) COLLATE "pg_catalog"."default" NOT NULL,
  "revision" int4,
  "created_by" varchar(64) COLLATE "pg_catalog"."default",
  "created_id" varchar(20) COLLATE "pg_catalog"."default",
  "created_time" timestamp(6),
  "updated_by" varchar(64) COLLATE "pg_catalog"."default",
  "updated_id" varchar(20) COLLATE "pg_catalog"."default",
  "updated_time" timestamp(6),
  "acc_id" varchar(20) COLLATE "pg_catalog"."default" NOT NULL,
  "im_user_id" varchar(64) COLLATE "pg_catalog"."default" NOT NULL,
  "sync_status" int4 NOT NULL DEFAULT 0,
  "last_error" varchar(500) COLLATE "pg_catalog"."default",
  "last_sync_time" timestamp(6),
  CONSTRAINT "t_ucenter_im_account_pkey" PRIMARY KEY ("id"),
  CONSTRAINT "uk_t_ucenter_im_account_acc" UNIQUE ("acc_id"),
  CONSTRAINT "uk_t_ucenter_im_account_im" UNIQUE ("im_user_id")
);

CREATE TABLE IF NOT EXISTS "public"."t_ucenter_im_service" (
  "id" varchar(20) COLLATE "pg_catalog"."default" NOT NULL,
  "revision" int4,
  "created_by" varchar(64) COLLATE "pg_catalog"."default",
  "created_id" varchar(20) COLLATE "pg_catalog"."default",
  "created_time" timestamp(6),
  "updated_by" varchar(64) COLLATE "pg_catalog"."default",
  "updated_id" varchar(20) COLLATE "pg_catalog"."default",
  "updated_time" timestamp(6),
  "tenant_id" varchar(64) COLLATE "pg_catalog"."default" NOT NULL,
  "im_user_id" varchar(64) COLLATE "pg_catalog"."default" NOT NULL,
  "sync_status" int4 NOT NULL DEFAULT 0,
  "last_error" varchar(500) COLLATE "pg_catalog"."default",
  "last_sync_time" timestamp(6),
  CONSTRAINT "t_ucenter_im_service_pkey" PRIMARY KEY ("id"),
  CONSTRAINT "uk_t_ucenter_im_service_tenant" UNIQUE ("tenant_id"),
  CONSTRAINT "uk_t_ucenter_im_service_im" UNIQUE ("im_user_id")
);

COMMENT ON TABLE "public"."t_ucenter_im_account" IS 'wukongim user sync';
COMMENT ON TABLE "public"."t_ucenter_im_service" IS 'wukongim tenant service account sync';

-- 挂在系统中心（code 005）下。若库里系统中心不是 005，改 pcode / ppcode 后再执行。
INSERT INTO "public"."p_ucenter_permission" (
  "id", "revision", "created_time", "updated_time",
  "name", "i18n", "code", "pcode", "ppcode",
  "strategy", "icon", "component", "url", "sort_no", "type",
  "description", "internal_or_external", "platform", "keep_alive"
) VALUES (
  '2069000000000000003', 0, NOW(), NOW(),
  E'\u5BA2\u670D\u5DE5\u4F5C\u53F0', 'menu.imDesk', '005007', '005', ';005;',
  0, 'CustomerServiceOutlined', NULL, '/view/im_desk', 11, 0,
  E'\u672C\u79DF\u6237\u5BA2\u670D\u4F1A\u8BDD', 0, 'mes-edge', false
) ON CONFLICT ("id") DO NOTHING;
