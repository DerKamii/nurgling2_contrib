/*
 *  This file is part of the Haven & Hearth game client.
 *  Copyright (C) 2009 Fredrik Tolf <fredrik@dolda2000.com>, and
 *                     Björn Johannessen <johannessen.bjorn@gmail.com>
 *
 *  Redistribution and/or modification of this file is subject to the
 *  terms of the GNU Lesser General Public License, version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  Other parts of this source tree adhere to other copying
 *  rights. Please see the file `COPYING' in the root directory of the
 *  source tree for details.
 *
 *  A copy the GNU Lesser General Public License is distributed along
 *  with the source tree of which this file is a part in the file
 *  `doc/LPGL-3'. If it is missing for any reason, please see the Free
 *  Software Foundation's website at <http://www.fsf.org/>, or write
 *  to the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 *  Boston, MA 02111-1307 USA
 */

package haven.resutil;

import java.util.*;
import haven.*;
import haven.Resource.Tile;
import haven.MapMesh.*;

public class RidgeTile extends GroundTile {
    public final int[] breaks;
    public final Resource[] walls;
    public final Resource[] lcorn;
    public final Resource[] rcorn;

    public RidgeTile(int id, Resource.Tileset set, int[] breaks, Resource[] walls, Resource[] lcorn, Resource[] rcorn) {
	super(id, set);
	this.breaks = breaks;
	this.walls = walls;
	this.lcorn = lcorn;
	this.rcorn = rcorn;
    }
    
    public boolean[] breaks(MapMesh m, Coord gc, int diff) {
	int z00 = m.map.getz(gc),
	    z10 = m.map.getz(gc.add(1, 0)),
	    z01 = m.map.getz(gc.add(0, 1)),
	    z11 = m.map.getz(gc.add(1, 1));
	return(new boolean[] {
		Math.abs(z00 - z10) >= diff,
		Math.abs(z10 - z11) >= diff,
		Math.abs(z11 - z01) >= diff,
		Math.abs(z01 - z00) >= diff,
	    });
    }

    public boolean isend(MapMesh m, Coord gc, boolean[] b) {
	return(((b[0]?1:0) + (b[1]?1:0) + (b[2]?1:0) + (b[3]?1:0)) == 1);
    }
    
    public boolean isstraight(MapMesh m, Coord gc, boolean[] b) {
	return((b[0] && b[2] && !b[1] && !b[3]) ||
	       (b[1] && b[3] && !b[0] && !b[2]));
    }

    @SuppressWarnings("unchecked")
    private static <T> T[] shift(T[] a, int n) {
	T[] r = (T[])java.lang.reflect.Array.newInstance(a.getClass().getComponentType(), a.length);
	for(int i = 0; i < a.length; i++)
	    r[(i + n) % a.length] = a[i];
	return(r);
    }

    public void makewall(MapMesh m, Coord3f ul, Coord3f bl, Coord3f br, Coord3f ur, Resource wall, float w) {
	float hw = w / 2.0f;
	float xbx, xby;
	float lzof, lzsf, lzs;
	float rzof, rzsf, rzs;
	float tysf, tys;
	{
	    double tx = br.x - bl.x, ty = br.y - bl.y;
	    double lf = 1.0 / Math.sqrt((tx * tx) + (ty * ty));
	    xbx = (float)(tx * lf); xby = (float)(ty * lf);
	    lzof = (float)((br.z - bl.z) * lf);
	    lzsf = (float)((ur.z - br.z - ul.z + bl.z) * lf / 11.0);
	    lzs  = (float)((ul.z - bl.z) / 11.0);
	    rzof = (float)((bl.z - br.z) * lf);
	    rzsf = (float)((ul.z - bl.z - ur.z + br.z) * lf / 11.0);
	    rzs  = (float)((ur.z - br.z) / 11.0);
	    tys  = (int)(((ul.z - bl.z) + 5) / 11);
	    tysf = (float)((((int)(((ur.z - br.z) + 5) / 11)) - tys) * lf);
	}
	float ybx = -xby, yby = xbx;
	for(FastMesh.MeshRes r : wall.layers(FastMesh.MeshRes.class)) {
	    MeshBuf buf = m.model(r.mat.get(), MeshBuf.class);
	    MeshBuf.Vertex[] vs = buf.copy(r.m);
	    for(MeshBuf.Vertex v : vs) {
		float x = v.pos.x, y = v.pos.y, z = v.pos.z;
		v.pos.x = (x * xbx) + (y * ybx) + bl.x;
		v.pos.y = (x * xby) + (y * yby) + bl.y;
		if(x < hw) {
		    v.pos.z = (lzof * x) + ((lzs + (lzsf * x)) * z) + bl.z;
		} else {
		    float X = w - x;
		    v.pos.z = (rzof * X) + ((rzs + (rzsf * X)) * z) + br.z;
		}
		float nx = v.nrm.x, ny = v.nrm.y;
		v.nrm.x = (nx * xbx) + (ny * ybx);
		v.nrm.y = (nx * xby) + (ny * yby);
		v.tex.y = (tys + (tysf * x)) * v.tex.y;
	    }
	}
    }

