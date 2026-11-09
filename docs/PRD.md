# 学习提醒 App 产品需求文档（PRD）

版本：v0.1  
日期：2026-10-22  
产品代号：LearningReminder  
平台：Android（原生 App，建议 Jetpack Compose + Kotlin）

## 1. 产品定位

这是一款家长和孩子日常学习任务追踪 App。核心目标：

1. 家长可布置孩子的学习任务。
2. 系统按时间提醒孩子，也可由家长手动提醒。
3. 家长/孩子可以输入完成结果。
4. 家长可查看当天、本周、某任务的完成情况。
5. 数据默认保存在本地，后续如需要可无缝加上家长端多设备同步。

> 说明：v1.0 按“家用、单设备优先”的轻量 Android App 设计。云同步 API 先作为架构预留，不阻塞第一款可用版本。

## 2. 使用角色

### 家长 / 管护人

主要使用者。

可执行：

- 选择孩子。
- 新增、修改、删除学习任务。
- 设置提醒时间和重复周期。
- 手动标记孩子今日任务的完成情况。
- 查看完成率、未完成原因、历史记录。
- 导出或分享当天/本周记录。

### 孩子

辅助使用者。

可执行：

- 查看今日任务清单。
- 点击任务进入打卡/完成页。
- 填写完成情况，例如完成、没完成、请假、需要家长确认。
- 上传或拍摄完成依据，例如书籍/作业截图，v1.0 自愿填写。

### 系统定时模块

负责：

- 按任务时间触达提醒。
- 按“稍后提醒”再触达一次。
- 夜间或儿童放学时段不做额外提醒。
- 提醒失败或被关闭时给出权限修复提示，不做静默失败。

## 3. 核心功能范围

### 3.1 首页 / 今日任务看板

需求：

- 默认展示“今日待办”，按任务完成状态分成：待完成、进行中、已完成、请假、超时。
- 显示每个任务的：
  - 任务名称
  - 科目/分类
  - 提醒时间
  - 重复频率（每天 / 每周几 / 每天除周末）
  - 完成状态
  - 完成方式（家长确认 / 自动判断 / 孩子自我汇报）
- 提供“一键生成今日”的插入式任务，仅在没有任务时显示。
- 首页可见：
  - 今日全部任务数
  - 已完成数
  - 完成率
  - 完成记录可以单独查看

验收：

- 没有任务时显示“先创建一个学习任务”。
- 每个任务卡片可以单独点击进入详情。
- 完成状态变化立即回写本地。

### 3.2 学习任务管理

需求：

任务字段：

| 字段 | 说明 | 必填 |
| --- | --- | --- |
| 任务标题 | 读书、写字、听英语等 | 是 |
| 科目/分类 | 语文、数学、英语、运动、阅读、外语等 | 否 |
| 关联孩子 | 默认当前选中的孩子 | 是 |
| 开始时间 | 任务建议开始时间 | 是 |
| 建议时长 | 可写“约 30 分钟” | 否 |
| 重复频率 | 每天、每周几、自定义、一次性 | 是 |
| 校正后提醒时间 | 可比建议开始时间更早/更晚 | 否 |
| 完成方式 | 家长确认 / 孩子录入 / 自动判断 | 是，默认家长确认 |
| 目标分数 / 期望值 | 例如 10 道题、读写 10 次；仅作展示和提醒 | 否 |
| 起止日期 | 默认长期有效 | 是，默认今天开始、三个月后结束 |
| 是否启用 | 临时关闭但不删除 | 是 |
| 备注 | 家长补充说明 | 否 |

操作：

- 新建任务。
- 从模板快速复制任务。
- 编辑任务。
- 暂停/恢复任务。
- 删除任务。
- 批量补昨天或周末任务。
- 批量切换开启/关闭。

验收：

- 新建任务时，重复时间和提醒时间冲突要提醒家长。
- 删除任务会提示“是否同时删除该任务的完成记录”。

