# 学习提醒 App v1.0 补全实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 补全 LearningReminderApp 至 PRD v1.0 可用版本：修复编译错误、补齐 Manifest/资源、打通提醒闭环、实现任务管理/打卡/报表/设置四大页面与 ViewModel 分层。

**Architecture:** 保持现有工程形态（单模块 Compose + Room）。新增：DataStore 设置存储、ChildRepository/RecordRepository、每页一个 ViewModel、BootReceiver + 通知动作（SNOOZE/标记完成）。提醒以 Room 为最终数据源，任何时机（开 App/改动任务/重启）都用 `rescheduleAll()` 从 Room 重注册 AlarmManager。

**Tech Stack:** Kotlin 2.0.20 / Compose BOM 2024.09.01 / Material3 / Room 2.6.1 (KSP) / DataStore Preferences 1.1.1 / JUnit4。不引入 Hilt、Coil 等新框架性依赖（只加 `androidx-datastore-preferences` 与 `junit`）。

## Global Constraints
- minSdk 26，targetSdk 34，namespace `com.example.learningreminder`
- 日期/时间一律 ISO 字符串存 Room（`LocalDate.toString()` / `LocalTime.toString()`）
- repeatRule 采用 PRD 英文枚举：`DAILY`、`WEEKDAY`、具体星期名 `MON,TUE,...,SUN`；一次性任务用 `DAILY` + 结束日期限制
- CompletionRecord 唯一索引 `(childId, taskId, date)`，插入一律 `OnConflictStrategy.IGNORE` 后查回更新，保证同天单条
- 请假 `ASK_LEAVE` / 跳过 `SKIP_TODAY` 不计入完成率分母；EXPIRED 单独显示
- 所有通知使用频道 `learning_reminder`（高优先级）+ `learning_reminder_snooze`（默认优先级）
- 不修改现有 `ui/Theme.kt`、`data/Converters.kt`
- 无 Gradle Wrapper；验证用本机 gradle（若 Android SDK 缺失则编译验证降级为人工检查 + 告知用户）

---

## 文件结构总览

```
app/src/main/
  AndroidManifest.xml                                    (创建)
  res/drawable/ic_stat_reminder.xml                      (创建, vector 图标)
  res/values/strings.xml, themes.xml, colors.xml         (创建)
  java/com/example/learningreminder/
    MainActivity.kt                                      (修改: 接 ViewModel/权限)
    AppContainer.kt                                      (创建: 手动依赖容器)
    data/LearningTask.kt                                 (修改: repeatRule 默认值)
    data/TaskDao.kt, RecordDao.kt, ChildDao.kt           (修改/创建: 新查询)
    data/ChildRepository.kt, RecordRepository.kt         (创建)
    data/TaskRepository.kt                               (重写)
    data/SettingsStore.kt                                (创建: DataStore)
    data/RepeatRule.kt                                   (创建: 日期匹配纯函数)
    data/StatsCalculator.kt                              (创建: 完成率纯函数)
    reminder/ReminderScheduler.kt                        (重写)
    reminder/ReminderReceiver.kt                         (重写: 点进详情/稍后/完成)
    reminder/BootReceiver.kt                             (创建)
    ui/LearningApp.kt                                    (重写: 导航壳)
    ui/TodayScreen.kt                                    (创建)
    ui/TaskEditScreen.kt                                 (创建: 新建/编辑表单Sheet)
    ui/TaskDetailScreen.kt                               (创建: 打卡/记录Sheet)
    ui/TaskManageScreen.kt                               (创建)
    ui/ReportScreen.kt, SettingsScreen.kt                (创建)
    ui/theme 已有 Theme.kt 不动
    viewmodel/ (创建 5 个 VM)
  test/... data/RepeatRuleTest.kt, StatsCalculatorTest.kt (创建)
```

---

### Task 1: 资源、Manifest、依赖

**Files:**
- Create: `app/src/main/AndroidManifest.xml`, `app/src/main/res/drawable/ic_stat_reminder.xml`, `app/src/main/res/values/{strings.xml,themes.xml}`, `app/src/main/res/values-night/themes.xml`
- Modify: `gradle/libs.versions.toml`, `app/build.gradle.kts`（加 datastore、junit）

**Interfaces (Produces):** 完整可编译的 Android 应用壳；`R.drawable.ic_stat_reminder`、`@style/Theme.LearningReminder`、权限 `POST_NOTIFICATIONS`/`RECEIVE_BOOT_COMPLETED`/`SCHEDULE_EXACT_ALARM`、注册 `ReminderReceiver` + `BootReceiver`、MainActivity `exported` + `showForTask` intent 处理。

