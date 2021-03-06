package com.example.opengldemo.fbo;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.opengl.GLES20;
import android.opengl.Matrix;

import com.example.BasicRender;
import com.example.object.Triangle;
import com.example.opengldemo.R;
import com.example.shaderUtil.BasicShaderProgram;
import com.example.shaderUtil.MatrixHelper;
import com.example.shaderUtil.TextureHelper;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

@SuppressLint("NewApi")
public class FboRender extends BasicRender {
	private Activity activity;
	private int meshTexture;
	private int tableTexture;

	private float[] projectionMatrix = new float[16];
	private float[] viewMatrix = new float[16];
	private float[] fboViewMatrix = new float[16];
	private final float[] modelMatrix = new float[16];
	private float[] viewProjectMatrix = new float[16];

	private BasicShaderProgram textureShader;
	private Triangle table;
	private float xAngle = 0;
	private float yAngle = 0;
	private float rotateAngle = 0;

	private int[] defaultFbo = new int[1];
	private int [] customFbo = new int[1];
	private int[] depthFrameBuffer = new int[1];
	private int[] currentFboTexture = new int[1];

	private int screenWidth, screenHeight;

	public FboRender(Activity activity) {
		this.activity = activity;
	}

	@Override
	public void onSurfaceCreated(GL10 arg0, EGLConfig arg1) {
		GLES20.glClearColor(1F, 1F, 1F, 0F);

		GLES20.glEnable(GLES20.GL_DEPTH_TEST);
		// GLES20.glEnable(GLES20.GL_CULL_FACE);

		createCustomFbo();

		meshTexture = TextureHelper.loadTexture(activity, R.drawable.face);
		tableTexture = TextureHelper.loadTexture(activity, R.drawable.air_hockey_surface);

		textureShader = new BasicShaderProgram(activity, R.raw.rotate_vertex_shader, R.raw.rotate_fragment_shader);

		table = new Triangle(new float[] {
				2.1f,  2.4f,   1f, 0.1f,
				1.8f,  2.7f,   0f, 0.1f,
				1.1f, -1.2f,   0f, 0.9f});
	}