### 3.3 完成记录打卡

需求：

- 按任务和日期生成打卡记录。
- 状态包括：
  - NOT_STARTED：未开始
  - IN_PROGRESS：进行中
  - DONE：完成
  - PARTIAL：部分完成
  - NO_SHOW：没做
  - ASK_LEAVE：请假
  - EXPIRED：过时未做
  - SKIP_TODAY：今日暂时跳过
- 完成时可选记录：
  - 完成时间
  - 是否自觉完成，无需提醒
  - 是否做得比目标更好
  - 孩子的备注
  - 家长备注
  - 打卡证据文本或图片
- App 内可以在任务卡片内快速打卡，也支持进入详情后修改。

验收：

- 同一任务同一天同一孩子只产生一条主完成记录。
- 后续修改不会生成重复记录，而是更新该记录。
- 补打卡可完成任务，但补打卡的记录需要标注过去日期。

### 3.4 提醒设置

需求：

- 提醒时间精度为分钟。
- 提醒方式包括：
  - 到达时间前置 0 / 5 / 15 / 30 分钟。
  - “稍后 5/10/15/20/30 分钟再提醒”。
  - 到达时间后，如果任务还没完成，可再提醒一次。
- 用户可设置：
  - 是否真正由系统通知提醒。
  - 是否允许在锁屏提示。
  - 是否免打扰。
  - 周一到周日各自的默认提醒开关。
- 提醒点击后进入任务详情，而不是只打开首页。

验收：

- App 杀掉进程后，系统提醒仍然可以触发。
- 通知权限被关闭时，下一次打开 App 要引导用户开启。
- 精确闹钟权限被关闭时，提醒降级为近似提醒并提示。

### 3.5 完成情况查看

需求：

- 按孩子、任务、时间段查看完成情况。
- 展示：
  - 今日完成率
  - 7 天完成率
  - 最近一次完成时间
  - 具体任务和记录的文字
  - 可按状态筛选
  - 可按科目筛选
  - 按日期点击查看当天详情
- 支持按：
  - 日
  - 周
  - 月
  - 自定义日期区间
  - 单个任务
- 支持分享或导出简单的文本/截图。

验收：

- 不会把未安排当天任务算作未完成。
- 请假和跳过不计入未完成，也不计入正常完成率。
- 超时没做的任务单独显示，以免家长误判孩子当天表现。

## 4. 页面与导航

| 页面 | 主要目的 | 关键对象 |
| --- | --- | --- |
| 家长选择页 | 启动时选择当前孩子 | ChildProfile |
| 今日看板 | 一眼看到今天有没有完成 | TodaySummary, TaskItem |
| 任务管理页 | 布置、修改、关闭学习任务 | Task |
| 任务详情 | 查看单任务历史和完成详情 | Task, CompletionRecord |
| 今日打卡 | 快速记录孩子完成情况 | CompletionRecord |
| 历史记录 | 查看过去完成情况 | CompletionRecord |
| 设置页 | 自定义提醒能力和无障碍设置 | ReminderRule |

建议导航：

```text
MainActivity
  -> BottomNavigation
      - 今日
      - 任务
      - 报告
      - 设置

从上述页面可以进入：
  - 选择学习主题
  - 任务详情
  - 打卡详情
  - 日历视图
  - 通知权限修复提示
```

## 5. 数据模型

以下为本地 Room 建议结构。

### ChildProfile

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | Long | 主键 |
| name | String | 孩子名 |
| birthDate | String? | 出生年月，可选 |
| avatarUri | String? | 本地头像 |
| createdAt | Long | 创建时间戳 |
| isDefault | Boolean | 是否默认当前孩子 |