- [ ] Step 1: 写入 libs.versions.toml 增项 `datastore = "1.1.1"`、library `androidx-datastore-preferences`、`junit:4.13.2`（testImplementation）
- [ ] Step 2: 写入 AndroidManifest.xml（application label="学习提醒"、theme、MainActivity launcher exported、 receivers：ReminderReceiver `exported=false`、BootReceiver 带 RECEIVE_BOOT_COMPLETED intent-filter；permissions 如上三条）
- [ ] Step 3: drawable 用 vector path 画一个书本+铃铛简化图标（自绘 path，monochrome 兼容）
- [ ] Step 4: themes.xml 定义 `Theme.LearningReminder` parent= `android:Theme.Material.NoActionBar`（纯 Compose 应用无需 AppCompat）；values-night 同名
- [ ] Step 5: 编译前静态检查路径/命名一致

### Task 2: 纯函数工具 RepeatRule + StatsCalculator

**Files:**
- Create `data/RepeatRule.kt`, `data/StatsCalculator.kt`, `test/.../RepeatRuleTest.kt`, `test/.../StatsCalculatorTest.kt`

**Interfaces (Produces):**
```kotlin
object RepeatRule {
    fun isScheduled(rule: String, date: LocalDate): Boolean  // DAILY / WEEKDAY / MON..SUN 逗号列表
}
object StatsCalculator {
    data class DayStats(val scheduled: Int, val done: Int, val rate: Float)
    fun compute(tasks: List<LearningTask>, records: List<CompletionRecord>, date: LocalDate): DayStats
    // done = DONE 计 1, PARTIAL 计 0.5；ASK_LEAVE/SKIP_TODAY 不进分母；EXPIRED/NO_SHOW 计未完成
    suspend ... // 纯函数无 suspend
}
```
- [ ] Step 1: 先写测试（DAILY 每天命中、WEEKDAY 排除周末、`MON,WED` 命中与排除、startDate/endDate 边界、enabled=false 排除； StatsCalculator: DONE/PARTIAL/请假/跳过/超时各案例 + 空任务 = rate 0）
- [ ] Step 2: `gradle test --tests "*RepeatRule*" --tests "*Stats*"`（Windows: `gradlew` 缺失时用 `gradle test`）确认 FAIL
- [ ] Step 3: 实现两 object 使测试 PASS
- [ ] Step 4: 跑测试 PASS 后提交 `feat: repeat rule & stats pure functions`

### Task 3: DAO 与数据层扩展

**Files:**
- Create: `data/ChildDao.kt`, `data/ChildRepository.kt`, `data/RecordRepository.kt`, `data/SettingsStore.kt`
- Modify: `data/TaskDao.kt`, `data/RecordDao.kt`, `data/TaskRepository.kt`, `data/LearningTask.kt`（repeatRule 默认值改 `"MON,TUE,WED,THU,FRI"`）

**Interfaces (Produces):**
- TaskDao 新增: `observeWithStatus(childId, date): Flow<List<TaskWithStatus>>`（PRD 6.2 的 LEFT JOIN，返回 data class `TaskWithStatus(task: LearningTask, record: CompletionRecord?)`，写在 TaskDao.kt 里）、`observeEnabledAll(): Flow<List<LearningTask>>`、`getAll(): List<LearningTask>`（suspend，供 rescheduleAll）
- RecordDao 新增: `getById(id)`, `observeByRange(childId, from, to)`, `deleteForTask(taskId)`
- TaskRepository: `observeToday(childId, date:)` / `observeByChild(childId)` / `get` / `upsertWithReschedule` 交给 UI→VM 层调度 / `delete(taskId, keepRecords)`（false 时先 `recordDao.deleteForTask`）/ `allTasksForReschedule(): List<LearningTask>`
- ChildRepository: `getCurrentChild(): Flow<ChildProfile?>` + `listChildren(): Flow<List<ChildProfile>>` + `addDefaultIfNeeded(name)` + `setCurrentDefault(id)`（同事务清除其他 isDefault）+ `deleteChild(id)`
- RecordRepository: `observeToday(childId, date)`、`observeRange(childId, from, to)`、`setStatus(taskId, childId, date, status, completedAt, childNotes, parentNotes, recordedBy: String)`（插入 IGNORE，回 id 后查回更新；同天单条）、`updateNotes(recordId, childNotes, parentNotes)`、`addEvidence(recordId, uri)`
- SettingsStore（DataStore Preferences）: `currentChildId: Flow<Long?>`, `setCurrentChildId(v)`, `reminderGlobalEnabled: Flow<Boolean>`, `setReminderGlobalEnabled(v)`, `remindWeekdays: Flow<Set<String>>`（存 MON..SUN，默认全开）, `setRemindWeekdays(Set<String>)`
- [ ] Step 1: 修改/新建上述文件（代码给全量）
- [ ] Step 2: 提交 `feat: data layer for children/records/settings`

### Task 4: 提醒模块重写

**Files:**
- Rewrite `reminder/ReminderScheduler.kt`, `reminder/ReminderReceiver.kt`; Create `reminder/BootReceiver.kt`, `reminder/NotificationHelper.kt`