	private void createCustomFbo(){
		GLES20.glGetIntegerv(GLES20.GL_FRAMEBUFFER_BINDING, defaultFbo, 0);
		GLES20.glGenFramebuffers(1, customFbo, 0);
		GLES20.glGenRenderbuffers(1, depthFrameBuffer, 0);
		GLES20.glBindRenderbuffer(GLES20.GL_RENDERBUFFER, depthFrameBuffer[0]);
		GLES20.glRenderbufferStorage(GLES20.GL_RENDERBUFFER, GLES20.GL_DEPTH_COMPONENT16, 256, 256);
		GLES20.glGenTextures(1, currentFboTexture, 0);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, currentFboTexture[0]);
		GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGB, 256, 256, 0,
				GLES20.GL_RGB, GLES20.GL_UNSIGNED_SHORT_5_6_5, null);
		GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
		GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
		GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
		GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
		GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, customFbo[0]);
		GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
				GLES20.GL_TEXTURE_2D, currentFboTexture[0], 0);
		GLES20.glFramebufferRenderbuffer(GLES20.GL_FRAMEBUFFER, GLES20.GL_DEPTH_ATTACHMENT,
				GLES20.GL_RENDERBUFFER, depthFrameBuffer[0]);
		int status = GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER);
		if(status == GLES20.GL_FRAMEBUFFER_COMPLETE){
			GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, customFbo[0]);
			GLES20.glViewport(0, 0, 256, 256);
			GLES20.glClearColor(0F, 1F, 1F, 0F);

			GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, defaultFbo[0]);
			GLES20.glViewport(0, 0, screenWidth, screenHeight);
			GLES20.glClearColor(1F, 1F, 1F, 0F);
		}
	}

	@Override
	public void onSurfaceChanged(GL10 arg0, int width, int height) {
		this.screenHeight = height;
		this.screenWidth = width;

		float fov = 120;
		float aspect = ((float) width / (float) height);
		float near = 0.1f;
		float far = 200f;
		MatrixHelper.perspectiveM(projectionMatrix, fov, aspect, near, far);

		Matrix.setLookAtM(viewMatrix, 0, 0f, 0f, 1f, 1f, 0.5f, -1f, 0f, 1f, 0f);
		Matrix.translateM(viewMatrix, 0, 0, 0, -2);

		Matrix.setLookAtM(fboViewMatrix, 0, 0f, 0f, 1f, 1f, 0.5f, -1f, 0f, 1f, 0f);
		Matrix.translateM(fboViewMatrix, 0, -1, -1, 0.5f);
//		Matrix.translateM(fboViewMatrix, 0, 0, -1, 0);
	}

	@Override
 	public void onDrawFrame(GL10 arg0) {
		drawCustomFbo();

		drawDefaultFbo();
	}

	private void drawCustomFbo(){
		GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, customFbo[0]);
		GLES20.glViewport(0, 0, 256, 256);
		GLES20.glClearColor(0F, 1F, 1F, 0F);
		GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
		textureShader.useProgram();
		float[] matrix = getCustomFboMatrix();
		textureShader.setUniform(matrix, tableTexture);
		table.bindData(textureShader);
		table.draw();// triangle
	}

	private void drawDefaultFbo(){
		GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, defaultFbo[0]);
		GLES20.glViewport(0, 0, screenWidth, screenHeight);
		GLES20.glClearColor(1F, 1F, 1F, 0F);
		GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
		textureShader.useProgram();
		textureShader.setUniform(getDefaultFboMatrix(), currentFboTexture[0]);
		table.bindData(textureShader);
		table.draw();// triangle
	}

	private float[] getCustomFboMatrix() {
		Matrix.multiplyMM(viewProjectMatrix, 0, projectionMatrix, 0, fboViewMatrix, 0);

		Matrix.setIdentityM(modelMatrix, 0);

		Matrix.translateM(modelMatrix, 0, 1.1f, -1.2f, 0);
		Matrix.rotateM(modelMatrix, 0, rotateAngle, 0f, 1f, 0f);
		Matrix.translateM(modelMatrix, 0, -1.1f, 1.2f, 0);

		float[] temp = new float[16];
		float[] resultMatrix = new float[16];
		Matrix.multiplyMM(temp, 0, viewProjectMatrix, 0, modelMatrix, 0);
		System.arraycopy(temp, 0, resultMatrix, 0, temp.length);
		rotateAngle++;
		return resultMatrix;
	}

	private float[] getDefaultFboMatrix() {
		Matrix.multiplyMM(viewProjectMatrix, 0, projectionMatrix, 0, viewMatrix, 0);

		Matrix.setIdentityM(modelMatrix, 0);

		Matrix.translateM(modelMatrix, 0, 1.1f, -1.2f, 0);
		Matrix.rotateM(modelMatrix, 0, xAngle, 1f, 0f, 0f);
		Matrix.rotateM(modelMatrix, 0, yAngle, 0f, 1f, 0f);
		Matrix.translateM(modelMatrix, 0, -1.1f, 1.2f, 0);

		float[] temp = new float[16];
		float[] resultMatrix = new float[16];
		Matrix.multiplyMM(temp, 0, viewProjectMatrix, 0, modelMatrix, 0);
		System.arraycopy(temp, 0, resultMatrix, 0, temp.length);
//		rotateAngle++;
		return resultMatrix;
	}

	public void rotate(float xAngle, float yAngle) {
		this.xAngle += xAngle;
		this.yAngle += yAngle;
	}
	
	 public void release(){
                GLES20.glDeleteRenderbuffers(1, depthFrameBuffer, 0);
                GLES20.glDeleteFramebuffers(1, customFbo, 0);
                GLES20.glDeleteTextures(1, currentFboTexture, 0);
        }
}
