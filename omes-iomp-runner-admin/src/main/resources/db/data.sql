/*
 Navicat Premium Dump SQL

 Source Server         : localhost_5432
 Source Server Type    : PostgreSQL
 Source Server Version : 160013 (160013)
 Source Host           : localhost:5432
 Source Catalog        : omes
 Source Schema         : public

 Target Server Type    : PostgreSQL
 Target Server Version : 160013 (160013)
 File Encoding         : 65001

 Date: 04/06/2026 10:51:24
*/


-- ----------------------------
-- Table structure for ai_agent_audit_log
-- ----------------------------
DROP TABLE IF EXISTS "public"."ai_agent_audit_log";
CREATE TABLE "public"."ai_agent_audit_log" (
  "id" varchar(64) COLLATE "pg_catalog"."default" NOT NULL,
  "session_id" varchar(64) COLLATE "pg_catalog"."default",
  "operator_id" varchar(128) COLLATE "pg_catalog"."default",
  "action" varchar(64) COLLATE "pg_catalog"."default",
  "success_flag" int2 NOT NULL,
  "message" text COLLATE "pg_catalog"."default",
  "created_at" timestamp(6) NOT NULL
)
;

-- ----------------------------
-- Table structure for ai_agent_chat_message
-- ----------------------------
DROP TABLE IF EXISTS "public"."ai_agent_chat_message";
CREATE TABLE "public"."ai_agent_chat_message" (
  "id" varchar(64) COLLATE "pg_catalog"."default" NOT NULL,
  "session_id" varchar(64) COLLATE "pg_catalog"."default" NOT NULL,
  "role" varchar(64) COLLATE "pg_catalog"."default",
  "content" text COLLATE "pg_catalog"."default",
  "created_at" timestamp(6) NOT NULL
)
;

-- ----------------------------
-- Table structure for ai_agent_chat_session
-- ----------------------------
DROP TABLE IF EXISTS "public"."ai_agent_chat_session";
CREATE TABLE "public"."ai_agent_chat_session" (
  "session_id" varchar(64) COLLATE "pg_catalog"."default" NOT NULL,
  "title" varchar(255) COLLATE "pg_catalog"."default",
  "operator_id" varchar(128) COLLATE "pg_catalog"."default",
  "created_at" timestamp(6) NOT NULL,
  "updated_at" timestamp(6) NOT NULL
)
;

-- ----------------------------
-- Table structure for oauth2_authorization
-- ----------------------------
DROP TABLE IF EXISTS "public"."oauth2_authorization";
CREATE TABLE "public"."oauth2_authorization" (
  "id" varchar(100) COLLATE "pg_catalog"."default" NOT NULL,
  "registered_client_id" varchar(100) COLLATE "pg_catalog"."default" NOT NULL,
  "principal_name" varchar(200) COLLATE "pg_catalog"."default" NOT NULL,
  "authorization_grant_type" varchar(100) COLLATE "pg_catalog"."default" NOT NULL,
  "authorized_scopes" varchar(1000) COLLATE "pg_catalog"."default",
  "attributes" bytea,
  "state" varchar(500) COLLATE "pg_catalog"."default",
  "authorization_code_value" bytea,
  "authorization_code_issued_at" timestamp(6),
  "authorization_code_expires_at" timestamp(6),
  "authorization_code_metadata" bytea,
  "access_token_value" bytea,
  "access_token_issued_at" timestamp(6),
  "access_token_expires_at" timestamp(6),
  "access_token_metadata" bytea,
  "access_token_type" varchar(100) COLLATE "pg_catalog"."default",
  "access_token_scopes" varchar(1000) COLLATE "pg_catalog"."default",
  "oidc_id_token_value" bytea,
  "oidc_id_token_issued_at" timestamp(6),
  "oidc_id_token_expires_at" timestamp(6),
  "oidc_id_token_metadata" bytea,
  "refresh_token_value" bytea,
  "refresh_token_issued_at" timestamp(6),
  "refresh_token_expires_at" timestamp(6),
  "refresh_token_metadata" bytea,
  "user_code_value" bytea,
  "user_code_issued_at" timestamp(6),
  "user_code_expires_at" timestamp(6),
  "user_code_metadata" bytea,
  "device_code_value" bytea,
  "device_code_issued_at" timestamp(6),
  "device_code_expires_at" timestamp(6),
  "device_code_metadata" bytea
)
;

-- ----------------------------
-- Table structure for oauth2_authorization_consent
-- ----------------------------
DROP TABLE IF EXISTS "public"."oauth2_authorization_consent";
CREATE TABLE "public"."oauth2_authorization_consent" (
  "registered_client_id" varchar(100) COLLATE "pg_catalog"."default" NOT NULL,
  "principal_name" varchar(200) COLLATE "pg_catalog"."default" NOT NULL,
  "authorities" varchar(1000) COLLATE "pg_catalog"."default" NOT NULL
)
;

-- ----------------------------
-- Table structure for oauth2_registered_client
-- ----------------------------
DROP TABLE IF EXISTS "public"."oauth2_registered_client";
CREATE TABLE "public"."oauth2_registered_client" (
  "id" varchar(100) COLLATE "pg_catalog"."default" NOT NULL,
  "client_id" varchar(100) COLLATE "pg_catalog"."default" NOT NULL,
  "client_id_issued_at" timestamp(6) NOT NULL,
  "client_secret" varchar(200) COLLATE "pg_catalog"."default",
  "client_secret_expires_at" timestamp(6),
  "client_name" varchar(200) COLLATE "pg_catalog"."default" NOT NULL,
  "client_authentication_methods" varchar(1000) COLLATE "pg_catalog"."default" NOT NULL,
  "authorization_grant_types" varchar(1000) COLLATE "pg_catalog"."default" NOT NULL,
  "redirect_uris" varchar(1000) COLLATE "pg_catalog"."default",
  "post_logout_redirect_uris" varchar(1000) COLLATE "pg_catalog"."default",
  "scopes" varchar(1000) COLLATE "pg_catalog"."default" NOT NULL,
  "client_settings" varchar(2000) COLLATE "pg_catalog"."default" NOT NULL,
  "token_settings" varchar(2000) COLLATE "pg_catalog"."default" NOT NULL
)
;

-- ----------------------------
-- Table structure for oauth_client_details
-- ----------------------------
DROP TABLE IF EXISTS "public"."oauth_client_details";
CREATE TABLE "public"."oauth_client_details" (
  "client_id" varchar(64) COLLATE "pg_catalog"."default" NOT NULL,
  "resource_ids" varchar(256) COLLATE "pg_catalog"."default",
  "client_secret" varchar(256) COLLATE "pg_catalog"."default",
  "scope" varchar(256) COLLATE "pg_catalog"."default",
  "authorized_grant_types" varchar(256) COLLATE "pg_catalog"."default",
  "web_server_redirect_uri" varchar(256) COLLATE "pg_catalog"."default",
  "authorities" varchar(256) COLLATE "pg_catalog"."default",
  "access_token_validity" int4,
  "refresh_token_validity" int4,
  "additional_information" varchar(4096) COLLATE "pg_catalog"."default",
  "autoapprove" varchar(256) COLLATE "pg_catalog"."default"
)
;

-- ----------------------------
-- Table structure for p_ucenter_acc
-- ----------------------------
DROP TABLE IF EXISTS "public"."p_ucenter_acc";
CREATE TABLE "public"."p_ucenter_acc" (
  "id" varchar(20) COLLATE "pg_catalog"."default" NOT NULL,
  "revision" int4,
  "created_by" varchar(64) COLLATE "pg_catalog"."default",
  "created_id" varchar(64) COLLATE "pg_catalog"."default",
  "created_time" timestamp(6),
  "updated_by" varchar(64) COLLATE "pg_catalog"."default",
  "updated_id" varchar(64) COLLATE "pg_catalog"."default",
  "updated_time" timestamp(6),
  "acc_name" varchar(60) COLLATE "pg_catalog"."default" NOT NULL,
  "password" varchar(60) COLLATE "pg_catalog"."default" NOT NULL,
  "nick_name" varchar(60) COLLATE "pg_catalog"."default" NOT NULL,
  "user_name" varchar(60) COLLATE "pg_catalog"."default" NOT NULL,
  "id_card" varchar(50) COLLATE "pg_catalog"."default",
  "mobile" varchar(50) COLLATE "pg_catalog"."default",
  "email" varchar(70) COLLATE "pg_catalog"."default",
  "sex" int2,
  "init" int2,
  "status" int2,
  "settled_time" timestamp(6),
  "expire_time" timestamp(6),
  "birth_day" timestamp(6),
  "del_flag" int2 DEFAULT 0,
  "source" varchar(20) COLLATE "pg_catalog"."default",
  "source_id" varchar(64) COLLATE "pg_catalog"."default",
  "platform" varchar(20) COLLATE "pg_catalog"."default" NOT NULL,
  "union_id" varchar(64) COLLATE "pg_catalog"."default",
  "avatar_url" varchar(255) COLLATE "pg_catalog"."default",
  "country" varchar(15) COLLATE "pg_catalog"."default",
  "province" varchar(15) COLLATE "pg_catalog"."default",
  "city" varchar(25) COLLATE "pg_catalog"."default",
  "language" varchar(10) COLLATE "pg_catalog"."default",
  "perfection" int2
)
;
COMMENT ON COLUMN "public"."p_ucenter_acc"."acc_name" IS '账户名';
COMMENT ON COLUMN "public"."p_ucenter_acc"."password" IS '账户密码';
COMMENT ON COLUMN "public"."p_ucenter_acc"."nick_name" IS '账户昵称';
COMMENT ON COLUMN "public"."p_ucenter_acc"."user_name" IS '用户名';
COMMENT ON COLUMN "public"."p_ucenter_acc"."id_card" IS '身份证号';
COMMENT ON COLUMN "public"."p_ucenter_acc"."mobile" IS '账户手机号';
COMMENT ON COLUMN "public"."p_ucenter_acc"."email" IS '账户邮箱';
COMMENT ON COLUMN "public"."p_ucenter_acc"."sex" IS '用户性别';
COMMENT ON COLUMN "public"."p_ucenter_acc"."init" IS '初始状态(1:是初始化账户)';
COMMENT ON COLUMN "public"."p_ucenter_acc"."status" IS '账户状态';
COMMENT ON COLUMN "public"."p_ucenter_acc"."settled_time" IS '入驻时间';
COMMENT ON COLUMN "public"."p_ucenter_acc"."expire_time" IS '过期时间';
COMMENT ON COLUMN "public"."p_ucenter_acc"."birth_day" IS '出生年月';
COMMENT ON COLUMN "public"."p_ucenter_acc"."del_flag" IS '逻辑删除';
COMMENT ON COLUMN "public"."p_ucenter_acc"."source" IS '来源';
COMMENT ON COLUMN "public"."p_ucenter_acc"."source_id" IS '来源id';
COMMENT ON COLUMN "public"."p_ucenter_acc"."platform" IS '平台';
COMMENT ON COLUMN "public"."p_ucenter_acc"."union_id" IS '来源方互通id';
COMMENT ON COLUMN "public"."p_ucenter_acc"."avatar_url" IS '头像路径';
COMMENT ON COLUMN "public"."p_ucenter_acc"."country" IS '国家';
COMMENT ON COLUMN "public"."p_ucenter_acc"."province" IS '省';
COMMENT ON COLUMN "public"."p_ucenter_acc"."city" IS '市';
COMMENT ON COLUMN "public"."p_ucenter_acc"."language" IS '语言';
COMMENT ON COLUMN "public"."p_ucenter_acc"."perfection" IS '账户完善度（0: 未完善，1:已完善）';
COMMENT ON TABLE "public"."p_ucenter_acc" IS '账户表';

-- ----------------------------
-- Table structure for p_ucenter_permission
-- ----------------------------
DROP TABLE IF EXISTS "public"."p_ucenter_permission";
CREATE TABLE "public"."p_ucenter_permission" (
  "id" varchar(20) COLLATE "pg_catalog"."default" NOT NULL,
  "revision" int4,
  "created_by" varchar(64) COLLATE "pg_catalog"."default",
  "created_id" varchar(20) COLLATE "pg_catalog"."default",
  "created_time" timestamp(6),
  "updated_by" varchar(64) COLLATE "pg_catalog"."default",
  "updated_id" varchar(20) COLLATE "pg_catalog"."default",
  "updated_time" timestamp(6),
  "name" varchar(40) COLLATE "pg_catalog"."default" NOT NULL,
  "i18n" varchar(40) COLLATE "pg_catalog"."default",
  "code" varchar(20) COLLATE "pg_catalog"."default" NOT NULL,
  "pcode" varchar(30) COLLATE "pg_catalog"."default",
  "ppcode" varchar(150) COLLATE "pg_catalog"."default",
  "strategy" int2,
  "icon" varchar(50) COLLATE "pg_catalog"."default",
  "component" varchar(150) COLLATE "pg_catalog"."default",
  "url" varchar(150) COLLATE "pg_catalog"."default",
  "sort_no" float8,
  "type" int2,
  "description" varchar(250) COLLATE "pg_catalog"."default",
  "internal_or_external" int2,
  "platform" varchar(20) COLLATE "pg_catalog"."default" NOT NULL,
  "keep_alive" bool DEFAULT false
)
;
COMMENT ON COLUMN "public"."p_ucenter_permission"."name" IS '名称';
COMMENT ON COLUMN "public"."p_ucenter_permission"."code" IS '编号';
COMMENT ON COLUMN "public"."p_ucenter_permission"."pcode" IS '父编号';
COMMENT ON COLUMN "public"."p_ucenter_permission"."ppcode" IS '祖宗编号';
COMMENT ON COLUMN "public"."p_ucenter_permission"."strategy" IS '权限策略';
COMMENT ON COLUMN "public"."p_ucenter_permission"."icon" IS '图标';
COMMENT ON COLUMN "public"."p_ucenter_permission"."component" IS '组件';
COMMENT ON COLUMN "public"."p_ucenter_permission"."url" IS '跳转网页链接';
COMMENT ON COLUMN "public"."p_ucenter_permission"."sort_no" IS '菜单排序';
COMMENT ON COLUMN "public"."p_ucenter_permission"."type" IS '权限类型(0:菜单权限  1:按钮权限  2:空描述权限)';
COMMENT ON COLUMN "public"."p_ucenter_permission"."description" IS '描述';
COMMENT ON COLUMN "public"."p_ucenter_permission"."internal_or_external" IS '外链菜单打开方式(0:内部打开 1:外部打开)';
COMMENT ON COLUMN "public"."p_ucenter_permission"."platform" IS '所属平台';
COMMENT ON TABLE "public"."p_ucenter_permission" IS '权限表';

-- ----------------------------
-- Table structure for p_ucenter_platform
-- ----------------------------
DROP TABLE IF EXISTS "public"."p_ucenter_platform";
CREATE TABLE "public"."p_ucenter_platform" (
  "id" varchar(20) COLLATE "pg_catalog"."default" NOT NULL,
  "name" varchar(25) COLLATE "pg_catalog"."default" NOT NULL,
  "code" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
  "revision" int4,
  "created_by" varchar(64) COLLATE "pg_catalog"."default",
  "created_id" varchar(64) COLLATE "pg_catalog"."default",
  "created_time" timestamp(6),
  "updated_by" varchar(64) COLLATE "pg_catalog"."default",
  "updated_id" varchar(64) COLLATE "pg_catalog"."default",
  "updated_time" timestamp(6)
)
;
COMMENT ON COLUMN "public"."p_ucenter_platform"."name" IS '名称';
COMMENT ON COLUMN "public"."p_ucenter_platform"."code" IS '编号';

