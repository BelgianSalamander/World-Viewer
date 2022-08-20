package com.corgitaco.worldviewer.client;

import com.corgitaco.worldviewer.common.WorldViewer;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.system.MemoryStack;

import java.io.IOException;

import static java.util.Objects.requireNonNull;
import static org.lwjgl.opengl.ARBBufferStorage.glBufferStorage;
import static org.lwjgl.opengl.ARBDirectStateAccess.*;
import static org.lwjgl.opengl.GL33.*;
import static org.lwjgl.system.rpmalloc.RPmalloc.*;

// Wip Instanced rendering.

public final class WorldScreenStructureSprites {
    private static final ResourceLocation FRAGMENT_PROGRAM_SOURCE = WorldViewer.createResourceLocation("shaders/structure_sprites/fragment.glsl");
    private static final ResourceLocation VERTEX_PROGRAM_SOURCE = WorldViewer.createResourceLocation("shaders/structure_sprites/vertex.glsl");

    private final int vao;
    private final int vbo;
    private final int ebo;

    private final int program;

    WorldScreenStructureSprites() {
        if (!CrossPlatformHelper.CAPABILITIES.GL_ARB_draw_instanced) {
            throw new CrossPlatformHelper.UnsupportedGLExtensionException("GL_ARB_draw_instanced is required.");
        }

        rpmalloc_initialize();
        rpmalloc_thread_initialize();

        var buffer = requireNonNull(rpaligned_calloc(4, 1, (24 + 6) * 4));

        buffer.putFloat(-0.5F).putFloat(-0.5F).putFloat(0.0F).putFloat(1.0F).putFloat(0.0F).putFloat(0.0F);
        buffer.putFloat( 0.5F).putFloat(-0.5F).putFloat(0.0F).putFloat(1.0F).putFloat(0.0F).putFloat(0.0F);
        buffer.putFloat( 0.5F).putFloat( 0.5F).putFloat(0.0F).putFloat(1.0F).putFloat(0.0F).putFloat(0.0F);
        buffer.putFloat(-0.5F).putFloat( 0.5F).putFloat(0.0F).putFloat(1.0F).putFloat(0.0F).putFloat(0.0F);

        buffer.putInt(0).putInt(1).putInt(2).putInt(2).putInt(3).putInt(0);

        buffer.flip();

        var vertices = 24 * 4;
        var elements = 6 * 4;

        if (CrossPlatformHelper.DIRECT_STATE_ACCESS) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                var buffers = stack.callocInt(2);

                glCreateBuffers(buffers);

                vbo = buffers.get();
                ebo = buffers.get();
            }

            vao = glCreateVertexArrays();

            var flags = GL_MAP_READ_BIT;

            buffer.limit(vertices);
            glNamedBufferStorage(vbo, buffer, flags);
            buffer.position(vertices);

            buffer.limit(vertices + elements);
            glNamedBufferStorage(ebo, buffer, flags);
            buffer.position(0);

            glVertexArrayVertexBuffer(vao, 0, vbo, 0, 24);

            glVertexArrayAttribFormat(vao, 0, 4, GL_FLOAT, false, 0);
            glVertexArrayAttribFormat(vao, 1, 2, GL_FLOAT, false, 16);

            glVertexArrayAttribBinding(vao, 0, 0);
            glVertexArrayAttribBinding(vao, 1, 0);

            glVertexArrayElementBuffer(vao, ebo);
        } else {
            vao = glGenVertexArrays();

            glBindVertexArray(vao);

            buffer.limit(vertices);
            glBindBuffer(GL_VERTEX_ARRAY, vbo = glCreateBuffers());
            if (CrossPlatformHelper.BUFFER_STORAGE) {
                glBufferStorage(GL_VERTEX_ARRAY, buffer, GL_MAP_READ_BIT);
            } else {
                glBufferData(GL_VERTEX_ARRAY, buffer, GL_STATIC_DRAW);
            }
            glBindBuffer(GL_VERTEX_ARRAY, 0);
            buffer.position(vertices);

            buffer.limit(vertices + elements);
            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo = glCreateBuffers());
            if (CrossPlatformHelper.BUFFER_STORAGE) {
                glBufferStorage(GL_ELEMENT_ARRAY_BUFFER, buffer, GL_MAP_READ_BIT);
            } else {
                glBufferData(GL_ELEMENT_ARRAY_BUFFER, buffer, GL_STATIC_DRAW);
            }
            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);
            buffer.position(0);

            glBindVertexArray(0);
        }

        rpfree(buffer);

        rpmalloc_thread_finalize(true);
        rpmalloc_finalize();

        var manager = Minecraft.getInstance().getResourceManager();

        String fragmentSource = "";
        String vertexFragment = "";
        try {
            fragmentSource = CrossPlatformHelper.read(manager.getResource(FRAGMENT_PROGRAM_SOURCE).getInputStream());
            vertexFragment = CrossPlatformHelper.read(manager.getResource(VERTEX_PROGRAM_SOURCE).getInputStream());

        } catch (IOException e) {
            e.printStackTrace();
        }

        program = glCreateProgram();

        var fragment = CrossPlatformHelper.createShader(GL_FRAGMENT_SHADER, fragmentSource);
        var vertex = CrossPlatformHelper.createShader(GL_VERTEX_SHADER, vertexFragment);

        glAttachShader(program, fragment);
        glAttachShader(program, vertex);
        glLinkProgram(program);
        glDetachShader(program, fragment);
        glDetachShader(program, vertex);

        glDeleteShader(fragment);
        glDeleteShader(vertex);

        glValidateProgram(program);
    }

    public void draw() {
        var shader = RenderSystem.getShader();

        glUseProgram(0);
        glUseProgram(program);

        glBindVertexArray(vao);

        if (CrossPlatformHelper.DIRECT_STATE_ACCESS) {
            glEnableVertexArrayAttrib(vao, 0);
            glEnableVertexArrayAttrib(vao, 1);
        } else {
            glEnableVertexAttribArray(0);
            glEnableVertexAttribArray(1);
        }

        glDrawElementsInstanced(GL_TRIANGLES, 6, GL_UNSIGNED_INT, 0, 1);

        if (CrossPlatformHelper.DIRECT_STATE_ACCESS) {
            glDisableVertexArrayAttrib(vao, 0);
            glDisableVertexArrayAttrib(vao, 1);
        } else {
            glDisableVertexAttribArray(0);
            glDisableVertexAttribArray(1);
        }

        glBindVertexArray(0);

        glUseProgram(0);

        glUseProgram(shader.getId());
    }

    public void close() {
        glDeleteVertexArrays(vao);
        glDeleteBuffers(vbo);
        glDeleteBuffers(ebo);

        glDeleteProgram(program);
    }
}
