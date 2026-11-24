package com.logiclabs.app.platform

private fun jsCopy(text: String): Boolean = js("""
(() => {
  try {
    if (navigator.clipboard && navigator.clipboard.writeText) {
      navigator.clipboard.writeText(text).catch(() => {});
      return true;
    }
    const ta = document.createElement('textarea');
    ta.value = text;
    ta.style.position = 'fixed';
    ta.style.opacity = '0';
    document.body.appendChild(ta);
    ta.focus();
    ta.select();
    const ok = document.execCommand('copy');
    document.body.removeChild(ta);
    return ok;
  } catch (e) { return false; }
})()
""")

private fun jsReadClipboard(callback: (String) -> Unit): Unit = js("""
(() => {
  try {
    if (navigator.clipboard && navigator.clipboard.readText) {
      navigator.clipboard.readText().then(t => callback(t || '')).catch(() => callback(''));
    } else {
      callback('');
    }
  } catch (e) { callback(''); }
})()
""")

private fun jsDownloadText(fileName: String, mimeType: String, content: String): Boolean = js("""
(() => {
  try {
    const blob = new Blob([content], { type: mimeType });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = fileName;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    setTimeout(() => URL.revokeObjectURL(url), 4000);
    return true;
  } catch (e) { return false; }
})()
""")

private fun jsDownloadBase64(fileName: String, mimeType: String, base64Data: String): Boolean = js("""
(() => {
  try {
    const bin = atob(base64Data);
    const bytes = new Uint8Array(bin.length);
    for (let i = 0; i < bin.length; i++) bytes[i] = bin.charCodeAt(i);
    const blob = new Blob([bytes], { type: mimeType });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = fileName;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    setTimeout(() => URL.revokeObjectURL(url), 4000);
    return true;
  } catch (e) { return false; }
})()
""")

private fun jsPickJsonFile(callback: (String, String) -> Unit): Unit = js("""
(() => {
  try {
    const input = document.createElement('input');
    input.type = 'file';
    input.accept = '.json,application/json';
    input.style.display = 'none';
    input.onchange = () => {
      const f = input.files && input.files[0];
      if (!f) { document.body.removeChild(input); return; }
      const reader = new FileReader();
      reader.onload = () => {
        callback(f.name || 'circuit.json', String(reader.result || ''));
        document.body.removeChild(input);
      };
      reader.onerror = () => {
        callback(f.name || 'circuit.json', '');
        document.body.removeChild(input);
      };
      reader.readAsText(f);
    };
    document.body.appendChild(input);
    input.click();
  } catch (e) {}
})()
""")

private fun jsBaseUrl(): String = js("window.location.origin + window.location.pathname")

private fun jsSupportsFullscreen(): Boolean = js("""
Boolean(document.fullscreenEnabled || document.webkitFullscreenEnabled)
""")

private fun jsIsFullscreen(): Boolean = js("""
Boolean(document.fullscreenElement || document.webkitFullscreenElement)
""")

private fun jsToggleFullscreen(): Boolean = js("""
(() => {
  try {
    const el = document.documentElement;
    if (document.fullscreenElement || document.webkitFullscreenElement) {
      if (document.exitFullscreen) document.exitFullscreen().catch(() => {});
      else if (document.webkitExitFullscreen) document.webkitExitFullscreen();
      return false;
    }
    if (el.requestFullscreen) { el.requestFullscreen().catch(() => {}); return true; }
    if (el.webkitRequestFullscreen) { el.webkitRequestFullscreen(); return true; }
    return false;
  } catch (e) { return false; }
})()
""")

private fun jsCanInstallPwa(): Boolean = js("Boolean(window.__logicLabsDeferredInstallPrompt)")

private fun jsPromptInstallPwa(callback: (Boolean) -> Unit): Unit = js("""
(() => {
  const p = window.__logicLabsDeferredInstallPrompt;
  if (!p) { callback(false); return; }
  try {
    p.prompt();
    p.userChoice.then(choice => {
      window.__logicLabsDeferredInstallPrompt = null;
      callback(Boolean(choice && choice.outcome === 'accepted'));
    }).catch(() => callback(false));
  } catch (e) { callback(false); }
})()
""")

private fun jsCanNativeShare(): Boolean = js("Boolean(navigator.share)")

private fun jsNativeShareUrl(title: String, text: String, url: String): Boolean = js("""
(() => {
  try {
    if (!navigator.share) return false;
    navigator.share({ title: title, text: text, url: url }).catch(() => {});
    return true;
  } catch (e) { return false; }
})()
""")

internal actual fun webPlatformCopyText(text: String): Boolean = jsCopy(text)

internal actual fun webPlatformReadClipboard(onResult: (String?) -> Unit) {
    jsReadClipboard { text -> onResult(text.ifEmpty { null }) }
}

internal actual fun webPlatformDownloadText(fileName: String, mimeType: String, content: String): Boolean =
    jsDownloadText(fileName, mimeType, content)

internal actual fun webPlatformDownloadBase64Bytes(fileName: String, mimeType: String, base64Data: String): Boolean =
    jsDownloadBase64(fileName, mimeType, base64Data)

internal actual fun webPlatformPickJsonFile(onLoaded: (fileName: String, content: String?) -> Unit) {
    jsPickJsonFile { name, text -> onLoaded(name, text.ifEmpty { null }) }
}

internal actual fun webPlatformBasePageUrl(): String = jsBaseUrl()

internal actual fun webPlatformEngineLabel(): String = "WASM"

internal actual fun webPlatformSupportsFullscreen(): Boolean = jsSupportsFullscreen()

internal actual fun webPlatformIsFullscreen(): Boolean = jsIsFullscreen()

internal actual fun webPlatformToggleFullscreen(): Boolean = jsToggleFullscreen()

internal actual fun webPlatformCanInstallPwa(): Boolean = jsCanInstallPwa()

internal actual fun webPlatformPromptInstallPwa(onResult: (Boolean) -> Unit) = jsPromptInstallPwa(onResult)

internal actual fun webPlatformCanNativeShare(): Boolean = jsCanNativeShare()

internal actual fun webPlatformNativeShareUrl(title: String, text: String, url: String): Boolean =
    jsNativeShareUrl(title, text, url)