-- ----------------------------
-- Table structure for p_ucenter_tenant
-- ----------------------------
DROP TABLE IF EXISTS "public"."p_ucenter_tenant";
CREATE TABLE "public"."p_ucenter_tenant" (
  "id" varchar(20) COLLATE "pg_catalog"."default" NOT NULL,
  "revision" int4,
  "created_by" varchar(64) COLLATE "pg_catalog"."default",
  "created_id" varchar(64) COLLATE "pg_catalog"."default",
  "created_time" timestamp(6),
  "updated_by" varchar(64) COLLATE "pg_catalog"."default",
  "updated_id" varchar(64) COLLATE "pg_catalog"."default",
  "updated_time" timestamp(6),
  "tenant_name" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
  "tenant_code" varchar(20) COLLATE "pg_catalog"."default" NOT NULL,
  "status" int2,
  "settled_time" timestamp(6),
  "expire_time" timestamp(6),
  "tenant_id" varchar(20) COLLATE "pg_catalog"."default" NOT NULL,
  "tenant_address" varchar(50) COLLATE "pg_catalog"."default",
  "tenant_contacts" varchar(10) COLLATE "pg_catalog"."default",
  "tenant_phone" varchar(50) COLLATE "pg_catalog"."default",
  "tenant_mail" varchar(40) COLLATE "pg_catalog"."default",
  "province_code" varchar(40) COLLATE "pg_catalog"."default",
  "city_code" varchar(40) COLLATE "pg_catalog"."default",
  "county_code" varchar(40) COLLATE "pg_catalog"."default",
  "street_code" varchar(40) COLLATE "pg_catalog"."default",
  "area_fullname" varchar(100) COLLATE "pg_catalog"."default",
  "tenant_coo" varchar(50) COLLATE "pg_catalog"."default",
  "pcode" varchar(20) COLLATE "pg_catalog"."default",
  "client_id" varchar(50) COLLATE "pg_catalog"."default",
  "management" int2,
  "manage_num" int4,
  "logo" varchar(150) COLLATE "pg_catalog"."default",
  "tel" varchar(150) COLLATE "pg_catalog"."default",
  "del_flag" int2 DEFAULT 0
)
;
COMMENT ON COLUMN "public"."p_ucenter_tenant"."tenant_name" IS '租户名';
COMMENT ON COLUMN "public"."p_ucenter_tenant"."tenant_code" IS '租户编号';
COMMENT ON COLUMN "public"."p_ucenter_tenant"."status" IS '租户状态';
COMMENT ON COLUMN "public"."p_ucenter_tenant"."settled_time" IS '入驻时间';
COMMENT ON COLUMN "public"."p_ucenter_tenant"."expire_time" IS '过期时间';
COMMENT ON COLUMN "public"."p_ucenter_tenant"."tenant_id" IS '配合框架占位';
COMMENT ON COLUMN "public"."p_ucenter_tenant"."tenant_address" IS '租户/机构所在地址';
COMMENT ON COLUMN "public"."p_ucenter_tenant"."tenant_contacts" IS '联系人姓名';
COMMENT ON COLUMN "public"."p_ucenter_tenant"."tenant_phone" IS '服务电话';
COMMENT ON COLUMN "public"."p_ucenter_tenant"."tenant_mail" IS '联系邮箱';
COMMENT ON COLUMN "public"."p_ucenter_tenant"."province_code" IS '省份code：详见yl_elder.r_city_dict省市级联字典表code';
COMMENT ON COLUMN "public"."p_ucenter_tenant"."city_code" IS '城市code：详见yl_elder.r_city_dict省市级联字典表code';
COMMENT ON COLUMN "public"."p_ucenter_tenant"."county_code" IS '区县code：详见yl_elder.r_city_dict省市级联字典表code';
COMMENT ON COLUMN "public"."p_ucenter_tenant"."street_code" IS '街道code：详见yl_elder.r_city_dict省市级联字典表code';
COMMENT ON COLUMN "public"."p_ucenter_tenant"."area_fullname" IS '所在区域：省-市-区县-街道拼接';
COMMENT ON COLUMN "public"."p_ucenter_tenant"."tenant_coo" IS '租户机构经纬度坐标';
COMMENT ON COLUMN "public"."p_ucenter_tenant"."client_id" IS '补充临时字段，作用待定';
COMMENT ON COLUMN "public"."p_ucenter_tenant"."management" IS '管控权限';
COMMENT ON COLUMN "public"."p_ucenter_tenant"."manage_num" IS '管控数量';
COMMENT ON COLUMN "public"."p_ucenter_tenant"."logo" IS 'logo';
COMMENT ON COLUMN "public"."p_ucenter_tenant"."tel" IS '客服电话';
COMMENT ON TABLE "public"."p_ucenter_tenant" IS '租户表';

-- ----------------------------
-- Table structure for r_bom_d
-- ----------------------------
DROP TABLE IF EXISTS "public"."r_bom_d";
CREATE TABLE "public"."r_bom_d" (
  "id" varchar(20) COLLATE "pg_catalog"."default" NOT NULL,
  "revision" int4,
  "created_by" varchar(20) COLLATE "pg_catalog"."default",
  "created_id" varchar(20) COLLATE "pg_catalog"."default",
  "created_time" timestamp(6),
  "updated_by" varchar(20) COLLATE "pg_catalog"."default",
  "updated_id" varchar(20) COLLATE "pg_catalog"."default",
  "updated_time" timestamp(6),
  "del_bit" int2 DEFAULT 0,
  "mat_id" varchar(20) COLLATE "pg_catalog"."default",
  "mat_name" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
  "mat_code" varchar(50) COLLATE "pg_catalog"."default",
  "mcode" varchar(50) COLLATE "pg_catalog"."default",
  "mat_scale" numeric(10,4),
  "dev_no" varchar(4) COLLATE "pg_catalog"."default",
  "attribute" int2,
  "priority" int4
)
;
COMMENT ON COLUMN "public"."r_bom_d"."mat_id" IS '物料id';
COMMENT ON COLUMN "public"."r_bom_d"."mat_name" IS '物料名称';
COMMENT ON COLUMN "public"."r_bom_d"."mat_code" IS '物料编号';
COMMENT ON COLUMN "public"."r_bom_d"."mcode" IS '主体编号';
COMMENT ON COLUMN "public"."r_bom_d"."mat_scale" IS '物料数值（对应物料清单的类型，决定是百分比还是固定值）';
COMMENT ON COLUMN "public"."r_bom_d"."dev_no" IS '设备编号';
COMMENT ON COLUMN "public"."r_bom_d"."attribute" IS '组份性质：0=主料，1=预混料（添加剂）,2=回机料,3=油,4=水';
COMMENT ON COLUMN "public"."r_bom_d"."priority" IS '优先级（数值越大，优先级越高）';
COMMENT ON TABLE "public"."r_bom_d" IS '配方详情';

-- ----------------------------
-- Table structure for r_dg_d
-- ----------------------------
DROP TABLE IF EXISTS "public"."r_dg_d";
CREATE TABLE "public"."r_dg_d" (
  "dg_id" varchar(20) COLLATE "pg_catalog"."default" NOT NULL,
  "d_id" varchar(20) COLLATE "pg_catalog"."default" NOT NULL,
  "mat_id" varchar(20) COLLATE "pg_catalog"."default" NOT NULL
)
;
COMMENT ON COLUMN "public"."r_dg_d"."dg_id" IS '设备组id';
COMMENT ON COLUMN "public"."r_dg_d"."d_id" IS '设备id';
COMMENT ON COLUMN "public"."r_dg_d"."mat_id" IS '物料id';
COMMENT ON TABLE "public"."r_dg_d" IS '设备组关联';

-- ----------------------------
-- Table structure for r_dg_equip
-- ----------------------------
DROP TABLE IF EXISTS "public"."r_dg_equip";
CREATE TABLE "public"."r_dg_equip" (
  "id" varchar(20) COLLATE "pg_catalog"."default" NOT NULL,
  "dg_id" varchar(20) COLLATE "pg_catalog"."default" NOT NULL,
  "equip_id" varchar(20) COLLATE "pg_catalog"."default" NOT NULL,
  CONSTRAINT "r_dg_equip_pkey" PRIMARY KEY ("id"),
  CONSTRAINT "uk_r_dg_equip" UNIQUE ("dg_id", "equip_id")
)
;
COMMENT ON COLUMN "public"."r_dg_equip"."dg_id" IS '设备能力id';
COMMENT ON COLUMN "public"."r_dg_equip"."equip_id" IS '设备id（t_equip）';
COMMENT ON TABLE "public"."r_dg_equip" IS '设备能力绑定设备';

-- ----------------------------
-- Table structure for r_dg_equip_mat
-- ----------------------------
DROP TABLE IF EXISTS "public"."r_dg_equip_mat";
CREATE TABLE "public"."r_dg_equip_mat" (
  "id" varchar(20) COLLATE "pg_catalog"."default" NOT NULL,
  "dg_id" varchar(20) COLLATE "pg_catalog"."default" NOT NULL,
  "equip_id" varchar(20) COLLATE "pg_catalog"."default" NOT NULL,
  "mat_code" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
  "max_capacity" numeric(18,4),
  CONSTRAINT "r_dg_equip_mat_pkey" PRIMARY KEY ("id"),
  CONSTRAINT "uk_r_dg_equip_mat" UNIQUE ("dg_id", "equip_id", "mat_code")
)
;
COMMENT ON COLUMN "public"."r_dg_equip_mat"."dg_id" IS '设备能力id';
COMMENT ON COLUMN "public"."r_dg_equip_mat"."equip_id" IS '设备id';
COMMENT ON COLUMN "public"."r_dg_equip_mat"."mat_code" IS '原料编号（t_mat.self_code）';
COMMENT ON COLUMN "public"."r_dg_equip_mat"."max_capacity" IS '该原料在本能力方案下的容量';
COMMENT ON TABLE "public"."r_dg_equip_mat" IS '设备能力加工原料及容量';

-- ----------------------------
-- Table structure for r_gw_binding
-- ----------------------------
DROP TABLE IF EXISTS "public"."r_gw_binding";
CREATE TABLE "public"."r_gw_binding" (
  "equip_id" varchar(20) COLLATE "pg_catalog"."default" NOT NULL,
  "gw_id" varchar(20) COLLATE "pg_catalog"."default",
  PRIMARY KEY ("equip_id")
)
;
COMMENT ON COLUMN "public"."r_gw_binding"."equip_id" IS '设备id';
COMMENT ON COLUMN "public"."r_gw_binding"."gw_id" IS '网关id';

-- ----------------------------
-- Table structure for r_message_read
-- ----------------------------
DROP TABLE IF EXISTS "public"."r_message_read";
CREATE TABLE "public"."r_message_read" (
  "message_id" varchar(20) COLLATE "pg_catalog"."default" NOT NULL,
  "acc_id" varchar(20) COLLATE "pg_catalog"."default" NOT NULL,
  "time" timestamp(6),
  "is_read" bool NOT NULL DEFAULT false
)
;
COMMENT ON COLUMN "public"."r_message_read"."message_id" IS '消息id';
COMMENT ON COLUMN "public"."r_message_read"."acc_id" IS '账户id';
COMMENT ON COLUMN "public"."r_message_read"."time" IS '读取时间';
COMMENT ON COLUMN "public"."r_message_read"."is_read" IS '是否已读';

-- ----------------------------
-- Table structure for r_mo_d
-- ----------------------------
DROP TABLE IF EXISTS "public"."r_mo_d";
CREATE TABLE "public"."r_mo_d" (
  "id" varchar(20) COLLATE "pg_catalog"."default" NOT NULL,
  "revision" int4,
  "created_by" varchar(20) COLLATE "pg_catalog"."default",
  "created_id" varchar(20) COLLATE "pg_catalog"."default",
  "created_time" timestamp(6),
  "updated_by" varchar(20) COLLATE "pg_catalog"."default",
  "updated_id" varchar(20) COLLATE "pg_catalog"."default",
  "updated_time" timestamp(6),
  "tenant_id" varchar(20) COLLATE "pg_catalog"."default",
  "del_bit" int2 DEFAULT 0,
  "mat_id" varchar(20) COLLATE "pg_catalog"."default",
  "mat_name" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
  "mat_code" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
  "mat_num" numeric(10,5),
  "priority" int4,
  "mcode" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
  "dev_no" varchar(20) COLLATE "pg_catalog"."default",
  "dev_name" varchar(50) COLLATE "pg_catalog"."default",
  "dg_code" varchar(20) COLLATE "pg_catalog"."default",
  "dg_name" varchar(50) COLLATE "pg_catalog"."default"
)
;
COMMENT ON COLUMN "public"."r_mo_d"."mat_id" IS '物料id';
COMMENT ON COLUMN "public"."r_mo_d"."mat_name" IS '物料名称';
COMMENT ON COLUMN "public"."r_mo_d"."mat_code" IS '物料编号';
COMMENT ON COLUMN "public"."r_mo_d"."mat_num" IS '物料数量';
COMMENT ON COLUMN "public"."r_mo_d"."priority" IS '优先级（数值越大，优先级越高）';
COMMENT ON COLUMN "public"."r_mo_d"."mcode" IS '归属清单';
COMMENT ON COLUMN "public"."r_mo_d"."dev_no" IS '设备编号';
COMMENT ON COLUMN "public"."r_mo_d"."dev_name" IS '设备名';
COMMENT ON COLUMN "public"."r_mo_d"."dg_code" IS '设备能力code';
COMMENT ON COLUMN "public"."r_mo_d"."dg_name" IS '设备能力名';
COMMENT ON TABLE "public"."r_mo_d" IS '生产清单详情';

