package com.logiclabs.feature.tools.report

internal actual fun webCopyToClipboard(text: String) {
    js("""
    (function(t) {
      try {
        if (navigator.clipboard && navigator.clipboard.writeText) {
          navigator.clipboard.writeText(t).catch(function() {});
        }
      } catch (e) {}
    })(text)
    """)
}
