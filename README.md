# Kitty IDE 🐱

**轻量化的 Android 端代码编辑器 | A lightweight mobile IDE for Android**

> Kitty IDE，您的轻量化移动工作站😼

---

## ✨ 特性

### 📁 项目管理
- **文件夹即项目**：用任意文件夹作为项目根，通过 SAF 访问，不强制目录结构
- **智能识别**：含 `.kitty_project.json` 的目录自动被识别为 Kitty 项目
- **所有项目列表**：自动扫描项目父目录，最近打开的项目排在最前面

### 📝 编辑体验
- **多文件标签**：同时打开多个文件，双击 / 长按标签弹出上下文菜单
- **递归文件树**：支持嵌套展开、单链目录自动合并展开（像 IntelliJ 那样）
- **行号 + 缩进引导线**：清晰看清代码层级
- **光标行高亮**：当前编辑行有淡色标记
- **捏合缩放**：双指缩放调整字号（10sp ~ 30sp）
- **点击区全覆盖**：点文字光标跳到点击位置，点空白仅聚焦

### ⌨️ 输入辅助
- **底部符号栏**：常用符号随手可得，常驻底部跟随键盘上移
- **成对符号智能跳转**：输入 `{}` 光标自动停在中间
- **回车自动缩进**：`{}` 后回车自动展开并缩进，普通换行继承上一行缩进

### ↩️ 撤销 / 重做
- 每个文件独立维护撤销栈
- **800ms 时间窗口合并**：连续快速输入算一次操作
- 栈上限 100，关闭标签自动释放

### 🖥️ 内置终端
- 捕获运行时的 `console.log / warn / error`
- 捕获未捕获异常和 Promise rejection
- 日志分级着色（LOG / INFO / WARN / ERROR）
- 编辑器页从右侧滑出，预览页从底部弹出，共享同一日志池

### ▶️ 运行预览
- 右上角 ▶️ 一键运行当前项目
- 智能选文件：当前是 HTML 直接跑，是 CSS/JS 自动找同目录的 `index.html`
- 内置 WebView 预览，不跳出 App
- 退出预览自动销毁 WebView，停止 JS 执行

### 🎨 主题与彩蛋
- **深色 / 浅色 / 跟随系统** 三种模式，即时切换并持久化
- **猫咪彩蛋**：点击启动页猫咪头像，随机旋转 50°~380°，旋转中随机换表情 🙀😾😽

---

## 🛠️ 技术栈

| 分类 | 技术 |
|---|---|
| 语言 | Kotlin |
| UI | Jetpack Compose + Material 3 |
| 架构 | MVVM（ViewModel + Repository） |
| 编辑器 | 纯原生 `BasicTextField` + 自定义绘制 |
| 异步 | Kotlin Coroutines + Flow |
| 存储 | SAF（Storage Access Framework）+ `java.io.File` |
| 最低版本 | Android 7.0 (API 24) |
| 目标版本 | Android 14 (API 34) |

---

## 📂 项目结构

```

app/src/main/java/com/kitty/ide/
├── AppInfo.kt                  # 应用元信息中心（版本/作者/开源地址）
├── MainActivity.kt
├── data/
│   ├── editor/
│   │   └── UndoManager.kt      # 撤销/重做管理器
│   ├── model/                  # 数据模型
│   ├── repository/             # 数据仓库（项目 / 设置）
│   └── terminal/
│       └── TerminalManager.kt  # 全局日志池
├── ui/
│   ├── component/              # 可复用组件（按钮 / 终端面板）
│   ├── navigation/             # 导航图
│   ├── screen/                 # 各页面（启动 / 编辑 / 预览 / 设置）
│   ├── theme/                  # 主题与配色
│   └── viewmodel/              # ViewModel
└── util/
└── NameValidator.kt        # 命名校验

```

---

## 🚀 构建

### 环境要求
- Android Studio Hedgehog 或更新
- JDK 17
- Android SDK 34

### 编译

```bash
./gradlew assembleDebug
```

产物位于 app/build/outputs/apk/debug/app-debug.apk

---

📌 版本历史

版本 主要内容
0.0.1 项目骨架、Compose 主题、启动页
0.0.2 文件系统设计、SAF 接入
0.0.3 全文件权限、标签管理、保存功能
0.0.4 底部符号栏
0.0.5 文件树新建功能、平滑展开
0.0.6 所有项目列表、最近项目排序
0.0.7 空状态提示、文件树提示
0.0.8 Web 运行预览（WebView）
0.0.9 启动页猫咪彩蛋
0.0.10 设置页（关于 / 主题切换）
0.0.11 版本号从 BuildConfig 读取，建立 AppInfo
0.0.12 内置终端（console 输出）
0.0.13 修复预览返回后文件重置
0.0.14 文件操作增强（重命名 / 删除）+ 命名校验
0.0.15 文件树长按菜单改为小菜单
0.0.16 撤销 / 重做
0.0.17 WebView 销毁修复、缩进引导线、光标行高亮、点击区全覆盖

---

📄 开源协议

本项目采用 Apache License 2.0 开源。

---

👤 作者

黄桃罐头吖386（HTGTY386）

· GitHub: @HTGTY
· 仓库: KittyIDE-Android

---

🙏 感谢

（名单待补充）

---

Copyright (C) 黄桃罐头吖386(HTGTY386)

Kitty IDE，您的轻量化移动工作站😼

*部分代码/文档由AI生成