-- ----------------------------
-- Table structure for r_mps_d
-- ----------------------------
DROP TABLE IF EXISTS "public"."r_mps_d";
CREATE TABLE "public"."r_mps_d" (
  "id" varchar(20) COLLATE "pg_catalog"."default" NOT NULL,
  "revision" int4,
  "created_by" varchar(20) COLLATE "pg_catalog"."default",
  "created_id" varchar(20) COLLATE "pg_catalog"."default",
  "created_time" timestamp(6),
  "updated_by" varchar(20) COLLATE "pg_catalog"."default",
  "updated_id" varchar(20) COLLATE "pg_catalog"."default",
  "updated_time" timestamp(6),
  "tenant_id" varchar(20) COLLATE "pg_catalog"."default",
  "del_bit" int2 DEFAULT 0,
  "mat_id" varchar(20) COLLATE "pg_catalog"."default",
  "mat_name" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
  "mat_code" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
  "mid" varchar(50) COLLATE "pg_catalog"."default",
  "mat_num" numeric(25,5),
  "actual_num" numeric(25,5),
  "dev_no" varchar(20) COLLATE "pg_catalog"."default",
  "dev_name" varchar(50) COLLATE "pg_catalog"."default",
  "attribute" int2,
  "priority" int4,
  "mo_code" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
  "dg_code" varchar(20) COLLATE "pg_catalog"."default",
  "dg_name" varchar(50) COLLATE "pg_catalog"."default"
)
;
COMMENT ON COLUMN "public"."r_mps_d"."mat_id" IS '原料id';
COMMENT ON COLUMN "public"."r_mps_d"."mat_name" IS '原料名称';
COMMENT ON COLUMN "public"."r_mps_d"."mat_code" IS '原料编号';
COMMENT ON COLUMN "public"."r_mps_d"."mid" IS '主体id';
COMMENT ON COLUMN "public"."r_mps_d"."mat_num" IS '理论值';
COMMENT ON COLUMN "public"."r_mps_d"."actual_num" IS '实际值';
COMMENT ON COLUMN "public"."r_mps_d"."dev_no" IS '设备号';
COMMENT ON COLUMN "public"."r_mps_d"."dev_name" IS '设备名';
COMMENT ON COLUMN "public"."r_mps_d"."attribute" IS '组份性质：0=主料，1=预混料（添加剂）,2=回机料,3=油,4=水';
COMMENT ON COLUMN "public"."r_mps_d"."priority" IS '投料顺序';
COMMENT ON COLUMN "public"."r_mps_d"."mo_code" IS '归属清单';
COMMENT ON COLUMN "public"."r_mps_d"."dg_code" IS '设备能力code';
COMMENT ON COLUMN "public"."r_mps_d"."dg_name" IS '设备能力名';
COMMENT ON TABLE "public"."r_mps_d" IS '生产计划详情';

-- ----------------------------
-- Table structure for r_mps_tf
-- ----------------------------
DROP TABLE IF EXISTS "public"."r_mps_tf";
CREATE TABLE "public"."r_mps_tf" (
  "id" varchar(20) COLLATE "pg_catalog"."default" NOT NULL,
  "revision" int4,
  "created_by" varchar(20) COLLATE "pg_catalog"."default",
  "created_id" varchar(20) COLLATE "pg_catalog"."default",
  "created_time" timestamp(6),
  "updated_by" varchar(20) COLLATE "pg_catalog"."default",
  "updated_id" varchar(20) COLLATE "pg_catalog"."default",
  "updated_time" timestamp(6),
  "mps_id" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
  "self_code" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
  "name" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
  "start_time" timestamp(6),
  "end_time" timestamp(6),
  "start_temperature" numeric(7,2),
  "end_temperature" numeric(7,2),
  "status" int2,
  "pre" varchar(50) COLLATE "pg_catalog"."default",
  "nex" varchar(50) COLLATE "pg_catalog"."default",
  "mo_code" varchar(50) COLLATE "pg_catalog"."default" NOT NULL
)
;
COMMENT ON COLUMN "public"."r_mps_tf"."mps_id" IS '执行计划id';
COMMENT ON COLUMN "public"."r_mps_tf"."self_code" IS '工艺流程编号(可以与工艺设置进行映射)';
COMMENT ON COLUMN "public"."r_mps_tf"."name" IS '工艺流程名称';
COMMENT ON COLUMN "public"."r_mps_tf"."start_time" IS '开始执行的时间';
COMMENT ON COLUMN "public"."r_mps_tf"."end_time" IS '执行完成时间';
COMMENT ON COLUMN "public"."r_mps_tf"."start_temperature" IS '入槽温度';
COMMENT ON COLUMN "public"."r_mps_tf"."end_temperature" IS '出槽温度';
COMMENT ON COLUMN "public"."r_mps_tf"."status" IS '流程状态(0:待执行|1执行中|2已完成|3停止|4异常中断)';
COMMENT ON COLUMN "public"."r_mps_tf"."pre" IS '上一流程编号';
COMMENT ON COLUMN "public"."r_mps_tf"."nex" IS '下一流程编号';
COMMENT ON COLUMN "public"."r_mps_tf"."mo_code" IS '归属清单';
COMMENT ON TABLE "public"."r_mps_tf" IS '工艺流程';

-- ----------------------------
-- Table structure for r_sync_resource
-- ----------------------------
DROP TABLE IF EXISTS "public"."r_sync_resource";
CREATE TABLE "public"."r_sync_resource" (
  "id" varchar(20) COLLATE "pg_catalog"."default" NOT NULL,
  "revision" int4,
  "created_by" varchar(20) COLLATE "pg_catalog"."default",
  "created_id" varchar(20) COLLATE "pg_catalog"."default",
  "created_time" timestamp(6),
  "updated_by" varchar(20) COLLATE "pg_catalog"."default",
  "updated_id" varchar(20) COLLATE "pg_catalog"."default",
  "updated_time" timestamp(6),
  "sync_id" varchar(20) COLLATE "pg_catalog"."default" NOT NULL,
  "point" varchar(20) COLLATE "pg_catalog"."default" NOT NULL,
  "status" varchar(20) COLLATE "pg_catalog"."default" NOT NULL,
  "req_data" text COLLATE "pg_catalog"."default",
  "resp_data" text COLLATE "pg_catalog"."default",
  "excep" text COLLATE "pg_catalog"."default"
)
;
COMMENT ON COLUMN "public"."r_sync_resource"."sync_id" IS '同步事务id';
COMMENT ON COLUMN "public"."r_sync_resource"."point" IS '当前事务资源';
COMMENT ON COLUMN "public"."r_sync_resource"."status" IS '当前资源状态（start|error|end）';
COMMENT ON COLUMN "public"."r_sync_resource"."req_data" IS '当前资源入参';
COMMENT ON COLUMN "public"."r_sync_resource"."resp_data" IS '当前资源出参';
COMMENT ON COLUMN "public"."r_sync_resource"."excep" IS '异常信息';
COMMENT ON TABLE "public"."r_sync_resource" IS '同步事务资源';

-- ----------------------------
-- Table structure for r_ucenter_acc_role
-- ----------------------------
DROP TABLE IF EXISTS "public"."r_ucenter_acc_role";
CREATE TABLE "public"."r_ucenter_acc_role" (
  "role_id" varchar(20) COLLATE "pg_catalog"."default" NOT NULL,
  "acc_id" varchar(20) COLLATE "pg_catalog"."default" NOT NULL
)
;
COMMENT ON COLUMN "public"."r_ucenter_acc_role"."role_id" IS '角色id';
COMMENT ON COLUMN "public"."r_ucenter_acc_role"."acc_id" IS '用户id';
COMMENT ON TABLE "public"."r_ucenter_acc_role" IS '账户角色';

-- ----------------------------
-- Table structure for r_ucenter_depart_role
-- ----------------------------
DROP TABLE IF EXISTS "public"."r_ucenter_depart_role";
CREATE TABLE "public"."r_ucenter_depart_role" (
  "role_id" varchar(20) COLLATE "pg_catalog"."default" NOT NULL,
  "depart_id" varchar(20) COLLATE "pg_catalog"."default" NOT NULL
)
;
COMMENT ON COLUMN "public"."r_ucenter_depart_role"."role_id" IS '角色id';
COMMENT ON COLUMN "public"."r_ucenter_depart_role"."depart_id" IS '部门id';
COMMENT ON TABLE "public"."r_ucenter_depart_role" IS '部门角色';

-- ----------------------------
-- Table structure for r_ucenter_depart_users
-- ----------------------------
DROP TABLE IF EXISTS "public"."r_ucenter_depart_users";
CREATE TABLE "public"."r_ucenter_depart_users" (
  "depart_id" varchar(20) COLLATE "pg_catalog"."default" NOT NULL,
  "acc_id" varchar(20) COLLATE "pg_catalog"."default" NOT NULL
)
;
COMMENT ON COLUMN "public"."r_ucenter_depart_users"."depart_id" IS '用户组id';
COMMENT ON COLUMN "public"."r_ucenter_depart_users"."acc_id" IS '用户id';
COMMENT ON TABLE "public"."r_ucenter_depart_users" IS '用户组用户关联表';

-- ----------------------------
-- Table structure for r_ucenter_role_permission
-- ----------------------------
DROP TABLE IF EXISTS "public"."r_ucenter_role_permission";
CREATE TABLE "public"."r_ucenter_role_permission" (
  "role_id" varchar(20) COLLATE "pg_catalog"."default" NOT NULL,
  "permission_id" varchar(20) COLLATE "pg_catalog"."default" NOT NULL
)
;
COMMENT ON COLUMN "public"."r_ucenter_role_permission"."role_id" IS '角色id';
COMMENT ON COLUMN "public"."r_ucenter_role_permission"."permission_id" IS '权限id';
COMMENT ON TABLE "public"."r_ucenter_role_permission" IS '角色权限';

-- ----------------------------
-- Table structure for r_ucenter_tenant_acc
-- ----------------------------
DROP TABLE IF EXISTS "public"."r_ucenter_tenant_acc";
CREATE TABLE "public"."r_ucenter_tenant_acc" (
  "tenant_id" varchar(20) COLLATE "pg_catalog"."default" NOT NULL,
  "acc_id" varchar(20) COLLATE "pg_catalog"."default" NOT NULL,
  "role" varchar(20) COLLATE "pg_catalog"."default" NOT NULL
)
;
COMMENT ON COLUMN "public"."r_ucenter_tenant_acc"."tenant_id" IS '租户id';
COMMENT ON COLUMN "public"."r_ucenter_tenant_acc"."acc_id" IS '用户id';
COMMENT ON COLUMN "public"."r_ucenter_tenant_acc"."role" IS '角色';
COMMENT ON TABLE "public"."r_ucenter_tenant_acc" IS '租户用户关联表';

-- ----------------------------
-- Table structure for r_ucenter_tenant_permission
-- ----------------------------
DROP TABLE IF EXISTS "public"."r_ucenter_tenant_permission";
CREATE TABLE "public"."r_ucenter_tenant_permission" (
  "tenant_id" varchar(20) COLLATE "pg_catalog"."default" NOT NULL,
  "permission_id" varchar(20) COLLATE "pg_catalog"."default" NOT NULL
)
;
COMMENT ON COLUMN "public"."r_ucenter_tenant_permission"."tenant_id" IS '租户id';
COMMENT ON COLUMN "public"."r_ucenter_tenant_permission"."permission_id" IS '权限id';
COMMENT ON TABLE "public"."r_ucenter_tenant_permission" IS '租户权限';

-- ----------------------------
-- Table structure for r_workshop_assign
-- ----------------------------
DROP TABLE IF EXISTS "public"."r_workshop_assign";
CREATE TABLE "public"."r_workshop_assign" (
  "assign_id" varchar(20) COLLATE "pg_catalog"."default" NOT NULL,
  "workshop_code" varchar(50) COLLATE "pg_catalog"."default" NOT NULL
)
;
COMMENT ON COLUMN "public"."r_workshop_assign"."assign_id" IS '角色id';
COMMENT ON COLUMN "public"."r_workshop_assign"."workshop_code" IS '场景编号';
COMMENT ON TABLE "public"."r_workshop_assign" IS '角色场景';

-- ----------------------------
-- Table structure for r_workshop_config_collect
-- ----------------------------
DROP TABLE IF EXISTS "public"."r_workshop_config_collect";
CREATE TABLE "public"."r_workshop_config_collect" (
  "workshop_id" varchar(20) COLLATE "pg_catalog"."default" NOT NULL,
  "config" text COLLATE "pg_catalog"."default",
  "gateway_ids" text COLLATE "pg_catalog"."default"
)
;
COMMENT ON COLUMN "public"."r_workshop_config_collect"."workshop_id" IS '设备id';
COMMENT ON COLUMN "public"."r_workshop_config_collect"."gateway_ids" IS '关联的网关ID列表，从 config.attrs 解析';
COMMENT ON TABLE "public"."r_workshop_config_collect" IS '场景采集配置';

-- ----------------------------
-- Table structure for r_workshop_config_meta2d
-- ----------------------------
DROP TABLE IF EXISTS "public"."r_workshop_config_meta2d";
CREATE TABLE "public"."r_workshop_config_meta2d" (
  "id" varchar(64) COLLATE "pg_catalog"."default" NOT NULL,
  "workshop_id" varchar(64) COLLATE "pg_catalog"."default" NOT NULL,
  "meta2d_config" text COLLATE "pg_catalog"."default",
  "revision" int4,
  "created_by" varchar(20) COLLATE "pg_catalog"."default",
  "created_id" varchar(20) COLLATE "pg_catalog"."default",
  "created_time" timestamp(6),
  "updated_by" varchar(20) COLLATE "pg_catalog"."default",
  "updated_id" varchar(20) COLLATE "pg_catalog"."default",
  "updated_time" timestamp(6)
)
;

-- ----------------------------
-- Table structure for r_workshop_config_scada
-- ----------------------------
DROP TABLE IF EXISTS "public"."r_workshop_config_scada";
CREATE TABLE "public"."r_workshop_config_scada" (
  "workshop_id" varchar(20) COLLATE "pg_catalog"."default" NOT NULL,
  "scada_config" text COLLATE "pg_catalog"."default",
  "id" varchar(32) COLLATE "pg_catalog"."default" NOT NULL,
  "revision" int4,
  "created_by" varchar(20) COLLATE "pg_catalog"."default",
  "created_id" varchar(20) COLLATE "pg_catalog"."default",
  "created_time" timestamp(6),
  "updated_by" varchar(20) COLLATE "pg_catalog"."default",
  "updated_id" varchar(20) COLLATE "pg_catalog"."default",
  "updated_time" timestamp(6)
)
;

-- ----------------------------
-- Table structure for t_bom
-- ----------------------------
DROP TABLE IF EXISTS "public"."t_bom";
CREATE TABLE "public"."t_bom" (
  "id" varchar(20) COLLATE "pg_catalog"."default" NOT NULL,
  "revision" int4,
  "created_by" varchar(20) COLLATE "pg_catalog"."default",
  "created_id" varchar(20) COLLATE "pg_catalog"."default",
  "created_time" timestamp(6),
  "updated_by" varchar(20) COLLATE "pg_catalog"."default",
  "updated_id" varchar(20) COLLATE "pg_catalog"."default",
  "updated_time" timestamp(6),
  "tenant_id" varchar(20) COLLATE "pg_catalog"."default",
  "name" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
  "self_code" varchar(50) COLLATE "pg_catalog"."default",
  "classify_code" varchar(50) COLLATE "pg_catalog"."default",
  "type" int2,
  "line" int4,
  "del_bit" int2 DEFAULT 0
)
;
COMMENT ON COLUMN "public"."t_bom"."self_code" IS '配方编号';
COMMENT ON COLUMN "public"."t_bom"."classify_code" IS '所属分类编号';
COMMENT ON COLUMN "public"."t_bom"."type" IS '配方类别（0: 固定数值，1：百分比数值）';
COMMENT ON COLUMN "public"."t_bom"."line" IS '暂时无用';
COMMENT ON TABLE "public"."t_bom" IS '配方';