### LearningTask

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | Long | 主键 |
| childId | Long | 关联孩子 |
| title | String | 任务标题 |
| subject | String | 科目/类别 |
| startDate | LocalDate | 开始日期 |
| endDate | LocalDate | 结束日期 |
| reminderTime | LocalTime | 触发提醒时间 |
| repeatRule | String | 重复规则，例如 DAILY, WEEKLY, MON,WED |
| suggestedMinutes | Int? | 建议时长 |
| completionPolicy | String | PARENT_REPORT / SELF_REPORT / AUTO |
| requiresEvidence | Boolean | 是否要求上传/录入说明 |
| enabled | Boolean | 是否启用 |
| notes | String? | 其他说明 |
| lastUpdated | Long | 本地变更时间 |

### CompletionRecord

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | Long | 主键 |
| taskId | Long | 关联 LearningTask |
| childId | Long | 冗余保存，方便统计 |
| date | LocalDate | 完成日 |
| status | String | NOT_STARTED / IN_PROGRESS / DONE / PARTIAL / NO_SHOW / ASK_LEAVE / SKIP_TODAY |
| completedAt | Long? | 实际完成时间 |
| evidenceUri | String? | 图片/拍照位置 |
| childNotes | String? | 孩子说明 |
| parentNotes | String? | 家长说明 |
| recordedBy | String | PARENT / CHILD / SYSTEM |
| autoStatus | Boolean | 是任务生成的默认状态还是手动更新 |
| lastUpdated | Long | 本地变更时间 |

### ReminderRule

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | Long | 主键 |
| taskId | Long | 可选，全局规则时为 null |
| childId | Long | 可选 |
| name | String | 规则名称 |
| time | LocalTime | 提醒时间 |
| daysOfWeek | String | 重复日期；未使用说明可参照任务 |
| enabled | Boolean | 是否开启 |
| leadMinutes | Int | 提前提醒分钟数 |
| snoozeMinutes | Int? | 未完成时稍后提醒 |
| notificationChannel | String | 学习提醒 / 重复提醒 / 紧急提醒 |

## 6. 本地架构与内部 API

> 建议按“页面只读 ViewModel，数据读写走 Repository，提醒走 Scheduler”的方式分层。

```text
UI（Compose）
  -> ViewModel / StateFlow
      -> Repository
          -> Room DAO
          -> ReminderScheduler
              -> AlarmManager
                  -> ReminderReceiver
                      -> NotificationManager
```

### 6.1 Repository API

注意：这里说的 API 是 Android App 内部 API，不依赖后端。

#### ChildRepository

```kotlin
suspend fun getCurrentChild(): ChildProfile?
suspend fun setCurrentChild(childId: Long)
suspend fun listChildren(): List<ChildProfile>
suspend fun addChild(name: String, birthDate: LocalDate?): ChildProfile
suspend fun updateChild(child: ChildProfile)
suspend fun deleteChild(childId: Long)
```

#### TaskRepository

```kotlin
fun observeTodayTasks(childId: Long, today: LocalDate): Flow<List<TaskWithStatus>>
fun observeTasks(childId: Long): Flow<List<LearningTask>>
suspend fun getTask(taskId: Long): LearningTask?
suspend fun createTask(task: LearningTask): Long
suspend fun updateTask(task: LearningTask)
suspend fun enableTask(taskId: Long, enabled: Boolean)
suspend fun deleteTask(taskId: Long, keepRecords: Boolean)
suspend fun getAllowedDates(task: LearningTask): List<LocalDate>
```

#### CompletionRecordRepository

```kotlin
suspend fun getRecord(taskId: Long, childId: Long, date: LocalDate): CompletionRecord?
fun observeTodaySummary(childId: Long, today: LocalDate): Flow<TodayCompletionSummary>
fun observeRangeStatistics(childId: Long, start: LocalDate, end: LocalDate): Flow<List<CompletionRecord>>
suspend fun updateStatus(
    taskId: Long,
    childId: Long,
    date: LocalDate,
    status: CompletionStatus,
    completedAt: Long?,
    childNotes: String?,
    parentNotes: String?,
    recordedBy: RecordedBy
): CompletionRecord
suspend fun addEvidence(recordId: Long, evidenceUri: String)
suspend fun updateNotes(recordId: Long, childNotes: String?, parentNotes: String?)
```

