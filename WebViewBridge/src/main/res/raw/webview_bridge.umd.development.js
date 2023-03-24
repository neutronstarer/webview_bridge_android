(function (factory) {
    typeof define === 'function' && define.amd ? define(factory) :
    factory();
}((function () { 'use strict';

    var Native =
    /**
     * Create native by namespace
     * @param namespace namespace
     */
    function Native(namespace) {
      var _this = this;
      /**
       *
       * @param message message object
       */
      this.send = function (message) {
        var wind = window;
        var m = {};
        m[_this.namespace] = message;
        try {
          var s = JSON.stringify(m);
          try {
            // android
            wind["webviewbridge/" + _this.namespace].postMessage(s);
            return;
          } catch (_) {}
          try {
            // ios
            wind.webkit.messageHandlers.webviewbridge.postMessage(s);
            return;
          } catch (_) {}
          // other
          _this.messages.push(m);
          _this.openUrl("https://webviewbridge?action=query&namespace=" + encodeURIComponent(_this.namespace));
        } catch (error) {
          console.error("[Bridge][Native] send fail: " + error);
        }
      };
      /**
       * Query messages by native
       * @returns serialized message list
       */
      this.query = function () {
        var v = JSON.stringify(_this.messages);
        _this.messages.splice(0, _this.messages.length);
        return v;
      };
      /**
       * Tell native to load or query
       * @param url url
       */
      this.openUrl = function (url) {
        var iframe = document.createElement('iframe');
        iframe.style.display = 'none';
        iframe.src = url;
        document.documentElement.appendChild(iframe);
        setTimeout(function () {
          document.documentElement.removeChild(iframe);
        }, 1);
      };
      this.messages = [];
      this.namespace = namespace;
    };
    var Nod = /*#__PURE__*/function () {
      function Nod(id, info, wind) {
        this.id = id;
        this.info = info;
        this.wind = wind;
      }
      var _proto = Nod.prototype;
      _proto.obj = function obj() {
        var v = {};
        v['id'] = this.id;
        v['info'] = this.info;
        v['href'] = this.wind.location.href;
        return v;
      };
      _proto.equalTo = function equalTo(nod) {
        return this.id == nod.id && this.wind == nod.wind;
      };
      _proto.send = function send(message) {
        this.wind.postMessage(JSON.stringify(message), "*");
      };
      return Nod;
    }();
    var Bridge = /*#__PURE__*/function () {
      /**
       *
       * @param namespace namespace
       */
      function Bridge(namespace) {
        var _this2 = this;
        this.receive = function (ev) {
          try {
            var source = ev.source,
              data = ev.data;
            var message = JSON.parse(data)[_this2.namespace];
            var typ = message.typ,
              from = message.from,
              to = message.to,
              body = message.body;
            if ("development" === "development") {
              console.log("[Bridge][Message] " + data);
            }
            if (to !== undefined || from === undefined) {
              return;
            }
            if (typ === "transmit") {
              var o = _this2.nods.get(from);
              if (o == undefined) {
                return;
              }
              if (source != (o == null ? void 0 : o.wind)) {
                throw "[Bridge][Transmit] window does not match for nod {\"id\":\"" + from + "\"}";
              }
              _this2["native"].send(message);
              return;
            }
            if (typ === "connect") {
              var info = body.info;
              var _o = _this2.nods.get(from);
              var n = new Nod(from, info, source);
              if (_o != undefined) {
                throw "[Bridge][Connect] duplicated, old nod " + JSON.stringify(_o.obj) + ", new nod " + JSON.stringify(n.obj);
              }
              _this2.nods.set(from, n);
              _this2["native"].send(message);
              return;
            }
            if (typ === "disconnect") {
              var _info = body.info;
              var _o2 = _this2.nods.get(from);
              var _n = new Nod(from, _info, source);
              if (_o2 == undefined) {
                return;
              }
              if (!_o2.equalTo(_n)) {
                throw "[Bridge][Disconnect] unmatched, old nod " + JSON.stringify(_o2.obj) + ", new nod " + JSON.stringify(_n.obj);
              }
              _this2.nods["delete"](from);
              _this2["native"].send(message);
              return;
            }
          } catch (_) {}
        };
        this.namespace = namespace;
        this["native"] = new Native(namespace);
        this.nods = new Map();
        this.load();
      }
      /**
       * query messages when no native handle work.
       * @returns json string of message array.
       */
      var _proto2 = Bridge.prototype;
      _proto2.query = function query() {
        return this["native"].query();
      }
      /**
       *  send message
       * @param str json string of message
       */;
      _proto2.send = function send(str) {
        try {
          var message = JSON.parse(str);
          var to = message[this.namespace].to;
          var n = this.nods.get(to);
          if (n == undefined) {
            throw "[Bridge][Transmit] nod {\"id\":\"" + to + "\"} is not found";
          }
          n.send(message);
        } catch (error) {
          console.error(error);
        }
      };
      _proto2.load = function load() {
        addEventListener("message", this.receive);
        addEventListener("unload", this.unload);
        // broadcast
        var m = {};
        m[this.namespace] = {
          typ: "load"
        };
        var broadcast = function broadcast(wind, message) {
          wind.postMessage(message, "*");
          var f = wind.frames;
          for (var i = 0, l = f.length; i < l; i++) {
            broadcast(f[i], message);
          }
        };
        broadcast(window, JSON.stringify(m));
      };
      _proto2.unload = function unload() {
        var _this3 = this;
        removeEventListener("message", this.receive);
        removeEventListener("unload", this.unload);
        /// disconnect all nods
        this.nods.forEach(function (nod) {
          var m = {
            typ: "disconnect",
            from: nod.id,
            body: {
              info: nod.info
            }
          };
          _this3["native"].send(m);
        });
        this.nods.clear();
      };
      return Bridge;
    }();
    var namespace = "<namespace>";
    var wind = window;
    var key = "webviewbridge/" + namespace;
    if (wind[key] == undefined) {
      wind[key] = /*#__PURE__*/new Bridge(namespace);
    }

})));
//# sourceMappingURL=webview_bridge.umd.development.js.map