-- ----------------------------
-- Table structure for t_bom_c
-- ----------------------------
DROP TABLE IF EXISTS "public"."t_bom_c";
CREATE TABLE "public"."t_bom_c" (
  "id" varchar(20) COLLATE "pg_catalog"."default" NOT NULL,
  "revision" int4,
  "created_by" varchar(20) COLLATE "pg_catalog"."default",
  "created_id" varchar(20) COLLATE "pg_catalog"."default",
  "created_time" timestamp(6),
  "updated_by" varchar(20) COLLATE "pg_catalog"."default",
  "updated_id" varchar(20) COLLATE "pg_catalog"."default",
  "updated_time" timestamp(6),
  "tenant_id" varchar(20) COLLATE "pg_catalog"."default",
  "name" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
  "self_code" varchar(50) COLLATE "pg_catalog"."default",
  "code" varchar(60) COLLATE "pg_catalog"."default",
  "pcode" varchar(60) COLLATE "pg_catalog"."default",
  "description" varchar(255) COLLATE "pg_catalog"."default"
)
;
COMMENT ON COLUMN "public"."t_bom_c"."name" IS '分类名称';
COMMENT ON COLUMN "public"."t_bom_c"."self_code" IS '分类编号';
COMMENT ON COLUMN "public"."t_bom_c"."code" IS '级联编号';
COMMENT ON COLUMN "public"."t_bom_c"."pcode" IS '级联父编号';
COMMENT ON COLUMN "public"."t_bom_c"."description" IS '描述';
COMMENT ON TABLE "public"."t_bom_c" IS '配方分类';

-- ----------------------------
-- Table structure for t_connect
-- ----------------------------
DROP TABLE IF EXISTS "public"."t_connect";
CREATE TABLE "public"."t_connect" (
  "id" varchar(20) COLLATE "pg_catalog"."default" NOT NULL,
  "revision" int4,
  "created_by" varchar(20) COLLATE "pg_catalog"."default",
  "created_id" varchar(20) COLLATE "pg_catalog"."default",
  "created_time" timestamp(6),
  "updated_by" varchar(20) COLLATE "pg_catalog"."default",
  "updated_id" varchar(20) COLLATE "pg_catalog"."default",
  "updated_time" timestamp(6),
  "tenant_id" varchar(20) COLLATE "pg_catalog"."default",
  "server_name" varchar(50) COLLATE "pg_catalog"."default",
  "host" varchar(255) COLLATE "pg_catalog"."default",
  "port" int4,
  "suffix" varchar(50) COLLATE "pg_catalog"."default",
  "protocol" varchar(20) COLLATE "pg_catalog"."default",
  "enabled" varchar(1) COLLATE "pg_catalog"."default",
  "username" varchar(150) COLLATE "pg_catalog"."default",
  "password" varchar(150) COLLATE "pg_catalog"."default",
  "valid_type" int2,
  "collect_cron" varchar(64) COLLATE "pg_catalog"."default"
)
;
COMMENT ON COLUMN "public"."t_connect"."server_name" IS '服务名';
COMMENT ON COLUMN "public"."t_connect"."host" IS 'ip';
COMMENT ON COLUMN "public"."t_connect"."suffix" IS '后缀';
COMMENT ON COLUMN "public"."t_connect"."protocol" IS '协议类型';
COMMENT ON COLUMN "public"."t_connect"."enabled" IS '是否启用';
COMMENT ON COLUMN "public"."t_connect"."valid_type" IS '认证类型（0:无认证， 1:Baisc）';
COMMENT ON COLUMN "public"."t_connect"."collect_cron" IS '采集cron表达式';
COMMENT ON TABLE "public"."t_connect" IS '网关';

-- ----------------------------
-- Table structure for t_dg
-- ----------------------------
DROP TABLE IF EXISTS "public"."t_dg";
CREATE TABLE "public"."t_dg" (
  "id" varchar(20) COLLATE "pg_catalog"."default" NOT NULL,
  "revision" int4,
  "created_by" varchar(20) COLLATE "pg_catalog"."default",
  "created_id" varchar(20) COLLATE "pg_catalog"."default",
  "created_time" timestamp(6),
  "updated_by" varchar(20) COLLATE "pg_catalog"."default",
  "updated_id" varchar(20) COLLATE "pg_catalog"."default",
  "updated_time" timestamp(6),
  "tenant_id" varchar(20) COLLATE "pg_catalog"."default",
  "name" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
  "self_code" varchar(50) COLLATE "pg_catalog"."default"
)
;
COMMENT ON COLUMN "public"."t_dg"."self_code" IS '编号';
COMMENT ON TABLE "public"."t_dg" IS '设备能力';

-- ----------------------------
-- Table structure for t_equip
-- ----------------------------
DROP TABLE IF EXISTS "public"."t_equip";
CREATE TABLE "public"."t_equip" (
  "id" varchar(20) COLLATE "pg_catalog"."default" NOT NULL,
  "revision" int4,
  "created_by" varchar(20) COLLATE "pg_catalog"."default",
  "created_id" varchar(20) COLLATE "pg_catalog"."default",
  "created_time" timestamp(6),
  "updated_by" varchar(20) COLLATE "pg_catalog"."default",
  "updated_id" varchar(20) COLLATE "pg_catalog"."default",
  "updated_time" timestamp(6),
  "tenant_id" varchar(20) COLLATE "pg_catalog"."default",
  "name" varchar(30) COLLATE "pg_catalog"."default" NOT NULL,
  "self_code" varchar(150) COLLATE "pg_catalog"."default" NOT NULL,
  "type" varchar(64) COLLATE "pg_catalog"."default",
  "model_id" varchar(20) COLLATE "pg_catalog"."default",
  "workshop_code" varchar(50) COLLATE "pg_catalog"."default",
  "enable_date" timestamp(6),
  "health_template_id" varchar(64) COLLATE "pg_catalog"."default"
)
;
COMMENT ON COLUMN "public"."t_equip"."name" IS '设备名';
COMMENT ON COLUMN "public"."t_equip"."self_code" IS '设备编号';
COMMENT ON COLUMN "public"."t_equip"."type" IS '设备类别';
COMMENT ON COLUMN "public"."t_equip"."model_id" IS '所属产品型号ID（t_product_model.id）';
COMMENT ON COLUMN "public"."t_equip"."workshop_code" IS '场景编号';
COMMENT ON COLUMN "public"."t_equip"."enable_date" IS '启用日期，用于健康分使用年限计算';
COMMENT ON COLUMN "public"."t_equip"."health_template_id" IS '关联健康规则模板ID';
COMMENT ON TABLE "public"."t_equip" IS '设备表';

-- ----------------------------
-- Table structure for t_equip_collect
-- ----------------------------
DROP TABLE IF EXISTS "public"."t_equip_collect";
CREATE TABLE "public"."t_equip_collect" (
  "id" varchar(20) COLLATE "pg_catalog"."default" NOT NULL,
  "revision" int4,
  "created_by" varchar(20) COLLATE "pg_catalog"."default",
  "created_id" varchar(20) COLLATE "pg_catalog"."default",
  "created_time" timestamp(6),
  "updated_by" varchar(20) COLLATE "pg_catalog"."default",
  "updated_id" varchar(20) COLLATE "pg_catalog"."default",
  "updated_time" timestamp(6),
  "tenant_id" varchar(20) COLLATE "pg_catalog"."default",
  "sn" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
  "data" jsonb,
  "time" timestamp(6) NOT NULL
)
;
COMMENT ON COLUMN "public"."t_equip_collect"."sn" IS '设备sn号';
COMMENT ON COLUMN "public"."t_equip_collect"."data" IS '采集的数据';
COMMENT ON COLUMN "public"."t_equip_collect"."time" IS '采集时间';

-- ----------------------------
-- Table structure for t_equip_health_indicator
-- ----------------------------
DROP TABLE IF EXISTS "public"."t_equip_health_indicator";
CREATE TABLE "public"."t_equip_health_indicator" (
  "id" varchar(32) COLLATE "pg_catalog"."default" NOT NULL,
  "equip_id" varchar(32) COLLATE "pg_catalog"."default",
  "sn" varchar(64) COLLATE "pg_catalog"."default" NOT NULL,
  "stat_time" timestamp(6) NOT NULL,
  "period_start" timestamp(6),
  "period_end" timestamp(6),
  "score" int4,
  "health_level" int4,
  "alarm_count" int4,
  "alarm_duration_minutes" int8,
  "run_duration_minutes" int8,
  "online_duration_minutes" int8,
  "period_minutes" int8,
  "template_id" varchar(32) COLLATE "pg_catalog"."default",
  "revision" int4,
  "created_by" varchar(20) COLLATE "pg_catalog"."default",
  "created_id" varchar(20) COLLATE "pg_catalog"."default",
  "created_time" timestamp(6),
  "updated_by" varchar(20) COLLATE "pg_catalog"."default",
  "updated_id" varchar(20) COLLATE "pg_catalog"."default",
  "updated_time" timestamp(6),
  "tenant_id" varchar(20) COLLATE "pg_catalog"."default"
)
;
COMMENT ON COLUMN "public"."t_equip_health_indicator"."id" IS '主键';
COMMENT ON COLUMN "public"."t_equip_health_indicator"."equip_id" IS '设备ID';
COMMENT ON COLUMN "public"."t_equip_health_indicator"."sn" IS '设备自编码';
COMMENT ON COLUMN "public"."t_equip_health_indicator"."stat_time" IS '统计时间点';
COMMENT ON COLUMN "public"."t_equip_health_indicator"."period_start" IS '周期开始';
COMMENT ON COLUMN "public"."t_equip_health_indicator"."period_end" IS '周期结束';
COMMENT ON COLUMN "public"."t_equip_health_indicator"."score" IS '健康得分0-100';
COMMENT ON COLUMN "public"."t_equip_health_indicator"."health_level" IS '健康等级0=健康,1=关注,2=预警,3=故障';
COMMENT ON COLUMN "public"."t_equip_health_indicator"."alarm_count" IS '报警次数';
COMMENT ON COLUMN "public"."t_equip_health_indicator"."alarm_duration_minutes" IS '报警总时长(分钟)';
COMMENT ON COLUMN "public"."t_equip_health_indicator"."run_duration_minutes" IS '运行总时长(分钟)';
COMMENT ON COLUMN "public"."t_equip_health_indicator"."online_duration_minutes" IS '在线总时长(分钟)';
COMMENT ON COLUMN "public"."t_equip_health_indicator"."period_minutes" IS '统计周期(分钟)';
COMMENT ON COLUMN "public"."t_equip_health_indicator"."template_id" IS '规则模板ID';
COMMENT ON TABLE "public"."t_equip_health_indicator" IS '设备健康指标';

-- ----------------------------
-- Table structure for t_equip_health_rule_template
-- ----------------------------
DROP TABLE IF EXISTS "public"."t_equip_health_rule_template";
CREATE TABLE "public"."t_equip_health_rule_template" (
  "id" varchar(32) COLLATE "pg_catalog"."default" NOT NULL,
  "name" varchar(128) COLLATE "pg_catalog"."default",
  "revision" int4,
  "created_by" varchar(20) COLLATE "pg_catalog"."default",
  "created_id" varchar(20) COLLATE "pg_catalog"."default",
  "created_time" timestamp(6),
  "updated_by" varchar(20) COLLATE "pg_catalog"."default",
  "updated_id" varchar(20) COLLATE "pg_catalog"."default",
  "updated_time" timestamp(6),
  "tenant_id" varchar(20) COLLATE "pg_catalog"."default",
  "config" text COLLATE "pg_catalog"."default"
)
;
COMMENT ON COLUMN "public"."t_equip_health_rule_template"."id" IS '主键';
COMMENT ON COLUMN "public"."t_equip_health_rule_template"."name" IS '模板名称';
COMMENT ON COLUMN "public"."t_equip_health_rule_template"."config" IS '规则配置JSON';
COMMENT ON TABLE "public"."t_equip_health_rule_template" IS '设备健康规则模板';

-- ----------------------------
-- Table structure for t_equip_realtime
-- ----------------------------
DROP TABLE IF EXISTS "public"."t_equip_realtime";
CREATE TABLE "public"."t_equip_realtime" (
  "id" varchar(64) COLLATE "pg_catalog"."default" NOT NULL,
  "tenant_id" varchar(64) COLLATE "pg_catalog"."default" NOT NULL,
  "self_code" varchar(128) COLLATE "pg_catalog"."default" NOT NULL,
  "gw_id" varchar(64) COLLATE "pg_catalog"."default",
  "online_state" int4 NOT NULL DEFAULT 0,
  "run_state" int4 NOT NULL DEFAULT 0,
  "alarm_state" int4 NOT NULL DEFAULT 0,
  "online_change_time" timestamp(6),
  "run_change_time" timestamp(6),
  "alarm_change_time" timestamp(6),
  "workshop_code" varchar(128) COLLATE "pg_catalog"."default",
  "name" varchar(256) COLLATE "pg_catalog"."default",
  "equip_time" timestamp(6),
  "alarm_level" int4,
  "payload" jsonb NOT NULL,
  "created_by" varchar(20) COLLATE "pg_catalog"."default",
  "created_id" varchar(20) COLLATE "pg_catalog"."default",
  "created_time" timestamp(6),
  "updated_by" varchar(20) COLLATE "pg_catalog"."default",
  "updated_id" varchar(20) COLLATE "pg_catalog"."default",
  "updated_time" timestamp(6),
  "revision" int4 DEFAULT 0
)
;

-- ----------------------------
-- Table structure for t_equip_record_alarm
-- ----------------------------
DROP TABLE IF EXISTS "public"."t_equip_record_alarm";
CREATE TABLE "public"."t_equip_record_alarm" (
  "id" varchar(20) COLLATE "pg_catalog"."default" NOT NULL,
  "revision" int4,
  "created_by" varchar(20) COLLATE "pg_catalog"."default",
  "created_id" varchar(20) COLLATE "pg_catalog"."default",
  "created_time" timestamp(6),
  "updated_by" varchar(20) COLLATE "pg_catalog"."default",
  "updated_id" varchar(20) COLLATE "pg_catalog"."default",
  "updated_time" timestamp(6),
  "tenant_id" varchar(20) COLLATE "pg_catalog"."default",
  "sn" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
  "start_time" timestamp(6) NOT NULL,
  "end_time" timestamp(6),
  "state" int2 NOT NULL,
  "reason" varchar(255) COLLATE "pg_catalog"."default",
  "level" int2,
  "event_id" varchar(60) COLLATE "pg_catalog"."default"
)
;
COMMENT ON COLUMN "public"."t_equip_record_alarm"."sn" IS '设备sn号';
COMMENT ON COLUMN "public"."t_equip_record_alarm"."start_time" IS '持续开始时间';
COMMENT ON COLUMN "public"."t_equip_record_alarm"."end_time" IS '持续结束时间';
COMMENT ON COLUMN "public"."t_equip_record_alarm"."state" IS '状态';
COMMENT ON COLUMN "public"."t_equip_record_alarm"."reason" IS '报警原因';
COMMENT ON COLUMN "public"."t_equip_record_alarm"."level" IS '报警等级: 0=轻微, 1=一般, 2=严重, 3=故障';
COMMENT ON COLUMN "public"."t_equip_record_alarm"."event_id" IS '事件id';
COMMENT ON TABLE "public"."t_equip_record_alarm" IS '设备报警状态记录';

