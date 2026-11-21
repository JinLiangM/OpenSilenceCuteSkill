# SilenceCute Skill（哦对了 这是大肥鱼给我写的README 有些命名很意义不明是正常的）

> Minecraft 1.20.1 / Forge 的「三角洲干员」技能 Mod —— 选一个干员，带一整套投掷物、位移技能和配套 HUD。

| 项目 | 内容 |
|---|---|
| Mod ID | `silencecute_skill` |
| 版本 | `Private-Beta 1.0` |
| 游戏版本 | Minecraft **1.20.1** |
| 加载器 | **Forge 47.4.x** |
| Java | **17** |
| 许可证 | GNU GPL-3.0 |

---

## 干员与技能

### 露娜 Luna（`luna`）
- **手雷**：拿在手上引信就开始跳，剩 0.6 秒自动脱手；爆炸伤害按距离线性衰减
- **电箭**：蓄力决定初速，落地展开电流场；范围内被感电的目标每 0.5 秒掉一次血（按最大生命百分比，有上下限）
- **侦察箭**：落地后持续扫描，标记范围内敌人

### 威龙 Vyron（`vyron`）
- **冲刺**：短距离高速位移，可自动跨台阶，击杀返还冷却
- **C4 磁吸炸弹**：抛出后吸附到第一个接触的方块或生物，3 秒引信；贴水平面时按投掷瞬间的视角摆放朝向，贴在生物身上吃满伤害但不击退，且玩家身上会有专属警报 HUD
- **终极技能**

### 疾风 Tempst（`tempst`）
- **战术翻滚**：位移 + 落地摔落减免（被动 / 楼梯有额外距离与速度加成）
- **终极技能**：开发中，默认关闭（见配置 `tempst_ultimate.enabled`）

---

## 按键

| 键 | 作用 |
|---|---|
| `V` | 位移技能（威龙冲刺 / 疾风翻滚） |
| `G` | C4 磁吸炸弹 |
| `X` | 终极技能 |
| 鼠标左键 / 右键 | 露娜的投掷物：按住蓄力、松开投出 |

---

## 指令

| 指令 | 说明 |
|---|---|
| `/deltaforce <luna\|vyron\|tempst>` | 切换干员（需授权） |
| `/Admin CheckPlayer <玩家>` | 查看权限信息 |
| `/Admin TestPlayer <玩家> true\|false` | 授权 / 取消「可自选干员」 |
| `/Admin setSkill <玩家> <干员>` | 把指定玩家锁定到某个干员 |
| `/Admin TestModule true\|false` | 开关「技能无冷却」（仅管理员） |
| `/Admin TestHUD` | 触发 HUD 测试 |

---

## 权限（鉴权）

- **服务端权威**：`access/AccessService` 是唯一裁决方，数据存在存档 `SavedData`（`AccessData`）
- **管理员** = OP 权限等级 ≥ 2，或单人存档房主
- **已授权玩家** = 被 `/Admin TestPlayer <玩家> true` 授权，可自选干员
- **未授权玩家**：干员锁死为露娜
- 状态变化时通过 `AccessSyncPacket` 下发（状态没变不会重复发包），客户端只做显示

---

## 配置

两份 Forge 配置：

| 文件 | 类型 | 内容 |
|---|---|---|
| `operator_skills-client.toml` | CLIENT | HUD、界面等视觉参数 |
| `operator_skills-server.toml` | SERVER | **全部技能数值**，会自动同步给客户端 |

服务端配置分 7 组、共 62 项：

`[c4]` · `[luna_grenade]` · `[luna_shock_arrow]` · `[luna_recon_arrow]` · `[dash]` · `[tempst_roll]` · `[tempst_ultimate]`

- 单人：`saves/<世界>/serverconfig/operator_skills-server.toml`
- 专用服务器：`<世界目录>/serverconfig/operator_skills-server.toml`
- 单位约定：时间 **秒**，速度 **格/秒**，距离 **格**，伤害 **点**（20 = 玩家满血）

---

## 从源码构建

```bash
# 需要 JDK 17
./gradlew build        # 产物在 build/libs/ 下（silencecute_skill-*.jar）
./gradlew runClient    # 启动开发客户端
```

- `lib/mixin-0.8.5-processor.jar` 是 Mixin 注解处理器，已随仓库附带
- 首次构建需要联网下载 ForgeGradle 依赖

---

## 目录结构

```
src/main/java/lol/silencecute
├── access/      鉴权与权限（AccessService / AccessData）
├── api/         干员枚举、伤害类型等公共 API
├── client/      客户端渲染、HUD、动画、音效、界面
├── command/     /Admin 与 /deltaforce 指令
├── component/   通用组件
├── config/      client / server 配置定义
├── entity/      投掷物实体（C4、手雷、电箭、侦察箭）
├── event/       被动与事件处理
├── manager/     各技能服务端逻辑（冷却、充能、施放）
├── mixin/       Mixin 补丁
├── modules/     模块化 HUD 组件
├── network/     网络包
├── registry/    注册表（物品、实体、音效、粒子…）
└── util/
```

---

## 许可证与致谢

本项目以 **GNU GPL-3.0** 发布，详见 [`LICENSE`](LICENSE)。

C4（磁吸炸弹）的投掷物物理、粘性吸附、以及基岩模型渲染管线，参考并移植自以下同样以 **GPL-3.0** 发布的项目的实现与约定：

- **LesRaisins Tactical Equipements**（`lrtactical`，作者 xjqsh）
- **Timeless and Classics Zero**（TACZ）

Minecraft 是 Mojang Studios 的商标，本项目与其无隶属关系，也未包含任何 Mojang 的专有资源。

---

## 已知问题

- 疾风终极技能尚未完成，默认关闭
- HUD 的「警告圈」半径（电箭危险半径 + 3 格）目前写死在客户端，未进配置
