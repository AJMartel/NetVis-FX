package netvis.view.util.jogl.gameengine;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;

public class Framebuffer {
    private int textureid;
    private int fbufferid;
    private int dbufferid;
    private int base;

    public Framebuffer(int b) {
        textureid = -1;
        fbufferid = -1;
        dbufferid = -1;

        base = b;
    }

    public void resize(int nb, GL2 gl) {
        base = nb;
        delete(gl);
    }

    public int bindFBuffer(GL2 gl) {
        if (fbufferid != -1) {
            return fbufferid;
        } else {
            create(gl);
        }

        return fbufferid;
    }

    public int bindDBuffer(GL2 gl) {
        if (dbufferid != -1) {
            return dbufferid;
        } else {
            create(gl);
        }

        return dbufferid;
    }

    public int bindTexture(GL2 gl) {
        if (textureid == -1) {
            create(gl);
        }

        gl.glTexEnvf(GL2.GL_TEXTURE_ENV, GL2.GL_TEXTURE_ENV_MODE, GL2.GL_COMBINE);
        return textureid;
    }

    public void create(GL2 gl) {
        int[] texture = new int[1];
        int[] fbuffer = new int[1];
        int[] dbuffer = new int[1];

        // Generate the texture to render to
        gl.glGenTextures(1, texture, 0);
        gl.glBindTexture(GL.GL_TEXTURE_2D, texture[0]);
        gl.glPixelStorei(GL.GL_UNPACK_ALIGNMENT, 1);
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_S, GL2.GL_CLAMP);
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_T, GL2.GL_CLAMP);
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, GL.GL_LINEAR_MIPMAP_LINEAR);
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_LINEAR_MIPMAP_LINEAR);

        // Reserve space for the texture - (not) make its dimensions twice as
        // big as necessary
        gl.glTexImage2D(GL.GL_TEXTURE_2D, 0, GL.GL_RGBA8, 2 * base, 2 * base, 0, GL.GL_BGRA,
                GL.GL_UNSIGNED_BYTE, null);
        gl.glGenerateMipmap(GL.GL_TEXTURE_2D);

        // Generate the framebuffer
        gl.glGenFramebuffers(1, fbuffer, 0);
        gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, fbuffer[0]);

        // Attach the texture to the framebuffer
        gl.glFramebufferTexture2D(GL.GL_FRAMEBUFFER, GL.GL_COLOR_ATTACHMENT0, GL.GL_TEXTURE_2D,
                texture[0], 0);

        // Create renderbuffer for the depth testing
        gl.glGenRenderbuffers(1, dbuffer, 0);

        // Bind renderbuffer
        gl.glBindRenderbuffer(GL2.GL_RENDERBUFFER, dbuffer[0]);

        // Init as a depth buffer
        gl.glRenderbufferStorage(GL2.GL_RENDERBUFFER, GL2.GL_DEPTH_COMPONENT24, 2 * base, 2 * base);

        // Attach to the FBO for depth
        gl.glFramebufferRenderbuffer(GL2.GL_FRAMEBUFFER, GL2.GL_DEPTH_ATTACHMENT,
                GL2.GL_RENDERBUFFER, dbuffer[0]);

        textureid = texture[0];
        fbufferid = fbuffer[0];
        dbufferid = dbuffer[0];
    }

    public void delete(GL2 gl) {
        // Cleanup
        gl.glDeleteTextures(1, new int[] { textureid }, 0);
        gl.glDeleteRenderbuffers(1, new int[] { dbufferid }, 0);
        gl.glDeleteFramebuffers(1, new int[] { fbufferid }, 0);

        discard();
    }

    public void discard() {
        textureid = -1;
        fbufferid = -1;
        dbufferid = -1;
    }

    public void setupView(GL2 gl) {
        gl.glViewport(0, 0, 2 * base, 2 * base);
        gl.glOrtho(-base, base, -base, base, -10, 1000);
        gl.glTranslated(0.0, 0.0, 5.0);

        gl.glClearColor (1.0f, 1.0f, 1.0f, 0.0f);
        gl.glClearDepth (1000.0);
        gl.glDepthFunc (GL.GL_GREATER);
        gl.glClear (GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);

        gl.glShadeModel(GL2.GL_FLAT);

        // Use the typical blending options
        gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
        gl.glEnable(GL.GL_BLEND);
    }
}
