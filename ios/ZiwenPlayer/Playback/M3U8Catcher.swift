import Foundation
import WebKit

/// 用隐藏的 WKWebView 加载播放页，通过注入脚本捕获 m3u8 地址。
/// Android 端靠 shouldInterceptRequest，iOS 的 WKWebView 没有该能力，只能走 JS Hook。
@MainActor
final class M3U8Catcher: NSObject, ObservableObject {

    var onCapture: ((String) -> Void)?
    var onTimeout: (() -> Void)?

    private var webView: WKWebView?
    private var timeoutTask: Task<Void, Never>?
    private var finished = false

    private static let captureScript = #"""
    (function(){
      if (window.__ziwenInjected) return;
      window.__ziwenInjected = true;
      function abs(u) { try { return new URL(u, location.href).href; } catch (e) { return u; } }
      function report(u, s) {
        try {
          if (!u) return;
          var str = String(u);
          if (str.indexOf('.m3u8') < 0) return;
          window.webkit.messageHandlers.capture.postMessage({ url: abs(str), source: s || '页面捕获' });
        } catch (e) {}
      }
      function parse(text) {
        try {
          if (typeof text !== 'string' || text.indexOf('#EXT-X-STREAM-INF') < 0) return;
          var lines = text.split(/\r?\n/);
          for (var i = 0; i < lines.length; i++) {
            var line = lines[i].trim();
            if (line && line.charAt(0) !== '#' && line.indexOf('.m3u8') >= 0) report(line, 'Master子链接');
          }
        } catch (e) {}
      }
      try {
        var rawOpen = XMLHttpRequest.prototype.open;
        XMLHttpRequest.prototype.open = function(method, url) { this.__ziwenURL = url; return rawOpen.apply(this, arguments); };
        var rawSend = XMLHttpRequest.prototype.send;
        XMLHttpRequest.prototype.send = function() {
          var self = this;
          try {
            self.addEventListener('load', function() {
              try {
                var u = String(self.__ziwenURL || '');
                if (u.indexOf('.m3u8') >= 0) report(u, 'XHR');
                var t = (self.responseType === '' || self.responseType === 'text') ? self.responseText : '';
                if (t) parse(t);
              } catch (e) {}
            });
          } catch (e) {}
          return rawSend.apply(this, arguments);
        };
      } catch (e) {}
      try {
        if (window.fetch) {
          var rawFetch = window.fetch;
          window.fetch = function() {
            var args = arguments;
            return rawFetch.apply(this, args).then(function(resp) {
              try {
                var u = (args[0] && args[0].url) ? args[0].url : args[0];
                if (String(u).indexOf('.m3u8') >= 0) report(u, 'Fetch');
              } catch (e) {}
              try { resp.clone().text().then(parse).catch(function(){}); } catch (e) {}
              return resp;
            });
          };
        }
      } catch (e) {}
      try {
        var observer = new PerformanceObserver(function(list) {
          var entries = list.getEntries();
          for (var i = 0; i < entries.length; i++) {
            if (String(entries[i].name).indexOf('.m3u8') >= 0) report(entries[i].name, 'Performance');
          }
        });
        observer.observe({ entryTypes: ['resource'] });
      } catch (e) {}
      function poll() {
        try {
          if (window.player_aaaa && window.player_aaaa.url) report(window.player_aaaa.url, 'player_aaaa');
        } catch (e) {}
      }
      setInterval(poll, 600);
      poll();
    })();
    """#

    func webViewIfNeeded() -> WKWebView {
        if let webView { return webView }
        let config = WKWebViewConfiguration()
        config.allowsInlineMediaPlayback = true
        config.mediaTypesRequiringUserActionForPlayback = []
        let script = WKUserScript(
            source: Self.captureScript,
            injectionTime: .atDocumentStart,
            forMainFrameOnly: false
        )
        config.userContentController.addUserScript(script)
        let created = WKWebView(frame: CGRect(x: 0, y: 0, width: 1, height: 1), configuration: config)
        created.isUserInteractionEnabled = false
        created.navigationDelegate = self
        webView = created
        return created
    }

    func start(_ urlString: String) {
        finished = false
        let view = webViewIfNeeded()
        view.configuration.userContentController.removeScriptMessageHandler(forName: "capture")
        view.configuration.userContentController.add(self, name: "capture")
        guard let url = URL(string: urlString) else {
            onTimeout?()
            return
        }
        view.load(URLRequest(url: url))
        timeoutTask?.cancel()
        timeoutTask = Task { [weak self] in
            try? await Task.sleep(nanoseconds: 9_000_000_000)
            guard !Task.isCancelled else { return }
            guard let self, !self.finished else { return }
            self.onTimeout?()
        }
    }

    func stop() {
        finished = true
        timeoutTask?.cancel()
        timeoutTask = nil
        webView?.configuration.userContentController.removeScriptMessageHandler(forName: "capture")
        webView?.stopLoading()
        webView?.loadHTMLString("", baseURL: nil)
    }

    private func handleCapture(_ url: String) {
        guard !finished else { return }
        finished = true
        timeoutTask?.cancel()
        timeoutTask = nil
        onCapture?(url)
    }
}

extension M3U8Catcher: WKNavigationDelegate {
    func webView(_ webView: WKWebView, didFinish navigation: WKNavigation!) {
        // 覆盖 iframe / 动态插入的情况，再注入一次
        webView.evaluateJavaScript(Self.captureScript, completionHandler: nil)
    }
}

extension M3U8Catcher: WKScriptMessageHandler {
    func userContentController(_ userContentController: WKUserContentController, didReceive message: WKScriptMessage) {
        guard message.name == "capture" else { return }
        let body = message.body
        if let dictionary = body as? [String: Any], let url = dictionary["url"] as? String {
            handleCapture(url)
        } else if let url = body as? String {
            handleCapture(url)
        }
    }
}
