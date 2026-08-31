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
# user constraint: FACING=NORTH (x270,y0) -> model S or N faces world N/S (not up/down)
# test: apply Y then X, with xRot sign and yRot sign combos, require up->FACING via some mechanism
# Actually let's just enumerate ALL tables that satisfy: UP identity, and print; then manually match user data
for order in ["XY","YX"]:
    for sx in [1,-1]:
        for sy in [1,-1]:
            def fwd(v, f):
                x, y = xrot_for(f), yrot_for(f)
                if order=="XY": return rot_y(rot_x(v, sx*x), sy*y)
                return rot_x(rot_y(v, sy*y), sx*x)
            # UP must be identity
            if any(dir_of(fwd(faces[n],"UP")) != n for n in ["UP","NORTH","EAST","SOUTH","WEST"]): continue
            print(f"=== order={order} sx={sx} sy={sy} (UP identity OK) ===")
            for f in ["UP","DOWN","NORTH","SOUTH","EAST","WEST"]:
                fwd_t = {n: dir_of(fwd(faces[n], f)) for n in ["NORTH","EAST","SOUTH","WEST"]}
                up_w = dir_of(fwd(faces["UP"], f))
                print(f'  {f}: up->{up_w}  N={fwd_t["NORTH"]} E={fwd_t["EAST"]} S={fwd_t["SOUTH"]} W={fwd_t["WEST"]}')
            print()