    private static final int[]
	cwx = {0, 1, 1, 0},
	cwy = {0, 0, 1, 1};

    public void remapquad(Plane p, int q, Tex tex) {
	int tx = tex.sz().x, ty = tex.sz().y;
	int hx = tx / 2, hy = ty / 2;
	p.texul = new Coord(cwx[q] * hx, cwy[q] * hy);
	p.texbr = p.texul.add(hx, hy);
    }

    public void remaphalf(Plane p, int fq, Tex tex) {
	int tx = tex.sz().x, ty = tex.sz().y;
	int hx = tx / 2, hy = ty / 2;
	int l = Math.min(cwx[fq], cwx[(fq + 1) % 4]), r = Math.max(cwx[fq], cwx[(fq + 1) % 4]) + 1;
	int t = Math.min(cwy[fq], cwy[(fq + 1) % 4]), b = Math.max(cwy[fq], cwy[(fq + 1) % 4]) + 1;
	p.texul = new Coord(l * hx, t * hy);
	p.texbr = new Coord(r * hx, b * hy);
    }

    public void layend(MapMesh m, Random rnd, Coord lc, Coord gc, boolean[] b) {
	int dir;
	for(dir = 0; dir < 4; dir++) {
	    if(b[dir])
		break;
	}
	Surface g = m.gnd();
	SPoint
	    bl = g.spoint(lc.add(cwx[dir], cwy[dir])),
	    br = g.spoint(lc.add(cwx[(dir + 1) % 4], cwy[(dir + 1) % 4])),
	    fr = g.spoint(lc.add(cwx[(dir + 2) % 4], cwy[(dir + 2) % 4])),
	    fl = g.spoint(lc.add(cwx[(dir + 3) % 4], cwy[(dir + 3) % 4]));
	boolean cw = bl.pos.z > br.pos.z;
	SPoint bu = new SPoint(bl.pos.add(br.pos).mul(0.5f));
	SPoint bb = new SPoint(bl.pos.add(br.pos).mul(0.5f));
	SPoint fm = new SPoint(fl.pos.add(fr.pos).mul(0.5f));
	Tile tile = set.ground.pick(rnd);
	Plane left, right;
	if(cw) {
	    bu.pos.z = bl.pos.z;
	    bb.pos.z = br.pos.z;
	    left  = m.new Plane(shift(new SPoint[] {fl, fm, bu, bl}, 5 - dir), 0, tile.tex(), false);
	    right = m.new Plane(shift(new SPoint[] {fm, fr, br, bb}, 5 - dir), 0, tile.tex(), false);
	} else {
	    bu.pos.z = br.pos.z;
	    bb.pos.z = bl.pos.z;
	    left  = m.new Plane(shift(new SPoint[] {fl, fm, bb, bl}, 5 - dir), 0, tile.tex(), false);
	    right = m.new Plane(shift(new SPoint[] {fm, fr, br, bu}, 5 - dir), 0, tile.tex(), false);
	}
	remaphalf(left , (dir + 3) % 4, tile.tex());
	remaphalf(right, (dir + 1) % 4, tile.tex());
	if(cw)
	    makewall(m, fm.pos, fm.pos, bb.pos, bu.pos, walls[rnd.nextInt(walls.length)], 11);
	else
	    makewall(m, bu.pos, bb.pos, fm.pos, fm.pos, walls[rnd.nextInt(walls.length)], 11);
    }
    