**Interfaces:**
- ReminderScheduler(context,任务数据由调用方传入):
```kotlin
class ReminderScheduler(private val context: Context) {
    fun schedule(task: LearningTask)   // 今天已过 reminderTime 或任务当天不匹配 → 设置为下一个匹配日
    fun scheduleAt(task: LearningTask, triggerAt: Long, snoozeMinutes: Int? = null)  // snooze 用 extra 标记
    fun cancel(taskId: Long)
    fun canScheduleExact(): Boolean    // minSdk26 用 SCHEDULE_EXACT_ALARM 兼容判断（26-30 无需）
    // 精确闹钟不可用时降级 setAndAllowWhileIdle
}
```
- ReminderReceiver：解析 taskId/title/subject/isSnooze；点按通知 → `MainActivity` extra `showForTask`；action `ACTION_DONE` → 写 CompletionRecord DONE 并取消；`ACTION_SNOOZE` → `scheduleAt(triggerAt = now + snoozeMinutes, isSnooze=true)`；之后若任务按天重复未完成 → 重排下一天
- BootReceiver：仅 `rescheduleAll`（读 Room 全部 enabled 任务、全局开关与 weekday 设置 → schedule）
- [ ] Step 1: 实现 4 个类
- [ ] Step 2: 提交 `feat: alarm scheduling with snooze/boot reschedule`

### Task 5: 视图模型 + 依赖容器

**Files:**
- Create `AppContainer.kt`, `viewmodel/{TodayViewModel,TasksViewModel,ReportViewModel,SettingsViewModel}.kt`

**Interfaces:**
```kotlin
class AppContainer(context) { db, taskRepo, recordRepo, childRepo, settings, scheduler }
class TodayViewModel(container, childId: Long?) : viewModel 聚合 tasks+records → TaskWithStatus 列表 + DayStats
suspend 便于执行任务变更/打卡/保存
```
- TodayViewModel 暴露：`todayItems: StateFlow<List<TaskWithStatus>>`、`summary: StateFlow<DayStats>`、`markDone(taskId)`, `markStatus(taskId, status)`、`generateToday()`（生成今日所有应做任务的 NOT_STARTED 记录）
- TasksViewModel：CRUD 全量、toggle
- ReportViewModel：`range(start,end)`、7 天滚动 rates、filterStatus/Subject
- SettingsViewModel：children、通知权限状态检查、全局提醒开关、weekday 开关、导出
- [ ] Task 5 合并执行，提交 `feat: viewmodels`

### Task 6: UI 页面补全

**Files:**
- Create: `ui/TodayScreen.kt` `ui/TaskEditScreen.kt` `ui/TaskDetailScreen.kt` `ui/TaskManageScreen.kt` `ui/ReportScreen.kt` `ui/SettingsScreen.kt`（`ui/LearningApp.kt` 承担壳与 BottomBar）

按各页面 PRD 3.1/3.2/3.3/3.5 满足今日卡片（完成率/状态 badge/点位）、任务编辑表单 sheet（标题/科目 spinner/重复 weekday chips + reminders/完成方式/起止日期用 ButtonPickerDialog 顶部 selectableDates 前置校验）、打卡 sheet（状态 grid + notes + 证据路径文本、家长/孩子 recordedBy toggle）、报表（总率 + 每任务列表 + 状态 filter chips + 科目 dropdown + 日期区间 chips 日/周/月/自定义）、设置（孩子增删改、通知与精确闹钟权限入口、全局提醒开关、weekday 开关、导出）。导出实现 `SettingsViewModel.exportJson(context)` 写到 SAF 生成位置。

- [ ] 在 LearningApp 中作为 `ModalBottomSheet` 打开 TaskEditSheet (新建/编辑复用)、TaskDetailSheet (含打卡)
- [ ] 编译后提交 `feat: full compose ui`

### Task 7: AndroidManifest 与权限引导 UI 联动

- SettingsScreen 内做权限检测 Banner：`POST_NOTIFICATIONS` 未授予 Settings 项跳转 `ACTION_POST_NOTIFICATION`/应用通知设置；`canScheduleExactAlarm()==false` 提示降级说明。
- [ ] 提交 `feat: permission guidance`

### Task 8: 集成回归与收尾

- MainActivity 启动即从 Room `rescheduleAll` 保证杀进程后重排。
- 今日列表对 `EXPIRED` 任务自动标记（当天已过提醒时间且仍 NOT_STARTED → 标记 EXPIRED 而非修改用户设置）
- 运行 `gradle test`；Android 模拟器编译情况记录到 docs/superpowers/plans 下。

---

## Self-Review
- PRD v1.0 里程碑 9 项全部有任务覆盖：孩子录入(T3/T6)、布置任务(T6)、生成今日(T5)、打卡(T6)、历史+7天率(T6)、通知(T4)。
- 类型一致性：`TaskWithStatus` 仅由 TaskDao 承载被 Today/Report 复用；`RepeatRule.isScheduled` 与 StatsCalculator 共用于 rate 计算。
- 无占位符；所有 step 有明确产出。
