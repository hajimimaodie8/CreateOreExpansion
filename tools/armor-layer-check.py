#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
armor-layer-check.py —— COE / CEWS 盔甲「装备层贴图」UV 区域自检（只读）

背景
----
1.21.1 的人形盔甲层贴图是 **64×32** 布局（不是玩家皮肤的 64×64）。四个 UV 块分别被
四个模型盒采样，模型定义见反编译源码：

  net/minecraft/client/model/HumanoidArmorModel.java:19-33
      createBodyLayer(CubeDeformation) 覆写了 right_leg / left_leg 为
      CubeListBuilder.create().texOffs(0, 16) ... 4x12x4
  net/minecraft/client/model/HumanoidModel.java:77-115
      head  texOffs(0, 0)   8x8x8
      hat   texOffs(32, 0)  8x8x8（deformation.extend(0.5)）
      body  texOffs(16, 16) 8x12x4
      right_arm texOffs(40, 16) 4x12x4
      left_arm  texOffs(40, 16).mirror() 4x12x4   <-- 与右臂同一块，水平镜像
      right_leg / left_leg 见 HumanoidArmorModel
  net/minecraft/client/renderer/entity/layers/HumanoidArmorLayer.java:107-128
      setPartVisibility：HEAD -> head + hat；CHEST -> body + 双臂；
      LEGS -> body + 双腿；FEET -> 双腿

所以本脚本按「模型盒」而不是按「看着像什么」来判定区域归属。

本脚本做什么
------------
1. 扫描 coe / cews 资源目录下所有 `textures/models/armor/*_layer_1.png` 与 `*_layer_2.png`
   （也支持 `--root` 指向任意目录，例如解包出来的原版 `assets/minecraft/textures/models/armor`）。
2. 用纯标准库解码 PNG，逐区域（头/帽/双腿/躯干/双臂/未使用区）统计**不透明像素数**。
3. 按「这块区域在这个层里会不会被渲染」断言：
     layer_1  可渲染区 = 头 / 帽 / 双腿 / 躯干 / 双臂     （头盔 + 胸甲 + 靴子共用一张图）
     layer_2  可渲染区 = 躯干 / 双腿                     （只有护腿用这张图）
   把「不该有内容却有内容」与「本应有内容却全透明」都列为可疑。
4. 打印人类可读表格 + 结论；发现可疑时给出**具体到文件与区域**的提示。
5. 附带检查物品图标 `textures/item/<set>_{helmet,chestplate,leggings,boots}.png`：
   尺寸、不透明像素数、alpha 形状指纹；同一套里两个图标的 alpha 形状完全相同 => 可疑（张冠李戴）。

本脚本**只读**，不修改任何文件，不写临时文件。纯标准库（struct / zlib / argparse / pathlib），无第三方依赖。

用法
----
    python tools/armor-layer-check.py
    python tools/armor-layer-check.py --root <某个包含 textures/models/armor 的目录>
    python tools/armor-layer-check.py --root <直接放着 *_layer_1.png 的目录> --no-icons
    python tools/armor-layer-check.py --verbose      # 额外打印每个面(上/下/右/前/左/后)的计数

