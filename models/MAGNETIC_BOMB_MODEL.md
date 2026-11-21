# 模型交接：磁吸炸弹

- 目标平台：Java 1.20.1 Forge
- 可编辑模型：`models/magnetic_bomb.bbmodel`
- 游戏模型：`src/main/resources/assets/silencecute_skill/models/item/magnetic_bomb.json`
- 贴图：`src/main/resources/assets/silencecute_skill/textures/entity/magnetic_bomb.png`，32×32
- 模型朝向：顶面朝 +Y，落地时磁吸脚朝 -Y
- 整体尺寸：宽 8.0、高 4.35、长 7.3（1/16 方块为单位）

## 骨骼树
- root，origin (8, 8, 8)
  - body，origin (8, 8, 8)

## 独立立体部件
- charge_front / charge_back：两块塑胶炸药
- strap_left / strap_right：固定束带
- detonator / indicator：起爆器与状态灯
- wire_left / wire_right：短导线
- magnet_left / magnet_right：底部磁吸脚

无骨骼动画；飞行翻转和粘附朝向由 `MagneticBombRenderer` 驱动。