-- ----------------------------
-- Table structure for t_equip_record_online
-- ----------------------------
DROP TABLE IF EXISTS "public"."t_equip_record_online";
CREATE TABLE "public"."t_equip_record_online" (
  "id" varchar(20) COLLATE "pg_catalog"."default" NOT NULL,
  "revision" int4,
  "created_by" varchar(20) COLLATE "pg_catalog"."default",
  "created_id" varchar(20) COLLATE "pg_catalog"."default",
  "created_time" timestamp(6),
  "updated_by" varchar(20) COLLATE "pg_catalog"."default",
  "updated_id" varchar(20) COLLATE "pg_catalog"."default",
  "updated_time" timestamp(6),
  "tenant_id" varchar(20) COLLATE "pg_catalog"."default",
  "sn" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
  "start_time" timestamp(6) NOT NULL,
  "end_time" timestamp(6),
  "state" int2 NOT NULL,
  "event_id" varchar(60) COLLATE "pg_catalog"."default"
)
;
COMMENT ON COLUMN "public"."t_equip_record_online"."sn" IS '设备sn号';
COMMENT ON COLUMN "public"."t_equip_record_online"."start_time" IS '持续开始时间';
COMMENT ON COLUMN "public"."t_equip_record_online"."end_time" IS '持续结束时间';
COMMENT ON COLUMN "public"."t_equip_record_online"."state" IS '状态';
COMMENT ON COLUMN "public"."t_equip_record_online"."event_id" IS '事件id';
COMMENT ON TABLE "public"."t_equip_record_online" IS '设备在线记录';

-- ----------------------------
-- Table structure for t_equip_record_run
-- ----------------------------
DROP TABLE IF EXISTS "public"."t_equip_record_run";
CREATE TABLE "public"."t_equip_record_run" (
  "id" varchar(20) COLLATE "pg_catalog"."default" NOT NULL,
  "revision" int4,
  "created_by" varchar(20) COLLATE "pg_catalog"."default",
  "created_id" varchar(20) COLLATE "pg_catalog"."default",
  "created_time" timestamp(6),
  "updated_by" varchar(20) COLLATE "pg_catalog"."default",
  "updated_id" varchar(20) COLLATE "pg_catalog"."default",
  "updated_time" timestamp(6),
  "tenant_id" varchar(20) COLLATE "pg_catalog"."default",
  "sn" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
  "start_time" timestamp(6) NOT NULL,
  "end_time" timestamp(6),
  "power_start" numeric(10,3),
  "power_end" numeric(10,3),
  "state" int2 NOT NULL,
  "event_id" varchar(60) COLLATE "pg_catalog"."default"
)
;
COMMENT ON COLUMN "public"."t_equip_record_run"."sn" IS '设备sn号';
COMMENT ON COLUMN "public"."t_equip_record_run"."start_time" IS '运行开始时间';
COMMENT ON COLUMN "public"."t_equip_record_run"."end_time" IS '运行结束时间';
COMMENT ON COLUMN "public"."t_equip_record_run"."power_start" IS '开始时电量';
COMMENT ON COLUMN "public"."t_equip_record_run"."power_end" IS '结束时电量';
COMMENT ON COLUMN "public"."t_equip_record_run"."state" IS '运行状态';
COMMENT ON COLUMN "public"."t_equip_record_run"."event_id" IS '事件id';
COMMENT ON TABLE "public"."t_equip_record_run" IS '设备运行表';

-- ----------------------------
-- Table structure for t_equip_state_snapshot
-- ----------------------------
DROP TABLE IF EXISTS "public"."t_equip_state_snapshot";
CREATE TABLE "public"."t_equip_state_snapshot" (
  "id" varchar(20) COLLATE "pg_catalog"."default" NOT NULL,
  "revision" int4,
  "created_by" varchar(20) COLLATE "pg_catalog"."default",
  "created_id" varchar(20) COLLATE "pg_catalog"."default",
  "created_time" timestamp(6),
  "updated_by" varchar(20) COLLATE "pg_catalog"."default",
  "updated_id" varchar(20) COLLATE "pg_catalog"."default",
  "updated_time" timestamp(6),
  "tenant_id" varchar(20) COLLATE "pg_catalog"."default",
  "sn" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
  "time" timestamp(6) NOT NULL,
  "run_state" int2,
  "alarm_state" int2,
  "online_state" int2
)
;
COMMENT ON COLUMN "public"."t_equip_state_snapshot"."sn" IS '设备sn号';
COMMENT ON COLUMN "public"."t_equip_state_snapshot"."time" IS '记录时间';
COMMENT ON COLUMN "public"."t_equip_state_snapshot"."run_state" IS '运行状态';
COMMENT ON COLUMN "public"."t_equip_state_snapshot"."alarm_state" IS '报警状态';
COMMENT ON COLUMN "public"."t_equip_state_snapshot"."online_state" IS '在线状态';
COMMENT ON TABLE "public"."t_equip_state_snapshot" IS '设备状态快照';

-- ----------------------------
-- Table structure for t_system_config
-- ----------------------------
DROP TABLE IF EXISTS "public"."t_system_config";
CREATE TABLE "public"."t_system_config" (
  "id" varchar(20) COLLATE "pg_catalog"."default" NOT NULL,
  "revision" int4,
  "created_by" varchar(64) COLLATE "pg_catalog"."default",
  "created_id" varchar(20) COLLATE "pg_catalog"."default",
  "created_time" timestamp(6),
  "updated_by" varchar(64) COLLATE "pg_catalog"."default",
  "updated_id" varchar(20) COLLATE "pg_catalog"."default",
  "updated_time" timestamp(6),
  "config_key" varchar(64) COLLATE "pg_catalog"."default" NOT NULL,
  "config" text COLLATE "pg_catalog"."default",
  CONSTRAINT "t_system_config_pkey" PRIMARY KEY ("id"),
  CONSTRAINT "uk_t_system_config_key" UNIQUE ("config_key")
)
;
COMMENT ON COLUMN "public"."t_system_config"."config_key" IS '配置键，如 app';
COMMENT ON COLUMN "public"."t_system_config"."config" IS '配置内容 JSON';
COMMENT ON TABLE "public"."t_system_config" IS '系统配置';

-- ----------------------------
-- Table structure for t_gateway
-- ----------------------------
DROP TABLE IF EXISTS "public"."t_gateway";
CREATE TABLE "public"."t_gateway" (
  "id" varchar(20) COLLATE "pg_catalog"."default" NOT NULL,
  "revision" int4,
  "created_by" varchar(20) COLLATE "pg_catalog"."default",
  "created_id" varchar(20) COLLATE "pg_catalog"."default",
  "created_time" timestamp(6),
  "updated_by" varchar(20) COLLATE "pg_catalog"."default",
  "updated_id" varchar(20) COLLATE "pg_catalog"."default",
  "updated_time" timestamp(6),
  "tenant_id" varchar(20) COLLATE "pg_catalog"."default",
  "server_name" varchar(50) COLLATE "pg_catalog"."default",
  "uri" varchar(255) COLLATE "pg_catalog"."default",
  "topic" varchar(50) COLLATE "pg_catalog"."default",
  "params" varchar(255) COLLATE "pg_catalog"."default",
  "protocol" varchar(20) COLLATE "pg_catalog"."default",
  "enabled" bool,
  "username" varchar(150) COLLATE "pg_catalog"."default",
  "password" varchar(150) COLLATE "pg_catalog"."default",
  "valid_type" int2,
  "collect_cron" varchar(64) COLLATE "pg_catalog"."default"
)
;
COMMENT ON COLUMN "public"."t_gateway"."server_name" IS '服务名';
COMMENT ON COLUMN "public"."t_gateway"."uri" IS '地址';
COMMENT ON COLUMN "public"."t_gateway"."topic" IS '主题';
COMMENT ON COLUMN "public"."t_gateway"."params" IS '参数';
COMMENT ON COLUMN "public"."t_gateway"."protocol" IS '协议类型';
COMMENT ON COLUMN "public"."t_gateway"."enabled" IS '是否启用';
COMMENT ON COLUMN "public"."t_gateway"."valid_type" IS '认证类型（0:无认证， 1:Baisc）';
COMMENT ON COLUMN "public"."t_gateway"."collect_cron" IS '采集cron表达式';
COMMENT ON TABLE "public"."t_gateway" IS '网关';

-- ----------------------------
-- Table structure for t_inspect_item
-- ----------------------------
DROP TABLE IF EXISTS "public"."t_inspect_item";
CREATE TABLE "public"."t_inspect_item" (
  "id" varchar(64) COLLATE "pg_catalog"."default" NOT NULL,
  "item_name" varchar(128) COLLATE "pg_catalog"."default",
  "item_type" int4,
  "unit" varchar(128) COLLATE "pg_catalog"."default",
  "tenant_id" varchar(64) COLLATE "pg_catalog"."default",
  "created_by" varchar(64) COLLATE "pg_catalog"."default",
  "created_id" varchar(64) COLLATE "pg_catalog"."default",
  "created_time" timestamp(6),
  "updated_by" varchar(64) COLLATE "pg_catalog"."default",
  "updated_id" varchar(64) COLLATE "pg_catalog"."default",
  "updated_time" timestamp(6),
  "revision" int4,
  "min_value" float8,
  "max_value" float8,
  "required_flag" bool
)
;
COMMENT ON COLUMN "public"."t_inspect_item"."item_name" IS '巡检项名称';
COMMENT ON COLUMN "public"."t_inspect_item"."item_type" IS '类型：1选择 2数值 3是否';
COMMENT ON COLUMN "public"."t_inspect_item"."unit" IS '单位或选项：数值填A/MPa等；选择可填 正常/不足、松动/正常';
COMMENT ON COLUMN "public"."t_inspect_item"."min_value" IS '最小值';
COMMENT ON COLUMN "public"."t_inspect_item"."max_value" IS '最大值';
COMMENT ON COLUMN "public"."t_inspect_item"."required_flag" IS '是否必填';

-- ----------------------------
-- Table structure for t_inspect_person
-- ----------------------------
DROP TABLE IF EXISTS "public"."t_inspect_person";
CREATE TABLE "public"."t_inspect_person" (
  "id" varchar(64) COLLATE "pg_catalog"."default" NOT NULL,
  "name" varchar(64) COLLATE "pg_catalog"."default",
  "account_id" varchar(64) COLLATE "pg_catalog"."default",
  "account_name" varchar(128) COLLATE "pg_catalog"."default",
  "mobile" varchar(32) COLLATE "pg_catalog"."default",
  "job_number" varchar(64) COLLATE "pg_catalog"."default",
  "remark" varchar(512) COLLATE "pg_catalog"."default",
  "tenant_id" varchar(64) COLLATE "pg_catalog"."default",
  "created_by" varchar(64) COLLATE "pg_catalog"."default",
  "created_id" varchar(64) COLLATE "pg_catalog"."default",
  "created_time" timestamp(6),
  "updated_by" varchar(64) COLLATE "pg_catalog"."default",
  "updated_id" varchar(64) COLLATE "pg_catalog"."default",
  "updated_time" timestamp(6),
  "revision" int4
)
;
COMMENT ON COLUMN "public"."t_inspect_person"."name" IS '姓名';
COMMENT ON COLUMN "public"."t_inspect_person"."account_id" IS '关联账户ID';
COMMENT ON COLUMN "public"."t_inspect_person"."account_name" IS '关联账户名称';
COMMENT ON COLUMN "public"."t_inspect_person"."mobile" IS '手机号';
COMMENT ON COLUMN "public"."t_inspect_person"."job_number" IS '工号';
COMMENT ON COLUMN "public"."t_inspect_person"."remark" IS '备注';

-- ----------------------------
-- Table structure for t_inspect_plan
-- ----------------------------
DROP TABLE IF EXISTS "public"."t_inspect_plan";
CREATE TABLE "public"."t_inspect_plan" (
  "id" varchar(64) COLLATE "pg_catalog"."default" NOT NULL,
  "name" varchar(128) COLLATE "pg_catalog"."default" NOT NULL,
  "cycle_type" int4,
  "cycle_config" varchar(128) COLLATE "pg_catalog"."default",
  "workshop_code" text COLLATE "pg_catalog"."default",
  "equip_ids" text COLLATE "pg_catalog"."default",
  "status" int4,
  "remark" varchar(512) COLLATE "pg_catalog"."default",
  "revision" int4,
  "created_by" varchar(20) COLLATE "pg_catalog"."default",
  "created_id" varchar(20) COLLATE "pg_catalog"."default",
  "created_time" timestamp(6),
  "updated_by" varchar(20) COLLATE "pg_catalog"."default",
  "updated_id" varchar(20) COLLATE "pg_catalog"."default",
  "updated_time" timestamp(6),
  "tenant_id" varchar(20) COLLATE "pg_catalog"."default",
  "template_id" varchar(64) COLLATE "pg_catalog"."default"
)
;
COMMENT ON COLUMN "public"."t_inspect_plan"."id" IS '主键';
COMMENT ON COLUMN "public"."t_inspect_plan"."name" IS '计划名称';
COMMENT ON COLUMN "public"."t_inspect_plan"."cycle_type" IS '周期类型：1每日 2每周 3每月';
COMMENT ON COLUMN "public"."t_inspect_plan"."cycle_config" IS '周期配置';
COMMENT ON COLUMN "public"."t_inspect_plan"."workshop_code" IS '场景编号';
COMMENT ON COLUMN "public"."t_inspect_plan"."equip_ids" IS '关联设备ID列表';
COMMENT ON COLUMN "public"."t_inspect_plan"."status" IS '状态：0禁用 1启用';
COMMENT ON COLUMN "public"."t_inspect_plan"."remark" IS '备注';
COMMENT ON COLUMN "public"."t_inspect_plan"."template_id" IS '关联巡检模板ID';
COMMENT ON TABLE "public"."t_inspect_plan" IS '巡检计划';

-- ----------------------------
-- Table structure for t_inspect_project
-- ----------------------------
DROP TABLE IF EXISTS "public"."t_inspect_project";
CREATE TABLE "public"."t_inspect_project" (
  "id" varchar(20) COLLATE "pg_catalog"."default" NOT NULL,
  "name" varchar(128) COLLATE "pg_catalog"."default",
  "code" varchar(64) COLLATE "pg_catalog"."default",
  "status" int4,
  "remark" varchar(512) COLLATE "pg_catalog"."default",
  "tenant_id" varchar(64) COLLATE "pg_catalog"."default",
  "created_by" varchar(64) COLLATE "pg_catalog"."default",
  "created_id" varchar(64) COLLATE "pg_catalog"."default",
  "created_time" timestamp(6),
  "updated_by" varchar(64) COLLATE "pg_catalog"."default",
  "updated_id" varchar(64) COLLATE "pg_catalog"."default",
  "updated_time" timestamp(6),
  "revision" int4
)
;
COMMENT ON COLUMN "public"."t_inspect_project"."name" IS '项目名称';
COMMENT ON COLUMN "public"."t_inspect_project"."code" IS '项目编号';
COMMENT ON COLUMN "public"."t_inspect_project"."status" IS '状态：0禁用 1启用';
COMMENT ON COLUMN "public"."t_inspect_project"."remark" IS '备注';

