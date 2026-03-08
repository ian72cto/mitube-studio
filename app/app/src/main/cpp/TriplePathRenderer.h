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
