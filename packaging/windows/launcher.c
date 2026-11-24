#ifndef UNICODE
#define UNICODE
#endif
#ifndef _UNICODE
#define _UNICODE
#endif
#define WIN32_LEAN_AND_MEAN

#include <windows.h>
#include <wchar.h>

static int file_exists(const wchar_t *path) {
    DWORD attrs = GetFileAttributesW(path);
    return (attrs != INVALID_FILE_ATTRIBUTES) && !(attrs & FILE_ATTRIBUTE_DIRECTORY);
}

static void show_error(const wchar_t *message) {
    MessageBoxW(NULL, message, L"Logic Labs — Launch Error", MB_OK | MB_ICONERROR);
}

static int launch_jvm(
    const wchar_t *exeDir,
    const wchar_t *javawPath,
    const wchar_t *jarPath,
    int forceSoftwareRender,
    DWORD *outQuickExitCode
) {
    wchar_t cmdLine[4096];
    const wchar_t *renderFlag = forceSoftwareRender ? L" -Dskiko.renderApi=SOFTWARE" : L"";

    int written = _snwprintf(
        cmdLine,
        4096,
        L"\"%ls\" -Xms48m -Xmx320m -XX:+UseSerialGC -XX:MinHeapFreeRatio=15 -XX:MaxHeapFreeRatio=30 -XX:SoftRefLRUPolicyMSPerMB=50 -Dfile.encoding=UTF-8 -Dsun.java2d.dpiaware=true%ls \"-Dlogiclabs.app.dir=%ls\" -jar \"%ls\"",
        javawPath,
        renderFlag,
        exeDir,
        jarPath
    );
    if (written <= 0 || written >= 4096) {
        show_error(L"Installation path is too long. Please move LogicLabs to a shorter folder path.");
        return 0;
    }

    STARTUPINFOW si;
    PROCESS_INFORMATION pi;
    ZeroMemory(&si, sizeof(si));
    ZeroMemory(&pi, sizeof(pi));
    si.cb = sizeof(si);

    BOOL ok = CreateProcessW(
        javawPath,
        cmdLine,
        NULL,
        NULL,
        FALSE,
        CREATE_NO_WINDOW,
        NULL,
        exeDir,
        &si,
        &pi
    );

    if (!ok) {
        return 0;
    }

    // Watch briefly in case a legacy Windows 10 GPU driver rejects Direct3D/OpenGL immediately.
    DWORD waitRes = WaitForSingleObject(pi.hProcess, 2500);
    if (waitRes == WAIT_OBJECT_0) {
        DWORD exitCode = 0;
        GetExitCodeProcess(pi.hProcess, &exitCode);
        CloseHandle(pi.hProcess);
        CloseHandle(pi.hThread);
        if (outQuickExitCode) *outQuickExitCode = exitCode;
        return -1; // Exited rapidly
    }

    CloseHandle(pi.hProcess);
    CloseHandle(pi.hThread);
    return 1; // Running normally
}

int WINAPI wWinMain(HINSTANCE hInstance, HINSTANCE hPrevInstance, PWSTR pCmdLine, int nCmdShow) {
    (void)hInstance;
    (void)hPrevInstance;
    (void)nCmdShow;

    wchar_t exePath[MAX_PATH];
    DWORD len = GetModuleFileNameW(NULL, exePath, MAX_PATH);
    if (len == 0 || len >= MAX_PATH) {
        show_error(L"Unable to determine LogicLabs.exe installation directory.");
        return 1;
    }

    wchar_t *lastSlash = wcsrchr(exePath, L'\\');
    if (lastSlash) {
        *lastSlash = L'\0';
    }

    wchar_t javawPath[MAX_PATH];
    wchar_t jarPath[MAX_PATH];
    _snwprintf(javawPath, MAX_PATH, L"%ls\\runtime\\bin\\javaw.exe", exePath);
    _snwprintf(jarPath, MAX_PATH, L"%ls\\app\\LogicLabs-windows-all.jar", exePath);

    if (!file_exists(javawPath)) {
        wchar_t msg[1024];
        _snwprintf(
            msg,
            1024,
            L"Bundled Java runtime was not found:\n%ls\n\nMake sure you extracted the entire LogicLabs-Windows-Portable.zip archive before running LogicLabs.exe.",
            javawPath
        );
        show_error(msg);
        return 2;
    }

    if (!file_exists(jarPath)) {
        wchar_t msg[1024];
        _snwprintf(
            msg,
            1024,
            L"Application bundle was not found:\n%ls\n\nMake sure the 'app' folder sits beside LogicLabs.exe.",
            jarPath
        );
        show_error(msg);
        return 3;
    }

    int forceSoftware = (pCmdLine != NULL && wcsstr(pCmdLine, L"--software") != NULL);
    DWORD quickExitCode = 0;

    int res = launch_jvm(exePath, javawPath, jarPath, forceSoftware, &quickExitCode);
    if (res == 1) {
        return 0;
    }

    // Automatic fallback to Skia software renderer if GPU initialization failed rapidly on Windows 10.
    if (res == -1 && quickExitCode != 0 && !forceSoftware) {
        DWORD fallbackExit = 0;
        int retry = launch_jvm(exePath, javawPath, jarPath, 1, &fallbackExit);
        if (retry == 1 || (retry == -1 && fallbackExit == 0)) {
            return 0;
        }
        wchar_t msg[512];
        _snwprintf(
            msg,
            512,
            L"Logic Labs failed to start (exit code %lu).\n\nTry running LogicLabs-SoftwareRender.bat or updating your graphics drivers.",
            (unsigned long)fallbackExit
        );
        show_error(msg);
        return 4;
    }

    if (res == 0) {
        show_error(L"Failed to launch the bundled Java runtime (CreateProcessW failed).");
        return 5;
    }

    return 0;
}