-- ----------------------------
-- Table structure for t_inspect_record
-- ----------------------------
DROP TABLE IF EXISTS "public"."t_inspect_record";
CREATE TABLE "public"."t_inspect_record" (
  "id" varchar(64) COLLATE "pg_catalog"."default" NOT NULL,
  "task_id" varchar(64) COLLATE "pg_catalog"."default" NOT NULL,
  "equip_id" varchar(64) COLLATE "pg_catalog"."default",
  "equip_name" varchar(128) COLLATE "pg_catalog"."default",
  "score" int4,
  "item_name" varchar(128) COLLATE "pg_catalog"."default",
  "result" int4,
  "remark" varchar(512) COLLATE "pg_catalog"."default",
  "photo_urls" varchar(1024) COLLATE "pg_catalog"."default",
  "record_time" timestamp(6),
  "revision" int4,
  "created_by" varchar(20) COLLATE "pg_catalog"."default",
  "created_id" varchar(20) COLLATE "pg_catalog"."default",
  "created_time" timestamp(6),
  "updated_by" varchar(20) COLLATE "pg_catalog"."default",
  "updated_id" varchar(20) COLLATE "pg_catalog"."default",
  "updated_time" timestamp(6),
  "tenant_id" varchar(20) COLLATE "pg_catalog"."default"
)
;
COMMENT ON COLUMN "public"."t_inspect_record"."id" IS '主键';
COMMENT ON COLUMN "public"."t_inspect_record"."task_id" IS '巡检任务ID';
COMMENT ON COLUMN "public"."t_inspect_record"."equip_id" IS '设备ID';
COMMENT ON COLUMN "public"."t_inspect_record"."equip_name" IS '设备编号/名称';
COMMENT ON COLUMN "public"."t_inspect_record"."score" IS '巡检分值';
COMMENT ON COLUMN "public"."t_inspect_record"."item_name" IS '巡检项名称';
COMMENT ON COLUMN "public"."t_inspect_record"."result" IS '巡检结果：0正常 1异常';
COMMENT ON COLUMN "public"."t_inspect_record"."remark" IS '备注';
COMMENT ON COLUMN "public"."t_inspect_record"."photo_urls" IS '照片URL，多张逗号分隔';
COMMENT ON COLUMN "public"."t_inspect_record"."record_time" IS '记录时间';
COMMENT ON TABLE "public"."t_inspect_record" IS '巡检记录';

-- ----------------------------
-- Table structure for t_inspect_record_item
-- ----------------------------
DROP TABLE IF EXISTS "public"."t_inspect_record_item";
CREATE TABLE "public"."t_inspect_record_item" (
  "id" varchar(64) COLLATE "pg_catalog"."default" NOT NULL,
  "record_id" varchar(64) COLLATE "pg_catalog"."default" NOT NULL,
  "item_id" varchar(64) COLLATE "pg_catalog"."default",
  "item_name" varchar(256) COLLATE "pg_catalog"."default",
  "content" varchar(512) COLLATE "pg_catalog"."default",
  "result" int2,
  "rule_score" int4,
  "score" int4,
  "remark" varchar(512) COLLATE "pg_catalog"."default",
  "photo_urls" varchar(2000) COLLATE "pg_catalog"."default",
  "tenant_id" varchar(64) COLLATE "pg_catalog"."default",
  "created_by" varchar(64) COLLATE "pg_catalog"."default",
  "created_id" varchar(64) COLLATE "pg_catalog"."default",
  "created_time" timestamp(6),
  "updated_by" varchar(64) COLLATE "pg_catalog"."default",
  "updated_id" varchar(64) COLLATE "pg_catalog"."default",
  "updated_time" timestamp(6),
  "revision" int4
)
;
COMMENT ON COLUMN "public"."t_inspect_record_item"."record_id" IS '巡检记录ID（主表）';
COMMENT ON COLUMN "public"."t_inspect_record_item"."item_id" IS '巡检项ID（模板项主键）';
COMMENT ON COLUMN "public"."t_inspect_record_item"."item_name" IS '巡检项名称';
COMMENT ON COLUMN "public"."t_inspect_record_item"."content" IS '巡检项内容：填写值/结果文本';
COMMENT ON COLUMN "public"."t_inspect_record_item"."result" IS '巡检结果：0正常 1异常';
COMMENT ON COLUMN "public"."t_inspect_record_item"."rule_score" IS '规则分值（配置项匹配得分，未乘权重）';
COMMENT ON COLUMN "public"."t_inspect_record_item"."score" IS '该项得分（按权重与配置规则计算）';
COMMENT ON COLUMN "public"."t_inspect_record_item"."remark" IS '备注';
COMMENT ON COLUMN "public"."t_inspect_record_item"."photo_urls" IS '照片URL，多张逗号分隔';
COMMENT ON TABLE "public"."t_inspect_record_item" IS '巡检记录明细（附表）';

-- ----------------------------
-- Table structure for t_inspect_task
-- ----------------------------
DROP TABLE IF EXISTS "public"."t_inspect_task";
CREATE TABLE "public"."t_inspect_task" (
  "id" varchar(64) COLLATE "pg_catalog"."default" NOT NULL,
  "plan_id" varchar(64) COLLATE "pg_catalog"."default" NOT NULL,
  "scheduled_time" timestamp(6),
  "status" int4,
  "executor_id" varchar(64) COLLATE "pg_catalog"."default",
  "executor_name" varchar(64) COLLATE "pg_catalog"."default",
  "actual_start_time" timestamp(6),
  "actual_end_time" timestamp(6),
  "remark" varchar(512) COLLATE "pg_catalog"."default",
  "revision" int4,
  "created_by" varchar(20) COLLATE "pg_catalog"."default",
  "created_id" varchar(20) COLLATE "pg_catalog"."default",
  "created_time" timestamp(6),
  "updated_by" varchar(20) COLLATE "pg_catalog"."default",
  "updated_id" varchar(20) COLLATE "pg_catalog"."default",
  "updated_time" timestamp(6),
  "tenant_id" varchar(20) COLLATE "pg_catalog"."default",
  "workshop_code" varchar(40) COLLATE "pg_catalog"."default",
  "executor_person_id" varchar(64) COLLATE "pg_catalog"."default",
  "template_id" varchar(64) COLLATE "pg_catalog"."default"
)
;
COMMENT ON COLUMN "public"."t_inspect_task"."id" IS '主键';
COMMENT ON COLUMN "public"."t_inspect_task"."plan_id" IS '巡检计划ID';
COMMENT ON COLUMN "public"."t_inspect_task"."scheduled_time" IS '计划执行时间';
COMMENT ON COLUMN "public"."t_inspect_task"."status" IS '状态：0待执行 1执行中 2已完成 3已逾期';
COMMENT ON COLUMN "public"."t_inspect_task"."executor_id" IS '执行人ID';
COMMENT ON COLUMN "public"."t_inspect_task"."executor_name" IS '执行人姓名';
COMMENT ON COLUMN "public"."t_inspect_task"."actual_start_time" IS '实际开始时间';
COMMENT ON COLUMN "public"."t_inspect_task"."actual_end_time" IS '实际完成时间';
COMMENT ON COLUMN "public"."t_inspect_task"."remark" IS '备注';
COMMENT ON COLUMN "public"."t_inspect_task"."workshop_code" IS '关联设备ID列表';
COMMENT ON COLUMN "public"."t_inspect_task"."executor_person_id" IS '指派的巡检人员ID';
COMMENT ON COLUMN "public"."t_inspect_task"."template_id" IS '关联巡检模板ID（来自计划）';
COMMENT ON TABLE "public"."t_inspect_task" IS '巡检任务';

-- ----------------------------
-- Table structure for t_inspect_template
-- ----------------------------
DROP TABLE IF EXISTS "public"."t_inspect_template";
CREATE TABLE "public"."t_inspect_template" (
  "id" varchar(64) COLLATE "pg_catalog"."default" NOT NULL,
  "name" varchar(128) COLLATE "pg_catalog"."default",
  "remark" varchar(512) COLLATE "pg_catalog"."default",
  "tenant_id" varchar(64) COLLATE "pg_catalog"."default",
  "created_by" varchar(64) COLLATE "pg_catalog"."default",
  "created_id" varchar(64) COLLATE "pg_catalog"."default",
  "created_time" timestamp(6),
  "updated_by" varchar(64) COLLATE "pg_catalog"."default",
  "updated_id" varchar(64) COLLATE "pg_catalog"."default",
  "updated_time" timestamp(6),
  "revision" int4
)
;
COMMENT ON COLUMN "public"."t_inspect_template"."name" IS '模板名称';
COMMENT ON COLUMN "public"."t_inspect_template"."remark" IS '备注';

-- ----------------------------
-- Table structure for t_inspect_template_item
-- ----------------------------
DROP TABLE IF EXISTS "public"."t_inspect_template_item";
CREATE TABLE "public"."t_inspect_template_item" (
  "id" varchar(64) COLLATE "pg_catalog"."default" NOT NULL,
  "template_id" varchar(64) COLLATE "pg_catalog"."default" NOT NULL,
  "product_code" varchar(64) COLLATE "pg_catalog"."default",
  "reference_item_id" varchar(64) COLLATE "pg_catalog"."default",
  "sort_order" int4,
  "weight" int4,
  "weight_rate" numeric(10,6),
  "rule_config" text COLLATE "pg_catalog"."default",
  "created_time" timestamp(6),
  "updated_time" timestamp(6),
  "tenant_id" varchar(64) COLLATE "pg_catalog"."default",
  "revision" int4,
  "created_by" varchar(64) COLLATE "pg_catalog"."default",
  "created_id" varchar(64) COLLATE "pg_catalog"."default",
  "updated_by" varchar(64) COLLATE "pg_catalog"."default",
  "updated_id" varchar(64) COLLATE "pg_catalog"."default"
)
;
COMMENT ON COLUMN "public"."t_inspect_template_item"."id" IS '主键';
COMMENT ON COLUMN "public"."t_inspect_template_item"."template_id" IS '所属模板ID';
COMMENT ON COLUMN "public"."t_inspect_template_item"."product_code" IS '所属产品编号（模板中的产品块）';
COMMENT ON COLUMN "public"."t_inspect_template_item"."reference_item_id" IS '引用的巡检项ID（从巡检项池载入时绑定）';
COMMENT ON COLUMN "public"."t_inspect_template_item"."sort_order" IS '排序号';
COMMENT ON COLUMN "public"."t_inspect_template_item"."weight" IS '权重（模板内该项的权重）';
COMMENT ON COLUMN "public"."t_inspect_template_item"."weight_rate" IS '百分比权重（该项权重/同产品块权重总值，如0.3）';
COMMENT ON COLUMN "public"."t_inspect_template_item"."rule_config" IS '规则配置JSON，如数值区间/是否/选项与分值';
COMMENT ON COLUMN "public"."t_inspect_template_item"."created_time" IS '创建时间';
COMMENT ON COLUMN "public"."t_inspect_template_item"."updated_time" IS '更新时间';
COMMENT ON COLUMN "public"."t_inspect_template_item"."tenant_id" IS '租户ID';
COMMENT ON TABLE "public"."t_inspect_template_item" IS '模板-巡检项关联表';

-- ----------------------------
-- Table structure for t_inspect_template_item_rule
-- ----------------------------
DROP TABLE IF EXISTS "public"."t_inspect_template_item_rule";
CREATE TABLE "public"."t_inspect_template_item_rule" (
  "id" varchar(64) COLLATE "pg_catalog"."default" NOT NULL,
  "template_item_id" varchar(64) COLLATE "pg_catalog"."default" NOT NULL,
  "rule_type" int4 NOT NULL,
  "bool_value" int2,
  "min_value" float8,
  "max_value" float8,
  "option_value" varchar(64) COLLATE "pg_catalog"."default",
  "weight" int4 NOT NULL,
  "sort_order" int4,
  "created_time" timestamp(6),
  "updated_time" timestamp(6),
  "tenant_id" varchar(64) COLLATE "pg_catalog"."default",
  "revision" int4
)
;
COMMENT ON COLUMN "public"."t_inspect_template_item_rule"."id" IS '主键';
COMMENT ON COLUMN "public"."t_inspect_template_item_rule"."template_item_id" IS '模板巡检项ID，关联 t_inspect_template_item.id';
COMMENT ON COLUMN "public"."t_inspect_template_item_rule"."rule_type" IS '规则类型：1=是否型，2=数值范围，3=选择值';
COMMENT ON COLUMN "public"."t_inspect_template_item_rule"."bool_value" IS '是否型：0=否 1=是（rule_type=1时有效）';
COMMENT ON COLUMN "public"."t_inspect_template_item_rule"."min_value" IS '数值型：最小值（含），rule_type=2';
COMMENT ON COLUMN "public"."t_inspect_template_item_rule"."max_value" IS '数值型：最大值（含），rule_type=2';
COMMENT ON COLUMN "public"."t_inspect_template_item_rule"."option_value" IS '选择型：选项值文本，rule_type=3';
COMMENT ON COLUMN "public"."t_inspect_template_item_rule"."weight" IS '该条件命中时的得分/权重';
COMMENT ON COLUMN "public"."t_inspect_template_item_rule"."sort_order" IS '排序（同一项多条规则时的优先级）';
COMMENT ON TABLE "public"."t_inspect_template_item_rule" IS '模板巡检项评分规则';

-- ----------------------------
-- Table structure for t_line
-- ----------------------------
DROP TABLE IF EXISTS "public"."t_line";
CREATE TABLE "public"."t_line" (
  "id" varchar(20) COLLATE "pg_catalog"."default" NOT NULL,
  "revision" int4,
  "created_by" varchar(20) COLLATE "pg_catalog"."default",
  "created_id" varchar(20) COLLATE "pg_catalog"."default",
  "created_time" timestamp(6),
  "updated_by" varchar(20) COLLATE "pg_catalog"."default",
  "updated_id" varchar(20) COLLATE "pg_catalog"."default",
  "updated_time" timestamp(6),
  "tenant_id" varchar(20) COLLATE "pg_catalog"."default",
  "self_code" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
  "name" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
  "product_code" varchar(64) COLLATE "pg_catalog"."default",
  "product_name" varchar(128) COLLATE "pg_catalog"."default",
  "material_code" varchar(64) COLLATE "pg_catalog"."default",
  "material_name" varchar(128) COLLATE "pg_catalog"."default",
  "version_no" varchar(32) COLLATE "pg_catalog"."default",
  "throughput" numeric(14,5),
  "step_interval" int4,
  "del_bit" int2 DEFAULT 0,
  "type" int2,
  "plc_time" timestamp(6),
  "sync_time" timestamp(6),
  "map_db" int4,
  "map_offset" varchar(100) COLLATE "pg_catalog"."default"
)
;
COMMENT ON COLUMN "public"."t_line"."self_code" IS '产线编号(可以与工艺设置进行映射)';
COMMENT ON COLUMN "public"."t_line"."name" IS '产线名称';
COMMENT ON COLUMN "public"."t_line"."product_code" IS '加工对象编号';
COMMENT ON COLUMN "public"."t_line"."product_name" IS '加工对象名称';
COMMENT ON COLUMN "public"."t_line"."material_code" IS '原材料编号';
COMMENT ON COLUMN "public"."t_line"."material_name" IS '原材料名称';
COMMENT ON COLUMN "public"."t_line"."version_no" IS '版本号';
COMMENT ON COLUMN "public"."t_line"."throughput" IS '吞吐量(单次生产总量)';
COMMENT ON COLUMN "public"."t_line"."step_interval" IS '步进值（单位秒）';
COMMENT ON COLUMN "public"."t_line"."type" IS '类型(0:固定映射 1:参数映射 2:流程映射)';
COMMENT ON COLUMN "public"."t_line"."plc_time" IS 'plc下载时间';
COMMENT ON COLUMN "public"."t_line"."sync_time" IS '同步时间';
COMMENT ON COLUMN "public"."t_line"."map_db" IS '映射db';
COMMENT ON COLUMN "public"."t_line"."map_offset" IS '映射偏移';
COMMENT ON TABLE "public"."t_line" IS '产线/工艺';