#### ReminderScheduler

```kotlin
suspend fun scheduleForTask(task: LearningTask)
suspend fun scheduleForRules(tasks: List<LearningTask>, rules: List<ReminderRule>)
suspend fun cancelForTask(taskId: Long)
suspend fun snooze(taskId: Long, childId: Long, delayMinutes: Int)
suspend fun rescheduleAll()
```

### 6.2 本地持久化方案

v1.0 不依赖账号和云端数据库。本地数据分成三类保存：

| 数据类型 | 存储方式 | 原因 |
| --- | --- | --- |
| 孩子资料、学习任务、完成记录、提醒规则 | Room SQLite | 结构化数据需要查询、排序、统计，也应该支持外键约束。 |
| 当前孩子、界面偏好、提醒开关、是否首次启动 | Jetpack DataStore Preferences | 单键值状态，适合放在轻量偏好存储里。 |
| 头像、打卡证据图片、导出的记录文本 | App `files` / `images` 目录 | 不放进 SQLite，避免数据库变成大文件。数据库里只保存本地 URI/文件名。 |

初始化：

```kotlin
val database = Room.databaseBuilder(
    context,
    LearningDatabase::class.java,
    "learning_reminder.db"
).fallbackToDestructiveMigrationOnDowngrade().build()
```

> 上线后不允许使用破坏性迁移清除用户数据。第一版可以先限定只支持正向迁移，后续升级必须写 `Migration(olderVersion, newerVersion)`。

关键约束：

1. 所有业务数据都保存在 Android App 私有目录里，其他 App 不能直接读取。
2. `LearningTask.childId` 外键指向 `ChildProfile.id`。
3. `CompletionRecord.taskId` 外键指向 `LearningTask.id`。
4. `CompletionRecord` 使用唯一索引 `(childId, taskId, date)`，避免同一天重复记录。
5. 数据表版本一开始为 1，字段新增时通过新表预留字段或迁移脚本扩展。
6. `Room` 中 `LocalDate`、`LocalTime` 用 ISO 字符串保存，避免不同设备时区导致阅读困难。
7. 图片只保存相对路径，不把图片数据直接塞进数据库。

典型查询：

```kotlin
@Query("""
SELECT t.id,
       t.title,
       t.reminderTime,
       c.status,
       c.completedAt
FROM learning_task t
LEFT JOIN completion_record c
  ON c.taskId = t.id
 AND c.date = :date
WHERE t.childId = :childId
  AND t.enabled = 1
  AND t.startDate <= :date
  AND t.endDate >= :date
  AND (t.repeatRule = 'DAILY' OR t.repeatRule LIKE :dayPattern)
ORDER BY t.reminderTime ASC
""")
fun observeTodayTaskStatuses(childId: Long, date: String, dayPattern: String): Flow<List<TodayTaskStatus>>
```

重启与提醒：

1. 系统闹钟不是数据源的最终出处。
2. 打开 App、新建任务、修改提醒、重启设备后，都从 Room 重新读取任务并注册 `AlarmManager`。
3. `BootReceiver` 只做“重新调度提醒”，不修改任务和完成记录。
4. 如果用户手动改变完成状态，完成结果写回 Room；提醒如果还需要重复触发，继续读取同样的任务数据。

用户删除行为：

| 操作 | 建议结果 |
| --- | --- |
| 删除任务 | 默认也删除该任务对应的完成记录；提示家长。 |
| 删除孩子 | 先列出任务和完成记录数量；只有用户二次确认后才一起删除。 |
| 卸载 App | 本地私有数据会被系统清理，应用没有恢复手段。 |
| 更换 App 版本 | 保留本地数据；如果数据结构变化，走 Room Migration。 |

导出与备份：