    public void layridge(MapMesh m, Random rnd, Coord lc, Coord gc, boolean[] b) {
	int z00 = m.map.getz(gc),
	    z10 = m.map.getz(gc.add(1, 0)),
	    z01 = m.map.getz(gc.add(0, 1)),
	    z11 = m.map.getz(gc.add(1, 1));
	int dir = b[0]?((z00 > z10)?0:2):((z00 > z01)?1:3);
	Surface g = m.gnd();
	SPoint
	    ur = g.spoint(lc.add(cwx[dir], cwy[dir])),
	    br = g.spoint(lc.add(cwx[(dir + 1) % 4], cwy[(dir + 1) % 4])),
	    bl = g.spoint(lc.add(cwx[(dir + 2) % 4], cwy[(dir + 2) % 4])),
	    ul = g.spoint(lc.add(cwx[(dir + 3) % 4], cwy[(dir + 3) % 4]));
	SPoint
	    mlu = new SPoint(ul.pos.add(bl.pos).mul(0.5f)),
	    mlb = new SPoint(ul.pos.add(bl.pos).mul(0.5f)),
	    mru = new SPoint(ur.pos.add(br.pos).mul(0.5f)),
	    mrb = new SPoint(ur.pos.add(br.pos).mul(0.5f));
	mlu.pos.z = ul.pos.z;
	mru.pos.z = ur.pos.z;
	mlb.pos.z = bl.pos.z;
	mrb.pos.z = br.pos.z;
	Tile tile = set.ground.pick(rnd);
	Plane upper = m.new Plane(shift(new SPoint[] {ul, mlu, mru, ur}, 5 - dir), 0, tile.tex(), false);
	Plane lower = m.new Plane(shift(new SPoint[] {mlb, bl, br, mrb}, 5 - dir), 0, tile.tex(), false);
	remaphalf(upper, (dir + 3) % 4, tile.tex());
	remaphalf(lower, (dir + 1) % 4, tile.tex());
	makewall(m, mlu.pos, mlb.pos, mrb.pos, mru.pos, walls[rnd.nextInt(walls.length)], 11);
    }
    
    public void mkcornwall(MapMesh m, Random rnd, Coord3f ul, Coord3f bl, Coord3f br, Coord3f ur, boolean cw) {
	if(cw)
	    makewall(m, ul, bl, br, ur, lcorn[rnd.nextInt(lcorn.length)], 5.5f);
	else
	    makewall(m, ul, bl, br, ur, rcorn[rnd.nextInt(rcorn.length)], 5.5f);
    }

