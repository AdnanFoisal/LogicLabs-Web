package com.logiclabs.feature.tools.feedback

internal actual fun webAudioPlayKind(kind: Int) {
    js("""
    (function(k) {
      try {
        var AudioCtx = window.AudioContext || window.webkitAudioContext;
        if (!AudioCtx) return;
        if (!window.__llAudioCtx) {
          window.__llAudioCtx = new AudioCtx();
          window.__llSfxBufs = {};
        }
        var ctx = window.__llAudioCtx;
        if (ctx.state === 'suspended') {
          ctx.resume().catch(function() {});
        }
        if (!window.__llSfxBufs[k]) {
          var sr = ctx.sampleRate || 44100;
          var ms = k === 1 ? 6 : (k === 2 ? 14 : 4);
          var len = Math.max(1, Math.floor(sr * ms / 1000));
          var buf = ctx.createBuffer(1, len, sr);
          var ch = buf.getChannelData(0);
          for (var i = 0; i < len; i++) {
            var t = i / sr;
            if (k === 1) {
              var env1 = Math.exp(-t * 620.0);
              ch[i] = (0.38 * (Math.random() * 2 - 1) + 0.52 * Math.sin(2 * Math.PI * 1200 * t)) * env1 * 0.45;
            } else if (k === 2) {
              var env2 = Math.exp(-t * 240.0);
              ch[i] = (0.75 * Math.sin(2 * Math.PI * 170 * t) + 0.25 * Math.sin(2 * Math.PI * 340 * t)) * env2 * 0.55;
            } else {
              var env3 = Math.exp(-t * 900.0);
              ch[i] = Math.sin(2 * Math.PI * 2200 * t) * env3 * 0.28;
            }
          }
          window.__llSfxBufs[k] = buf;
        }
        var src = ctx.createBufferSource();
        src.buffer = window.__llSfxBufs[k];
        src.connect(ctx.destination);
        src.start();
      } catch (e) {}
    })(kind)
    """)
}

internal actual fun webAudioRelease() = Unit
