import copy
import json
import math
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / "src/main/java/lol/silencecute"
ASSETS = ROOT / "src/main/resources/assets/silencecute_skill"
clips = json.loads((ASSETS / "animations/throwable/m67_grenade.animation.json").read_text())["animations"]
bones = {b["name"]: b for b in json.loads((ASSETS / "geo_models/throwable/m67_grenade_geo.json").read_text())["minecraft:geometry"][0]["bones"]}


def identity():
    return [[float(i == j) for j in range(4)] for i in range(4)]


def multiply(a, b):
    return [[sum(a[i][k] * b[k][j] for k in range(4)) for j in range(4)] for i in range(4)]


def translate(v):
    m = identity()
    for i in range(3):
        m[i][3] = v[i]
    return m


def scale(v):
    if isinstance(v, (float, int)):
        v = [v] * 3
    m = identity()
    for i in range(3):
        m[i][i] = v[i]
    return m


def rotate(v):
    m = identity()
    for axis in (2, 1, 0):
        angle = math.radians(v[axis])
        c, s = math.cos(angle), math.sin(angle)
        a, b = ((1, 2), (2, 0), (0, 1))[axis]
        r = identity()
        r[a][a] = r[b][b] = c
        r[a][b], r[b][a] = -s, s
        m = multiply(m, r)
    return m


def point(m, p):
    return [sum(m[i][j] * (p + [1])[j] for j in range(4)) for i in range(3)]


pose = {name: dict(values) for name, values in clips["static_idle"]["bones"].items()}
for name, values in clips["unlock_safe_loop"]["bones"].items():
    pose.setdefault(name, {}).update(values)


def bone_matrix(name, sampled_pose=pose):
    b = bones[name]
    p = bones.get(b.get("parent"))
    m = bone_matrix(p["name"], sampled_pose) if p else multiply(translate([0, 1.5, 0]), rotate([0, 0, 180]))
    if not p:
        view = bones["idle_view"]["pivot"]
        m = multiply(m, translate([-view[0] / 16, view[1] / 16, -view[2] / 16]))
    t = sampled_pose.get(name, {})
    pos, pivot = t.get("position", [0, 0, 0]), b["pivot"]
    parent = p["pivot"] if p else [0, 24, 0]
    m = multiply(m, translate([(pivot[0] - parent[0] + pos[0]) / 16,
                              (parent[1] - pivot[1] - pos[1]) / 16,
                              (pivot[2] - parent[2] + pos[2]) / 16]))
    m = multiply(m, rotate(b.get("rotation", [0, 0, 0])))
    m = multiply(m, rotate(t.get("rotation", [0, 0, 0])))
    return multiply(m, scale(t.get("scale", [1, 1, 1])))


left = multiply(bone_matrix("lefthand_pos"), rotate([0, 0, 180]))
shoulder = point(left, [5 / 16, 2 / 16, 0])
fist = point(left, [5 / 16, 14 / 16, 0])
assert fist[2] < shoulder[2] - 0.65, (shoulder, fist)
assert -0.65 < fist[0] < 0.1 and -0.65 < fist[1] < 0.1 and fist[2] < -0.4, fist
previous_pose = copy.deepcopy(pose)
previous_pose["lefthand"]["position"] = [14, -12, 1]
previous_left = multiply(bone_matrix("lefthand_pos", previous_pose), rotate([0, 0, 180]))
for arm_point in ([5 / 16, 2 / 16, 0], [5 / 16, 14 / 16, 0]):
    previous = point(previous_left, arm_point)
    current = point(left, arm_point)
    assert math.isclose(current[1] - previous[1], -2 / 16, abs_tol=1e-8), (previous, current)
    assert math.isclose(current[0], previous[0], abs_tol=1e-8), (previous, current)
    assert math.isclose(current[2], previous[2], abs_tol=1e-8), (previous, current)
right = point(bone_matrix("righthand_pos"), [0, 0, 0])
grenade = point(bone_matrix("grenade"), [0, 0, 0])
assert right[2] > 0 and grenade[2] > 0, (right, grenade)
assert pose["ring"]["scale"] == 0
assert clips["unlock_safe"]["animation_length"] == 1.0833
assert clips["throw"]["animation_length"] == 0.5
print("姿势检查通过：左臂朝前且下移 2 个模型单位，右手与手雷位于镜头外，拉环保持隐藏")

