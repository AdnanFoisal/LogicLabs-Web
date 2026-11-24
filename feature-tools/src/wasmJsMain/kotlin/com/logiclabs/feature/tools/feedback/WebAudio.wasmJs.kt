package com.logiclabs.feature.tools.feedback

private fun jsPlaySfx(kind: Int): Unit = js("""
(() => {
  try {
    const AudioCtx = window.AudioContext || window.webkitAudioContext;
    if (!AudioCtx) return;
    if (!window.__llAudioCtx) {
      window.__llAudioCtx = new AudioCtx();
      window.__llSfxBufs = {};
    }
    const ctx = window.__llAudioCtx;
    if (ctx.state === 'suspended') {
      ctx.resume().catch(() => {});
    }
    if (!window.__llSfxBufs[kind]) {
      const sr = ctx.sampleRate || 44100;
      const ms = kind === 1 ? 6 : (kind === 2 ? 14 : 4);
      const len = Math.max(1, Math.floor(sr * ms / 1000));
      const buf = ctx.createBuffer(1, len, sr);
      const ch = buf.getChannelData(0);
      for (let i = 0; i < len; i++) {
        const t = i / sr;
        if (kind === 1) {
          const env = Math.exp(-t * 620.0);
          ch[i] = (0.38 * (Math.random() * 2 - 1) + 0.52 * Math.sin(2 * Math.PI * 1200 * t)) * env * 0.45;
        } else if (kind === 2) {
          const env = Math.exp(-t * 240.0);
          ch[i] = (0.75 * Math.sin(2 * Math.PI * 170 * t) + 0.25 * Math.sin(2 * Math.PI * 340 * t)) * env * 0.55;
        } else {
          const env = Math.exp(-t * 900.0);
          ch[i] = Math.sin(2 * Math.PI * 2200 * t) * env * 0.28;
        }
      }
      window.__llSfxBufs[kind] = buf;
    }
    const src = ctx.createBufferSource();
    src.buffer = window.__llSfxBufs[kind];
    src.connect(ctx.destination);
    src.start();
  } catch (e) {}
})()
""")

internal actual fun webAudioPlayKind(kind: Int) = jsPlaySfx(kind)

internal actual fun webAudioRelease() {
    // Keep shared browser AudioContext alive across route transitions
}
