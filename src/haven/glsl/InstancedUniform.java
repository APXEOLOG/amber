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

package haven.glsl;

import java.util.*;
import java.nio.*;
import javax.media.opengl.*;
import haven.*;
import haven.GLState.Slot;
import haven.GLState.Buffer;

public abstract class InstancedUniform {
    public final Slot[] deps;
    public final Uniform.AutoApply uniform;
    public final Attribute.AutoInstanced attrib;

    public InstancedUniform(Type type, String infix, Slot... deps) {
	this.deps = deps;
	uniform = new Uniform.AutoApply(type, infix, deps) {
		public void apply(GOut g, int location) {InstancedUniform.this.apply(g, location);}
	    };
	attrib = new Attribute.AutoInstanced(type, infix) {
		public GLBuffer bindiarr(GOut g, List<Buffer> inst) {return(InstancedUniform.this.bindiarr(g, inst));}
		public void unbindiarr(GOut g, GLBuffer buf) {InstancedUniform.this.unbindiarr(g, buf);}
	    };
    }

    private static final Object refproc = new PostProc.AutoID("refproc", 9000);
    public Expression ref() {
	return(new PostProc.AutoMacro(refproc) {
		public Expression expand(Context ctx) {
		    if(!((ShaderContext)ctx).prog.instanced)
			return(uniform.ref());
		    else
			return(attrib.ref());
		}
	    });
    }

    protected abstract void apply(GOut g, int location);
    protected abstract GLBuffer bindiarr(GOut g, List<Buffer> inst);
    protected abstract void unbindiarr(GOut g, GLBuffer buf);

    public static abstract class Mat4 extends InstancedUniform {
	public Mat4(String infix, Slot... deps) {super(Type.MAT4, infix, deps);}

	public abstract Matrix4f forstate(GOut g, Buffer buf);

	protected void apply(GOut g, int loc) {
	    g.gl.glUniformMatrix4fv(loc, 1, false, forstate(g, g.st.state()).m, 0);
	}

	protected GLBuffer bindiarr(GOut g, List<Buffer> inst) {
	    float[] buf = new float[inst.size() * 16];
	    int i = 0;
	    for(Buffer st : inst) {
		System.arraycopy(forstate(g, st).m, 0, buf, i, 16);
		i += 16;
	    }
	    GL2 gl = g.gl;
	    GLBuffer bo = new GLBuffer(gl);
	    gl.glBindBuffer(GL.GL_ARRAY_BUFFER, bo.id);
	    gl.glBufferData(GL.GL_ARRAY_BUFFER, buf.length * 4, FloatBuffer.wrap(buf), GL.GL_STATIC_DRAW);
	    int loc = g.st.prog.attrib(attrib);
	    gl.glVertexAttribPointer(loc + 0, 4, GL.GL_FLOAT, false, 64,  0);
	    gl.glVertexAttribPointer(loc + 1, 4, GL.GL_FLOAT, false, 64, 16);
	    gl.glVertexAttribPointer(loc + 2, 4, GL.GL_FLOAT, false, 64, 32);
	    gl.glVertexAttribPointer(loc + 3, 4, GL.GL_FLOAT, false, 64, 48);
	    gl.glEnableVertexAttribArray(loc + 0);
	    gl.glEnableVertexAttribArray(loc + 1);
	    gl.glEnableVertexAttribArray(loc + 2);
	    gl.glEnableVertexAttribArray(loc + 3);
	    ((GL3)gl).glVertexAttribDivisor(loc + 0, 1);
	    ((GL3)gl).glVertexAttribDivisor(loc + 1, 1);
	    ((GL3)gl).glVertexAttribDivisor(loc + 2, 1);
	    ((GL3)gl).glVertexAttribDivisor(loc + 3, 1);
	    gl.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);
	    return(bo);
	}

	protected void unbindiarr(GOut g, GLBuffer buf) {
	    GL2 gl = g.gl;
	    int loc = g.st.prog.attrib(attrib);
	    gl.glDisableVertexAttribArray(loc + 0);
	    gl.glDisableVertexAttribArray(loc + 1);
	    gl.glDisableVertexAttribArray(loc + 2);
	    gl.glDisableVertexAttribArray(loc + 3);
	    ((GL3)gl).glVertexAttribDivisor(loc + 0, 0);
	    ((GL3)gl).glVertexAttribDivisor(loc + 1, 0);
	    ((GL3)gl).glVertexAttribDivisor(loc + 2, 0);
	    ((GL3)gl).glVertexAttribDivisor(loc + 3, 0);
	    buf.dispose();
	}
    }
}
