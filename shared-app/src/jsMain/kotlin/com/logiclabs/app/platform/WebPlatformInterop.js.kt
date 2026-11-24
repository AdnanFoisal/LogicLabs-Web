package com.logiclabs.app.platform

internal actual fun webPlatformCopyText(text: String): Boolean {
    return js("""
    (function(t) {
      try {
        if (navigator.clipboard && navigator.clipboard.writeText) {
          navigator.clipboard.writeText(t).catch(function() {});
          return true;
        }
        var ta = document.createElement('textarea');
        ta.value = t;
        ta.style.position = 'fixed';
        ta.style.opacity = '0';
        document.body.appendChild(ta);
        ta.focus();
        ta.select();
        var ok = document.execCommand('copy');
        document.body.removeChild(ta);
        return ok;
      } catch (e) { return false; }
    })(text)
    """).unsafeCast<Boolean>()
}

internal actual fun webPlatformReadClipboard(onResult: (String?) -> Unit) {
    val cb: (String) -> Unit = { s -> onResult(s.ifEmpty { null }) }
    js("""
    (function(callback) {
      try {
        if (navigator.clipboard && navigator.clipboard.readText) {
          navigator.clipboard.readText().then(function(t) { callback(t || ''); }).catch(function() { callback(''); });
        } else {
          callback('');
        }
      } catch (e) { callback(''); }
    })(cb)
    """)
}

internal actual fun webPlatformDownloadText(fileName: String, mimeType: String, content: String): Boolean {
    return js("""
    (function(fn, mt, body) {
      try {
        var blob = new Blob([body], { type: mt });
        var url = URL.createObjectURL(blob);
        var a = document.createElement('a');
        a.href = url;
        a.download = fn;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        setTimeout(function() { URL.revokeObjectURL(url); }, 4000);
        return true;
      } catch (e) { return false; }
    })(fileName, mimeType, content)
    """).unsafeCast<Boolean>()
}

internal actual fun webPlatformDownloadBase64Bytes(fileName: String, mimeType: String, base64Data: String): Boolean {
    return js("""
    (function(fn, mt, b64) {
      try {
        var bin = atob(b64);
        var bytes = new Uint8Array(bin.length);
        for (var i = 0; i < bin.length; i++) bytes[i] = bin.charCodeAt(i);
        var blob = new Blob([bytes], { type: mt });
        var url = URL.createObjectURL(blob);
        var a = document.createElement('a');
        a.href = url;
        a.download = fn;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        setTimeout(function() { URL.revokeObjectURL(url); }, 4000);
        return true;
      } catch (e) { return false; }
    })(fileName, mimeType, base64Data)
    """).unsafeCast<Boolean>()
}

internal actual fun webPlatformPickJsonFile(onLoaded: (fileName: String, content: String?) -> Unit) {
    val cb: (String, String) -> Unit = { name, body -> onLoaded(name, body.ifEmpty { null }) }
    js("""
    (function(callback) {
      try {
        var input = document.createElement('input');
        input.type = 'file';
        input.accept = '.json,application/json';
        input.style.display = 'none';
        input.onchange = function() {
          var f = input.files && input.files[0];
          if (!f) { document.body.removeChild(input); return; }
          var reader = new FileReader();
          reader.onload = function() {
            callback(f.name || 'circuit.json', String(reader.result || ''));
            document.body.removeChild(input);
          };
          reader.onerror = function() {
            callback(f.name || 'circuit.json', '');
            document.body.removeChild(input);
          };
          reader.readAsText(f);
        };
        document.body.appendChild(input);
        input.click();
      } catch (e) {}
    })(cb)
    """)
}

internal actual fun webPlatformBasePageUrl(): String =
    js("window.location.origin + window.location.pathname").unsafeCast<String>()

internal actual fun webPlatformEngineLabel(): String = "JS"

internal actual fun webPlatformSupportsFullscreen(): Boolean =
    js("Boolean(document.fullscreenEnabled || document.webkitFullscreenEnabled)").unsafeCast<Boolean>()

internal actual fun webPlatformIsFullscreen(): Boolean =
    js("Boolean(document.fullscreenElement || document.webkitFullscreenElement)").unsafeCast<Boolean>()

internal actual fun webPlatformToggleFullscreen(): Boolean {
    return js("""
    (function() {
      try {
        var el = document.documentElement;
        if (document.fullscreenElement || document.webkitFullscreenElement) {
          if (document.exitFullscreen) document.exitFullscreen().catch(function(){});
          else if (document.webkitExitFullscreen) document.webkitExitFullscreen();
          return false;
        }
        if (el.requestFullscreen) { el.requestFullscreen().catch(function(){}); return true; }
        if (el.webkitRequestFullscreen) { el.webkitRequestFullscreen(); return true; }
        return false;
      } catch (e) { return false; }
    })()
    """).unsafeCast<Boolean>()
}

internal actual fun webPlatformCanInstallPwa(): Boolean =
    js("Boolean(window.__logicLabsDeferredInstallPrompt)").unsafeCast<Boolean>()

internal actual fun webPlatformPromptInstallPwa(onResult: (Boolean) -> Unit) {
    val cb: (Boolean) -> Unit = onResult
    js("""
    (function(callback) {
      var p = window.__logicLabsDeferredInstallPrompt;
      if (!p) { callback(false); return; }
      try {
        p.prompt();
        p.userChoice.then(function(choice) {
          window.__logicLabsDeferredInstallPrompt = null;
          callback(Boolean(choice && choice.outcome === 'accepted'));
        }).catch(function() { callback(false); });
      } catch (e) { callback(false); }
    })(cb)
    """)
}

internal actual fun webPlatformCanNativeShare(): Boolean =
    js("Boolean(navigator.share)").unsafeCast<Boolean>()

internal actual fun webPlatformNativeShareUrl(title: String, text: String, url: String): Boolean {
    return js("""
    (function(ti, tx, u) {
      try {
        if (!navigator.share) return false;
        navigator.share({ title: ti, text: tx, url: u }).catch(function(){});
        return true;
      } catch (e) { return false; }
    })(title, text, url)
    """).unsafeCast<Boolean>()
}
