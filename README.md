# 学习提醒 App（LearningReminder）

家长与孩子的日常学习任务追踪 Android App：家长布置学习任务 → 系统按时间提醒 → 打卡记录完成情况 → 查看日/周完成率。v1.0 为纯本地版本，数据全部保存在设备私有目录，不请求账号与云端。

## 技术栈

- Kotlin 2.0.20 / Jetpack Compose（Material 3，BOM 2024.09.01）
- Room 2.6.1（KSP）本地数据库
- DataStore Preferences（当前孩子、提醒偏好）
- AlarmManager + BroadcastReceiver（精确提醒、稍后提醒、开机重排）
- minSdk 26 / targetSdk 34，AGP 8.5.2，JDK 17

## 架构

```text
UI（Compose Screen）
  -> ViewModel / StateFlow
      -> Repository
          -> Room DAO
      -> ReminderScheduler -> AlarmManager -> ReminderReceiver -> NotificationManager
```

- `data/`：实体、DAO、Repository、重复规则与完成率纯函数（`RepeatRule`、`StatsCalculator`）
- `viewmodel/`：Today / Tasks / Report / Settings 四个 ViewModel
- `ui/`：今日看板、任务管理、报表、设置，编辑与打卡用 ModalBottomSheet
- `reminder/`：调度、通知、开机重排

## 功能（v1.0）

- 多孩子档案，默认孩子可切换
- 任务管理：标题、科目、起止日期、提醒时间、重复规则（每天 / 工作日 / 指定周几）、建议时长、完成方式、是否需依据、备注、启停、删除
- 今日看板：完成率、任务卡状态徽标、空态引导
- 打卡：完成 / 部分完成 / 进行中 / 请假 / 没做 / 跳过，含孩子与家长备注；同任务同天只保留一条记录
- 报表：日 / 周 / 月 / 自定义区间，近 7 天完成率，按状态筛选，完成记录列表
- 提醒：到点精确提醒、通知内「完成 / 稍后提醒」、开机与打开 App 自动重排、通知与精确闹钟权限引导与降级
- 设置：孩子管理、提醒总开关、按周几提醒、JSON 导出

## 构建

```bash
./gradlew assembleDebug        # 产出 app/build/outputs/apk/debug/app-debug.apk
./gradlew testDebugUnitTest    # 单元测试
```

首次构建需在 `local.properties` 中配置 `sdk.dir`（或用 `ANDROID_HOME` 环境变量）。

## 文档

- 产品需求：`docs/PRD.md`
- 实施计划：`docs/superpowers/plans/2026-10-22-learning-reminder-v1.md`
- 交互原型：`prototype/index.html`