-- ----------------------------
-- Table structure for t_mat
-- ----------------------------
DROP TABLE IF EXISTS "public"."t_mat";
CREATE TABLE "public"."t_mat" (
  "id" varchar(20) COLLATE "pg_catalog"."default" NOT NULL,
  "revision" int4,
  "created_by" varchar(20) COLLATE "pg_catalog"."default",
  "created_id" varchar(20) COLLATE "pg_catalog"."default",
  "created_time" timestamp(6),
  "updated_by" varchar(20) COLLATE "pg_catalog"."default",
  "updated_id" varchar(20) COLLATE "pg_catalog"."default",
  "updated_time" timestamp(6),
  "self_code" varchar(50) COLLATE "pg_catalog"."default",
  "name" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
  "classify_code" varchar(50) COLLATE "pg_catalog"."default",
  "del_bit" int2 DEFAULT 0,
  CONSTRAINT "t_mat_pkey" PRIMARY KEY ("id")
)
;
COMMENT ON COLUMN "public"."t_mat"."self_code" IS '物料编号';
COMMENT ON COLUMN "public"."t_mat"."name" IS '物料名称';
COMMENT ON COLUMN "public"."t_mat"."classify_code" IS '所属分类编号';
COMMENT ON COLUMN "public"."t_mat"."del_bit" IS '逻辑删除';
COMMENT ON TABLE "public"."t_mat" IS '物料';

-- ----------------------------
-- Table structure for t_mo
-- ----------------------------
DROP TABLE IF EXISTS "public"."t_mo";
CREATE TABLE "public"."t_mo" (
  "id" varchar(20) COLLATE "pg_catalog"."default" NOT NULL,
  "revision" int4,
  "created_by" varchar(20) COLLATE "pg_catalog"."default",
  "created_id" varchar(20) COLLATE "pg_catalog"."default",
  "created_time" timestamp(6),
  "updated_by" varchar(20) COLLATE "pg_catalog"."default",
  "updated_id" varchar(20) COLLATE "pg_catalog"."default",
  "updated_time" timestamp(6),
  "tenant_id" varchar(20) COLLATE "pg_catalog"."default",
  "self_code" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
  "status" int4 DEFAULT 0,
  "product_id" varchar(20) COLLATE "pg_catalog"."default",
  "product_name" varchar(100) COLLATE "pg_catalog"."default",
  "product_code" varchar(50) COLLATE "pg_catalog"."default",
  "num" int4,
  "weight" numeric(25,5),
  "line_code" varchar(50) COLLATE "pg_catalog"."default",
  "surplus" int4,
  "exec_time" timestamp(6),
  "del_bit" int2 DEFAULT 0,
  "source" int4,
  "source_id" varchar(64) COLLATE "pg_catalog"."default",
  PRIMARY KEY ("id")
)
;
CREATE UNIQUE INDEX "uk_t_mo_self_code" ON "public"."t_mo" USING btree ("self_code");
COMMENT ON COLUMN "public"."t_mo"."status" IS '0INIT 1PART 2RUN 3COMPLETE 4CANCEL';
COMMENT ON TABLE "public"."t_mo" IS 'manufacturing order';

-- ----------------------------
-- Table structure for t_mps
-- ----------------------------
DROP TABLE IF EXISTS "public"."t_mps";
CREATE TABLE "public"."t_mps" (
  "id" varchar(20) COLLATE "pg_catalog"."default" NOT NULL,
  "revision" int4,
  "created_by" varchar(20) COLLATE "pg_catalog"."default",
  "created_id" varchar(20) COLLATE "pg_catalog"."default",
  "created_time" timestamp(6),
  "updated_by" varchar(20) COLLATE "pg_catalog"."default",
  "updated_id" varchar(20) COLLATE "pg_catalog"."default",
  "updated_time" timestamp(6),
  "mo_code" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
  "sequence" int4,
  "line" varchar(50) COLLATE "pg_catalog"."default",
  "exec_time" timestamp(6),
  "num" int4,
  "weight" numeric(25,5),
  "batch" int4,
  "status" int4 DEFAULT 0,
  "priority" numeric(25,5),
  "del_bit" int2 DEFAULT 0,
  PRIMARY KEY ("id")
)
;
CREATE INDEX "idx_t_mps_mo_code" ON "public"."t_mps" USING btree ("mo_code");
CREATE INDEX "idx_t_mps_status" ON "public"."t_mps" USING btree ("status");
COMMENT ON COLUMN "public"."t_mps"."status" IS '0WAIT_QUE 1WAIT_EXEC 2EXECING 3COMPLETE 4FILE 5CANCEL';
COMMENT ON TABLE "public"."t_mps" IS 'mps batch plan';

-- ----------------------------
-- Table structure for t_mo_adjust_log
-- ----------------------------
DROP TABLE IF EXISTS "public"."t_mo_adjust_log";
CREATE TABLE "public"."t_mo_adjust_log" (
  "id" varchar(20) COLLATE "pg_catalog"."default" NOT NULL,
  "revision" int4,
  "created_by" varchar(20) COLLATE "pg_catalog"."default",
  "created_id" varchar(20) COLLATE "pg_catalog"."default",
  "created_time" timestamp(6),
  "updated_by" varchar(20) COLLATE "pg_catalog"."default",
  "updated_id" varchar(20) COLLATE "pg_catalog"."default",
  "updated_time" timestamp(6),
  "tenant_id" varchar(20) COLLATE "pg_catalog"."default",
  "request_id" varchar(64) COLLATE "pg_catalog"."default" NOT NULL,
  "mo_code" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
  "adjust_type" varchar(32) COLLATE "pg_catalog"."default" NOT NULL,
  "source" varchar(16) COLLATE "pg_catalog"."default",
  "before_json" text COLLATE "pg_catalog"."default",
  "after_json" text COLLATE "pg_catalog"."default",
  "affect_mps_ids" text COLLATE "pg_catalog"."default",
  "status" int4 DEFAULT 0,
  "err_msg" varchar(500) COLLATE "pg_catalog"."default",
  "operator" varchar(64) COLLATE "pg_catalog"."default",
  "create_time" timestamp(6),
  PRIMARY KEY ("id")
)
;
CREATE UNIQUE INDEX "uk_t_mo_adjust_log_request_id" ON "public"."t_mo_adjust_log" USING btree ("request_id");
CREATE INDEX "idx_t_mo_adjust_log_mo_code" ON "public"."t_mo_adjust_log" USING btree ("mo_code");
COMMENT ON COLUMN "public"."t_mo_adjust_log"."request_id" IS 'idempotency key';
COMMENT ON COLUMN "public"."t_mo_adjust_log"."adjust_type" IS 'CANCEL_MO/CANCEL_MPS/RESCHEDULE/PRIORITY/CHANGE_LINE/CHANGE_DEV/QTY_UP/QTY_DOWN';
COMMENT ON COLUMN "public"."t_mo_adjust_log"."status" IS '0PENDING 1SUCCESS 2FAILED';
COMMENT ON TABLE "public"."t_mo_adjust_log" IS 'mo adjust audit log';

-- ----------------------------
-- Table structure for t_mc
-- ----------------------------
DROP TABLE IF EXISTS "public"."t_mc";
CREATE TABLE "public"."t_mc" (
  "id" varchar(20) COLLATE "pg_catalog"."default" NOT NULL,
  "revision" int4,
  "created_by" varchar(20) COLLATE "pg_catalog"."default",
  "created_id" varchar(20) COLLATE "pg_catalog"."default",
  "created_time" timestamp(6),
  "updated_by" varchar(20) COLLATE "pg_catalog"."default",
  "updated_id" varchar(20) COLLATE "pg_catalog"."default",
  "updated_time" timestamp(6),
  "self_code" varchar(50) COLLATE "pg_catalog"."default",
  "code" varchar(60) COLLATE "pg_catalog"."default",
  "pcode" varchar(60) COLLATE "pg_catalog"."default",
  "name" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
  CONSTRAINT "t_mc_pkey" PRIMARY KEY ("id")
)
;
COMMENT ON COLUMN "public"."t_mc"."self_code" IS '分类编号';
COMMENT ON COLUMN "public"."t_mc"."code" IS '级联编号';
COMMENT ON COLUMN "public"."t_mc"."pcode" IS '级联父编号';
COMMENT ON COLUMN "public"."t_mc"."name" IS '分类名称';
COMMENT ON TABLE "public"."t_mc" IS '物料分类';

-- ----------------------------
-- Table structure for t_message
-- ----------------------------
DROP TABLE IF EXISTS "public"."t_message";
CREATE TABLE "public"."t_message" (
  "id" varchar(20) COLLATE "pg_catalog"."default" NOT NULL,
  "revision" int4,
  "created_by" varchar(20) COLLATE "pg_catalog"."default",
  "created_id" varchar(20) COLLATE "pg_catalog"."default",
  "created_time" timestamp(6),
  "updated_by" varchar(20) COLLATE "pg_catalog"."default",
  "updated_id" varchar(20) COLLATE "pg_catalog"."default",
  "updated_time" timestamp(6),
  "tenant_id" varchar(20) COLLATE "pg_catalog"."default",
  "title" varchar(255) COLLATE "pg_catalog"."default",
  "context" text COLLATE "pg_catalog"."default",
  "type" int4,
  "platform" varchar(64) COLLATE "pg_catalog"."default",
  "notify_id" varchar(20) COLLATE "pg_catalog"."default",
  "source" varchar(64) COLLATE "pg_catalog"."default",
  "source_id" varchar(64) COLLATE "pg_catalog"."default",
  CONSTRAINT "t_message_pkey" PRIMARY KEY ("id")
)
;
COMMENT ON COLUMN "public"."t_message"."title" IS '标题';
COMMENT ON COLUMN "public"."t_message"."context" IS '内容';
COMMENT ON COLUMN "public"."t_message"."type" IS '消息类型';
COMMENT ON COLUMN "public"."t_message"."platform" IS '平台';
COMMENT ON COLUMN "public"."t_message"."notify_id" IS '通知id';
COMMENT ON COLUMN "public"."t_message"."source" IS '来源';
COMMENT ON COLUMN "public"."t_message"."source_id" IS '来源id';
COMMENT ON TABLE "public"."t_message" IS '消息';

-- ----------------------------
-- Table structure for t_notify
-- ----------------------------
DROP TABLE IF EXISTS "public"."t_notify";
CREATE TABLE "public"."t_notify" (
  "id" varchar(20) COLLATE "pg_catalog"."default" NOT NULL,
  "revision" int4,
  "created_by" varchar(20) COLLATE "pg_catalog"."default",
  "created_id" varchar(20) COLLATE "pg_catalog"."default",
  "created_time" timestamp(6),
  "updated_by" varchar(20) COLLATE "pg_catalog"."default",
  "updated_id" varchar(20) COLLATE "pg_catalog"."default",
  "updated_time" timestamp(6),
  "tenant_id" varchar(20) COLLATE "pg_catalog"."default",
  "title" varchar(255) COLLATE "pg_catalog"."default",
  "context" text COLLATE "pg_catalog"."default",
  "type" int4,
  "status" int4,
  "platform" varchar(255) COLLATE "pg_catalog"."default",
  "step" int4 DEFAULT 0,
  "source" varchar(64) COLLATE "pg_catalog"."default",
  "source_id" varchar(64) COLLATE "pg_catalog"."default",
  CONSTRAINT "t_notify_pkey" PRIMARY KEY ("id")
)
;
COMMENT ON COLUMN "public"."t_notify"."title" IS '标题';
COMMENT ON COLUMN "public"."t_notify"."context" IS '内容';
COMMENT ON COLUMN "public"."t_notify"."type" IS '通知类型';
COMMENT ON COLUMN "public"."t_notify"."status" IS '状态';
COMMENT ON COLUMN "public"."t_notify"."platform" IS '平台，多个以|分隔';
COMMENT ON COLUMN "public"."t_notify"."step" IS '推送步骤';
COMMENT ON COLUMN "public"."t_notify"."source" IS '来源';
COMMENT ON COLUMN "public"."t_notify"."source_id" IS '来源id';
COMMENT ON TABLE "public"."t_notify" IS '通知';

-- ----------------------------
-- Table structure for t_product
-- ----------------------------
DROP TABLE IF EXISTS "public"."t_product";
CREATE TABLE "public"."t_product" (
  "id" varchar(20) COLLATE "pg_catalog"."default" NOT NULL,
  "revision" int4,
  "created_by" varchar(20) COLLATE "pg_catalog"."default",
  "created_id" varchar(20) COLLATE "pg_catalog"."default",
  "created_time" timestamp(6),
  "updated_by" varchar(20) COLLATE "pg_catalog"."default",
  "updated_id" varchar(20) COLLATE "pg_catalog"."default",
  "updated_time" timestamp(6),
  "tenant_id" varchar(20) COLLATE "pg_catalog"."default",
  "name" varchar(128) COLLATE "pg_catalog"."default",
  "code" varchar(64) COLLATE "pg_catalog"."default",
  "image_url" varchar(512) COLLATE "pg_catalog"."default",
  "attr_config" jsonb,
  CONSTRAINT "t_product_pkey" PRIMARY KEY ("id")
)
;
COMMENT ON COLUMN "public"."t_product"."name" IS '产品名称';
COMMENT ON COLUMN "public"."t_product"."code" IS '产品编号';
COMMENT ON COLUMN "public"."t_product"."image_url" IS '产品图片地址';
COMMENT ON COLUMN "public"."t_product"."attr_config" IS '产品属性模板JSON';
COMMENT ON TABLE "public"."t_product" IS '产品';

-- ----------------------------
-- Table structure for t_product_model
-- ----------------------------
DROP TABLE IF EXISTS "public"."t_product_model";
CREATE TABLE "public"."t_product_model" (
  "id" varchar(20) COLLATE "pg_catalog"."default" NOT NULL,
  "revision" int4,
  "created_by" varchar(20) COLLATE "pg_catalog"."default",
  "created_id" varchar(20) COLLATE "pg_catalog"."default",
  "created_time" timestamp(6),
  "updated_by" varchar(20) COLLATE "pg_catalog"."default",
  "updated_id" varchar(20) COLLATE "pg_catalog"."default",
  "updated_time" timestamp(6),
  "tenant_id" varchar(20) COLLATE "pg_catalog"."default",
  "product_code" varchar(64) COLLATE "pg_catalog"."default",
  "name" varchar(128) COLLATE "pg_catalog"."default",
  "code" varchar(64) COLLATE "pg_catalog"."default",
  "attr_config" jsonb,
  CONSTRAINT "t_product_model_pkey" PRIMARY KEY ("id")
)
;
COMMENT ON COLUMN "public"."t_product_model"."product_code" IS '所属产品编号';
COMMENT ON COLUMN "public"."t_product_model"."name" IS '型号名称';
COMMENT ON COLUMN "public"."t_product_model"."code" IS '型号编号';
COMMENT ON COLUMN "public"."t_product_model"."attr_config" IS '型号采集映射JSON';
COMMENT ON TABLE "public"."t_product_model" IS '产品型号';

