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
