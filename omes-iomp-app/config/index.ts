import {get, isMp} from "@/uni_modules/cool-unix";
import {proxy} from "@/config/proxy";

// 判断当前是否为开发环境
export const isDev = process.env.NODE_ENV == "development";

// 忽略 token：OAuth2/验证码/注册等走 SAS 网关本地路径
export const ignoreTokens: string[] = [
    "/oauth2/*",
    "/open/captcha*",
    "/authentication/*",
];

// 微信配置
type WxConfig = {
    debug: boolean;
};

// 配置类型定义
type Config = {
    name: string; // 应用名称
    version: string; // 应用版本
    locale: string; // 应用语言
    website: string; // 官网地址
    host: string; // API 主机（OMES-SAS 统一网关）
    baseUrl: string; // API 基础路径
    authBaseUrl: string; // OAuth2/验证码（与 baseUrl 相同，均走 SAS 网关）
    showDarkButton: boolean; // 是否显示暗色模式切换按钮
    isCustomTabBar: boolean; // 是否自定义 tabBar
    backTop: boolean; // 是否显示回到顶部按钮
    wx: WxConfig; // 微信配置
    platform: string;
    equipRefreshTime: number;
};

// 根据环境导出最终配置
export const config = {
    name: "OMES",
    version: "1.0.0",
    locale: "zh",
    website: "",
    showDarkButton: isMp() ? false : true,
    isCustomTabBar: true,
    backTop: true,
    platform: "mes-app",
    wx: {
        debug: false
    },
    equipRefreshTime: 10 * 1000,
    ...(isDev ? getPath("dev") : getPath("prod"))
} as Config;

function getPath(env: string) {
    const host = get(proxy, env + `.target`) as string;
    const authTarget = get(proxy, env + `.authTarget`) as string | null;
    // 生产走反代：/api 由网关去掉后再转发 SAS。开发直连 SAS，后端没有 /api
    const prefix = env == "prod" ? "/api" : "";
    const authRoot = authTarget != null && authTarget.length > 0 ? authTarget : host;
    return {
        host,
        baseUrl: host + prefix,
        authBaseUrl: authRoot + prefix
    };
}

// 导出代理相关配置
export * from "./proxy";