-- ----------------------------
-- Table structure for t_qa
-- ----------------------------
DROP TABLE IF EXISTS "public"."t_qa";
CREATE TABLE "public"."t_qa" (
  "id" varchar(20) COLLATE "pg_catalog"."default" NOT NULL,
  "revision" int4,
  "created_by" varchar(20) COLLATE "pg_catalog"."default",
  "created_id" varchar(20) COLLATE "pg_catalog"."default",
  "created_time" timestamp(6),
  "updated_by" varchar(20) COLLATE "pg_catalog"."default",
  "updated_id" varchar(20) COLLATE "pg_catalog"."default",
  "updated_time" timestamp(6),
  "tenant_id" varchar(20) COLLATE "pg_catalog"."default",
  "mps_id" varchar(20) COLLATE "pg_catalog"."default",
  "mps_tf_id" varchar(20) COLLATE "pg_catalog"."default",
  "msg" text COLLATE "pg_catalog"."default",
  "result" int4,
  "pass" numeric(10,4),
  CONSTRAINT "t_qa_pkey" PRIMARY KEY ("id")
)
;
COMMENT ON COLUMN "public"."t_qa"."mps_id" IS '生产计划id';
COMMENT ON COLUMN "public"."t_qa"."mps_tf_id" IS '工艺流程id';
COMMENT ON COLUMN "public"."t_qa"."msg" IS '质检说明';
COMMENT ON COLUMN "public"."t_qa"."result" IS '质检结果';
COMMENT ON COLUMN "public"."t_qa"."pass" IS '合格量';
COMMENT ON TABLE "public"."t_qa" IS '质检记录';

-- ----------------------------
-- Table structure for t_sync
-- ----------------------------
DROP TABLE IF EXISTS "public"."t_sync";
CREATE TABLE "public"."t_sync" (
  "id" varchar(20) COLLATE "pg_catalog"."default" NOT NULL,
  "revision" int4,
  "created_by" varchar(20) COLLATE "pg_catalog"."default",
  "created_id" varchar(20) COLLATE "pg_catalog"."default",
  "created_time" timestamp(6),
  "updated_by" varchar(20) COLLATE "pg_catalog"."default",
  "updated_id" varchar(20) COLLATE "pg_catalog"."default",
  "updated_time" timestamp(6),
  "tenant_id" varchar(20) COLLATE "pg_catalog"."default",
  "sync_tx" varchar(64) COLLATE "pg_catalog"."default",
  "part_start_timestamp" timestamp(6),
  "part_end_timestamp" timestamp(6),
  "status" varchar(20) COLLATE "pg_catalog"."default",
  "pre_min" varchar(64) COLLATE "pg_catalog"."default",
  "pre_max" varchar(64) COLLATE "pg_catalog"."default",
  "part_min" varchar(64) COLLATE "pg_catalog"."default",
  "part_max" varchar(64) COLLATE "pg_catalog"."default",
  CONSTRAINT "t_sync_pkey" PRIMARY KEY ("id")
)
;
COMMENT ON COLUMN "public"."t_sync"."sync_tx" IS '同步分类';
COMMENT ON COLUMN "public"."t_sync"."part_start_timestamp" IS '分片开始时间戳';
COMMENT ON COLUMN "public"."t_sync"."part_end_timestamp" IS '分片结束时间戳';
COMMENT ON COLUMN "public"."t_sync"."status" IS '同步状态';
COMMENT ON COLUMN "public"."t_sync"."pre_min" IS '上一分片的最小值';
COMMENT ON COLUMN "public"."t_sync"."pre_max" IS '上一分片的最大值';
COMMENT ON COLUMN "public"."t_sync"."part_min" IS '分片最小值';
COMMENT ON COLUMN "public"."t_sync"."part_max" IS '分片最大值';
COMMENT ON TABLE "public"."t_sync" IS '同步事务';

-- ----------------------------
-- Table structure for t_task
-- ----------------------------
DROP TABLE IF EXISTS "public"."t_task";
CREATE TABLE "public"."t_task" (
  "id" varchar(20) COLLATE "pg_catalog"."default" NOT NULL,
  "revision" int4,
  "created_by" varchar(20) COLLATE "pg_catalog"."default",
  "created_id" varchar(20) COLLATE "pg_catalog"."default",
  "created_time" timestamp(6),
  "updated_by" varchar(20) COLLATE "pg_catalog"."default",
  "updated_id" varchar(20) COLLATE "pg_catalog"."default",
  "updated_time" timestamp(6),
  "name" varchar(50) COLLATE "pg_catalog"."default",
  "type" varchar(50) COLLATE "pg_catalog"."default",
  "cron" varchar(64) COLLATE "pg_catalog"."default",
  "status" int4,
  CONSTRAINT "t_task_pkey" PRIMARY KEY ("id")
)
;
COMMENT ON COLUMN "public"."t_task"."name" IS '名称';
COMMENT ON COLUMN "public"."t_task"."type" IS '任务类型';
COMMENT ON COLUMN "public"."t_task"."cron" IS 'cron表达式';
COMMENT ON COLUMN "public"."t_task"."status" IS '任务状态';
COMMENT ON TABLE "public"."t_task" IS '定时任务';

-- ----------------------------
-- Table structure for t_tf
-- ----------------------------
DROP TABLE IF EXISTS "public"."t_tf";
CREATE TABLE "public"."t_tf" (
  "id" varchar(20) COLLATE "pg_catalog"."default" NOT NULL,
  "revision" int4,
  "created_by" varchar(20) COLLATE "pg_catalog"."default",
  "created_id" varchar(20) COLLATE "pg_catalog"."default",
  "created_time" timestamp(6),
  "updated_by" varchar(20) COLLATE "pg_catalog"."default",
  "updated_id" varchar(20) COLLATE "pg_catalog"."default",
  "updated_time" timestamp(6),
  "tenant_id" varchar(20) COLLATE "pg_catalog"."default",
  "self_code" varchar(50) COLLATE "pg_catalog"."default",
  "name" varchar(50) COLLATE "pg_catalog"."default",
  "step_no" int4,
  "step_content" text COLLATE "pg_catalog"."default",
  "equip_json" text COLLATE "pg_catalog"."default",
  "tooling_json" text COLLATE "pg_catalog"."default",
  "line_id" varchar(20) COLLATE "pg_catalog"."default",
  "step_script" text COLLATE "pg_catalog"."default",
  "step_engine_config" text COLLATE "pg_catalog"."default",
  CONSTRAINT "t_tf_pkey" PRIMARY KEY ("id")
)
;
COMMENT ON COLUMN "public"."t_tf"."self_code" IS '工序编号';
COMMENT ON COLUMN "public"."t_tf"."name" IS '工序名称';
COMMENT ON COLUMN "public"."t_tf"."step_no" IS '工序序号';
COMMENT ON COLUMN "public"."t_tf"."step_content" IS '工序内容';
COMMENT ON COLUMN "public"."t_tf"."equip_json" IS '设备JSON';
COMMENT ON COLUMN "public"."t_tf"."tooling_json" IS '工装JSON';
COMMENT ON COLUMN "public"."t_tf"."line_id" IS '产线id';
COMMENT ON COLUMN "public"."t_tf"."step_script" IS '工序执行脚本JSON';
COMMENT ON COLUMN "public"."t_tf"."step_engine_config" IS '流程引擎编译配置JSON';
COMMENT ON TABLE "public"."t_tf" IS '工序';

-- ----------------------------
-- Table structure for t_tf_edge
-- ----------------------------
DROP TABLE IF EXISTS "public"."t_tf_edge";
CREATE TABLE "public"."t_tf_edge" (
  "id" varchar(20) COLLATE "pg_catalog"."default" NOT NULL,
  "revision" int4,
  "created_by" varchar(20) COLLATE "pg_catalog"."default",
  "created_id" varchar(20) COLLATE "pg_catalog"."default",
  "created_time" timestamp(6),
  "updated_by" varchar(20) COLLATE "pg_catalog"."default",
  "updated_id" varchar(20) COLLATE "pg_catalog"."default",
  "updated_time" timestamp(6),
  "tenant_id" varchar(20) COLLATE "pg_catalog"."default",
  "line_id" varchar(20) COLLATE "pg_catalog"."default",
  "from_tf_id" varchar(20) COLLATE "pg_catalog"."default",
  "to_tf_id" varchar(20) COLLATE "pg_catalog"."default",
  CONSTRAINT "t_tf_edge_pkey" PRIMARY KEY ("id")
)
;
COMMENT ON COLUMN "public"."t_tf_edge"."line_id" IS '产线id';
COMMENT ON COLUMN "public"."t_tf_edge"."from_tf_id" IS '起始工序id';
COMMENT ON COLUMN "public"."t_tf_edge"."to_tf_id" IS '目标工序id';
COMMENT ON TABLE "public"."t_tf_edge" IS '工序流转';

-- ----------------------------
-- Table structure for t_ucenter_depart
-- ----------------------------
DROP TABLE IF EXISTS "public"."t_ucenter_depart";
CREATE TABLE "public"."t_ucenter_depart" (
  "id" varchar(20) COLLATE "pg_catalog"."default" NOT NULL,
  "revision" int4,
  "created_by" varchar(64) COLLATE "pg_catalog"."default",
  "created_id" varchar(64) COLLATE "pg_catalog"."default",
  "created_time" timestamp(6),
  "updated_by" varchar(64) COLLATE "pg_catalog"."default",
  "updated_id" varchar(64) COLLATE "pg_catalog"."default",
  "updated_time" timestamp(6),
  "tenant_id" varchar(20) COLLATE "pg_catalog"."default",
  "name" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
  "code" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
  "pcode" varchar(50) COLLATE "pg_catalog"."default",
  "ppcode" varchar(500) COLLATE "pg_catalog"."default",
  CONSTRAINT "t_ucenter_depart_pkey" PRIMARY KEY ("id")
)
;
COMMENT ON COLUMN "public"."t_ucenter_depart"."name" IS '部门名';
COMMENT ON COLUMN "public"."t_ucenter_depart"."code" IS '部门编号';
COMMENT ON COLUMN "public"."t_ucenter_depart"."pcode" IS '上级部门编号';
COMMENT ON COLUMN "public"."t_ucenter_depart"."ppcode" IS '全部上级部门编号，分号分隔';
COMMENT ON TABLE "public"."t_ucenter_depart" IS '部门';

-- ----------------------------
-- Table structure for t_ucenter_role
-- ----------------------------
DROP TABLE IF EXISTS "public"."t_ucenter_role";
CREATE TABLE "public"."t_ucenter_role" (
  "id" varchar(20) COLLATE "pg_catalog"."default" NOT NULL,
  "revision" int4,
  "created_by" varchar(64) COLLATE "pg_catalog"."default",
  "created_id" varchar(64) COLLATE "pg_catalog"."default",
  "created_time" timestamp(6),
  "updated_by" varchar(64) COLLATE "pg_catalog"."default",
  "updated_id" varchar(64) COLLATE "pg_catalog"."default",
  "updated_time" timestamp(6),
  "tenant_id" varchar(20) COLLATE "pg_catalog"."default",
  "name" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
  "code" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
  "description" varchar(250) COLLATE "pg_catalog"."default",
  "type" int4,
  CONSTRAINT "t_ucenter_role_pkey" PRIMARY KEY ("id")
)
;
COMMENT ON COLUMN "public"."t_ucenter_role"."name" IS '角色名';
COMMENT ON COLUMN "public"."t_ucenter_role"."code" IS '角色编号';
COMMENT ON COLUMN "public"."t_ucenter_role"."description" IS '角色描述';
COMMENT ON COLUMN "public"."t_ucenter_role"."type" IS '角色类别';
COMMENT ON TABLE "public"."t_ucenter_role" IS '角色';

-- ----------------------------
-- Table structure for t_workshop
-- ----------------------------
DROP TABLE IF EXISTS "public"."t_workshop";
CREATE TABLE "public"."t_workshop" (
  "id" varchar(20) COLLATE "pg_catalog"."default" NOT NULL,
  "revision" int4,
  "created_by" varchar(20) COLLATE "pg_catalog"."default",
  "created_id" varchar(20) COLLATE "pg_catalog"."default",
  "created_time" timestamp(6),
  "updated_by" varchar(20) COLLATE "pg_catalog"."default",
  "updated_id" varchar(20) COLLATE "pg_catalog"."default",
  "updated_time" timestamp(6),
  "name" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
  "self_code" varchar(50) COLLATE "pg_catalog"."default",
  "code" varchar(60) COLLATE "pg_catalog"."default",
  "pcode" varchar(60) COLLATE "pg_catalog"."default",
  "lng" numeric(12,8),
  "lat" numeric(12,8),
  "address" varchar(255) COLLATE "pg_catalog"."default",
  CONSTRAINT "t_workshop_pkey" PRIMARY KEY ("id")
)
;
COMMENT ON COLUMN "public"."t_workshop"."name" IS '场景名称';
COMMENT ON COLUMN "public"."t_workshop"."self_code" IS '场景编号';
COMMENT ON COLUMN "public"."t_workshop"."code" IS '级联编号';
COMMENT ON COLUMN "public"."t_workshop"."pcode" IS '级联父编号';
COMMENT ON COLUMN "public"."t_workshop"."lng" IS '经度';
COMMENT ON COLUMN "public"."t_workshop"."lat" IS '纬度';
COMMENT ON COLUMN "public"."t_workshop"."address" IS '地址';
COMMENT ON TABLE "public"."t_workshop" IS '场景';

-- ----------------------------
-- Table structure for t_workshop_collect
-- ----------------------------
DROP TABLE IF EXISTS "public"."t_workshop_collect";
CREATE TABLE "public"."t_workshop_collect" (
  "id" varchar(20) COLLATE "pg_catalog"."default" NOT NULL,
  "revision" int4,
  "created_by" varchar(20) COLLATE "pg_catalog"."default",
  "created_id" varchar(20) COLLATE "pg_catalog"."default",
  "created_time" timestamp(6),
  "updated_by" varchar(20) COLLATE "pg_catalog"."default",
  "updated_id" varchar(20) COLLATE "pg_catalog"."default",
  "updated_time" timestamp(6),
  "tenant_id" varchar(20) COLLATE "pg_catalog"."default",
  "workshop_id" varchar(20) COLLATE "pg_catalog"."default",
  "data" jsonb,
  "time" timestamp(6),
  CONSTRAINT "t_workshop_collect_pkey" PRIMARY KEY ("id")
)
;
COMMENT ON COLUMN "public"."t_workshop_collect"."workshop_id" IS '场景id';
COMMENT ON COLUMN "public"."t_workshop_collect"."data" IS '采集数据';
COMMENT ON COLUMN "public"."t_workshop_collect"."time" IS '采集时间';
COMMENT ON TABLE "public"."t_workshop_collect" IS '场景采集';