1. v1.0 可以不做云同步，但至少支持“导出 JSON”。
2. 导出内容包含孩子、任务、完成记录和提醒配置。
3. 文件默认保存到用户选择的位置或公共 Download 目录，需要通过系统文件接口。
4. 导入时按任务和完成记录的唯一键去重，不覆盖已有记录，除非用户明确选择填空。
5. 图片导出可以先支持批量压缩打包，后续如拍照/上传功能落地时一起优化。

备元工具：

```kotlin
suspend fun exportAllData(targetUri: Uri)
suspend fun importJsonBackup(sourceUri: Uri, replaceExisting: Boolean)
```

测试建议：

1. 同一孩子同一任务同一天重复插入时，应触发唯一索引冲突，而不是生成两条记录。
2. 删除任务后，LEFT JOIN 查询不应该报 NPE。
3. 修改 `LocalDate` 存储格式后，老数据应能通过迁移脚本转换。
4. 杀掉 App 后重新打开，任务和完成记录能从 SQLite 正常读取。
5. 重启手机后，提醒重新注册，闹钟仍然可以触发。

### 6.3 需要管理的系统权限

| 权限 | 用途 |
| --- | --- |
| POST_NOTIFICATIONS | 发送提醒通知 |
| SCHEDULE_EXACT_ALARM | 提醒时间需要精确时使用 |
| RECEIVE_BOOT_COMPLETED | 重启后 schedule 提醒 |
| REQUEST_IGNORE_BATTERY_OPTIMIZATIONS | 引导家长手动豁免清理后台 |
| CAMERA / READ_MEDIA_IMAGES | 只有考虑上传完成依据时才使用 |

## 7. 可选云同步 API

> 仅在用户主动开同步时使用。v1.0 不强制实现。

### 7.1 服务简介

目标：支持同一个家长在多部手机上查看孩子的任务和完成记录，或老师/亲戚只读查看。

建议路径版本：`/api/v1`  
传输：HTTPS + JSON。

### 7.2 REST API 规划

#### 鉴权

```http
POST /api/v1/auth/login
```

请求：

```json
{
  "phone": "13800000000",
  "verificationCode": "123456"
}
```

响应：

```json
{
  "accessToken": "aaaa.bbb.ccc",
  "refreshToken": "rrrr",
  "expiresAt": 1770000000000
}
```

#### 家庭和孩子

```http
GET    /api/v1/family
GET    /api/v1/family/{familyId}
POST   /api/v1/family/{familyId}/children
PATCH  /api/v1/family/{familyId}/children/{childId}
DELETE /api/v1/family/{familyId}/children/{childId}
```

响应示例：

```json
{
  "id": "child_001",
  "familyId": "family_001",
  "name": "阿里",
  "birthDate": "2017-10-22",
  "createdAt": 1770000000000
}
```

#### 学习任务

```http
GET    /api/v1/family/{familyId}/children/{childId}/tasks
POST   /api/v1/family/{familyId}/children/{childId}/tasks
GET    /api/v1/family/{familyId}/children/{childId}/tasks/{taskId}
PATCH  /api/v1/family/{familyId}/children/{childId}/tasks/{taskId}
DELETE /api/v1/family/{familyId}/children/{childId}/tasks/{taskId}
```

请求体示例：

```json
{
  "title": "晚上朗读 20 分钟",
  "subject": "READING",
  "startDate": "2026-10-22",
  "endDate": "2026-12-22",
  "reminderTime": "19:30",
  "repeatRule": "MON,TUE,WED,THU,FRI",
  "suggestedMinutes": 20,
  "completionPolicy": "PARENT_REPORT",
  "enabled": true
}
```

#### 完成记录

```http
GET   /api/v1/family/{familyId}/children/{childId}/records?from=2026-10-22&to=2026-10-28&taskId=task_001
POST  /api/v1/family/{familyId}/children/{childId}/records
PATCH /api/v1/family/{familyId}/children/{childId}/records/{recordId}
```

请求体示例：

