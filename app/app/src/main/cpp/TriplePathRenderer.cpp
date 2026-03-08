#include "TriplePathRenderer.h"
#include <android/log.h>

#define LOG_TAG "TriplePathRenderer"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

TriplePathRenderer::TriplePathRenderer() {}

TriplePathRenderer::~TriplePathRenderer() {
    Release();
}

bool TriplePathRenderer::Initialize() {
    return InitEGL();
}

bool TriplePathRenderer::InitEGL() {
    mDisplay = eglGetDisplay(EGL_DEFAULT_DISPLAY);
    if (mDisplay == EGL_NO_DISPLAY) return false;

    eglInitialize(mDisplay, nullptr, nullptr);

    const EGLint attribs[] = {
        EGL_RENDERABLE_TYPE, EGL_OPENGL_ES2_BIT,
        EGL_SURFACE_TYPE, EGL_WINDOW_BIT,
        EGL_BLUE_SIZE, 8,
        EGL_GREEN_SIZE, 8,
        EGL_RED_SIZE, 8,
        EGL_NONE
    };

    EGLint numConfigs;
    eglChooseConfig(mDisplay, attribs, &mConfig, 1, &numConfigs);

    const EGLint contextAttribs[] = {
        EGL_CONTEXT_CLIENT_VERSION, 2,
        EGL_NONE
    };
    mContext = eglCreateContext(mDisplay, mConfig, EGL_NO_CONTEXT, contextAttribs);
    
    return true;
}

void TriplePathRenderer::SetCleanWindow(ANativeWindow* window) {
    std::lock_guard<std::mutex> lock(mRenderMutex);
    mCleanWindow = window;
    if (mCleanSurface != EGL_NO_SURFACE) eglDestroySurface(mDisplay, mCleanSurface);
    mCleanSurface = CreateEGLSurface(window);
}

void TriplePathRenderer::SetProgramWindow(ANativeWindow* window) {
    std::lock_guard<std::mutex> lock(mRenderMutex);
    mProgramWindow = window;
    if (mProgramSurface != EGL_NO_SURFACE) eglDestroySurface(mDisplay, mProgramSurface);
    mProgramSurface = CreateEGLSurface(window);
}

void TriplePathRenderer::SetOperatorWindow(ANativeWindow* window) {
    std::lock_guard<std::mutex> lock(mRenderMutex);
    mOperatorWindow = window;
    if (mOperatorSurface != EGL_NO_SURFACE) eglDestroySurface(mDisplay, mOperatorSurface);
    mOperatorSurface = CreateEGLSurface(window);
}

EGLSurface TriplePathRenderer::CreateEGLSurface(ANativeWindow* window) {
    if (!window || mDisplay == EGL_NO_DISPLAY) return EGL_NO_SURFACE;
    const EGLint surfaceAttribs[] = { EGL_NONE };
    return eglCreateWindowSurface(mDisplay, mConfig, window, surfaceAttribs);
}

// 이 함수는 카메라 프레임 콜백(onFrameAvailable) 스레드에서 호출됩니다.
void TriplePathRenderer::OnFrameAvailable(ASurfaceTexture* cameraTexture) {
    std::lock_guard<std::mutex> lock(mRenderMutex);

    if (!cameraTexture) return;

    // 1. 카메라 텍스처 업데이트 및 Matrix 가져오기
    ASurfaceTexture_updateTexImage(cameraTexture);
    float transformMatrix[16];
    ASurfaceTexture_getTransformMatrix(cameraTexture, transformMatrix);
    
    // (사전 조건) ASurfaceTexture를 생성할 때 사용한 GL Texture ID를 가져와야 합니다.
    // 여기서는 예시로 textureId를 얻었다고 가정합니다 (실제로는 클래스 멤버로 바인딩 상태 관리 필요).
    GLuint textureId = 0; // TODO: 바인딩된 GL_TEXTURE_EXTERNAL_OES ID 

    // 2. Path 1: Clean Feed (Pure Video) -> SRT Encoder
    if (mCleanSurface != EGL_NO_SURFACE) {
        DrawFrame(mCleanSurface, textureId, transformMatrix, 1);
    }

    // 3. Path 2: Program Feed (Overlay Mixed) -> Local Record / Encoder
    if (mProgramSurface != EGL_NO_SURFACE) {
        DrawFrame(mProgramSurface, textureId, transformMatrix, 2);
    }

    // 4. Path 3: Operator Feed (UI Preview) -> Display
    if (mOperatorSurface != EGL_NO_SURFACE) {
        DrawFrame(mOperatorSurface, textureId, transformMatrix, 3);
    }
}

void TriplePathRenderer::DrawFrame(EGLSurface surface, GLuint textureId, const float* transformMatrix, int pathType) {
    // EGL Context를 해당 Surface에 바인딩
    if (!eglMakeCurrent(mDisplay, surface, surface, mContext)) {
        LOGE("eglMakeCurrent failed for path %d", pathType);
        return;
    }

    // TODO: 셰이더 프로그램 바인딩, 버텍스 설정, 텍스처 바인딩 (GL_TEXTURE_EXTERNAL_OES)
    // pathType 에 따라 셰이더 내에서 오버레이 텍스처(HTML5 프레임버퍼 레이어 등) 합성 로직이 달라집니다.
    // - pathType 1: 원본 OES 텍스처만 그대로 복사 (단순 passthrough 셰이더)
    // - pathType 2/3: 프레임버퍼 포스트 프로세싱 셰이더를 통해 텍스트/이미지를 합성

    // 실제 Draw 콜 (가상)
    // glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);

    // Front/Back 버퍼 스왑하여 렌더링 결과물 전송
    eglSwapBuffers(mDisplay, surface);
}

void TriplePathRenderer::Release() {
    std::lock_guard<std::mutex> lock(mRenderMutex);
    if (mDisplay != EGL_NO_DISPLAY) {
        eglMakeCurrent(mDisplay, EGL_NO_SURFACE, EGL_NO_SURFACE, EGL_NO_CONTEXT);
        if (mCleanSurface != EGL_NO_SURFACE) eglDestroySurface(mDisplay, mCleanSurface);
        if (mProgramSurface != EGL_NO_SURFACE) eglDestroySurface(mDisplay, mProgramSurface);
        if (mOperatorSurface != EGL_NO_SURFACE) eglDestroySurface(mDisplay, mOperatorSurface);
        if (mContext != EGL_NO_CONTEXT) eglDestroyContext(mDisplay, mContext);
        eglTerminate(mDisplay);
    }
    mDisplay = EGL_NO_DISPLAY;
    mContext = EGL_NO_CONTEXT;
}
