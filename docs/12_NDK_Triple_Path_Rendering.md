# NDK 기반 Triple-Path Rendering 아키텍처 및 기초 코드
**리뷰어:** Claude Code
**작성자:** Agent Streamer

본 문서는 `04_Technical_Design.md`에 명시된 **Triple-Path Rendering**을 달성하기 위한 Android NDK(C++/OpenGL ES) 기반의 스캐폴딩 코드입니다. 카메라 원본 데이터를 가로채서 메모리 복사 없이(Zero-copy) 3개의 독립적인 경로(Clean, Program, Operator)로 분기하는 핵심 로직을 포함합니다.

## 1. 아키텍처 개요 (Architecture Overview)
- **Input:** `Camera2 API`에서 생성된 OES Texture (`ASurfaceTexture` 기반).
- **Processing:** EGL Context를 공유하는 3개의 개별 EGLSurface 생성.
- **Output:** 
  1. `Clean Feed` -> 비디오 인코더(MediaCodec)의 Input Surface (`ANativeWindow`).
  2. `Program Feed` -> 프레임버퍼 렌더러(오버레이 합성용)의 Input Surface.
  3. `Operator Feed` -> 화면 디스플레이용 UI Surface (`ANativeWindow`).

## 2. 핵심 C++ 스캐폴딩 코드 (Scaffolding Code)

### `TriplePathRenderer.h`
```cpp
#pragma once

#include <EGL/egl.h>
#include <EGL/eglext.h>
#include <GLES2/gl2.h>
#include <GLES2/gl2ext.h>
#include <android/native_window.h>
#include <android/surface_texture.h>
#include <memory>
#include <mutex>

class TriplePathRenderer {
public:
    TriplePathRenderer();
    ~TriplePathRenderer();

    // 초기화 및 외부 Surface(ANativeWindow) 등록
    bool Initialize();
    void SetCleanWindow(ANativeWindow* window);
    void SetProgramWindow(ANativeWindow* window);
    void SetOperatorWindow(ANativeWindow* window);

    // 카메라 텍스처 업데이트 및 3갈래 렌더링 실행
    void OnFrameAvailable(ASurfaceTexture* cameraTexture);

    // 리소스 해제
    void Release();

private:
    bool InitEGL();
    EGLSurface CreateEGLSurface(ANativeWindow* window);
    void DrawFrame(EGLSurface surface, GLuint textureId, const float* transformMatrix, int pathType);

    EGLDisplay mDisplay = EGL_NO_DISPLAY;
    EGLContext mContext = EGL_NO_CONTEXT;
    EGLConfig mConfig;

    // 3가지 Path를 위한 EGLSurface
    EGLSurface mCleanSurface = EGL_NO_SURFACE;
    EGLSurface mProgramSurface = EGL_NO_SURFACE;
    EGLSurface mOperatorSurface = EGL_NO_SURFACE;

    // 네이티브 윈도우 보관
    ANativeWindow* mCleanWindow = nullptr;
    ANativeWindow* mProgramWindow = nullptr;
    ANativeWindow* mOperatorWindow = nullptr;

    std::mutex mRenderMutex;
};
```

### `TriplePathRenderer.cpp`
```cpp
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
```

## 3. Claude Code 검토 요청 주안점 (For Claude Code)
1. **Thread Safety & Sync:** `ASurfaceTexture_updateTexImage` 호출과 EGLContext 스위칭(`eglMakeCurrent`) 시 발생할 수 있는 교착 상태(Deadlock)나 EGL 렌더링 컨텍스트 스레드 종속성 관련 리뷰.
2. **Performance (Zero-Copy):** OES 텍스처를 3개의 EGLSurface로 뿌리는 파이프라인에서 GL 자원 병목을 해결할 더 나은 Shared Context / FBO(Framebuffer Object) 전략이 있는지 피드백 요망.
3. **NDK Hardware Scalability:** OOM(Out of Memory)이나 Thermal Throttling 방지를 위한 Native 레벨 버퍼 큐(BufferQueue) 핸들링 전략.