```json
{
  "taskId": "task_001",
  "date": "2026-10-22",
  "status": "DONE",
  "completedAt": 1770000000000,
  "childNotes": "读完 2 篇课文",
  "parentNotes": "完成得不错"
}
```

响应：

```json
{
  "id": "record_001",
  "taskId": "task_001",
  "childId": "child_001",
  "date": "2026-10-22",
  "status": "DONE",
  "completedAt": 1770000000000,
  "lastUpdated": 1770000000100
}
```

### 7.3 批量同步 API

适合解决多设备差异。

```http
GET  /api/v1/family/{familyId}/changes?cursor=0
POST /api/v1/family/{familyId}/changes
```

原则：

1. 客户端保存 `lastSyncCursor`。
2. 服务端按 `lastUpdated` 返回新增/修改/删除。
3. 客户端发现冲突时，默认以下优先级：
   - 本地状态比 `NOT_STARTED` 更新时，保留本地。
   - 服务端 `lastUpdated` 更新时，用服务端覆盖。
   - 记录来源字段冲突时，以记录者为准：家长 > 孩子 > 系统。
4. 所有记录带 `lastUpdated`，忽略空同步窗口。

### 7.4 错误格式

```json
{
  "code": "TASK_NOT_FOUND",
  "message": "Learning task not found",
  "traceId": "trace_abc123"
}
```

常用 code：

| code | 说明 |
| --- | --- |
| UNAUTHORIZED | 登录无效 |
| FORBIDDEN | 没有权限 |
| TASK_NOT_FOUND | 任务不存在 |
| RECORD_NOT_FOUND | 打卡记录不存在 |
| CHILD_NOT_FOUND | 孩子不存在 |
| DUPLICATE_RECEIVED | 同一天同任务重复记录 |
| SERVER_ERROR | 服务端异常 |

## 8. 状态与规则

任务可见规则：

1. 任务未开始 -> 可创建。
2. 日期在 startDate 至 endDate 内且 repeatRule 匹配 -> 当天可见。
3. 用户手动 `enabled = false` -> 隐藏。
4. 完成记录 status 为 `SKIP_TODAY` / `ASK_LEAVE` -> 正常可见，但不算未完成。
5. 完成后仍然可见，方便修改或查看详情。

完成率计算：

```text
今日完成率 = 已完成数 / 当天应做任务数

应做任务数 = startDate <= today && endDate >= today && repeatRule 匹配 && enabled
已完成数 = status == DONE
```

注意：

- `PARTIAL` 可以算 0.5，也可以在报表中单独说明。
- 请假和跳过不参与分数。
- 未完成只统计当天已过提醒时间但仍非 DONE 的任务。

## 9. 里程碑安排

### v0.1：PRD + 结构确认

交付：

- 本文 PRD。
- 数据表和 API 设计。
- Android 工程目录规划。

### v1.0：本地可用版本

交付：

- 可运行 Android App。
- 家长本地录入孩子基本信息。
- 手动布置学习任务。
- 自动生成每日任务。
- 手动打卡，完成 / 部分完成 / 请假 / 没做。
- 保存历史，查看 7 天完成率。
- 发送本地通知提醒。

### v1.1：体验增强

- 孩子专属简化界面。
- 拍照上传。
- 更详细周报。
- 提醒死区/免打扰。
- 一键导出记录。

### v2.0：多设备同步

- 服务端 API。
- 登录与家庭授权。
- 爸爸妈妈一并查看。
- 老师或第三方只读查看。

## 10. 验收要点

1. 家长可以创建一个学习任务，选择每天或部分工作日。
2. App 重启后提醒不会丢失。
3. 完成率不会把未启用或不在周期内的任务算失败。
4. 家长可以给同一个任务在当天修改完成状态，而不会产生重复记录。
5. 历史记录可以按日期或任务查看。
6. 通知权限缺失时给用户明确提示。
7. 单机使用时，不请求账号、云同步和远程存储。
8. 设置里的目标精度不会造成界面文字溢出或按钮遮挡。

