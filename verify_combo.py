import math
def rot_x(v, deg):
    r = math.radians(deg); c, s = math.cos(r), math.sin(r)
    x, y, z = v
    return (x, y*c - z*s, y*s + z*c)
def rot_y(v, deg):
    r = math.radians(deg); c, s = math.cos(r), math.sin(r)
    x, y, z = v
    return (x*c + z*s, y, -x*s + z*c)
def dir_of(v):
    x, y, z = v
    ax = max(abs(x), abs(y), abs(z))
    if abs(x) == ax: return "EAST" if x > 0 else "WEST"
    if abs(y) == ax: return "UP" if y > 0 else "DOWN"
    return "SOUTH" if z > 0 else "NORTH"
def xrot_for(f):
    return 0 if f == "UP" else 180 if f == "DOWN" else 270
def yrot_for(f):
    return {"NORTH":0,"SOUTH":180,"WEST":90,"EAST":270,"UP":0,"DOWN":0}[f]
faces = {"UP":(0,1,0),"NORTH":(0,0,-1),"EAST":(1,0,0),"SOUTH":(0,0,1),"WEST":(-1,0,0)}
# Test various combos; key constraint from user:
# FACING=NORTH: click world UP opens model N or S face -> so model N/S face faces world N/S
# FACING=EAST: click world EAST opens world WEST -> model face at world EAST is opened when clicking EAST
# FACING=UP: identity (normal) -> UP must be identity
combos = []
for order in ["XY","YX"]:
    for sx in [1,-1]:
        for sy in [1,-1]:
            for yextra in [0, 180]:
                def fwd(vec, f):
                    x, y = xrot_for(f), yrot_for(f) + yextra
                    if order == "XY":
                        return rot_y(rot_x(vec, sx*x), sy*y)
                    return rot_x(rot_y(vec, sy*y), sx*x)
                # check UP identity
                if fwd(faces["UP"], "UP") != (0,1,0): continue
                # check FACING=UP all identity
                ok_id = all(fwd(faces[n], "UP") == faces[n] for n in ["NORTH","EAST","SOUTH","WEST"])
                if not ok_id: continue
                # check up->FACING for horizontals
                ok_up = all(dir_of(fwd(faces["UP"], f)) == f for f in ["NORTH","SOUTH","EAST","WEST"])
                if not ok_up: continue
                combos.append((order, sx, sy, yextra))
for c in combos:
    order, sx, sy, yextra = c
    print(f"=== order={order} sx={sx} sy={sy} yextra={yextra} ===")
    for f in ["UP","DOWN","NORTH","SOUTH","EAST","WEST"]:
        x, y = xrot_for(f), yrot_for(f) + yextra
        fwd = {n: dir_of((rot_y(rot_x(faces[n], sx*x), sy*y) if order=="XY" else rot_x(rot_y(faces[n], sy*y), sx*x))) for n in ["NORTH","EAST","SOUTH","WEST"]}
        print(f'  {f}: N={fwd["NORTH"]} E={fwd["EAST"]} S={fwd["SOUTH"]} W={fwd["WEST"]}')
    print()