    public void laycomplex(MapMesh m, Random rnd, Coord lc, Coord gc, boolean[] b) {
	Surface g = m.gnd();
	SPoint[] crn = {
	    g.spoint(lc),
	    g.spoint(lc.add(1, 0)),
	    g.spoint(lc.add(1, 1)),
	    g.spoint(lc.add(0, 1)),
	};
	int s;
	for(s = 0; true; s++) {
	    if(b[s]) {
		s = (s + 1) % 4;
		break;
	    }
	}
	SPoint[] ct = new SPoint[4];
	SPoint[] h1 = new SPoint[4];
	SPoint[] h2 = new SPoint[4];
	{
	    for(int i = s, n = 0; n < 4; i = (i + 1) % 4, n++) {
		if(!b[(i + 3) % 4]) {
		    h1[i] = h2[(i + 3) % 4];
		    h1[i].pos.z = (h1[i].pos.z + crn[i].pos.z) * 0.5f;
		} else {
		    h1[i] = new SPoint(crn[(i + 3) % 4].pos.add(crn[i].pos).mul(0.5f));
		    h1[i].pos.z = crn[i].pos.z;
		}
		h2[i] = new SPoint(crn[(i + 1) % 4].pos.add(crn[i].pos).mul(0.5f));
		h2[i].pos.z = crn[i].pos.z;
	    }
	    SPoint cc = null;
	    for(int i = s, n = 0; n < 4; i = (i + 1) % 4, n++) {
		if(cc == null) {
		    cc = new SPoint(crn[0].pos.add(crn[1].pos).add(crn[2].pos).add(crn[3].pos).mul(0.25f));
		    if(b[i])
			cc.pos.z = crn[i].pos.z;
		    else
			cc.pos.z = (h1[i].pos.z + h2[(i + 1) % 4].pos.z) * 0.5f;
		}
		ct[i] = cc;
		if(b[i])
		    cc = null;
	    }
	}
	Tile tile = set.ground.pick(rnd);
	boolean cont = false;
	for(int i = s, n = 0; n < 4; i = (i + 1) % 4, n++) {
	    if(cont) {
		cont = false;
	    } else if(!b[i]) {
		Plane pl = m.new Plane(shift(new SPoint[] {crn[i], h1[i], h2[(i + 1) % 4], crn[(i + 1) % 4]}, 4 - i), 0, tile.tex(), false);
		remaphalf(pl, i, tile.tex());
		cont = true;
		SPoint pc = ct[(i + 3) % 4], cc = ct[i];
		if(pc.pos.z > cc.pos.z)
		    mkcornwall(m, rnd, pc.pos, cc.pos, h1[i].pos, h2[(i + 3) % 4].pos, true);
		else
		    mkcornwall(m, rnd, h1[i].pos, h2[(i + 3) % 4].pos, pc.pos, cc.pos, false);
	    } else if(!b[(i + 3) % 4]) {
		Plane pl = m.new Plane(shift(new SPoint[] {crn[i], h1[i], ct[i], h2[i]}, 4 - i), 0, tile.tex(), false);
		remapquad(pl, i, tile.tex());
	    } else {
		Plane pl = m.new Plane(shift(new SPoint[] {crn[i], h1[i], ct[i], h2[i]}, 4 - i), 0, tile.tex(), false);
		remapquad(pl, i, tile.tex());
		SPoint pc = ct[(i + 3) % 4], cc = ct[i];
		if(pc.pos.z > cc.pos.z)
		    mkcornwall(m, rnd, pc.pos, cc.pos, h1[i].pos, h2[(i + 3) % 4].pos, true);
		else
		    mkcornwall(m, rnd, h1[i].pos, h2[(i + 3) % 4].pos, pc.pos, cc.pos, false);
	    }
	}
    }

    public boolean isplain(MapMesh m, Coord gc) {
	boolean[] b = breaks(m, gc, breaks[0]);
	return(b[0] || b[1] || b[2] || b[3]);
    }

    public void lay(MapMesh m, Random rnd, Coord lc, Coord gc) {
	boolean[] b = breaks(m, gc, breaks[0]);
	if(b[0] || b[1] || b[2] || b[3]) {
	    if(isend(m, gc, b)) {
		layend(m, rnd, lc, gc, b);
	    } else if(isstraight(m, gc, b)) {
		layridge(m, rnd, lc, gc, b);
	    } else {
		laycomplex(m, rnd, lc, gc, b);
	    }
	} else {
	    super.lay(m, rnd, lc, gc);
	}
    }
    
    public void layover(MapMesh m, Coord lc, Coord gc, int z, Tile t) {
	boolean[] b = breaks(m, gc, breaks[0]);
	if(b[0] || b[1] || b[2] || b[3]) {
	} else {
	    super.layover(m, lc, gc, z, t);
	}
    }
}