退出码：0 = 没有发现问题；1 = 至少有一条错误级问题；2 = 参数/读取失败。
"""

from __future__ import annotations

import argparse
import struct
import sys
import zlib
from pathlib import Path

# --------------------------------------------------------------------------------------
# 1) 极简 PNG 解码（8 位为主，兼容 1/2/4/16 位与调色板；不支持 Adam7 隔行）
# --------------------------------------------------------------------------------------

_PNG_SIG = b"\x89PNG\r\n\x1a\n"


def _paeth(a: int, b: int, c: int) -> int:
    p = a + b - c
    pa, pb, pc = abs(p - a), abs(p - b), abs(p - c)
    if pa <= pb and pa <= pc:
        return a
    if pb <= pc:
        return b
    return c


def read_png_rgba(path: Path):
    """返回 (width, height, bytearray RGBA)。失败抛 ValueError。"""
    raw_file = path.read_bytes()
    if raw_file[:8] != _PNG_SIG:
        raise ValueError("不是 PNG（签名不匹配）")

    pos = 8
    idat = bytearray()
    width = height = depth = ctype = None
    interlace = 0
    palette = None
    trns = None

    while pos + 8 <= len(raw_file):
        (length,) = struct.unpack(">I", raw_file[pos:pos + 4])
        ctag = raw_file[pos + 4:pos + 8]
        chunk = raw_file[pos + 8:pos + 8 + length]
        pos += 12 + length
        if ctag == b"IHDR":
            width, height, depth, ctype, _comp, _filt, interlace = struct.unpack(">IIBBBBB", chunk)
        elif ctag == b"PLTE":
            palette = chunk
        elif ctag == b"tRNS":
            trns = chunk
        elif ctag == b"IDAT":
            idat += chunk
        elif ctag == b"IEND":
            break

    if width is None:
        raise ValueError("缺少 IHDR")
    if interlace:
        raise ValueError("Adam7 隔行 PNG 不支持（Minecraft 贴图不会是隔行的）")
    if ctype not in (0, 2, 3, 4, 6):
        raise ValueError("未知颜色类型 %r" % (ctype,))

    channels = {0: 1, 2: 3, 3: 1, 4: 2, 6: 4}[ctype]
    bits_per_pixel = channels * depth
    bpp = max(1, (bits_per_pixel + 7) // 8)
    stride = (width * bits_per_pixel + 7) // 8

    data = zlib.decompress(bytes(idat))
    expected = (stride + 1) * height
    if len(data) < expected:
        raise ValueError("IDAT 数据不足：%d < %d" % (len(data), expected))

    lines = []
    prev = bytearray(stride)
    p = 0
    for _y in range(height):
        ft = data[p]
        p += 1
        line = bytearray(data[p:p + stride])
        p += stride
        if ft == 1:
            for i in range(bpp, stride):
                line[i] = (line[i] + line[i - bpp]) & 0xFF
        elif ft == 2:
            for i in range(stride):
                line[i] = (line[i] + prev[i]) & 0xFF
        elif ft == 3:
            for i in range(stride):
                a = line[i - bpp] if i >= bpp else 0
                line[i] = (line[i] + ((a + prev[i]) >> 1)) & 0xFF
        elif ft == 4:
            for i in range(stride):
                a = line[i - bpp] if i >= bpp else 0
                c = prev[i - bpp] if i >= bpp else 0
                line[i] = (line[i] + _paeth(a, prev[i], c)) & 0xFF
        elif ft != 0:
            raise ValueError("非法滤波器类型 %d" % ft)
        lines.append(bytes(line))
        prev = line

    def unpack_row(line: bytes):
        """把一行解成 samples 列表；返回 (samples, 是否已缩放到 0..255)。"""
        count = width * channels
        if depth == 8:
            return list(line[:count]), True
        if depth == 16:
            return [line[i * 2] for i in range(count)], True
        per_byte = 8 // depth
        mask = (1 << depth) - 1
        vals = []
        for byte in line:
            for k in range(per_byte):
                vals.append((byte >> (8 - depth * (k + 1))) & mask)
        vals = vals[:count]
        if ctype == 3:
            return vals, False  # 调色板索引不能缩放
        scale = 255.0 / mask
        return [int(round(v * scale)) for v in vals], True

    rgba = bytearray(width * height * 4)
    for y in range(height):
        samples, _scaled = unpack_row(lines[y])
        base = y * width * 4
        for x in range(width):
            o = x * channels
            if ctype == 0:       # 灰度
                g = samples[o]
                r = gg = b = g
                a = 255
            elif ctype == 4:     # 灰度 + alpha
                r = gg = b = samples[o]
                a = samples[o + 1]
            elif ctype == 2:     # RGB
                r, gg, b = samples[o], samples[o + 1], samples[o + 2]
                a = 255
            elif ctype == 6:     # RGBA
                r, gg, b, a = samples[o], samples[o + 1], samples[o + 2], samples[o + 3]
            else:                # 调色板
                idx = samples[o]
                if palette is None or idx * 3 + 2 >= len(palette):
                    r = gg = b = 0
                    a = 0
                else:
                    r, gg, b = palette[idx * 3], palette[idx * 3 + 1], palette[idx * 3 + 2]
                    a = trns[idx] if (trns is not None and idx < len(trns)) else 255
            d = base + x * 4
            rgba[d] = r
            rgba[d + 1] = gg
            rgba[d + 2] = b
            rgba[d + 3] = a

    return width, height, rgba


# --------------------------------------------------------------------------------------
# 2) UV 区域表（来自 HumanoidArmorModel / HumanoidModel / HumanoidArmorLayer，见文件头）
# --------------------------------------------------------------------------------------

# key, 中文名, x, y, w, h, 采样它的模型盒
REGIONS = [
    ("head",   "头 head",     0,  0, 32, 16, "head 盒 8x8x8（头盔）"),
    ("hat",    "帽 hat",     32,  0, 32, 16, "hat 盒 8x8x8（头盔，比 head 大 0.5px）"),
    ("legs",   "双腿 leg",    0, 16, 16, 16, "左右腿共用的 leg 盒 4x12x4（靴子；护腿也用它）"),
    ("body",   "躯干 body",  16, 16, 24, 16, "body 盒 8x12x4（胸甲躯干；护腿的腰胯）"),
    ("arms",   "双臂 arm",   40, 16, 16, 16, "左右臂共用的 arm 盒 4x12x4（护臂；左臂镜像）"),
    ("unused", "未使用区",   56, 16,  8, 16, "64x32 布局里没有任何模型盒采样它"),
]

REGION_BY_KEY = {r[0]: r for r in REGIONS}

# 每个「层」实际会被渲染的区域（依据 HumanoidArmorLayer.setPartVisibility + 槽位）
RENDERED_REGIONS = {
    "layer_1": ("head", "hat", "legs", "body", "arms"),
    "layer_2": ("body", "legs"),
}

# 各模型盒的几何参数（w, h, d），用于拆「上/下/右/前/左/后」六个面
BOX_GEOMETRY = {
    "head": (0, 0, 8, 8, 8),
    "hat": (32, 0, 8, 8, 8),
    "body": (16, 16, 8, 12, 4),
    "arms": (40, 16, 4, 12, 4),
    "legs": (0, 16, 4, 12, 4),
}


def box_faces(x, y, w, h, d):
    """按 Minecraft 盒展开规则返回六个面 (名称, x, y, w, h)。"""
    return [
        ("上 top", x + d, y, w, d),
        ("下 bottom", x + d + w, y, w, d),
        ("右 right", x, y + d, d, h),
        ("前 front", x + d, y + d, w, h),
        ("左 left", x + d + w, y + d, d, h),
        ("后 back", x + d + w + d, y + d, w, h),
    ]


def count_opaque(width, height, rgba, x, y, w, h):
    """统计矩形内 alpha>0 的像素数（矩形超出图片则只算交集）。返回 (opaque, total)。"""
    x0, y0 = max(0, x), max(0, y)
    x1, y1 = min(width, x + w), min(height, y + h)
    if x1 <= x0 or y1 <= y0:
        return 0, 0
    n = 0
    for yy in range(y0, y1):
        base = yy * width * 4
        for xx in range(x0, x1):
            if rgba[base + xx * 4 + 3] > 0:
                n += 1
    return n, (x1 - x0) * (y1 - y0)


# --------------------------------------------------------------------------------------
# 3) 扫描
# --------------------------------------------------------------------------------------

def find_armor_files(root: Path):
    """在 root 下找出所有 *_layer_1.png / *_layer_2.png。"""
    hits = []
    if not root.is_dir():
        return hits
    direct = sorted(root.glob("*_layer_1.png")) + sorted(root.glob("*_layer_2.png"))
    if direct:
        return direct
    for p in sorted(root.rglob("*_layer_*.png")):
        if p.parent.name == "armor" or "models" in p.parts:
            hits.append(p)
    return hits


def layer_of(path: Path):
    name = path.stem
    if name.endswith("_layer_1"):
        return "layer_1"
    if name.endswith("_layer_2"):
        return "layer_2"
    return None


def base_of(path: Path):
    name = path.stem
    for suffix in ("_layer_1", "_layer_2"):
        if name.endswith(suffix):
            return name[: -len(suffix)]
    return name


# --------------------------------------------------------------------------------------
# 4) 判定
# --------------------------------------------------------------------------------------

def analyse(path: Path, verbose: bool):
    """返回 (findings, rows, meta)。findings 每项 (level, message)。"""
    findings = []
    rows = []
    meta = {}

    try:
        width, height, rgba = read_png_rgba(path)
    except Exception as exc:  # noqa: BLE001 - 想把任何解码失败都变成一条 finding
        return [("error", "PNG 解码失败：%s" % exc)], [], {}

    layer = layer_of(path)
    meta["size"] = (width, height)
    meta["layer"] = layer

    counts = {}
    for key, label, x, y, w, h, _desc in REGIONS:
        n, total = count_opaque(width, height, rgba, x, y, w, h)
        counts[key] = n
        rows.append((label, "%d,%d %dx%d" % (x, y, w, h), n, total, n > 0))
    meta["counts"] = counts
    meta["opaque_total"] = count_opaque(width, height, rgba, 0, 0, width, height)[0]

    # --- 尺寸 ---
    if (width, height) != (64, 32):
        if (width, height) == (64, 64):
            findings.append(
                ("warn", "尺寸是 64x64；1.21.1 盔甲层用 64x32 布局（只采样 y=0..31）。"
                         "能显示，但 y=32..63 完全不会被读到，容易让人以为画了没效果。")
            )
        else:
            findings.append(
                ("error", "尺寸是 %dx%d；盔甲层应为 64x32。UV 采样会整体错位。" % (width, height))
            )

    # --- 整件透明 ---
    if meta["opaque_total"] == 0:
        findings.append(("error", "整张图没有任何不透明像素 —— 这一层穿上后完全不可见。"))

    # --- 不该有内容却有内容 ---
    if layer in RENDERED_REGIONS:
        rendered = RENDERED_REGIONS[layer]
        for key, label, _x, _y, _w, _h, desc in REGIONS:
            if key in rendered:
                continue
            if counts[key] > 0:
                level = "error" if key == "unused" else "warn"
                findings.append(
                    (level, "%s 区有 %d 个不透明像素，但 %s 这一层【不会渲染】该区域（%s）"
                            " —— 这些像素永远看不到。" % (label, counts[key], layer, desc))
                )
    else:
        findings.append(("error", "文件名既不以 _layer_1 也不以 _layer_2 结尾，无法判断层级。"))

    # --- 该有内容却全透明（按「这一层覆盖了哪些槽位」推期望）---
    if layer == "layer_1":
        if counts["head"] == 0 and counts["hat"] == 0:
            findings.append(("warn", "头 head 与帽 hat 两区都全透明 —— 头盔槽（HEAD）戴上后看不到任何东西。"))
        if counts["legs"] == 0:
            findings.append(("warn", "双腿 leg 区全透明 —— 靴子槽（FEET）戴上后看不到任何东西。"))
        if counts["body"] == 0 and counts["arms"] == 0:
            findings.append(("warn", "躯干 body 与双臂 arm 两区都全透明 —— 胸甲槽（CHEST）戴上后看不到任何东西。"))
        elif counts["arms"] == 0:
            findings.append(("warn", "双臂 arm 区全透明但躯干 body 有内容 —— 胸甲只有躯干、护臂不可见"
                                     "（典型「护臂不渲染」）。"))
        elif counts["body"] == 0:
            findings.append(("warn", "躯干 body 区全透明但双臂 arm 有内容 —— 胸甲只有护臂、身体不可见。"))
    elif layer == "layer_2":
        if counts["legs"] == 0:
            findings.append(("warn", "双腿 leg 区全透明 —— 护腿槽（LEGS）戴上后腿部看不到东西。"))
        if counts["body"] == 0:
            findings.append(("warn", "躯干 body 区全透明 —— 护腿的腰胯部分不可见"
                                     "（LEGS 槽会渲染 body 盒）。"))

    # --- 面级明细（--verbose）---
    if verbose:
        for key in ("head", "hat", "body", "arms", "legs"):
            bx, by, bw, bh, bd = BOX_GEOMETRY[key]
            for fname, fx, fy, fw, fh in box_faces(bx, by, bw, bh, bd):
                n, total = count_opaque(width, height, rgba, fx, fy, fw, fh)
                rows.append(("    %s %s" % (REGION_BY_KEY[key][1], fname),
                             "%d,%d %dx%d" % (fx, fy, fw, fh), n, total, n > 0))

    return findings, rows, meta


def alpha_fingerprint(path: Path):
    """返回物品图标的不透明形状指纹（宽, 高, 不透明数, 每行位图的 SHA1 前 12 位）。"""
    import hashlib
    width, height, rgba = read_png_rgba(path)
    bits = bytearray()
    n = 0
    for y in range(height):
        base = y * width * 4
        for x in range(width):
            v = 1 if rgba[base + x * 4 + 3] > 0 else 0
            n += v
            bits.append(v)
    h = hashlib.sha1(bytes(bits)).hexdigest()[:12]
    return width, height, n, h


ICON_PIECES = ("helmet", "chestplate", "leggings", "boots")


def check_icons(item_dir: Path, out):
    """同一套里两个图标的 alpha 形状完全相同 => 可疑（张冠李戴）。"""
    by_set = {}
    for piece in ICON_PIECES:
        for p in sorted(item_dir.glob("*_%s.png" % piece)):
            setname = p.stem[: -(len(piece) + 1)]
            by_set.setdefault(setname, {})[piece] = p

    any_suspicious = False
    for setname in sorted(by_set):
        pieces = by_set[setname]
        if len(pieces) < 2:
            continue
        out("")
        out("  物品图标：%s（%d 件）" % (setname, len(pieces)))
        info = {}
        for piece in ICON_PIECES:
            p = pieces.get(piece)
            if p is None:
                continue
            try:
                w, h, n, hh = alpha_fingerprint(p)
            except Exception as exc:  # noqa: BLE001
                out("    %-11s 解码失败：%s" % (piece, exc))
                continue
            info[piece] = (w, h, n, hh, p)
            note = ""
            if (w, h) != (16, 16):
                note = "  <-- 物品图标应为 16x16"
            if n == 0:
                note = "  <-- 整张透明"
            out("    %-11s %2dx%-2d  不透明 %3d  形状指纹 %s%s" % (piece, w, h, n, hh, note))
        seen = {}
        for piece, (w, h, n, hh, p) in info.items():
            if hh in seen:
                any_suspicious = True
                out("    [!] %s 与 %s 的 alpha 形状**完全相同**（指纹 %s）"
                    " —— 其中一个很可能贴错了文件。" % (seen[hh], piece, hh))
            else:
                seen[hh] = piece
    return any_suspicious


# --------------------------------------------------------------------------------------
# 5) main
# --------------------------------------------------------------------------------------

def default_roots(script_path: Path):
    repo = script_path.resolve().parent.parent  # tools/ 的上一级 = 仓库根
    roots = []
    for sub in ("coe", "cews"):
        res = repo / sub / "src" / "main" / "resources"
        if res.is_dir():
            roots.append(res)
    if not roots:
        roots.append(repo)
    return roots


def main(argv=None):
    ap = argparse.ArgumentParser(
        description="COE/CEWS 盔甲层贴图 UV 区域自检（只读）",
        formatter_class=argparse.RawDescriptionHelpFormatter,
    )
    ap.add_argument("--root", action="append", default=None,
                    help="要扫描的目录（可重复）。默认自动取 <仓库>/coe|cews/src/main/resources")
    ap.add_argument("--verbose", action="store_true", help="额外打印每个模型盒六个面的计数")
    ap.add_argument("--no-icons", action="store_true", help="跳过物品图标检查")
    args = ap.parse_args(argv)

    script_path = Path(__file__)
    roots = [Path(r) for r in args.root] if args.root else default_roots(script_path)

    lines = []

    def out(s=""):
        lines.append(s)
        print(s)

    out("=" * 92)
    out("COE/CEWS 盔甲层贴图 UV 区域自检（只读）")
    out("布局基准：64x32 / head(0,0) hat(32,0) legs(0,16) body(16,16) arms(40,16) unused(56,16)")
    out("依据：HumanoidArmorModel.java:19-33 / HumanoidModel.java:77-115 /")
    out("      HumanoidArmorLayer.java:107-128（1.21.1 + NeoForge 21.1.228 反编译源码）")
    out("=" * 92)

    errors = 0
    warns = 0
    found_any = False
    summary = []

    for root in roots:
        files = find_armor_files(root)
        out("")
        out("### 扫描根：%s" % root)
        if not files:
            out("    （没找到 *_layer_1.png / *_layer_2.png）")
            continue
        found_any = True

        by_base = {}
        for f in files:
            by_base.setdefault(base_of(f), {})[layer_of(f)] = f

        for base in sorted(by_base):
            pair = by_base[base]
            out("")
            out("--- %s ---" % base)
            for layer in ("layer_1", "layer_2"):
                p = pair.get(layer)
                if p is None:
                    out("  [%s] 缺少文件（%s_%s.png）" % (layer, base, layer))
                    if layer == "layer_2":
                        out("        -> 护腿槽会回落到缺失贴图（紫黑格 / 不显示）；"
                            "靴子与头盔、胸甲都在 layer_1，只有护腿在 layer_2。")
                    continue

                findings, rows, meta = analyse(p, args.verbose)
                rel = p
                try:
                    rel = p.relative_to(root)
                except ValueError:
                    pass
                out("  [%s] %s   %s" % (layer, rel, "%dx%d" % meta["size"]))
                out("    %-12s %-13s %8s %8s  %s" % ("区域", "UV(x,y wxh)", "不透明", "区域像素", "有内容"))
                for label, uv, n, total, has in rows:
                    out("    %-12s %-13s %8d %8d  %s" % (label, uv, n, total, "是" if has else "否"))

                cnt = meta["counts"]
                out("    判定：不透明总计 %d；" % meta["opaque_total"]
                    + " ".join("%s=%d" % (k, cnt[k]) for k in ("head", "hat", "legs", "body", "arms", "unused")))
                if not findings:
                    out("    => 未发现区域级问题。")
                for level, msg in findings:
                    tag = "错误" if level == "error" else "可疑"
                    if level == "error":
                        errors += 1
                    else:
                        warns += 1
                    out("    [%s] %s: %s" % (tag, rel, msg))
                summary.append((base, layer, meta, len(findings)))

    if found_any and not args.no_icons:
        out("")
        out("=" * 92)
        out("附：物品图标（textures/item/<set>_{helmet,chestplate,leggings,boots}.png）形状核对")
        out("说明：物品图标走 item/generated 模型，与装备层贴图是**两条完全独立**的路径。")
        out("      同一套里两个图标的 alpha 形状完全一致 => 很可能其中一个贴错了文件。")
        out("=" * 92)
        for root in roots:
            item_dirs = sorted({p for p in root.rglob("textures/item") if p.is_dir()}) if root.is_dir() else []
            if not item_dirs and (root / "item").is_dir():
                item_dirs = [root / "item"]
            for d in item_dirs:
                out("")
                out("### 图标目录：%s" % d)
                if check_icons(d, out):
                    warns += 1

    out("")
    out("=" * 92)
    out("汇总")
    out("=" * 92)
    for base, layer, meta, nf in summary:
        cnt = meta["counts"]
        out("  %-16s %-8s 头=%-5d 帽=%-5d 腿=%-5d 身=%-5d 臂=%-5d 未用=%-5d  问题=%d"
            % (base, layer, cnt["head"], cnt["hat"], cnt["legs"], cnt["body"], cnt["arms"], cnt["unused"], nf))
    out("")
    out("错误级：%d  可疑级：%d" % (errors, warns))
    if errors:
        out("结论：发现**错误级**问题，请按上面逐条的文件+区域提示处理。")
        return 1
    if warns:
        out("结论：没有错误级问题，但有可疑项，请人工核对。")
        return 0
    out("结论：所有盔甲层贴图的区域使用都在合理范围内。")
    return 0


if __name__ == "__main__":
    try:
        sys.exit(main())
    except Exception as exc:  # noqa: BLE001
        print("运行失败：%s" % exc, file=sys.stderr)
        sys.exit(2)