client = (SRC / "LunaGrenadeClient.java").read_text()
compat = (SRC / "TaczGrenadeCompat.java").read_text()
input_compat = (SRC / "TaczCompat.java").read_text()
service = (SRC / "LunaGrenadeService.java").read_text()
ballistics = (SRC / "LunaGrenadeBallistics.java").read_text()
entity = (SRC / "LunaGrenadeEntity.java").read_text()
assert not (SRC / "LunaGrenadeTrajectory.java").exists()
for java_file in SRC.glob("*.java"):
    assert "LunaGrenadeTrajectory" not in java_file.read_text(), java_file
assert "RenderLevelStageEvent" not in client
assert "nextVelocity" not in ballistics
assert "InputEvent.MouseButton.Pre" in client
assert "requireAttackRelease" in client
assert "isShootPhysicallyDown" in client
assert "GunShootEvent" in compat and "GunFireEvent" in compat
assert "blocksGunfire" in compat and "clientBlocker.test(shooter)" in compat
assert 'getField("SHOOT_KEY")' in input_compat
assert "shootKey.setDown(false)" in input_compat
assert "LunaGrenadeBallistics.origin(player, 1.0F)" in service
assert "LunaGrenadeBallistics.velocity(player, 1.0F)" in service
assert "THROW_VELOCITY = 1.05F" in service
assert "THROW_ARC_DEGREES = -6.0F" in service
assert "GRAVITY = 0.045F" in ballistics
assert "return LunaGrenadeBallistics.GRAVITY;" in entity
assert "super.tick();" in entity
assert "grenade.shoot(velocity.x, velocity.y, velocity.z, (float) velocity.length(), 0.0F)" in service
assert '"Luna grenade priming slowdown", -0.50D' in service
assert "FULL_FUSE_TICKS = 5 * 20" in service
assert "AUTO_THROW_REMAINING_TICKS = 12" in service
assert "COOLDOWN_TICKS" not in service
assert "RANDOM.nextBoolean()" in service
assert "MAX_DAMAGE = 110.0F" in entity
assert "DAMAGE_RADIUS = 8.0D" in entity
print("输入检查通过：独立扳机、双端开火事件与松键恢复逻辑保留")
print("抛物线移除检查通过：无绘制入口和预测残留，实际投掷参数与温雷机制保持不变")

magnetic = (SRC / "MagneticBombEntity.java").read_text()
hud = (SRC / "SkillHudOverlay.java").read_text()
events = (SRC / "ClientEvents.java").read_text()
sync_packet = (SRC / "LunaGrenadeSyncPacket.java").read_text()
assert "MAX_CHARGES = 2" in service, "手雷上限必须为两枚"
assert "RECHARGE_TICKS = 45 * 20" in service, "每枚手雷必须独立恢复 45 秒"
assert "RECHARGING.computeIfAbsent" in service and ".add(now + RECHARGE_TICKS)" in service
assert "slot != player.getInventory().selected" in service
assert "stowForWeaponSwitch" in client and "Action.CANCEL" in client
assert "LunaGrenadeClient.grenadeCharges()" in hud and "2/2" in hud
assert "int charges" in sync_packet and "int rechargeMillis" in sync_packet
assert "ModDamageTypes.explosion(level(), this, owner)" in entity
assert "ModDamageTypes.explosion(level(), this, owner)" in magnetic
assert "damageSources().explosion(" not in entity and "damageSources().explosion(" not in magnetic
damage_type = ROOT / "src/main/resources/data/silencecute_skill/damage_type/luna_explosion.json"
armor_tag = ROOT / "src/main/resources/data/minecraft/tags/damage_type/bypasses_armor.json"
assert json.loads(damage_type.read_text())["message_id"] == "explosion"
assert json.loads(damage_type.read_text())["scaling"] == "never"
assert "silencecute_skill:luna_explosion" in armor_tag.read_text()
assert "serverConfirmed" in events and "未检测到服务器端模组" in events
assert 'PROTOCOL = "7"' in (SRC / "ModNetwork.java").read_text()
print("r76 检查通过：两枚储量独立恢复、换槽自动收雷、爆炸伤害改用自定义类型、服务器缺模组提醒")
