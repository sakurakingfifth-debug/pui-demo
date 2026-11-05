"""把 SVG 图标包转成 Compose 能直接绘制的紧凑文本格式。

输出每行一个图标：
    Display Name|0.50e@M...|1@M...
字段说明：
    - 每段用 | 分隔，第一段是显示名；
    - 其余每段 = `alpha` + 可选 `e`(evenodd) + `@` + pathData；
    - 颜色全部丢弃，只保留透明度 —— 这些图标都是单色 + 叠色，运行时用主题色 tint。
"""
import csv, os, re, sys, html

SVG_NUM = r"[-+]?[0-9]*\.?[0-9]+(?:[eE][-+]?[0-9]+)?"

def attrs(tag):
    return dict(re.findall(r'([\w:-]+)\s*=\s*"([^"]*)"', tag))

def shape_to_path(tag, a):
    """circle / ellipse / rect 转成等价 path data。"""
    def f(k, d=0.0):
        try: return float(a.get(k, d))
        except ValueError: return d
    if tag == "circle":
        cx, cy, r = f("cx"), f("cy"), f("r")
        return (f"M{cx-r},{cy} a{r},{r} 0 1,0 {2*r},0 "
                f"a{r},{r} 0 1,0 {-2*r},0 Z")
    if tag == "ellipse":
        cx, cy, rx, ry = f("cx"), f("cy"), f("rx"), f("ry")
        return (f"M{cx-rx},{cy} a{rx},{ry} 0 1,0 {2*rx},0 "
                f"a{rx},{ry} 0 1,0 {-2*rx},0 Z")
    if tag == "rect":
        x, y, w, h, r = f("x"), f("y"), f("width"), f("height"), f("rx")
        if r <= 0:
            return f"M{x},{y} h{w} v{h} h{-w} Z"
        return (f"M{x+r},{y} h{w-2*r} a{r},{r} 0 0,1 {r},{r} v{h-2*r} "
                f"a{r},{r} 0 0,1 {-r},{r} h{-(w-2*r)} a{r},{r} 0 0,1 {-r},{-r} "
                f"v{-(h-2*r)} a{r},{r} 0 0,1 {r},{-r} Z")
    return None

def convert(svg_path, name):
    raw = open(svg_path, encoding="utf-8").read()
    raw = re.sub(r"<!--.*?-->", "", raw, flags=re.S)
    parts = []
    # 只扫 path / circle / ellipse / rect；<g> 仅当容器，不处理 transform（全库只有 3 个用）
    for m in re.finditer(r"<(path|circle|ellipse|rect)\b([^>]*)/?>", raw):
        tag, body = m.group(1), m.group(2)
        a = attrs(body)
        if a.get("stroke") not in (None, "none"):
            continue  # 描边图标跳过（全库 1 个）
        d = a.get("d") if tag == "path" else shape_to_path(tag, a)
        if not d:
            continue
        d = re.sub(r"\s+", " ", d.strip())
        alpha = a.get("opacity") or a.get("fill-opacity") or "1"
        try: al = float(alpha)
        except ValueError: al = 1.0
        flag = "e" if a.get("fill-rule") == "evenodd" or a.get("clip-rule") == "evenodd" else ""
        parts.append(f"{al:.2f}{flag}@{d}")
    if not parts:
        return None
    return name + "|" + "|".join(parts)

def build(root, out):
    man = os.path.join(root, "manifest.csv")
    names = {}
    with open(man, encoding="utf-8-sig") as fh:
        for row in csv.DictReader(fh):
            names[row["file"].split("/")[-1]] = row["name"].strip()
    lines = []
    for fn in sorted(os.listdir(os.path.join(root, "svg"))):
        if not fn.endswith(".svg"):
            continue
        display = names.get(fn) or fn[:-4].replace("-", " ").title()
        line = convert(os.path.join(root, "svg", fn), display.replace("|", "/"))
        if line:
            lines.append(line)
    os.makedirs(os.path.dirname(out), exist_ok=True)
    with open(out, "w", encoding="utf-8") as fh:
        fh.write("\n".join(lines))
    # 卡片上的预览条只需要前 4 个图标：单独存一份几百字节的小文件，
    # 免得为了 4 个图标去读 1.6MB 的主文件（那会在首页组合时卡一下）
    prev = out.replace(".txt", "_preview.txt")
    with open(prev, "w", encoding="utf-8") as fh:
        fh.write("\n".join(lines[:4]))
    print(f"{out}: {len(lines)} icons, {os.path.getsize(out)/1024:.0f} KB")

if __name__ == "__main__":
    base = "D:/SVG/_x"
    outdir = sys.argv[1]
    build(os.path.join(base, "Solar_Bold_Duotone_Icons"), os.path.join(outdir, "icons_solar.txt"))
    build(os.path.join(base, "Iconsax_Duotone_Filled_Icons"), os.path.join(outdir, "icons_iconsax.txt"))
