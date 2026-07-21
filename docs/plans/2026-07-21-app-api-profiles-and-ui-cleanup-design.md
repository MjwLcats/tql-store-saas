# App 接口环境切换与 UI 清理设计

## 目标

开发者分别在公司和家中电脑运行 iOS 自定义基座时，可以在 `config/app.js` 中通过一行配置选择对应的 Gateway 地址；同时移除首页顶部品牌眉题及 tabBar 的数字和红点角标。

## 接口配置

`app.js` 提供 `company`、`home`、`auto` 三个命名环境。`company` 指向 `http://10.10.5.44:8080`，`home` 指向 `http://172.20.10.6:8080`，`auto` 沿用 Vite 编译时自动检测出的电脑局域网地址。开发者只修改 `ACTIVE_API_PROFILE` 即可切换，不需要改请求模块。

为确保 iOS 自定义基座允许访问局域网 HTTP 服务，`manifest.json` 的 iOS `privacyDescription` 增加 `NSLocalNetworkUsageDescription`，并保留已有的 ATS HTTP 放行配置。修改原生权限后需要重新制作并安装自定义调试基座。

## UI 与验证

自定义 tabBar 不再渲染 badge 或 dot，数据项中也移除对应静态演示值；首页标题上方的品牌眉题及其样式一并删除。自动化测试覆盖三套接口地址、当前环境、iOS 本地网络权限以及 UI 元素清理。H5 移动视口验证确认标题和 tabBar 无残留角标，页面切换仍正常。
