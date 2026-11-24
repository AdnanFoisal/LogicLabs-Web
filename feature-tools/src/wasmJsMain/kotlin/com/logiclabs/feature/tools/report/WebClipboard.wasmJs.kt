package com.logiclabs.feature.tools.report

private fun jsCopyText(text: String): Unit = js("""
(() => {
  try {
    if (navigator.clipboard && navigator.clipboard.writeText) {
      navigator.clipboard.writeText(text).catch(() => {});
    }
  } catch (e) {}
})()
""")

internal actual fun webCopyToClipboard(text: String) = jsCopyText(text)
