# App tabBar 单页面重构设计

## 问题

旧实现同时使用 `pages.json` 原生 tabBar、`uni.switchTab`、四个 tab 页面壳和每页各自创建的自定义 tabBar。tab 页面会被缓存，而每套自定义导航又维护 `visualIndex`、`moving` 和延迟定时器。快速或连续切换时，旧页面定时器与新页面生命周期相互覆盖，导致页面来回跳；失败后的视觉索引没有回滚，又会让后续点击被误判为当前项。

## 方案

应用保留 `pages/home/index.vue` 作为登录后的唯一页面壳，工作台、任务、消息和我的继续作为壳内的四个内容面板。自定义 tabBar 变为无状态受控组件：选中态只读取父页面传入的 `activeIndex`，点击时仅发送 `change` 事件。父页面是唯一状态源，收到索引后同步切换内容。

移除 `pages.json` 的原生 tabBar 配置及三个页面壳路由，不再调用 `uni.switchTab`、`uni.hideTabBar`，也不再使用延迟导航定时器。胶囊滑动效果继续依赖 `activeIndex` 的 CSS transform 过渡，因此不需要额外导航锁。

## 验证

- 连续点击四个 tab，内容标题、面板和选中图标应同步更新。
- 快速往返点击时不得出现回跳或失效。
- 页面栈不因 tab 点击增加，登录和退出登录路由保持不变。
- 自动化回归测试禁止重新引入 `switchTab`、导航定时器和原生 tabBar 配置。
