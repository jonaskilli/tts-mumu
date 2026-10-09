(() => {
  var __create = Object.create;
  var __defProp = Object.defineProperty;
  var __getOwnPropDesc = Object.getOwnPropertyDescriptor;
  var __getOwnPropNames = Object.getOwnPropertyNames;
  var __getProtoOf = Object.getPrototypeOf;
  var __hasOwnProp = Object.prototype.hasOwnProperty;
  var __commonJS = (cb, mod) => function __require() {
    return mod || (0, cb[__getOwnPropNames(cb)[0]])((mod = { exports: {} }).exports, mod), mod.exports;
  };
  var __copyProps = (to, from, except, desc) => {
    if (from && typeof from === "object" || typeof from === "function") {
      for (let key of __getOwnPropNames(from))
        if (!__hasOwnProp.call(to, key) && key !== except)
          __defProp(to, key, { get: () => from[key], enumerable: !(desc = __getOwnPropDesc(from, key)) || desc.enumerable });
    }
    return to;
  };
  var __toESM = (mod, isNodeMode, target) => (target = mod != null ? __create(__getProtoOf(mod)) : {}, __copyProps(
    // If the importer is in node compatibility mode or this is not an ESM
    // file that has been converted to a CommonJS file using a Babel-
    // compatible transform (i.e. "__esModule" has not been set), then set
    // "default" to the CommonJS "module.exports" for node compatibility.
    isNodeMode || !mod || !mod.__esModule ? __defProp(target, "default", { value: mod, enumerable: true }) : target,
    mod
  ));

  // node_modules/react/cjs/react.production.min.js
  var require_react_production_min = __commonJS({
    "node_modules/react/cjs/react.production.min.js"(exports) {
      "use strict";
      var l = Symbol.for("react.element");
      var n = Symbol.for("react.portal");
      var p = Symbol.for("react.fragment");
      var q = Symbol.for("react.strict_mode");
      var r = Symbol.for("react.profiler");
      var t = Symbol.for("react.provider");
      var u = Symbol.for("react.context");
      var v = Symbol.for("react.forward_ref");
      var w = Symbol.for("react.suspense");
      var x = Symbol.for("react.memo");
      var y = Symbol.for("react.lazy");
      var z = Symbol.iterator;
      function A(a) {
        if (null === a || "object" !== typeof a) return null;
        a = z && a[z] || a["@@iterator"];
        return "function" === typeof a ? a : null;
      }
      var B = { isMounted: function() {
        return false;
      }, enqueueForceUpdate: function() {
      }, enqueueReplaceState: function() {
      }, enqueueSetState: function() {
      } };
      var C = Object.assign;
      var D = {};
      function E(a, b, e) {
        this.props = a;
        this.context = b;
        this.refs = D;
        this.updater = e || B;
      }
      E.prototype.isReactComponent = {};
      E.prototype.setState = function(a, b) {
        if ("object" !== typeof a && "function" !== typeof a && null != a) throw Error("setState(...): takes an object of state variables to update or a function which returns an object of state variables.");
        this.updater.enqueueSetState(this, a, b, "setState");
      };
      E.prototype.forceUpdate = function(a) {
        this.updater.enqueueForceUpdate(this, a, "forceUpdate");
      };
      function F() {
      }
      F.prototype = E.prototype;
      function G(a, b, e) {
        this.props = a;
        this.context = b;
        this.refs = D;
        this.updater = e || B;
      }
      var H = G.prototype = new F();
      H.constructor = G;
      C(H, E.prototype);
      H.isPureReactComponent = true;
      var I = Array.isArray;
      var J = Object.prototype.hasOwnProperty;
      var K = { current: null };
      var L = { key: true, ref: true, __self: true, __source: true };
      function M(a, b, e) {
        var d, c = {}, k = null, h = null;
        if (null != b) for (d in void 0 !== b.ref && (h = b.ref), void 0 !== b.key && (k = "" + b.key), b) J.call(b, d) && !L.hasOwnProperty(d) && (c[d] = b[d]);
        var g = arguments.length - 2;
        if (1 === g) c.children = e;
        else if (1 < g) {
          for (var f = Array(g), m = 0; m < g; m++) f[m] = arguments[m + 2];
          c.children = f;
        }
        if (a && a.defaultProps) for (d in g = a.defaultProps, g) void 0 === c[d] && (c[d] = g[d]);
        return { $$typeof: l, type: a, key: k, ref: h, props: c, _owner: K.current };
      }
      function N(a, b) {
        return { $$typeof: l, type: a.type, key: b, ref: a.ref, props: a.props, _owner: a._owner };
      }
      function O(a) {
        return "object" === typeof a && null !== a && a.$$typeof === l;
      }
      function escape(a) {
        var b = { "=": "=0", ":": "=2" };
        return "$" + a.replace(/[=:]/g, function(a2) {
          return b[a2];
        });
      }
      var P = /\/+/g;
      function Q(a, b) {
        return "object" === typeof a && null !== a && null != a.key ? escape("" + a.key) : b.toString(36);
      }
      function R(a, b, e, d, c) {
        var k = typeof a;
        if ("undefined" === k || "boolean" === k) a = null;
        var h = false;
        if (null === a) h = true;
        else switch (k) {
          case "string":
          case "number":
            h = true;
            break;
          case "object":
            switch (a.$$typeof) {
              case l:
              case n:
                h = true;
            }
        }
        if (h) return h = a, c = c(h), a = "" === d ? "." + Q(h, 0) : d, I(c) ? (e = "", null != a && (e = a.replace(P, "$&/") + "/"), R(c, b, e, "", function(a2) {
          return a2;
        })) : null != c && (O(c) && (c = N(c, e + (!c.key || h && h.key === c.key ? "" : ("" + c.key).replace(P, "$&/") + "/") + a)), b.push(c)), 1;
        h = 0;
        d = "" === d ? "." : d + ":";
        if (I(a)) for (var g = 0; g < a.length; g++) {
          k = a[g];
          var f = d + Q(k, g);
          h += R(k, b, e, f, c);
        }
        else if (f = A(a), "function" === typeof f) for (a = f.call(a), g = 0; !(k = a.next()).done; ) k = k.value, f = d + Q(k, g++), h += R(k, b, e, f, c);
        else if ("object" === k) throw b = String(a), Error("Objects are not valid as a React child (found: " + ("[object Object]" === b ? "object with keys {" + Object.keys(a).join(", ") + "}" : b) + "). If you meant to render a collection of children, use an array instead.");
        return h;
      }
      function S(a, b, e) {
        if (null == a) return a;
        var d = [], c = 0;
        R(a, d, "", "", function(a2) {
          return b.call(e, a2, c++);
        });
        return d;
      }
      function T(a) {
        if (-1 === a._status) {
          var b = a._result;
          b = b();
          b.then(function(b2) {
            if (0 === a._status || -1 === a._status) a._status = 1, a._result = b2;
          }, function(b2) {
            if (0 === a._status || -1 === a._status) a._status = 2, a._result = b2;
          });
          -1 === a._status && (a._status = 0, a._result = b);
        }
        if (1 === a._status) return a._result.default;
        throw a._result;
      }
      var U = { current: null };
      var V = { transition: null };
      var W = { ReactCurrentDispatcher: U, ReactCurrentBatchConfig: V, ReactCurrentOwner: K };
      function X() {
        throw Error("act(...) is not supported in production builds of React.");
      }
      exports.Children = { map: S, forEach: function(a, b, e) {
        S(a, function() {
          b.apply(this, arguments);
        }, e);
      }, count: function(a) {
        var b = 0;
        S(a, function() {
          b++;
        });
        return b;
      }, toArray: function(a) {
        return S(a, function(a2) {
          return a2;
        }) || [];
      }, only: function(a) {
        if (!O(a)) throw Error("React.Children.only expected to receive a single React element child.");
        return a;
      } };
      exports.Component = E;
      exports.Fragment = p;
      exports.Profiler = r;
      exports.PureComponent = G;
      exports.StrictMode = q;
      exports.Suspense = w;
      exports.__SECRET_INTERNALS_DO_NOT_USE_OR_YOU_WILL_BE_FIRED = W;
      exports.act = X;
      exports.cloneElement = function(a, b, e) {
        if (null === a || void 0 === a) throw Error("React.cloneElement(...): The argument must be a React element, but you passed " + a + ".");
        var d = C({}, a.props), c = a.key, k = a.ref, h = a._owner;
        if (null != b) {
          void 0 !== b.ref && (k = b.ref, h = K.current);
          void 0 !== b.key && (c = "" + b.key);
          if (a.type && a.type.defaultProps) var g = a.type.defaultProps;
          for (f in b) J.call(b, f) && !L.hasOwnProperty(f) && (d[f] = void 0 === b[f] && void 0 !== g ? g[f] : b[f]);
        }
        var f = arguments.length - 2;
        if (1 === f) d.children = e;
        else if (1 < f) {
          g = Array(f);
          for (var m = 0; m < f; m++) g[m] = arguments[m + 2];
          d.children = g;
        }
        return { $$typeof: l, type: a.type, key: c, ref: k, props: d, _owner: h };
      };
      exports.createContext = function(a) {
        a = { $$typeof: u, _currentValue: a, _currentValue2: a, _threadCount: 0, Provider: null, Consumer: null, _defaultValue: null, _globalName: null };
        a.Provider = { $$typeof: t, _context: a };
        return a.Consumer = a;
      };
      exports.createElement = M;
      exports.createFactory = function(a) {
        var b = M.bind(null, a);
        b.type = a;
        return b;
      };
      exports.createRef = function() {
        return { current: null };
      };
      exports.forwardRef = function(a) {
        return { $$typeof: v, render: a };
      };
      exports.isValidElement = O;
      exports.lazy = function(a) {
        return { $$typeof: y, _payload: { _status: -1, _result: a }, _init: T };
      };
      exports.memo = function(a, b) {
        return { $$typeof: x, type: a, compare: void 0 === b ? null : b };
      };
      exports.startTransition = function(a) {
        var b = V.transition;
        V.transition = {};
        try {
          a();
        } finally {
          V.transition = b;
        }
      };
      exports.unstable_act = X;
      exports.useCallback = function(a, b) {
        return U.current.useCallback(a, b);
      };
      exports.useContext = function(a) {
        return U.current.useContext(a);
      };
      exports.useDebugValue = function() {
      };
      exports.useDeferredValue = function(a) {
        return U.current.useDeferredValue(a);
      };
      exports.useEffect = function(a, b) {
        return U.current.useEffect(a, b);
      };
      exports.useId = function() {
        return U.current.useId();
      };
      exports.useImperativeHandle = function(a, b, e) {
        return U.current.useImperativeHandle(a, b, e);
      };
      exports.useInsertionEffect = function(a, b) {
        return U.current.useInsertionEffect(a, b);
      };
      exports.useLayoutEffect = function(a, b) {
        return U.current.useLayoutEffect(a, b);
      };
      exports.useMemo = function(a, b) {
        return U.current.useMemo(a, b);
      };
      exports.useReducer = function(a, b, e) {
        return U.current.useReducer(a, b, e);
      };
      exports.useRef = function(a) {
        return U.current.useRef(a);
      };
      exports.useState = function(a) {
        return U.current.useState(a);
      };
      exports.useSyncExternalStore = function(a, b, e) {
        return U.current.useSyncExternalStore(a, b, e);
      };
      exports.useTransition = function() {
        return U.current.useTransition();
      };
      exports.version = "18.3.1";
    }
  });

  // node_modules/react/index.js
  var require_react = __commonJS({
    "node_modules/react/index.js"(exports, module) {
      "use strict";
      if (true) {
        module.exports = require_react_production_min();
      } else {
        module.exports = null;
      }
    }
  });

  // node_modules/scheduler/cjs/scheduler.production.min.js
  var require_scheduler_production_min = __commonJS({
    "node_modules/scheduler/cjs/scheduler.production.min.js"(exports) {
      "use strict";
      function f(a, b) {
        var c = a.length;
        a.push(b);
        a: for (; 0 < c; ) {
          var d = c - 1 >>> 1, e = a[d];
          if (0 < g(e, b)) a[d] = b, a[c] = e, c = d;
          else break a;
        }
      }
      function h(a) {
        return 0 === a.length ? null : a[0];
      }
      function k(a) {
        if (0 === a.length) return null;
        var b = a[0], c = a.pop();
        if (c !== b) {
          a[0] = c;
          a: for (var d = 0, e = a.length, w = e >>> 1; d < w; ) {
            var m = 2 * (d + 1) - 1, C = a[m], n = m + 1, x = a[n];
            if (0 > g(C, c)) n < e && 0 > g(x, C) ? (a[d] = x, a[n] = c, d = n) : (a[d] = C, a[m] = c, d = m);
            else if (n < e && 0 > g(x, c)) a[d] = x, a[n] = c, d = n;
            else break a;
          }
        }
        return b;
      }
      function g(a, b) {
        var c = a.sortIndex - b.sortIndex;
        return 0 !== c ? c : a.id - b.id;
      }
      if ("object" === typeof performance && "function" === typeof performance.now) {
        l = performance;
        exports.unstable_now = function() {
          return l.now();
        };
      } else {
        p = Date, q = p.now();
        exports.unstable_now = function() {
          return p.now() - q;
        };
      }
      var l;
      var p;
      var q;
      var r = [];
      var t = [];
      var u = 1;
      var v = null;
      var y = 3;
      var z = false;
      var A = false;
      var B = false;
      var D = "function" === typeof setTimeout ? setTimeout : null;
      var E = "function" === typeof clearTimeout ? clearTimeout : null;
      var F = "undefined" !== typeof setImmediate ? setImmediate : null;
      "undefined" !== typeof navigator && void 0 !== navigator.scheduling && void 0 !== navigator.scheduling.isInputPending && navigator.scheduling.isInputPending.bind(navigator.scheduling);
      function G(a) {
        for (var b = h(t); null !== b; ) {
          if (null === b.callback) k(t);
          else if (b.startTime <= a) k(t), b.sortIndex = b.expirationTime, f(r, b);
          else break;
          b = h(t);
        }
      }
      function H(a) {
        B = false;
        G(a);
        if (!A) if (null !== h(r)) A = true, I(J);
        else {
          var b = h(t);
          null !== b && K(H, b.startTime - a);
        }
      }
      function J(a, b) {
        A = false;
        B && (B = false, E(L), L = -1);
        z = true;
        var c = y;
        try {
          G(b);
          for (v = h(r); null !== v && (!(v.expirationTime > b) || a && !M()); ) {
            var d = v.callback;
            if ("function" === typeof d) {
              v.callback = null;
              y = v.priorityLevel;
              var e = d(v.expirationTime <= b);
              b = exports.unstable_now();
              "function" === typeof e ? v.callback = e : v === h(r) && k(r);
              G(b);
            } else k(r);
            v = h(r);
          }
          if (null !== v) var w = true;
          else {
            var m = h(t);
            null !== m && K(H, m.startTime - b);
            w = false;
          }
          return w;
        } finally {
          v = null, y = c, z = false;
        }
      }
      var N = false;
      var O = null;
      var L = -1;
      var P = 5;
      var Q = -1;
      function M() {
        return exports.unstable_now() - Q < P ? false : true;
      }
      function R() {
        if (null !== O) {
          var a = exports.unstable_now();
          Q = a;
          var b = true;
          try {
            b = O(true, a);
          } finally {
            b ? S() : (N = false, O = null);
          }
        } else N = false;
      }
      var S;
      if ("function" === typeof F) S = function() {
        F(R);
      };
      else if ("undefined" !== typeof MessageChannel) {
        T = new MessageChannel(), U = T.port2;
        T.port1.onmessage = R;
        S = function() {
          U.postMessage(null);
        };
      } else S = function() {
        D(R, 0);
      };
      var T;
      var U;
      function I(a) {
        O = a;
        N || (N = true, S());
      }
      function K(a, b) {
        L = D(function() {
          a(exports.unstable_now());
        }, b);
      }
      exports.unstable_IdlePriority = 5;
      exports.unstable_ImmediatePriority = 1;
      exports.unstable_LowPriority = 4;
      exports.unstable_NormalPriority = 3;
      exports.unstable_Profiling = null;
      exports.unstable_UserBlockingPriority = 2;
      exports.unstable_cancelCallback = function(a) {
        a.callback = null;
      };
      exports.unstable_continueExecution = function() {
        A || z || (A = true, I(J));
      };
      exports.unstable_forceFrameRate = function(a) {
        0 > a || 125 < a ? console.error("forceFrameRate takes a positive int between 0 and 125, forcing frame rates higher than 125 fps is not supported") : P = 0 < a ? Math.floor(1e3 / a) : 5;
      };
      exports.unstable_getCurrentPriorityLevel = function() {
        return y;
      };
      exports.unstable_getFirstCallbackNode = function() {
        return h(r);
      };
      exports.unstable_next = function(a) {
        switch (y) {
          case 1:
          case 2:
          case 3:
            var b = 3;
            break;
          default:
            b = y;
        }
        var c = y;
        y = b;
        try {
          return a();
        } finally {
          y = c;
        }
      };
      exports.unstable_pauseExecution = function() {
      };
      exports.unstable_requestPaint = function() {
      };
      exports.unstable_runWithPriority = function(a, b) {
        switch (a) {
          case 1:
          case 2:
          case 3:
          case 4:
          case 5:
            break;
          default:
            a = 3;
        }
        var c = y;
        y = a;
        try {
          return b();
        } finally {
          y = c;
        }
      };
      exports.unstable_scheduleCallback = function(a, b, c) {
        var d = exports.unstable_now();
        "object" === typeof c && null !== c ? (c = c.delay, c = "number" === typeof c && 0 < c ? d + c : d) : c = d;
        switch (a) {
          case 1:
            var e = -1;
            break;
          case 2:
            e = 250;
            break;
          case 5:
            e = 1073741823;
            break;
          case 4:
            e = 1e4;
            break;
          default:
            e = 5e3;
        }
        e = c + e;
        a = { id: u++, callback: b, priorityLevel: a, startTime: c, expirationTime: e, sortIndex: -1 };
        c > d ? (a.sortIndex = c, f(t, a), null === h(r) && a === h(t) && (B ? (E(L), L = -1) : B = true, K(H, c - d))) : (a.sortIndex = e, f(r, a), A || z || (A = true, I(J)));
        return a;
      };
      exports.unstable_shouldYield = M;
      exports.unstable_wrapCallback = function(a) {
        var b = y;
        return function() {
          var c = y;
          y = b;
          try {
            return a.apply(this, arguments);
          } finally {
            y = c;
          }
        };
      };
    }
  });

  // node_modules/scheduler/index.js
  var require_scheduler = __commonJS({
    "node_modules/scheduler/index.js"(exports, module) {
      "use strict";
      if (true) {
        module.exports = require_scheduler_production_min();
      } else {
        module.exports = null;
      }
    }
  });

  // node_modules/react-dom/cjs/react-dom.production.min.js
  var require_react_dom_production_min = __commonJS({
    "node_modules/react-dom/cjs/react-dom.production.min.js"(exports) {
      "use strict";
      var aa = require_react();
      var ca = require_scheduler();
      function p(a) {
        for (var b = "https://reactjs.org/docs/error-decoder.html?invariant=" + a, c = 1; c < arguments.length; c++) b += "&args[]=" + encodeURIComponent(arguments[c]);
        return "Minified React error #" + a + "; visit " + b + " for the full message or use the non-minified dev environment for full errors and additional helpful warnings.";
      }
      var da = /* @__PURE__ */ new Set();
      var ea = {};
      function fa(a, b) {
        ha(a, b);
        ha(a + "Capture", b);
      }
      function ha(a, b) {
        ea[a] = b;
        for (a = 0; a < b.length; a++) da.add(b[a]);
      }
      var ia = !("undefined" === typeof window || "undefined" === typeof window.document || "undefined" === typeof window.document.createElement);
      var ja = Object.prototype.hasOwnProperty;
      var ka = /^[:A-Z_a-z\u00C0-\u00D6\u00D8-\u00F6\u00F8-\u02FF\u0370-\u037D\u037F-\u1FFF\u200C-\u200D\u2070-\u218F\u2C00-\u2FEF\u3001-\uD7FF\uF900-\uFDCF\uFDF0-\uFFFD][:A-Z_a-z\u00C0-\u00D6\u00D8-\u00F6\u00F8-\u02FF\u0370-\u037D\u037F-\u1FFF\u200C-\u200D\u2070-\u218F\u2C00-\u2FEF\u3001-\uD7FF\uF900-\uFDCF\uFDF0-\uFFFD\-.0-9\u00B7\u0300-\u036F\u203F-\u2040]*$/;
      var la = {};
      var ma = {};
      function oa(a) {
        if (ja.call(ma, a)) return true;
        if (ja.call(la, a)) return false;
        if (ka.test(a)) return ma[a] = true;
        la[a] = true;
        return false;
      }
      function pa(a, b, c, d) {
        if (null !== c && 0 === c.type) return false;
        switch (typeof b) {
          case "function":
          case "symbol":
            return true;
          case "boolean":
            if (d) return false;
            if (null !== c) return !c.acceptsBooleans;
            a = a.toLowerCase().slice(0, 5);
            return "data-" !== a && "aria-" !== a;
          default:
            return false;
        }
      }
      function qa(a, b, c, d) {
        if (null === b || "undefined" === typeof b || pa(a, b, c, d)) return true;
        if (d) return false;
        if (null !== c) switch (c.type) {
          case 3:
            return !b;
          case 4:
            return false === b;
          case 5:
            return isNaN(b);
          case 6:
            return isNaN(b) || 1 > b;
        }
        return false;
      }
      function v(a, b, c, d, e, f, g) {
        this.acceptsBooleans = 2 === b || 3 === b || 4 === b;
        this.attributeName = d;
        this.attributeNamespace = e;
        this.mustUseProperty = c;
        this.propertyName = a;
        this.type = b;
        this.sanitizeURL = f;
        this.removeEmptyString = g;
      }
      var z = {};
      "children dangerouslySetInnerHTML defaultValue defaultChecked innerHTML suppressContentEditableWarning suppressHydrationWarning style".split(" ").forEach(function(a) {
        z[a] = new v(a, 0, false, a, null, false, false);
      });
      [["acceptCharset", "accept-charset"], ["className", "class"], ["htmlFor", "for"], ["httpEquiv", "http-equiv"]].forEach(function(a) {
        var b = a[0];
        z[b] = new v(b, 1, false, a[1], null, false, false);
      });
      ["contentEditable", "draggable", "spellCheck", "value"].forEach(function(a) {
        z[a] = new v(a, 2, false, a.toLowerCase(), null, false, false);
      });
      ["autoReverse", "externalResourcesRequired", "focusable", "preserveAlpha"].forEach(function(a) {
        z[a] = new v(a, 2, false, a, null, false, false);
      });
      "allowFullScreen async autoFocus autoPlay controls default defer disabled disablePictureInPicture disableRemotePlayback formNoValidate hidden loop noModule noValidate open playsInline readOnly required reversed scoped seamless itemScope".split(" ").forEach(function(a) {
        z[a] = new v(a, 3, false, a.toLowerCase(), null, false, false);
      });
      ["checked", "multiple", "muted", "selected"].forEach(function(a) {
        z[a] = new v(a, 3, true, a, null, false, false);
      });
      ["capture", "download"].forEach(function(a) {
        z[a] = new v(a, 4, false, a, null, false, false);
      });
      ["cols", "rows", "size", "span"].forEach(function(a) {
        z[a] = new v(a, 6, false, a, null, false, false);
      });
      ["rowSpan", "start"].forEach(function(a) {
        z[a] = new v(a, 5, false, a.toLowerCase(), null, false, false);
      });
      var ra = /[\-:]([a-z])/g;
      function sa(a) {
        return a[1].toUpperCase();
      }
      "accent-height alignment-baseline arabic-form baseline-shift cap-height clip-path clip-rule color-interpolation color-interpolation-filters color-profile color-rendering dominant-baseline enable-background fill-opacity fill-rule flood-color flood-opacity font-family font-size font-size-adjust font-stretch font-style font-variant font-weight glyph-name glyph-orientation-horizontal glyph-orientation-vertical horiz-adv-x horiz-origin-x image-rendering letter-spacing lighting-color marker-end marker-mid marker-start overline-position overline-thickness paint-order panose-1 pointer-events rendering-intent shape-rendering stop-color stop-opacity strikethrough-position strikethrough-thickness stroke-dasharray stroke-dashoffset stroke-linecap stroke-linejoin stroke-miterlimit stroke-opacity stroke-width text-anchor text-decoration text-rendering underline-position underline-thickness unicode-bidi unicode-range units-per-em v-alphabetic v-hanging v-ideographic v-mathematical vector-effect vert-adv-y vert-origin-x vert-origin-y word-spacing writing-mode xmlns:xlink x-height".split(" ").forEach(function(a) {
        var b = a.replace(
          ra,
          sa
        );
        z[b] = new v(b, 1, false, a, null, false, false);
      });
      "xlink:actuate xlink:arcrole xlink:role xlink:show xlink:title xlink:type".split(" ").forEach(function(a) {
        var b = a.replace(ra, sa);
        z[b] = new v(b, 1, false, a, "http://www.w3.org/1999/xlink", false, false);
      });
      ["xml:base", "xml:lang", "xml:space"].forEach(function(a) {
        var b = a.replace(ra, sa);
        z[b] = new v(b, 1, false, a, "http://www.w3.org/XML/1998/namespace", false, false);
      });
      ["tabIndex", "crossOrigin"].forEach(function(a) {
        z[a] = new v(a, 1, false, a.toLowerCase(), null, false, false);
      });
      z.xlinkHref = new v("xlinkHref", 1, false, "xlink:href", "http://www.w3.org/1999/xlink", true, false);
      ["src", "href", "action", "formAction"].forEach(function(a) {
        z[a] = new v(a, 1, false, a.toLowerCase(), null, true, true);
      });
      function ta(a, b, c, d) {
        var e = z.hasOwnProperty(b) ? z[b] : null;
        if (null !== e ? 0 !== e.type : d || !(2 < b.length) || "o" !== b[0] && "O" !== b[0] || "n" !== b[1] && "N" !== b[1]) qa(b, c, e, d) && (c = null), d || null === e ? oa(b) && (null === c ? a.removeAttribute(b) : a.setAttribute(b, "" + c)) : e.mustUseProperty ? a[e.propertyName] = null === c ? 3 === e.type ? false : "" : c : (b = e.attributeName, d = e.attributeNamespace, null === c ? a.removeAttribute(b) : (e = e.type, c = 3 === e || 4 === e && true === c ? "" : "" + c, d ? a.setAttributeNS(d, b, c) : a.setAttribute(b, c)));
      }
      var ua = aa.__SECRET_INTERNALS_DO_NOT_USE_OR_YOU_WILL_BE_FIRED;
      var va = Symbol.for("react.element");
      var wa = Symbol.for("react.portal");
      var ya = Symbol.for("react.fragment");
      var za = Symbol.for("react.strict_mode");
      var Aa = Symbol.for("react.profiler");
      var Ba = Symbol.for("react.provider");
      var Ca = Symbol.for("react.context");
      var Da = Symbol.for("react.forward_ref");
      var Ea = Symbol.for("react.suspense");
      var Fa = Symbol.for("react.suspense_list");
      var Ga = Symbol.for("react.memo");
      var Ha = Symbol.for("react.lazy");
      Symbol.for("react.scope");
      Symbol.for("react.debug_trace_mode");
      var Ia = Symbol.for("react.offscreen");
      Symbol.for("react.legacy_hidden");
      Symbol.for("react.cache");
      Symbol.for("react.tracing_marker");
      var Ja = Symbol.iterator;
      function Ka(a) {
        if (null === a || "object" !== typeof a) return null;
        a = Ja && a[Ja] || a["@@iterator"];
        return "function" === typeof a ? a : null;
      }
      var A = Object.assign;
      var La;
      function Ma(a) {
        if (void 0 === La) try {
          throw Error();
        } catch (c) {
          var b = c.stack.trim().match(/\n( *(at )?)/);
          La = b && b[1] || "";
        }
        return "\n" + La + a;
      }
      var Na = false;
      function Oa(a, b) {
        if (!a || Na) return "";
        Na = true;
        var c = Error.prepareStackTrace;
        Error.prepareStackTrace = void 0;
        try {
          if (b) if (b = function() {
            throw Error();
          }, Object.defineProperty(b.prototype, "props", { set: function() {
            throw Error();
          } }), "object" === typeof Reflect && Reflect.construct) {
            try {
              Reflect.construct(b, []);
            } catch (l) {
              var d = l;
            }
            Reflect.construct(a, [], b);
          } else {
            try {
              b.call();
            } catch (l) {
              d = l;
            }
            a.call(b.prototype);
          }
          else {
            try {
              throw Error();
            } catch (l) {
              d = l;
            }
            a();
          }
        } catch (l) {
          if (l && d && "string" === typeof l.stack) {
            for (var e = l.stack.split("\n"), f = d.stack.split("\n"), g = e.length - 1, h = f.length - 1; 1 <= g && 0 <= h && e[g] !== f[h]; ) h--;
            for (; 1 <= g && 0 <= h; g--, h--) if (e[g] !== f[h]) {
              if (1 !== g || 1 !== h) {
                do
                  if (g--, h--, 0 > h || e[g] !== f[h]) {
                    var k = "\n" + e[g].replace(" at new ", " at ");
                    a.displayName && k.includes("<anonymous>") && (k = k.replace("<anonymous>", a.displayName));
                    return k;
                  }
                while (1 <= g && 0 <= h);
              }
              break;
            }
          }
        } finally {
          Na = false, Error.prepareStackTrace = c;
        }
        return (a = a ? a.displayName || a.name : "") ? Ma(a) : "";
      }
      function Pa(a) {
        switch (a.tag) {
          case 5:
            return Ma(a.type);
          case 16:
            return Ma("Lazy");
          case 13:
            return Ma("Suspense");
          case 19:
            return Ma("SuspenseList");
          case 0:
          case 2:
          case 15:
            return a = Oa(a.type, false), a;
          case 11:
            return a = Oa(a.type.render, false), a;
          case 1:
            return a = Oa(a.type, true), a;
          default:
            return "";
        }
      }
      function Qa(a) {
        if (null == a) return null;
        if ("function" === typeof a) return a.displayName || a.name || null;
        if ("string" === typeof a) return a;
        switch (a) {
          case ya:
            return "Fragment";
          case wa:
            return "Portal";
          case Aa:
            return "Profiler";
          case za:
            return "StrictMode";
          case Ea:
            return "Suspense";
          case Fa:
            return "SuspenseList";
        }
        if ("object" === typeof a) switch (a.$$typeof) {
          case Ca:
            return (a.displayName || "Context") + ".Consumer";
          case Ba:
            return (a._context.displayName || "Context") + ".Provider";
          case Da:
            var b = a.render;
            a = a.displayName;
            a || (a = b.displayName || b.name || "", a = "" !== a ? "ForwardRef(" + a + ")" : "ForwardRef");
            return a;
          case Ga:
            return b = a.displayName || null, null !== b ? b : Qa(a.type) || "Memo";
          case Ha:
            b = a._payload;
            a = a._init;
            try {
              return Qa(a(b));
            } catch (c) {
            }
        }
        return null;
      }
      function Ra(a) {
        var b = a.type;
        switch (a.tag) {
          case 24:
            return "Cache";
          case 9:
            return (b.displayName || "Context") + ".Consumer";
          case 10:
            return (b._context.displayName || "Context") + ".Provider";
          case 18:
            return "DehydratedFragment";
          case 11:
            return a = b.render, a = a.displayName || a.name || "", b.displayName || ("" !== a ? "ForwardRef(" + a + ")" : "ForwardRef");
          case 7:
            return "Fragment";
          case 5:
            return b;
          case 4:
            return "Portal";
          case 3:
            return "Root";
          case 6:
            return "Text";
          case 16:
            return Qa(b);
          case 8:
            return b === za ? "StrictMode" : "Mode";
          case 22:
            return "Offscreen";
          case 12:
            return "Profiler";
          case 21:
            return "Scope";
          case 13:
            return "Suspense";
          case 19:
            return "SuspenseList";
          case 25:
            return "TracingMarker";
          case 1:
          case 0:
          case 17:
          case 2:
          case 14:
          case 15:
            if ("function" === typeof b) return b.displayName || b.name || null;
            if ("string" === typeof b) return b;
        }
        return null;
      }
      function Sa(a) {
        switch (typeof a) {
          case "boolean":
          case "number":
          case "string":
          case "undefined":
            return a;
          case "object":
            return a;
          default:
            return "";
        }
      }
      function Ta(a) {
        var b = a.type;
        return (a = a.nodeName) && "input" === a.toLowerCase() && ("checkbox" === b || "radio" === b);
      }
      function Ua(a) {
        var b = Ta(a) ? "checked" : "value", c = Object.getOwnPropertyDescriptor(a.constructor.prototype, b), d = "" + a[b];
        if (!a.hasOwnProperty(b) && "undefined" !== typeof c && "function" === typeof c.get && "function" === typeof c.set) {
          var e = c.get, f = c.set;
          Object.defineProperty(a, b, { configurable: true, get: function() {
            return e.call(this);
          }, set: function(a2) {
            d = "" + a2;
            f.call(this, a2);
          } });
          Object.defineProperty(a, b, { enumerable: c.enumerable });
          return { getValue: function() {
            return d;
          }, setValue: function(a2) {
            d = "" + a2;
          }, stopTracking: function() {
            a._valueTracker = null;
            delete a[b];
          } };
        }
      }
      function Va(a) {
        a._valueTracker || (a._valueTracker = Ua(a));
      }
      function Wa(a) {
        if (!a) return false;
        var b = a._valueTracker;
        if (!b) return true;
        var c = b.getValue();
        var d = "";
        a && (d = Ta(a) ? a.checked ? "true" : "false" : a.value);
        a = d;
        return a !== c ? (b.setValue(a), true) : false;
      }
      function Xa(a) {
        a = a || ("undefined" !== typeof document ? document : void 0);
        if ("undefined" === typeof a) return null;
        try {
          return a.activeElement || a.body;
        } catch (b) {
          return a.body;
        }
      }
      function Ya(a, b) {
        var c = b.checked;
        return A({}, b, { defaultChecked: void 0, defaultValue: void 0, value: void 0, checked: null != c ? c : a._wrapperState.initialChecked });
      }
      function Za(a, b) {
        var c = null == b.defaultValue ? "" : b.defaultValue, d = null != b.checked ? b.checked : b.defaultChecked;
        c = Sa(null != b.value ? b.value : c);
        a._wrapperState = { initialChecked: d, initialValue: c, controlled: "checkbox" === b.type || "radio" === b.type ? null != b.checked : null != b.value };
      }
      function ab(a, b) {
        b = b.checked;
        null != b && ta(a, "checked", b, false);
      }
      function bb(a, b) {
        ab(a, b);
        var c = Sa(b.value), d = b.type;
        if (null != c) if ("number" === d) {
          if (0 === c && "" === a.value || a.value != c) a.value = "" + c;
        } else a.value !== "" + c && (a.value = "" + c);
        else if ("submit" === d || "reset" === d) {
          a.removeAttribute("value");
          return;
        }
        b.hasOwnProperty("value") ? cb(a, b.type, c) : b.hasOwnProperty("defaultValue") && cb(a, b.type, Sa(b.defaultValue));
        null == b.checked && null != b.defaultChecked && (a.defaultChecked = !!b.defaultChecked);
      }
      function db(a, b, c) {
        if (b.hasOwnProperty("value") || b.hasOwnProperty("defaultValue")) {
          var d = b.type;
          if (!("submit" !== d && "reset" !== d || void 0 !== b.value && null !== b.value)) return;
          b = "" + a._wrapperState.initialValue;
          c || b === a.value || (a.value = b);
          a.defaultValue = b;
        }
        c = a.name;
        "" !== c && (a.name = "");
        a.defaultChecked = !!a._wrapperState.initialChecked;
        "" !== c && (a.name = c);
      }
      function cb(a, b, c) {
        if ("number" !== b || Xa(a.ownerDocument) !== a) null == c ? a.defaultValue = "" + a._wrapperState.initialValue : a.defaultValue !== "" + c && (a.defaultValue = "" + c);
      }
      var eb = Array.isArray;
      function fb(a, b, c, d) {
        a = a.options;
        if (b) {
          b = {};
          for (var e = 0; e < c.length; e++) b["$" + c[e]] = true;
          for (c = 0; c < a.length; c++) e = b.hasOwnProperty("$" + a[c].value), a[c].selected !== e && (a[c].selected = e), e && d && (a[c].defaultSelected = true);
        } else {
          c = "" + Sa(c);
          b = null;
          for (e = 0; e < a.length; e++) {
            if (a[e].value === c) {
              a[e].selected = true;
              d && (a[e].defaultSelected = true);
              return;
            }
            null !== b || a[e].disabled || (b = a[e]);
          }
          null !== b && (b.selected = true);
        }
      }
      function gb(a, b) {
        if (null != b.dangerouslySetInnerHTML) throw Error(p(91));
        return A({}, b, { value: void 0, defaultValue: void 0, children: "" + a._wrapperState.initialValue });
      }
      function hb(a, b) {
        var c = b.value;
        if (null == c) {
          c = b.children;
          b = b.defaultValue;
          if (null != c) {
            if (null != b) throw Error(p(92));
            if (eb(c)) {
              if (1 < c.length) throw Error(p(93));
              c = c[0];
            }
            b = c;
          }
          null == b && (b = "");
          c = b;
        }
        a._wrapperState = { initialValue: Sa(c) };
      }
      function ib(a, b) {
        var c = Sa(b.value), d = Sa(b.defaultValue);
        null != c && (c = "" + c, c !== a.value && (a.value = c), null == b.defaultValue && a.defaultValue !== c && (a.defaultValue = c));
        null != d && (a.defaultValue = "" + d);
      }
      function jb(a) {
        var b = a.textContent;
        b === a._wrapperState.initialValue && "" !== b && null !== b && (a.value = b);
      }
      function kb(a) {
        switch (a) {
          case "svg":
            return "http://www.w3.org/2000/svg";
          case "math":
            return "http://www.w3.org/1998/Math/MathML";
          default:
            return "http://www.w3.org/1999/xhtml";
        }
      }
      function lb(a, b) {
        return null == a || "http://www.w3.org/1999/xhtml" === a ? kb(b) : "http://www.w3.org/2000/svg" === a && "foreignObject" === b ? "http://www.w3.org/1999/xhtml" : a;
      }
      var mb;
      var nb = function(a) {
        return "undefined" !== typeof MSApp && MSApp.execUnsafeLocalFunction ? function(b, c, d, e) {
          MSApp.execUnsafeLocalFunction(function() {
            return a(b, c, d, e);
          });
        } : a;
      }(function(a, b) {
        if ("http://www.w3.org/2000/svg" !== a.namespaceURI || "innerHTML" in a) a.innerHTML = b;
        else {
          mb = mb || document.createElement("div");
          mb.innerHTML = "<svg>" + b.valueOf().toString() + "</svg>";
          for (b = mb.firstChild; a.firstChild; ) a.removeChild(a.firstChild);
          for (; b.firstChild; ) a.appendChild(b.firstChild);
        }
      });
      function ob(a, b) {
        if (b) {
          var c = a.firstChild;
          if (c && c === a.lastChild && 3 === c.nodeType) {
            c.nodeValue = b;
            return;
          }
        }
        a.textContent = b;
      }
      var pb = {
        animationIterationCount: true,
        aspectRatio: true,
        borderImageOutset: true,
        borderImageSlice: true,
        borderImageWidth: true,
        boxFlex: true,
        boxFlexGroup: true,
        boxOrdinalGroup: true,
        columnCount: true,
        columns: true,
        flex: true,
        flexGrow: true,
        flexPositive: true,
        flexShrink: true,
        flexNegative: true,
        flexOrder: true,
        gridArea: true,
        gridRow: true,
        gridRowEnd: true,
        gridRowSpan: true,
        gridRowStart: true,
        gridColumn: true,
        gridColumnEnd: true,
        gridColumnSpan: true,
        gridColumnStart: true,
        fontWeight: true,
        lineClamp: true,
        lineHeight: true,
        opacity: true,
        order: true,
        orphans: true,
        tabSize: true,
        widows: true,
        zIndex: true,
        zoom: true,
        fillOpacity: true,
        floodOpacity: true,
        stopOpacity: true,
        strokeDasharray: true,
        strokeDashoffset: true,
        strokeMiterlimit: true,
        strokeOpacity: true,
        strokeWidth: true
      };
      var qb = ["Webkit", "ms", "Moz", "O"];
      Object.keys(pb).forEach(function(a) {
        qb.forEach(function(b) {
          b = b + a.charAt(0).toUpperCase() + a.substring(1);
          pb[b] = pb[a];
        });
      });
      function rb(a, b, c) {
        return null == b || "boolean" === typeof b || "" === b ? "" : c || "number" !== typeof b || 0 === b || pb.hasOwnProperty(a) && pb[a] ? ("" + b).trim() : b + "px";
      }
      function sb(a, b) {
        a = a.style;
        for (var c in b) if (b.hasOwnProperty(c)) {
          var d = 0 === c.indexOf("--"), e = rb(c, b[c], d);
          "float" === c && (c = "cssFloat");
          d ? a.setProperty(c, e) : a[c] = e;
        }
      }
      var tb = A({ menuitem: true }, { area: true, base: true, br: true, col: true, embed: true, hr: true, img: true, input: true, keygen: true, link: true, meta: true, param: true, source: true, track: true, wbr: true });
      function ub(a, b) {
        if (b) {
          if (tb[a] && (null != b.children || null != b.dangerouslySetInnerHTML)) throw Error(p(137, a));
          if (null != b.dangerouslySetInnerHTML) {
            if (null != b.children) throw Error(p(60));
            if ("object" !== typeof b.dangerouslySetInnerHTML || !("__html" in b.dangerouslySetInnerHTML)) throw Error(p(61));
          }
          if (null != b.style && "object" !== typeof b.style) throw Error(p(62));
        }
      }
      function vb(a, b) {
        if (-1 === a.indexOf("-")) return "string" === typeof b.is;
        switch (a) {
          case "annotation-xml":
          case "color-profile":
          case "font-face":
          case "font-face-src":
          case "font-face-uri":
          case "font-face-format":
          case "font-face-name":
          case "missing-glyph":
            return false;
          default:
            return true;
        }
      }
      var wb = null;
      function xb(a) {
        a = a.target || a.srcElement || window;
        a.correspondingUseElement && (a = a.correspondingUseElement);
        return 3 === a.nodeType ? a.parentNode : a;
      }
      var yb = null;
      var zb = null;
      var Ab = null;
      function Bb(a) {
        if (a = Cb(a)) {
          if ("function" !== typeof yb) throw Error(p(280));
          var b = a.stateNode;
          b && (b = Db(b), yb(a.stateNode, a.type, b));
        }
      }
      function Eb(a) {
        zb ? Ab ? Ab.push(a) : Ab = [a] : zb = a;
      }
      function Fb() {
        if (zb) {
          var a = zb, b = Ab;
          Ab = zb = null;
          Bb(a);
          if (b) for (a = 0; a < b.length; a++) Bb(b[a]);
        }
      }
      function Gb(a, b) {
        return a(b);
      }
      function Hb() {
      }
      var Ib = false;
      function Jb(a, b, c) {
        if (Ib) return a(b, c);
        Ib = true;
        try {
          return Gb(a, b, c);
        } finally {
          if (Ib = false, null !== zb || null !== Ab) Hb(), Fb();
        }
      }
      function Kb(a, b) {
        var c = a.stateNode;
        if (null === c) return null;
        var d = Db(c);
        if (null === d) return null;
        c = d[b];
        a: switch (b) {
          case "onClick":
          case "onClickCapture":
          case "onDoubleClick":
          case "onDoubleClickCapture":
          case "onMouseDown":
          case "onMouseDownCapture":
          case "onMouseMove":
          case "onMouseMoveCapture":
          case "onMouseUp":
          case "onMouseUpCapture":
          case "onMouseEnter":
            (d = !d.disabled) || (a = a.type, d = !("button" === a || "input" === a || "select" === a || "textarea" === a));
            a = !d;
            break a;
          default:
            a = false;
        }
        if (a) return null;
        if (c && "function" !== typeof c) throw Error(p(231, b, typeof c));
        return c;
      }
      var Lb = false;
      if (ia) try {
        Mb = {};
        Object.defineProperty(Mb, "passive", { get: function() {
          Lb = true;
        } });
        window.addEventListener("test", Mb, Mb);
        window.removeEventListener("test", Mb, Mb);
      } catch (a) {
        Lb = false;
      }
      var Mb;
      function Nb(a, b, c, d, e, f, g, h, k) {
        var l = Array.prototype.slice.call(arguments, 3);
        try {
          b.apply(c, l);
        } catch (m) {
          this.onError(m);
        }
      }
      var Ob = false;
      var Pb = null;
      var Qb = false;
      var Rb = null;
      var Sb = { onError: function(a) {
        Ob = true;
        Pb = a;
      } };
      function Tb(a, b, c, d, e, f, g, h, k) {
        Ob = false;
        Pb = null;
        Nb.apply(Sb, arguments);
      }
      function Ub(a, b, c, d, e, f, g, h, k) {
        Tb.apply(this, arguments);
        if (Ob) {
          if (Ob) {
            var l = Pb;
            Ob = false;
            Pb = null;
          } else throw Error(p(198));
          Qb || (Qb = true, Rb = l);
        }
      }
      function Vb(a) {
        var b = a, c = a;
        if (a.alternate) for (; b.return; ) b = b.return;
        else {
          a = b;
          do
            b = a, 0 !== (b.flags & 4098) && (c = b.return), a = b.return;
          while (a);
        }
        return 3 === b.tag ? c : null;
      }
      function Wb(a) {
        if (13 === a.tag) {
          var b = a.memoizedState;
          null === b && (a = a.alternate, null !== a && (b = a.memoizedState));
          if (null !== b) return b.dehydrated;
        }
        return null;
      }
      function Xb(a) {
        if (Vb(a) !== a) throw Error(p(188));
      }
      function Yb(a) {
        var b = a.alternate;
        if (!b) {
          b = Vb(a);
          if (null === b) throw Error(p(188));
          return b !== a ? null : a;
        }
        for (var c = a, d = b; ; ) {
          var e = c.return;
          if (null === e) break;
          var f = e.alternate;
          if (null === f) {
            d = e.return;
            if (null !== d) {
              c = d;
              continue;
            }
            break;
          }
          if (e.child === f.child) {
            for (f = e.child; f; ) {
              if (f === c) return Xb(e), a;
              if (f === d) return Xb(e), b;
              f = f.sibling;
            }
            throw Error(p(188));
          }
          if (c.return !== d.return) c = e, d = f;
          else {
            for (var g = false, h = e.child; h; ) {
              if (h === c) {
                g = true;
                c = e;
                d = f;
                break;
              }
              if (h === d) {
                g = true;
                d = e;
                c = f;
                break;
              }
              h = h.sibling;
            }
            if (!g) {
              for (h = f.child; h; ) {
                if (h === c) {
                  g = true;
                  c = f;
                  d = e;
                  break;
                }
                if (h === d) {
                  g = true;
                  d = f;
                  c = e;
                  break;
                }
                h = h.sibling;
              }
              if (!g) throw Error(p(189));
            }
          }
          if (c.alternate !== d) throw Error(p(190));
        }
        if (3 !== c.tag) throw Error(p(188));
        return c.stateNode.current === c ? a : b;
      }
      function Zb(a) {
        a = Yb(a);
        return null !== a ? $b(a) : null;
      }
      function $b(a) {
        if (5 === a.tag || 6 === a.tag) return a;
        for (a = a.child; null !== a; ) {
          var b = $b(a);
          if (null !== b) return b;
          a = a.sibling;
        }
        return null;
      }
      var ac = ca.unstable_scheduleCallback;
      var bc = ca.unstable_cancelCallback;
      var cc = ca.unstable_shouldYield;
      var dc = ca.unstable_requestPaint;
      var B = ca.unstable_now;
      var ec = ca.unstable_getCurrentPriorityLevel;
      var fc = ca.unstable_ImmediatePriority;
      var gc = ca.unstable_UserBlockingPriority;
      var hc = ca.unstable_NormalPriority;
      var ic = ca.unstable_LowPriority;
      var jc = ca.unstable_IdlePriority;
      var kc = null;
      var lc = null;
      function mc(a) {
        if (lc && "function" === typeof lc.onCommitFiberRoot) try {
          lc.onCommitFiberRoot(kc, a, void 0, 128 === (a.current.flags & 128));
        } catch (b) {
        }
      }
      var oc = Math.clz32 ? Math.clz32 : nc;
      var pc = Math.log;
      var qc = Math.LN2;
      function nc(a) {
        a >>>= 0;
        return 0 === a ? 32 : 31 - (pc(a) / qc | 0) | 0;
      }
      var rc = 64;
      var sc = 4194304;
      function tc(a) {
        switch (a & -a) {
          case 1:
            return 1;
          case 2:
            return 2;
          case 4:
            return 4;
          case 8:
            return 8;
          case 16:
            return 16;
          case 32:
            return 32;
          case 64:
          case 128:
          case 256:
          case 512:
          case 1024:
          case 2048:
          case 4096:
          case 8192:
          case 16384:
          case 32768:
          case 65536:
          case 131072:
          case 262144:
          case 524288:
          case 1048576:
          case 2097152:
            return a & 4194240;
          case 4194304:
          case 8388608:
          case 16777216:
          case 33554432:
          case 67108864:
            return a & 130023424;
          case 134217728:
            return 134217728;
          case 268435456:
            return 268435456;
          case 536870912:
            return 536870912;
          case 1073741824:
            return 1073741824;
          default:
            return a;
        }
      }
      function uc(a, b) {
        var c = a.pendingLanes;
        if (0 === c) return 0;
        var d = 0, e = a.suspendedLanes, f = a.pingedLanes, g = c & 268435455;
        if (0 !== g) {
          var h = g & ~e;
          0 !== h ? d = tc(h) : (f &= g, 0 !== f && (d = tc(f)));
        } else g = c & ~e, 0 !== g ? d = tc(g) : 0 !== f && (d = tc(f));
        if (0 === d) return 0;
        if (0 !== b && b !== d && 0 === (b & e) && (e = d & -d, f = b & -b, e >= f || 16 === e && 0 !== (f & 4194240))) return b;
        0 !== (d & 4) && (d |= c & 16);
        b = a.entangledLanes;
        if (0 !== b) for (a = a.entanglements, b &= d; 0 < b; ) c = 31 - oc(b), e = 1 << c, d |= a[c], b &= ~e;
        return d;
      }
      function vc(a, b) {
        switch (a) {
          case 1:
          case 2:
          case 4:
            return b + 250;
          case 8:
          case 16:
          case 32:
          case 64:
          case 128:
          case 256:
          case 512:
          case 1024:
          case 2048:
          case 4096:
          case 8192:
          case 16384:
          case 32768:
          case 65536:
          case 131072:
          case 262144:
          case 524288:
          case 1048576:
          case 2097152:
            return b + 5e3;
          case 4194304:
          case 8388608:
          case 16777216:
          case 33554432:
          case 67108864:
            return -1;
          case 134217728:
          case 268435456:
          case 536870912:
          case 1073741824:
            return -1;
          default:
            return -1;
        }
      }
      function wc(a, b) {
        for (var c = a.suspendedLanes, d = a.pingedLanes, e = a.expirationTimes, f = a.pendingLanes; 0 < f; ) {
          var g = 31 - oc(f), h = 1 << g, k = e[g];
          if (-1 === k) {
            if (0 === (h & c) || 0 !== (h & d)) e[g] = vc(h, b);
          } else k <= b && (a.expiredLanes |= h);
          f &= ~h;
        }
      }
      function xc(a) {
        a = a.pendingLanes & -1073741825;
        return 0 !== a ? a : a & 1073741824 ? 1073741824 : 0;
      }
      function yc() {
        var a = rc;
        rc <<= 1;
        0 === (rc & 4194240) && (rc = 64);
        return a;
      }
      function zc(a) {
        for (var b = [], c = 0; 31 > c; c++) b.push(a);
        return b;
      }
      function Ac(a, b, c) {
        a.pendingLanes |= b;
        536870912 !== b && (a.suspendedLanes = 0, a.pingedLanes = 0);
        a = a.eventTimes;
        b = 31 - oc(b);
        a[b] = c;
      }
      function Bc(a, b) {
        var c = a.pendingLanes & ~b;
        a.pendingLanes = b;
        a.suspendedLanes = 0;
        a.pingedLanes = 0;
        a.expiredLanes &= b;
        a.mutableReadLanes &= b;
        a.entangledLanes &= b;
        b = a.entanglements;
        var d = a.eventTimes;
        for (a = a.expirationTimes; 0 < c; ) {
          var e = 31 - oc(c), f = 1 << e;
          b[e] = 0;
          d[e] = -1;
          a[e] = -1;
          c &= ~f;
        }
      }
      function Cc(a, b) {
        var c = a.entangledLanes |= b;
        for (a = a.entanglements; c; ) {
          var d = 31 - oc(c), e = 1 << d;
          e & b | a[d] & b && (a[d] |= b);
          c &= ~e;
        }
      }
      var C = 0;
      function Dc(a) {
        a &= -a;
        return 1 < a ? 4 < a ? 0 !== (a & 268435455) ? 16 : 536870912 : 4 : 1;
      }
      var Ec;
      var Fc;
      var Gc;
      var Hc;
      var Ic;
      var Jc = false;
      var Kc = [];
      var Lc = null;
      var Mc = null;
      var Nc = null;
      var Oc = /* @__PURE__ */ new Map();
      var Pc = /* @__PURE__ */ new Map();
      var Qc = [];
      var Rc = "mousedown mouseup touchcancel touchend touchstart auxclick dblclick pointercancel pointerdown pointerup dragend dragstart drop compositionend compositionstart keydown keypress keyup input textInput copy cut paste click change contextmenu reset submit".split(" ");
      function Sc(a, b) {
        switch (a) {
          case "focusin":
          case "focusout":
            Lc = null;
            break;
          case "dragenter":
          case "dragleave":
            Mc = null;
            break;
          case "mouseover":
          case "mouseout":
            Nc = null;
            break;
          case "pointerover":
          case "pointerout":
            Oc.delete(b.pointerId);
            break;
          case "gotpointercapture":
          case "lostpointercapture":
            Pc.delete(b.pointerId);
        }
      }
      function Tc(a, b, c, d, e, f) {
        if (null === a || a.nativeEvent !== f) return a = { blockedOn: b, domEventName: c, eventSystemFlags: d, nativeEvent: f, targetContainers: [e] }, null !== b && (b = Cb(b), null !== b && Fc(b)), a;
        a.eventSystemFlags |= d;
        b = a.targetContainers;
        null !== e && -1 === b.indexOf(e) && b.push(e);
        return a;
      }
      function Uc(a, b, c, d, e) {
        switch (b) {
          case "focusin":
            return Lc = Tc(Lc, a, b, c, d, e), true;
          case "dragenter":
            return Mc = Tc(Mc, a, b, c, d, e), true;
          case "mouseover":
            return Nc = Tc(Nc, a, b, c, d, e), true;
          case "pointerover":
            var f = e.pointerId;
            Oc.set(f, Tc(Oc.get(f) || null, a, b, c, d, e));
            return true;
          case "gotpointercapture":
            return f = e.pointerId, Pc.set(f, Tc(Pc.get(f) || null, a, b, c, d, e)), true;
        }
        return false;
      }
      function Vc(a) {
        var b = Wc(a.target);
        if (null !== b) {
          var c = Vb(b);
          if (null !== c) {
            if (b = c.tag, 13 === b) {
              if (b = Wb(c), null !== b) {
                a.blockedOn = b;
                Ic(a.priority, function() {
                  Gc(c);
                });
                return;
              }
            } else if (3 === b && c.stateNode.current.memoizedState.isDehydrated) {
              a.blockedOn = 3 === c.tag ? c.stateNode.containerInfo : null;
              return;
            }
          }
        }
        a.blockedOn = null;
      }
      function Xc(a) {
        if (null !== a.blockedOn) return false;
        for (var b = a.targetContainers; 0 < b.length; ) {
          var c = Yc(a.domEventName, a.eventSystemFlags, b[0], a.nativeEvent);
          if (null === c) {
            c = a.nativeEvent;
            var d = new c.constructor(c.type, c);
            wb = d;
            c.target.dispatchEvent(d);
            wb = null;
          } else return b = Cb(c), null !== b && Fc(b), a.blockedOn = c, false;
          b.shift();
        }
        return true;
      }
      function Zc(a, b, c) {
        Xc(a) && c.delete(b);
      }
      function $c() {
        Jc = false;
        null !== Lc && Xc(Lc) && (Lc = null);
        null !== Mc && Xc(Mc) && (Mc = null);
        null !== Nc && Xc(Nc) && (Nc = null);
        Oc.forEach(Zc);
        Pc.forEach(Zc);
      }
      function ad(a, b) {
        a.blockedOn === b && (a.blockedOn = null, Jc || (Jc = true, ca.unstable_scheduleCallback(ca.unstable_NormalPriority, $c)));
      }
      function bd(a) {
        function b(b2) {
          return ad(b2, a);
        }
        if (0 < Kc.length) {
          ad(Kc[0], a);
          for (var c = 1; c < Kc.length; c++) {
            var d = Kc[c];
            d.blockedOn === a && (d.blockedOn = null);
          }
        }
        null !== Lc && ad(Lc, a);
        null !== Mc && ad(Mc, a);
        null !== Nc && ad(Nc, a);
        Oc.forEach(b);
        Pc.forEach(b);
        for (c = 0; c < Qc.length; c++) d = Qc[c], d.blockedOn === a && (d.blockedOn = null);
        for (; 0 < Qc.length && (c = Qc[0], null === c.blockedOn); ) Vc(c), null === c.blockedOn && Qc.shift();
      }
      var cd = ua.ReactCurrentBatchConfig;
      var dd = true;
      function ed(a, b, c, d) {
        var e = C, f = cd.transition;
        cd.transition = null;
        try {
          C = 1, fd(a, b, c, d);
        } finally {
          C = e, cd.transition = f;
        }
      }
      function gd(a, b, c, d) {
        var e = C, f = cd.transition;
        cd.transition = null;
        try {
          C = 4, fd(a, b, c, d);
        } finally {
          C = e, cd.transition = f;
        }
      }
      function fd(a, b, c, d) {
        if (dd) {
          var e = Yc(a, b, c, d);
          if (null === e) hd(a, b, d, id, c), Sc(a, d);
          else if (Uc(e, a, b, c, d)) d.stopPropagation();
          else if (Sc(a, d), b & 4 && -1 < Rc.indexOf(a)) {
            for (; null !== e; ) {
              var f = Cb(e);
              null !== f && Ec(f);
              f = Yc(a, b, c, d);
              null === f && hd(a, b, d, id, c);
              if (f === e) break;
              e = f;
            }
            null !== e && d.stopPropagation();
          } else hd(a, b, d, null, c);
        }
      }
      var id = null;
      function Yc(a, b, c, d) {
        id = null;
        a = xb(d);
        a = Wc(a);
        if (null !== a) if (b = Vb(a), null === b) a = null;
        else if (c = b.tag, 13 === c) {
          a = Wb(b);
          if (null !== a) return a;
          a = null;
        } else if (3 === c) {
          if (b.stateNode.current.memoizedState.isDehydrated) return 3 === b.tag ? b.stateNode.containerInfo : null;
          a = null;
        } else b !== a && (a = null);
        id = a;
        return null;
      }
      function jd(a) {
        switch (a) {
          case "cancel":
          case "click":
          case "close":
          case "contextmenu":
          case "copy":
          case "cut":
          case "auxclick":
          case "dblclick":
          case "dragend":
          case "dragstart":
          case "drop":
          case "focusin":
          case "focusout":
          case "input":
          case "invalid":
          case "keydown":
          case "keypress":
          case "keyup":
          case "mousedown":
          case "mouseup":
          case "paste":
          case "pause":
          case "play":
          case "pointercancel":
          case "pointerdown":
          case "pointerup":
          case "ratechange":
          case "reset":
          case "resize":
          case "seeked":
          case "submit":
          case "touchcancel":
          case "touchend":
          case "touchstart":
          case "volumechange":
          case "change":
          case "selectionchange":
          case "textInput":
          case "compositionstart":
          case "compositionend":
          case "compositionupdate":
          case "beforeblur":
          case "afterblur":
          case "beforeinput":
          case "blur":
          case "fullscreenchange":
          case "focus":
          case "hashchange":
          case "popstate":
          case "select":
          case "selectstart":
            return 1;
          case "drag":
          case "dragenter":
          case "dragexit":
          case "dragleave":
          case "dragover":
          case "mousemove":
          case "mouseout":
          case "mouseover":
          case "pointermove":
          case "pointerout":
          case "pointerover":
          case "scroll":
          case "toggle":
          case "touchmove":
          case "wheel":
          case "mouseenter":
          case "mouseleave":
          case "pointerenter":
          case "pointerleave":
            return 4;
          case "message":
            switch (ec()) {
              case fc:
                return 1;
              case gc:
                return 4;
              case hc:
              case ic:
                return 16;
              case jc:
                return 536870912;
              default:
                return 16;
            }
          default:
            return 16;
        }
      }
      var kd = null;
      var ld = null;
      var md = null;
      function nd() {
        if (md) return md;
        var a, b = ld, c = b.length, d, e = "value" in kd ? kd.value : kd.textContent, f = e.length;
        for (a = 0; a < c && b[a] === e[a]; a++) ;
        var g = c - a;
        for (d = 1; d <= g && b[c - d] === e[f - d]; d++) ;
        return md = e.slice(a, 1 < d ? 1 - d : void 0);
      }
      function od(a) {
        var b = a.keyCode;
        "charCode" in a ? (a = a.charCode, 0 === a && 13 === b && (a = 13)) : a = b;
        10 === a && (a = 13);
        return 32 <= a || 13 === a ? a : 0;
      }
      function pd() {
        return true;
      }
      function qd() {
        return false;
      }
      function rd(a) {
        function b(b2, d, e, f, g) {
          this._reactName = b2;
          this._targetInst = e;
          this.type = d;
          this.nativeEvent = f;
          this.target = g;
          this.currentTarget = null;
          for (var c in a) a.hasOwnProperty(c) && (b2 = a[c], this[c] = b2 ? b2(f) : f[c]);
          this.isDefaultPrevented = (null != f.defaultPrevented ? f.defaultPrevented : false === f.returnValue) ? pd : qd;
          this.isPropagationStopped = qd;
          return this;
        }
        A(b.prototype, { preventDefault: function() {
          this.defaultPrevented = true;
          var a2 = this.nativeEvent;
          a2 && (a2.preventDefault ? a2.preventDefault() : "unknown" !== typeof a2.returnValue && (a2.returnValue = false), this.isDefaultPrevented = pd);
        }, stopPropagation: function() {
          var a2 = this.nativeEvent;
          a2 && (a2.stopPropagation ? a2.stopPropagation() : "unknown" !== typeof a2.cancelBubble && (a2.cancelBubble = true), this.isPropagationStopped = pd);
        }, persist: function() {
        }, isPersistent: pd });
        return b;
      }
      var sd = { eventPhase: 0, bubbles: 0, cancelable: 0, timeStamp: function(a) {
        return a.timeStamp || Date.now();
      }, defaultPrevented: 0, isTrusted: 0 };
      var td = rd(sd);
      var ud = A({}, sd, { view: 0, detail: 0 });
      var vd = rd(ud);
      var wd;
      var xd;
      var yd;
      var Ad = A({}, ud, { screenX: 0, screenY: 0, clientX: 0, clientY: 0, pageX: 0, pageY: 0, ctrlKey: 0, shiftKey: 0, altKey: 0, metaKey: 0, getModifierState: zd, button: 0, buttons: 0, relatedTarget: function(a) {
        return void 0 === a.relatedTarget ? a.fromElement === a.srcElement ? a.toElement : a.fromElement : a.relatedTarget;
      }, movementX: function(a) {
        if ("movementX" in a) return a.movementX;
        a !== yd && (yd && "mousemove" === a.type ? (wd = a.screenX - yd.screenX, xd = a.screenY - yd.screenY) : xd = wd = 0, yd = a);
        return wd;
      }, movementY: function(a) {
        return "movementY" in a ? a.movementY : xd;
      } });
      var Bd = rd(Ad);
      var Cd = A({}, Ad, { dataTransfer: 0 });
      var Dd = rd(Cd);
      var Ed = A({}, ud, { relatedTarget: 0 });
      var Fd = rd(Ed);
      var Gd = A({}, sd, { animationName: 0, elapsedTime: 0, pseudoElement: 0 });
      var Hd = rd(Gd);
      var Id = A({}, sd, { clipboardData: function(a) {
        return "clipboardData" in a ? a.clipboardData : window.clipboardData;
      } });
      var Jd = rd(Id);
      var Kd = A({}, sd, { data: 0 });
      var Ld = rd(Kd);
      var Md = {
        Esc: "Escape",
        Spacebar: " ",
        Left: "ArrowLeft",
        Up: "ArrowUp",
        Right: "ArrowRight",
        Down: "ArrowDown",
        Del: "Delete",
        Win: "OS",
        Menu: "ContextMenu",
        Apps: "ContextMenu",
        Scroll: "ScrollLock",
        MozPrintableKey: "Unidentified"
      };
      var Nd = {
        8: "Backspace",
        9: "Tab",
        12: "Clear",
        13: "Enter",
        16: "Shift",
        17: "Control",
        18: "Alt",
        19: "Pause",
        20: "CapsLock",
        27: "Escape",
        32: " ",
        33: "PageUp",
        34: "PageDown",
        35: "End",
        36: "Home",
        37: "ArrowLeft",
        38: "ArrowUp",
        39: "ArrowRight",
        40: "ArrowDown",
        45: "Insert",
        46: "Delete",
        112: "F1",
        113: "F2",
        114: "F3",
        115: "F4",
        116: "F5",
        117: "F6",
        118: "F7",
        119: "F8",
        120: "F9",
        121: "F10",
        122: "F11",
        123: "F12",
        144: "NumLock",
        145: "ScrollLock",
        224: "Meta"
      };
      var Od = { Alt: "altKey", Control: "ctrlKey", Meta: "metaKey", Shift: "shiftKey" };
      function Pd(a) {
        var b = this.nativeEvent;
        return b.getModifierState ? b.getModifierState(a) : (a = Od[a]) ? !!b[a] : false;
      }
      function zd() {
        return Pd;
      }
      var Qd = A({}, ud, { key: function(a) {
        if (a.key) {
          var b = Md[a.key] || a.key;
          if ("Unidentified" !== b) return b;
        }
        return "keypress" === a.type ? (a = od(a), 13 === a ? "Enter" : String.fromCharCode(a)) : "keydown" === a.type || "keyup" === a.type ? Nd[a.keyCode] || "Unidentified" : "";
      }, code: 0, location: 0, ctrlKey: 0, shiftKey: 0, altKey: 0, metaKey: 0, repeat: 0, locale: 0, getModifierState: zd, charCode: function(a) {
        return "keypress" === a.type ? od(a) : 0;
      }, keyCode: function(a) {
        return "keydown" === a.type || "keyup" === a.type ? a.keyCode : 0;
      }, which: function(a) {
        return "keypress" === a.type ? od(a) : "keydown" === a.type || "keyup" === a.type ? a.keyCode : 0;
      } });
      var Rd = rd(Qd);
      var Sd = A({}, Ad, { pointerId: 0, width: 0, height: 0, pressure: 0, tangentialPressure: 0, tiltX: 0, tiltY: 0, twist: 0, pointerType: 0, isPrimary: 0 });
      var Td = rd(Sd);
      var Ud = A({}, ud, { touches: 0, targetTouches: 0, changedTouches: 0, altKey: 0, metaKey: 0, ctrlKey: 0, shiftKey: 0, getModifierState: zd });
      var Vd = rd(Ud);
      var Wd = A({}, sd, { propertyName: 0, elapsedTime: 0, pseudoElement: 0 });
      var Xd = rd(Wd);
      var Yd = A({}, Ad, {
        deltaX: function(a) {
          return "deltaX" in a ? a.deltaX : "wheelDeltaX" in a ? -a.wheelDeltaX : 0;
        },
        deltaY: function(a) {
          return "deltaY" in a ? a.deltaY : "wheelDeltaY" in a ? -a.wheelDeltaY : "wheelDelta" in a ? -a.wheelDelta : 0;
        },
        deltaZ: 0,
        deltaMode: 0
      });
      var Zd = rd(Yd);
      var $d = [9, 13, 27, 32];
      var ae = ia && "CompositionEvent" in window;
      var be = null;
      ia && "documentMode" in document && (be = document.documentMode);
      var ce = ia && "TextEvent" in window && !be;
      var de = ia && (!ae || be && 8 < be && 11 >= be);
      var ee = String.fromCharCode(32);
      var fe = false;
      function ge(a, b) {
        switch (a) {
          case "keyup":
            return -1 !== $d.indexOf(b.keyCode);
          case "keydown":
            return 229 !== b.keyCode;
          case "keypress":
          case "mousedown":
          case "focusout":
            return true;
          default:
            return false;
        }
      }
      function he(a) {
        a = a.detail;
        return "object" === typeof a && "data" in a ? a.data : null;
      }
      var ie = false;
      function je(a, b) {
        switch (a) {
          case "compositionend":
            return he(b);
          case "keypress":
            if (32 !== b.which) return null;
            fe = true;
            return ee;
          case "textInput":
            return a = b.data, a === ee && fe ? null : a;
          default:
            return null;
        }
      }
      function ke(a, b) {
        if (ie) return "compositionend" === a || !ae && ge(a, b) ? (a = nd(), md = ld = kd = null, ie = false, a) : null;
        switch (a) {
          case "paste":
            return null;
          case "keypress":
            if (!(b.ctrlKey || b.altKey || b.metaKey) || b.ctrlKey && b.altKey) {
              if (b.char && 1 < b.char.length) return b.char;
              if (b.which) return String.fromCharCode(b.which);
            }
            return null;
          case "compositionend":
            return de && "ko" !== b.locale ? null : b.data;
          default:
            return null;
        }
      }
      var le = { color: true, date: true, datetime: true, "datetime-local": true, email: true, month: true, number: true, password: true, range: true, search: true, tel: true, text: true, time: true, url: true, week: true };
      function me(a) {
        var b = a && a.nodeName && a.nodeName.toLowerCase();
        return "input" === b ? !!le[a.type] : "textarea" === b ? true : false;
      }
      function ne(a, b, c, d) {
        Eb(d);
        b = oe(b, "onChange");
        0 < b.length && (c = new td("onChange", "change", null, c, d), a.push({ event: c, listeners: b }));
      }
      var pe = null;
      var qe = null;
      function re(a) {
        se(a, 0);
      }
      function te(a) {
        var b = ue(a);
        if (Wa(b)) return a;
      }
      function ve(a, b) {
        if ("change" === a) return b;
      }
      var we = false;
      if (ia) {
        if (ia) {
          ye = "oninput" in document;
          if (!ye) {
            ze = document.createElement("div");
            ze.setAttribute("oninput", "return;");
            ye = "function" === typeof ze.oninput;
          }
          xe = ye;
        } else xe = false;
        we = xe && (!document.documentMode || 9 < document.documentMode);
      }
      var xe;
      var ye;
      var ze;
      function Ae() {
        pe && (pe.detachEvent("onpropertychange", Be), qe = pe = null);
      }
      function Be(a) {
        if ("value" === a.propertyName && te(qe)) {
          var b = [];
          ne(b, qe, a, xb(a));
          Jb(re, b);
        }
      }
      function Ce(a, b, c) {
        "focusin" === a ? (Ae(), pe = b, qe = c, pe.attachEvent("onpropertychange", Be)) : "focusout" === a && Ae();
      }
      function De(a) {
        if ("selectionchange" === a || "keyup" === a || "keydown" === a) return te(qe);
      }
      function Ee(a, b) {
        if ("click" === a) return te(b);
      }
      function Fe(a, b) {
        if ("input" === a || "change" === a) return te(b);
      }
      function Ge(a, b) {
        return a === b && (0 !== a || 1 / a === 1 / b) || a !== a && b !== b;
      }
      var He = "function" === typeof Object.is ? Object.is : Ge;
      function Ie(a, b) {
        if (He(a, b)) return true;
        if ("object" !== typeof a || null === a || "object" !== typeof b || null === b) return false;
        var c = Object.keys(a), d = Object.keys(b);
        if (c.length !== d.length) return false;
        for (d = 0; d < c.length; d++) {
          var e = c[d];
          if (!ja.call(b, e) || !He(a[e], b[e])) return false;
        }
        return true;
      }
      function Je(a) {
        for (; a && a.firstChild; ) a = a.firstChild;
        return a;
      }
      function Ke(a, b) {
        var c = Je(a);
        a = 0;
        for (var d; c; ) {
          if (3 === c.nodeType) {
            d = a + c.textContent.length;
            if (a <= b && d >= b) return { node: c, offset: b - a };
            a = d;
          }
          a: {
            for (; c; ) {
              if (c.nextSibling) {
                c = c.nextSibling;
                break a;
              }
              c = c.parentNode;
            }
            c = void 0;
          }
          c = Je(c);
        }
      }
      function Le(a, b) {
        return a && b ? a === b ? true : a && 3 === a.nodeType ? false : b && 3 === b.nodeType ? Le(a, b.parentNode) : "contains" in a ? a.contains(b) : a.compareDocumentPosition ? !!(a.compareDocumentPosition(b) & 16) : false : false;
      }
      function Me() {
        for (var a = window, b = Xa(); b instanceof a.HTMLIFrameElement; ) {
          try {
            var c = "string" === typeof b.contentWindow.location.href;
          } catch (d) {
            c = false;
          }
          if (c) a = b.contentWindow;
          else break;
          b = Xa(a.document);
        }
        return b;
      }
      function Ne(a) {
        var b = a && a.nodeName && a.nodeName.toLowerCase();
        return b && ("input" === b && ("text" === a.type || "search" === a.type || "tel" === a.type || "url" === a.type || "password" === a.type) || "textarea" === b || "true" === a.contentEditable);
      }
      function Oe(a) {
        var b = Me(), c = a.focusedElem, d = a.selectionRange;
        if (b !== c && c && c.ownerDocument && Le(c.ownerDocument.documentElement, c)) {
          if (null !== d && Ne(c)) {
            if (b = d.start, a = d.end, void 0 === a && (a = b), "selectionStart" in c) c.selectionStart = b, c.selectionEnd = Math.min(a, c.value.length);
            else if (a = (b = c.ownerDocument || document) && b.defaultView || window, a.getSelection) {
              a = a.getSelection();
              var e = c.textContent.length, f = Math.min(d.start, e);
              d = void 0 === d.end ? f : Math.min(d.end, e);
              !a.extend && f > d && (e = d, d = f, f = e);
              e = Ke(c, f);
              var g = Ke(
                c,
                d
              );
              e && g && (1 !== a.rangeCount || a.anchorNode !== e.node || a.anchorOffset !== e.offset || a.focusNode !== g.node || a.focusOffset !== g.offset) && (b = b.createRange(), b.setStart(e.node, e.offset), a.removeAllRanges(), f > d ? (a.addRange(b), a.extend(g.node, g.offset)) : (b.setEnd(g.node, g.offset), a.addRange(b)));
            }
          }
          b = [];
          for (a = c; a = a.parentNode; ) 1 === a.nodeType && b.push({ element: a, left: a.scrollLeft, top: a.scrollTop });
          "function" === typeof c.focus && c.focus();
          for (c = 0; c < b.length; c++) a = b[c], a.element.scrollLeft = a.left, a.element.scrollTop = a.top;
        }
      }
      var Pe = ia && "documentMode" in document && 11 >= document.documentMode;
      var Qe = null;
      var Re = null;
      var Se = null;
      var Te = false;
      function Ue(a, b, c) {
        var d = c.window === c ? c.document : 9 === c.nodeType ? c : c.ownerDocument;
        Te || null == Qe || Qe !== Xa(d) || (d = Qe, "selectionStart" in d && Ne(d) ? d = { start: d.selectionStart, end: d.selectionEnd } : (d = (d.ownerDocument && d.ownerDocument.defaultView || window).getSelection(), d = { anchorNode: d.anchorNode, anchorOffset: d.anchorOffset, focusNode: d.focusNode, focusOffset: d.focusOffset }), Se && Ie(Se, d) || (Se = d, d = oe(Re, "onSelect"), 0 < d.length && (b = new td("onSelect", "select", null, b, c), a.push({ event: b, listeners: d }), b.target = Qe)));
      }
      function Ve(a, b) {
        var c = {};
        c[a.toLowerCase()] = b.toLowerCase();
        c["Webkit" + a] = "webkit" + b;
        c["Moz" + a] = "moz" + b;
        return c;
      }
      var We = { animationend: Ve("Animation", "AnimationEnd"), animationiteration: Ve("Animation", "AnimationIteration"), animationstart: Ve("Animation", "AnimationStart"), transitionend: Ve("Transition", "TransitionEnd") };
      var Xe = {};
      var Ye = {};
      ia && (Ye = document.createElement("div").style, "AnimationEvent" in window || (delete We.animationend.animation, delete We.animationiteration.animation, delete We.animationstart.animation), "TransitionEvent" in window || delete We.transitionend.transition);
      function Ze(a) {
        if (Xe[a]) return Xe[a];
        if (!We[a]) return a;
        var b = We[a], c;
        for (c in b) if (b.hasOwnProperty(c) && c in Ye) return Xe[a] = b[c];
        return a;
      }
      var $e = Ze("animationend");
      var af = Ze("animationiteration");
      var bf = Ze("animationstart");
      var cf = Ze("transitionend");
      var df = /* @__PURE__ */ new Map();
      var ef = "abort auxClick cancel canPlay canPlayThrough click close contextMenu copy cut drag dragEnd dragEnter dragExit dragLeave dragOver dragStart drop durationChange emptied encrypted ended error gotPointerCapture input invalid keyDown keyPress keyUp load loadedData loadedMetadata loadStart lostPointerCapture mouseDown mouseMove mouseOut mouseOver mouseUp paste pause play playing pointerCancel pointerDown pointerMove pointerOut pointerOver pointerUp progress rateChange reset resize seeked seeking stalled submit suspend timeUpdate touchCancel touchEnd touchStart volumeChange scroll toggle touchMove waiting wheel".split(" ");
      function ff(a, b) {
        df.set(a, b);
        fa(b, [a]);
      }
      for (gf = 0; gf < ef.length; gf++) {
        hf = ef[gf], jf = hf.toLowerCase(), kf = hf[0].toUpperCase() + hf.slice(1);
        ff(jf, "on" + kf);
      }
      var hf;
      var jf;
      var kf;
      var gf;
      ff($e, "onAnimationEnd");
      ff(af, "onAnimationIteration");
      ff(bf, "onAnimationStart");
      ff("dblclick", "onDoubleClick");
      ff("focusin", "onFocus");
      ff("focusout", "onBlur");
      ff(cf, "onTransitionEnd");
      ha("onMouseEnter", ["mouseout", "mouseover"]);
      ha("onMouseLeave", ["mouseout", "mouseover"]);
      ha("onPointerEnter", ["pointerout", "pointerover"]);
      ha("onPointerLeave", ["pointerout", "pointerover"]);
      fa("onChange", "change click focusin focusout input keydown keyup selectionchange".split(" "));
      fa("onSelect", "focusout contextmenu dragend focusin keydown keyup mousedown mouseup selectionchange".split(" "));
      fa("onBeforeInput", ["compositionend", "keypress", "textInput", "paste"]);
      fa("onCompositionEnd", "compositionend focusout keydown keypress keyup mousedown".split(" "));
      fa("onCompositionStart", "compositionstart focusout keydown keypress keyup mousedown".split(" "));
      fa("onCompositionUpdate", "compositionupdate focusout keydown keypress keyup mousedown".split(" "));
      var lf = "abort canplay canplaythrough durationchange emptied encrypted ended error loadeddata loadedmetadata loadstart pause play playing progress ratechange resize seeked seeking stalled suspend timeupdate volumechange waiting".split(" ");
      var mf = new Set("cancel close invalid load scroll toggle".split(" ").concat(lf));
      function nf(a, b, c) {
        var d = a.type || "unknown-event";
        a.currentTarget = c;
        Ub(d, b, void 0, a);
        a.currentTarget = null;
      }
      function se(a, b) {
        b = 0 !== (b & 4);
        for (var c = 0; c < a.length; c++) {
          var d = a[c], e = d.event;
          d = d.listeners;
          a: {
            var f = void 0;
            if (b) for (var g = d.length - 1; 0 <= g; g--) {
              var h = d[g], k = h.instance, l = h.currentTarget;
              h = h.listener;
              if (k !== f && e.isPropagationStopped()) break a;
              nf(e, h, l);
              f = k;
            }
            else for (g = 0; g < d.length; g++) {
              h = d[g];
              k = h.instance;
              l = h.currentTarget;
              h = h.listener;
              if (k !== f && e.isPropagationStopped()) break a;
              nf(e, h, l);
              f = k;
            }
          }
        }
        if (Qb) throw a = Rb, Qb = false, Rb = null, a;
      }
      function D(a, b) {
        var c = b[of];
        void 0 === c && (c = b[of] = /* @__PURE__ */ new Set());
        var d = a + "__bubble";
        c.has(d) || (pf(b, a, 2, false), c.add(d));
      }
      function qf(a, b, c) {
        var d = 0;
        b && (d |= 4);
        pf(c, a, d, b);
      }
      var rf = "_reactListening" + Math.random().toString(36).slice(2);
      function sf(a) {
        if (!a[rf]) {
          a[rf] = true;
          da.forEach(function(b2) {
            "selectionchange" !== b2 && (mf.has(b2) || qf(b2, false, a), qf(b2, true, a));
          });
          var b = 9 === a.nodeType ? a : a.ownerDocument;
          null === b || b[rf] || (b[rf] = true, qf("selectionchange", false, b));
        }
      }
      function pf(a, b, c, d) {
        switch (jd(b)) {
          case 1:
            var e = ed;
            break;
          case 4:
            e = gd;
            break;
          default:
            e = fd;
        }
        c = e.bind(null, b, c, a);
        e = void 0;
        !Lb || "touchstart" !== b && "touchmove" !== b && "wheel" !== b || (e = true);
        d ? void 0 !== e ? a.addEventListener(b, c, { capture: true, passive: e }) : a.addEventListener(b, c, true) : void 0 !== e ? a.addEventListener(b, c, { passive: e }) : a.addEventListener(b, c, false);
      }
      function hd(a, b, c, d, e) {
        var f = d;
        if (0 === (b & 1) && 0 === (b & 2) && null !== d) a: for (; ; ) {
          if (null === d) return;
          var g = d.tag;
          if (3 === g || 4 === g) {
            var h = d.stateNode.containerInfo;
            if (h === e || 8 === h.nodeType && h.parentNode === e) break;
            if (4 === g) for (g = d.return; null !== g; ) {
              var k = g.tag;
              if (3 === k || 4 === k) {
                if (k = g.stateNode.containerInfo, k === e || 8 === k.nodeType && k.parentNode === e) return;
              }
              g = g.return;
            }
            for (; null !== h; ) {
              g = Wc(h);
              if (null === g) return;
              k = g.tag;
              if (5 === k || 6 === k) {
                d = f = g;
                continue a;
              }
              h = h.parentNode;
            }
          }
          d = d.return;
        }
        Jb(function() {
          var d2 = f, e2 = xb(c), g2 = [];
          a: {
            var h2 = df.get(a);
            if (void 0 !== h2) {
              var k2 = td, n = a;
              switch (a) {
                case "keypress":
                  if (0 === od(c)) break a;
                case "keydown":
                case "keyup":
                  k2 = Rd;
                  break;
                case "focusin":
                  n = "focus";
                  k2 = Fd;
                  break;
                case "focusout":
                  n = "blur";
                  k2 = Fd;
                  break;
                case "beforeblur":
                case "afterblur":
                  k2 = Fd;
                  break;
                case "click":
                  if (2 === c.button) break a;
                case "auxclick":
                case "dblclick":
                case "mousedown":
                case "mousemove":
                case "mouseup":
                case "mouseout":
                case "mouseover":
                case "contextmenu":
                  k2 = Bd;
                  break;
                case "drag":
                case "dragend":
                case "dragenter":
                case "dragexit":
                case "dragleave":
                case "dragover":
                case "dragstart":
                case "drop":
                  k2 = Dd;
                  break;
                case "touchcancel":
                case "touchend":
                case "touchmove":
                case "touchstart":
                  k2 = Vd;
                  break;
                case $e:
                case af:
                case bf:
                  k2 = Hd;
                  break;
                case cf:
                  k2 = Xd;
                  break;
                case "scroll":
                  k2 = vd;
                  break;
                case "wheel":
                  k2 = Zd;
                  break;
                case "copy":
                case "cut":
                case "paste":
                  k2 = Jd;
                  break;
                case "gotpointercapture":
                case "lostpointercapture":
                case "pointercancel":
                case "pointerdown":
                case "pointermove":
                case "pointerout":
                case "pointerover":
                case "pointerup":
                  k2 = Td;
              }
              var t = 0 !== (b & 4), J = !t && "scroll" === a, x = t ? null !== h2 ? h2 + "Capture" : null : h2;
              t = [];
              for (var w = d2, u; null !== w; ) {
                u = w;
                var F = u.stateNode;
                5 === u.tag && null !== F && (u = F, null !== x && (F = Kb(w, x), null != F && t.push(tf(w, F, u))));
                if (J) break;
                w = w.return;
              }
              0 < t.length && (h2 = new k2(h2, n, null, c, e2), g2.push({ event: h2, listeners: t }));
            }
          }
          if (0 === (b & 7)) {
            a: {
              h2 = "mouseover" === a || "pointerover" === a;
              k2 = "mouseout" === a || "pointerout" === a;
              if (h2 && c !== wb && (n = c.relatedTarget || c.fromElement) && (Wc(n) || n[uf])) break a;
              if (k2 || h2) {
                h2 = e2.window === e2 ? e2 : (h2 = e2.ownerDocument) ? h2.defaultView || h2.parentWindow : window;
                if (k2) {
                  if (n = c.relatedTarget || c.toElement, k2 = d2, n = n ? Wc(n) : null, null !== n && (J = Vb(n), n !== J || 5 !== n.tag && 6 !== n.tag)) n = null;
                } else k2 = null, n = d2;
                if (k2 !== n) {
                  t = Bd;
                  F = "onMouseLeave";
                  x = "onMouseEnter";
                  w = "mouse";
                  if ("pointerout" === a || "pointerover" === a) t = Td, F = "onPointerLeave", x = "onPointerEnter", w = "pointer";
                  J = null == k2 ? h2 : ue(k2);
                  u = null == n ? h2 : ue(n);
                  h2 = new t(F, w + "leave", k2, c, e2);
                  h2.target = J;
                  h2.relatedTarget = u;
                  F = null;
                  Wc(e2) === d2 && (t = new t(x, w + "enter", n, c, e2), t.target = u, t.relatedTarget = J, F = t);
                  J = F;
                  if (k2 && n) b: {
                    t = k2;
                    x = n;
                    w = 0;
                    for (u = t; u; u = vf(u)) w++;
                    u = 0;
                    for (F = x; F; F = vf(F)) u++;
                    for (; 0 < w - u; ) t = vf(t), w--;
                    for (; 0 < u - w; ) x = vf(x), u--;
                    for (; w--; ) {
                      if (t === x || null !== x && t === x.alternate) break b;
                      t = vf(t);
                      x = vf(x);
                    }
                    t = null;
                  }
                  else t = null;
                  null !== k2 && wf(g2, h2, k2, t, false);
                  null !== n && null !== J && wf(g2, J, n, t, true);
                }
              }
            }
            a: {
              h2 = d2 ? ue(d2) : window;
              k2 = h2.nodeName && h2.nodeName.toLowerCase();
              if ("select" === k2 || "input" === k2 && "file" === h2.type) var na = ve;
              else if (me(h2)) if (we) na = Fe;
              else {
                na = De;
                var xa = Ce;
              }
              else (k2 = h2.nodeName) && "input" === k2.toLowerCase() && ("checkbox" === h2.type || "radio" === h2.type) && (na = Ee);
              if (na && (na = na(a, d2))) {
                ne(g2, na, c, e2);
                break a;
              }
              xa && xa(a, h2, d2);
              "focusout" === a && (xa = h2._wrapperState) && xa.controlled && "number" === h2.type && cb(h2, "number", h2.value);
            }
            xa = d2 ? ue(d2) : window;
            switch (a) {
              case "focusin":
                if (me(xa) || "true" === xa.contentEditable) Qe = xa, Re = d2, Se = null;
                break;
              case "focusout":
                Se = Re = Qe = null;
                break;
              case "mousedown":
                Te = true;
                break;
              case "contextmenu":
              case "mouseup":
              case "dragend":
                Te = false;
                Ue(g2, c, e2);
                break;
              case "selectionchange":
                if (Pe) break;
              case "keydown":
              case "keyup":
                Ue(g2, c, e2);
            }
            var $a;
            if (ae) b: {
              switch (a) {
                case "compositionstart":
                  var ba = "onCompositionStart";
                  break b;
                case "compositionend":
                  ba = "onCompositionEnd";
                  break b;
                case "compositionupdate":
                  ba = "onCompositionUpdate";
                  break b;
              }
              ba = void 0;
            }
            else ie ? ge(a, c) && (ba = "onCompositionEnd") : "keydown" === a && 229 === c.keyCode && (ba = "onCompositionStart");
            ba && (de && "ko" !== c.locale && (ie || "onCompositionStart" !== ba ? "onCompositionEnd" === ba && ie && ($a = nd()) : (kd = e2, ld = "value" in kd ? kd.value : kd.textContent, ie = true)), xa = oe(d2, ba), 0 < xa.length && (ba = new Ld(ba, a, null, c, e2), g2.push({ event: ba, listeners: xa }), $a ? ba.data = $a : ($a = he(c), null !== $a && (ba.data = $a))));
            if ($a = ce ? je(a, c) : ke(a, c)) d2 = oe(d2, "onBeforeInput"), 0 < d2.length && (e2 = new Ld("onBeforeInput", "beforeinput", null, c, e2), g2.push({ event: e2, listeners: d2 }), e2.data = $a);
          }
          se(g2, b);
        });
      }
      function tf(a, b, c) {
        return { instance: a, listener: b, currentTarget: c };
      }
      function oe(a, b) {
        for (var c = b + "Capture", d = []; null !== a; ) {
          var e = a, f = e.stateNode;
          5 === e.tag && null !== f && (e = f, f = Kb(a, c), null != f && d.unshift(tf(a, f, e)), f = Kb(a, b), null != f && d.push(tf(a, f, e)));
          a = a.return;
        }
        return d;
      }
      function vf(a) {
        if (null === a) return null;
        do
          a = a.return;
        while (a && 5 !== a.tag);
        return a ? a : null;
      }
      function wf(a, b, c, d, e) {
        for (var f = b._reactName, g = []; null !== c && c !== d; ) {
          var h = c, k = h.alternate, l = h.stateNode;
          if (null !== k && k === d) break;
          5 === h.tag && null !== l && (h = l, e ? (k = Kb(c, f), null != k && g.unshift(tf(c, k, h))) : e || (k = Kb(c, f), null != k && g.push(tf(c, k, h))));
          c = c.return;
        }
        0 !== g.length && a.push({ event: b, listeners: g });
      }
      var xf = /\r\n?/g;
      var yf = /\u0000|\uFFFD/g;
      function zf(a) {
        return ("string" === typeof a ? a : "" + a).replace(xf, "\n").replace(yf, "");
      }
      function Af(a, b, c) {
        b = zf(b);
        if (zf(a) !== b && c) throw Error(p(425));
      }
      function Bf() {
      }
      var Cf = null;
      var Df = null;
      function Ef(a, b) {
        return "textarea" === a || "noscript" === a || "string" === typeof b.children || "number" === typeof b.children || "object" === typeof b.dangerouslySetInnerHTML && null !== b.dangerouslySetInnerHTML && null != b.dangerouslySetInnerHTML.__html;
      }
      var Ff = "function" === typeof setTimeout ? setTimeout : void 0;
      var Gf = "function" === typeof clearTimeout ? clearTimeout : void 0;
      var Hf = "function" === typeof Promise ? Promise : void 0;
      var Jf = "function" === typeof queueMicrotask ? queueMicrotask : "undefined" !== typeof Hf ? function(a) {
        return Hf.resolve(null).then(a).catch(If);
      } : Ff;
      function If(a) {
        setTimeout(function() {
          throw a;
        });
      }
      function Kf(a, b) {
        var c = b, d = 0;
        do {
          var e = c.nextSibling;
          a.removeChild(c);
          if (e && 8 === e.nodeType) if (c = e.data, "/$" === c) {
            if (0 === d) {
              a.removeChild(e);
              bd(b);
              return;
            }
            d--;
          } else "$" !== c && "$?" !== c && "$!" !== c || d++;
          c = e;
        } while (c);
        bd(b);
      }
      function Lf(a) {
        for (; null != a; a = a.nextSibling) {
          var b = a.nodeType;
          if (1 === b || 3 === b) break;
          if (8 === b) {
            b = a.data;
            if ("$" === b || "$!" === b || "$?" === b) break;
            if ("/$" === b) return null;
          }
        }
        return a;
      }
      function Mf(a) {
        a = a.previousSibling;
        for (var b = 0; a; ) {
          if (8 === a.nodeType) {
            var c = a.data;
            if ("$" === c || "$!" === c || "$?" === c) {
              if (0 === b) return a;
              b--;
            } else "/$" === c && b++;
          }
          a = a.previousSibling;
        }
        return null;
      }
      var Nf = Math.random().toString(36).slice(2);
      var Of = "__reactFiber$" + Nf;
      var Pf = "__reactProps$" + Nf;
      var uf = "__reactContainer$" + Nf;
      var of = "__reactEvents$" + Nf;
      var Qf = "__reactListeners$" + Nf;
      var Rf = "__reactHandles$" + Nf;
      function Wc(a) {
        var b = a[Of];
        if (b) return b;
        for (var c = a.parentNode; c; ) {
          if (b = c[uf] || c[Of]) {
            c = b.alternate;
            if (null !== b.child || null !== c && null !== c.child) for (a = Mf(a); null !== a; ) {
              if (c = a[Of]) return c;
              a = Mf(a);
            }
            return b;
          }
          a = c;
          c = a.parentNode;
        }
        return null;
      }
      function Cb(a) {
        a = a[Of] || a[uf];
        return !a || 5 !== a.tag && 6 !== a.tag && 13 !== a.tag && 3 !== a.tag ? null : a;
      }
      function ue(a) {
        if (5 === a.tag || 6 === a.tag) return a.stateNode;
        throw Error(p(33));
      }
      function Db(a) {
        return a[Pf] || null;
      }
      var Sf = [];
      var Tf = -1;
      function Uf(a) {
        return { current: a };
      }
      function E(a) {
        0 > Tf || (a.current = Sf[Tf], Sf[Tf] = null, Tf--);
      }
      function G(a, b) {
        Tf++;
        Sf[Tf] = a.current;
        a.current = b;
      }
      var Vf = {};
      var H = Uf(Vf);
      var Wf = Uf(false);
      var Xf = Vf;
      function Yf(a, b) {
        var c = a.type.contextTypes;
        if (!c) return Vf;
        var d = a.stateNode;
        if (d && d.__reactInternalMemoizedUnmaskedChildContext === b) return d.__reactInternalMemoizedMaskedChildContext;
        var e = {}, f;
        for (f in c) e[f] = b[f];
        d && (a = a.stateNode, a.__reactInternalMemoizedUnmaskedChildContext = b, a.__reactInternalMemoizedMaskedChildContext = e);
        return e;
      }
      function Zf(a) {
        a = a.childContextTypes;
        return null !== a && void 0 !== a;
      }
      function $f() {
        E(Wf);
        E(H);
      }
      function ag(a, b, c) {
        if (H.current !== Vf) throw Error(p(168));
        G(H, b);
        G(Wf, c);
      }
      function bg(a, b, c) {
        var d = a.stateNode;
        b = b.childContextTypes;
        if ("function" !== typeof d.getChildContext) return c;
        d = d.getChildContext();
        for (var e in d) if (!(e in b)) throw Error(p(108, Ra(a) || "Unknown", e));
        return A({}, c, d);
      }
      function cg(a) {
        a = (a = a.stateNode) && a.__reactInternalMemoizedMergedChildContext || Vf;
        Xf = H.current;
        G(H, a);
        G(Wf, Wf.current);
        return true;
      }
      function dg(a, b, c) {
        var d = a.stateNode;
        if (!d) throw Error(p(169));
        c ? (a = bg(a, b, Xf), d.__reactInternalMemoizedMergedChildContext = a, E(Wf), E(H), G(H, a)) : E(Wf);
        G(Wf, c);
      }
      var eg = null;
      var fg = false;
      var gg = false;
      function hg(a) {
        null === eg ? eg = [a] : eg.push(a);
      }
      function ig(a) {
        fg = true;
        hg(a);
      }
      function jg() {
        if (!gg && null !== eg) {
          gg = true;
          var a = 0, b = C;
          try {
            var c = eg;
            for (C = 1; a < c.length; a++) {
              var d = c[a];
              do
                d = d(true);
              while (null !== d);
            }
            eg = null;
            fg = false;
          } catch (e) {
            throw null !== eg && (eg = eg.slice(a + 1)), ac(fc, jg), e;
          } finally {
            C = b, gg = false;
          }
        }
        return null;
      }
      var kg = [];
      var lg = 0;
      var mg = null;
      var ng = 0;
      var og = [];
      var pg = 0;
      var qg = null;
      var rg = 1;
      var sg = "";
      function tg(a, b) {
        kg[lg++] = ng;
        kg[lg++] = mg;
        mg = a;
        ng = b;
      }
      function ug(a, b, c) {
        og[pg++] = rg;
        og[pg++] = sg;
        og[pg++] = qg;
        qg = a;
        var d = rg;
        a = sg;
        var e = 32 - oc(d) - 1;
        d &= ~(1 << e);
        c += 1;
        var f = 32 - oc(b) + e;
        if (30 < f) {
          var g = e - e % 5;
          f = (d & (1 << g) - 1).toString(32);
          d >>= g;
          e -= g;
          rg = 1 << 32 - oc(b) + e | c << e | d;
          sg = f + a;
        } else rg = 1 << f | c << e | d, sg = a;
      }
      function vg(a) {
        null !== a.return && (tg(a, 1), ug(a, 1, 0));
      }
      function wg(a) {
        for (; a === mg; ) mg = kg[--lg], kg[lg] = null, ng = kg[--lg], kg[lg] = null;
        for (; a === qg; ) qg = og[--pg], og[pg] = null, sg = og[--pg], og[pg] = null, rg = og[--pg], og[pg] = null;
      }
      var xg = null;
      var yg = null;
      var I = false;
      var zg = null;
      function Ag(a, b) {
        var c = Bg(5, null, null, 0);
        c.elementType = "DELETED";
        c.stateNode = b;
        c.return = a;
        b = a.deletions;
        null === b ? (a.deletions = [c], a.flags |= 16) : b.push(c);
      }
      function Cg(a, b) {
        switch (a.tag) {
          case 5:
            var c = a.type;
            b = 1 !== b.nodeType || c.toLowerCase() !== b.nodeName.toLowerCase() ? null : b;
            return null !== b ? (a.stateNode = b, xg = a, yg = Lf(b.firstChild), true) : false;
          case 6:
            return b = "" === a.pendingProps || 3 !== b.nodeType ? null : b, null !== b ? (a.stateNode = b, xg = a, yg = null, true) : false;
          case 13:
            return b = 8 !== b.nodeType ? null : b, null !== b ? (c = null !== qg ? { id: rg, overflow: sg } : null, a.memoizedState = { dehydrated: b, treeContext: c, retryLane: 1073741824 }, c = Bg(18, null, null, 0), c.stateNode = b, c.return = a, a.child = c, xg = a, yg = null, true) : false;
          default:
            return false;
        }
      }
      function Dg(a) {
        return 0 !== (a.mode & 1) && 0 === (a.flags & 128);
      }
      function Eg(a) {
        if (I) {
          var b = yg;
          if (b) {
            var c = b;
            if (!Cg(a, b)) {
              if (Dg(a)) throw Error(p(418));
              b = Lf(c.nextSibling);
              var d = xg;
              b && Cg(a, b) ? Ag(d, c) : (a.flags = a.flags & -4097 | 2, I = false, xg = a);
            }
          } else {
            if (Dg(a)) throw Error(p(418));
            a.flags = a.flags & -4097 | 2;
            I = false;
            xg = a;
          }
        }
      }
      function Fg(a) {
        for (a = a.return; null !== a && 5 !== a.tag && 3 !== a.tag && 13 !== a.tag; ) a = a.return;
        xg = a;
      }
      function Gg(a) {
        if (a !== xg) return false;
        if (!I) return Fg(a), I = true, false;
        var b;
        (b = 3 !== a.tag) && !(b = 5 !== a.tag) && (b = a.type, b = "head" !== b && "body" !== b && !Ef(a.type, a.memoizedProps));
        if (b && (b = yg)) {
          if (Dg(a)) throw Hg(), Error(p(418));
          for (; b; ) Ag(a, b), b = Lf(b.nextSibling);
        }
        Fg(a);
        if (13 === a.tag) {
          a = a.memoizedState;
          a = null !== a ? a.dehydrated : null;
          if (!a) throw Error(p(317));
          a: {
            a = a.nextSibling;
            for (b = 0; a; ) {
              if (8 === a.nodeType) {
                var c = a.data;
                if ("/$" === c) {
                  if (0 === b) {
                    yg = Lf(a.nextSibling);
                    break a;
                  }
                  b--;
                } else "$" !== c && "$!" !== c && "$?" !== c || b++;
              }
              a = a.nextSibling;
            }
            yg = null;
          }
        } else yg = xg ? Lf(a.stateNode.nextSibling) : null;
        return true;
      }
      function Hg() {
        for (var a = yg; a; ) a = Lf(a.nextSibling);
      }
      function Ig() {
        yg = xg = null;
        I = false;
      }
      function Jg(a) {
        null === zg ? zg = [a] : zg.push(a);
      }
      var Kg = ua.ReactCurrentBatchConfig;
      function Lg(a, b, c) {
        a = c.ref;
        if (null !== a && "function" !== typeof a && "object" !== typeof a) {
          if (c._owner) {
            c = c._owner;
            if (c) {
              if (1 !== c.tag) throw Error(p(309));
              var d = c.stateNode;
            }
            if (!d) throw Error(p(147, a));
            var e = d, f = "" + a;
            if (null !== b && null !== b.ref && "function" === typeof b.ref && b.ref._stringRef === f) return b.ref;
            b = function(a2) {
              var b2 = e.refs;
              null === a2 ? delete b2[f] : b2[f] = a2;
            };
            b._stringRef = f;
            return b;
          }
          if ("string" !== typeof a) throw Error(p(284));
          if (!c._owner) throw Error(p(290, a));
        }
        return a;
      }
      function Mg(a, b) {
        a = Object.prototype.toString.call(b);
        throw Error(p(31, "[object Object]" === a ? "object with keys {" + Object.keys(b).join(", ") + "}" : a));
      }
      function Ng(a) {
        var b = a._init;
        return b(a._payload);
      }
      function Og(a) {
        function b(b2, c2) {
          if (a) {
            var d2 = b2.deletions;
            null === d2 ? (b2.deletions = [c2], b2.flags |= 16) : d2.push(c2);
          }
        }
        function c(c2, d2) {
          if (!a) return null;
          for (; null !== d2; ) b(c2, d2), d2 = d2.sibling;
          return null;
        }
        function d(a2, b2) {
          for (a2 = /* @__PURE__ */ new Map(); null !== b2; ) null !== b2.key ? a2.set(b2.key, b2) : a2.set(b2.index, b2), b2 = b2.sibling;
          return a2;
        }
        function e(a2, b2) {
          a2 = Pg(a2, b2);
          a2.index = 0;
          a2.sibling = null;
          return a2;
        }
        function f(b2, c2, d2) {
          b2.index = d2;
          if (!a) return b2.flags |= 1048576, c2;
          d2 = b2.alternate;
          if (null !== d2) return d2 = d2.index, d2 < c2 ? (b2.flags |= 2, c2) : d2;
          b2.flags |= 2;
          return c2;
        }
        function g(b2) {
          a && null === b2.alternate && (b2.flags |= 2);
          return b2;
        }
        function h(a2, b2, c2, d2) {
          if (null === b2 || 6 !== b2.tag) return b2 = Qg(c2, a2.mode, d2), b2.return = a2, b2;
          b2 = e(b2, c2);
          b2.return = a2;
          return b2;
        }
        function k(a2, b2, c2, d2) {
          var f2 = c2.type;
          if (f2 === ya) return m(a2, b2, c2.props.children, d2, c2.key);
          if (null !== b2 && (b2.elementType === f2 || "object" === typeof f2 && null !== f2 && f2.$$typeof === Ha && Ng(f2) === b2.type)) return d2 = e(b2, c2.props), d2.ref = Lg(a2, b2, c2), d2.return = a2, d2;
          d2 = Rg(c2.type, c2.key, c2.props, null, a2.mode, d2);
          d2.ref = Lg(a2, b2, c2);
          d2.return = a2;
          return d2;
        }
        function l(a2, b2, c2, d2) {
          if (null === b2 || 4 !== b2.tag || b2.stateNode.containerInfo !== c2.containerInfo || b2.stateNode.implementation !== c2.implementation) return b2 = Sg(c2, a2.mode, d2), b2.return = a2, b2;
          b2 = e(b2, c2.children || []);
          b2.return = a2;
          return b2;
        }
        function m(a2, b2, c2, d2, f2) {
          if (null === b2 || 7 !== b2.tag) return b2 = Tg(c2, a2.mode, d2, f2), b2.return = a2, b2;
          b2 = e(b2, c2);
          b2.return = a2;
          return b2;
        }
        function q(a2, b2, c2) {
          if ("string" === typeof b2 && "" !== b2 || "number" === typeof b2) return b2 = Qg("" + b2, a2.mode, c2), b2.return = a2, b2;
          if ("object" === typeof b2 && null !== b2) {
            switch (b2.$$typeof) {
              case va:
                return c2 = Rg(b2.type, b2.key, b2.props, null, a2.mode, c2), c2.ref = Lg(a2, null, b2), c2.return = a2, c2;
              case wa:
                return b2 = Sg(b2, a2.mode, c2), b2.return = a2, b2;
              case Ha:
                var d2 = b2._init;
                return q(a2, d2(b2._payload), c2);
            }
            if (eb(b2) || Ka(b2)) return b2 = Tg(b2, a2.mode, c2, null), b2.return = a2, b2;
            Mg(a2, b2);
          }
          return null;
        }
        function r(a2, b2, c2, d2) {
          var e2 = null !== b2 ? b2.key : null;
          if ("string" === typeof c2 && "" !== c2 || "number" === typeof c2) return null !== e2 ? null : h(a2, b2, "" + c2, d2);
          if ("object" === typeof c2 && null !== c2) {
            switch (c2.$$typeof) {
              case va:
                return c2.key === e2 ? k(a2, b2, c2, d2) : null;
              case wa:
                return c2.key === e2 ? l(a2, b2, c2, d2) : null;
              case Ha:
                return e2 = c2._init, r(
                  a2,
                  b2,
                  e2(c2._payload),
                  d2
                );
            }
            if (eb(c2) || Ka(c2)) return null !== e2 ? null : m(a2, b2, c2, d2, null);
            Mg(a2, c2);
          }
          return null;
        }
        function y(a2, b2, c2, d2, e2) {
          if ("string" === typeof d2 && "" !== d2 || "number" === typeof d2) return a2 = a2.get(c2) || null, h(b2, a2, "" + d2, e2);
          if ("object" === typeof d2 && null !== d2) {
            switch (d2.$$typeof) {
              case va:
                return a2 = a2.get(null === d2.key ? c2 : d2.key) || null, k(b2, a2, d2, e2);
              case wa:
                return a2 = a2.get(null === d2.key ? c2 : d2.key) || null, l(b2, a2, d2, e2);
              case Ha:
                var f2 = d2._init;
                return y(a2, b2, c2, f2(d2._payload), e2);
            }
            if (eb(d2) || Ka(d2)) return a2 = a2.get(c2) || null, m(b2, a2, d2, e2, null);
            Mg(b2, d2);
          }
          return null;
        }
        function n(e2, g2, h2, k2) {
          for (var l2 = null, m2 = null, u = g2, w = g2 = 0, x = null; null !== u && w < h2.length; w++) {
            u.index > w ? (x = u, u = null) : x = u.sibling;
            var n2 = r(e2, u, h2[w], k2);
            if (null === n2) {
              null === u && (u = x);
              break;
            }
            a && u && null === n2.alternate && b(e2, u);
            g2 = f(n2, g2, w);
            null === m2 ? l2 = n2 : m2.sibling = n2;
            m2 = n2;
            u = x;
          }
          if (w === h2.length) return c(e2, u), I && tg(e2, w), l2;
          if (null === u) {
            for (; w < h2.length; w++) u = q(e2, h2[w], k2), null !== u && (g2 = f(u, g2, w), null === m2 ? l2 = u : m2.sibling = u, m2 = u);
            I && tg(e2, w);
            return l2;
          }
          for (u = d(e2, u); w < h2.length; w++) x = y(u, e2, w, h2[w], k2), null !== x && (a && null !== x.alternate && u.delete(null === x.key ? w : x.key), g2 = f(x, g2, w), null === m2 ? l2 = x : m2.sibling = x, m2 = x);
          a && u.forEach(function(a2) {
            return b(e2, a2);
          });
          I && tg(e2, w);
          return l2;
        }
        function t(e2, g2, h2, k2) {
          var l2 = Ka(h2);
          if ("function" !== typeof l2) throw Error(p(150));
          h2 = l2.call(h2);
          if (null == h2) throw Error(p(151));
          for (var u = l2 = null, m2 = g2, w = g2 = 0, x = null, n2 = h2.next(); null !== m2 && !n2.done; w++, n2 = h2.next()) {
            m2.index > w ? (x = m2, m2 = null) : x = m2.sibling;
            var t2 = r(e2, m2, n2.value, k2);
            if (null === t2) {
              null === m2 && (m2 = x);
              break;
            }
            a && m2 && null === t2.alternate && b(e2, m2);
            g2 = f(t2, g2, w);
            null === u ? l2 = t2 : u.sibling = t2;
            u = t2;
            m2 = x;
          }
          if (n2.done) return c(
            e2,
            m2
          ), I && tg(e2, w), l2;
          if (null === m2) {
            for (; !n2.done; w++, n2 = h2.next()) n2 = q(e2, n2.value, k2), null !== n2 && (g2 = f(n2, g2, w), null === u ? l2 = n2 : u.sibling = n2, u = n2);
            I && tg(e2, w);
            return l2;
          }
          for (m2 = d(e2, m2); !n2.done; w++, n2 = h2.next()) n2 = y(m2, e2, w, n2.value, k2), null !== n2 && (a && null !== n2.alternate && m2.delete(null === n2.key ? w : n2.key), g2 = f(n2, g2, w), null === u ? l2 = n2 : u.sibling = n2, u = n2);
          a && m2.forEach(function(a2) {
            return b(e2, a2);
          });
          I && tg(e2, w);
          return l2;
        }
        function J(a2, d2, f2, h2) {
          "object" === typeof f2 && null !== f2 && f2.type === ya && null === f2.key && (f2 = f2.props.children);
          if ("object" === typeof f2 && null !== f2) {
            switch (f2.$$typeof) {
              case va:
                a: {
                  for (var k2 = f2.key, l2 = d2; null !== l2; ) {
                    if (l2.key === k2) {
                      k2 = f2.type;
                      if (k2 === ya) {
                        if (7 === l2.tag) {
                          c(a2, l2.sibling);
                          d2 = e(l2, f2.props.children);
                          d2.return = a2;
                          a2 = d2;
                          break a;
                        }
                      } else if (l2.elementType === k2 || "object" === typeof k2 && null !== k2 && k2.$$typeof === Ha && Ng(k2) === l2.type) {
                        c(a2, l2.sibling);
                        d2 = e(l2, f2.props);
                        d2.ref = Lg(a2, l2, f2);
                        d2.return = a2;
                        a2 = d2;
                        break a;
                      }
                      c(a2, l2);
                      break;
                    } else b(a2, l2);
                    l2 = l2.sibling;
                  }
                  f2.type === ya ? (d2 = Tg(f2.props.children, a2.mode, h2, f2.key), d2.return = a2, a2 = d2) : (h2 = Rg(f2.type, f2.key, f2.props, null, a2.mode, h2), h2.ref = Lg(a2, d2, f2), h2.return = a2, a2 = h2);
                }
                return g(a2);
              case wa:
                a: {
                  for (l2 = f2.key; null !== d2; ) {
                    if (d2.key === l2) if (4 === d2.tag && d2.stateNode.containerInfo === f2.containerInfo && d2.stateNode.implementation === f2.implementation) {
                      c(a2, d2.sibling);
                      d2 = e(d2, f2.children || []);
                      d2.return = a2;
                      a2 = d2;
                      break a;
                    } else {
                      c(a2, d2);
                      break;
                    }
                    else b(a2, d2);
                    d2 = d2.sibling;
                  }
                  d2 = Sg(f2, a2.mode, h2);
                  d2.return = a2;
                  a2 = d2;
                }
                return g(a2);
              case Ha:
                return l2 = f2._init, J(a2, d2, l2(f2._payload), h2);
            }
            if (eb(f2)) return n(a2, d2, f2, h2);
            if (Ka(f2)) return t(a2, d2, f2, h2);
            Mg(a2, f2);
          }
          return "string" === typeof f2 && "" !== f2 || "number" === typeof f2 ? (f2 = "" + f2, null !== d2 && 6 === d2.tag ? (c(a2, d2.sibling), d2 = e(d2, f2), d2.return = a2, a2 = d2) : (c(a2, d2), d2 = Qg(f2, a2.mode, h2), d2.return = a2, a2 = d2), g(a2)) : c(a2, d2);
        }
        return J;
      }
      var Ug = Og(true);
      var Vg = Og(false);
      var Wg = Uf(null);
      var Xg = null;
      var Yg = null;
      var Zg = null;
      function $g() {
        Zg = Yg = Xg = null;
      }
      function ah(a) {
        var b = Wg.current;
        E(Wg);
        a._currentValue = b;
      }
      function bh(a, b, c) {
        for (; null !== a; ) {
          var d = a.alternate;
          (a.childLanes & b) !== b ? (a.childLanes |= b, null !== d && (d.childLanes |= b)) : null !== d && (d.childLanes & b) !== b && (d.childLanes |= b);
          if (a === c) break;
          a = a.return;
        }
      }
      function ch(a, b) {
        Xg = a;
        Zg = Yg = null;
        a = a.dependencies;
        null !== a && null !== a.firstContext && (0 !== (a.lanes & b) && (dh = true), a.firstContext = null);
      }
      function eh(a) {
        var b = a._currentValue;
        if (Zg !== a) if (a = { context: a, memoizedValue: b, next: null }, null === Yg) {
          if (null === Xg) throw Error(p(308));
          Yg = a;
          Xg.dependencies = { lanes: 0, firstContext: a };
        } else Yg = Yg.next = a;
        return b;
      }
      var fh = null;
      function gh(a) {
        null === fh ? fh = [a] : fh.push(a);
      }
      function hh(a, b, c, d) {
        var e = b.interleaved;
        null === e ? (c.next = c, gh(b)) : (c.next = e.next, e.next = c);
        b.interleaved = c;
        return ih(a, d);
      }
      function ih(a, b) {
        a.lanes |= b;
        var c = a.alternate;
        null !== c && (c.lanes |= b);
        c = a;
        for (a = a.return; null !== a; ) a.childLanes |= b, c = a.alternate, null !== c && (c.childLanes |= b), c = a, a = a.return;
        return 3 === c.tag ? c.stateNode : null;
      }
      var jh = false;
      function kh(a) {
        a.updateQueue = { baseState: a.memoizedState, firstBaseUpdate: null, lastBaseUpdate: null, shared: { pending: null, interleaved: null, lanes: 0 }, effects: null };
      }
      function lh(a, b) {
        a = a.updateQueue;
        b.updateQueue === a && (b.updateQueue = { baseState: a.baseState, firstBaseUpdate: a.firstBaseUpdate, lastBaseUpdate: a.lastBaseUpdate, shared: a.shared, effects: a.effects });
      }
      function mh(a, b) {
        return { eventTime: a, lane: b, tag: 0, payload: null, callback: null, next: null };
      }
      function nh(a, b, c) {
        var d = a.updateQueue;
        if (null === d) return null;
        d = d.shared;
        if (0 !== (K & 2)) {
          var e = d.pending;
          null === e ? b.next = b : (b.next = e.next, e.next = b);
          d.pending = b;
          return ih(a, c);
        }
        e = d.interleaved;
        null === e ? (b.next = b, gh(d)) : (b.next = e.next, e.next = b);
        d.interleaved = b;
        return ih(a, c);
      }
      function oh(a, b, c) {
        b = b.updateQueue;
        if (null !== b && (b = b.shared, 0 !== (c & 4194240))) {
          var d = b.lanes;
          d &= a.pendingLanes;
          c |= d;
          b.lanes = c;
          Cc(a, c);
        }
      }
      function ph(a, b) {
        var c = a.updateQueue, d = a.alternate;
        if (null !== d && (d = d.updateQueue, c === d)) {
          var e = null, f = null;
          c = c.firstBaseUpdate;
          if (null !== c) {
            do {
              var g = { eventTime: c.eventTime, lane: c.lane, tag: c.tag, payload: c.payload, callback: c.callback, next: null };
              null === f ? e = f = g : f = f.next = g;
              c = c.next;
            } while (null !== c);
            null === f ? e = f = b : f = f.next = b;
          } else e = f = b;
          c = { baseState: d.baseState, firstBaseUpdate: e, lastBaseUpdate: f, shared: d.shared, effects: d.effects };
          a.updateQueue = c;
          return;
        }
        a = c.lastBaseUpdate;
        null === a ? c.firstBaseUpdate = b : a.next = b;
        c.lastBaseUpdate = b;
      }
      function qh(a, b, c, d) {
        var e = a.updateQueue;
        jh = false;
        var f = e.firstBaseUpdate, g = e.lastBaseUpdate, h = e.shared.pending;
        if (null !== h) {
          e.shared.pending = null;
          var k = h, l = k.next;
          k.next = null;
          null === g ? f = l : g.next = l;
          g = k;
          var m = a.alternate;
          null !== m && (m = m.updateQueue, h = m.lastBaseUpdate, h !== g && (null === h ? m.firstBaseUpdate = l : h.next = l, m.lastBaseUpdate = k));
        }
        if (null !== f) {
          var q = e.baseState;
          g = 0;
          m = l = k = null;
          h = f;
          do {
            var r = h.lane, y = h.eventTime;
            if ((d & r) === r) {
              null !== m && (m = m.next = {
                eventTime: y,
                lane: 0,
                tag: h.tag,
                payload: h.payload,
                callback: h.callback,
                next: null
              });
              a: {
                var n = a, t = h;
                r = b;
                y = c;
                switch (t.tag) {
                  case 1:
                    n = t.payload;
                    if ("function" === typeof n) {
                      q = n.call(y, q, r);
                      break a;
                    }
                    q = n;
                    break a;
                  case 3:
                    n.flags = n.flags & -65537 | 128;
                  case 0:
                    n = t.payload;
                    r = "function" === typeof n ? n.call(y, q, r) : n;
                    if (null === r || void 0 === r) break a;
                    q = A({}, q, r);
                    break a;
                  case 2:
                    jh = true;
                }
              }
              null !== h.callback && 0 !== h.lane && (a.flags |= 64, r = e.effects, null === r ? e.effects = [h] : r.push(h));
            } else y = { eventTime: y, lane: r, tag: h.tag, payload: h.payload, callback: h.callback, next: null }, null === m ? (l = m = y, k = q) : m = m.next = y, g |= r;
            h = h.next;
            if (null === h) if (h = e.shared.pending, null === h) break;
            else r = h, h = r.next, r.next = null, e.lastBaseUpdate = r, e.shared.pending = null;
          } while (1);
          null === m && (k = q);
          e.baseState = k;
          e.firstBaseUpdate = l;
          e.lastBaseUpdate = m;
          b = e.shared.interleaved;
          if (null !== b) {
            e = b;
            do
              g |= e.lane, e = e.next;
            while (e !== b);
          } else null === f && (e.shared.lanes = 0);
          rh |= g;
          a.lanes = g;
          a.memoizedState = q;
        }
      }
      function sh(a, b, c) {
        a = b.effects;
        b.effects = null;
        if (null !== a) for (b = 0; b < a.length; b++) {
          var d = a[b], e = d.callback;
          if (null !== e) {
            d.callback = null;
            d = c;
            if ("function" !== typeof e) throw Error(p(191, e));
            e.call(d);
          }
        }
      }
      var th = {};
      var uh = Uf(th);
      var vh = Uf(th);
      var wh = Uf(th);
      function xh(a) {
        if (a === th) throw Error(p(174));
        return a;
      }
      function yh(a, b) {
        G(wh, b);
        G(vh, a);
        G(uh, th);
        a = b.nodeType;
        switch (a) {
          case 9:
          case 11:
            b = (b = b.documentElement) ? b.namespaceURI : lb(null, "");
            break;
          default:
            a = 8 === a ? b.parentNode : b, b = a.namespaceURI || null, a = a.tagName, b = lb(b, a);
        }
        E(uh);
        G(uh, b);
      }
      function zh() {
        E(uh);
        E(vh);
        E(wh);
      }
      function Ah(a) {
        xh(wh.current);
        var b = xh(uh.current);
        var c = lb(b, a.type);
        b !== c && (G(vh, a), G(uh, c));
      }
      function Bh(a) {
        vh.current === a && (E(uh), E(vh));
      }
      var L = Uf(0);
      function Ch(a) {
        for (var b = a; null !== b; ) {
          if (13 === b.tag) {
            var c = b.memoizedState;
            if (null !== c && (c = c.dehydrated, null === c || "$?" === c.data || "$!" === c.data)) return b;
          } else if (19 === b.tag && void 0 !== b.memoizedProps.revealOrder) {
            if (0 !== (b.flags & 128)) return b;
          } else if (null !== b.child) {
            b.child.return = b;
            b = b.child;
            continue;
          }
          if (b === a) break;
          for (; null === b.sibling; ) {
            if (null === b.return || b.return === a) return null;
            b = b.return;
          }
          b.sibling.return = b.return;
          b = b.sibling;
        }
        return null;
      }
      var Dh = [];
      function Eh() {
        for (var a = 0; a < Dh.length; a++) Dh[a]._workInProgressVersionPrimary = null;
        Dh.length = 0;
      }
      var Fh = ua.ReactCurrentDispatcher;
      var Gh = ua.ReactCurrentBatchConfig;
      var Hh = 0;
      var M = null;
      var N = null;
      var O = null;
      var Ih = false;
      var Jh = false;
      var Kh = 0;
      var Lh = 0;
      function P() {
        throw Error(p(321));
      }
      function Mh(a, b) {
        if (null === b) return false;
        for (var c = 0; c < b.length && c < a.length; c++) if (!He(a[c], b[c])) return false;
        return true;
      }
      function Nh(a, b, c, d, e, f) {
        Hh = f;
        M = b;
        b.memoizedState = null;
        b.updateQueue = null;
        b.lanes = 0;
        Fh.current = null === a || null === a.memoizedState ? Oh : Ph;
        a = c(d, e);
        if (Jh) {
          f = 0;
          do {
            Jh = false;
            Kh = 0;
            if (25 <= f) throw Error(p(301));
            f += 1;
            O = N = null;
            b.updateQueue = null;
            Fh.current = Qh;
            a = c(d, e);
          } while (Jh);
        }
        Fh.current = Rh;
        b = null !== N && null !== N.next;
        Hh = 0;
        O = N = M = null;
        Ih = false;
        if (b) throw Error(p(300));
        return a;
      }
      function Sh() {
        var a = 0 !== Kh;
        Kh = 0;
        return a;
      }
      function Th() {
        var a = { memoizedState: null, baseState: null, baseQueue: null, queue: null, next: null };
        null === O ? M.memoizedState = O = a : O = O.next = a;
        return O;
      }
      function Uh() {
        if (null === N) {
          var a = M.alternate;
          a = null !== a ? a.memoizedState : null;
        } else a = N.next;
        var b = null === O ? M.memoizedState : O.next;
        if (null !== b) O = b, N = a;
        else {
          if (null === a) throw Error(p(310));
          N = a;
          a = { memoizedState: N.memoizedState, baseState: N.baseState, baseQueue: N.baseQueue, queue: N.queue, next: null };
          null === O ? M.memoizedState = O = a : O = O.next = a;
        }
        return O;
      }
      function Vh(a, b) {
        return "function" === typeof b ? b(a) : b;
      }
      function Wh(a) {
        var b = Uh(), c = b.queue;
        if (null === c) throw Error(p(311));
        c.lastRenderedReducer = a;
        var d = N, e = d.baseQueue, f = c.pending;
        if (null !== f) {
          if (null !== e) {
            var g = e.next;
            e.next = f.next;
            f.next = g;
          }
          d.baseQueue = e = f;
          c.pending = null;
        }
        if (null !== e) {
          f = e.next;
          d = d.baseState;
          var h = g = null, k = null, l = f;
          do {
            var m = l.lane;
            if ((Hh & m) === m) null !== k && (k = k.next = { lane: 0, action: l.action, hasEagerState: l.hasEagerState, eagerState: l.eagerState, next: null }), d = l.hasEagerState ? l.eagerState : a(d, l.action);
            else {
              var q = {
                lane: m,
                action: l.action,
                hasEagerState: l.hasEagerState,
                eagerState: l.eagerState,
                next: null
              };
              null === k ? (h = k = q, g = d) : k = k.next = q;
              M.lanes |= m;
              rh |= m;
            }
            l = l.next;
          } while (null !== l && l !== f);
          null === k ? g = d : k.next = h;
          He(d, b.memoizedState) || (dh = true);
          b.memoizedState = d;
          b.baseState = g;
          b.baseQueue = k;
          c.lastRenderedState = d;
        }
        a = c.interleaved;
        if (null !== a) {
          e = a;
          do
            f = e.lane, M.lanes |= f, rh |= f, e = e.next;
          while (e !== a);
        } else null === e && (c.lanes = 0);
        return [b.memoizedState, c.dispatch];
      }
      function Xh(a) {
        var b = Uh(), c = b.queue;
        if (null === c) throw Error(p(311));
        c.lastRenderedReducer = a;
        var d = c.dispatch, e = c.pending, f = b.memoizedState;
        if (null !== e) {
          c.pending = null;
          var g = e = e.next;
          do
            f = a(f, g.action), g = g.next;
          while (g !== e);
          He(f, b.memoizedState) || (dh = true);
          b.memoizedState = f;
          null === b.baseQueue && (b.baseState = f);
          c.lastRenderedState = f;
        }
        return [f, d];
      }
      function Yh() {
      }
      function Zh(a, b) {
        var c = M, d = Uh(), e = b(), f = !He(d.memoizedState, e);
        f && (d.memoizedState = e, dh = true);
        d = d.queue;
        $h(ai.bind(null, c, d, a), [a]);
        if (d.getSnapshot !== b || f || null !== O && O.memoizedState.tag & 1) {
          c.flags |= 2048;
          bi(9, ci.bind(null, c, d, e, b), void 0, null);
          if (null === Q) throw Error(p(349));
          0 !== (Hh & 30) || di(c, b, e);
        }
        return e;
      }
      function di(a, b, c) {
        a.flags |= 16384;
        a = { getSnapshot: b, value: c };
        b = M.updateQueue;
        null === b ? (b = { lastEffect: null, stores: null }, M.updateQueue = b, b.stores = [a]) : (c = b.stores, null === c ? b.stores = [a] : c.push(a));
      }
      function ci(a, b, c, d) {
        b.value = c;
        b.getSnapshot = d;
        ei(b) && fi(a);
      }
      function ai(a, b, c) {
        return c(function() {
          ei(b) && fi(a);
        });
      }
      function ei(a) {
        var b = a.getSnapshot;
        a = a.value;
        try {
          var c = b();
          return !He(a, c);
        } catch (d) {
          return true;
        }
      }
      function fi(a) {
        var b = ih(a, 1);
        null !== b && gi(b, a, 1, -1);
      }
      function hi(a) {
        var b = Th();
        "function" === typeof a && (a = a());
        b.memoizedState = b.baseState = a;
        a = { pending: null, interleaved: null, lanes: 0, dispatch: null, lastRenderedReducer: Vh, lastRenderedState: a };
        b.queue = a;
        a = a.dispatch = ii.bind(null, M, a);
        return [b.memoizedState, a];
      }
      function bi(a, b, c, d) {
        a = { tag: a, create: b, destroy: c, deps: d, next: null };
        b = M.updateQueue;
        null === b ? (b = { lastEffect: null, stores: null }, M.updateQueue = b, b.lastEffect = a.next = a) : (c = b.lastEffect, null === c ? b.lastEffect = a.next = a : (d = c.next, c.next = a, a.next = d, b.lastEffect = a));
        return a;
      }
      function ji() {
        return Uh().memoizedState;
      }
      function ki(a, b, c, d) {
        var e = Th();
        M.flags |= a;
        e.memoizedState = bi(1 | b, c, void 0, void 0 === d ? null : d);
      }
      function li(a, b, c, d) {
        var e = Uh();
        d = void 0 === d ? null : d;
        var f = void 0;
        if (null !== N) {
          var g = N.memoizedState;
          f = g.destroy;
          if (null !== d && Mh(d, g.deps)) {
            e.memoizedState = bi(b, c, f, d);
            return;
          }
        }
        M.flags |= a;
        e.memoizedState = bi(1 | b, c, f, d);
      }
      function mi(a, b) {
        return ki(8390656, 8, a, b);
      }
      function $h(a, b) {
        return li(2048, 8, a, b);
      }
      function ni(a, b) {
        return li(4, 2, a, b);
      }
      function oi(a, b) {
        return li(4, 4, a, b);
      }
      function pi(a, b) {
        if ("function" === typeof b) return a = a(), b(a), function() {
          b(null);
        };
        if (null !== b && void 0 !== b) return a = a(), b.current = a, function() {
          b.current = null;
        };
      }
      function qi(a, b, c) {
        c = null !== c && void 0 !== c ? c.concat([a]) : null;
        return li(4, 4, pi.bind(null, b, a), c);
      }
      function ri() {
      }
      function si(a, b) {
        var c = Uh();
        b = void 0 === b ? null : b;
        var d = c.memoizedState;
        if (null !== d && null !== b && Mh(b, d[1])) return d[0];
        c.memoizedState = [a, b];
        return a;
      }
      function ti(a, b) {
        var c = Uh();
        b = void 0 === b ? null : b;
        var d = c.memoizedState;
        if (null !== d && null !== b && Mh(b, d[1])) return d[0];
        a = a();
        c.memoizedState = [a, b];
        return a;
      }
      function ui(a, b, c) {
        if (0 === (Hh & 21)) return a.baseState && (a.baseState = false, dh = true), a.memoizedState = c;
        He(c, b) || (c = yc(), M.lanes |= c, rh |= c, a.baseState = true);
        return b;
      }
      function vi(a, b) {
        var c = C;
        C = 0 !== c && 4 > c ? c : 4;
        a(true);
        var d = Gh.transition;
        Gh.transition = {};
        try {
          a(false), b();
        } finally {
          C = c, Gh.transition = d;
        }
      }
      function wi() {
        return Uh().memoizedState;
      }
      function xi(a, b, c) {
        var d = yi(a);
        c = { lane: d, action: c, hasEagerState: false, eagerState: null, next: null };
        if (zi(a)) Ai(b, c);
        else if (c = hh(a, b, c, d), null !== c) {
          var e = R();
          gi(c, a, d, e);
          Bi(c, b, d);
        }
      }
      function ii(a, b, c) {
        var d = yi(a), e = { lane: d, action: c, hasEagerState: false, eagerState: null, next: null };
        if (zi(a)) Ai(b, e);
        else {
          var f = a.alternate;
          if (0 === a.lanes && (null === f || 0 === f.lanes) && (f = b.lastRenderedReducer, null !== f)) try {
            var g = b.lastRenderedState, h = f(g, c);
            e.hasEagerState = true;
            e.eagerState = h;
            if (He(h, g)) {
              var k = b.interleaved;
              null === k ? (e.next = e, gh(b)) : (e.next = k.next, k.next = e);
              b.interleaved = e;
              return;
            }
          } catch (l) {
          } finally {
          }
          c = hh(a, b, e, d);
          null !== c && (e = R(), gi(c, a, d, e), Bi(c, b, d));
        }
      }
      function zi(a) {
        var b = a.alternate;
        return a === M || null !== b && b === M;
      }
      function Ai(a, b) {
        Jh = Ih = true;
        var c = a.pending;
        null === c ? b.next = b : (b.next = c.next, c.next = b);
        a.pending = b;
      }
      function Bi(a, b, c) {
        if (0 !== (c & 4194240)) {
          var d = b.lanes;
          d &= a.pendingLanes;
          c |= d;
          b.lanes = c;
          Cc(a, c);
        }
      }
      var Rh = { readContext: eh, useCallback: P, useContext: P, useEffect: P, useImperativeHandle: P, useInsertionEffect: P, useLayoutEffect: P, useMemo: P, useReducer: P, useRef: P, useState: P, useDebugValue: P, useDeferredValue: P, useTransition: P, useMutableSource: P, useSyncExternalStore: P, useId: P, unstable_isNewReconciler: false };
      var Oh = { readContext: eh, useCallback: function(a, b) {
        Th().memoizedState = [a, void 0 === b ? null : b];
        return a;
      }, useContext: eh, useEffect: mi, useImperativeHandle: function(a, b, c) {
        c = null !== c && void 0 !== c ? c.concat([a]) : null;
        return ki(
          4194308,
          4,
          pi.bind(null, b, a),
          c
        );
      }, useLayoutEffect: function(a, b) {
        return ki(4194308, 4, a, b);
      }, useInsertionEffect: function(a, b) {
        return ki(4, 2, a, b);
      }, useMemo: function(a, b) {
        var c = Th();
        b = void 0 === b ? null : b;
        a = a();
        c.memoizedState = [a, b];
        return a;
      }, useReducer: function(a, b, c) {
        var d = Th();
        b = void 0 !== c ? c(b) : b;
        d.memoizedState = d.baseState = b;
        a = { pending: null, interleaved: null, lanes: 0, dispatch: null, lastRenderedReducer: a, lastRenderedState: b };
        d.queue = a;
        a = a.dispatch = xi.bind(null, M, a);
        return [d.memoizedState, a];
      }, useRef: function(a) {
        var b = Th();
        a = { current: a };
        return b.memoizedState = a;
      }, useState: hi, useDebugValue: ri, useDeferredValue: function(a) {
        return Th().memoizedState = a;
      }, useTransition: function() {
        var a = hi(false), b = a[0];
        a = vi.bind(null, a[1]);
        Th().memoizedState = a;
        return [b, a];
      }, useMutableSource: function() {
      }, useSyncExternalStore: function(a, b, c) {
        var d = M, e = Th();
        if (I) {
          if (void 0 === c) throw Error(p(407));
          c = c();
        } else {
          c = b();
          if (null === Q) throw Error(p(349));
          0 !== (Hh & 30) || di(d, b, c);
        }
        e.memoizedState = c;
        var f = { value: c, getSnapshot: b };
        e.queue = f;
        mi(ai.bind(
          null,
          d,
          f,
          a
        ), [a]);
        d.flags |= 2048;
        bi(9, ci.bind(null, d, f, c, b), void 0, null);
        return c;
      }, useId: function() {
        var a = Th(), b = Q.identifierPrefix;
        if (I) {
          var c = sg;
          var d = rg;
          c = (d & ~(1 << 32 - oc(d) - 1)).toString(32) + c;
          b = ":" + b + "R" + c;
          c = Kh++;
          0 < c && (b += "H" + c.toString(32));
          b += ":";
        } else c = Lh++, b = ":" + b + "r" + c.toString(32) + ":";
        return a.memoizedState = b;
      }, unstable_isNewReconciler: false };
      var Ph = {
        readContext: eh,
        useCallback: si,
        useContext: eh,
        useEffect: $h,
        useImperativeHandle: qi,
        useInsertionEffect: ni,
        useLayoutEffect: oi,
        useMemo: ti,
        useReducer: Wh,
        useRef: ji,
        useState: function() {
          return Wh(Vh);
        },
        useDebugValue: ri,
        useDeferredValue: function(a) {
          var b = Uh();
          return ui(b, N.memoizedState, a);
        },
        useTransition: function() {
          var a = Wh(Vh)[0], b = Uh().memoizedState;
          return [a, b];
        },
        useMutableSource: Yh,
        useSyncExternalStore: Zh,
        useId: wi,
        unstable_isNewReconciler: false
      };
      var Qh = { readContext: eh, useCallback: si, useContext: eh, useEffect: $h, useImperativeHandle: qi, useInsertionEffect: ni, useLayoutEffect: oi, useMemo: ti, useReducer: Xh, useRef: ji, useState: function() {
        return Xh(Vh);
      }, useDebugValue: ri, useDeferredValue: function(a) {
        var b = Uh();
        return null === N ? b.memoizedState = a : ui(b, N.memoizedState, a);
      }, useTransition: function() {
        var a = Xh(Vh)[0], b = Uh().memoizedState;
        return [a, b];
      }, useMutableSource: Yh, useSyncExternalStore: Zh, useId: wi, unstable_isNewReconciler: false };
      function Ci(a, b) {
        if (a && a.defaultProps) {
          b = A({}, b);
          a = a.defaultProps;
          for (var c in a) void 0 === b[c] && (b[c] = a[c]);
          return b;
        }
        return b;
      }
      function Di(a, b, c, d) {
        b = a.memoizedState;
        c = c(d, b);
        c = null === c || void 0 === c ? b : A({}, b, c);
        a.memoizedState = c;
        0 === a.lanes && (a.updateQueue.baseState = c);
      }
      var Ei = { isMounted: function(a) {
        return (a = a._reactInternals) ? Vb(a) === a : false;
      }, enqueueSetState: function(a, b, c) {
        a = a._reactInternals;
        var d = R(), e = yi(a), f = mh(d, e);
        f.payload = b;
        void 0 !== c && null !== c && (f.callback = c);
        b = nh(a, f, e);
        null !== b && (gi(b, a, e, d), oh(b, a, e));
      }, enqueueReplaceState: function(a, b, c) {
        a = a._reactInternals;
        var d = R(), e = yi(a), f = mh(d, e);
        f.tag = 1;
        f.payload = b;
        void 0 !== c && null !== c && (f.callback = c);
        b = nh(a, f, e);
        null !== b && (gi(b, a, e, d), oh(b, a, e));
      }, enqueueForceUpdate: function(a, b) {
        a = a._reactInternals;
        var c = R(), d = yi(a), e = mh(c, d);
        e.tag = 2;
        void 0 !== b && null !== b && (e.callback = b);
        b = nh(a, e, d);
        null !== b && (gi(b, a, d, c), oh(b, a, d));
      } };
      function Fi(a, b, c, d, e, f, g) {
        a = a.stateNode;
        return "function" === typeof a.shouldComponentUpdate ? a.shouldComponentUpdate(d, f, g) : b.prototype && b.prototype.isPureReactComponent ? !Ie(c, d) || !Ie(e, f) : true;
      }
      function Gi(a, b, c) {
        var d = false, e = Vf;
        var f = b.contextType;
        "object" === typeof f && null !== f ? f = eh(f) : (e = Zf(b) ? Xf : H.current, d = b.contextTypes, f = (d = null !== d && void 0 !== d) ? Yf(a, e) : Vf);
        b = new b(c, f);
        a.memoizedState = null !== b.state && void 0 !== b.state ? b.state : null;
        b.updater = Ei;
        a.stateNode = b;
        b._reactInternals = a;
        d && (a = a.stateNode, a.__reactInternalMemoizedUnmaskedChildContext = e, a.__reactInternalMemoizedMaskedChildContext = f);
        return b;
      }
      function Hi(a, b, c, d) {
        a = b.state;
        "function" === typeof b.componentWillReceiveProps && b.componentWillReceiveProps(c, d);
        "function" === typeof b.UNSAFE_componentWillReceiveProps && b.UNSAFE_componentWillReceiveProps(c, d);
        b.state !== a && Ei.enqueueReplaceState(b, b.state, null);
      }
      function Ii(a, b, c, d) {
        var e = a.stateNode;
        e.props = c;
        e.state = a.memoizedState;
        e.refs = {};
        kh(a);
        var f = b.contextType;
        "object" === typeof f && null !== f ? e.context = eh(f) : (f = Zf(b) ? Xf : H.current, e.context = Yf(a, f));
        e.state = a.memoizedState;
        f = b.getDerivedStateFromProps;
        "function" === typeof f && (Di(a, b, f, c), e.state = a.memoizedState);
        "function" === typeof b.getDerivedStateFromProps || "function" === typeof e.getSnapshotBeforeUpdate || "function" !== typeof e.UNSAFE_componentWillMount && "function" !== typeof e.componentWillMount || (b = e.state, "function" === typeof e.componentWillMount && e.componentWillMount(), "function" === typeof e.UNSAFE_componentWillMount && e.UNSAFE_componentWillMount(), b !== e.state && Ei.enqueueReplaceState(e, e.state, null), qh(a, c, e, d), e.state = a.memoizedState);
        "function" === typeof e.componentDidMount && (a.flags |= 4194308);
      }
      function Ji(a, b) {
        try {
          var c = "", d = b;
          do
            c += Pa(d), d = d.return;
          while (d);
          var e = c;
        } catch (f) {
          e = "\nError generating stack: " + f.message + "\n" + f.stack;
        }
        return { value: a, source: b, stack: e, digest: null };
      }
      function Ki(a, b, c) {
        return { value: a, source: null, stack: null != c ? c : null, digest: null != b ? b : null };
      }
      function Li(a, b) {
        try {
          console.error(b.value);
        } catch (c) {
          setTimeout(function() {
            throw c;
          });
        }
      }
      var Mi = "function" === typeof WeakMap ? WeakMap : Map;
      function Ni(a, b, c) {
        c = mh(-1, c);
        c.tag = 3;
        c.payload = { element: null };
        var d = b.value;
        c.callback = function() {
          Oi || (Oi = true, Pi = d);
          Li(a, b);
        };
        return c;
      }
      function Qi(a, b, c) {
        c = mh(-1, c);
        c.tag = 3;
        var d = a.type.getDerivedStateFromError;
        if ("function" === typeof d) {
          var e = b.value;
          c.payload = function() {
            return d(e);
          };
          c.callback = function() {
            Li(a, b);
          };
        }
        var f = a.stateNode;
        null !== f && "function" === typeof f.componentDidCatch && (c.callback = function() {
          Li(a, b);
          "function" !== typeof d && (null === Ri ? Ri = /* @__PURE__ */ new Set([this]) : Ri.add(this));
          var c2 = b.stack;
          this.componentDidCatch(b.value, { componentStack: null !== c2 ? c2 : "" });
        });
        return c;
      }
      function Si(a, b, c) {
        var d = a.pingCache;
        if (null === d) {
          d = a.pingCache = new Mi();
          var e = /* @__PURE__ */ new Set();
          d.set(b, e);
        } else e = d.get(b), void 0 === e && (e = /* @__PURE__ */ new Set(), d.set(b, e));
        e.has(c) || (e.add(c), a = Ti.bind(null, a, b, c), b.then(a, a));
      }
      function Ui(a) {
        do {
          var b;
          if (b = 13 === a.tag) b = a.memoizedState, b = null !== b ? null !== b.dehydrated ? true : false : true;
          if (b) return a;
          a = a.return;
        } while (null !== a);
        return null;
      }
      function Vi(a, b, c, d, e) {
        if (0 === (a.mode & 1)) return a === b ? a.flags |= 65536 : (a.flags |= 128, c.flags |= 131072, c.flags &= -52805, 1 === c.tag && (null === c.alternate ? c.tag = 17 : (b = mh(-1, 1), b.tag = 2, nh(c, b, 1))), c.lanes |= 1), a;
        a.flags |= 65536;
        a.lanes = e;
        return a;
      }
      var Wi = ua.ReactCurrentOwner;
      var dh = false;
      function Xi(a, b, c, d) {
        b.child = null === a ? Vg(b, null, c, d) : Ug(b, a.child, c, d);
      }
      function Yi(a, b, c, d, e) {
        c = c.render;
        var f = b.ref;
        ch(b, e);
        d = Nh(a, b, c, d, f, e);
        c = Sh();
        if (null !== a && !dh) return b.updateQueue = a.updateQueue, b.flags &= -2053, a.lanes &= ~e, Zi(a, b, e);
        I && c && vg(b);
        b.flags |= 1;
        Xi(a, b, d, e);
        return b.child;
      }
      function $i(a, b, c, d, e) {
        if (null === a) {
          var f = c.type;
          if ("function" === typeof f && !aj(f) && void 0 === f.defaultProps && null === c.compare && void 0 === c.defaultProps) return b.tag = 15, b.type = f, bj(a, b, f, d, e);
          a = Rg(c.type, null, d, b, b.mode, e);
          a.ref = b.ref;
          a.return = b;
          return b.child = a;
        }
        f = a.child;
        if (0 === (a.lanes & e)) {
          var g = f.memoizedProps;
          c = c.compare;
          c = null !== c ? c : Ie;
          if (c(g, d) && a.ref === b.ref) return Zi(a, b, e);
        }
        b.flags |= 1;
        a = Pg(f, d);
        a.ref = b.ref;
        a.return = b;
        return b.child = a;
      }
      function bj(a, b, c, d, e) {
        if (null !== a) {
          var f = a.memoizedProps;
          if (Ie(f, d) && a.ref === b.ref) if (dh = false, b.pendingProps = d = f, 0 !== (a.lanes & e)) 0 !== (a.flags & 131072) && (dh = true);
          else return b.lanes = a.lanes, Zi(a, b, e);
        }
        return cj(a, b, c, d, e);
      }
      function dj(a, b, c) {
        var d = b.pendingProps, e = d.children, f = null !== a ? a.memoizedState : null;
        if ("hidden" === d.mode) if (0 === (b.mode & 1)) b.memoizedState = { baseLanes: 0, cachePool: null, transitions: null }, G(ej, fj), fj |= c;
        else {
          if (0 === (c & 1073741824)) return a = null !== f ? f.baseLanes | c : c, b.lanes = b.childLanes = 1073741824, b.memoizedState = { baseLanes: a, cachePool: null, transitions: null }, b.updateQueue = null, G(ej, fj), fj |= a, null;
          b.memoizedState = { baseLanes: 0, cachePool: null, transitions: null };
          d = null !== f ? f.baseLanes : c;
          G(ej, fj);
          fj |= d;
        }
        else null !== f ? (d = f.baseLanes | c, b.memoizedState = null) : d = c, G(ej, fj), fj |= d;
        Xi(a, b, e, c);
        return b.child;
      }
      function gj(a, b) {
        var c = b.ref;
        if (null === a && null !== c || null !== a && a.ref !== c) b.flags |= 512, b.flags |= 2097152;
      }
      function cj(a, b, c, d, e) {
        var f = Zf(c) ? Xf : H.current;
        f = Yf(b, f);
        ch(b, e);
        c = Nh(a, b, c, d, f, e);
        d = Sh();
        if (null !== a && !dh) return b.updateQueue = a.updateQueue, b.flags &= -2053, a.lanes &= ~e, Zi(a, b, e);
        I && d && vg(b);
        b.flags |= 1;
        Xi(a, b, c, e);
        return b.child;
      }
      function hj(a, b, c, d, e) {
        if (Zf(c)) {
          var f = true;
          cg(b);
        } else f = false;
        ch(b, e);
        if (null === b.stateNode) ij(a, b), Gi(b, c, d), Ii(b, c, d, e), d = true;
        else if (null === a) {
          var g = b.stateNode, h = b.memoizedProps;
          g.props = h;
          var k = g.context, l = c.contextType;
          "object" === typeof l && null !== l ? l = eh(l) : (l = Zf(c) ? Xf : H.current, l = Yf(b, l));
          var m = c.getDerivedStateFromProps, q = "function" === typeof m || "function" === typeof g.getSnapshotBeforeUpdate;
          q || "function" !== typeof g.UNSAFE_componentWillReceiveProps && "function" !== typeof g.componentWillReceiveProps || (h !== d || k !== l) && Hi(b, g, d, l);
          jh = false;
          var r = b.memoizedState;
          g.state = r;
          qh(b, d, g, e);
          k = b.memoizedState;
          h !== d || r !== k || Wf.current || jh ? ("function" === typeof m && (Di(b, c, m, d), k = b.memoizedState), (h = jh || Fi(b, c, h, d, r, k, l)) ? (q || "function" !== typeof g.UNSAFE_componentWillMount && "function" !== typeof g.componentWillMount || ("function" === typeof g.componentWillMount && g.componentWillMount(), "function" === typeof g.UNSAFE_componentWillMount && g.UNSAFE_componentWillMount()), "function" === typeof g.componentDidMount && (b.flags |= 4194308)) : ("function" === typeof g.componentDidMount && (b.flags |= 4194308), b.memoizedProps = d, b.memoizedState = k), g.props = d, g.state = k, g.context = l, d = h) : ("function" === typeof g.componentDidMount && (b.flags |= 4194308), d = false);
        } else {
          g = b.stateNode;
          lh(a, b);
          h = b.memoizedProps;
          l = b.type === b.elementType ? h : Ci(b.type, h);
          g.props = l;
          q = b.pendingProps;
          r = g.context;
          k = c.contextType;
          "object" === typeof k && null !== k ? k = eh(k) : (k = Zf(c) ? Xf : H.current, k = Yf(b, k));
          var y = c.getDerivedStateFromProps;
          (m = "function" === typeof y || "function" === typeof g.getSnapshotBeforeUpdate) || "function" !== typeof g.UNSAFE_componentWillReceiveProps && "function" !== typeof g.componentWillReceiveProps || (h !== q || r !== k) && Hi(b, g, d, k);
          jh = false;
          r = b.memoizedState;
          g.state = r;
          qh(b, d, g, e);
          var n = b.memoizedState;
          h !== q || r !== n || Wf.current || jh ? ("function" === typeof y && (Di(b, c, y, d), n = b.memoizedState), (l = jh || Fi(b, c, l, d, r, n, k) || false) ? (m || "function" !== typeof g.UNSAFE_componentWillUpdate && "function" !== typeof g.componentWillUpdate || ("function" === typeof g.componentWillUpdate && g.componentWillUpdate(d, n, k), "function" === typeof g.UNSAFE_componentWillUpdate && g.UNSAFE_componentWillUpdate(d, n, k)), "function" === typeof g.componentDidUpdate && (b.flags |= 4), "function" === typeof g.getSnapshotBeforeUpdate && (b.flags |= 1024)) : ("function" !== typeof g.componentDidUpdate || h === a.memoizedProps && r === a.memoizedState || (b.flags |= 4), "function" !== typeof g.getSnapshotBeforeUpdate || h === a.memoizedProps && r === a.memoizedState || (b.flags |= 1024), b.memoizedProps = d, b.memoizedState = n), g.props = d, g.state = n, g.context = k, d = l) : ("function" !== typeof g.componentDidUpdate || h === a.memoizedProps && r === a.memoizedState || (b.flags |= 4), "function" !== typeof g.getSnapshotBeforeUpdate || h === a.memoizedProps && r === a.memoizedState || (b.flags |= 1024), d = false);
        }
        return jj(a, b, c, d, f, e);
      }
      function jj(a, b, c, d, e, f) {
        gj(a, b);
        var g = 0 !== (b.flags & 128);
        if (!d && !g) return e && dg(b, c, false), Zi(a, b, f);
        d = b.stateNode;
        Wi.current = b;
        var h = g && "function" !== typeof c.getDerivedStateFromError ? null : d.render();
        b.flags |= 1;
        null !== a && g ? (b.child = Ug(b, a.child, null, f), b.child = Ug(b, null, h, f)) : Xi(a, b, h, f);
        b.memoizedState = d.state;
        e && dg(b, c, true);
        return b.child;
      }
      function kj(a) {
        var b = a.stateNode;
        b.pendingContext ? ag(a, b.pendingContext, b.pendingContext !== b.context) : b.context && ag(a, b.context, false);
        yh(a, b.containerInfo);
      }
      function lj(a, b, c, d, e) {
        Ig();
        Jg(e);
        b.flags |= 256;
        Xi(a, b, c, d);
        return b.child;
      }
      var mj = { dehydrated: null, treeContext: null, retryLane: 0 };
      function nj(a) {
        return { baseLanes: a, cachePool: null, transitions: null };
      }
      function oj(a, b, c) {
        var d = b.pendingProps, e = L.current, f = false, g = 0 !== (b.flags & 128), h;
        (h = g) || (h = null !== a && null === a.memoizedState ? false : 0 !== (e & 2));
        if (h) f = true, b.flags &= -129;
        else if (null === a || null !== a.memoizedState) e |= 1;
        G(L, e & 1);
        if (null === a) {
          Eg(b);
          a = b.memoizedState;
          if (null !== a && (a = a.dehydrated, null !== a)) return 0 === (b.mode & 1) ? b.lanes = 1 : "$!" === a.data ? b.lanes = 8 : b.lanes = 1073741824, null;
          g = d.children;
          a = d.fallback;
          return f ? (d = b.mode, f = b.child, g = { mode: "hidden", children: g }, 0 === (d & 1) && null !== f ? (f.childLanes = 0, f.pendingProps = g) : f = pj(g, d, 0, null), a = Tg(a, d, c, null), f.return = b, a.return = b, f.sibling = a, b.child = f, b.child.memoizedState = nj(c), b.memoizedState = mj, a) : qj(b, g);
        }
        e = a.memoizedState;
        if (null !== e && (h = e.dehydrated, null !== h)) return rj(a, b, g, d, h, e, c);
        if (f) {
          f = d.fallback;
          g = b.mode;
          e = a.child;
          h = e.sibling;
          var k = { mode: "hidden", children: d.children };
          0 === (g & 1) && b.child !== e ? (d = b.child, d.childLanes = 0, d.pendingProps = k, b.deletions = null) : (d = Pg(e, k), d.subtreeFlags = e.subtreeFlags & 14680064);
          null !== h ? f = Pg(h, f) : (f = Tg(f, g, c, null), f.flags |= 2);
          f.return = b;
          d.return = b;
          d.sibling = f;
          b.child = d;
          d = f;
          f = b.child;
          g = a.child.memoizedState;
          g = null === g ? nj(c) : { baseLanes: g.baseLanes | c, cachePool: null, transitions: g.transitions };
          f.memoizedState = g;
          f.childLanes = a.childLanes & ~c;
          b.memoizedState = mj;
          return d;
        }
        f = a.child;
        a = f.sibling;
        d = Pg(f, { mode: "visible", children: d.children });
        0 === (b.mode & 1) && (d.lanes = c);
        d.return = b;
        d.sibling = null;
        null !== a && (c = b.deletions, null === c ? (b.deletions = [a], b.flags |= 16) : c.push(a));
        b.child = d;
        b.memoizedState = null;
        return d;
      }
      function qj(a, b) {
        b = pj({ mode: "visible", children: b }, a.mode, 0, null);
        b.return = a;
        return a.child = b;
      }
      function sj(a, b, c, d) {
        null !== d && Jg(d);
        Ug(b, a.child, null, c);
        a = qj(b, b.pendingProps.children);
        a.flags |= 2;
        b.memoizedState = null;
        return a;
      }
      function rj(a, b, c, d, e, f, g) {
        if (c) {
          if (b.flags & 256) return b.flags &= -257, d = Ki(Error(p(422))), sj(a, b, g, d);
          if (null !== b.memoizedState) return b.child = a.child, b.flags |= 128, null;
          f = d.fallback;
          e = b.mode;
          d = pj({ mode: "visible", children: d.children }, e, 0, null);
          f = Tg(f, e, g, null);
          f.flags |= 2;
          d.return = b;
          f.return = b;
          d.sibling = f;
          b.child = d;
          0 !== (b.mode & 1) && Ug(b, a.child, null, g);
          b.child.memoizedState = nj(g);
          b.memoizedState = mj;
          return f;
        }
        if (0 === (b.mode & 1)) return sj(a, b, g, null);
        if ("$!" === e.data) {
          d = e.nextSibling && e.nextSibling.dataset;
          if (d) var h = d.dgst;
          d = h;
          f = Error(p(419));
          d = Ki(f, d, void 0);
          return sj(a, b, g, d);
        }
        h = 0 !== (g & a.childLanes);
        if (dh || h) {
          d = Q;
          if (null !== d) {
            switch (g & -g) {
              case 4:
                e = 2;
                break;
              case 16:
                e = 8;
                break;
              case 64:
              case 128:
              case 256:
              case 512:
              case 1024:
              case 2048:
              case 4096:
              case 8192:
              case 16384:
              case 32768:
              case 65536:
              case 131072:
              case 262144:
              case 524288:
              case 1048576:
              case 2097152:
              case 4194304:
              case 8388608:
              case 16777216:
              case 33554432:
              case 67108864:
                e = 32;
                break;
              case 536870912:
                e = 268435456;
                break;
              default:
                e = 0;
            }
            e = 0 !== (e & (d.suspendedLanes | g)) ? 0 : e;
            0 !== e && e !== f.retryLane && (f.retryLane = e, ih(a, e), gi(d, a, e, -1));
          }
          tj();
          d = Ki(Error(p(421)));
          return sj(a, b, g, d);
        }
        if ("$?" === e.data) return b.flags |= 128, b.child = a.child, b = uj.bind(null, a), e._reactRetry = b, null;
        a = f.treeContext;
        yg = Lf(e.nextSibling);
        xg = b;
        I = true;
        zg = null;
        null !== a && (og[pg++] = rg, og[pg++] = sg, og[pg++] = qg, rg = a.id, sg = a.overflow, qg = b);
        b = qj(b, d.children);
        b.flags |= 4096;
        return b;
      }
      function vj(a, b, c) {
        a.lanes |= b;
        var d = a.alternate;
        null !== d && (d.lanes |= b);
        bh(a.return, b, c);
      }
      function wj(a, b, c, d, e) {
        var f = a.memoizedState;
        null === f ? a.memoizedState = { isBackwards: b, rendering: null, renderingStartTime: 0, last: d, tail: c, tailMode: e } : (f.isBackwards = b, f.rendering = null, f.renderingStartTime = 0, f.last = d, f.tail = c, f.tailMode = e);
      }
      function xj(a, b, c) {
        var d = b.pendingProps, e = d.revealOrder, f = d.tail;
        Xi(a, b, d.children, c);
        d = L.current;
        if (0 !== (d & 2)) d = d & 1 | 2, b.flags |= 128;
        else {
          if (null !== a && 0 !== (a.flags & 128)) a: for (a = b.child; null !== a; ) {
            if (13 === a.tag) null !== a.memoizedState && vj(a, c, b);
            else if (19 === a.tag) vj(a, c, b);
            else if (null !== a.child) {
              a.child.return = a;
              a = a.child;
              continue;
            }
            if (a === b) break a;
            for (; null === a.sibling; ) {
              if (null === a.return || a.return === b) break a;
              a = a.return;
            }
            a.sibling.return = a.return;
            a = a.sibling;
          }
          d &= 1;
        }
        G(L, d);
        if (0 === (b.mode & 1)) b.memoizedState = null;
        else switch (e) {
          case "forwards":
            c = b.child;
            for (e = null; null !== c; ) a = c.alternate, null !== a && null === Ch(a) && (e = c), c = c.sibling;
            c = e;
            null === c ? (e = b.child, b.child = null) : (e = c.sibling, c.sibling = null);
            wj(b, false, e, c, f);
            break;
          case "backwards":
            c = null;
            e = b.child;
            for (b.child = null; null !== e; ) {
              a = e.alternate;
              if (null !== a && null === Ch(a)) {
                b.child = e;
                break;
              }
              a = e.sibling;
              e.sibling = c;
              c = e;
              e = a;
            }
            wj(b, true, c, null, f);
            break;
          case "together":
            wj(b, false, null, null, void 0);
            break;
          default:
            b.memoizedState = null;
        }
        return b.child;
      }
      function ij(a, b) {
        0 === (b.mode & 1) && null !== a && (a.alternate = null, b.alternate = null, b.flags |= 2);
      }
      function Zi(a, b, c) {
        null !== a && (b.dependencies = a.dependencies);
        rh |= b.lanes;
        if (0 === (c & b.childLanes)) return null;
        if (null !== a && b.child !== a.child) throw Error(p(153));
        if (null !== b.child) {
          a = b.child;
          c = Pg(a, a.pendingProps);
          b.child = c;
          for (c.return = b; null !== a.sibling; ) a = a.sibling, c = c.sibling = Pg(a, a.pendingProps), c.return = b;
          c.sibling = null;
        }
        return b.child;
      }
      function yj(a, b, c) {
        switch (b.tag) {
          case 3:
            kj(b);
            Ig();
            break;
          case 5:
            Ah(b);
            break;
          case 1:
            Zf(b.type) && cg(b);
            break;
          case 4:
            yh(b, b.stateNode.containerInfo);
            break;
          case 10:
            var d = b.type._context, e = b.memoizedProps.value;
            G(Wg, d._currentValue);
            d._currentValue = e;
            break;
          case 13:
            d = b.memoizedState;
            if (null !== d) {
              if (null !== d.dehydrated) return G(L, L.current & 1), b.flags |= 128, null;
              if (0 !== (c & b.child.childLanes)) return oj(a, b, c);
              G(L, L.current & 1);
              a = Zi(a, b, c);
              return null !== a ? a.sibling : null;
            }
            G(L, L.current & 1);
            break;
          case 19:
            d = 0 !== (c & b.childLanes);
            if (0 !== (a.flags & 128)) {
              if (d) return xj(a, b, c);
              b.flags |= 128;
            }
            e = b.memoizedState;
            null !== e && (e.rendering = null, e.tail = null, e.lastEffect = null);
            G(L, L.current);
            if (d) break;
            else return null;
          case 22:
          case 23:
            return b.lanes = 0, dj(a, b, c);
        }
        return Zi(a, b, c);
      }
      var zj;
      var Aj;
      var Bj;
      var Cj;
      zj = function(a, b) {
        for (var c = b.child; null !== c; ) {
          if (5 === c.tag || 6 === c.tag) a.appendChild(c.stateNode);
          else if (4 !== c.tag && null !== c.child) {
            c.child.return = c;
            c = c.child;
            continue;
          }
          if (c === b) break;
          for (; null === c.sibling; ) {
            if (null === c.return || c.return === b) return;
            c = c.return;
          }
          c.sibling.return = c.return;
          c = c.sibling;
        }
      };
      Aj = function() {
      };
      Bj = function(a, b, c, d) {
        var e = a.memoizedProps;
        if (e !== d) {
          a = b.stateNode;
          xh(uh.current);
          var f = null;
          switch (c) {
            case "input":
              e = Ya(a, e);
              d = Ya(a, d);
              f = [];
              break;
            case "select":
              e = A({}, e, { value: void 0 });
              d = A({}, d, { value: void 0 });
              f = [];
              break;
            case "textarea":
              e = gb(a, e);
              d = gb(a, d);
              f = [];
              break;
            default:
              "function" !== typeof e.onClick && "function" === typeof d.onClick && (a.onclick = Bf);
          }
          ub(c, d);
          var g;
          c = null;
          for (l in e) if (!d.hasOwnProperty(l) && e.hasOwnProperty(l) && null != e[l]) if ("style" === l) {
            var h = e[l];
            for (g in h) h.hasOwnProperty(g) && (c || (c = {}), c[g] = "");
          } else "dangerouslySetInnerHTML" !== l && "children" !== l && "suppressContentEditableWarning" !== l && "suppressHydrationWarning" !== l && "autoFocus" !== l && (ea.hasOwnProperty(l) ? f || (f = []) : (f = f || []).push(l, null));
          for (l in d) {
            var k = d[l];
            h = null != e ? e[l] : void 0;
            if (d.hasOwnProperty(l) && k !== h && (null != k || null != h)) if ("style" === l) if (h) {
              for (g in h) !h.hasOwnProperty(g) || k && k.hasOwnProperty(g) || (c || (c = {}), c[g] = "");
              for (g in k) k.hasOwnProperty(g) && h[g] !== k[g] && (c || (c = {}), c[g] = k[g]);
            } else c || (f || (f = []), f.push(
              l,
              c
            )), c = k;
            else "dangerouslySetInnerHTML" === l ? (k = k ? k.__html : void 0, h = h ? h.__html : void 0, null != k && h !== k && (f = f || []).push(l, k)) : "children" === l ? "string" !== typeof k && "number" !== typeof k || (f = f || []).push(l, "" + k) : "suppressContentEditableWarning" !== l && "suppressHydrationWarning" !== l && (ea.hasOwnProperty(l) ? (null != k && "onScroll" === l && D("scroll", a), f || h === k || (f = [])) : (f = f || []).push(l, k));
          }
          c && (f = f || []).push("style", c);
          var l = f;
          if (b.updateQueue = l) b.flags |= 4;
        }
      };
      Cj = function(a, b, c, d) {
        c !== d && (b.flags |= 4);
      };
      function Dj(a, b) {
        if (!I) switch (a.tailMode) {
          case "hidden":
            b = a.tail;
            for (var c = null; null !== b; ) null !== b.alternate && (c = b), b = b.sibling;
            null === c ? a.tail = null : c.sibling = null;
            break;
          case "collapsed":
            c = a.tail;
            for (var d = null; null !== c; ) null !== c.alternate && (d = c), c = c.sibling;
            null === d ? b || null === a.tail ? a.tail = null : a.tail.sibling = null : d.sibling = null;
        }
      }
      function S(a) {
        var b = null !== a.alternate && a.alternate.child === a.child, c = 0, d = 0;
        if (b) for (var e = a.child; null !== e; ) c |= e.lanes | e.childLanes, d |= e.subtreeFlags & 14680064, d |= e.flags & 14680064, e.return = a, e = e.sibling;
        else for (e = a.child; null !== e; ) c |= e.lanes | e.childLanes, d |= e.subtreeFlags, d |= e.flags, e.return = a, e = e.sibling;
        a.subtreeFlags |= d;
        a.childLanes = c;
        return b;
      }
      function Ej(a, b, c) {
        var d = b.pendingProps;
        wg(b);
        switch (b.tag) {
          case 2:
          case 16:
          case 15:
          case 0:
          case 11:
          case 7:
          case 8:
          case 12:
          case 9:
          case 14:
            return S(b), null;
          case 1:
            return Zf(b.type) && $f(), S(b), null;
          case 3:
            d = b.stateNode;
            zh();
            E(Wf);
            E(H);
            Eh();
            d.pendingContext && (d.context = d.pendingContext, d.pendingContext = null);
            if (null === a || null === a.child) Gg(b) ? b.flags |= 4 : null === a || a.memoizedState.isDehydrated && 0 === (b.flags & 256) || (b.flags |= 1024, null !== zg && (Fj(zg), zg = null));
            Aj(a, b);
            S(b);
            return null;
          case 5:
            Bh(b);
            var e = xh(wh.current);
            c = b.type;
            if (null !== a && null != b.stateNode) Bj(a, b, c, d, e), a.ref !== b.ref && (b.flags |= 512, b.flags |= 2097152);
            else {
              if (!d) {
                if (null === b.stateNode) throw Error(p(166));
                S(b);
                return null;
              }
              a = xh(uh.current);
              if (Gg(b)) {
                d = b.stateNode;
                c = b.type;
                var f = b.memoizedProps;
                d[Of] = b;
                d[Pf] = f;
                a = 0 !== (b.mode & 1);
                switch (c) {
                  case "dialog":
                    D("cancel", d);
                    D("close", d);
                    break;
                  case "iframe":
                  case "object":
                  case "embed":
                    D("load", d);
                    break;
                  case "video":
                  case "audio":
                    for (e = 0; e < lf.length; e++) D(lf[e], d);
                    break;
                  case "source":
                    D("error", d);
                    break;
                  case "img":
                  case "image":
                  case "link":
                    D(
                      "error",
                      d
                    );
                    D("load", d);
                    break;
                  case "details":
                    D("toggle", d);
                    break;
                  case "input":
                    Za(d, f);
                    D("invalid", d);
                    break;
                  case "select":
                    d._wrapperState = { wasMultiple: !!f.multiple };
                    D("invalid", d);
                    break;
                  case "textarea":
                    hb(d, f), D("invalid", d);
                }
                ub(c, f);
                e = null;
                for (var g in f) if (f.hasOwnProperty(g)) {
                  var h = f[g];
                  "children" === g ? "string" === typeof h ? d.textContent !== h && (true !== f.suppressHydrationWarning && Af(d.textContent, h, a), e = ["children", h]) : "number" === typeof h && d.textContent !== "" + h && (true !== f.suppressHydrationWarning && Af(
                    d.textContent,
                    h,
                    a
                  ), e = ["children", "" + h]) : ea.hasOwnProperty(g) && null != h && "onScroll" === g && D("scroll", d);
                }
                switch (c) {
                  case "input":
                    Va(d);
                    db(d, f, true);
                    break;
                  case "textarea":
                    Va(d);
                    jb(d);
                    break;
                  case "select":
                  case "option":
                    break;
                  default:
                    "function" === typeof f.onClick && (d.onclick = Bf);
                }
                d = e;
                b.updateQueue = d;
                null !== d && (b.flags |= 4);
              } else {
                g = 9 === e.nodeType ? e : e.ownerDocument;
                "http://www.w3.org/1999/xhtml" === a && (a = kb(c));
                "http://www.w3.org/1999/xhtml" === a ? "script" === c ? (a = g.createElement("div"), a.innerHTML = "<script><\/script>", a = a.removeChild(a.firstChild)) : "string" === typeof d.is ? a = g.createElement(c, { is: d.is }) : (a = g.createElement(c), "select" === c && (g = a, d.multiple ? g.multiple = true : d.size && (g.size = d.size))) : a = g.createElementNS(a, c);
                a[Of] = b;
                a[Pf] = d;
                zj(a, b, false, false);
                b.stateNode = a;
                a: {
                  g = vb(c, d);
                  switch (c) {
                    case "dialog":
                      D("cancel", a);
                      D("close", a);
                      e = d;
                      break;
                    case "iframe":
                    case "object":
                    case "embed":
                      D("load", a);
                      e = d;
                      break;
                    case "video":
                    case "audio":
                      for (e = 0; e < lf.length; e++) D(lf[e], a);
                      e = d;
                      break;
                    case "source":
                      D("error", a);
                      e = d;
                      break;
                    case "img":
                    case "image":
                    case "link":
                      D(
                        "error",
                        a
                      );
                      D("load", a);
                      e = d;
                      break;
                    case "details":
                      D("toggle", a);
                      e = d;
                      break;
                    case "input":
                      Za(a, d);
                      e = Ya(a, d);
                      D("invalid", a);
                      break;
                    case "option":
                      e = d;
                      break;
                    case "select":
                      a._wrapperState = { wasMultiple: !!d.multiple };
                      e = A({}, d, { value: void 0 });
                      D("invalid", a);
                      break;
                    case "textarea":
                      hb(a, d);
                      e = gb(a, d);
                      D("invalid", a);
                      break;
                    default:
                      e = d;
                  }
                  ub(c, e);
                  h = e;
                  for (f in h) if (h.hasOwnProperty(f)) {
                    var k = h[f];
                    "style" === f ? sb(a, k) : "dangerouslySetInnerHTML" === f ? (k = k ? k.__html : void 0, null != k && nb(a, k)) : "children" === f ? "string" === typeof k ? ("textarea" !== c || "" !== k) && ob(a, k) : "number" === typeof k && ob(a, "" + k) : "suppressContentEditableWarning" !== f && "suppressHydrationWarning" !== f && "autoFocus" !== f && (ea.hasOwnProperty(f) ? null != k && "onScroll" === f && D("scroll", a) : null != k && ta(a, f, k, g));
                  }
                  switch (c) {
                    case "input":
                      Va(a);
                      db(a, d, false);
                      break;
                    case "textarea":
                      Va(a);
                      jb(a);
                      break;
                    case "option":
                      null != d.value && a.setAttribute("value", "" + Sa(d.value));
                      break;
                    case "select":
                      a.multiple = !!d.multiple;
                      f = d.value;
                      null != f ? fb(a, !!d.multiple, f, false) : null != d.defaultValue && fb(
                        a,
                        !!d.multiple,
                        d.defaultValue,
                        true
                      );
                      break;
                    default:
                      "function" === typeof e.onClick && (a.onclick = Bf);
                  }
                  switch (c) {
                    case "button":
                    case "input":
                    case "select":
                    case "textarea":
                      d = !!d.autoFocus;
                      break a;
                    case "img":
                      d = true;
                      break a;
                    default:
                      d = false;
                  }
                }
                d && (b.flags |= 4);
              }
              null !== b.ref && (b.flags |= 512, b.flags |= 2097152);
            }
            S(b);
            return null;
          case 6:
            if (a && null != b.stateNode) Cj(a, b, a.memoizedProps, d);
            else {
              if ("string" !== typeof d && null === b.stateNode) throw Error(p(166));
              c = xh(wh.current);
              xh(uh.current);
              if (Gg(b)) {
                d = b.stateNode;
                c = b.memoizedProps;
                d[Of] = b;
                if (f = d.nodeValue !== c) {
                  if (a = xg, null !== a) switch (a.tag) {
                    case 3:
                      Af(d.nodeValue, c, 0 !== (a.mode & 1));
                      break;
                    case 5:
                      true !== a.memoizedProps.suppressHydrationWarning && Af(d.nodeValue, c, 0 !== (a.mode & 1));
                  }
                }
                f && (b.flags |= 4);
              } else d = (9 === c.nodeType ? c : c.ownerDocument).createTextNode(d), d[Of] = b, b.stateNode = d;
            }
            S(b);
            return null;
          case 13:
            E(L);
            d = b.memoizedState;
            if (null === a || null !== a.memoizedState && null !== a.memoizedState.dehydrated) {
              if (I && null !== yg && 0 !== (b.mode & 1) && 0 === (b.flags & 128)) Hg(), Ig(), b.flags |= 98560, f = false;
              else if (f = Gg(b), null !== d && null !== d.dehydrated) {
                if (null === a) {
                  if (!f) throw Error(p(318));
                  f = b.memoizedState;
                  f = null !== f ? f.dehydrated : null;
                  if (!f) throw Error(p(317));
                  f[Of] = b;
                } else Ig(), 0 === (b.flags & 128) && (b.memoizedState = null), b.flags |= 4;
                S(b);
                f = false;
              } else null !== zg && (Fj(zg), zg = null), f = true;
              if (!f) return b.flags & 65536 ? b : null;
            }
            if (0 !== (b.flags & 128)) return b.lanes = c, b;
            d = null !== d;
            d !== (null !== a && null !== a.memoizedState) && d && (b.child.flags |= 8192, 0 !== (b.mode & 1) && (null === a || 0 !== (L.current & 1) ? 0 === T && (T = 3) : tj()));
            null !== b.updateQueue && (b.flags |= 4);
            S(b);
            return null;
          case 4:
            return zh(), Aj(a, b), null === a && sf(b.stateNode.containerInfo), S(b), null;
          case 10:
            return ah(b.type._context), S(b), null;
          case 17:
            return Zf(b.type) && $f(), S(b), null;
          case 19:
            E(L);
            f = b.memoizedState;
            if (null === f) return S(b), null;
            d = 0 !== (b.flags & 128);
            g = f.rendering;
            if (null === g) if (d) Dj(f, false);
            else {
              if (0 !== T || null !== a && 0 !== (a.flags & 128)) for (a = b.child; null !== a; ) {
                g = Ch(a);
                if (null !== g) {
                  b.flags |= 128;
                  Dj(f, false);
                  d = g.updateQueue;
                  null !== d && (b.updateQueue = d, b.flags |= 4);
                  b.subtreeFlags = 0;
                  d = c;
                  for (c = b.child; null !== c; ) f = c, a = d, f.flags &= 14680066, g = f.alternate, null === g ? (f.childLanes = 0, f.lanes = a, f.child = null, f.subtreeFlags = 0, f.memoizedProps = null, f.memoizedState = null, f.updateQueue = null, f.dependencies = null, f.stateNode = null) : (f.childLanes = g.childLanes, f.lanes = g.lanes, f.child = g.child, f.subtreeFlags = 0, f.deletions = null, f.memoizedProps = g.memoizedProps, f.memoizedState = g.memoizedState, f.updateQueue = g.updateQueue, f.type = g.type, a = g.dependencies, f.dependencies = null === a ? null : { lanes: a.lanes, firstContext: a.firstContext }), c = c.sibling;
                  G(L, L.current & 1 | 2);
                  return b.child;
                }
                a = a.sibling;
              }
              null !== f.tail && B() > Gj && (b.flags |= 128, d = true, Dj(f, false), b.lanes = 4194304);
            }
            else {
              if (!d) if (a = Ch(g), null !== a) {
                if (b.flags |= 128, d = true, c = a.updateQueue, null !== c && (b.updateQueue = c, b.flags |= 4), Dj(f, true), null === f.tail && "hidden" === f.tailMode && !g.alternate && !I) return S(b), null;
              } else 2 * B() - f.renderingStartTime > Gj && 1073741824 !== c && (b.flags |= 128, d = true, Dj(f, false), b.lanes = 4194304);
              f.isBackwards ? (g.sibling = b.child, b.child = g) : (c = f.last, null !== c ? c.sibling = g : b.child = g, f.last = g);
            }
            if (null !== f.tail) return b = f.tail, f.rendering = b, f.tail = b.sibling, f.renderingStartTime = B(), b.sibling = null, c = L.current, G(L, d ? c & 1 | 2 : c & 1), b;
            S(b);
            return null;
          case 22:
          case 23:
            return Hj(), d = null !== b.memoizedState, null !== a && null !== a.memoizedState !== d && (b.flags |= 8192), d && 0 !== (b.mode & 1) ? 0 !== (fj & 1073741824) && (S(b), b.subtreeFlags & 6 && (b.flags |= 8192)) : S(b), null;
          case 24:
            return null;
          case 25:
            return null;
        }
        throw Error(p(156, b.tag));
      }
      function Ij(a, b) {
        wg(b);
        switch (b.tag) {
          case 1:
            return Zf(b.type) && $f(), a = b.flags, a & 65536 ? (b.flags = a & -65537 | 128, b) : null;
          case 3:
            return zh(), E(Wf), E(H), Eh(), a = b.flags, 0 !== (a & 65536) && 0 === (a & 128) ? (b.flags = a & -65537 | 128, b) : null;
          case 5:
            return Bh(b), null;
          case 13:
            E(L);
            a = b.memoizedState;
            if (null !== a && null !== a.dehydrated) {
              if (null === b.alternate) throw Error(p(340));
              Ig();
            }
            a = b.flags;
            return a & 65536 ? (b.flags = a & -65537 | 128, b) : null;
          case 19:
            return E(L), null;
          case 4:
            return zh(), null;
          case 10:
            return ah(b.type._context), null;
          case 22:
          case 23:
            return Hj(), null;
          case 24:
            return null;
          default:
            return null;
        }
      }
      var Jj = false;
      var U = false;
      var Kj = "function" === typeof WeakSet ? WeakSet : Set;
      var V = null;
      function Lj(a, b) {
        var c = a.ref;
        if (null !== c) if ("function" === typeof c) try {
          c(null);
        } catch (d) {
          W(a, b, d);
        }
        else c.current = null;
      }
      function Mj(a, b, c) {
        try {
          c();
        } catch (d) {
          W(a, b, d);
        }
      }
      var Nj = false;
      function Oj(a, b) {
        Cf = dd;
        a = Me();
        if (Ne(a)) {
          if ("selectionStart" in a) var c = { start: a.selectionStart, end: a.selectionEnd };
          else a: {
            c = (c = a.ownerDocument) && c.defaultView || window;
            var d = c.getSelection && c.getSelection();
            if (d && 0 !== d.rangeCount) {
              c = d.anchorNode;
              var e = d.anchorOffset, f = d.focusNode;
              d = d.focusOffset;
              try {
                c.nodeType, f.nodeType;
              } catch (F) {
                c = null;
                break a;
              }
              var g = 0, h = -1, k = -1, l = 0, m = 0, q = a, r = null;
              b: for (; ; ) {
                for (var y; ; ) {
                  q !== c || 0 !== e && 3 !== q.nodeType || (h = g + e);
                  q !== f || 0 !== d && 3 !== q.nodeType || (k = g + d);
                  3 === q.nodeType && (g += q.nodeValue.length);
                  if (null === (y = q.firstChild)) break;
                  r = q;
                  q = y;
                }
                for (; ; ) {
                  if (q === a) break b;
                  r === c && ++l === e && (h = g);
                  r === f && ++m === d && (k = g);
                  if (null !== (y = q.nextSibling)) break;
                  q = r;
                  r = q.parentNode;
                }
                q = y;
              }
              c = -1 === h || -1 === k ? null : { start: h, end: k };
            } else c = null;
          }
          c = c || { start: 0, end: 0 };
        } else c = null;
        Df = { focusedElem: a, selectionRange: c };
        dd = false;
        for (V = b; null !== V; ) if (b = V, a = b.child, 0 !== (b.subtreeFlags & 1028) && null !== a) a.return = b, V = a;
        else for (; null !== V; ) {
          b = V;
          try {
            var n = b.alternate;
            if (0 !== (b.flags & 1024)) switch (b.tag) {
              case 0:
              case 11:
              case 15:
                break;
              case 1:
                if (null !== n) {
                  var t = n.memoizedProps, J = n.memoizedState, x = b.stateNode, w = x.getSnapshotBeforeUpdate(b.elementType === b.type ? t : Ci(b.type, t), J);
                  x.__reactInternalSnapshotBeforeUpdate = w;
                }
                break;
              case 3:
                var u = b.stateNode.containerInfo;
                1 === u.nodeType ? u.textContent = "" : 9 === u.nodeType && u.documentElement && u.removeChild(u.documentElement);
                break;
              case 5:
              case 6:
              case 4:
              case 17:
                break;
              default:
                throw Error(p(163));
            }
          } catch (F) {
            W(b, b.return, F);
          }
          a = b.sibling;
          if (null !== a) {
            a.return = b.return;
            V = a;
            break;
          }
          V = b.return;
        }
        n = Nj;
        Nj = false;
        return n;
      }
      function Pj(a, b, c) {
        var d = b.updateQueue;
        d = null !== d ? d.lastEffect : null;
        if (null !== d) {
          var e = d = d.next;
          do {
            if ((e.tag & a) === a) {
              var f = e.destroy;
              e.destroy = void 0;
              void 0 !== f && Mj(b, c, f);
            }
            e = e.next;
          } while (e !== d);
        }
      }
      function Qj(a, b) {
        b = b.updateQueue;
        b = null !== b ? b.lastEffect : null;
        if (null !== b) {
          var c = b = b.next;
          do {
            if ((c.tag & a) === a) {
              var d = c.create;
              c.destroy = d();
            }
            c = c.next;
          } while (c !== b);
        }
      }
      function Rj(a) {
        var b = a.ref;
        if (null !== b) {
          var c = a.stateNode;
          switch (a.tag) {
            case 5:
              a = c;
              break;
            default:
              a = c;
          }
          "function" === typeof b ? b(a) : b.current = a;
        }
      }
      function Sj(a) {
        var b = a.alternate;
        null !== b && (a.alternate = null, Sj(b));
        a.child = null;
        a.deletions = null;
        a.sibling = null;
        5 === a.tag && (b = a.stateNode, null !== b && (delete b[Of], delete b[Pf], delete b[of], delete b[Qf], delete b[Rf]));
        a.stateNode = null;
        a.return = null;
        a.dependencies = null;
        a.memoizedProps = null;
        a.memoizedState = null;
        a.pendingProps = null;
        a.stateNode = null;
        a.updateQueue = null;
      }
      function Tj(a) {
        return 5 === a.tag || 3 === a.tag || 4 === a.tag;
      }
      function Uj(a) {
        a: for (; ; ) {
          for (; null === a.sibling; ) {
            if (null === a.return || Tj(a.return)) return null;
            a = a.return;
          }
          a.sibling.return = a.return;
          for (a = a.sibling; 5 !== a.tag && 6 !== a.tag && 18 !== a.tag; ) {
            if (a.flags & 2) continue a;
            if (null === a.child || 4 === a.tag) continue a;
            else a.child.return = a, a = a.child;
          }
          if (!(a.flags & 2)) return a.stateNode;
        }
      }
      function Vj(a, b, c) {
        var d = a.tag;
        if (5 === d || 6 === d) a = a.stateNode, b ? 8 === c.nodeType ? c.parentNode.insertBefore(a, b) : c.insertBefore(a, b) : (8 === c.nodeType ? (b = c.parentNode, b.insertBefore(a, c)) : (b = c, b.appendChild(a)), c = c._reactRootContainer, null !== c && void 0 !== c || null !== b.onclick || (b.onclick = Bf));
        else if (4 !== d && (a = a.child, null !== a)) for (Vj(a, b, c), a = a.sibling; null !== a; ) Vj(a, b, c), a = a.sibling;
      }
      function Wj(a, b, c) {
        var d = a.tag;
        if (5 === d || 6 === d) a = a.stateNode, b ? c.insertBefore(a, b) : c.appendChild(a);
        else if (4 !== d && (a = a.child, null !== a)) for (Wj(a, b, c), a = a.sibling; null !== a; ) Wj(a, b, c), a = a.sibling;
      }
      var X = null;
      var Xj = false;
      function Yj(a, b, c) {
        for (c = c.child; null !== c; ) Zj(a, b, c), c = c.sibling;
      }
      function Zj(a, b, c) {
        if (lc && "function" === typeof lc.onCommitFiberUnmount) try {
          lc.onCommitFiberUnmount(kc, c);
        } catch (h) {
        }
        switch (c.tag) {
          case 5:
            U || Lj(c, b);
          case 6:
            var d = X, e = Xj;
            X = null;
            Yj(a, b, c);
            X = d;
            Xj = e;
            null !== X && (Xj ? (a = X, c = c.stateNode, 8 === a.nodeType ? a.parentNode.removeChild(c) : a.removeChild(c)) : X.removeChild(c.stateNode));
            break;
          case 18:
            null !== X && (Xj ? (a = X, c = c.stateNode, 8 === a.nodeType ? Kf(a.parentNode, c) : 1 === a.nodeType && Kf(a, c), bd(a)) : Kf(X, c.stateNode));
            break;
          case 4:
            d = X;
            e = Xj;
            X = c.stateNode.containerInfo;
            Xj = true;
            Yj(a, b, c);
            X = d;
            Xj = e;
            break;
          case 0:
          case 11:
          case 14:
          case 15:
            if (!U && (d = c.updateQueue, null !== d && (d = d.lastEffect, null !== d))) {
              e = d = d.next;
              do {
                var f = e, g = f.destroy;
                f = f.tag;
                void 0 !== g && (0 !== (f & 2) ? Mj(c, b, g) : 0 !== (f & 4) && Mj(c, b, g));
                e = e.next;
              } while (e !== d);
            }
            Yj(a, b, c);
            break;
          case 1:
            if (!U && (Lj(c, b), d = c.stateNode, "function" === typeof d.componentWillUnmount)) try {
              d.props = c.memoizedProps, d.state = c.memoizedState, d.componentWillUnmount();
            } catch (h) {
              W(c, b, h);
            }
            Yj(a, b, c);
            break;
          case 21:
            Yj(a, b, c);
            break;
          case 22:
            c.mode & 1 ? (U = (d = U) || null !== c.memoizedState, Yj(a, b, c), U = d) : Yj(a, b, c);
            break;
          default:
            Yj(a, b, c);
        }
      }
      function ak(a) {
        var b = a.updateQueue;
        if (null !== b) {
          a.updateQueue = null;
          var c = a.stateNode;
          null === c && (c = a.stateNode = new Kj());
          b.forEach(function(b2) {
            var d = bk.bind(null, a, b2);
            c.has(b2) || (c.add(b2), b2.then(d, d));
          });
        }
      }
      function ck(a, b) {
        var c = b.deletions;
        if (null !== c) for (var d = 0; d < c.length; d++) {
          var e = c[d];
          try {
            var f = a, g = b, h = g;
            a: for (; null !== h; ) {
              switch (h.tag) {
                case 5:
                  X = h.stateNode;
                  Xj = false;
                  break a;
                case 3:
                  X = h.stateNode.containerInfo;
                  Xj = true;
                  break a;
                case 4:
                  X = h.stateNode.containerInfo;
                  Xj = true;
                  break a;
              }
              h = h.return;
            }
            if (null === X) throw Error(p(160));
            Zj(f, g, e);
            X = null;
            Xj = false;
            var k = e.alternate;
            null !== k && (k.return = null);
            e.return = null;
          } catch (l) {
            W(e, b, l);
          }
        }
        if (b.subtreeFlags & 12854) for (b = b.child; null !== b; ) dk(b, a), b = b.sibling;
      }
      function dk(a, b) {
        var c = a.alternate, d = a.flags;
        switch (a.tag) {
          case 0:
          case 11:
          case 14:
          case 15:
            ck(b, a);
            ek(a);
            if (d & 4) {
              try {
                Pj(3, a, a.return), Qj(3, a);
              } catch (t) {
                W(a, a.return, t);
              }
              try {
                Pj(5, a, a.return);
              } catch (t) {
                W(a, a.return, t);
              }
            }
            break;
          case 1:
            ck(b, a);
            ek(a);
            d & 512 && null !== c && Lj(c, c.return);
            break;
          case 5:
            ck(b, a);
            ek(a);
            d & 512 && null !== c && Lj(c, c.return);
            if (a.flags & 32) {
              var e = a.stateNode;
              try {
                ob(e, "");
              } catch (t) {
                W(a, a.return, t);
              }
            }
            if (d & 4 && (e = a.stateNode, null != e)) {
              var f = a.memoizedProps, g = null !== c ? c.memoizedProps : f, h = a.type, k = a.updateQueue;
              a.updateQueue = null;
              if (null !== k) try {
                "input" === h && "radio" === f.type && null != f.name && ab(e, f);
                vb(h, g);
                var l = vb(h, f);
                for (g = 0; g < k.length; g += 2) {
                  var m = k[g], q = k[g + 1];
                  "style" === m ? sb(e, q) : "dangerouslySetInnerHTML" === m ? nb(e, q) : "children" === m ? ob(e, q) : ta(e, m, q, l);
                }
                switch (h) {
                  case "input":
                    bb(e, f);
                    break;
                  case "textarea":
                    ib(e, f);
                    break;
                  case "select":
                    var r = e._wrapperState.wasMultiple;
                    e._wrapperState.wasMultiple = !!f.multiple;
                    var y = f.value;
                    null != y ? fb(e, !!f.multiple, y, false) : r !== !!f.multiple && (null != f.defaultValue ? fb(
                      e,
                      !!f.multiple,
                      f.defaultValue,
                      true
                    ) : fb(e, !!f.multiple, f.multiple ? [] : "", false));
                }
                e[Pf] = f;
              } catch (t) {
                W(a, a.return, t);
              }
            }
            break;
          case 6:
            ck(b, a);
            ek(a);
            if (d & 4) {
              if (null === a.stateNode) throw Error(p(162));
              e = a.stateNode;
              f = a.memoizedProps;
              try {
                e.nodeValue = f;
              } catch (t) {
                W(a, a.return, t);
              }
            }
            break;
          case 3:
            ck(b, a);
            ek(a);
            if (d & 4 && null !== c && c.memoizedState.isDehydrated) try {
              bd(b.containerInfo);
            } catch (t) {
              W(a, a.return, t);
            }
            break;
          case 4:
            ck(b, a);
            ek(a);
            break;
          case 13:
            ck(b, a);
            ek(a);
            e = a.child;
            e.flags & 8192 && (f = null !== e.memoizedState, e.stateNode.isHidden = f, !f || null !== e.alternate && null !== e.alternate.memoizedState || (fk = B()));
            d & 4 && ak(a);
            break;
          case 22:
            m = null !== c && null !== c.memoizedState;
            a.mode & 1 ? (U = (l = U) || m, ck(b, a), U = l) : ck(b, a);
            ek(a);
            if (d & 8192) {
              l = null !== a.memoizedState;
              if ((a.stateNode.isHidden = l) && !m && 0 !== (a.mode & 1)) for (V = a, m = a.child; null !== m; ) {
                for (q = V = m; null !== V; ) {
                  r = V;
                  y = r.child;
                  switch (r.tag) {
                    case 0:
                    case 11:
                    case 14:
                    case 15:
                      Pj(4, r, r.return);
                      break;
                    case 1:
                      Lj(r, r.return);
                      var n = r.stateNode;
                      if ("function" === typeof n.componentWillUnmount) {
                        d = r;
                        c = r.return;
                        try {
                          b = d, n.props = b.memoizedProps, n.state = b.memoizedState, n.componentWillUnmount();
                        } catch (t) {
                          W(d, c, t);
                        }
                      }
                      break;
                    case 5:
                      Lj(r, r.return);
                      break;
                    case 22:
                      if (null !== r.memoizedState) {
                        gk(q);
                        continue;
                      }
                  }
                  null !== y ? (y.return = r, V = y) : gk(q);
                }
                m = m.sibling;
              }
              a: for (m = null, q = a; ; ) {
                if (5 === q.tag) {
                  if (null === m) {
                    m = q;
                    try {
                      e = q.stateNode, l ? (f = e.style, "function" === typeof f.setProperty ? f.setProperty("display", "none", "important") : f.display = "none") : (h = q.stateNode, k = q.memoizedProps.style, g = void 0 !== k && null !== k && k.hasOwnProperty("display") ? k.display : null, h.style.display = rb("display", g));
                    } catch (t) {
                      W(a, a.return, t);
                    }
                  }
                } else if (6 === q.tag) {
                  if (null === m) try {
                    q.stateNode.nodeValue = l ? "" : q.memoizedProps;
                  } catch (t) {
                    W(a, a.return, t);
                  }
                } else if ((22 !== q.tag && 23 !== q.tag || null === q.memoizedState || q === a) && null !== q.child) {
                  q.child.return = q;
                  q = q.child;
                  continue;
                }
                if (q === a) break a;
                for (; null === q.sibling; ) {
                  if (null === q.return || q.return === a) break a;
                  m === q && (m = null);
                  q = q.return;
                }
                m === q && (m = null);
                q.sibling.return = q.return;
                q = q.sibling;
              }
            }
            break;
          case 19:
            ck(b, a);
            ek(a);
            d & 4 && ak(a);
            break;
          case 21:
            break;
          default:
            ck(
              b,
              a
            ), ek(a);
        }
      }
      function ek(a) {
        var b = a.flags;
        if (b & 2) {
          try {
            a: {
              for (var c = a.return; null !== c; ) {
                if (Tj(c)) {
                  var d = c;
                  break a;
                }
                c = c.return;
              }
              throw Error(p(160));
            }
            switch (d.tag) {
              case 5:
                var e = d.stateNode;
                d.flags & 32 && (ob(e, ""), d.flags &= -33);
                var f = Uj(a);
                Wj(a, f, e);
                break;
              case 3:
              case 4:
                var g = d.stateNode.containerInfo, h = Uj(a);
                Vj(a, h, g);
                break;
              default:
                throw Error(p(161));
            }
          } catch (k) {
            W(a, a.return, k);
          }
          a.flags &= -3;
        }
        b & 4096 && (a.flags &= -4097);
      }
      function hk(a, b, c) {
        V = a;
        ik(a, b, c);
      }
      function ik(a, b, c) {
        for (var d = 0 !== (a.mode & 1); null !== V; ) {
          var e = V, f = e.child;
          if (22 === e.tag && d) {
            var g = null !== e.memoizedState || Jj;
            if (!g) {
              var h = e.alternate, k = null !== h && null !== h.memoizedState || U;
              h = Jj;
              var l = U;
              Jj = g;
              if ((U = k) && !l) for (V = e; null !== V; ) g = V, k = g.child, 22 === g.tag && null !== g.memoizedState ? jk(e) : null !== k ? (k.return = g, V = k) : jk(e);
              for (; null !== f; ) V = f, ik(f, b, c), f = f.sibling;
              V = e;
              Jj = h;
              U = l;
            }
            kk(a, b, c);
          } else 0 !== (e.subtreeFlags & 8772) && null !== f ? (f.return = e, V = f) : kk(a, b, c);
        }
      }
      function kk(a) {
        for (; null !== V; ) {
          var b = V;
          if (0 !== (b.flags & 8772)) {
            var c = b.alternate;
            try {
              if (0 !== (b.flags & 8772)) switch (b.tag) {
                case 0:
                case 11:
                case 15:
                  U || Qj(5, b);
                  break;
                case 1:
                  var d = b.stateNode;
                  if (b.flags & 4 && !U) if (null === c) d.componentDidMount();
                  else {
                    var e = b.elementType === b.type ? c.memoizedProps : Ci(b.type, c.memoizedProps);
                    d.componentDidUpdate(e, c.memoizedState, d.__reactInternalSnapshotBeforeUpdate);
                  }
                  var f = b.updateQueue;
                  null !== f && sh(b, f, d);
                  break;
                case 3:
                  var g = b.updateQueue;
                  if (null !== g) {
                    c = null;
                    if (null !== b.child) switch (b.child.tag) {
                      case 5:
                        c = b.child.stateNode;
                        break;
                      case 1:
                        c = b.child.stateNode;
                    }
                    sh(b, g, c);
                  }
                  break;
                case 5:
                  var h = b.stateNode;
                  if (null === c && b.flags & 4) {
                    c = h;
                    var k = b.memoizedProps;
                    switch (b.type) {
                      case "button":
                      case "input":
                      case "select":
                      case "textarea":
                        k.autoFocus && c.focus();
                        break;
                      case "img":
                        k.src && (c.src = k.src);
                    }
                  }
                  break;
                case 6:
                  break;
                case 4:
                  break;
                case 12:
                  break;
                case 13:
                  if (null === b.memoizedState) {
                    var l = b.alternate;
                    if (null !== l) {
                      var m = l.memoizedState;
                      if (null !== m) {
                        var q = m.dehydrated;
                        null !== q && bd(q);
                      }
                    }
                  }
                  break;
                case 19:
                case 17:
                case 21:
                case 22:
                case 23:
                case 25:
                  break;
                default:
                  throw Error(p(163));
              }
              U || b.flags & 512 && Rj(b);
            } catch (r) {
              W(b, b.return, r);
            }
          }
          if (b === a) {
            V = null;
            break;
          }
          c = b.sibling;
          if (null !== c) {
            c.return = b.return;
            V = c;
            break;
          }
          V = b.return;
        }
      }
      function gk(a) {
        for (; null !== V; ) {
          var b = V;
          if (b === a) {
            V = null;
            break;
          }
          var c = b.sibling;
          if (null !== c) {
            c.return = b.return;
            V = c;
            break;
          }
          V = b.return;
        }
      }
      function jk(a) {
        for (; null !== V; ) {
          var b = V;
          try {
            switch (b.tag) {
              case 0:
              case 11:
              case 15:
                var c = b.return;
                try {
                  Qj(4, b);
                } catch (k) {
                  W(b, c, k);
                }
                break;
              case 1:
                var d = b.stateNode;
                if ("function" === typeof d.componentDidMount) {
                  var e = b.return;
                  try {
                    d.componentDidMount();
                  } catch (k) {
                    W(b, e, k);
                  }
                }
                var f = b.return;
                try {
                  Rj(b);
                } catch (k) {
                  W(b, f, k);
                }
                break;
              case 5:
                var g = b.return;
                try {
                  Rj(b);
                } catch (k) {
                  W(b, g, k);
                }
            }
          } catch (k) {
            W(b, b.return, k);
          }
          if (b === a) {
            V = null;
            break;
          }
          var h = b.sibling;
          if (null !== h) {
            h.return = b.return;
            V = h;
            break;
          }
          V = b.return;
        }
      }
      var lk = Math.ceil;
      var mk = ua.ReactCurrentDispatcher;
      var nk = ua.ReactCurrentOwner;
      var ok = ua.ReactCurrentBatchConfig;
      var K = 0;
      var Q = null;
      var Y = null;
      var Z = 0;
      var fj = 0;
      var ej = Uf(0);
      var T = 0;
      var pk = null;
      var rh = 0;
      var qk = 0;
      var rk = 0;
      var sk = null;
      var tk = null;
      var fk = 0;
      var Gj = Infinity;
      var uk = null;
      var Oi = false;
      var Pi = null;
      var Ri = null;
      var vk = false;
      var wk = null;
      var xk = 0;
      var yk = 0;
      var zk = null;
      var Ak = -1;
      var Bk = 0;
      function R() {
        return 0 !== (K & 6) ? B() : -1 !== Ak ? Ak : Ak = B();
      }
      function yi(a) {
        if (0 === (a.mode & 1)) return 1;
        if (0 !== (K & 2) && 0 !== Z) return Z & -Z;
        if (null !== Kg.transition) return 0 === Bk && (Bk = yc()), Bk;
        a = C;
        if (0 !== a) return a;
        a = window.event;
        a = void 0 === a ? 16 : jd(a.type);
        return a;
      }
      function gi(a, b, c, d) {
        if (50 < yk) throw yk = 0, zk = null, Error(p(185));
        Ac(a, c, d);
        if (0 === (K & 2) || a !== Q) a === Q && (0 === (K & 2) && (qk |= c), 4 === T && Ck(a, Z)), Dk(a, d), 1 === c && 0 === K && 0 === (b.mode & 1) && (Gj = B() + 500, fg && jg());
      }
      function Dk(a, b) {
        var c = a.callbackNode;
        wc(a, b);
        var d = uc(a, a === Q ? Z : 0);
        if (0 === d) null !== c && bc(c), a.callbackNode = null, a.callbackPriority = 0;
        else if (b = d & -d, a.callbackPriority !== b) {
          null != c && bc(c);
          if (1 === b) 0 === a.tag ? ig(Ek.bind(null, a)) : hg(Ek.bind(null, a)), Jf(function() {
            0 === (K & 6) && jg();
          }), c = null;
          else {
            switch (Dc(d)) {
              case 1:
                c = fc;
                break;
              case 4:
                c = gc;
                break;
              case 16:
                c = hc;
                break;
              case 536870912:
                c = jc;
                break;
              default:
                c = hc;
            }
            c = Fk(c, Gk.bind(null, a));
          }
          a.callbackPriority = b;
          a.callbackNode = c;
        }
      }
      function Gk(a, b) {
        Ak = -1;
        Bk = 0;
        if (0 !== (K & 6)) throw Error(p(327));
        var c = a.callbackNode;
        if (Hk() && a.callbackNode !== c) return null;
        var d = uc(a, a === Q ? Z : 0);
        if (0 === d) return null;
        if (0 !== (d & 30) || 0 !== (d & a.expiredLanes) || b) b = Ik(a, d);
        else {
          b = d;
          var e = K;
          K |= 2;
          var f = Jk();
          if (Q !== a || Z !== b) uk = null, Gj = B() + 500, Kk(a, b);
          do
            try {
              Lk();
              break;
            } catch (h) {
              Mk(a, h);
            }
          while (1);
          $g();
          mk.current = f;
          K = e;
          null !== Y ? b = 0 : (Q = null, Z = 0, b = T);
        }
        if (0 !== b) {
          2 === b && (e = xc(a), 0 !== e && (d = e, b = Nk(a, e)));
          if (1 === b) throw c = pk, Kk(a, 0), Ck(a, d), Dk(a, B()), c;
          if (6 === b) Ck(a, d);
          else {
            e = a.current.alternate;
            if (0 === (d & 30) && !Ok(e) && (b = Ik(a, d), 2 === b && (f = xc(a), 0 !== f && (d = f, b = Nk(a, f))), 1 === b)) throw c = pk, Kk(a, 0), Ck(a, d), Dk(a, B()), c;
            a.finishedWork = e;
            a.finishedLanes = d;
            switch (b) {
              case 0:
              case 1:
                throw Error(p(345));
              case 2:
                Pk(a, tk, uk);
                break;
              case 3:
                Ck(a, d);
                if ((d & 130023424) === d && (b = fk + 500 - B(), 10 < b)) {
                  if (0 !== uc(a, 0)) break;
                  e = a.suspendedLanes;
                  if ((e & d) !== d) {
                    R();
                    a.pingedLanes |= a.suspendedLanes & e;
                    break;
                  }
                  a.timeoutHandle = Ff(Pk.bind(null, a, tk, uk), b);
                  break;
                }
                Pk(a, tk, uk);
                break;
              case 4:
                Ck(a, d);
                if ((d & 4194240) === d) break;
                b = a.eventTimes;
                for (e = -1; 0 < d; ) {
                  var g = 31 - oc(d);
                  f = 1 << g;
                  g = b[g];
                  g > e && (e = g);
                  d &= ~f;
                }
                d = e;
                d = B() - d;
                d = (120 > d ? 120 : 480 > d ? 480 : 1080 > d ? 1080 : 1920 > d ? 1920 : 3e3 > d ? 3e3 : 4320 > d ? 4320 : 1960 * lk(d / 1960)) - d;
                if (10 < d) {
                  a.timeoutHandle = Ff(Pk.bind(null, a, tk, uk), d);
                  break;
                }
                Pk(a, tk, uk);
                break;
              case 5:
                Pk(a, tk, uk);
                break;
              default:
                throw Error(p(329));
            }
          }
        }
        Dk(a, B());
        return a.callbackNode === c ? Gk.bind(null, a) : null;
      }
      function Nk(a, b) {
        var c = sk;
        a.current.memoizedState.isDehydrated && (Kk(a, b).flags |= 256);
        a = Ik(a, b);
        2 !== a && (b = tk, tk = c, null !== b && Fj(b));
        return a;
      }
      function Fj(a) {
        null === tk ? tk = a : tk.push.apply(tk, a);
      }
      function Ok(a) {
        for (var b = a; ; ) {
          if (b.flags & 16384) {
            var c = b.updateQueue;
            if (null !== c && (c = c.stores, null !== c)) for (var d = 0; d < c.length; d++) {
              var e = c[d], f = e.getSnapshot;
              e = e.value;
              try {
                if (!He(f(), e)) return false;
              } catch (g) {
                return false;
              }
            }
          }
          c = b.child;
          if (b.subtreeFlags & 16384 && null !== c) c.return = b, b = c;
          else {
            if (b === a) break;
            for (; null === b.sibling; ) {
              if (null === b.return || b.return === a) return true;
              b = b.return;
            }
            b.sibling.return = b.return;
            b = b.sibling;
          }
        }
        return true;
      }
      function Ck(a, b) {
        b &= ~rk;
        b &= ~qk;
        a.suspendedLanes |= b;
        a.pingedLanes &= ~b;
        for (a = a.expirationTimes; 0 < b; ) {
          var c = 31 - oc(b), d = 1 << c;
          a[c] = -1;
          b &= ~d;
        }
      }
      function Ek(a) {
        if (0 !== (K & 6)) throw Error(p(327));
        Hk();
        var b = uc(a, 0);
        if (0 === (b & 1)) return Dk(a, B()), null;
        var c = Ik(a, b);
        if (0 !== a.tag && 2 === c) {
          var d = xc(a);
          0 !== d && (b = d, c = Nk(a, d));
        }
        if (1 === c) throw c = pk, Kk(a, 0), Ck(a, b), Dk(a, B()), c;
        if (6 === c) throw Error(p(345));
        a.finishedWork = a.current.alternate;
        a.finishedLanes = b;
        Pk(a, tk, uk);
        Dk(a, B());
        return null;
      }
      function Qk(a, b) {
        var c = K;
        K |= 1;
        try {
          return a(b);
        } finally {
          K = c, 0 === K && (Gj = B() + 500, fg && jg());
        }
      }
      function Rk(a) {
        null !== wk && 0 === wk.tag && 0 === (K & 6) && Hk();
        var b = K;
        K |= 1;
        var c = ok.transition, d = C;
        try {
          if (ok.transition = null, C = 1, a) return a();
        } finally {
          C = d, ok.transition = c, K = b, 0 === (K & 6) && jg();
        }
      }
      function Hj() {
        fj = ej.current;
        E(ej);
      }
      function Kk(a, b) {
        a.finishedWork = null;
        a.finishedLanes = 0;
        var c = a.timeoutHandle;
        -1 !== c && (a.timeoutHandle = -1, Gf(c));
        if (null !== Y) for (c = Y.return; null !== c; ) {
          var d = c;
          wg(d);
          switch (d.tag) {
            case 1:
              d = d.type.childContextTypes;
              null !== d && void 0 !== d && $f();
              break;
            case 3:
              zh();
              E(Wf);
              E(H);
              Eh();
              break;
            case 5:
              Bh(d);
              break;
            case 4:
              zh();
              break;
            case 13:
              E(L);
              break;
            case 19:
              E(L);
              break;
            case 10:
              ah(d.type._context);
              break;
            case 22:
            case 23:
              Hj();
          }
          c = c.return;
        }
        Q = a;
        Y = a = Pg(a.current, null);
        Z = fj = b;
        T = 0;
        pk = null;
        rk = qk = rh = 0;
        tk = sk = null;
        if (null !== fh) {
          for (b = 0; b < fh.length; b++) if (c = fh[b], d = c.interleaved, null !== d) {
            c.interleaved = null;
            var e = d.next, f = c.pending;
            if (null !== f) {
              var g = f.next;
              f.next = e;
              d.next = g;
            }
            c.pending = d;
          }
          fh = null;
        }
        return a;
      }
      function Mk(a, b) {
        do {
          var c = Y;
          try {
            $g();
            Fh.current = Rh;
            if (Ih) {
              for (var d = M.memoizedState; null !== d; ) {
                var e = d.queue;
                null !== e && (e.pending = null);
                d = d.next;
              }
              Ih = false;
            }
            Hh = 0;
            O = N = M = null;
            Jh = false;
            Kh = 0;
            nk.current = null;
            if (null === c || null === c.return) {
              T = 1;
              pk = b;
              Y = null;
              break;
            }
            a: {
              var f = a, g = c.return, h = c, k = b;
              b = Z;
              h.flags |= 32768;
              if (null !== k && "object" === typeof k && "function" === typeof k.then) {
                var l = k, m = h, q = m.tag;
                if (0 === (m.mode & 1) && (0 === q || 11 === q || 15 === q)) {
                  var r = m.alternate;
                  r ? (m.updateQueue = r.updateQueue, m.memoizedState = r.memoizedState, m.lanes = r.lanes) : (m.updateQueue = null, m.memoizedState = null);
                }
                var y = Ui(g);
                if (null !== y) {
                  y.flags &= -257;
                  Vi(y, g, h, f, b);
                  y.mode & 1 && Si(f, l, b);
                  b = y;
                  k = l;
                  var n = b.updateQueue;
                  if (null === n) {
                    var t = /* @__PURE__ */ new Set();
                    t.add(k);
                    b.updateQueue = t;
                  } else n.add(k);
                  break a;
                } else {
                  if (0 === (b & 1)) {
                    Si(f, l, b);
                    tj();
                    break a;
                  }
                  k = Error(p(426));
                }
              } else if (I && h.mode & 1) {
                var J = Ui(g);
                if (null !== J) {
                  0 === (J.flags & 65536) && (J.flags |= 256);
                  Vi(J, g, h, f, b);
                  Jg(Ji(k, h));
                  break a;
                }
              }
              f = k = Ji(k, h);
              4 !== T && (T = 2);
              null === sk ? sk = [f] : sk.push(f);
              f = g;
              do {
                switch (f.tag) {
                  case 3:
                    f.flags |= 65536;
                    b &= -b;
                    f.lanes |= b;
                    var x = Ni(f, k, b);
                    ph(f, x);
                    break a;
                  case 1:
                    h = k;
                    var w = f.type, u = f.stateNode;
                    if (0 === (f.flags & 128) && ("function" === typeof w.getDerivedStateFromError || null !== u && "function" === typeof u.componentDidCatch && (null === Ri || !Ri.has(u)))) {
                      f.flags |= 65536;
                      b &= -b;
                      f.lanes |= b;
                      var F = Qi(f, h, b);
                      ph(f, F);
                      break a;
                    }
                }
                f = f.return;
              } while (null !== f);
            }
            Sk(c);
          } catch (na) {
            b = na;
            Y === c && null !== c && (Y = c = c.return);
            continue;
          }
          break;
        } while (1);
      }
      function Jk() {
        var a = mk.current;
        mk.current = Rh;
        return null === a ? Rh : a;
      }
      function tj() {
        if (0 === T || 3 === T || 2 === T) T = 4;
        null === Q || 0 === (rh & 268435455) && 0 === (qk & 268435455) || Ck(Q, Z);
      }
      function Ik(a, b) {
        var c = K;
        K |= 2;
        var d = Jk();
        if (Q !== a || Z !== b) uk = null, Kk(a, b);
        do
          try {
            Tk();
            break;
          } catch (e) {
            Mk(a, e);
          }
        while (1);
        $g();
        K = c;
        mk.current = d;
        if (null !== Y) throw Error(p(261));
        Q = null;
        Z = 0;
        return T;
      }
      function Tk() {
        for (; null !== Y; ) Uk(Y);
      }
      function Lk() {
        for (; null !== Y && !cc(); ) Uk(Y);
      }
      function Uk(a) {
        var b = Vk(a.alternate, a, fj);
        a.memoizedProps = a.pendingProps;
        null === b ? Sk(a) : Y = b;
        nk.current = null;
      }
      function Sk(a) {
        var b = a;
        do {
          var c = b.alternate;
          a = b.return;
          if (0 === (b.flags & 32768)) {
            if (c = Ej(c, b, fj), null !== c) {
              Y = c;
              return;
            }
          } else {
            c = Ij(c, b);
            if (null !== c) {
              c.flags &= 32767;
              Y = c;
              return;
            }
            if (null !== a) a.flags |= 32768, a.subtreeFlags = 0, a.deletions = null;
            else {
              T = 6;
              Y = null;
              return;
            }
          }
          b = b.sibling;
          if (null !== b) {
            Y = b;
            return;
          }
          Y = b = a;
        } while (null !== b);
        0 === T && (T = 5);
      }
      function Pk(a, b, c) {
        var d = C, e = ok.transition;
        try {
          ok.transition = null, C = 1, Wk(a, b, c, d);
        } finally {
          ok.transition = e, C = d;
        }
        return null;
      }
      function Wk(a, b, c, d) {
        do
          Hk();
        while (null !== wk);
        if (0 !== (K & 6)) throw Error(p(327));
        c = a.finishedWork;
        var e = a.finishedLanes;
        if (null === c) return null;
        a.finishedWork = null;
        a.finishedLanes = 0;
        if (c === a.current) throw Error(p(177));
        a.callbackNode = null;
        a.callbackPriority = 0;
        var f = c.lanes | c.childLanes;
        Bc(a, f);
        a === Q && (Y = Q = null, Z = 0);
        0 === (c.subtreeFlags & 2064) && 0 === (c.flags & 2064) || vk || (vk = true, Fk(hc, function() {
          Hk();
          return null;
        }));
        f = 0 !== (c.flags & 15990);
        if (0 !== (c.subtreeFlags & 15990) || f) {
          f = ok.transition;
          ok.transition = null;
          var g = C;
          C = 1;
          var h = K;
          K |= 4;
          nk.current = null;
          Oj(a, c);
          dk(c, a);
          Oe(Df);
          dd = !!Cf;
          Df = Cf = null;
          a.current = c;
          hk(c, a, e);
          dc();
          K = h;
          C = g;
          ok.transition = f;
        } else a.current = c;
        vk && (vk = false, wk = a, xk = e);
        f = a.pendingLanes;
        0 === f && (Ri = null);
        mc(c.stateNode, d);
        Dk(a, B());
        if (null !== b) for (d = a.onRecoverableError, c = 0; c < b.length; c++) e = b[c], d(e.value, { componentStack: e.stack, digest: e.digest });
        if (Oi) throw Oi = false, a = Pi, Pi = null, a;
        0 !== (xk & 1) && 0 !== a.tag && Hk();
        f = a.pendingLanes;
        0 !== (f & 1) ? a === zk ? yk++ : (yk = 0, zk = a) : yk = 0;
        jg();
        return null;
      }
      function Hk() {
        if (null !== wk) {
          var a = Dc(xk), b = ok.transition, c = C;
          try {
            ok.transition = null;
            C = 16 > a ? 16 : a;
            if (null === wk) var d = false;
            else {
              a = wk;
              wk = null;
              xk = 0;
              if (0 !== (K & 6)) throw Error(p(331));
              var e = K;
              K |= 4;
              for (V = a.current; null !== V; ) {
                var f = V, g = f.child;
                if (0 !== (V.flags & 16)) {
                  var h = f.deletions;
                  if (null !== h) {
                    for (var k = 0; k < h.length; k++) {
                      var l = h[k];
                      for (V = l; null !== V; ) {
                        var m = V;
                        switch (m.tag) {
                          case 0:
                          case 11:
                          case 15:
                            Pj(8, m, f);
                        }
                        var q = m.child;
                        if (null !== q) q.return = m, V = q;
                        else for (; null !== V; ) {
                          m = V;
                          var r = m.sibling, y = m.return;
                          Sj(m);
                          if (m === l) {
                            V = null;
                            break;
                          }
                          if (null !== r) {
                            r.return = y;
                            V = r;
                            break;
                          }
                          V = y;
                        }
                      }
                    }
                    var n = f.alternate;
                    if (null !== n) {
                      var t = n.child;
                      if (null !== t) {
                        n.child = null;
                        do {
                          var J = t.sibling;
                          t.sibling = null;
                          t = J;
                        } while (null !== t);
                      }
                    }
                    V = f;
                  }
                }
                if (0 !== (f.subtreeFlags & 2064) && null !== g) g.return = f, V = g;
                else b: for (; null !== V; ) {
                  f = V;
                  if (0 !== (f.flags & 2048)) switch (f.tag) {
                    case 0:
                    case 11:
                    case 15:
                      Pj(9, f, f.return);
                  }
                  var x = f.sibling;
                  if (null !== x) {
                    x.return = f.return;
                    V = x;
                    break b;
                  }
                  V = f.return;
                }
              }
              var w = a.current;
              for (V = w; null !== V; ) {
                g = V;
                var u = g.child;
                if (0 !== (g.subtreeFlags & 2064) && null !== u) u.return = g, V = u;
                else b: for (g = w; null !== V; ) {
                  h = V;
                  if (0 !== (h.flags & 2048)) try {
                    switch (h.tag) {
                      case 0:
                      case 11:
                      case 15:
                        Qj(9, h);
                    }
                  } catch (na) {
                    W(h, h.return, na);
                  }
                  if (h === g) {
                    V = null;
                    break b;
                  }
                  var F = h.sibling;
                  if (null !== F) {
                    F.return = h.return;
                    V = F;
                    break b;
                  }
                  V = h.return;
                }
              }
              K = e;
              jg();
              if (lc && "function" === typeof lc.onPostCommitFiberRoot) try {
                lc.onPostCommitFiberRoot(kc, a);
              } catch (na) {
              }
              d = true;
            }
            return d;
          } finally {
            C = c, ok.transition = b;
          }
        }
        return false;
      }
      function Xk(a, b, c) {
        b = Ji(c, b);
        b = Ni(a, b, 1);
        a = nh(a, b, 1);
        b = R();
        null !== a && (Ac(a, 1, b), Dk(a, b));
      }
      function W(a, b, c) {
        if (3 === a.tag) Xk(a, a, c);
        else for (; null !== b; ) {
          if (3 === b.tag) {
            Xk(b, a, c);
            break;
          } else if (1 === b.tag) {
            var d = b.stateNode;
            if ("function" === typeof b.type.getDerivedStateFromError || "function" === typeof d.componentDidCatch && (null === Ri || !Ri.has(d))) {
              a = Ji(c, a);
              a = Qi(b, a, 1);
              b = nh(b, a, 1);
              a = R();
              null !== b && (Ac(b, 1, a), Dk(b, a));
              break;
            }
          }
          b = b.return;
        }
      }
      function Ti(a, b, c) {
        var d = a.pingCache;
        null !== d && d.delete(b);
        b = R();
        a.pingedLanes |= a.suspendedLanes & c;
        Q === a && (Z & c) === c && (4 === T || 3 === T && (Z & 130023424) === Z && 500 > B() - fk ? Kk(a, 0) : rk |= c);
        Dk(a, b);
      }
      function Yk(a, b) {
        0 === b && (0 === (a.mode & 1) ? b = 1 : (b = sc, sc <<= 1, 0 === (sc & 130023424) && (sc = 4194304)));
        var c = R();
        a = ih(a, b);
        null !== a && (Ac(a, b, c), Dk(a, c));
      }
      function uj(a) {
        var b = a.memoizedState, c = 0;
        null !== b && (c = b.retryLane);
        Yk(a, c);
      }
      function bk(a, b) {
        var c = 0;
        switch (a.tag) {
          case 13:
            var d = a.stateNode;
            var e = a.memoizedState;
            null !== e && (c = e.retryLane);
            break;
          case 19:
            d = a.stateNode;
            break;
          default:
            throw Error(p(314));
        }
        null !== d && d.delete(b);
        Yk(a, c);
      }
      var Vk;
      Vk = function(a, b, c) {
        if (null !== a) if (a.memoizedProps !== b.pendingProps || Wf.current) dh = true;
        else {
          if (0 === (a.lanes & c) && 0 === (b.flags & 128)) return dh = false, yj(a, b, c);
          dh = 0 !== (a.flags & 131072) ? true : false;
        }
        else dh = false, I && 0 !== (b.flags & 1048576) && ug(b, ng, b.index);
        b.lanes = 0;
        switch (b.tag) {
          case 2:
            var d = b.type;
            ij(a, b);
            a = b.pendingProps;
            var e = Yf(b, H.current);
            ch(b, c);
            e = Nh(null, b, d, a, e, c);
            var f = Sh();
            b.flags |= 1;
            "object" === typeof e && null !== e && "function" === typeof e.render && void 0 === e.$$typeof ? (b.tag = 1, b.memoizedState = null, b.updateQueue = null, Zf(d) ? (f = true, cg(b)) : f = false, b.memoizedState = null !== e.state && void 0 !== e.state ? e.state : null, kh(b), e.updater = Ei, b.stateNode = e, e._reactInternals = b, Ii(b, d, a, c), b = jj(null, b, d, true, f, c)) : (b.tag = 0, I && f && vg(b), Xi(null, b, e, c), b = b.child);
            return b;
          case 16:
            d = b.elementType;
            a: {
              ij(a, b);
              a = b.pendingProps;
              e = d._init;
              d = e(d._payload);
              b.type = d;
              e = b.tag = Zk(d);
              a = Ci(d, a);
              switch (e) {
                case 0:
                  b = cj(null, b, d, a, c);
                  break a;
                case 1:
                  b = hj(null, b, d, a, c);
                  break a;
                case 11:
                  b = Yi(null, b, d, a, c);
                  break a;
                case 14:
                  b = $i(null, b, d, Ci(d.type, a), c);
                  break a;
              }
              throw Error(p(
                306,
                d,
                ""
              ));
            }
            return b;
          case 0:
            return d = b.type, e = b.pendingProps, e = b.elementType === d ? e : Ci(d, e), cj(a, b, d, e, c);
          case 1:
            return d = b.type, e = b.pendingProps, e = b.elementType === d ? e : Ci(d, e), hj(a, b, d, e, c);
          case 3:
            a: {
              kj(b);
              if (null === a) throw Error(p(387));
              d = b.pendingProps;
              f = b.memoizedState;
              e = f.element;
              lh(a, b);
              qh(b, d, null, c);
              var g = b.memoizedState;
              d = g.element;
              if (f.isDehydrated) if (f = { element: d, isDehydrated: false, cache: g.cache, pendingSuspenseBoundaries: g.pendingSuspenseBoundaries, transitions: g.transitions }, b.updateQueue.baseState = f, b.memoizedState = f, b.flags & 256) {
                e = Ji(Error(p(423)), b);
                b = lj(a, b, d, c, e);
                break a;
              } else if (d !== e) {
                e = Ji(Error(p(424)), b);
                b = lj(a, b, d, c, e);
                break a;
              } else for (yg = Lf(b.stateNode.containerInfo.firstChild), xg = b, I = true, zg = null, c = Vg(b, null, d, c), b.child = c; c; ) c.flags = c.flags & -3 | 4096, c = c.sibling;
              else {
                Ig();
                if (d === e) {
                  b = Zi(a, b, c);
                  break a;
                }
                Xi(a, b, d, c);
              }
              b = b.child;
            }
            return b;
          case 5:
            return Ah(b), null === a && Eg(b), d = b.type, e = b.pendingProps, f = null !== a ? a.memoizedProps : null, g = e.children, Ef(d, e) ? g = null : null !== f && Ef(d, f) && (b.flags |= 32), gj(a, b), Xi(a, b, g, c), b.child;
          case 6:
            return null === a && Eg(b), null;
          case 13:
            return oj(a, b, c);
          case 4:
            return yh(b, b.stateNode.containerInfo), d = b.pendingProps, null === a ? b.child = Ug(b, null, d, c) : Xi(a, b, d, c), b.child;
          case 11:
            return d = b.type, e = b.pendingProps, e = b.elementType === d ? e : Ci(d, e), Yi(a, b, d, e, c);
          case 7:
            return Xi(a, b, b.pendingProps, c), b.child;
          case 8:
            return Xi(a, b, b.pendingProps.children, c), b.child;
          case 12:
            return Xi(a, b, b.pendingProps.children, c), b.child;
          case 10:
            a: {
              d = b.type._context;
              e = b.pendingProps;
              f = b.memoizedProps;
              g = e.value;
              G(Wg, d._currentValue);
              d._currentValue = g;
              if (null !== f) if (He(f.value, g)) {
                if (f.children === e.children && !Wf.current) {
                  b = Zi(a, b, c);
                  break a;
                }
              } else for (f = b.child, null !== f && (f.return = b); null !== f; ) {
                var h = f.dependencies;
                if (null !== h) {
                  g = f.child;
                  for (var k = h.firstContext; null !== k; ) {
                    if (k.context === d) {
                      if (1 === f.tag) {
                        k = mh(-1, c & -c);
                        k.tag = 2;
                        var l = f.updateQueue;
                        if (null !== l) {
                          l = l.shared;
                          var m = l.pending;
                          null === m ? k.next = k : (k.next = m.next, m.next = k);
                          l.pending = k;
                        }
                      }
                      f.lanes |= c;
                      k = f.alternate;
                      null !== k && (k.lanes |= c);
                      bh(
                        f.return,
                        c,
                        b
                      );
                      h.lanes |= c;
                      break;
                    }
                    k = k.next;
                  }
                } else if (10 === f.tag) g = f.type === b.type ? null : f.child;
                else if (18 === f.tag) {
                  g = f.return;
                  if (null === g) throw Error(p(341));
                  g.lanes |= c;
                  h = g.alternate;
                  null !== h && (h.lanes |= c);
                  bh(g, c, b);
                  g = f.sibling;
                } else g = f.child;
                if (null !== g) g.return = f;
                else for (g = f; null !== g; ) {
                  if (g === b) {
                    g = null;
                    break;
                  }
                  f = g.sibling;
                  if (null !== f) {
                    f.return = g.return;
                    g = f;
                    break;
                  }
                  g = g.return;
                }
                f = g;
              }
              Xi(a, b, e.children, c);
              b = b.child;
            }
            return b;
          case 9:
            return e = b.type, d = b.pendingProps.children, ch(b, c), e = eh(e), d = d(e), b.flags |= 1, Xi(a, b, d, c), b.child;
          case 14:
            return d = b.type, e = Ci(d, b.pendingProps), e = Ci(d.type, e), $i(a, b, d, e, c);
          case 15:
            return bj(a, b, b.type, b.pendingProps, c);
          case 17:
            return d = b.type, e = b.pendingProps, e = b.elementType === d ? e : Ci(d, e), ij(a, b), b.tag = 1, Zf(d) ? (a = true, cg(b)) : a = false, ch(b, c), Gi(b, d, e), Ii(b, d, e, c), jj(null, b, d, true, a, c);
          case 19:
            return xj(a, b, c);
          case 22:
            return dj(a, b, c);
        }
        throw Error(p(156, b.tag));
      };
      function Fk(a, b) {
        return ac(a, b);
      }
      function $k(a, b, c, d) {
        this.tag = a;
        this.key = c;
        this.sibling = this.child = this.return = this.stateNode = this.type = this.elementType = null;
        this.index = 0;
        this.ref = null;
        this.pendingProps = b;
        this.dependencies = this.memoizedState = this.updateQueue = this.memoizedProps = null;
        this.mode = d;
        this.subtreeFlags = this.flags = 0;
        this.deletions = null;
        this.childLanes = this.lanes = 0;
        this.alternate = null;
      }
      function Bg(a, b, c, d) {
        return new $k(a, b, c, d);
      }
      function aj(a) {
        a = a.prototype;
        return !(!a || !a.isReactComponent);
      }
      function Zk(a) {
        if ("function" === typeof a) return aj(a) ? 1 : 0;
        if (void 0 !== a && null !== a) {
          a = a.$$typeof;
          if (a === Da) return 11;
          if (a === Ga) return 14;
        }
        return 2;
      }
      function Pg(a, b) {
        var c = a.alternate;
        null === c ? (c = Bg(a.tag, b, a.key, a.mode), c.elementType = a.elementType, c.type = a.type, c.stateNode = a.stateNode, c.alternate = a, a.alternate = c) : (c.pendingProps = b, c.type = a.type, c.flags = 0, c.subtreeFlags = 0, c.deletions = null);
        c.flags = a.flags & 14680064;
        c.childLanes = a.childLanes;
        c.lanes = a.lanes;
        c.child = a.child;
        c.memoizedProps = a.memoizedProps;
        c.memoizedState = a.memoizedState;
        c.updateQueue = a.updateQueue;
        b = a.dependencies;
        c.dependencies = null === b ? null : { lanes: b.lanes, firstContext: b.firstContext };
        c.sibling = a.sibling;
        c.index = a.index;
        c.ref = a.ref;
        return c;
      }
      function Rg(a, b, c, d, e, f) {
        var g = 2;
        d = a;
        if ("function" === typeof a) aj(a) && (g = 1);
        else if ("string" === typeof a) g = 5;
        else a: switch (a) {
          case ya:
            return Tg(c.children, e, f, b);
          case za:
            g = 8;
            e |= 8;
            break;
          case Aa:
            return a = Bg(12, c, b, e | 2), a.elementType = Aa, a.lanes = f, a;
          case Ea:
            return a = Bg(13, c, b, e), a.elementType = Ea, a.lanes = f, a;
          case Fa:
            return a = Bg(19, c, b, e), a.elementType = Fa, a.lanes = f, a;
          case Ia:
            return pj(c, e, f, b);
          default:
            if ("object" === typeof a && null !== a) switch (a.$$typeof) {
              case Ba:
                g = 10;
                break a;
              case Ca:
                g = 9;
                break a;
              case Da:
                g = 11;
                break a;
              case Ga:
                g = 14;
                break a;
              case Ha:
                g = 16;
                d = null;
                break a;
            }
            throw Error(p(130, null == a ? a : typeof a, ""));
        }
        b = Bg(g, c, b, e);
        b.elementType = a;
        b.type = d;
        b.lanes = f;
        return b;
      }
      function Tg(a, b, c, d) {
        a = Bg(7, a, d, b);
        a.lanes = c;
        return a;
      }
      function pj(a, b, c, d) {
        a = Bg(22, a, d, b);
        a.elementType = Ia;
        a.lanes = c;
        a.stateNode = { isHidden: false };
        return a;
      }
      function Qg(a, b, c) {
        a = Bg(6, a, null, b);
        a.lanes = c;
        return a;
      }
      function Sg(a, b, c) {
        b = Bg(4, null !== a.children ? a.children : [], a.key, b);
        b.lanes = c;
        b.stateNode = { containerInfo: a.containerInfo, pendingChildren: null, implementation: a.implementation };
        return b;
      }
      function al(a, b, c, d, e) {
        this.tag = b;
        this.containerInfo = a;
        this.finishedWork = this.pingCache = this.current = this.pendingChildren = null;
        this.timeoutHandle = -1;
        this.callbackNode = this.pendingContext = this.context = null;
        this.callbackPriority = 0;
        this.eventTimes = zc(0);
        this.expirationTimes = zc(-1);
        this.entangledLanes = this.finishedLanes = this.mutableReadLanes = this.expiredLanes = this.pingedLanes = this.suspendedLanes = this.pendingLanes = 0;
        this.entanglements = zc(0);
        this.identifierPrefix = d;
        this.onRecoverableError = e;
        this.mutableSourceEagerHydrationData = null;
      }
      function bl(a, b, c, d, e, f, g, h, k) {
        a = new al(a, b, c, h, k);
        1 === b ? (b = 1, true === f && (b |= 8)) : b = 0;
        f = Bg(3, null, null, b);
        a.current = f;
        f.stateNode = a;
        f.memoizedState = { element: d, isDehydrated: c, cache: null, transitions: null, pendingSuspenseBoundaries: null };
        kh(f);
        return a;
      }
      function cl(a, b, c) {
        var d = 3 < arguments.length && void 0 !== arguments[3] ? arguments[3] : null;
        return { $$typeof: wa, key: null == d ? null : "" + d, children: a, containerInfo: b, implementation: c };
      }
      function dl(a) {
        if (!a) return Vf;
        a = a._reactInternals;
        a: {
          if (Vb(a) !== a || 1 !== a.tag) throw Error(p(170));
          var b = a;
          do {
            switch (b.tag) {
              case 3:
                b = b.stateNode.context;
                break a;
              case 1:
                if (Zf(b.type)) {
                  b = b.stateNode.__reactInternalMemoizedMergedChildContext;
                  break a;
                }
            }
            b = b.return;
          } while (null !== b);
          throw Error(p(171));
        }
        if (1 === a.tag) {
          var c = a.type;
          if (Zf(c)) return bg(a, c, b);
        }
        return b;
      }
      function el(a, b, c, d, e, f, g, h, k) {
        a = bl(c, d, true, a, e, f, g, h, k);
        a.context = dl(null);
        c = a.current;
        d = R();
        e = yi(c);
        f = mh(d, e);
        f.callback = void 0 !== b && null !== b ? b : null;
        nh(c, f, e);
        a.current.lanes = e;
        Ac(a, e, d);
        Dk(a, d);
        return a;
      }
      function fl(a, b, c, d) {
        var e = b.current, f = R(), g = yi(e);
        c = dl(c);
        null === b.context ? b.context = c : b.pendingContext = c;
        b = mh(f, g);
        b.payload = { element: a };
        d = void 0 === d ? null : d;
        null !== d && (b.callback = d);
        a = nh(e, b, g);
        null !== a && (gi(a, e, g, f), oh(a, e, g));
        return g;
      }
      function gl(a) {
        a = a.current;
        if (!a.child) return null;
        switch (a.child.tag) {
          case 5:
            return a.child.stateNode;
          default:
            return a.child.stateNode;
        }
      }
      function hl(a, b) {
        a = a.memoizedState;
        if (null !== a && null !== a.dehydrated) {
          var c = a.retryLane;
          a.retryLane = 0 !== c && c < b ? c : b;
        }
      }
      function il(a, b) {
        hl(a, b);
        (a = a.alternate) && hl(a, b);
      }
      function jl() {
        return null;
      }
      var kl = "function" === typeof reportError ? reportError : function(a) {
        console.error(a);
      };
      function ll(a) {
        this._internalRoot = a;
      }
      ml.prototype.render = ll.prototype.render = function(a) {
        var b = this._internalRoot;
        if (null === b) throw Error(p(409));
        fl(a, b, null, null);
      };
      ml.prototype.unmount = ll.prototype.unmount = function() {
        var a = this._internalRoot;
        if (null !== a) {
          this._internalRoot = null;
          var b = a.containerInfo;
          Rk(function() {
            fl(null, a, null, null);
          });
          b[uf] = null;
        }
      };
      function ml(a) {
        this._internalRoot = a;
      }
      ml.prototype.unstable_scheduleHydration = function(a) {
        if (a) {
          var b = Hc();
          a = { blockedOn: null, target: a, priority: b };
          for (var c = 0; c < Qc.length && 0 !== b && b < Qc[c].priority; c++) ;
          Qc.splice(c, 0, a);
          0 === c && Vc(a);
        }
      };
      function nl(a) {
        return !(!a || 1 !== a.nodeType && 9 !== a.nodeType && 11 !== a.nodeType);
      }
      function ol(a) {
        return !(!a || 1 !== a.nodeType && 9 !== a.nodeType && 11 !== a.nodeType && (8 !== a.nodeType || " react-mount-point-unstable " !== a.nodeValue));
      }
      function pl() {
      }
      function ql(a, b, c, d, e) {
        if (e) {
          if ("function" === typeof d) {
            var f = d;
            d = function() {
              var a2 = gl(g);
              f.call(a2);
            };
          }
          var g = el(b, d, a, 0, null, false, false, "", pl);
          a._reactRootContainer = g;
          a[uf] = g.current;
          sf(8 === a.nodeType ? a.parentNode : a);
          Rk();
          return g;
        }
        for (; e = a.lastChild; ) a.removeChild(e);
        if ("function" === typeof d) {
          var h = d;
          d = function() {
            var a2 = gl(k);
            h.call(a2);
          };
        }
        var k = bl(a, 0, false, null, null, false, false, "", pl);
        a._reactRootContainer = k;
        a[uf] = k.current;
        sf(8 === a.nodeType ? a.parentNode : a);
        Rk(function() {
          fl(b, k, c, d);
        });
        return k;
      }
      function rl(a, b, c, d, e) {
        var f = c._reactRootContainer;
        if (f) {
          var g = f;
          if ("function" === typeof e) {
            var h = e;
            e = function() {
              var a2 = gl(g);
              h.call(a2);
            };
          }
          fl(b, g, a, e);
        } else g = ql(c, b, a, e, d);
        return gl(g);
      }
      Ec = function(a) {
        switch (a.tag) {
          case 3:
            var b = a.stateNode;
            if (b.current.memoizedState.isDehydrated) {
              var c = tc(b.pendingLanes);
              0 !== c && (Cc(b, c | 1), Dk(b, B()), 0 === (K & 6) && (Gj = B() + 500, jg()));
            }
            break;
          case 13:
            Rk(function() {
              var b2 = ih(a, 1);
              if (null !== b2) {
                var c2 = R();
                gi(b2, a, 1, c2);
              }
            }), il(a, 1);
        }
      };
      Fc = function(a) {
        if (13 === a.tag) {
          var b = ih(a, 134217728);
          if (null !== b) {
            var c = R();
            gi(b, a, 134217728, c);
          }
          il(a, 134217728);
        }
      };
      Gc = function(a) {
        if (13 === a.tag) {
          var b = yi(a), c = ih(a, b);
          if (null !== c) {
            var d = R();
            gi(c, a, b, d);
          }
          il(a, b);
        }
      };
      Hc = function() {
        return C;
      };
      Ic = function(a, b) {
        var c = C;
        try {
          return C = a, b();
        } finally {
          C = c;
        }
      };
      yb = function(a, b, c) {
        switch (b) {
          case "input":
            bb(a, c);
            b = c.name;
            if ("radio" === c.type && null != b) {
              for (c = a; c.parentNode; ) c = c.parentNode;
              c = c.querySelectorAll("input[name=" + JSON.stringify("" + b) + '][type="radio"]');
              for (b = 0; b < c.length; b++) {
                var d = c[b];
                if (d !== a && d.form === a.form) {
                  var e = Db(d);
                  if (!e) throw Error(p(90));
                  Wa(d);
                  bb(d, e);
                }
              }
            }
            break;
          case "textarea":
            ib(a, c);
            break;
          case "select":
            b = c.value, null != b && fb(a, !!c.multiple, b, false);
        }
      };
      Gb = Qk;
      Hb = Rk;
      var sl = { usingClientEntryPoint: false, Events: [Cb, ue, Db, Eb, Fb, Qk] };
      var tl = { findFiberByHostInstance: Wc, bundleType: 0, version: "18.3.1", rendererPackageName: "react-dom" };
      var ul = { bundleType: tl.bundleType, version: tl.version, rendererPackageName: tl.rendererPackageName, rendererConfig: tl.rendererConfig, overrideHookState: null, overrideHookStateDeletePath: null, overrideHookStateRenamePath: null, overrideProps: null, overridePropsDeletePath: null, overridePropsRenamePath: null, setErrorHandler: null, setSuspenseHandler: null, scheduleUpdate: null, currentDispatcherRef: ua.ReactCurrentDispatcher, findHostInstanceByFiber: function(a) {
        a = Zb(a);
        return null === a ? null : a.stateNode;
      }, findFiberByHostInstance: tl.findFiberByHostInstance || jl, findHostInstancesForRefresh: null, scheduleRefresh: null, scheduleRoot: null, setRefreshHandler: null, getCurrentFiber: null, reconcilerVersion: "18.3.1-next-f1338f8080-20240426" };
      if ("undefined" !== typeof __REACT_DEVTOOLS_GLOBAL_HOOK__) {
        vl = __REACT_DEVTOOLS_GLOBAL_HOOK__;
        if (!vl.isDisabled && vl.supportsFiber) try {
          kc = vl.inject(ul), lc = vl;
        } catch (a) {
        }
      }
      var vl;
      exports.__SECRET_INTERNALS_DO_NOT_USE_OR_YOU_WILL_BE_FIRED = sl;
      exports.createPortal = function(a, b) {
        var c = 2 < arguments.length && void 0 !== arguments[2] ? arguments[2] : null;
        if (!nl(b)) throw Error(p(200));
        return cl(a, b, null, c);
      };
      exports.createRoot = function(a, b) {
        if (!nl(a)) throw Error(p(299));
        var c = false, d = "", e = kl;
        null !== b && void 0 !== b && (true === b.unstable_strictMode && (c = true), void 0 !== b.identifierPrefix && (d = b.identifierPrefix), void 0 !== b.onRecoverableError && (e = b.onRecoverableError));
        b = bl(a, 1, false, null, null, c, false, d, e);
        a[uf] = b.current;
        sf(8 === a.nodeType ? a.parentNode : a);
        return new ll(b);
      };
      exports.findDOMNode = function(a) {
        if (null == a) return null;
        if (1 === a.nodeType) return a;
        var b = a._reactInternals;
        if (void 0 === b) {
          if ("function" === typeof a.render) throw Error(p(188));
          a = Object.keys(a).join(",");
          throw Error(p(268, a));
        }
        a = Zb(b);
        a = null === a ? null : a.stateNode;
        return a;
      };
      exports.flushSync = function(a) {
        return Rk(a);
      };
      exports.hydrate = function(a, b, c) {
        if (!ol(b)) throw Error(p(200));
        return rl(null, a, b, true, c);
      };
      exports.hydrateRoot = function(a, b, c) {
        if (!nl(a)) throw Error(p(405));
        var d = null != c && c.hydratedSources || null, e = false, f = "", g = kl;
        null !== c && void 0 !== c && (true === c.unstable_strictMode && (e = true), void 0 !== c.identifierPrefix && (f = c.identifierPrefix), void 0 !== c.onRecoverableError && (g = c.onRecoverableError));
        b = el(b, null, a, 1, null != c ? c : null, e, false, f, g);
        a[uf] = b.current;
        sf(a);
        if (d) for (a = 0; a < d.length; a++) c = d[a], e = c._getVersion, e = e(c._source), null == b.mutableSourceEagerHydrationData ? b.mutableSourceEagerHydrationData = [c, e] : b.mutableSourceEagerHydrationData.push(
          c,
          e
        );
        return new ml(b);
      };
      exports.render = function(a, b, c) {
        if (!ol(b)) throw Error(p(200));
        return rl(null, a, b, false, c);
      };
      exports.unmountComponentAtNode = function(a) {
        if (!ol(a)) throw Error(p(40));
        return a._reactRootContainer ? (Rk(function() {
          rl(null, null, a, false, function() {
            a._reactRootContainer = null;
            a[uf] = null;
          });
        }), true) : false;
      };
      exports.unstable_batchedUpdates = Qk;
      exports.unstable_renderSubtreeIntoContainer = function(a, b, c, d) {
        if (!ol(c)) throw Error(p(200));
        if (null == a || void 0 === a._reactInternals) throw Error(p(38));
        return rl(a, b, c, false, d);
      };
      exports.version = "18.3.1-next-f1338f8080-20240426";
    }
  });

  // node_modules/react-dom/index.js
  var require_react_dom = __commonJS({
    "node_modules/react-dom/index.js"(exports, module) {
      "use strict";
      function checkDCE() {
        if (typeof __REACT_DEVTOOLS_GLOBAL_HOOK__ === "undefined" || typeof __REACT_DEVTOOLS_GLOBAL_HOOK__.checkDCE !== "function") {
          return;
        }
        if (false) {
          throw new Error("^_^");
        }
        try {
          __REACT_DEVTOOLS_GLOBAL_HOOK__.checkDCE(checkDCE);
        } catch (err) {
          console.error(err);
        }
      }
      if (true) {
        checkDCE();
        module.exports = require_react_dom_production_min();
      } else {
        module.exports = null;
      }
    }
  });

  // node_modules/react-dom/client.js
  var require_client = __commonJS({
    "node_modules/react-dom/client.js"(exports) {
      "use strict";
      var m = require_react_dom();
      if (true) {
        exports.createRoot = m.createRoot;
        exports.hydrateRoot = m.hydrateRoot;
      } else {
        i = m.__SECRET_INTERNALS_DO_NOT_USE_OR_YOU_WILL_BE_FIRED;
        exports.createRoot = function(c, o) {
          i.usingClientEntryPoint = true;
          try {
            return m.createRoot(c, o);
          } finally {
            i.usingClientEntryPoint = false;
          }
        };
        exports.hydrateRoot = function(c, h, o) {
          i.usingClientEntryPoint = true;
          try {
            return m.hydrateRoot(c, h, o);
          } finally {
            i.usingClientEntryPoint = false;
          }
        };
      }
      var i;
    }
  });

  // entry.js
  var React3 = __toESM(require_react());
  var import_client = __toESM(require_client());

  // ../dsh-codearts-src/plugin-src/client/jet-hub.js
  var React2 = __toESM(require_react(), 1);

  // ../dsh-codearts-src/plugin-src/client/credits-capabilities.js
  var CREDITS_CAPABILITIES = Object.freeze({
    codearts: Object.freeze({ balance: true, dailyCheckin: true }),
    buddy: Object.freeze({ balance: true, dailyCheckin: true }),
    workbuddy: Object.freeze({ balance: true, dailyCheckin: false }),
    lobsterai: Object.freeze({ balance: true, dailyCheckin: true }),
    // Qoder：余额（`sash/api/v2/me/usage`）+ 每日领取
    // （`sash/api/v1/me/campaigns` → `POST …/{campaignId}/claim`，
    // 2026-09-21 由 keylog 解密抓包解出）。
    // 显式登记而非省略 —— 单测要求本表与 PROVIDERS 同步。
    qoder: Object.freeze({ balance: true, dailyCheckin: true }),
    // Qoder **中国版**（`qodercn`）：两项都有，与国际版同形。
    //
    // 依据（设计文档 E7/E10）：CN 的 `/sash/api/v2/me/usage` 与
    // `/sash/api/v1/me/campaigns` 零凭据实测返回 `401 {"code":"TOKEN_INVALID",
    // "message":"missing authorization token"}`，与国际版**逐字节同形**；
    // CN asar 里同样是 `Fh = Object.freeze({ clientType: 10, … })`，
    // 即桌面 app 身份这个值两站共用。
    //
    // ⚠️ 「端点存在」不等于「活动一定下发」—— 真实领取由
    // `pnpm test:e2e:qodercn-credits` 验证。若将来确认 CN 无签到，改这里时
    // 必须换成强证据（扫 CN asar 无 claim 端点），不要写「某次没看到」：
    // 国际版正是凭一次 `campaigns:[]` 误判成「无签到」，而真相是那天已领
    //（活动每日 10:00 UTC+8 刷新）。
    qodercn: Object.freeze({ balance: true, dailyCheckin: true }),
    // TRAE：余额与签到都有（`/trae/api/v2/pay/ide_user_ent_usage` +
    // `checkin_credits/status` → `checkin_credits/claim`，见 `src/trae-credits.ts`）。
    trae: Object.freeze({ balance: true, dailyCheckin: true }),
    // Cline：**只有余额**，没有签到。
    //
    // 余额：`GET /api/v1/users/{accountId}/balance`
    // （实测 `{data:{userId, balance:500000}, success:true}`，见 `src/cline-credits.ts`）。
    //
    // ⚠️ `dailyCheckin: false` 的依据是**对整个 sidecar 二进制做字符串扫描**：
    // `checkin` / `check-in` / `daily` / `campaign` 均无任何 Cline 业务端点命中
    // （`campaign` 的命中是 PostHog 的 UTM 参数与 feature-flag 事件属性；
    // `daily` 是 YAML cron 别名与 Blob 导出频率枚举）。
    // 这比「某次调用没看到」强，但仍不等于「永远不存在」—— 若将来 Cline 增加
    // 签到，需按 Qoder 那次教训重新采集（见 AGENTS.md 的对应章节）。
    //
    // `subscriptionQuota`：**订阅额度窗口 + 请求记录**（官方端点，见
    // `src/cline-quota.ts`）。这是**本表唯一**具备该项的渠道 —— 另外九家的
    // 订阅计量形状未知（多为按积分余额计费，没有「5 小时 / 周 / 月窗口」这一层），
    // 故不登记；未登记即不支持，面板也就不渲染按钮、不发请求。
    cline: Object.freeze({ balance: true, dailyCheckin: false, subscriptionQuota: true }),
    // Loomy（讯飞）：三项能力齐全，且是**唯一**有第三项（新手任务）的渠道。
    //
    // 余额：`GET /api/v1/points/records`（**只读**）—— 刻意不用 `first-login`，
    //   那是写端点，在面板挂载这种高频路径上调用会意外触发签到。
    // 每日签到：`POST /api/v1/points/first-login`。⚠️ 语义是「触发每日赠送额度」
    //   而不是「+5000 积分」：实测 `dailyBalance = dailyQuota - dailyConsumed`
    //   （4992 = 5000 - 8），消耗后不回补。
    // 新手任务：`GET/POST /api/v1/onboarding/tasks*`，8 个任务合计 **10000 分**，
    //   **一次性**（每号只能领一次），故必须与每日签到分开成一个独立按钮 ——
    //   混进「一键签到」会导致每天对已领完的账号发 8 个必然 alreadyCompleted 的请求。
    loomy: Object.freeze({ balance: true, dailyCheckin: true, onboardingTasks: true }),
    // Raccoon Work（商汤小浣熊）：余额 + **一次性**登录奖励。
    //
    // 余额：`GET /api/web/points/v1/balance`（**只读**，实测返回
    //   `{available_points, daily_points, reward_points, topup_points}`）。
    //
    // ⚠️ **不登记 `dailyCheckin`，且这不是遗漏** —— 实测「每日 300 积分」是
    //   **服务端按日自动发放**的（账单里 `biz_type: 'daily_grant'`，
    //   该账号 13:30 注册、13:31 即到账），**没有可调用的签到端点**。
    //   把它实现成签到按钮会让用户每次点击都必然失败 ——
    //   与 CodeArts 早期「对不支持的 provider 无条件发请求」是同一类缺陷。
    //
    // 登录奖励：`POST /api/web/desktop/v1/login/points/grant`，3000 分，
    //   **幂等一次性**（已领过返回 `granted:false` 且账单里能看到上一次记录）。
    //   语义与 Loomy 的新手任务同构，故登记为 `onboardingTasks` 而**不是**
    //   `dailyCheckin` —— 后者会让用户以为每天都真的加了额度。
    //   ⚠️ 该端点**需要** `X-Client-Platform` 头（值见 RaccoonProduct.clientPlatform）。
    raccoon: Object.freeze({ balance: true, onboardingTasks: true }),
    // MiniMax Code（中国版）：余额 + 每日签到**都有**（与 raccoon 不同）。
    //
    // 余额：`GET /minimax-cloud/api/v1/credit/details`（**只读**，实测返回
    //   `{total_count, base_resp}`；⚠️ **空明细时 `details` 字段整个缺失**，
    //   故解析必须容忍缺失 —— 见 `src/minimax-credits.ts` 的 `unwrapEnvelopeData`）。
    //   ⚠️ 该端点是**平铺响应**（`total_count` 与 `base_resp` 同级、没有 `data` 键），
    //   与签到端点的信封结构不同。
    //
    // 每日签到：`GET /minimax-cloud/api/v1/signin/status?timezone_id=<IANA>` +
    //   `POST …/signin/claim?timezone_id=<IANA>`（body `{}`）。
    //   ⚠️ **`timezone_id` 是 query 参数且必填** —— 实测放请求头会回
    //   `1406010011 invalid timezone_id`，且**那也是 HTTP 200**（只看状态码会误判成功）。
    //   ⚠️ **`points` 是总数，`bonus_points` 是其中的「额外」部分，不得相加**：
    //   实测第 1 天 `points: 800` / `bonus_points: 400`，截图按钮即「签到得 800」
    //   + 右上角「额外 400」角标。相加会虚高一倍（用户 2026-09-28 纠正）。
    //   ⚠️ 幂等判据是响应体的 `claim_result`（`1`=真领取、`2`=已领过），
    //   **不是 HTTP 状态码**（重复领取同样返回 200）。
    minimax: Object.freeze({ balance: true, dailyCheckin: true }),
    /**
     * ZCode（智谱）：余额与每日领取**都有**。
     *
     * - **余额**：`GET /api/v1/zcode-plan/billing/balance`
     *   （需 `Authorization: Bearer <zcodejwt>` + `X-Device-Mid`；实测返回
     *   `{total_units, used_units, remaining_units, period}`）。
     * - **每日领取**：`event/report`(补活跃信号) → `billing/preview` → `billing/claim`。
     *   ⚠️ 领取**需要阿里云 captcha**（由本插件的常驻 chromium 产出）。
     *
     * ⚠️ 这里如实登记为 `balance: true, dailyCheckin: true`，**尽管 ZCode 的
     * 额度单位是 token 而不是积分** —— 能力矩阵回答的是「有没有这项能力」，
     * 不是「量纲是否一致」。量纲差异在面板与 RPC 层如实标注（见
     * `src/jet-hub-rpc.ts` 里 zcode 的 balances 分支与
     * `src/zcode-auth.ts` 的 `claimDaily`）。
     */
    zcode: Object.freeze({ balance: true, dailyCheckin: true }),
    /**
     * AutoClaw（智谱系渠道，UTTS app 侧 13 渠道之一）：**只有余额**，没有签到。
     *
     * - **余额**：app 侧 `AutoclawChannel.queryCredits` 有实现（queryCredits 端点通）。
     * - **每日签到**：app 侧 `AutoclawChannel` **无 checkIn 实现** —— 渠道的
     *   checkIn 路径只会返回「该渠道无签到接口」。登记 `dailyCheckin: false`
     *   让面板不渲染签到按钮、不发必然失败的请求（与 cline 的登记理由同型）。
     * - **续期**：app 侧 refresh 有实现；续期能力不在本表登记（走
     *   `account.refresh` 端点的渠道级判断，app 侧不支持时桥会报错并显示原因）。
     *
     * ⚠️ app 侧 provider 同名 `autoclaw`（与插件 PROVIDERS 表的 id 一致，无需映射）。
     */
    autoclaw: Object.freeze({ balance: true, dailyCheckin: false }),
    /**
     * OpenCode：**显示**额度行，但语义不是「余额」而是「**通道可用性**」。
     *
     * ## 为什么不是 `balance: false`（2026-10-02 改，用户报障「只有 opencode 没有显示」）
     *
     * 我曾登记 `balance: false`，理由是「Zen 是按量计费的网关，没有可查询的
     * 余额数字」。但那个登记**把整个徽标挡死了** —— 组件第一件事就是
     * `supportsCreditBalance(provider)`，为 false 直接 `return null`，
     * 用户看到的就是「opencode 没有用量」，而 Zen 明明有额度（余额耗尽会回
     * `402 Insufficient account funds`）。
     *
     * ## 改后的口径
     *
     * Zen **没有公开的余额 API**（实测 15 个候选路径全 404，见
     * `docs/superpowers/specs/2026-10-02-opencode-zen-endpoint-matrix.md`），
     * 所以徽标展示**我们真正测得到的东西**：每个通道（账号槽 / 匿名通道）
     * 当前是否可用、是否处于限额冷却。数据来自本地 `modelRateLimits`，
     * **零网络请求**。宿主侧见 `jet-hub-rpc.ts` 的 `OPENCODE.id` 分支。
     *
     * ⚠️ 徽标会显示「N 通道」而非「N 积分」——这是**如实**的，不要改成
     * 假装有余额数字（那会在用户充值后显示错误的数字）。
     */
    opencode: Object.freeze({ balance: true, dailyCheckin: false }),
    /**
     * Gemini Code Assist（Google Cloud Code 免费线）：**只有余额**，没有签到。
     *
     * - **余额**：`POST /v1internal:retrieveUserQuotaSummary`（**只读**），
     *   返回 `{groups:[{displayName, buckets:[…]}]}`，我们只取两个桶：
     *   `gemini-5h`（5 小时窗口）与 `gemini-weekly`（周窗口）。
     *   ⚠️ 量纲是**剩余比例**（`remainingFraction`，0~1）而不是积分 ——
     *   面板按百分比展示，与 ZCode 的 token 量纲同理，能力矩阵只回答
     *   「有没有这项能力」，量纲差异在卡片上如实标注。
     *
     * - ⚠️ **不登记 `dailyCheckin`，且这不是遗漏** —— Cloud Code 免费线是
     *   纯配额制，**没有可调用的签到/领取端点**。登记它会让用户每次点击都
     *   必然失败（与 Raccoon 那条同因：签到是服务端按日自动发放的）。
     *
     * - ⚠️ 未授权时后端返回 `{balance: null, error: '尚未授权 Google 账号'}`，
     *   卡片显示原因而不是 0 —— 与「配额真的用完了」是**两回事**。
     */
    gemini: Object.freeze({ balance: true, dailyCheckin: false }),
    /**
     * 聚合 provider（跨渠道临期优先）。
     *
     * ⚠️ **如实登记为 `false`**：它**没有自己的账号与余额** —— 它复用各渠道的账号，
     * 积分与限流由各渠道自己管理（见 `aggregate-panel-logic.js` + `jet-hub.js` 的 `AggregatePanel` 的说明区）。
     *
     * ⚠️ 这一项**不影响**用量徽标能否工作：徽标对聚合的处理是「重定向到实际选中的
     * 渠道」（P3），重定向发生在 `supportsCreditBalance` 门控**之前**，门控看到的
     * `provider` 已是真实渠道。故这里填 `false` 是**诚实**的，不是「为了绕过门控」。
     *
     * ⚠️ 它也顺带保证聚合面板**不长出**「刷新积分 / 一键领取」按钮 ——
     * 那两处的门控（`canLoadCredits` / `supportsCredits`）读同一张表。
     */
    aggregate: Object.freeze({ balance: false, dailyCheckin: false })
  });
  var RATE_LIMIT_CAPABILITIES = Object.freeze({
    // Loomy（讯飞）：**不返回限流错误** —— 积分耗尽时静默降级为扣永久积分，
    // 故「重测 / 重置」这组按钮对它无意义（重测还会白烧积分）。
    loomy: Object.freeze({ rateLimit: false }),
    // Gemini（Google Cloud Code）：**理由与 Loomy 不同** —— 它确实会回 429，
    // `gemini-adapter.ts` 也确实会写 `modelRateLimits`（60s 冷却 / 401 时 300s）。
    // 但它的限流是**服务端配额窗口制**（5 小时窗口 + 周窗口），不是「等一会儿
    // 就好」的临时冷却：本地标记清掉、重测通过，配额本身一点没恢复，下一次
    // 请求立刻又是 429。于是这组按钮对 Gemini 只剩副作用：
    //   - 「重测」会对每个被标记的模型**真发一条消息**，白烧本就紧张的窗口配额，
    //     且结论恒为「仍然受限」（判据见 `src/account-probe.ts` 的 `retestAccount`）；
    //   - 「重置」只清本地标记，把账号重新放回可选用池，随即再撞 429 再写回来。
    // 真正能改变状态的动作是**换账号**或**等窗口 resetTime**，两者都不在这组按钮里。
    // 用户报障原文：「重测按钮你确认过会发请求吗，为什么响应这么快？可以移除吗」
    // （响应快是因为当时该账号没有 `modelRateLimits` 标记，`retestAccount` 在
    // `modelIds.length === 0` 处提前返回，一次请求都没发）。
    gemini: Object.freeze({ rateLimit: false })
  });
  function supportsRateLimit(provider) {
    return RATE_LIMIT_CAPABILITIES[provider]?.rateLimit !== false;
  }
  var ACCOUNT_TEST_CAPABILITIES = Object.freeze({
    gemini: Object.freeze({ test: true })
  });
  function supportsAccountTest(provider) {
    return ACCOUNT_TEST_CAPABILITIES[provider]?.test === true;
  }
  var PERMANENT_LOCK_EXPIRING_WINDOW_DAYS = 15;
  function supportsPermanentLock(provider) {
    return provider === "loomy" || provider === "buddy" || provider === "workbuddy" || provider === "trae" || provider === "lobsterai";
  }
  function supportsCreditPackageList(provider) {
    return provider === "buddy" || provider === "workbuddy" || provider === "lobsterai" || provider === "qoder" || provider === "qodercn" || provider === "trae";
  }
  function permanentLockCopy(provider, windowDays) {
    if (provider === "buddy" || provider === "workbuddy" || provider === "trae" || provider === "lobsterai") {
      const days = normalizeWindowDays(windowDays);
      return Object.freeze({
        days,
        lockTitle: `锁定永久积分后只消耗「${days} 天内到期」的积分包（那部分再不用就作废）。这类积分用尽后将没有可用账号。点此锁定。`,
        lockedTitle: `当前已锁定永久积分：只消耗「${days} 天内到期」的积分包。这类积分用尽后将没有可用账号。点此解锁。`,
        lockedNotice: `已锁定永久积分：只消耗 ${days} 天内到期的积分包。这类积分用尽后将无可用账号。`,
        unlockedNotice: `已解锁永久积分：${days} 天内到期的积分用尽后，会继续使用更晚到期的积分。`
      });
    }
    return Object.freeze({
      days: null,
      lockTitle: "锁定永久积分后只消耗每日赠送额度（今日额度用尽即无可用账号），可保住永久积分。点此锁定。",
      lockedTitle: "当前已锁定永久积分：只消耗每日赠送额度。今日额度用尽后将没有可用账号。点此解锁。",
      lockedNotice: "已锁定永久积分：只消耗每日赠送额度。今日额度用尽后将无可用账号。",
      unlockedNotice: "已解锁永久积分：今日额度用尽后会继续使用永久积分。"
    });
  }
  function normalizeWindowDays(value) {
    if (value === void 0 || value === null || value === "") {
      return PERMANENT_LOCK_EXPIRING_WINDOW_DAYS;
    }
    const parsed = Number(value);
    return Number.isFinite(parsed) && parsed >= 0 ? Math.round(parsed) : PERMANENT_LOCK_EXPIRING_WINDOW_DAYS;
  }
  function supportsCreditBalance(provider) {
    return CREDITS_CAPABILITIES[provider]?.balance === true;
  }
  function supportsDailyCheckin(provider) {
    return CREDITS_CAPABILITIES[provider]?.dailyCheckin === true;
  }
  function checkinProviders() {
    return Object.keys(CREDITS_CAPABILITIES).filter(supportsDailyCheckin);
  }
  function supportsOnboardingTasks(provider) {
    return CREDITS_CAPABILITIES[provider]?.onboardingTasks === true;
  }
  function supportsSubscriptionQuota(provider) {
    return CREDITS_CAPABILITIES[provider]?.subscriptionQuota === true;
  }

  // ../dsh-codearts-src/plugin-src/client/account-order.js
  function orderAfterDrop(ids, sourceId, targetId, position = "before") {
    const from = ids.indexOf(sourceId);
    const to = ids.indexOf(targetId);
    if (from === -1 || to === -1 || from === to) return null;
    const next = [...ids];
    next.splice(from, 1);
    const targetIndex = next.indexOf(targetId);
    next.splice(position === "after" ? targetIndex + 1 : targetIndex, 0, sourceId);
    return next;
  }
  function dropPositionFromPointer(clientY, rect) {
    if (!rect || !rect.height) return "before";
    return clientY > rect.top + rect.height / 2 ? "after" : "before";
  }

  // ../dsh-codearts-src/plugin-src/client/opencode-proxy-modal.js
  var React = __toESM(require_react(), 1);
  var LOCAL_PORT_PRESETS = [
    { port: 7897, label: "7897", url: "http://127.0.0.1:7897", hint: "Clash / mihomo 混合端口" },
    { port: 7890, label: "7890", url: "http://127.0.0.1:7890", hint: "Clash 旧版 HTTP 端口" },
    { port: 1080, label: "1080", url: "socks5://127.0.0.1:1080", hint: "SOCKS5" },
    { port: 10808, label: "10808", url: "socks5://127.0.0.1:10808", hint: "SOCKS5（v2rayN 等）" }
  ];
  function guessMode(url) {
    if (!url) return "local";
    if (url.startsWith("socks5")) return "socks5";
    if (url.includes("127.0.0.1") || url.includes("localhost")) return "local";
    return "http";
  }
  var MODES = [
    { id: "local", label: "本地代理端口", hint: "本机已跑着 Clash / v2rayN 之类，直接填它的端口" },
    { id: "http", label: "HTTP(S)", hint: "形如 http://user:pass@host:port" },
    { id: "socks5", label: "SOCKS5", hint: "形如 socks5://user:pass@host:port" }
  ];
  function OpencodeProxyModal({ ctx, accountId, current, onClose }) {
    const [mode, setMode] = React.useState(guessMode(current));
    const [url, setUrl] = React.useState(current || "");
    const [busy, setBusy] = React.useState(false);
    const [testing, setTesting] = React.useState(false);
    const [error, setError] = React.useState("");
    const [result, setResult] = React.useState(null);
    const close = () => {
      if (onClose) onClose();
    };
    const save = async (value) => {
      setBusy(true);
      setError("");
      try {
        await ctx.rpc({ method: "opencode.setProxy", payload: { accountId, proxy: value } });
        close();
      } catch (err) {
        setError(err && err.message ? err.message : String(err));
      } finally {
        setBusy(false);
      }
    };
    const test = async () => {
      setTesting(true);
      setError("");
      setResult(null);
      try {
        const r = await ctx.rpc({ method: "opencode.testProxy", payload: { proxy: url } });
        setResult(r);
      } catch (err) {
        setError(err && err.message ? err.message : String(err));
      } finally {
        setTesting(false);
      }
    };
    const activeHint = (MODES.find((m) => m.id === mode) || {}).hint || "";
    return React.createElement(
      "div",
      {
        className: "dim-jh-modalOverlay dim-jh-modalOverlay--top",
        onClick: (e) => {
          if (e.target === e.currentTarget) close();
        }
      },
      React.createElement(
        "div",
        {
          className: "dim-jh-modal",
          role: "dialog",
          "aria-modal": "true",
          style: { maxWidth: "520px" }
        },
        React.createElement(
          "div",
          { className: "dim-jh-modalHead" },
          React.createElement("div", { className: "dim-jh-modalTitle" }, "设置出口代理"),
          React.createElement(
            "span",
            { className: "dim-jh-modalSubtitle" },
            current ? "当前：已配置" : "当前：直连（本机出口）"
          )
        ),
        // ⚠️ 这段提示是本功能的存在理由，必须说清「多账号 ≠ 多配额」。
        React.createElement(
          "p",
          { className: "dim-jh-modalHint" },
          "OpenCode 的免费通道按出口 IP 限流。不设代理时，本账号与其它未设代理的账号（以及匿名通道）共用同一个出口，也就是共用同一份额度；设置代理后该账号走独立出口。"
        ),
        // ⚠️ 内容必须放进 modalBody（flex:1; min-height:0; overflow-y:auto），
        // 否则弹窗较高时内容会被裁掉（见 jet-hub.js 既有 modal 的注释）。
        React.createElement(
          "div",
          { className: "dim-jh-modalBody" },
          React.createElement(
            "div",
            { className: "dim-jh-fieldRow" },
            MODES.map((m) => React.createElement(
              "label",
              { key: m.id, className: "dim-jh-radio" },
              React.createElement("input", {
                type: "radio",
                name: "opencode-proxy-mode",
                checked: mode === m.id,
                onChange: () => {
                  setMode(m.id);
                  setError("");
                  setResult(null);
                }
              }),
              m.label
            ))
          ),
          React.createElement("p", { className: "dim-jh-hint" }, activeHint),
          mode === "local" && React.createElement(
            "div",
            { className: "dim-jh-presetRow" },
            LOCAL_PORT_PRESETS.map((p) => React.createElement("button", {
              key: p.port,
              type: "button",
              className: "dim-jh-btn",
              title: p.hint,
              onClick: () => {
                setUrl(p.url);
                setError("");
                setResult(null);
              }
            }, p.label))
          ),
          React.createElement("input", {
            className: "dim-jh-input",
            placeholder: mode === "socks5" ? "socks5://user:pass@host:port" : "http://user:pass@host:port",
            value: url,
            // ⚠️ 含认证信息时用 password 类型：代理口令不该裸显在屏幕上。
            type: url.includes("@") ? "password" : "text",
            onChange: (e) => {
              setUrl(e.target.value);
              setResult(null);
            }
          }),
          result && React.createElement(
            "p",
            { className: "dim-jh-hint" },
            `出口 IP ${result.exitIp}（${result.country || "未知地区"}）· ${result.latencyMs}ms`
          ),
          error && React.createElement("p", { className: "dim-jh-error" }, error)
        ),
        React.createElement(
          "div",
          { className: "dim-jh-modalActions" },
          React.createElement("button", {
            type: "button",
            className: "dim-jh-btn",
            disabled: testing || busy || url.trim().length === 0,
            onClick: test
          }, testing ? "测试中…" : "测试连接"),
          // 只有已配置过才显示「清除」：没配过就没有可清除的东西。
          current ? React.createElement("button", {
            type: "button",
            className: "dim-jh-btn",
            disabled: busy,
            onClick: () => save("")
          }, "清除代理") : null,
          React.createElement("button", {
            type: "button",
            className: "dim-jh-btn dim-jh-btnPrimary",
            disabled: busy || url.trim().length === 0,
            onClick: () => save(url)
          }, "保存"),
          React.createElement("button", { type: "button", className: "dim-jh-btn", onClick: close }, "取消")
        )
      )
    );
  }
  function OpencodeKeyModal({ error, busy, inputRef, onSubmit, onSubmitAnonymous, onClose }) {
    const [mode, setMode] = React.useState("key");
    const submit = () => {
      if (mode === "anonymous") {
        onSubmitAnonymous();
        return;
      }
      const value = inputRef && inputRef.current ? inputRef.current.value : "";
      if (String(value).trim() === "") return;
      onSubmit(value);
    };
    return React.createElement(
      "div",
      {
        className: "dim-jh-modalOverlay dim-jh-modalOverlay--top",
        onClick: (e) => {
          if (e.target === e.currentTarget && !busy && onClose) onClose();
        }
      },
      React.createElement(
        "div",
        {
          className: "dim-jh-modal",
          role: "dialog",
          "aria-modal": "true",
          style: { maxWidth: "520px" }
        },
        React.createElement(
          "div",
          { className: "dim-jh-modalHead" },
          React.createElement("div", { className: "dim-jh-modalTitle" }, "添加 OpenCode 账号")
        ),
        // 两种身份：API key 账号 / 匿名通道（无需凭据）。
        React.createElement(
          "div",
          { className: "dim-jh-fieldRow" },
          [
            { id: "key", label: "API key 账号" },
            { id: "anonymous", label: "匿名通道" }
          ].map((m) => React.createElement(
            "label",
            { key: m.id, className: "dim-jh-radio" },
            React.createElement("input", {
              type: "radio",
              name: "opencode-add-mode",
              checked: mode === m.id,
              onChange: () => setMode(m.id)
            }),
            m.label
          ))
        ),
        mode === "key" ? React.createElement(
          React.Fragment,
          null,
          React.createElement(
            "p",
            { className: "dim-jh-modalHint" },
            "在 opencode.ai/auth 生成 API key（形如 sk-…）后粘贴到下方。可启用付费模型，并为每个账号单独设置出口代理。"
          ),
          // ⚠️ 内容必须在 modalBody 里（flex:1; min-height:0; overflow-y:auto），
          // 否则弹窗内容会被裁掉（见 jet-hub.js 既有 modal 的注释）。
          React.createElement(
            "div",
            { className: "dim-jh-modalBody" },
            React.createElement(
              "div",
              { className: "dim-jh-formRows" },
              React.createElement("input", {
                ref: inputRef,
                className: "dim-jh-input",
                // ⚠️ password 类型：API key 是凭据，不该在屏幕上裸显。
                type: "password",
                placeholder: "sk-…",
                autoFocus: true,
                // Enter 直接提交：粘贴 key 后最自然的动作就是回车。
                onKeyDown: (e) => {
                  if (e.key === "Enter") {
                    e.preventDefault();
                    submit();
                  }
                }
              })
            ),
            // ⚠️ 错误留在弹窗内：把整个账号列表切成错误态会让用户刚填的 key
            // 与错误信息一起消失，只能刷新重试。
            error ? React.createElement("p", { className: "dim-jh-error", role: "alert" }, "添加失败：" + error) : null
          )
        ) : React.createElement(
          "div",
          { className: "dim-jh-modalBody" },
          React.createElement(
            "p",
            { className: "dim-jh-modalHint" },
            "匿名通道无需任何凭据（上游认字面量 public），用于免费模型。可以添加多条，每条可单独设置出口代理。"
          ),
          // ⚠️⚠️ **诚实说明指纹不增加配额**：匿名通道按出口 IP 限额
          // （实测：换 key、换伪装头、换指纹全部无效）。要多份额度只能给
          // 不同匿名通道配**不同代理**；指纹分离的价值是防关联。
          // 不说清楚的话，用户加 5 条匿名通道却只看到一份额度，会以为坏了。
          React.createElement(
            "p",
            { className: "dim-jh-hint" },
            "注意：匿名通道的额度按「出口 IP」计算。多条匿名通道若共用同一个出口，额度不会增加；给它们分别配置不同代理，才会各自获得独立额度。"
          ),
          error ? React.createElement("p", { className: "dim-jh-error", role: "alert" }, "添加失败：" + error) : null
        ),
        React.createElement(
          "div",
          { className: "dim-jh-modalActions" },
          React.createElement("button", {
            type: "button",
            className: "dim-jh-btn dim-jh-btnPrimary",
            disabled: busy,
            onClick: submit
          }, busy ? "添加中…" : "添加"),
          React.createElement("button", {
            type: "button",
            className: "dim-jh-btn",
            disabled: busy,
            onClick: () => {
              if (onClose) onClose();
            }
          }, "取消")
        )
      )
    );
  }

  // ../dsh-codearts-src/plugin-src/client/credit-expiry.js
  var DAY_MS = 24 * 60 * 60 * 1e3;
  function normalizeWindowDays2(windowDays) {
    if (windowDays === void 0 || windowDays === null || windowDays === "") return null;
    const parsed = Number(windowDays);
    return Number.isFinite(parsed) && parsed >= 0 ? parsed : null;
  }
  function daysUntilExpiry(pkg, now) {
    const end = pkg && typeof pkg.deductionEndTime === "number" ? pkg.deductionEndTime : null;
    if (end === null || !Number.isFinite(end) || end <= 0) return null;
    const at = typeof now === "number" && Number.isFinite(now) ? now : Date.now();
    return (end - at) / DAY_MS;
  }
  function splitCreditsByExpiry(packages, windowDays, now) {
    const days = normalizeWindowDays2(windowDays);
    if (days === null || !Array.isArray(packages)) return null;
    const windowMs = days * DAY_MS;
    const at = typeof now === "number" && Number.isFinite(now) ? now : Date.now();
    let expiring = 0;
    let permanent = 0;
    for (const pkg of packages) {
      if (!pkg || pkg.active !== true) continue;
      const remaining = Number(pkg.remaining);
      if (!Number.isFinite(remaining) || remaining <= 0) continue;
      const end = Number(pkg.deductionEndTime);
      const known = Number.isFinite(end) && end > 0;
      if (known && end - at < windowMs) expiring += remaining;
      else permanent += remaining;
    }
    return { expiring, permanent };
  }
  function expiryBucketLabel(pkg, windowDays, now) {
    const days = normalizeWindowDays2(windowDays);
    const left = daysUntilExpiry(pkg, now);
    if (left === null) return "到期时间未知";
    if (days === null) return null;
    return left < days ? `${Math.ceil(left)} 天内到期` : `还有 ${Math.ceil(left)} 天`;
  }
  function formatExpirySplitLine(split, format) {
    if (!split) return null;
    const expiring = format(split.expiring);
    const permanent = format(split.permanent);
    if (expiring === null || permanent === null) return null;
    return `长期 ${permanent} · 临时 ${expiring}`;
  }
  var DAILY_POOL_NAMES = ["每日赠送", "每日积分"];
  function findDailyPool(packages) {
    if (!Array.isArray(packages)) return null;
    return packages.find((pkg) => pkg && DAILY_POOL_NAMES.includes(pkg.name)) ?? null;
  }
  function formatPoolSplitLine(packages, format, longTermLabel = "长期") {
    const daily = findDailyPool(packages);
    if (daily === null) return null;
    const restSum = packages.reduce((sum, pkg) => {
      if (!pkg || pkg === daily) return sum;
      if (pkg.active !== true) return sum;
      const remaining = Number(pkg.remaining);
      return Number.isFinite(remaining) && remaining > 0 ? sum + remaining : sum;
    }, 0);
    const dailyValue = format(Number(daily.remaining) || 0);
    const restValue = format(restSum);
    if (dailyValue === null || restValue === null) return null;
    return `${longTermLabel} ${restValue} · 每日 ${dailyValue}`;
  }
  function packageExpiryMs(pkg) {
    const dedEnd = Number(pkg && pkg.deductionEndTime);
    if (Number.isFinite(dedEnd) && dedEnd > 0) return dedEnd;
    const exp = pkg && pkg.expiredTime ? String(pkg.expiredTime) : "";
    if (exp.length > 0) {
      const ms = Date.parse(exp.replace(" ", "T"));
      if (Number.isFinite(ms) && ms > 0) return ms;
    }
    return null;
  }
  function formatPackageExpiry(pkg, now) {
    const end = packageExpiryMs(pkg);
    if (end === null) return "长期";
    const at = Number.isFinite(now) ? now : Date.now();
    const date = new Date(end).toISOString().slice(0, 10);
    const days = Math.ceil((end - at) / DAY_MS);
    if (days <= 0) return `${date}（已过期）`;
    return `${date}（${days} 天后）`;
  }
  function formatPackageTooltip(packages, options = {}) {
    const { format, now = Date.now(), maxRows = 12 } = options;
    if (!Array.isArray(packages) || packages.length === 0) return null;
    const at = Number.isFinite(now) ? now : Date.now();
    const usable = packages.filter((pkg) => {
      if (!pkg || pkg.active === false) return false;
      const remaining = Number(pkg.remaining);
      if (!Number.isFinite(remaining) || remaining <= 0) return false;
      const end = packageExpiryMs(pkg);
      if (end !== null && end <= at) return false;
      return true;
    });
    if (usable.length === 0) return null;
    const sorted = [...usable].sort((a, b) => {
      const endA = packageExpiryMs(a);
      const endB = packageExpiryMs(b);
      const keyA = endA === null ? Infinity : endA;
      const keyB = endB === null ? Infinity : endB;
      if (keyA !== keyB) return keyA - keyB;
      return (Number(b && b.remaining) || 0) - (Number(a && a.remaining) || 0);
    });
    const lines = sorted.slice(0, maxRows).map((pkg) => {
      const name = pkg && pkg.name || "未命名";
      const remaining = format ? format(Number(pkg && pkg.remaining) || 0) : String(pkg && pkg.remaining);
      const total = format ? format(Number(pkg && pkg.total) || 0) : String(pkg && pkg.total);
      return `${name}  ${remaining} / ${total}  ${formatPackageExpiry(pkg, at)}`;
    });
    const rest = sorted.slice(maxRows);
    if (rest.length > 0) {
      const sum = rest.reduce((acc, p) => acc + (Number(p && p.remaining) || 0), 0);
      lines.push(`…另有 ${rest.length} 个包${sum > 0 ? `，合计剩余 ${format ? format(sum) : sum}` : ""}`);
    }
    return lines.join("\n");
  }

  // ../dsh-codearts-src/plugin-src/client/account-model-link.js
  function disablingLeavesNoEnabledAccount(accounts, accountId, provider) {
    const list = Array.isArray(accounts) ? accounts : [];
    const target = list.find((a) => a?.id === accountId);
    if (target === void 0 || target.enabled === false) return false;
    const stillEnabled = list.some(
      (a) => a?.provider === provider && a?.id !== accountId && a?.enabled !== false
    );
    return !stillEnabled;
  }
  function allModelsDisabled(models) {
    const list = Array.isArray(models) ? models : [];
    if (list.length === 0) return false;
    return list.every((m) => m?.disabled === true);
  }

  // ../dsh-codearts-src/plugin-src/client/new-account.js
  function newAccountAsksChannel(provider) {
    return provider === "zcode";
  }
  function buildCreateAccountPayload(provider, zcodeProvider) {
    if (provider !== "zcode") return { provider };
    return { provider, zcodeProvider };
  }

  // ../dsh-codearts-src/plugin-src/client/model-bulk.js
  function bulkButtonState(models, busy) {
    if (busy || !models || models.length === 0) {
      return { openAllDisabled: true, closeAllDisabled: true };
    }
    const anyDisabled = models.some((m) => m.disabled);
    const anyEnabled = models.some((m) => !m.disabled);
    return { openAllDisabled: !anyDisabled, closeAllDisabled: !anyEnabled };
  }

  // ../dsh-codearts-src/plugin-src/client/model-filter.js
  var MODEL_STATUS_FILTERS = Object.freeze(["all", "enabled", "disabled"]);
  function normalizeStatusFilter(status) {
    return MODEL_STATUS_FILTERS.includes(status) ? status : "all";
  }
  function matchesModelQuery(model, query) {
    const needle = typeof query === "string" ? query.trim().toLowerCase() : "";
    if (needle.length === 0) return true;
    const name = typeof model?.name === "string" ? model.name : "";
    const id = typeof model?.id === "string" ? model.id : "";
    return name.toLowerCase().includes(needle) || id.toLowerCase().includes(needle);
  }
  function filterModels(models, options = {}) {
    const list = Array.isArray(models) ? models : [];
    const status = normalizeStatusFilter(options.status);
    const query = typeof options.query === "string" ? options.query : "";
    return list.filter((model) => {
      if (status === "enabled" && model?.disabled === true) return false;
      if (status === "disabled" && model?.disabled !== true) return false;
      return matchesModelQuery(model, query);
    });
  }
  function isFilterActive(options = {}) {
    const query = typeof options.query === "string" ? options.query.trim() : "";
    return query.length > 0 || normalizeStatusFilter(options.status) !== "all";
  }

  // ../dsh-codearts-src/plugin-src/client/openai-gateway-panel.js
  async function copyToClipboard(value) {
    try {
      if (typeof navigator === "undefined" || !navigator.clipboard?.writeText) return false;
      await navigator.clipboard.writeText(value);
      return true;
    } catch {
      return false;
    }
  }
  function gatewayApiKeyHint(apiKey) {
    if (!apiKey) return "网关尚未生成过密钥：启用一次网关即会自动生成。";
    if (apiKey.fromEnv) return "密钥来自环境变量 DSH_OPENAI_GATEWAY_API_KEY（不在文件里）。";
    return apiKey.path ? `密钥文件：${apiKey.path}` : "密钥来自环境变量。";
  }
  function gatewayButtonLabel(status) {
    if (!status) return "网关";
    return status.running ? "网关 ●" : "网关";
  }
  function modelSupportsImage(model) {
    return Array.isArray(model?.input) && model.input.includes("image");
  }
  function modelCapabilityBadge(model) {
    return modelSupportsImage(model) ? "可发图片" : "";
  }
  function formatModelIdList(models) {
    if (!models || models.length === 0) return "";
    return models.map((model) => model.id).join("\n");
  }
  function gatewayProviderOf(entry) {
    const provider = entry?.provider;
    if (typeof provider === "string" && provider !== "") return provider;
    const id = typeof entry?.id === "string" ? entry.id : "";
    const slash = id.indexOf("/");
    return slash > 0 ? id.slice(0, slash) : "";
  }
  function gatewayModelKeyOf(entry) {
    const inner = entry?.model;
    if (typeof inner === "string" && inner !== "") return inner;
    const id = typeof entry?.id === "string" ? entry.id : "";
    const provider = gatewayProviderOf(entry);
    if (provider !== "" && id.startsWith(provider + "/")) return id.slice(provider.length + 1);
    return id;
  }
  var GATEWAY_CARD_AUTOCOLLAPSE_AT = 24;
  function groupGatewayEntries(entries, countFold) {
    const list = Array.isArray(entries) ? entries : [];
    const byProvider = /* @__PURE__ */ new Map();
    for (const entry of list) {
      const provider = gatewayProviderOf(entry);
      let bucket = byProvider.get(provider);
      if (bucket === void 0) {
        bucket = [];
        byProvider.set(provider, bucket);
      }
      bucket.push(entry);
    }
    return [...byProvider].map(([provider, bucket]) => ({
      provider,
      entries: bucket,
      counts: {
        total: bucket.length,
        fold: typeof countFold === "function" ? bucket.filter(countFold).length : 0
      }
    }));
  }
  function groupGatewayModels(models) {
    return groupGatewayEntries(models, (model) => modelSupportsImage(model));
  }
  function gatewayCardExpanded(card, options = {}) {
    const toggled = options.toggled;
    if (toggled === true || toggled === false) return toggled;
    if (options.searching === true) return true;
    return (card?.counts?.total ?? 0) <= GATEWAY_CARD_AUTOCOLLAPSE_AT;
  }
  function gatewayCardLabel(provider, labelOf) {
    if (provider === "") return "（未标注供应商）";
    if (typeof labelOf === "function") {
      const label = labelOf(provider);
      if (typeof label === "string" && label !== "") return label;
    }
    return provider;
  }
  function gatewayModelsHint(models, source) {
    if (!models || models.length === 0) {
      if (source === "none") {
        return "拿不到任何 provider 列表（不是登录问题）：通常是 DSH 侧模型服务未就绪，重启 DSH 或点「刷新」再试。";
      }
      return "还没有可用模型：登录至少一个供应商后点「刷新」。";
    }
    const providers = new Set(models.map((model) => gatewayProviderOf(model))).size;
    const base = `共开启 ${models.length} 个可用模型，来自 ${providers} 个供应商，ID 区分大小写。`;
    return source === "adapters" ? base + "（目录来自本插件已注册的适配器，可能不含 DSH 自带的模型）" : base;
  }
  function gatewayModelsEmptyHint(query) {
    const needle = typeof query === "string" ? query.trim() : "";
    if (needle.length === 0) return "当前没有可用模型。";
    return `没有匹配「${needle}」的模型。搜索会匹配模型 ID 与展示名（不区分大小写）。`;
  }
  function gatewayEffortRow(model) {
    const reasoning = model?.reasoning;
    const efforts = reasoning?.efforts;
    if (!Array.isArray(efforts) || efforts.length === 0) return null;
    return {
      id: model.id,
      // ⚠️ 带上权威供应商与内部模型名：对照表也要按供应商折成卡片，而行内只显示
      // **去掉前缀**的名字（与模型清单同一取舍，见 `gatewayModelKeyOf`）。
      // 复制出去的仍是完整 `id`（客户端配置要的是它）。
      provider: gatewayProviderOf(model),
      model: gatewayModelKeyOf(model),
      // 展示名留给搜索用（与模型清单的搜索共用 matchesModelQuery）。
      name: typeof model.name === "string" ? model.name : "",
      // 两个名字都给出来：`id` 是网关实际下发的 wire 值，`name` 是 DSH 界面上的叫法
      // （用户就是照它填错的，故两列都得能对上）。
      declared: efforts.map((effort) => `${effort.id}（${effort.name}）`).join(" · "),
      fill: Array.isArray(reasoning.openai_efforts) ? reasoning.openai_efforts.join(", ") : "",
      // 「真实 id 与应填的规范名不完全一致」= 客户端侧的显示名会与 DSH 界面不同，
      // 这类行才是用户真正会看错的，值得在 UI 上标出来。
      //
      // ⚠️ **`canonical === undefined`（上游给了个没登记的私有 id，如 `turbo`）也算**：
      // 那种档位**任何规范名都表达不出来**，`fill` 列会比 `declared` 列少一档。
      // 原判据只认「canonical 与 id 不同」，于是这种行恰恰**不标徽章** ——
      // 用户看到一档填不出来的东西，却没有任何解释。
      lossy: efforts.some((effort) => effort.canonical === void 0 || effort.canonical !== effort.id)
    };
  }
  function gatewayEffortRows(models) {
    if (!Array.isArray(models)) return [];
    return models.map(gatewayEffortRow).filter(Boolean);
  }
  function matchesEffortQuery(row, query) {
    if (matchesModelQuery(row, query)) return true;
    const needle = typeof query === "string" ? query.trim().toLowerCase() : "";
    if (needle.length === 0) return true;
    return `${row?.declared ?? ""} ${row?.fill ?? ""}`.toLowerCase().includes(needle);
  }
  function filterEffortRows(rows, query) {
    const list = Array.isArray(rows) ? rows : [];
    if (!isFilterActive({ query })) return list;
    return list.filter((row) => matchesEffortQuery(row, query));
  }
  function gatewayEffortsHintLines(models) {
    const rows = gatewayEffortRows(models);
    if (rows.length === 0) {
      return ["当前已开启的模型都没声明思考档位：网关不下发该参数，按模型默认走。"];
    }
    const lossy = rows.filter((row) => row.lossy).length;
    const enabled = Array.isArray(models) ? models.length : rows.length;
    return [
      `已开启的 ${enabled} 个模型里，有 ${rows.length} 个可选思考档位${lossy > 0 ? `（${lossy} 个不同名，标「需对照」）` : ""}。`,
      "「客户端该填」为 CC Switch 需配置的映射档位，未登记的档位名称将被拒绝。"
    ];
  }
  function gatewayEffortsEmptyHint(query) {
    const needle = typeof query === "string" ? query.trim() : "";
    if (needle.length === 0) return "当前没有模型声明思考档位。";
    return `没有匹配「${needle}」的模型。搜索会匹配模型 id / 展示名，也会匹配档位名（真实 id 与 DSH 界面上的叫法都算，例如 xhigh / Max / Extra）。`;
  }
  function gatewayEffortsText(rows) {
    const list = Array.isArray(rows) ? rows : [];
    return list.map((row) => [
      row.id,
      `  真实档位：${row.declared}`,
      `  客户端该填：${row.fill}`
    ].join("\n")).join("\n\n");
  }
  function gatewayButtonTitle(status) {
    if (!status) return "本机 OpenAI 网关：读取状态中。";
    if (status.blockedByEnv) {
      return "本机 OpenAI 网关：已被环境变量 DSH_OPENAI_GATEWAY_ENABLED 停用，在这里改开关不会让它监听端口。";
    }
    if (status.running) return `本机 OpenAI 网关：运行中（${gatewayEndpoint(status)}）。`;
    if (status.enabled) return "本机 OpenAI 网关：已选择开启，但当前未在监听（通常是端口被占用）。";
    return "本机 OpenAI 网关：已关闭。";
  }
  function gatewayEndpoint(status) {
    const address = status?.address;
    if (!address) return "";
    return `http://${address.host}:${address.port}/v1`;
  }
  function gatewayStatusLines(status) {
    if (!status) return ["正在读取网关状态…"];
    if (status.blockedByEnv) {
      return [
        "已被环境变量 DSH_OPENAI_GATEWAY_ENABLED 停用，网关不会监听端口。",
        "在下面的开关里做出的选择会被记住，但需要先取消该环境变量才会生效。"
      ];
    }
    if (status.running) {
      const endpoint = gatewayEndpoint(status);
      return [
        // ⚠️ 用户 2026-10-04 要求精简（原三句「网关正在运行 / 把外部客户端的… /
        // 地址：…」合并成下面这组短行）。**语义必须保住两条**：
        //   1. 推荐 CC Switch —— 它是本仓库对 Codex/Cline 那类客户端的既有推荐入口；
        //   2. **两种协议都要点名**：Responses API 是后加的，不说就没人知道它存在。
        // ⚠️ 措辞不得写成「可切换」：两个端点同时都在，没有互斥开关（用例会红）。
        "推荐使用 CC Switch 进行网关配置。",
        "协议支持：OpenAI Chat Completions 与 OpenAI Responses API。",
        // 地址单列一行（带端口），它是用户要抄走的东西。
        endpoint ? `API 请求地址：${endpoint}` : ""
      ].filter(Boolean);
    }
    if (status.enabled) {
      return [
        "已选择开启，但网关当前没有在监听。",
        "最常见的原因是端口被其它程序占用 —— 换 DSH_OPENAI_GATEWAY_PORT 后重启即可。"
      ];
    }
    return ["网关已关闭，不会监听任何端口。"];
  }
  function gatewaySwitchDisabled(status) {
    if (!status) return true;
    return status.blockedByEnv;
  }
  function gatewayToggleNotice(status, nextEnabled) {
    if (status?.blockedByEnv) {
      return "选择已保存，但 DSH_OPENAI_GATEWAY_ENABLED 仍在停用网关，取消它才会生效。";
    }
    if (!nextEnabled) return "网关已关闭，不再监听端口。";
    if (status?.running) return `网关已启动：${gatewayEndpoint(status)}`;
    return "已选择开启，但网关没有在监听，请检查端口是否被占用。";
  }

  // ../dsh-codearts-src/plugin-src/client/aggregate-panel-logic.js
  function sortModelsForPanel(models) {
    if (!Array.isArray(models)) return [];
    return [...models].sort((a, b) => {
      const byCount = (b?.candidates?.length ?? 0) - (a?.candidates?.length ?? 0);
      if (byCount !== 0) return byCount;
      return String(a?.canonicalId ?? "").localeCompare(String(b?.canonicalId ?? ""));
    });
  }
  function isAllCandidatesRejected(model, rejections) {
    if (typeof model !== "object" || model === null) return false;
    const candidates = model.candidates;
    if (!Array.isArray(candidates) || candidates.length === 0) return false;
    return candidates.every((candidate) => isRejected(
      rejections,
      model.canonicalId,
      candidate?.provider,
      candidate?.realId
    ));
  }
  function rejectionsFromCatalog(models) {
    const out = {};
    if (!Array.isArray(models)) return out;
    for (const model of models) {
      const canonicalId = model?.canonicalId;
      if (typeof canonicalId !== "string" || canonicalId.length === 0) continue;
      if (!Array.isArray(model.candidates)) continue;
      for (const candidate of model.candidates) {
        if (candidate?.rejected !== true) continue;
        const provider = candidate.provider;
        const realId = candidate.realId;
        if (typeof provider !== "string" || provider.length === 0) continue;
        if (typeof realId !== "string" || realId.length === 0) continue;
        if (!Object.hasOwn(out, canonicalId)) out[canonicalId] = {};
        if (!Object.hasOwn(out[canonicalId], provider)) out[canonicalId][provider] = {};
        out[canonicalId][provider][realId] = true;
      }
    }
    return out;
  }
  function sortCandidatesByExpiry(rows) {
    if (!Array.isArray(rows)) return [];
    const of = (row) => {
      const value = row?.expiry;
      return typeof value === "number" && Number.isFinite(value) ? value : Number.POSITIVE_INFINITY;
    };
    return [...rows].sort((a, b) => {
      const byExpiry = of(a) - of(b);
      if (byExpiry !== 0) return byExpiry;
      return String(a?.provider ?? "").localeCompare(String(b?.provider ?? ""));
    });
  }
  function expiryLabel(expiry, nowMs = Date.now()) {
    if (typeof expiry !== "number" || Number.isNaN(expiry)) return "?";
    if (expiry === -1) return "查不到";
    if (!Number.isFinite(expiry)) return "长期";
    if (expiry <= 0) return "?";
    const diff = expiry - nowMs;
    if (diff <= 0) return "已过期";
    const minutes = Math.floor(diff / 6e4);
    if (minutes < 1) return "不到 1 分钟";
    if (minutes < 60) return `${minutes} 分钟`;
    const hours = Math.floor(minutes / 60);
    if (hours < 24) return `${hours} 小时`;
    return `${Math.floor(hours / 24)} 天`;
  }
  function candidateRowLabel(candidate) {
    const provider = typeof candidate?.provider === "string" ? candidate.provider : "?";
    const realId = typeof candidate?.realId === "string" ? candidate.realId : "?";
    const parts = [provider, realId];
    const price = candidate?.price;
    if (typeof price === "number" && Number.isFinite(price)) {
      parts.push(price === 0 ? "免费" : `x${price}`);
    }
    let label = parts.join(" · ");
    if (candidate?.viaPatch === true) label += " ⚠️补丁";
    return label;
  }
  function isRejected(rejections, canonicalId, provider, realId) {
    if (typeof rejections !== "object" || rejections === null || Array.isArray(rejections)) return false;
    const byProvider = rejections[canonicalId];
    if (typeof byProvider !== "object" || byProvider === null || Array.isArray(byProvider)) return false;
    const byRealId = byProvider[provider];
    if (typeof byRealId !== "object" || byRealId === null || Array.isArray(byRealId)) return false;
    return byRealId[realId] === true;
  }
  function toggleRejection(rejections, canonicalId, provider, realId, rejected) {
    const base = typeof rejections === "object" && rejections !== null && !Array.isArray(rejections) ? rejections : {};
    const next = { ...base };
    const perProvider = { ...next[canonicalId] ?? {} };
    const perRealId = { ...perProvider[provider] ?? {} };
    if (rejected === true) perRealId[realId] = true;
    else delete perRealId[realId];
    if (Object.keys(perRealId).length === 0) delete perProvider[provider];
    else perProvider[provider] = perRealId;
    if (Object.keys(perProvider).length === 0) delete next[canonicalId];
    else next[canonicalId] = perProvider;
    return next;
  }

  // ../dsh-codearts-src/plugin-src/client/provider-toggle.js
  function groupProviders(providers, statuses) {
    const list = Array.isArray(providers) ? providers : [];
    const map = statuses && typeof statuses === "object" ? statuses : {};
    const open = [];
    const closed = [];
    for (const provider of list) {
      const isClosed = map[provider?.id]?.closed === true;
      if (isClosed) closed.push(provider);
      else open.push(provider);
    }
    return { open, closed };
  }
  function providerSwitchState(status) {
    if (status === void 0 || status === null || typeof status !== "object") {
      return { checked: true, disabled: true, reason: "状态尚未读取" };
    }
    const total = typeof status.models?.total === "number" ? status.models.total : 0;
    if (total <= 0) {
      return {
        checked: true,
        disabled: true,
        reason: "该供应商没有可关闭的模型"
      };
    }
    return { checked: status.closed !== true, disabled: false, reason: null };
  }
  function summarizeProviderToggle(enabled, res) {
    const models = typeof res?.models === "number" ? res.models : 0;
    const accounts = typeof res?.accounts === "number" ? res.accounts : 0;
    if (enabled) {
      const parts2 = [];
      parts2.push(models > 0 ? `已打开 ${models} 个模型` : "模型本就全部打开");
      parts2.push(accounts > 0 ? `已启用 ${accounts} 个账号` : "账号本就全部启用");
      return parts2.join("，");
    }
    const parts = [];
    parts.push(models > 0 ? `已关闭 ${models} 个模型` : "没有模型需要关闭");
    parts.push(accounts > 0 ? `已停用 ${accounts} 个账号` : "没有账号需要停用");
    return parts.join("，");
  }
  function sortOpenProvidersByOrder(openProviders, order) {
    const list = Array.isArray(openProviders) ? openProviders : [];
    const seq = Array.isArray(order) ? order : [];
    if (seq.length === 0) return list;
    const rank = /* @__PURE__ */ new Map();
    for (let index = 0; index < seq.length; index++) {
      const id = seq[index];
      if (typeof id === "string" && id.length > 0 && !rank.has(id)) rank.set(id, index);
    }
    if (rank.size === 0) return list;
    return [...list].sort((a, b) => {
      const ra = rank.get(a?.id);
      const rb = rank.get(b?.id);
      if (ra !== void 0 && rb !== void 0) return ra - rb;
      if (ra !== void 0) return -1;
      if (rb !== void 0) return 1;
      return 0;
    });
  }
  function nextProviderOrderAfterDrop(openIds, closedIds, sourceId, targetId, position, oldOrder) {
    const moved = orderAfterDrop(openIds, sourceId, targetId, position);
    if (moved === null) return null;
    if (!Array.isArray(closedIds) || closedIds.length === 0) return moved;
    if (!Array.isArray(oldOrder) || oldOrder.length === 0) return [...moved, ...closedIds];
    const openSet = new Set(openIds);
    const closedSet = new Set(closedIds);
    const predecessor = /* @__PURE__ */ new Map();
    let lastOpen = null;
    for (const id of oldOrder) {
      if (openSet.has(id)) {
        lastOpen = id;
        continue;
      }
      if (closedSet.has(id) && !predecessor.has(id)) predecessor.set(id, lastOpen);
    }
    const result = [...moved];
    const orphans = [];
    const groups = /* @__PURE__ */ new Map();
    for (const id of oldOrder) {
      if (!closedSet.has(id) || !predecessor.has(id)) continue;
      const pred = predecessor.get(id);
      if (pred === null) {
        orphans.push(id);
        continue;
      }
      if (!groups.has(pred)) groups.set(pred, []);
      groups.get(pred).push(id);
    }
    for (const [pred, ids] of groups) {
      const at = result.indexOf(pred);
      if (at === -1) {
        orphans.push(...ids);
        continue;
      }
      result.splice(at + 1, 0, ...ids);
    }
    for (const id of closedIds) if (!result.includes(id)) orphans.push(id);
    for (const id of orphans) result.push(id);
    return result;
  }
  function providerSwitchRows(providers, statuses, order) {
    const { open, closed } = groupProviders(providers, statuses);
    const orderedOpen = sortOpenProvidersByOrder(open, order);
    const toRow = (provider) => {
      const id = provider?.id;
      const status = statuses && typeof statuses === "object" ? statuses[id] : void 0;
      const sw = providerSwitchState(status);
      return {
        id,
        label: provider?.label || id,
        checked: sw.checked,
        disabled: sw.disabled,
        reason: sw.reason,
        models: status?.models ?? null,
        accounts: status?.accounts ?? null
      };
    };
    return [...orderedOpen.map(toRow), ...closed.map(toRow)];
  }
  function providerToggleSummary(providers, statuses) {
    const list = Array.isArray(providers) ? providers : [];
    const { open, closed } = groupProviders(list, statuses);
    return {
      open: open.length,
      closed: closed.length,
      total: list.length,
      known: statuses !== null && statuses !== void 0 && typeof statuses === "object"
    };
  }

  // ../dsh-codearts-src/plugin-src/client/model-groups.js
  var BILLING_GROUPS = Object.freeze([
    Object.freeze({ key: "subscription", label: "订阅额度", hint: "Cline Pass 订阅模型（cline-pass/*）" }),
    Object.freeze({ key: "free", label: "免费额度", hint: "Cline 远端 free 集合（含 cline-free/* 与 stealth/*）" }),
    Object.freeze({ key: "cloud", label: "Cline Cloud", hint: "Cline Cloud 模型（cline-cloud/*）" }),
    Object.freeze({ key: "metered", label: "按量计费", hint: "其余模型：走账户余额按量结算" })
  ]);
  var METERED_GROUP_KEY = "metered";
  function billingGroupOf(model) {
    if (model?.isFree === true) return "free";
    const id = typeof model?.id === "string" ? model.id : "";
    if (id.startsWith("cline-pass/")) return "subscription";
    if (id.startsWith("cline-cloud/")) return "cloud";
    return METERED_GROUP_KEY;
  }
  function groupModelsForDisplay(models, options = {}) {
    const list = Array.isArray(models) ? models : [];
    const buckets = new Map(BILLING_GROUPS.map((group) => [group.key, []]));
    for (const model of list) {
      const key = billingGroupOf(model);
      const bucket = buckets.get(key);
      if (bucket === void 0) continue;
      bucket.push(model);
    }
    const groups = [];
    for (const definition of BILLING_GROUPS) {
      const total = buckets.get(definition.key) ?? [];
      if (total.length === 0) continue;
      const shown = filterModels(total, options);
      if (shown.length === 0) continue;
      groups.push({
        key: definition.key,
        label: definition.label,
        hint: definition.hint,
        models: shown,
        counts: {
          total: total.length,
          shown: shown.length,
          disabled: shown.filter((model) => model.disabled === true).length
        }
      });
    }
    return groups;
  }
  function groupExpanded(group, options = {}) {
    const toggled = options.toggled;
    if (toggled === true || toggled === false) return toggled;
    if (options.filterActive === true) return true;
    return group?.key !== METERED_GROUP_KEY;
  }
  function groupBulkStateFor(group, busy) {
    return bulkButtonState(group?.models ?? null, busy);
  }

  // ../dsh-codearts-src/plugin-src/client/tokens-per-second.js
  var TOKENS_PER_SECOND_UNIT = "tok/s";
  function formatTokensPerSecond(tps) {
    const value = typeof tps === "number" && Number.isFinite(tps) ? tps : 0;
    const clamped = Math.max(0, value);
    return clamped >= 10 ? String(Math.round(clamped)) : String(Math.round(clamped * 10) / 10);
  }
  function tokensPerSecond(outputTokens, decodeMs) {
    if (!(typeof decodeMs === "number" && Number.isFinite(decodeMs) && decodeMs > 0)) return null;
    const tokens = typeof outputTokens === "number" && Number.isFinite(outputTokens) ? outputTokens : 0;
    return Math.max(0, tokens) / (decodeMs / 1e3);
  }
  function formatRowTokensPerSecond(row) {
    if (row?.usageReported !== true) return "—";
    const total = Number(row?.totalMs ?? 0);
    const first = Number(row?.ttftMs ?? 0);
    if (!(first > 0)) return "—";
    const value = tokensPerSecond(Number(row?.outputTokens ?? 0), total - first);
    return value === null ? "—" : `${formatTokensPerSecond(value)} ${TOKENS_PER_SECOND_UNIT}`;
  }

  // ../dsh-codearts-src/plugin-src/client/token-ledger-panel.js
  function formatTokenCount(value) {
    const n = typeof value === "number" && Number.isFinite(value) ? Math.max(0, Math.trunc(value)) : 0;
    if (n >= 1e6) return trimZeros(n / 1e6) + "M";
    if (n >= 1e4) return trimZeros(n / 1e3) + "K";
    return n.toLocaleString("en-US");
  }
  function trimZeros(n) {
    return String(Math.round(n * 100) / 100);
  }
  function channelLabel(channel) {
    return channel === "gateway" ? "网关" : "直连";
  }
  function channelOrder(a, b) {
    return (a === "direct" ? 0 : 1) - (b === "direct" ? 0 : 1);
  }
  function tokenSummaryText(row) {
    if (!row || typeof row !== "object") return "—";
    const parts = ["↓" + formatTokenCount(row.inputTokens ?? 0), "↑" + formatTokenCount(row.outputTokens ?? 0)];
    if ((row.cacheReadTokens ?? 0) > 0) parts.push("⚡" + formatTokenCount(row.cacheReadTokens));
    if ((row.cacheWriteTokens ?? 0) > 0) parts.push("✎" + formatTokenCount(row.cacheWriteTokens));
    if ((row.reasoningTokens ?? 0) > 0) parts.push("🧠" + formatTokenCount(row.reasoningTokens));
    return parts.join(" ");
  }
  function entryTokenText(entry) {
    if (entry?.usageReported !== true) return "—";
    return tokenSummaryText(entry);
  }
  function formatDuration(ms) {
    const n = typeof ms === "number" && Number.isFinite(ms) && ms > 0 ? Math.round(ms) : 0;
    if (n === 0) return "—";
    if (n < 1e3) return n + "ms";
    return trimZeros(n / 1e3) + "s";
  }
  function formatTtft(entry) {
    const n = typeof entry?.ttftMs === "number" && Number.isFinite(entry.ttftMs) && entry.ttftMs > 0 ? Math.round(entry.ttftMs) : 0;
    if (n === 0) return "—";
    return formatDuration(n);
  }
  function formatTps(entry) {
    const v = entry?.tps;
    if (typeof v !== "number" || !Number.isFinite(v) || v <= 0) return "—";
    return v + " tok/s";
  }
  function avgPerfText(row) {
    const ttft = typeof row?.avgTtftMs === "number" && row.avgTtftMs > 0 ? formatDuration(row.avgTtftMs) : null;
    const tps = typeof row?.avgTps === "number" && row.avgTps > 0 ? row.avgTps + " tok/s" : null;
    if (ttft === null && tps === null) return "—";
    return "首字 " + (ttft ?? "—") + " · " + (tps ?? "—");
  }
  function avgPerfTooltip() {
    return "均值口径：对有实测值的请求取算术平均。「首字」= 首个 chunk 到达前的耗时；「速率」= 全部输出 token ÷（首块之后 → 结束）的时长，要求解码时长 ≥ 100ms（过短视为不可测、不计入）。算术均值对离群样本敏感：个别极快/极慢的请求会明显拉高或拉低该值。";
  }
  var HISTORY_RANGES = [
    { key: "today", label: "今日", days: 1 },
    { key: "7d", label: "近 7 天", days: 7 },
    { key: "30d", label: "近 30 天", days: 30 },
    { key: "all", label: "全部", days: 0 }
  ];
  function rangeDaysOf(key) {
    const hit = HISTORY_RANGES.find((r) => r.key === key);
    return hit === void 0 ? 0 : hit.days;
  }
  function todayDayKey() {
    const shifted = new Date(Date.now() + 8 * 60 * 60 * 1e3);
    const pad = (x) => String(x).padStart(2, "0");
    return shifted.getUTCFullYear() + "-" + pad(shifted.getUTCMonth() + 1) + "-" + pad(shifted.getUTCDate());
  }
  function trendBars(historyDays, maxBars = 30) {
    const list = Array.isArray(historyDays) ? historyDays : [];
    const today = todayDayKey();
    return list.slice(0, maxBars).map((d) => ({
      day: d?.day ?? "",
      isToday: d?.day === today,
      requests: d?.totals?.requests ?? 0,
      tokens: (d?.totals?.inputTokens ?? 0) + (d?.totals?.outputTokens ?? 0)
    }));
  }
  var BAR_HEIGHT_MIN_PERCENT = 4;
  var BAR_HEIGHT_MAX_PERCENT = 88;
  var BAR_HEIGHT_SPAN_PERCENT = 84;
  var BAR_HEIGHT_SINGLE_PERCENT = 72;
  function barHeightPercent(tokens, maxTokens, barCount = 0) {
    const max = typeof maxTokens === "number" && maxTokens > 0 ? maxTokens : 0;
    const t = typeof tokens === "number" && tokens > 0 ? tokens : 0;
    if (max === 0 || t === 0) return 0;
    const ratio = t / max;
    if (barCount <= 1) return BAR_HEIGHT_SINGLE_PERCENT;
    const scaled = BAR_HEIGHT_MIN_PERCENT + ratio * BAR_HEIGHT_SPAN_PERCENT;
    return Math.round(Math.min(BAR_HEIGHT_MAX_PERCENT, Math.max(BAR_HEIGHT_MIN_PERCENT, scaled)));
  }
  function barDayLabel(day) {
    if (typeof day !== "string" || day.length < 10) return "";
    const m = day.slice(5, 7);
    const d = day.slice(8, 10);
    return /^\d{2}-\d{2}$/.test(m + "-" + d) ? m + "-" + d : "";
  }
  function historyTitle(rangeKey) {
    const hit = HISTORY_RANGES.find((r) => r.key === rangeKey);
    return "历史用量 · " + (hit === void 0 ? "全部" : hit.label);
  }
  function formatEntryTime(ts) {
    const d = typeof ts === "number" && Number.isFinite(ts) ? new Date(ts) : null;
    if (d === null || Number.isNaN(d.getTime())) return "—";
    const pad = (x) => String(x).padStart(2, "0");
    return pad(d.getHours()) + ":" + pad(d.getMinutes()) + ":" + pad(d.getSeconds());
  }
  function ledgerSubtitle() {
    return "本机流水 · 明细重启清空 · 日累计已存盘";
  }
  function accountLabel(accountId) {
    return typeof accountId === "string" && accountId.length > 0 ? accountId : "未归属";
  }
  function channelCardTitle(channelRow) {
    const requests = channelRow?.totals?.requests ?? 0;
    return channelLabel(channelRow?.channel) + " · " + requests + " 次请求";
  }
  function hasAnyData(snapshot) {
    return (snapshot?.entries?.length ?? 0) > 0;
  }

  // ../dsh-codearts-src/plugin-src/client/backup-crypto.js
  var KDF_ITERATIONS = 31e4;
  var KEY_LENGTH_BITS = 256;
  var SALT_BYTES = 16;
  var IV_BYTES = 12;
  function isEncryptedBackup(value) {
    return typeof value === "object" && value !== null && typeof value.kdf === "string" && typeof value.ciphertext === "string";
  }
  async function encryptBackup(payload, passphrase) {
    const encoder = new TextEncoder();
    const salt = crypto.getRandomValues(new Uint8Array(SALT_BYTES));
    const iv = crypto.getRandomValues(new Uint8Array(IV_BYTES));
    const key = await deriveKey(passphrase, salt, KDF_ITERATIONS, ["encrypt"]);
    const plaintext = encoder.encode(JSON.stringify(payload));
    const ciphertext = await crypto.subtle.encrypt({ name: "AES-GCM", iv }, key, plaintext);
    return {
      format: "dsh-codearts-auth/backup.encrypted",
      kdf: "PBKDF2",
      hash: "SHA-256",
      iterations: KDF_ITERATIONS,
      salt: toBase64(salt),
      iv: toBase64(iv),
      ciphertext: toBase64(new Uint8Array(ciphertext))
    };
  }
  async function decryptBackup(container, passphrase) {
    if (!isEncryptedBackup(container)) {
      throw new Error("不是加密备份文件");
    }
    const salt = fromBase64(container.salt);
    const iv = fromBase64(container.iv);
    const ciphertext = fromBase64(container.ciphertext);
    const iterations = Number.isSafeInteger(container.iterations) && container.iterations > 0 ? container.iterations : KDF_ITERATIONS;
    const key = await deriveKey(passphrase, salt, iterations, ["decrypt"]);
    const plaintext = await crypto.subtle.decrypt({ name: "AES-GCM", iv }, key, ciphertext);
    return JSON.parse(new TextDecoder().decode(plaintext));
  }
  async function deriveKey(passphrase, salt, iterations, usages) {
    const encoder = new TextEncoder();
    const keyMaterial = await crypto.subtle.importKey(
      "raw",
      encoder.encode(passphrase),
      "PBKDF2",
      false,
      ["deriveKey"]
    );
    return crypto.subtle.deriveKey(
      { name: "PBKDF2", salt, iterations, hash: "SHA-256" },
      keyMaterial,
      { name: "AES-GCM", length: KEY_LENGTH_BITS },
      false,
      usages
    );
  }
  function toBase64(bytes) {
    let binary = "";
    const chunk = 32768;
    for (let i = 0; i < bytes.length; i += chunk) {
      binary += String.fromCharCode(...bytes.subarray(i, i + chunk));
    }
    return btoa(binary);
  }
  function fromBase64(value) {
    const binary = atob(value);
    const bytes = new Uint8Array(binary.length);
    for (let i = 0; i < binary.length; i++) {
      bytes[i] = binary.charCodeAt(i);
    }
    return bytes;
  }

  // ../dsh-codearts-src/plugin-src/client/credits-format.js
  function formatCredits(value) {
    if (typeof value !== "number" || !Number.isFinite(value)) return null;
    return Number.isInteger(value) ? String(value) : value.toFixed(2);
  }
  function formatTokens(value) {
    if (typeof value !== "number" || !Number.isFinite(value)) return null;
    const abs = Math.abs(value);
    if (abs >= 1e6) return `${(value / 1e6).toFixed(2)}M`;
    if (abs >= 1e3) return `${(value / 1e3).toFixed(2)}K`;
    return String(Math.round(value));
  }
  var QUOTA_UNIT = "%";
  function formatQuota(value) {
    if (typeof value !== "number" || !Number.isFinite(value)) return null;
    return `${Math.round(value)}%`;
  }
  function formatUnits(value, unit) {
    if (unit === "token") return formatTokens(value);
    if (unit === QUOTA_UNIT) return formatQuota(value);
    return formatCredits(value);
  }
  function unitLabel(unit) {
    if (unit === "token") return "Token";
    if (unit === QUOTA_UNIT) return "额度";
    return "积分";
  }
  function formatClaimGains(totals) {
    if (totals === null || typeof totals !== "object") return null;
    const parts = [];
    for (const unit of ["token", "credit"]) {
      const value = Number(totals[unit]);
      if (!Number.isFinite(value) || value <= 0) continue;
      parts.push(`+${formatUnits(value, unit)}${unitLabel(unit)}`);
    }
    return parts.length === 0 ? null : parts.join(", ");
  }
  function formatQuotaLine(packages, unit) {
    if (unit !== QUOTA_UNIT || !Array.isArray(packages)) return null;
    const parts = [];
    for (const pkg of packages) {
      if (!pkg) continue;
      const value = formatQuota(pkg.remaining);
      if (value === null) continue;
      parts.push(`${pkg.name || "未命名"} ${value}`);
    }
    return parts.length === 0 ? null : parts.join(" · ");
  }
  function formatQuotaDetail(packages) {
    if (!Array.isArray(packages)) return null;
    const lines = [];
    for (const pkg of packages) {
      if (!pkg) continue;
      const value = formatQuota(pkg.remaining);
      if (value === null) continue;
      const reset = typeof pkg.cycleEndTime === "string" && pkg.cycleEndTime.length > 0 ? ` · 重置于 ${pkg.cycleEndTime}` : "";
      lines.push(`${pkg.name || "未命名"}：剩余 ${value}${reset}`);
    }
    return lines.length === 0 ? null : lines.join("\n");
  }

  // ../dsh-codearts-src/plugin-src/client/quota-format.js
  var QUOTA_WINDOWS = Object.freeze([
    ["five_hour", "5 小时"],
    ["weekly", "本周"],
    ["monthly", "本月"]
  ]);
  function quotaWindowsOf(windows) {
    const known = new Map(windows.map((win) => [String(win.type), win]));
    const ordered = QUOTA_WINDOWS.filter(([type]) => known.has(type)).map(([type, label]) => [type, label, known.get(type)]);
    const extra = windows.filter((win) => !QUOTA_WINDOWS.some(([type]) => type === String(win.type))).map((win) => [String(win.type), String(win.type), win]);
    return [...ordered, ...extra];
  }
  function quotaCountdown(resetsAt) {
    const at = Date.parse(String(resetsAt ?? ""));
    if (!Number.isFinite(at)) return "";
    const minutes = Math.round((at - Date.now()) / 6e4);
    if (minutes <= 0) return "";
    const days = Math.floor(minutes / 1440);
    const hours = Math.floor(minutes % 1440 / 60);
    const mins = minutes % 60;
    if (days > 0) return `${days} 天 ${hours} 小时`;
    if (hours > 0) return `${hours} 小时 ${mins} 分钟`;
    return `${Math.max(1, mins)} 分钟`;
  }
  function quotaResetsIn(resetsAt) {
    const left = quotaCountdown(resetsAt);
    return left === "" ? "" : `${left}后重置`;
  }
  function quotaTone(percent) {
    if (!Number.isFinite(percent)) return "ok";
    if (percent >= 90) return "error";
    if (percent >= 70) return "warn";
    return "ok";
  }
  function quotaPercentValue(percent) {
    const n = Number(percent ?? 0);
    if (!Number.isFinite(n)) return 0;
    return Math.max(0, Math.min(100, n));
  }
  function formatQuotaPercent(percent) {
    return `${Math.round(quotaPercentValue(percent))}%`;
  }

  // ../dsh-codearts-src/plugin-src/client/jet-hub.js
  var CODEARTS_ICON = "data:image/png;base64,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";
  var CODEBUDDY_ICON = "data:image/png;base64,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";
  var WORKBUDDY_ICON = "data:image/png;base64,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";
  var LOBSTERAI_ICON = "data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAyNCAyNCIgd2lkdGg9IjI0IiBoZWlnaHQ9IjI0Ij48cmVjdCB3aWR0aD0iMjQiIGhlaWdodD0iMjQiIHJ4PSI1IiBmaWxsPSIjZTg1MDNhIi8+PHBhdGggZD0iTTEyIDUuNWMtMi40IDAtNC4yIDEuNi00LjIgNHY1LjJjMCAyLjMgMS44IDMuOCA0LjIgMy44czQuMi0xLjUgNC4yLTMuOFY5LjVjMC0yLjQtMS44LTQtNC4yLTR6IiBmaWxsPSIjZmZmIi8+PGNpcmNsZSBjeD0iMTAuMyIgY3k9IjEwLjIiIHI9IjEiIGZpbGw9IiNlODUwM2EiLz48Y2lyY2xlIGN4PSIxMy43IiBjeT0iMTAuMiIgcj0iMSIgZmlsbD0iI2U4NTAzYSIvPjxwYXRoIGQ9Ik04LjQgNy4yIDYuMiA0LjltOS40IDIuMyAyLjItMi4zTTkuOSAxOC41bC0xLjQgMm02LjYtMiAxLjQgMiIgc3Ryb2tlPSIjZmZmIiBzdHJva2Utd2lkdGg9IjEuNCIgc3Ryb2tlLWxpbmVjYXA9InJvdW5kIiBmaWxsPSJub25lIi8+PC9zdmc+";
  var QODER_ICON = "data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAyNCAyNCIgd2lkdGg9IjI0IiBoZWlnaHQ9IjI0Ij48cmVjdCB3aWR0aD0iMjQiIGhlaWdodD0iMjQiIHJ4PSI1IiBmaWxsPSIjMWYyYTNmIi8+PGNpcmNsZSBjeD0iMTEiIGN5PSIxMSIgcj0iNC42IiBmaWxsPSJub25lIiBzdHJva2U9IiNmZmYiIHN0cm9rZS13aWR0aD0iMS44Ii8+PHBhdGggZD0iTTEzLjkgMTMuOSAxNyAxN2EwLjk1IDAuOTUgMCAwIDEtMS4zNSAxLjM1bC0zLjEtMy4xIiBmaWxsPSIjZmZmIi8+PC9zdmc+";
  var QODERCN_ICON = "data:image/png;base64,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";
  var TRAE_ICON = "data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjM4IDM4IDQzNSA0MzUiIHdpZHRoPSIyNCIgaGVpZ2h0PSIyNCI+PHBhdGggZmlsbD0iIzMyRjA4QyIgZmlsbC1ydWxlPSJldmVub2RkIiBkPSJNNTggMTE2aDM5NXYyNzlIMTE1di01NUg1OHpNMTE1IDE3MmgyODF2MTY4SDExNXoiLz48cGF0aCBmaWxsPSIjMzJGMDhDIiBkPSJNMjE1LjUgMjE1LjVsMzkgMzktMzkgMzktMzktMzl6TTMyOSAyMTUuNWwzOSAzOS0zOSAzOS0zOS0zOXoiLz48L3N2Zz4=";
  var CLINE_ICON = "data:image/png;base64,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";
  var LOOMY_ICON = 'data:image/svg+xml,%3Csvg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24"%3E%3Crect width="24" height="24" rx="6" fill="%23007aff"/%3E%3Ctext x="12" y="17.5" font-size="15" font-family="sans-serif" font-weight="700" fill="white" text-anchor="middle"%3EL%3C/text%3E%3C/svg%3E';
  var RACCOON_ICON = "data:image/png;base64,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";
  var MINIMAX_ICON = "data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iODAiIGhlaWdodD0iODAiIHZpZXdCb3g9IjAgMCA4MCA4MCIgZmlsbD0ibm9uZSIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj4KPGcgY2xpcC1wYXRoPSJ1cmwoI2NsaXAwXzQ3XzY0MDUpIj4KPHJlY3Qgd2lkdGg9IjgwIiBoZWlnaHQ9IjgwIiBmaWxsPSIjN0RDNkZGIiBzdHlsZT0iZmlsbDojN0RDNkZGO2ZpbGw6Y29sb3IoZGlzcGxheS1wMyAwLjQ5MDIgMC43NzY1IDEuMDAwMCk7ZmlsbC1vcGFjaXR5OjE7Ii8+CjxwYXRoIGQ9Ik02Ni45NTc1IDUwLjY1NTZDNjYuOTU3NSA1MS43MDYgNjYuNDg1NSA1Mi43MDA4IDY1LjY3MTkgNTMuMzY1MUw1Ni4wMDgzIDYxLjI1NTZDNTUuMzgzOCA2MS43NjU1IDU0LjYwMjMgNjIuMDQ0IDUzLjc5NiA2Mi4wNDRIMTcuMTExOEMxNS4zMDA3IDYyLjA0NCAxMy44MzI1IDYwLjU3NTggMTMuODMyNSA1OC43NjQ3VjMxLjEwNzZDMTMuODMyNSAzMC4wNTQ3IDE0LjMwNjggMjkuMDU3OSAxNS4xMjM3IDI4LjM5MzZMMjUuODc2MyAxOS42NTAzQzI2LjUgMTkuMTQzMiAyNy4yNzkzIDE4Ljg2NjMgMjguMDgzMiAxOC44NjYzSDYzLjY3ODJDNjUuNDg5MyAxOC44NjYzIDY2Ljk1NzUgMjAuMzM0NSA2Ni45NTc1IDIyLjE0NTZWNTAuNjU1NloiIGZpbGw9ImJsYWNrIiBzdHlsZT0iZmlsbDpibGFjaztmaWxsLW9wYWNpdHk6MTsiLz4KPHBhdGggZD0iTTYwLjM5ODQgNDguMzY4NEM2MC4zOTg0IDQ4Ljg5NDYgNjAuMTYxNCA0OS4zOTI4IDU5Ljc1MzMgNDkuNzI1TDUzLjE3NDIgNTUuMDc4NUM1Mi44NjIzIDU1LjMzMjMgNTIuNDcyNCA1NS40NzA5IDUyLjA3MDMgNTUuNDcwOUgyMS41MzVDMjAuOTMxMyA1NS40NzA5IDIwLjQ0MTkgNTQuOTgxNSAyMC40NDE5IDU0LjM3NzhWMzMuMjYwMUMyMC40NDE5IDMyLjczMiAyMC42ODA1IDMyLjIzMjIgMjEuMDkxMSAzMS45MDAxTDI4LjYzNDcgMjUuNzk5OEMyOC45NDYyIDI1LjU0NzkgMjkuMzM0NyAyNS40MTA2IDI5LjczNTMgMjUuNDEwN0w1OS4zMDU4IDI1LjQyNDVDNTkuOTA5MyAyNS40MjQ3IDYwLjM5ODQgMjUuOTE0MSA2MC4zOTg0IDI2LjUxNzZWNDguMzY4NFoiIGZpbGw9IndoaXRlIiBzdHlsZT0iZmlsbDp3aGl0ZTtmaWxsLW9wYWNpdHk6MTsiLz4KPHBhdGggZD0iTTI2LjU1NDcgNDMuNjYxOUMyNi41NTQ3IDQyLjY5NTkgMjcuMzM3NyA0MS45MTI5IDI4LjMwMzcgNDEuOTEyOUgzMi40NTc1QzMzLjQyMzQgNDEuOTEyOSAzNC4yMDY0IDQyLjY5NTkgMzQuMjA2NCA0My42NjE5VjU2LjIzMjZIMjYuNTU0N1Y0My42NjE5WiIgZmlsbD0iYmxhY2siIHN0eWxlPSJmaWxsOmJsYWNrO2ZpbGwtb3BhY2l0eToxOyIvPgo8cGF0aCBkPSJNMzguMTQxOCA0My42NjE5QzM4LjE0MTggNDIuNjk1OSAzOC45MjQ5IDQxLjkxMjkgMzkuODkwOCA0MS45MTI5SDQ0LjA0NDZDNDUuMDEwNiA0MS45MTI5IDQ1Ljc5MzYgNDIuNjk1OSA0NS43OTM2IDQzLjY2MTlWNTYuMjMyNkgzOC4xNDE4VjQzLjY2MTlaIiBmaWxsPSJibGFjayIgc3R5bGU9ImZpbGw6YmxhY2s7ZmlsbC1vcGFjaXR5OjE7Ii8+CjwvZz4KPGRlZnM+CjxjbGlwUGF0aCBpZD0iY2xpcDBfNDdfNjQwNSI+CjxwYXRoIGQ9Ik0wIDIwQzAgOC45NTQzMSA4Ljk1NDMxIDAgMjAgMEg2MEM3MS4wNDU3IDAgODAgOC45NTQzMSA4MCAyMFY2MEM4MCA3MS4wNDU3IDcxLjA0NTcgODAgNjAgODBIMjBDOC45NTQzMSA4MCAwIDcxLjA0NTcgMCA2MFYyMFoiIGZpbGw9IndoaXRlIiBzdHlsZT0iZmlsbDp3aGl0ZTtmaWxsLW9wYWNpdHk6MTsiLz4KPC9jbGlwUGF0aD4KPC9kZWZzPgo8L3N2Zz4K";
  var ZCODE_ICON = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAACAAAAAgCAYAAABzenr0AAAAgklEQVR42u3XsRGAIAyF4UxgYe0g7j+FpZtgRwN36stLAhruqP+v4ICIdNaybsViy92yCj+CeMW7CO94gwgFRMUrIgFTAPbzgPe/Aa5nAInTAGicAtDE1QBtXAVgxGEAeuIpAFYYArDj81xEoQDLd+A1ID8k3wTkXDDEaDbEcBo1nl/XXoK4yMqvMgAAAABJRU5ErkJggg==";
  var OPENCODE_ICON = "data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAyNCAyNCI+PHJlY3Qgd2lkdGg9IjI0IiBoZWlnaHQ9IjI0IiByeD0iNSIgZmlsbD0iIzEyMTYxZCIvPjxwYXRoIGQ9Ik02LjUgOC41IDEwLjUgMTJsLTQgMy41IiBzdHJva2U9IiNmZmYiIHN0cm9rZS13aWR0aD0iMS44IiBmaWxsPSJub25lIiBzdHJva2UtbGluZWNhcD0icm91bmQiIHN0cm9rZS1saW5lam9pbj0icm91bmQiLz48cGF0aCBkPSJNMTIuNSAxNmg1IiBzdHJva2U9IiNmZmYiIHN0cm9rZS13aWR0aD0iMS44IiBmaWxsPSJub25lIiBzdHJva2UtbGluZWNhcD0icm91bmQiLz48L3N2Zz4=";
  var GEMINI_ICON = "data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAyNCAyNCI+PGRlZnM+PGxpbmVhckdyYWRpZW50IGlkPSJnIiB4MT0iMCIgeTE9IjAiIHgyPSIxIiB5Mj0iMSI+PHN0b3Agb2Zmc2V0PSIwIiBzdG9wLWNvbG9yPSIjNDI4NUY0Ii8+PHN0b3Agb2Zmc2V0PSIxIiBzdG9wLWNvbG9yPSIjOUI3MkNCIi8+PC9saW5lYXJHcmFkaWVudD48L2RlZnM+PHBhdGggZD0iTTEyIDJjLjQgMy45IDIuMSA2LjYgNiA4LTMuOSAxLjQtNS42IDQuMS02IDgtLjQtMy45LTIuMS02LjYtNi04IDMuOS0xLjQgNS42LTQuMSA2LThaIiBmaWxsPSJ1cmwoI2cpIi8+PHBhdGggZD0iTTE5LjUgMTRjLjIgMS43LjkgMi45IDIuNiAzLjUtMS43LjYtMi40IDEuOC0yLjYgMy41LS4yLTEuNy0uOS0yLjktMi42LTMuNSAxLjctLjYgMi40LTEuOCAyLjYtMy41WiIgZmlsbD0idXJsKCNnKSIvPjwvc3ZnPg==";
  var AUTOCLAW_ICON = "data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAyNCAyNCI+PGRlZnM+PGxpbmVhckdyYWRpZW50IGlkPSJnIiB4MT0iMCIgeTE9IjAiIHgyPSIxIiB5Mj0iMSI+PHN0b3Agb2Zmc2V0PSIwIiBzdG9wLWNvbG9yPSIjNEY4REY3Ii8+PHN0b3Agb2Zmc2V0PSIxIiBzdG9wLWNvbG9yPSIjOUI3MkNCIi8+PC9saW5lYXJHcmFkaWVudD48L2RlZnM+PHJlY3QgeD0iMS41IiB5PSIxLjUiIHdpZHRoPSIyMSIgaGVpZ2h0PSIyMSIgcng9IjUiIGZpbGw9InVybCgjZykiLz48cGF0aCBkPSJNMTMuNCA0LjggOC4yIDEzLjFoMy40bC0xIDYuMSA1LjQtOC40aC0zLjVaIiBmaWxsPSIjZmZmIi8+PC9zdmc+";
  var AGGREGATE_ICON = "data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAyNCAyNCI+PGNpcmNsZSBjeD0iNSIgY3k9IjUiIHI9IjIuNiIgZmlsbD0iIzRGOERGNyIvPjxjaXJjbGUgY3g9IjUiIGN5PSIxOSIgcj0iMi42IiBmaWxsPSIjOUI3MkNCIi8+PGNpcmNsZSBjeD0iMTkiIGN5PSIxMiIgcj0iMi42IiBmaWxsPSIjNEY4REY3Ii8+PHBhdGggZD0iTTcgNi4yIDE2LjQgMTFNNyAxNy44IDE2LjQgMTMiIHN0cm9rZT0iIzlCNzJDQiIgc3Ryb2tlLXdpZHRoPSIxLjciIHN0cm9rZS1saW5lY2FwPSJyb3VuZCIgZmlsbD0ibm9uZSIvPjxjaXJjbGUgY3g9IjEyIiBjeT0iMTIiIHI9IjEuNSIgZmlsbD0iIzlCNzJDQiIvPjwvc3ZnPg==";
  var PROVIDERS = Object.freeze([
    { id: "codearts", label: "CodeArts (华为云)", icon: CODEARTS_ICON, logoClass: "codearts" },
    { id: "buddy", label: "CodeBuddy (腾讯)", icon: CODEBUDDY_ICON, logoClass: "buddy" },
    // ⚠️ 这几条 label 是**最长行**，直接决定 rail 要多宽才不会出省略号
    // （WorkBuddy 国际版这条实测要 141px）。改长度前先看 `jet-hub-styles.js`
    // 里 .dim-jh-rail 的算式；且 raccoon 那条与 `RaccoonProduct.displayName`
    // 有跨文件一致性断言（`raccoon-client-panel.spec.ts`），不能只改一处。
    { id: "workbuddy", label: "WorkBuddy (国际版)", icon: WORKBUDDY_ICON, logoClass: "workbuddy" },
    { id: "lobsterai", label: "LobsterAI (有道)", icon: LOBSTERAI_ICON, logoClass: "lobsterai" },
    { id: "qoder", label: "Qoder", icon: QODER_ICON, logoClass: "qoder" },
    // ⚠️ label 用『Qoder (中国版)』而非『Qoder CN』——与 `QODER_CN.displayName`
    // 保持一致；长度受上面那条算式约束。
    { id: "qodercn", label: "Qoder (中国版)", icon: QODERCN_ICON, logoClass: "qodercn" },
    { id: "trae", label: "TRAE (字节)", icon: TRAE_ICON, logoClass: "trae" },
    { id: "cline", label: "Cline", icon: CLINE_ICON, logoClass: "cline" },
    { id: "loomy", label: "Loomy (讯飞)", icon: LOOMY_ICON, logoClass: "loomy" },
    // ⚠️ 用『Raccoon (商汤)』而非『Raccoon Work (商汤)』—— 后者在 provider 列表里
    // **触发换行**（用户报障）。与 `RaccoonProduct.displayName` 保持一致，
    // 且这条一致性由 `raccoon-client-panel.spec.ts` 锁死。
    { id: "raccoon", label: "Raccoon (商汤)", icon: RACCOON_ICON, logoClass: "raccoon" },
    { id: "minimax", label: "MiniMax Code", icon: MINIMAX_ICON, logoClass: "minimax" },
    /**
     * ZCode（智谱）。
     *
     * ⚠️ 注释**刻意不写「第几个 provider」**（Gitee issue IKJLK3 F2）：这种序号型
     *   注释在增删 provider 时必然失效 —— 原来这里写「第十个」、下面 opencode 那条
     *   写「第 12 个」，两者都已与实际顺序对不上，却一直没人发现。改用不含序号的表述。
     *
     * ⚠️ 用『ZCode (智谱)』，与 `ZCODE.displayName` 保持一致；长度也刻意
     * 控制在不会触发换行/省略号的范围内（见上面 workbuddy 那条的算式）。
     *
     * ⚠️ 它走**标准两步式登录**（与 codearts / qoder / trae 同型）：
     * 后端立刻返回官方授权 URL（`https://bigmodel.cn/login?appId=zcode…`），
     * 前端弹窗、用户在浏览器授权，后端轮询到 `status: "ready"` 后拿到 token。
     * 唯一的例外是**登录渠道要先问一句**（`bigmodel` / `zai`，见 `new-account.js`）——
     * 这两个渠道的授权页与凭据落点都不同，必须由用户选。
     * 除那一次选择外，它**不需要任何特殊分支** —— 与其余 provider 共用同一条
     * 「弹窗 + 登录轮询」路径。
     */
    { id: "zcode", label: "ZCode (智谱)", icon: ZCODE_ICON, logoClass: "zcode" },
    /**
     * AutoClaw（智谱系渠道）—— UTTS app 侧 13 渠道之一。
     *
     * ⚠️ app 侧 provider 同名 `autoclaw`，**无需映射**（不同于插件叫 `buddy`、
     * app 叫 `codebuddy` 那条桥内转换）。id 也保持纯小写字母（不带连字符）。
     *
     * ⚠️ 能力（app 侧已核实）：余额**有**（queryCredits）、签到**无**
     * （渠道 checkIn 返回「该渠道无签到接口」）、续期**有**（refresh）。
     * 能力表见 `credits-capabilities.js`：balance:true, dailyCheckin:false。
     *
     * ⚠️ label 用「AutoClaw (智谱)」：与 app ChatChannels.displayName
     * 「AutoClaw 智谱」呼应，但保持本表「渠道名 (厂商)」的既有格式；
     * 长度短于 WorkBuddy 那条最长行（141px 实测），不会撑破 rail。
     */
    { id: "autoclaw", label: "AutoClaw (智谱)", icon: AUTOCLAW_ICON, logoClass: "autoclaw" },
    /**
     * OpenCode。
     *
     * ⚠️ label 必须与 `OPENCODE.displayName` **逐字一致**（'OpenCode'）：
     * rail 宽度算式与 `tests/unit/opencode-client-panel.spec.ts` 的跨文件
     * 一致性断言都依赖它（raccoon 那条同理，改一处会连锁失败）。
     *
     * ⚠️ 它是**唯一不跳浏览器**的 provider：登录走自绘的 API key 弹窗
     * （`OpencodeKeyModal`），故 `createAccount` 在最前面就改道、**不会**走到
     * 下面的「弹窗 + 轮询」路径 —— 也因此它**不**经过 zCode 的渠道选择弹窗。
     */
    { id: "opencode", label: "OpenCode", icon: OPENCODE_ICON, logoClass: "opencode" },
    /**
     * Gemini Code Assist（Google Cloud Code 免费线，第 14 个 provider）。
     *
     * ⚠️ label 必须与 `GEMINI.displayName` **逐字一致**（`src/gemini.ts` 的
     * `displayName: 'Gemini Code Assist'`）：rail 宽度算式（`.dim-jh-rail` 的
     * 228px）与跨文件一致性断言都依赖它。这条是本表里**最长**的 label，
     * 但它仍短于既有的「WorkBuddy (国际版)」（141px 实测），故不会撑破 rail。
     *
     * ⚠️ 登录走**浏览器回调式 OAuth**（本地 `127.0.0.1` 起回调服务），
     * 后端在 `account.create` 里**立即返回 loginUrl** —— 前端与其余 provider
     * 共用同一条「弹窗 + 登录轮询」路径，无需特殊分支。
     */
    { id: "gemini", label: "Gemini Code Assist", icon: GEMINI_ICON, logoClass: "gemini" },
    /**
     * 聚合 provider（跨渠道临期优先）。
     *
     * ⚠️ 它**不是**一个普通渠道：没有账号、没有凭据、没有积分、没有登录按钮。
     * 面板是独立组件 `AggregatePanel`（见 `renderRail` 与面板分流处的注释）。
     *
     * ⚠️ 加它会让**四处**从 `PROVIDERS` 正则派生 id 的既有断言需要同步
     *（`zcode-wiring.spec.ts` 两处、`credits-capabilities.spec.ts`、
     *`jet-hub-store.spec.ts`）—— 那四处已在 P2 Task 6 一并处理。
     *
     * ⚠️ id 必须**纯小写字母**（不带连字符）：上面那四处里**三处**用的字符类是
     * `[a-z]+` / `[a-z0-9]+`，连字符会让它们**静默漏算**一个 id（保险失效比变红更糟）。
     */
    { id: "aggregate", label: "聚合 (跨渠道)", icon: AGGREGATE_ICON, logoClass: "aggregate" }
  ]);
  function providerLabel(id) {
    return PROVIDERS.find((p) => p.id === id)?.label ?? id;
  }
  var GROWTH_TASK_PROVIDERS = /* @__PURE__ */ new Set(["buddy"]);
  function ProviderLogo({ provider }) {
    const p = PROVIDERS.find((p2) => p2.id === provider);
    if (!p) return null;
    return React2.createElement(
      "span",
      { className: `dim-jh-providerIcon ${p.logoClass}` },
      React2.createElement("img", { src: p.icon, alt: "", width: 20, height: 20 })
    );
  }
  function formatTime(ts) {
    if (!ts || ts <= 0) return null;
    const d = new Date(ts);
    const now = Date.now();
    if (ts < now) return "已过期";
    const diff = ts - now;
    if (diff < 36e5) return `${Math.round(diff / 6e4)} 分钟后`;
    if (diff < 864e5) return `${Math.round(diff / 36e5)} 小时后`;
    return d.toLocaleString("zh-CN", { month: "short", day: "numeric", hour: "2-digit", minute: "2-digit" });
  }
  var RETEST_HELP = "对本账号每个「限额重置」标记的模型真实发送一条最小消息：正常返回则清除该标记，仍被限流则保留。会消耗少量模型额度。";
  var RETEST_ALL_HELP = "对本页全部账号（含已停用）执行「重测」：逐个模型真实发送一条最小消息，正常返回才清除标记。停用账号同样会发送。会消耗模型额度。";
  var RESET_HELP = "直接清除本账号的全部「限额重置」标记，不发送任何请求。适用于你已确认额度恢复、只想清掉显示的情况。";
  var RESET_ALL_HELP = "直接清除本页全部账号（含已停用）的「限额重置」标记，不发送任何请求。";
  var TEST_HELP = "无条件真实发送一条最小消息给这个账号（不依赖是否存在「限额重置」标记），用来确认账号当下到底还能不能用。与「重测」的区别：重测只测「已有标记」的模型，没有标记时一次请求都不发；测试不看标记，直接挑一个模型发出去。只回报结果，不修改任何限流标记。会消耗少量模型额度。";
  var MODEL_LIST_HELP = "列出该 Provider 的全部模型。每个模型后面的开关默认打开；关闭后，该模型不再出现在对话框的模型选择列表里（黑名单制：只有被关闭的才隐藏，其余含服务端新增的模型一律默认显示）。此设置持久化保存，可随时重新打开。";
  function summarizeProbe(kind, res) {
    if (kind === "reset" || kind === "resetAll") {
      const n = res?.clearedCount ?? 0;
      return n > 0 ? `已清除 ${n} 条限流标记` : "没有可清除的限流标记";
    }
    const accounts = res?.accounts ?? [];
    const cleared = res?.clearedCount ?? 0;
    const still = accounts.reduce((sum, a) => sum + (a.stillLimited?.length ?? 0), 0);
    const parts = [];
    if (cleared > 0) parts.push(`已清除 ${cleared} 条`);
    if (still > 0) parts.push(`${still} 条仍受限`);
    if (parts.length === 0) parts.push("没有可重测的限流标记");
    return parts.join("，");
  }
  function summarizeTest(res) {
    const model = res?.modelId || "未知模型";
    if (res?.ok) return `测试通过：${model} 正常返回`;
    const reason = res?.message || "仍受限";
    return `测试未通过：${model} — ${reason}`;
  }
  function formatPackageLine(pkg, windowDays, now) {
    const remaining = formatUnits(pkg.remaining, pkg.unit) ?? "?";
    const total = formatUnits(pkg.total, pkg.unit) ?? "?";
    const parts = [`${pkg.active ? "" : "[已失效] "}${pkg.name || "未命名"}: ${remaining} / ${total}`];
    const daysLeft = daysUntilExpiry(pkg, now);
    if (!pkg.active && pkg.expiredTime) parts.push(`失效于 ${pkg.expiredTime}`);
    else if (daysLeft !== null) {
      const label = expiryBucketLabel(pkg, windowDays, now);
      parts.push(`距到期 ${Math.ceil(daysLeft)} 天${label ? `（${label}）` : ""}`);
    } else if (pkg.cycleEndTime) parts.push(`本周期至 ${pkg.cycleEndTime}`);
    return parts.join(" · ");
  }
  function CreditBalanceRow({ balance, error, loading, windowDays, provider }) {
    const all = balance?.packages || [];
    const unit = all.find((p) => p && p.unit)?.unit;
    const label = unitLabel(unit);
    if (loading) {
      return React2.createElement(
        "div",
        { className: "dim-jh-metaRow" },
        React2.createElement("dt", null, label),
        React2.createElement("dd", { "data-tone": "muted" }, "读取中…")
      );
    }
    if (error || !balance) {
      return React2.createElement(
        "div",
        { className: "dim-jh-metaRow" },
        React2.createElement("dt", null, label),
        React2.createElement(
          "dd",
          { "data-tone": "warn", title: error || "查询失败" },
          error || "查询失败"
        )
      );
    }
    const total = formatUnits(balance.total, unit) ?? "0";
    const activeCount = all.filter((p) => p.active).length;
    const quotaText = formatQuotaLine(all, unit);
    const now = Date.now();
    const split = splitCreditsByExpiry(all, windowDays, now);
    const formatForUnit = (value) => formatUnits(value, unit);
    const expiryText = formatExpirySplitLine(split, formatForUnit);
    const detail = [
      all.length > 1 ? `共 ${all.length} 个资源包，${activeCount} 个有效` : null,
      ...all.map((pkg) => formatPackageLine(pkg, windowDays, now))
    ].filter(Boolean).join("\n");
    const quotaDetail = quotaText === null ? null : formatQuotaDetail(all);
    const poolSplitText = formatPoolSplitLine(
      all,
      formatForUnit,
      // Loomy 的另一个池就叫「永久积分」；Raccoon 的奖励/会员/充值三种池
      // 到期规则各不相同，不能统称永久。
      provider === "loomy" ? "永久" : "长期"
    );
    return React2.createElement(
      "div",
      { className: "dim-jh-metaRow" },
      // ⚠ 标签按单位走：ZCode 是 token，显示「Token」而不是「积分」。
      React2.createElement("dt", null, label),
      React2.createElement(
        "dd",
        {
          className: "dim-jh-creditValue",
          title: quotaDetail ?? (detail || void 0)
        },
        // ⚠️ 配额窗口显示**逐窗口百分比**而不是均值：均值（94.5）既不是上游给的数，
        // 在「额度」这个标签下更会被读成 94.5 个积分。见 quotaText 的注释。
        React2.createElement("strong", { className: "dim-jh-creditTotal" }, quotaText ?? total),
        // 当日池分桶（loomy / raccoon）：`formatPoolSplitLine` 已覆盖原先硬编码的
        // loomy 两池判据，且对 Raccoon 的「每日积分」同样成立（用户 2026-09-29 要求）。
        // ⚠ 数字按**包自身的单位**格式化（remote 的单位支持）：池可能来自
        // 不同 provider，不能假定都是积分。
        poolSplitText ? React2.createElement("span", { className: "dim-jh-creditPools" }, poolSplitText) : null,
        // 两个 buddy + TRAE + LobsterAI：按「会不会近期作废」分桶，与选号判据同一套规则。
        // 这条也解释了「锁定永久积分后为什么没有可用账号」——临时桶是 0。
        // ⚠️ 用词「长期」不是「永久」：这些积分都有到期日，只是较远（用户定）。
        // ⚠️ 与上面的池分桶互斥：有当日池的（loomy / raccoon）走池名分桶，
        // 没有的走到期时间分桶。
        !poolSplitText && expiryText ? React2.createElement("span", {
          className: "dim-jh-creditPools",
          title: `临时 = 距扣费截止不足 ${windowDays} 天（再不用就作废，优先消耗）；长期 = 其余积分（锁定永久积分后不参与消耗）。`
        }, expiryText) : null,
        // ⚠️ 配额单位（Gemini）下**不渲染**这句：它的两个「包」是 5 小时窗口与
        // 周窗口，不是资源包 —— 说「2/2 个资源包有效」既没信息量又误导
        // （用户报障原文：「不要渲染 / 2/2 个资源包有效」）。窗口自身的读数
        // 已由上面的 `quotaText` 逐条列出，这里再补一句纯属噪音。
        // ⚠️ 非配额单位（真正由多个资源包构成余额的 provider）行为**逐字不变**。
        quotaText === null && !poolSplitText && !expiryText && all.length > 1 ? React2.createElement(
          "span",
          { className: "dim-jh-creditPackages" },
          `${activeCount}/${all.length} 个资源包有效`
        ) : null,
        // 失效额度单独提示：它们仍在服务端响应里，但不计入上面的数字
        // ⚠️ 配额单位下恒不渲染（`expiredTotal` 就是 0），留着分支只为不改变
        // 其它 provider 的行为。
        balance.expiredTotal > 0 ? React2.createElement(
          "span",
          { className: "dim-jh-creditExpired" },
          `另有 ${formatUnits(balance.expiredTotal, unit)} 已失效`
        ) : null
      )
    );
  }
  function isAnonymousAccountId(accountId) {
    return String(accountId || "").indexOf("opencode-anon-") === 0;
  }
  function AccountCard({
    account,
    index,
    order,
    provider,
    onToggle,
    onDelete,
    onRetest,
    onReset,
    onTest,
    onClaimOnboarding,
    onboardingBusy,
    busy,
    credits,
    creditsLoading,
    showCredits,
    showPackageList,
    windowDays,
    showRateLimitActions,
    drag,
    // ⚠️ UTTS app Jet 桥的快捷行（签到 / 续期）：传了才渲染对应按钮。
    // 命名刻意避开既有 `onRetest` / `onReset`，续期也不叫 `onRefresh`（防撞名），
    // busy 标识用 `checkinBusyId` / `refreshBusyId`（无则恒显普通文字）。
    onCheckin,
    onRefreshAccount,
    checkinBusyId,
    refreshBusyId,
    // ⚠️ opencode 专属：传了才渲染「代理」「指纹」两个按钮（见按钮区注释）。
    // 前者额外需要 current 代理串，故签名与 onRetest 略有不同。
    onOpenProxy,
    onRotateFingerprint
  }) {
    const rateLimits = account.modelRateLimits ? Object.entries(account.modelRateLimits).filter(([, v]) => v > Date.now()) : [];
    const expired = typeof account.expiresAt === "number" && account.expiresAt > 0 && account.expiresAt <= Date.now();
    const hasAnyLimit = Boolean(account.modelRateLimits && Object.keys(account.modelRateLimits).length > 0);
    const dragProps = drag || {};
    const hoverPackages = credits?.balance?.packages;
    const packageTooltip = showPackageList && hoverPackages?.length ? formatPackageTooltip(hoverPackages, {
      format: (value) => formatUnits(value, hoverPackages.find((p) => p && p.unit)?.unit),
      now: Date.now()
    }) : null;
    const accountTitle = packageTooltip ?? (showPackageList && creditsLoading ? "资源包加载中…" : void 0);
    return React2.createElement(
      "div",
      {
        className: "dim-jh-accountCard",
        "data-enabled": account.enabled,
        "data-dragging": dragProps.isDragging ? "true" : void 0,
        // 插入位置指示：before 画在卡片上方，after 画在下方 —— 必须与
        // 实际落点一致，否则用户按指示拖放却得到不同结果。
        "data-dropBefore": dragProps.isDropTarget && dragProps.dropPosition !== "after" ? "true" : void 0,
        "data-dropAfter": dragProps.isDropTarget && dragProps.dropPosition === "after" ? "true" : void 0,
        // 整卡可拖：抓取柄之外也能拖，手感更好；但文本选择区（凭据/时间）
        // 仍可正常选中——HTML5 拖拽不会阻止选择。
        draggable: dragProps.enabled ? "true" : void 0,
        onDragStart: dragProps.onDragStart,
        onDragEnd: dragProps.onDragEnd,
        onDragOver: dragProps.onDragOver,
        onDrop: dragProps.onDrop
      },
      React2.createElement(
        "div",
        { className: "dim-jh-accountTop" },
        // 抓取柄 + 序号：序号即自动选号的优先级，让"拖到第一位"的含义明确。
        dragProps.enabled ? React2.createElement("span", {
          className: "dim-jh-dragHandle",
          title: "拖动以调整顺序（顺序即自动选号优先级）",
          "aria-hidden": "true"
        }, "⠿") : null,
        dragProps.enabled ? React2.createElement(
          "span",
          { className: "dim-jh-accountOrder", title: "自动选号优先级" },
          String((order ?? index ?? 0) + 1)
        ) : null,
        React2.createElement("span", {
          className: "dim-jh-accountStatus",
          "data-on": account.enabled ? "true" : "false",
          title: account.enabled ? "已启用" : "已停用",
          "aria-hidden": "true"
        }),
        React2.createElement(
          "span",
          {
            className: "dim-jh-accountName",
            // 资源包列表（剩余/总量 + 到期时间）。仅 buddy 系挂，见 packageTooltip。
            title: accountTitle
          },
          account.nickname || account.id
        ),
        React2.createElement("span", {
          className: "dim-jh-accountTag",
          "data-tone": account.enabled ? "on" : "off",
          // 同账号名：hover 出资源包列表（两处都挂，用户 hover 哪个都能看见）。
          title: accountTitle
        }, account.enabled ? "已启用" : "已停用"),
        // ⚠️ 匿名通道标记：它不需要 key、只用于免费模型，额度按**出口 IP** 计。
        // 标注出来是为了让用户知道「这几条不是登录账号」，
        // 以及为什么给它们配不同代理才会各自获得独立额度。
        provider === "opencode" && isAnonymousAccountId(account.id) ? React2.createElement("span", {
          className: "dim-jh-accountTag",
          "data-tone": "on",
          title: "匿名通道：无需 API key，仅用于免费模型。额度按出口 IP 计算 —— 给它单独配置代理，才会获得独立额度。"
        }, "匿名") : null
      ),
      React2.createElement(
        "dl",
        { className: "dim-jh-accountMeta" },
        React2.createElement(
          "div",
          { className: "dim-jh-metaRow" },
          React2.createElement("dt", null, "凭据"),
          React2.createElement("dd", null, React2.createElement("code", null, account.credentialRef))
        ),
        React2.createElement(
          "div",
          { className: "dim-jh-metaRow" },
          React2.createElement("dt", null, "有效期"),
          React2.createElement(
            "dd",
            { "data-tone": expired ? "warn" : void 0 },
            account.expiresAt ? `${formatTime(account.expiresAt) || "未知"}${account.refreshable ? " · 自动续期" : ""}` : "未知"
          )
        ),
        // 账号规格（目前只有 Gemini 有）：Pro / Free / Ultra。
        //
        // ⚠️ 判据是 `extra.accountTier` 有值才渲染 —— 取不到档位（上游改协议、
        // 网络失败、非 Gemini 的 provider）时**整行不出现**，不显示「未知」也不
        // 报错：它只是一栏附注信息，不该制造一条无法修复的提示。
        // `title` 放上游原文（`Google AI Pro（g1-pro-tier）`），面板只放得下一个词。
        credits?.extra?.accountTier ? React2.createElement(
          "div",
          { className: "dim-jh-metaRow" },
          React2.createElement("dt", null, "账号规格"),
          React2.createElement(
            "dd",
            { title: credits.extra.accountTier.title },
            credits.extra.accountTier.label
          )
        ) : null,
        // 不支持积分余额的 provider 不渲染该行：留着它只能显示「查询失败」，
        // 而失败原因是「这个 provider 根本没有此接口」——与其展示一条无法修复
        // 的错误，不如不展示。
        showCredits ? React2.createElement(CreditBalanceRow, {
          balance: credits?.balance ?? null,
          error: credits?.error,
          loading: creditsLoading,
          // 「临时 / 长期」分桶的窗口天数（buddy 系 + TRAE + LobsterAI 有值）。
          windowDays,
          // 池名分桶（loomy / raccoon）要按 provider 决定另一个池的标签。
          provider
        }) : null
      ),
      rateLimits.length > 0 ? React2.createElement(
        "div",
        { className: "dim-jh-rateLimits" },
        React2.createElement("span", { className: "dim-jh-rateLimitsLabel" }, "限额重置"),
        rateLimits.map(([modelId, resetAt]) => React2.createElement("span", {
          key: modelId,
          className: "dim-jh-ttlBadge",
          title: `模型 ${modelId}`
        }, `${modelId} · ${formatTime(resetAt)}`))
      ) : null,
      // 快捷行（签到 / 续期，UTTS app 的 Jet 桥能力）：放在 rateLimits 之后、
      // accountActions 之前 —— 它是高频操作，比低频的「重测/重置/删除」行更靠上。
      // ⚠️ 两个回调都是「存在才渲染」：能力表不支持签到的渠道（如 AutoClaw）
      // 只出现「续期」，不出现一个点了必然失败的按钮。
      onCheckin || onRefreshAccount ? React2.createElement(
        "div",
        { className: "dim-jh-quickActions" },
        onCheckin ? React2.createElement("button", {
          className: "dim-jh-btn",
          // 本账号任一快捷操作在途就禁用（两条 RPC 共用一次重入闸门）；
          // busy 是面板级（重测等）在途状态，一并禁用防并发。
          disabled: busy || checkinBusyId === account.id || refreshBusyId === account.id,
          onClick: () => onCheckin(account.id)
        }, checkinBusyId === account.id ? "签到中…" : "签到") : null,
        onRefreshAccount ? React2.createElement("button", {
          className: "dim-jh-btn",
          disabled: busy || checkinBusyId === account.id || refreshBusyId === account.id,
          onClick: () => onRefreshAccount(account.id)
        }, refreshBusyId === account.id ? "续期中…" : "续期") : null
      ) : null,
      React2.createElement(
        "div",
        { className: "dim-jh-accountActions" },
        // 新手任务（仅 Loomy）：一次性 10000 分，每号只能领一次。
        // 与「一键领取积分」（每日签到）是**不同**的操作，故独立按钮 ——
        // 混进「一键签到」会导致每天对已领完的账号发 8 个必然 alreadyCompleted 的请求。
        //
        // ⚠️ **只显示礼物图标**（用户报障：「领取新手任务」文字太长、按钮溢出行尾）。
        // 该行有 5 个按钮且 `flex-wrap: nowrap`，多一个宽按钮就会被挤出容器。
        // 文案移到 `title`（hover tooltip）与 `aria-label`（无障碍）里。
        onClaimOnboarding ? React2.createElement("button", {
          className: "dim-jh-btn dim-jh-iconBtn",
          // tooltip 说明「是什么 + 一次性 + 多少分」，因为图标本身不自解释
          title: "领取新手任务（合计 10000 积分，每个账号仅能领取一次）",
          "aria-label": "领取新手任务",
          disabled: busy || onboardingBusy,
          onClick: () => onClaimOnboarding(account.id)
        }, onboardingBusy ? "领取中…" : React2.createElement(
          "svg",
          {
            width: 14,
            height: 14,
            viewBox: "0 0 24 24",
            fill: "none",
            stroke: "currentColor",
            strokeWidth: 2,
            strokeLinecap: "round",
            strokeLinejoin: "round",
            "aria-hidden": "true",
            focusable: "false"
          },
          // 礼物盒：盒身 + 盖子 + 竖带 + 蝴蝶结
          React2.createElement("rect", { x: 3, y: 8, width: 18, height: 4, rx: 1 }),
          React2.createElement("path", { d: "M12 8v13" }),
          React2.createElement("path", { d: "M19 12v7a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2v-7" }),
          React2.createElement("path", { d: "M7.5 8a2.5 2.5 0 0 1 0-5A4.8 4.8 0 0 1 12 8a4.8 4.8 0 0 1 4.5-5 2.5 2.5 0 0 1 0 5" })
        )) : null,
        // 卡片级「重测 / 重置」同样只对会限流的 provider 有意义
        // （Loomy 不返回限流错误，故这两个按钮对它永远禁用 —— 直接不渲染）。
        //
        // ⚠️ 「测试」与上面两个**互不替代**，故用**独立**的 props 存在性开关
        // （与 `onOpenProxy` 同款约定）：重测只测已有标记的模型、没有标记时零请求；
        // 测试无条件真发一次。当前 Gemini 恰好只登记了测试、没登记重测 —— 若把
        // 两者绑在同一个开关上，就会出现「要么都渲染（重测对它无意义）、要么
        // 都不渲染（测试消失）」的二选一。
        //
        // ⚠️ 该行是 `flex-wrap: nowrap`，多一个按钮就可能溢出（见上方礼物图标
        // 按钮的注释）。故「测试」放在最前，且**只有**在 provider 同时登记了
        // 测试与重测时才会出现 6 个按钮 —— 当前没有任何 provider 两者皆登记。
        onTest ? React2.createElement("button", {
          className: "dim-jh-btn",
          title: TEST_HELP,
          // 不依赖 hasAnyLimit：测试的意义正是「没有标记时也能探活」。
          disabled: busy,
          onClick: () => onTest(account.id)
        }, "测试") : null,
        showRateLimitActions ? React2.createElement("button", {
          className: "dim-jh-btn",
          title: RETEST_HELP,
          disabled: busy || !hasAnyLimit,
          onClick: () => onRetest(account.id)
        }, "重测") : null,
        showRateLimitActions ? React2.createElement("button", {
          className: "dim-jh-btn",
          title: RESET_HELP,
          disabled: busy || !hasAnyLimit,
          onClick: () => onReset(account.id)
        }, "重置") : null,
        React2.createElement("button", {
          className: "dim-jh-btn",
          onClick: () => onToggle(account.id, !account.enabled)
        }, account.enabled ? "停用" : "启用"),
        // ⚠️ 仅 opencode：出口代理与指纹轮换是该 provider **独有**的账号维度
        // （其余 provider 没有这两项）。用 props 存在性开关而非
        // `provider === 'opencode'` 硬判断 —— 前者让 AccountCard 无需知道
        // provider 列表，也避免以后新增同类 provider 时漏改。
        // 位置在「停用」之后、「删除」之前：删除按钮带 data-kind='danger'，
        // 是这一行的视觉终点，不能被挤到中间。
        onOpenProxy ? React2.createElement("button", {
          className: "dim-jh-btn",
          // ⚠️ tooltip 必须解释「不设置会怎样」：看到「代理」按钮很容易
          // 当成锦上添花，实际不设 = 与其它账号共用同一出口（同一份额度）。
          title: account.opencodeProxy ? "出口代理：" + account.opencodeProxy + "（点击修改）" : "设置该账号的出口代理；不设置则与其它未设代理的账号共享本机出口 IP",
          onClick: () => onOpenProxy(account.id, account.opencodeProxy || "")
        }, "代理") : null,
        onRotateFingerprint ? React2.createElement("button", {
          className: "dim-jh-btn",
          title: "轮换该账号的指纹（生成新的 project id；用于怀疑多个账号被关联时）",
          disabled: busy,
          onClick: () => onRotateFingerprint(account.id)
        }, "指纹") : null,
        React2.createElement("button", {
          className: "dim-jh-btn",
          "data-kind": "danger",
          onClick: () => onDelete(account.id)
        }, "删除")
      )
    );
  }
  function ModelToggle({ model, busy, onToggle, onRestore }) {
    const dead = model.dead === true;
    return React2.createElement(
      "label",
      {
        className: "dim-jh-modelRow",
        "data-disabled": model.disabled ? "true" : "false",
        "data-dead": dead ? "true" : "false",
        title: dead ? `${model.id}（上游已下架，点击「重新显示」可恢复）` : model.id
      },
      React2.createElement(
        "span",
        { className: "dim-jh-modelInfo" },
        React2.createElement("strong", { className: "dim-jh-modelName" }, model.name || model.id),
        dead ? React2.createElement("span", { className: "dim-jh-modelDead" }, "已下架") : null,
        React2.createElement("code", { className: "dim-jh-modelId" }, model.id)
      ),
      dead ? React2.createElement("button", {
        type: "button",
        className: "dim-jh-modelRestore",
        disabled: busy,
        onClick: (event) => {
          event.preventDefault();
          event.stopPropagation();
          onRestore(model.id);
        }
      }, "重新显示") : React2.createElement("input", {
        type: "checkbox",
        className: "dim-jh-switch",
        role: "switch",
        checked: !model.disabled,
        disabled: busy,
        "aria-label": `${model.name || model.id} 是否在模型选择中显示`,
        onChange: () => onToggle(model.id, !model.disabled)
      })
    );
  }
  function ModelListPanel({ provider, rpcCall, onClose }) {
    const [models, setModels] = React2.useState(null);
    const [phase, setPhase] = React2.useState("loading");
    const [error, setError] = React2.useState(null);
    const [toggleError, setToggleError] = React2.useState(null);
    const [busyIds, setBusyIds] = React2.useState(() => /* @__PURE__ */ new Set());
    const [deadRestoreBusy, setDeadRestoreBusy] = React2.useState(false);
    const [bulkBusy, setBulkBusy] = React2.useState(false);
    const [query, setQuery] = React2.useState("");
    const [statusFilter, setStatusFilter] = React2.useState("all");
    const [groupToggles, setGroupToggles] = React2.useState({});
    const [groupBusy, setGroupBusy] = React2.useState(null);
    const mounted = React2.useRef(true);
    const load = React2.useCallback(async () => {
      setPhase("loading");
      setError(null);
      try {
        const res = await rpcCall("model.list", { provider });
        if (!mounted.current) return;
        setModels(res.models || []);
        setPhase("ready");
      } catch (caught) {
        if (!mounted.current) return;
        setError(caught?.message || "无法读取模型列表");
        setPhase("error");
      }
    }, [provider, rpcCall]);
    React2.useEffect(() => {
      mounted.current = true;
      void load();
      return () => {
        mounted.current = false;
      };
    }, [load]);
    React2.useEffect(() => {
      const onKeyDown = (event) => {
        if (event.key === "Escape") onClose();
      };
      document.addEventListener("keydown", onKeyDown);
      return () => document.removeEventListener("keydown", onKeyDown);
    }, [onClose]);
    const toggleModel = async (modelId, disabled) => {
      setBusyIds((prev) => new Set(prev).add(modelId));
      setToggleError(null);
      try {
        await rpcCall("model.setDisabled", { provider, modelId, disabled });
        if (!mounted.current) return;
        setModels((prev) => (prev || []).map((m) => m.id === modelId ? { ...m, disabled } : m));
      } catch (caught) {
        console.error("[jet-hub] toggle model failed:", caught);
        if (!mounted.current) return;
        setToggleError(caught?.message || "切换模型显示状态失败");
      } finally {
        if (mounted.current) {
          setBusyIds((prev) => {
            const next = new Set(prev);
            next.delete(modelId);
            return next;
          });
        }
      }
    };
    const restoreModel = async (modelId) => {
      setBusyIds((prev) => new Set(prev).add(modelId));
      try {
        await rpcCall("model.clearDead", { provider, modelId });
        if (!mounted.current) return;
        await load();
      } catch (caught) {
        console.error("[jet-hub] restore dead model failed:", caught);
        if (!mounted.current) return;
        setToggleError(caught?.message || "恢复模型失败");
      } finally {
        if (mounted.current) {
          setBusyIds((prev) => {
            const next = new Set(prev);
            next.delete(modelId);
            return next;
          });
        }
      }
    };
    const all = models || [];
    const hiddenCount = all.filter((m) => m.disabled).length;
    const deadCount = all.filter((m) => m.dead === true).length;
    const restoreAllDead = async () => {
      if (deadCount === 0) return;
      if (!confirm(`确认把 ${deadCount} 个「已下架」模型重新显示？若它们确实已被上游移除，使用时会再次失败并被重新隐藏。`)) return;
      setDeadRestoreBusy(true);
      try {
        await rpcCall("model.clearDead", { provider });
        if (!mounted.current) return;
        await load();
      } catch (caught) {
        console.error("[jet-hub] restore all dead models failed:", caught);
        if (!mounted.current) return;
        setToggleError(caught?.message || "恢复模型失败");
      } finally {
        if (mounted.current) setDeadRestoreBusy(false);
      }
    };
    const providerLabel2 = PROVIDERS.find((p) => p.id === provider)?.label || provider;
    const bulk = bulkButtonState(models, bulkBusy);
    const filtered = filterModels(all, { query, status: statusFilter });
    const filtering = isFilterActive({ query, status: statusFilter });
    const groups = groupModelsForDisplay(all, { query, status: statusFilter });
    const isGroupExpanded = (group) => groupExpanded(group, {
      filterActive: filtering,
      toggled: groupToggles[group.key]
    });
    const toggleGroup = (group) => {
      const next = !isGroupExpanded(group);
      setGroupToggles((prev) => ({ ...prev, [group.key]: next }));
    };
    const setGroupDisabled = async (group, disabled) => {
      const ids = group.models.map((model) => model.id);
      if (disabled && !confirm(
        `确认关闭「${group.label}」的 ${ids.length} 个模型？关闭后它们不再出现在对话框的模型选择里。`
      )) {
        return;
      }
      setGroupBusy(group.key);
      setToggleError(null);
      try {
        await rpcCall("model.setDisabledMany", { provider, modelIds: ids, disabled });
        if (!mounted.current) return;
        const changed = new Set(ids);
        setModels((prev) => (prev || []).map((m) => changed.has(m.id) ? { ...m, disabled } : m));
      } catch (caught) {
        console.error("[jet-hub] group toggle failed:", caught);
        if (!mounted.current) return;
        setToggleError(caught?.message || `本组批量${disabled ? "关闭" : "打开"}模型失败`);
      } finally {
        if (mounted.current) setGroupBusy(null);
      }
    };
    const resetFilters = () => {
      setQuery("");
      setStatusFilter("all");
    };
    const setAllDisabled = async (disabled) => {
      if (disabled && !confirm(`确认关闭全部 ${all.length} 个模型？关闭后它们不再出现在对话框的模型选择里。`)) {
        return;
      }
      setBulkBusy(true);
      setToggleError(null);
      try {
        await rpcCall("model.setAllDisabled", { provider, disabled });
        if (!mounted.current) return;
        setModels((prev) => (prev || []).map((m) => ({ ...m, disabled })));
      } catch (caught) {
        console.error("[jet-hub] bulk toggle failed:", caught);
        if (!mounted.current) return;
        setToggleError(caught?.message || `批量${disabled ? "关闭" : "打开"}模型失败`);
      } finally {
        if (mounted.current) setBulkBusy(false);
      }
    };
    const dialog = React2.createElement(
      "div",
      {
        // `--top`：顶部锚定。列表长度随搜索变化，若垂直居中会让弹窗整体上下跳动
        //（见 jet-hub-styles.js 中该修饰类的说明）。
        className: "dim-jh-modalOverlay dim-jh-modalOverlay--top",
        // 点击遮罩关闭；点击弹窗内部不关闭（stopPropagation 由内层容器负责）。
        onClick: (event) => {
          if (event.target === event.currentTarget) onClose();
        }
      },
      React2.createElement(
        "div",
        {
          className: "dim-jh-modal",
          role: "dialog",
          "aria-modal": "true",
          "aria-label": `${providerLabel2} 模型列表`
        },
        React2.createElement(
          "div",
          { className: "dim-jh-modalHead" },
          React2.createElement(
            "div",
            { className: "dim-jh-modalTitle" },
            React2.createElement("strong", null, "模型列表"),
            React2.createElement("span", { className: "dim-jh-modalSubtitle" }, providerLabel2),
            phase === "ready" ? React2.createElement(
              "span",
              { className: "dim-jh-modelPanelCount" },
              filtering ? `${filtered.length} / ${all.length} 个模型${hiddenCount > 0 ? `，已隐藏 ${hiddenCount} 个` : ""}` : `${all.length} 个模型${hiddenCount > 0 ? `，已隐藏 ${hiddenCount} 个` : ""}`
            ) : null
          ),
          React2.createElement(
            "div",
            { className: "dim-jh-modelPanelActions" },
            React2.createElement("button", {
              className: "dim-jh-btn",
              disabled: phase === "loading",
              onClick: () => void load()
            }, phase === "loading" ? "读取中…" : "刷新"),
            React2.createElement("button", {
              className: "dim-jh-btn",
              "data-kind": "primary",
              onClick: onClose
            }, "完成")
          )
        ),
        React2.createElement(
          "p",
          { className: "dim-jh-modalHint" },
          "关闭开关后该模型不再出现在对话框的模型选择里；其余模型（含服务端新增的）默认显示。"
        ),
        // ⚠️ 失效模型提示条：它们已被上游下架并从目录剔除，故单独说明「为什么看不见」
        // 并给出**批量**恢复入口（逐个点很麻烦，而误判往往不止一个）。
        phase === "ready" && deadCount > 0 ? React2.createElement(
          "div",
          { className: "dim-jh-modelDeadBar" },
          React2.createElement(
            "span",
            null,
            `有 ${deadCount} 个模型被上游下架，已自动从列表隐藏。`
          ),
          React2.createElement("button", {
            className: "dim-jh-btn",
            disabled: deadRestoreBusy,
            title: "清空该供应商的失效模型记录，让它们重新出现在列表里。",
            onClick: () => void restoreAllDead()
          }, deadRestoreBusy ? "恢复中…" : "全部重新显示")
        ) : null,
        // 搜索 + 状态筛选：Cline 的目录实测近 500 条，没有它就只能一页页翻。
        // 只在列表可用时渲染（载入中/出错时没有可筛的内容）。
        phase === "ready" && all.length > 0 ? React2.createElement(
          "div",
          { className: "dim-jh-modelFilterBar" },
          React2.createElement("input", {
            type: "search",
            className: "dim-jh-input dim-jh-modelSearch",
            placeholder: "搜索模型名或 id…",
            value: query,
            "aria-label": "搜索模型",
            onChange: (event) => setQuery(event.target.value)
          }),
          React2.createElement(
            "div",
            { className: "dim-jh-modelStatusFilter", role: "group", "aria-label": "按状态筛选" },
            [["all", "全部"], ["enabled", "已打开"], ["disabled", "已关闭"]].map(([value, label]) => React2.createElement("button", {
              key: value,
              className: "dim-jh-btn",
              "data-active": statusFilter === value ? "true" : "false",
              "aria-pressed": statusFilter === value ? "true" : "false",
              onClick: () => setStatusFilter(value)
            }, label))
          ),
          filtering ? React2.createElement("button", {
            className: "dim-jh-btn",
            title: "清空搜索词与状态筛选，恢复完整列表。",
            onClick: resetFilters
          }, "清空筛选") : null
        ) : null,
        // 批量工具条：只在列表可用时渲染。计数从标题挪到这里，避免与标题争宽。
        phase === "ready" && all.length > 0 ? React2.createElement(
          "div",
          { className: "dim-jh-modelBulkBar" },
          React2.createElement("button", {
            className: "dim-jh-btn",
            title: "打开该 Provider 的全部模型开关（含此前被关闭的）。",
            disabled: bulk.openAllDisabled,
            onClick: () => void setAllDisabled(false)
          }, bulkBusy ? "处理中…" : "打开全部"),
          React2.createElement("button", {
            className: "dim-jh-btn",
            title: "关闭该 Provider 的全部模型开关，关闭后它们不再出现在对话框的模型选择里。",
            disabled: bulk.closeAllDisabled,
            onClick: () => void setAllDisabled(true)
          }, bulkBusy ? "处理中…" : "关闭全部")
        ) : null,
        toggleError ? React2.createElement("div", {
          className: "dim-jh-probeNotice",
          "data-tone": "error",
          role: "alert"
        }, React2.createElement("div", null, toggleError)) : null,
        phase === "error" ? React2.createElement(
          "div",
          { className: "dim-jh-modalBody" },
          React2.createElement(
            "div",
            { className: "dim-jh-empty" },
            React2.createElement("p", null, error),
            React2.createElement("button", { className: "dim-jh-btn", onClick: () => void load() }, "重新读取")
          )
        ) : phase === "loading" ? React2.createElement(
          "div",
          { className: "dim-jh-modalBody" },
          React2.createElement("div", { className: "dim-jh-empty" }, "正在读取模型列表…")
        ) : all.length === 0 ? React2.createElement(
          "div",
          { className: "dim-jh-modalBody" },
          React2.createElement(
            "div",
            { className: "dim-jh-empty" },
            React2.createElement("p", null, "该 Provider 当前没有可用的模型。")
          )
        ) : filtered.length === 0 ? React2.createElement(
          "div",
          { className: "dim-jh-modalBody" },
          React2.createElement(
            "div",
            { className: "dim-jh-empty" },
            React2.createElement("p", null, "没有符合当前搜索与筛选条件的模型。"),
            React2.createElement("button", { className: "dim-jh-btn", onClick: resetFilters }, "清空筛选")
          )
        ) : React2.createElement(
          "div",
          { className: "dim-jh-modalBody" },
          React2.createElement(
            "div",
            { className: "dim-jh-modelList" },
            // **按计费/来源分组**渲染（订阅 / 免费 / Cline Cloud / 按量计费）。
            // 组内仍是全部筛选结果（**无渲染上限**）：改动前 478 条就是
            // 一次性全渲染、工作正常，加「显示更多」属于功能收缩。
            groups.map((group) => {
              const expanded = isGroupExpanded(group);
              const groupBulk = groupBulkStateFor(group, bulkBusy || groupBusy !== null);
              return React2.createElement(
                "div",
                {
                  key: group.key,
                  className: "dim-jh-modelGroup"
                },
                React2.createElement(
                  "div",
                  { className: "dim-jh-modelGroupHead" },
                  React2.createElement("button", {
                    className: "dim-jh-modelGroupToggle",
                    "aria-expanded": expanded ? "true" : "false",
                    title: group.hint,
                    onClick: () => toggleGroup(group)
                  }, `${expanded ? "▾" : "▸"} ${group.label}`),
                  React2.createElement(
                    "span",
                    { className: "dim-jh-modelGroupCount" },
                    group.counts.disabled > 0 ? `${group.counts.shown} 个 · 已关闭 ${group.counts.disabled}` : `${group.counts.shown} 个`
                  ),
                  React2.createElement("button", {
                    className: "dim-jh-btn dim-jh-modelGroupBtn",
                    title: `打开「${group.label}」的全部模型（不影响其它分组）`,
                    disabled: groupBulk.openAllDisabled,
                    onClick: () => void setGroupDisabled(group, false)
                  }, "全开"),
                  React2.createElement("button", {
                    className: "dim-jh-btn dim-jh-modelGroupBtn",
                    title: `关闭「${group.label}」的全部模型（不影响其它分组）`,
                    disabled: groupBulk.closeAllDisabled,
                    onClick: () => void setGroupDisabled(group, true)
                  }, "全关")
                ),
                expanded ? React2.createElement(
                  "div",
                  { className: "dim-jh-modelGroupBody" },
                  group.models.map((model) => React2.createElement(ModelToggle, {
                    key: model.id,
                    model,
                    // 批量提交期间一并禁用单条开关：黑名单是整体写入，
                    // 并发提交必然互相覆盖（后写的会丢掉先写的改动）。
                    // 分组批量也算「批量」，故一并计入。
                    busy: busyIds.has(model.id) || bulkBusy || groupBusy !== null,
                    onToggle: (id, disabled) => void toggleModel(id, disabled),
                    onRestore: (id) => void restoreModel(id)
                  }))
                ) : null
              );
            })
          )
        )
      )
    );
    return dialog;
  }
  function formatStamp(ts) {
    const at = new Date(Number(ts ?? 0));
    if (!Number.isFinite(at.getTime())) return "-";
    const pad = (part) => String(part).padStart(2, "0");
    const clock = `${pad(at.getHours())}:${pad(at.getMinutes())}:${pad(at.getSeconds())}`;
    if (at.toDateString() === (/* @__PURE__ */ new Date()).toDateString()) return clock;
    return `${pad(at.getMonth() + 1)}-${pad(at.getDate())} ${clock.slice(0, 5)}`;
  }
  function formatTokenCount2(value) {
    const n = Math.max(0, Number(value ?? 0));
    if (!Number.isFinite(n)) return "0";
    if (n < 1e5) return n.toLocaleString("en-US");
    if (n < 1e6) {
      const k = (n / 1e3).toFixed(1);
      return Number(k) >= 1e3 ? `${(n / 1e6).toFixed(1)}M` : `${k}k`;
    }
    return `${(n / 1e6).toFixed(1)}M`;
  }
  function formatMs(value) {
    const n = Number(value ?? 0);
    if (!Number.isFinite(n) || n <= 0) return "—";
    return n >= 1e3 ? `${(n / 1e3).toFixed(1)}s` : `${Math.round(n)}ms`;
  }
  function tokenParts(row) {
    if (row?.usageReported !== true) return null;
    const parts = [
      { key: "in", icon: "↓", value: Number(row.inputTokens ?? 0) },
      { key: "out", icon: "↑", value: Number(row.outputTokens ?? 0) }
    ];
    if (Number(row.cacheReadTokens ?? 0) > 0) {
      parts.push({ key: "cache", icon: "⚡", value: Number(row.cacheReadTokens) });
    }
    if (Number(row.reasoningTokens ?? 0) > 0) {
      parts.push({ key: "think", icon: "🧠", value: Number(row.reasoningTokens) });
    }
    return parts;
  }
  function tokenSummary(row) {
    const parts = tokenParts(row);
    if (parts === null) return "—";
    return parts.map((part) => `${part.icon}${formatTokenCount2(part.value)}`).join(" ");
  }
  function tokenSummaryExact(row) {
    const parts = tokenParts(row);
    if (parts === null) return "";
    return parts.map((part) => `${part.icon}${part.value.toLocaleString("en-US")}`).join(" ");
  }
  var TOKEN_LEGEND = "↓输入 ↑输出 ⚡缓存 🧠推理；— 表示网关本次未返回用量";
  function tokenTooltip(row) {
    const exact = tokenSummaryExact(row);
    return exact === "" ? TOKEN_LEGEND : `${exact}
${TOKEN_LEGEND}`;
  }
  function latencyParts(row) {
    const first = Number(row?.ttftMs ?? 0);
    const total = Number(row?.totalMs ?? 0);
    const rate = formatRowTokensPerSecond(row);
    const decodeMs = total - first;
    const rateTitle = rate !== "—" ? `官方 TPS 口径：输出 ${Number(row?.outputTokens ?? 0)} tok ÷ 生成阶段 ${(decodeMs / 1e3).toFixed(2)}s（首字之后到结束，含推理 token；官方取整：≥10 整数、<10 一位小数）` : row?.usageReported === true ? "缺少可用的生成阶段时长（首字时刻缺失或总耗时不大于首字）—— 速率不可测" : "网关本次未返回用量 —— 速率不可测";
    return { first, total, rate, rateTitle };
  }
  function StatusDot({ ok, title }) {
    return React2.createElement("span", {
      className: "dim-jh-quotaDot",
      "data-tone": ok ? "ok" : "error",
      title,
      "aria-label": title
    });
  }
  function ClineQuotaPanel({ rpcCall, onClose }) {
    const mounted = React2.useRef(true);
    React2.useEffect(() => () => {
      mounted.current = false;
    }, []);
    const [quota, setQuota] = React2.useState([]);
    const [quotaPhase, setQuotaPhase] = React2.useState("loading");
    const [quotaError, setQuotaError] = React2.useState("");
    const [viewIndex, setViewIndex] = React2.useState(0);
    const [logNonce, setLogNonce] = React2.useState(0);
    const [rows, setRows] = React2.useState([]);
    const [logPhase, setLogPhase] = React2.useState("idle");
    const [logError, setLogError] = React2.useState("");
    const loadQuota = React2.useCallback(async () => {
      setQuotaPhase("loading");
      setQuotaError("");
      try {
        const res = await rpcCall("cline.quota", { provider: "cline" });
        if (!mounted.current) return;
        const list = Array.isArray(res?.accounts) ? res.accounts : [];
        setQuota(list);
        setQuotaPhase("ready");
      } catch (caught) {
        if (!mounted.current) return;
        console.error("[jet-hub] load cline quota failed:", caught);
        setQuotaError(caught?.message || "订阅额度查询失败");
        setQuotaPhase("error");
      }
    }, [rpcCall]);
    React2.useEffect(() => {
      void loadQuota();
    }, [loadQuota]);
    const loadLog = React2.useCallback(async (accountId) => {
      setLogPhase("loading");
      setLogError("");
      try {
        const res = await rpcCall("cline.requestLog", { provider: "cline", accountId });
        if (!mounted.current) return;
        setRows(Array.isArray(res?.rows) ? res.rows : []);
      } catch (caught) {
        if (!mounted.current) return;
        console.error("[jet-hub] load cline request log failed:", caught);
        setLogError(caught?.message || "请求记录查询失败");
      } finally {
        if (mounted.current) setLogPhase("ready");
      }
    }, [rpcCall]);
    const viewAccount = quota.length > 0 ? quota[Math.min(viewIndex, quota.length - 1)] : void 0;
    const viewAccountId = viewAccount?.accountId ?? "";
    const stepView = (delta) => {
      if (quota.length === 0) return;
      setViewIndex((prev) => ((prev + delta) % quota.length + quota.length) % quota.length);
    };
    React2.useEffect(() => {
      if (viewAccountId === "") return;
      void loadLog(viewAccountId);
    }, [viewAccountId, logNonce, loadLog]);
    const renderQuota = () => {
      if (quotaPhase === "loading" && quota.length === 0) {
        return React2.createElement("div", { className: "dim-jh-empty" }, "正在读取订阅额度…");
      }
      if (quotaPhase === "error") {
        return React2.createElement(
          "div",
          { className: "dim-jh-empty", role: "alert" },
          React2.createElement("p", null, quotaError),
          React2.createElement("button", { className: "dim-jh-btn", onClick: () => void loadQuota() }, "重试")
        );
      }
      if (quota.length === 0) {
        return React2.createElement("div", { className: "dim-jh-empty" }, "尚未配置账号");
      }
      const entry = viewAccount;
      if (entry === void 0) return null;
      const pager = quota.length > 1 ? React2.createElement(
        "div",
        { className: "dim-jh-quotaPager" },
        React2.createElement("button", {
          className: "dim-jh-quotaArrow",
          title: "上一个账号",
          "aria-label": "上一个账号",
          onClick: () => stepView(-1)
        }, "‹"),
        React2.createElement(
          "div",
          { className: "dim-jh-quotaAccountName" },
          React2.createElement("span", {
            className: "dim-jh-quotaAccountLabel",
            title: entry.nickname || entry.accountId
          }, entry.nickname || entry.accountId),
          React2.createElement(
            "span",
            { className: "dim-jh-quotaIndex" },
            `第 ${viewIndex + 1} / ${quota.length} 个`
          )
        ),
        React2.createElement("button", {
          className: "dim-jh-quotaArrow",
          title: "下一个账号",
          "aria-label": "下一个账号",
          onClick: () => stepView(1)
        }, "›")
      ) : null;
      const windows = quotaWindowsOf(entry.windows);
      const body = entry.ok ? windows.length === 0 ? React2.createElement("div", { className: "dim-jh-quotaMuted" }, "官方未返回额度窗口。") : React2.createElement(
        "div",
        { className: "dim-jh-quotaWindows" },
        windows.map(([type, label, win]) => {
          const percent = quotaPercentValue(win.percentUsed);
          const tone = quotaTone(percent);
          const resetsIn = quotaResetsIn(win.resetsAt);
          return React2.createElement(
            "div",
            {
              key: `${type}`,
              className: "dim-jh-quotaWindow"
            },
            React2.createElement(
              "div",
              { className: "dim-jh-quotaWindowHead" },
              React2.createElement("span", { className: "dim-jh-quotaWindowName" }, label),
              // ⚠️ 18px 大字 + 夹取后取整(参考实现同款):百分比是这张卡
              // 唯一要读的数,值得占最大的字级。
              React2.createElement("span", {
                className: "dim-jh-quotaWindowPercent",
                "data-tone": tone
              }, formatQuotaPercent(percent))
            ),
            React2.createElement(
              "div",
              {
                className: "dim-jh-quotaBar",
                role: "progressbar",
                "aria-label": `${label} 已用`,
                "aria-valuenow": percent,
                "aria-valuemin": 0,
                "aria-valuemax": 100
              },
              React2.createElement("div", {
                className: "dim-jh-quotaBarFill",
                "data-tone": tone,
                style: { width: `${percent}%` }
              })
            ),
            resetsIn === "" ? null : React2.createElement("div", { className: "dim-jh-quotaReset" }, resetsIn)
          );
        })
      ) : React2.createElement(
        "div",
        { className: "dim-jh-quotaMuted" },
        `暂时读不到官方额度。${entry.error ? ` ${entry.error}` : ""}`
      );
      return React2.createElement("div", {
        // ⚠️ 按账号 id 作 key → 切账号时**重新挂载**该块(参考实现同款):
        // 否则进度条的 width 过渡会在两个账号的读数之间播放,
        // 看起来像"这个账号的额度在涨",而那只是动画。
        key: entry.accountId,
        className: "dim-jh-quotaGroup"
      }, pager, body);
    };
    const renderLog = () => {
      if (viewAccountId === "") return null;
      const head = React2.createElement(
        "h3",
        { className: "dim-jh-quotaSectionTitle" },
        "请求记录"
      );
      const hint = React2.createElement(
        "p",
        { className: "dim-jh-quotaLogHint" },
        "是本插件发出的请求流水（进程内存，重启后清空），不是官方账单 —— 官方渠道的消费在 Cline 自己的用量页里。"
      );
      const tableHead = React2.createElement(
        "thead",
        null,
        React2.createElement(
          "tr",
          null,
          React2.createElement("th", { className: "dim-jh-quotaDotCol" }, ""),
          React2.createElement("th", { className: "dim-jh-quotaWhenCol" }, "时间"),
          React2.createElement("th", null, "模型 / 上游"),
          React2.createElement("th", null, "TOKEN"),
          React2.createElement("th", null, "延迟")
        )
      );
      const colGroup = React2.createElement(
        "colgroup",
        null,
        React2.createElement("col", { className: "dim-jh-quotaDotCol" }),
        React2.createElement("col", { className: "dim-jh-quotaWhenCol" }),
        React2.createElement("col", null),
        React2.createElement("col", { className: "dim-jh-quotaTokensCol" }),
        React2.createElement("col", { className: "dim-jh-quotaLoadCol" })
      );
      const tableBody = React2.createElement(
        "tbody",
        null,
        rows.flatMap((row, index) => {
          const key = `${row.ts}-${index}`;
          const failed = row.error !== void 0;
          const figures = latencyParts(row);
          const label = String(row.model ?? "").replace(/^cline-pass\//, "");
          const cells = [
            React2.createElement("td", null, React2.createElement(StatusDot, {
              ok: !failed,
              title: failed ? String(row.error) : "成功"
            })),
            React2.createElement("td", { className: "dim-jh-quotaWhen" }, formatStamp(row.ts)),
            React2.createElement(
              "td",
              null,
              React2.createElement("span", {
                className: "dim-jh-quotaModel",
                title: String(row.model ?? "")
              }, label || "—"),
              // 上游与模型是两个维度:同模型可能由不同通道服务,拼一列会让
              // 「同名不同上游」的行无法区分。
              React2.createElement(
                "span",
                { className: "dim-jh-quotaMeta" },
                React2.createElement(
                  "span",
                  { className: "dim-jh-quotaTag" },
                  row.upstream || "—"
                )
              )
            ),
            React2.createElement("td", {
              className: "dim-jh-quotaTokens",
              // ⚠️ tooltip = **精确**数字 + 图例:单元格里超过 10 万会缩写成 k/M,
              // tooltip 是唯一保留个位的地方;`—` 的含义也只在图例里解释
              // (参考实现同款)。
              title: tokenTooltip(row)
            }, tokenSummary(row)),
            React2.createElement(
              "td",
              { className: "dim-jh-quotaLoad" },
              React2.createElement(
                "span",
                { className: "dim-jh-quotaLoadRow" },
                React2.createElement("span", { className: "dim-jh-quotaLoadKey" }, "首字"),
                React2.createElement("span", null, formatMs(figures.first))
              ),
              React2.createElement(
                "span",
                { className: "dim-jh-quotaLoadRow" },
                React2.createElement("span", { className: "dim-jh-quotaLoadKey" }, "总耗时"),
                React2.createElement("span", null, formatMs(figures.total))
              ),
              React2.createElement(
                "span",
                { className: "dim-jh-quotaLoadRow", title: figures.rateTitle },
                React2.createElement("span", { className: "dim-jh-quotaLoadKey" }, "输出速度"),
                React2.createElement("span", null, figures.rate)
              )
            )
          ];
          const rowEl = React2.createElement("tr", {
            key,
            "data-error": failed ? "error" : void 0,
            // 整行 restate 一遍事实(含 token 与速率):截图或复制时信息不丢
            // (参考实现同款)。⚠️ 这里用**有界**的 tokenSummary(不是精确值),
            // 与参考实现一致:精确个位只出现在 TOKEN 单元格的 tooltip 里。
            title: [
              String(row.model ?? ""),
              `${row.upstream || "—"} · ${tokenSummary(row)}`,
              `首字 ${formatMs(figures.first)} · 总耗时 ${formatMs(figures.total)} · 输出速度 ${figures.rate}`,
              // 推理强度:有值时才有这一行(参考实现同款)。
              row.effort ? `推理强度 ${row.effort}` : "",
              failed ? String(row.error) : ""
            ].filter((line) => line !== "").join("\n")
          }, ...cells);
          if (failed) {
            return [
              rowEl,
              React2.createElement(
                "tr",
                { key: `${key}-err`, "data-error": "error" },
                React2.createElement("td", null, ""),
                React2.createElement("td", null, ""),
                React2.createElement("td", {
                  className: "dim-jh-quotaError",
                  colSpan: 3,
                  title: String(row.error)
                }, String(row.error))
              )
            ];
          }
          return [rowEl];
        })
      );
      const table = React2.createElement(
        "table",
        { className: "dim-jh-quotaTable" },
        colGroup,
        tableHead,
        tableBody
      );
      const wrap = React2.createElement(
        "div",
        { className: "dim-jh-quotaTableWrap" },
        table
      );
      const empty = React2.createElement(
        "div",
        { className: "dim-jh-empty" },
        logError === "" ? "暂无记录。（面板打开后新发起的请求才会出现在这里）" : logError
      );
      const body = logPhase === "loading" ? React2.createElement("div", { className: "dim-jh-empty" }, "正在读取请求记录…") : rows.length === 0 ? empty : wrap;
      return React2.createElement(
        "div",
        { className: "dim-jh-quotaLog" },
        head,
        hint,
        body
      );
    };
    const quotaSubtitle = () => {
      if (quota.length > 1) return `Cline · ${quota.length} 个账号`;
      const only = quota[0];
      if (only === void 0) return "Cline";
      return `Cline · 账号 ${only.nickname || only.accountId}`;
    };
    return React2.createElement(
      "div",
      {
        className: "dim-jh-modalOverlay dim-jh-modalOverlay--top",
        onClick: (event) => {
          if (event.target === event.currentTarget) onClose();
        }
      },
      React2.createElement(
        "div",
        {
          className: "dim-jh-modal",
          role: "dialog",
          "aria-modal": "true",
          "aria-label": "Cline 订阅额度"
        },
        React2.createElement(
          "div",
          { className: "dim-jh-modalHead" },
          React2.createElement(
            "div",
            { className: "dim-jh-modalTitle" },
            React2.createElement("strong", null, "订阅额度"),
            React2.createElement("span", { className: "dim-jh-modalSubtitle" }, quotaSubtitle())
          ),
          React2.createElement(
            "div",
            { className: "dim-jh-modelPanelActions" },
            React2.createElement("button", {
              className: "dim-jh-btn",
              disabled: quotaPhase === "loading",
              title: "重新查询全部账号的订阅额度窗口，并重读当前账号的请求记录。",
              onClick: () => {
                void loadQuota();
                setLogNonce((n) => n + 1);
              }
            }, quotaPhase === "loading" ? "读取中…" : "刷新"),
            React2.createElement("button", {
              className: "dim-jh-btn",
              "data-kind": "primary",
              onClick: onClose
            }, "完成")
          )
        ),
        React2.createElement(
          "p",
          { className: "dim-jh-modalHint" },
          "额度窗口来自 Cline 官方网关；请求记录是本插件自己发出的请求流水（重启后清空）。两者与账号卡片上的「积分」是三份不同的读数：积分答「还剩多少」，额度答「各时间窗用掉百分之几」，记录答「每一笔发了多久、花了多少 token」。"
        ),
        // ⚠️ 内容**必须**放进 .dim-jh-modalBody（flex:1; min-height:0; overflow-y:auto）。
        // .dim-jh-modal 是 max-height 有限的 flex **列**容器，子项默认不可收缩，
        // 内容直接铺在里面就会**画出弹窗边界之外** —— 首版正是漏了这一层：
        // 额度卡 + 请求表把弹窗撑破，看起来像「弹窗位置不对、内容显示不对」。
        // 模型列表弹窗的内容同样在 modalBody 里（见其 error/loading/empty 分支）。
        React2.createElement(
          "div",
          { className: "dim-jh-modalBody" },
          renderQuota(),
          renderLog()
        )
      )
    );
  }
  function TokenLedgerPanel({ rpcCall, onClose }) {
    const mounted = React2.useRef(true);
    React2.useEffect(() => () => {
      mounted.current = false;
    }, []);
    const [snapshot, setSnapshot] = React2.useState(null);
    const [phase, setPhase] = React2.useState("loading");
    const [error, setError] = React2.useState("");
    const [selected, setSelected] = React2.useState(null);
    const [historyRange, setHistoryRange] = React2.useState("7d");
    const [history, setHistory] = React2.useState(null);
    const [historyPhase, setHistoryPhase] = React2.useState("idle");
    const [expandedDay, setExpandedDay] = React2.useState(null);
    const load = React2.useCallback(async () => {
      setPhase("loading");
      setError("");
      try {
        const res = await rpcCall("usage.tokenLedger", {});
        if (!mounted.current) return;
        setSnapshot(res?.snapshot ?? null);
        setPhase("ready");
      } catch (caught) {
        if (!mounted.current) return;
        console.error("[jet-hub] load token ledger failed:", caught);
        setError(caught?.message || "Token 用量读取失败");
        setPhase("error");
      }
    }, [rpcCall]);
    React2.useEffect(() => {
      void load();
    }, [load]);
    const loadHistory = React2.useCallback(async (rangeKey) => {
      setHistoryPhase("loading");
      try {
        const days = rangeDaysOf(rangeKey);
        const res = await rpcCall("usage.tokenLedgerHistory", days > 0 ? { sinceDays: days } : {});
        if (!mounted.current) return;
        setHistory(res ?? null);
        setHistoryPhase("ready");
      } catch (caught) {
        if (!mounted.current) return;
        console.error("[jet-hub] load token ledger history failed:", caught);
        setHistory(null);
        setHistoryPhase("error");
      }
    }, [rpcCall]);
    React2.useEffect(() => {
      void loadHistory(historyRange);
    }, [historyRange, loadHistory]);
    const channels = React2.useMemo(() => {
      const list = Array.isArray(snapshot?.channels) ? [...snapshot.channels] : [];
      list.sort((a, b) => channelOrder(a?.channel, b?.channel));
      return selected === null ? list : list.filter((c) => c?.channel === selected);
    }, [snapshot, selected]);
    const perfCell = (row) => React2.createElement("span", {
      className: "dim-jh-ledgerPerf",
      title: avgPerfTooltip()
    }, avgPerfText(row));
    const renderModelRow = (modelRow, key) => React2.createElement(
      "div",
      {
        key,
        className: "dim-jh-ledgerModel"
      },
      React2.createElement("span", { className: "dim-jh-ledgerModelName", title: modelRow.model }, modelRow.model),
      React2.createElement(
        "span",
        { className: "dim-jh-ledgerModelReq", title: "请求数（含失败）" },
        modelRow.requests + " 次"
      ),
      perfCell(modelRow),
      React2.createElement(
        "span",
        { className: "dim-jh-ledgerModelTokens" },
        tokenSummaryText(modelRow)
      )
    );
    const renderSummary = () => {
      if (phase === "loading" && snapshot === null) {
        return React2.createElement("div", { className: "dim-jh-empty" }, "正在读取 Token 用量…");
      }
      if (phase === "error") {
        return React2.createElement(
          "div",
          { className: "dim-jh-empty", role: "alert" },
          React2.createElement("p", null, error),
          React2.createElement("button", { className: "dim-jh-btn", onClick: () => void load() }, "重试")
        );
      }
      if (!hasAnyData(snapshot)) {
        return React2.createElement(
          "div",
          { className: "dim-jh-empty" },
          React2.createElement("p", null, "还没有任何请求记录。"),
          React2.createElement(
            "p",
            null,
            "本插件在每次模型请求结束时记录 token 用量（含直连与网关两条通道），重启 DSH 后清空。"
          )
        );
      }
      const present = Array.from(new Set((snapshot?.channels ?? []).map((c) => c?.channel)));
      const filterRow = present.length > 1 ? React2.createElement(
        "div",
        { className: "dim-jh-ledgerFilters", key: "filters" },
        [null, ...present].map((key) => React2.createElement("button", {
          key: key === null ? "all" : key,
          className: "dim-jh-btn dim-jh-ledgerFilterBtn",
          "data-active": selected === key ? "true" : "false",
          onClick: () => setSelected(key)
        }, key === null ? "全部" : channelLabel(key)))
      ) : null;
      const renderProviderBody = (providerRow) => {
        const accounts = providerRow.accounts ?? [];
        if (accounts.length > 1) {
          return accounts.map((accountRow) => React2.createElement(
            "div",
            {
              key: providerRow.provider + "#" + accountRow.accountId,
              className: "dim-jh-ledgerAccount"
            },
            React2.createElement(
              "div",
              { className: "dim-jh-ledgerAccountHead" },
              React2.createElement(
                "span",
                { className: "dim-jh-ledgerAccountName", title: accountRow.accountId },
                accountLabel(accountRow.accountId)
              ),
              perfCell(accountRow.totals),
              React2.createElement(
                "span",
                { className: "dim-jh-ledgerAccountSum" },
                tokenSummaryText(accountRow.totals)
              )
            ),
            accountRow.models.map((modelRow) => renderModelRow(modelRow, modelRow.model))
          ));
        }
        return accounts.flatMap((accountRow) => accountRow.models.map((modelRow) => renderModelRow(modelRow, providerRow.provider + "#" + modelRow.model)));
      };
      return React2.createElement(
        React2.Fragment,
        null,
        filterRow,
        // 汇总卡：渠道 → provider → 账号 → 模型（第 2 期加了账号层）。
        // provider / 账号行带自身小计（与模型行同列对齐）。
        channels.map((channelRow) => React2.createElement(
          "div",
          {
            key: channelRow.channel,
            className: "dim-jh-ledgerCard"
          },
          React2.createElement(
            "div",
            { className: "dim-jh-ledgerCardHead" },
            React2.createElement("strong", null, channelCardTitle(channelRow)),
            perfCell(channelRow.totals),
            React2.createElement(
              "span",
              { className: "dim-jh-ledgerCardSum" },
              tokenSummaryText(channelRow.totals)
            )
          ),
          channelRow.providers.map((providerRow) => React2.createElement(
            "div",
            {
              key: providerRow.provider,
              className: "dim-jh-ledgerProvider"
            },
            React2.createElement(
              "div",
              { className: "dim-jh-ledgerProviderHead" },
              React2.createElement("span", { className: "dim-jh-ledgerProviderName" }, providerRow.provider),
              perfCell(providerRow.totals),
              React2.createElement(
                "span",
                { className: "dim-jh-ledgerProviderSum" },
                tokenSummaryText(providerRow.totals)
              )
            ),
            renderProviderBody(providerRow)
          ))
        ))
      );
    };
    const entries = Array.isArray(snapshot?.entries) ? snapshot.entries : [];
    const renderEntries = () => {
      if (entries.length === 0) return null;
      return React2.createElement(
        "table",
        { className: "dim-jh-ledgerTable", key: "entries" },
        React2.createElement(
          "thead",
          null,
          React2.createElement(
            "tr",
            null,
            ["时间", "渠道", "模型", "TOKEN", "首字", "速率", "耗时"].map((h) => React2.createElement("th", { key: h }, h))
          )
        ),
        React2.createElement("tbody", null, entries.map((entry, index) => React2.createElement(
          "tr",
          {
            key: entry.ts + "-" + index,
            title: entry.error !== void 0 ? "失败：" + entry.error : void 0
          },
          React2.createElement("td", null, formatEntryTime(entry.ts)),
          React2.createElement("td", null, channelLabel(entry.channel)),
          React2.createElement("td", { className: "dim-jh-ledgerEntryModel" }, entry.model),
          React2.createElement("td", null, entryTokenText(entry)),
          React2.createElement("td", null, formatTtft(entry)),
          React2.createElement("td", null, formatTps(entry)),
          React2.createElement("td", null, formatDuration(entry.durationMs))
        )))
      );
    };
    const historyDays = Array.isArray(history?.history) ? history.history : [];
    const bars = trendBars(historyDays);
    const maxTokens = bars.reduce((max, b) => Math.max(max, b.tokens), 0);
    const renderHistory = () => {
      const rangeRow = React2.createElement(
        "div",
        { className: "dim-jh-ledgerFilters", key: "historyRanges" },
        HISTORY_RANGES.map((r) => React2.createElement("button", {
          key: r.key,
          className: "dim-jh-btn dim-jh-ledgerFilterBtn",
          "data-active": historyRange === r.key ? "true" : "false",
          onClick: () => {
            setHistoryRange(r.key);
            setExpandedDay(null);
          }
        }, r.label)),
        React2.createElement(
          "span",
          { className: "dim-jh-ledgerPerf", style: { marginLeft: "auto" } },
          history !== null && history.totals !== void 0 ? "合计 ↓" + formatTokenCount2(history.totals.inputTokens) + " ↑" + formatTokenCount2(history.totals.outputTokens) + " · " + (history.totals.requests ?? 0) + " 次请求" : ""
        )
      );
      const body = historyPhase === "loading" ? React2.createElement("div", { className: "dim-jh-empty", key: "histLoading" }, "正在读取历史用量…") : historyPhase === "error" ? React2.createElement("div", { className: "dim-jh-empty", key: "histError" }, "历史用量读取失败（不影响明细）。") : bars.length === 0 ? React2.createElement(
        "div",
        { className: "dim-jh-empty", key: "histEmpty" },
        "还没有历史用量。每笔请求都会按日累计入盘 —— 今天发起的请求明天就能在这里看到。"
      ) : React2.createElement(
        React2.Fragment,
        { key: "histBody" },
        // 趋势柱状图：每根柱 = 一天，点击下钻该日聚合树（再点收起）。
        // ⚠️ 柱高必须传 `bars.length`：单日窗口下不传根数时最大柱恒 100%，
        // 撑满容器成「白块」（见 barHeightPercent 的缺陷注释）。
        React2.createElement(
          "div",
          { className: "dim-jh-ledgerTrend" },
          bars.map((b) => React2.createElement(
            "div",
            {
              key: b.day,
              className: "dim-jh-ledgerTrendCol",
              title: b.day + "：↓↑ " + formatTokenCount2(b.tokens) + " · " + b.requests + " 次",
              onClick: () => setExpandedDay((prev) => prev === b.day ? null : b.day)
            },
            React2.createElement("div", {
              className: "dim-jh-ledgerTrendBar",
              "data-today": b.isToday ? "true" : "false",
              "data-active": expandedDay === b.day ? "true" : "false",
              style: { height: barHeightPercent(b.tokens, maxTokens, bars.length) + "%" }
            }),
            // 日期短标签常驻显示：光靠色块无法区分哪天（tooltip 要 hover）。
            React2.createElement("div", { className: "dim-jh-ledgerTrendLabel" }, barDayLabel(b.day))
          )),
          ...expandedDay !== null ? historyDays.filter((d) => d?.day === expandedDay).map((dayRow) => renderDayTree(dayRow)) : []
        )
      );
      return React2.createElement(
        "div",
        { className: "dim-jh-ledgerSection", key: "history" },
        React2.createElement(
          "div",
          { className: "dim-jh-ledgerSectionTitle" },
          historyTitle(historyRange)
        ),
        rangeRow,
        body
      );
    };
    const renderDayTree = (dayRow) => React2.createElement(
      "div",
      { key: "day-" + dayRow.day, className: "dim-jh-ledgerCard" },
      React2.createElement(
        "div",
        { className: "dim-jh-ledgerCardHead" },
        React2.createElement("strong", null, dayRow.day + " · " + (dayRow.totals?.requests ?? 0) + " 次请求"),
        perfCell(dayRow.totals),
        React2.createElement(
          "span",
          { className: "dim-jh-ledgerCardSum" },
          tokenSummaryText(dayRow.totals)
        )
      ),
      dayRow.channels.map((channelRow) => React2.createElement(
        "div",
        { key: channelRow.channel, className: "dim-jh-ledgerProvider" },
        React2.createElement(
          "div",
          { className: "dim-jh-ledgerProviderHead" },
          React2.createElement("span", { className: "dim-jh-ledgerProviderName" }, channelLabel(channelRow.channel)),
          perfCell(channelRow.totals),
          React2.createElement(
            "span",
            { className: "dim-jh-ledgerProviderSum" },
            tokenSummaryText(channelRow.totals)
          )
        ),
        channelRow.providers.map((providerRow) => React2.createElement(
          "div",
          {
            key: providerRow.provider,
            className: "dim-jh-ledgerAccount"
          },
          React2.createElement(
            "div",
            { className: "dim-jh-ledgerAccountHead" },
            React2.createElement("span", { className: "dim-jh-ledgerAccountName" }, providerRow.provider),
            perfCell(providerRow.totals),
            React2.createElement(
              "span",
              { className: "dim-jh-ledgerAccountSum" },
              tokenSummaryText(providerRow.totals)
            )
          ),
          (providerRow.accounts ?? []).length > 1 ? providerRow.accounts.map((accountRow) => React2.createElement(
            "div",
            {
              key: providerRow.provider + "#" + accountRow.accountId,
              className: "dim-jh-ledgerAccount",
              style: { padding: "0 0 0 8px" }
            },
            React2.createElement(
              "div",
              { className: "dim-jh-ledgerAccountHead" },
              React2.createElement(
                "span",
                { className: "dim-jh-ledgerAccountName" },
                accountLabel(accountRow.accountId)
              ),
              React2.createElement(
                "span",
                { className: "dim-jh-ledgerAccountSum" },
                tokenSummaryText(accountRow.totals)
              )
            ),
            accountRow.models.map((modelRow) => renderModelRow(modelRow, accountRow.accountId + "#" + modelRow.model))
          )) : (providerRow.accounts ?? []).flatMap((accountRow) => accountRow.models.map((modelRow) => renderModelRow(modelRow, providerRow.provider + "#" + modelRow.model)))
        ))
      ))
    );
    return React2.createElement(
      "div",
      {
        className: "dim-jh-modalOverlay dim-jh-modalOverlay--top",
        onClick: (event) => {
          if (event.target === event.currentTarget) onClose();
        }
      },
      React2.createElement(
        "div",
        {
          className: "dim-jh-modal",
          role: "dialog",
          "aria-modal": "true",
          "aria-label": "Token 用量"
        },
        React2.createElement(
          "div",
          { className: "dim-jh-modalHead" },
          React2.createElement(
            "div",
            { className: "dim-jh-modalTitle" },
            React2.createElement("strong", null, "Token 用量"),
            React2.createElement("span", { className: "dim-jh-modalSubtitle" }, ledgerSubtitle())
          ),
          React2.createElement(
            "div",
            { className: "dim-jh-modelPanelActions" },
            React2.createElement("button", {
              className: "dim-jh-btn",
              disabled: phase === "loading",
              title: "重新读取本机 Token 账本。",
              onClick: () => void load()
            }, phase === "loading" ? "读取中…" : "刷新"),
            React2.createElement("button", {
              className: "dim-jh-btn",
              "data-kind": "primary",
              onClick: onClose
            }, "完成")
          )
        ),
        React2.createElement(
          "p",
          { className: "dim-jh-modalHint" },
          "按渠道（直连 / 网关）、供应商、账号与模型统计本机发出的每笔模型请求。↓输入 ↑输出 ⚡缓存 ✎缓存写入 🧠推理；— 表示该笔未收到用量（失败或中断）。明细含「首字用时」与「输出速率」（首块之后 → 结束，官方口径）；汇总行的「均值」只对有实测值的请求平均（速率要求解码时长 ≥ 100ms，过短视为不可测；且为算术平均，个别极快/极慢的离群请求会明显拉高或拉低该值 —— 悬停均值格看口径）。明细保留最近 500 笔（重启清空）；按日累计已存盘（token-ledger.json）。"
        ),
        React2.createElement(
          "div",
          { className: "dim-jh-modalBody" },
          renderHistory(),
          renderSummary(),
          renderEntries()
        )
      )
    );
  }
  function ProviderPanel({ provider, rpcCall }) {
    const [accounts, setAccounts] = React2.useState([]);
    const [phase, setPhase] = React2.useState("loading");
    const [error, setError] = React2.useState(null);
    const [creating, setCreating] = React2.useState(false);
    const [zcodeProvider, setZcodeProvider] = React2.useState("bigmodel");
    const [pendingLogin, setPendingLogin] = React2.useState(void 0);
    const [probeBusy, setProbeBusy] = React2.useState(null);
    const [probeNotice, setProbeNotice] = React2.useState(null);
    const [credits, setCredits] = React2.useState({});
    const [creditsLoading, setCreditsLoading] = React2.useState(false);
    const [expiryWindowDays, setExpiryWindowDays] = React2.useState(null);
    const [loginUrlForManual, setLoginUrlForManual] = React2.useState(null);
    const [loginLinkCopied, setLoginLinkCopied] = React2.useState(null);
    const [proxyModal, setProxyModal] = React2.useState(null);
    const [keyModal, setKeyModal] = React2.useState(null);
    const keyInputRef = React2.useRef(null);
    const [draggingId, setDraggingId] = React2.useState(null);
    const [dropTargetId, setDropTargetId] = React2.useState(null);
    const [dropPosition, setDropPosition] = React2.useState("before");
    const [reordering, setReordering] = React2.useState(false);
    const [reorderError, setReorderError] = React2.useState(null);
    const mounted = React2.useRef(true);
    const accountsRef = React2.useRef([]);
    const pollRef = React2.useRef(0);
    const claimInFlightRef = React2.useRef(false);
    const loadAccounts = React2.useCallback(async () => {
      setPhase("loading");
      setError(null);
      try {
        const res = await rpcCall("account.list", { provider });
        if (!mounted.current) return;
        const list = res.accounts || [];
        accountsRef.current = list;
        setAccounts(list);
        setPhase("ready");
      } catch (caught) {
        if (!mounted.current) return;
        setError(caught?.message || "无法读取账号列表");
        setPhase("error");
      }
    }, [provider, rpcCall]);
    const canLoadCredits = supportsCreditBalance(provider);
    const supportsCredits = supportsDailyCheckin(provider);
    const supportsGrowthTasks = GROWTH_TASK_PROVIDERS.has(provider);
    const canShowSubscriptionQuota = supportsSubscriptionQuota(provider);
    const loadCredits = React2.useCallback(async () => {
      if (!canLoadCredits) return;
      setCreditsLoading(true);
      try {
        const res = await rpcCall("credits.balances", { provider });
        if (!mounted.current) return;
        const next = {};
        for (const item of res.accounts || []) {
          next[item.accountId] = { balance: item.balance, error: item.error, extra: item.extra };
        }
        setCredits(next);
        setExpiryWindowDays(res?.windowDays ?? null);
      } catch (caught) {
        console.error("[jet-hub] load credits failed:", caught);
        if (!mounted.current) return;
        const snapshot = accountsRef.current;
        setCredits((prev) => {
          const next = { ...prev };
          for (const account of snapshot) {
            next[account.id] = { balance: null, error: caught?.message || "积分查询失败" };
          }
          return next;
        });
      } finally {
        if (mounted.current) setCreditsLoading(false);
      }
    }, [provider, rpcCall, canLoadCredits]);
    React2.useEffect(() => {
      mounted.current = true;
      void loadAccounts();
      if (canLoadCredits) void loadCredits();
      return () => {
        mounted.current = false;
        if (pollRef.current !== 0) {
          clearInterval(pollRef.current);
          pollRef.current = 0;
        }
        claimInFlightRef.current = false;
      };
    }, [provider]);
    const [claiming, setClaiming] = React2.useState(false);
    const [accountBusy, setAccountBusy] = React2.useState(null);
    const [claimNotice, setClaimNotice] = React2.useState(null);
    const [onboarding, setOnboarding] = React2.useState(null);
    const [onboardingLoading, setOnboardingLoading] = React2.useState(false);
    const [onboardingNotice, setOnboardingNotice] = React2.useState(null);
    const canClaimOnboarding = supportsOnboardingTasks(provider);
    const canCheckinAccount = supportsDailyCheckin(provider);
    const canRefreshAccount = true;
    const canLockPermanent = supportsPermanentLock(provider);
    const lockCopy = permanentLockCopy(provider, expiryWindowDays);
    const [permanentLocked, setPermanentLocked] = React2.useState(false);
    const [lockBusy, setLockBusy] = React2.useState(false);
    const [lockNotice, setLockNotice] = React2.useState(null);
    React2.useEffect(() => {
      if (!canLockPermanent) return void 0;
      let alive = true;
      void (async () => {
        try {
          const res = await rpcCall("credits.permanentLock", { provider });
          if (!alive) return;
          setPermanentLocked(res?.locked === true);
          setExpiryWindowDays(res?.windowDays ?? null);
        } catch (caught) {
          console.error("[jet-hub] load permanent lock failed:", caught);
        }
      })();
      return () => {
        alive = false;
      };
    }, [canLockPermanent, provider]);
    const togglePermanentLock = async () => {
      if (!canLockPermanent) return;
      const next = !permanentLocked;
      setLockBusy(true);
      setLockNotice(null);
      try {
        const res = await rpcCall("credits.permanentLock", { provider, locked: next });
        if (!mounted.current) return;
        setPermanentLocked(res?.locked === true);
        setExpiryWindowDays(res?.windowDays ?? null);
        setLockNotice({
          tone: "ok",
          text: next ? lockCopy.lockedNotice : lockCopy.unlockedNotice
        });
      } catch (caught) {
        console.error("[jet-hub] toggle permanent lock failed:", caught);
        if (!mounted.current) return;
        setLockNotice({ tone: "error", text: `操作失败：${caught?.message || "未知错误"}` });
      } finally {
        if (mounted.current) setLockBusy(false);
      }
    };
    const claimOnboarding = async (accountId) => {
      if (!canClaimOnboarding) return;
      setOnboardingLoading(true);
      setOnboardingNotice(null);
      try {
        const res = await rpcCall("onboarding.claim", { provider, accountId });
        const parts = [];
        if (res.claimed.length > 0) {
          const gained = res.claimed.reduce((sum, item) => sum + item.points, 0);
          parts.push(`本次领取 ${res.claimed.length} 个任务（+${gained} 积分）`);
        }
        if (res.skipped.length > 0) parts.push(`${res.skipped.length} 个此前已完成`);
        setOnboardingNotice({
          tone: "ok",
          text: parts.length > 0 ? parts.join("，") : "没有可领取的任务",
          details: [
            `累计已领 ${res.earned} / ${res.total}`,
            ...res.claimed.map((item) => `${item.title} +${item.points}`)
          ]
        });
        setOnboarding({ earned: res.earned, total: res.total, skipped: res.skipped });
        if (canLoadCredits) await loadCredits();
      } catch (caught) {
        setOnboardingNotice({ tone: "error", text: caught?.message || "领取新手任务失败" });
      } finally {
        if (mounted.current) setOnboardingLoading(false);
      }
    };
    const [showModels, setShowModels] = React2.useState(false);
    const [showQuota, setShowQuota] = React2.useState(false);
    const claimCredits = async () => {
      if (!supportsCredits) return;
      if (claimInFlightRef.current) {
        setClaimNotice({
          tone: "warn",
          text: "上一次领取仍在进行中（成长任务一轮可能数分钟），请等它结束。",
          details: []
        });
        return;
      }
      claimInFlightRef.current = true;
      setClaiming(true);
      setClaimNotice(null);
      try {
        const res = await rpcCall("credits.claimAll", {
          provider,
          runTasks: supportsGrowthTasks
        });
        const { summary, results, growth } = res;
        const parts = [];
        if (summary.claimed > 0) parts.push(`${summary.claimed} 个账号领取成功`);
        if (summary.alreadyClaimed > 0) parts.push(`${summary.alreadyClaimed} 个今日已领取`);
        if (summary.inactive > 0) parts.push(`${summary.inactive} 个活动未开启`);
        if (summary.failed > 0) parts.push(`${summary.failed} 个失败`);
        if (!mounted.current) return;
        const details = [];
        for (const item of results || []) {
          const outcome = item.outcome || {};
          if (outcome.kind === "claimed") {
            const unit = outcome.unit === "token" ? "token" : "credit";
            details.push(
              `${item.nickname || item.accountId}：领取成功 +${formatUnits(outcome.credit, unit)}${unitLabel(unit)}`
            );
          } else if (outcome.kind === "already-claimed") {
            details.push(`${item.nickname || item.accountId}：${outcome.message || "今天已领取"}`);
          } else if (outcome.kind === "inactive") {
            details.push(`${item.nickname || item.accountId}：${outcome.message || "不在活动范围"}`);
          } else if (outcome.kind === "failed") {
            details.push(`${item.nickname || item.accountId}：失败 — ${outcome.message || "未知原因"}`);
          }
        }
        let growthFailed = 0;
        let growthTimedOut = false;
        if (Array.isArray(growth) && growth.length > 0) {
          let completed = 0;
          let clientOnly = 0;
          let claimedCount = 0;
          let claimedCredit = 0;
          for (const account of growth) {
            const who = account.nickname || account.accountId;
            completed += account.completed || 0;
            clientOnly += account.clientOnly || 0;
            growthFailed += account.failed || 0;
            claimedCount += account.claimedCount || 0;
            claimedCredit += account.claimedCredit || 0;
            if (account.timedOut) growthTimedOut = true;
            if (account.error) {
              details.push(`${who}：${account.error}`);
              continue;
            }
            const tasks = account.tasks || [];
            const settled = tasks.filter((t) => t.ok && !t.clientOnly);
            const broken = tasks.filter((t) => !t.ok);
            const needClient = tasks.filter((t) => t.ok && t.clientOnly);
            if (settled.length === 0 && broken.length === 0) {
              const why = needClient.length > 0 ? `${needClient.length} 项判据只在客户端行为里记录，API 推不动` : "本轮无 API 可达的任务";
              details.push(`${who}：${why}（未领到积分）`);
              continue;
            }
            const head = [
              settled.length > 0 ? `完成 ${settled.length} 项` : null,
              broken.length > 0 ? `失败 ${broken.length} 项` : null,
              needClient.length > 0 ? `需客户端 ${needClient.length} 项` : null
            ].filter(Boolean).join("，");
            details.push(`${who}：${head}`);
            for (const task of settled.concat(broken)) {
              const label = task.taskCode || "未知任务";
              const reason = task.message || "";
              if (!task.ok) details.push(`　· ${label}：失败 — ${reason}`);
              else details.push(`　· ${label}：${reason}`);
            }
          }
          if (completed > 0) parts.push(`成长完成 ${completed} 项`);
          if (clientOnly > 0) parts.push(`成长需客户端 ${clientOnly} 项`);
          if (growthFailed > 0) parts.push(`成长失败 ${growthFailed} 项`);
          if (claimedCount > 0) parts.push(`自动领取 ${claimedCount} 项（+${claimedCredit} 积分）`);
          if (growthTimedOut) parts.push("本轮时间预算用尽，剩余项未执行，再点一次即可继续");
        }
        setClaimNotice({
          tone: summary.failed > 0 || growthFailed > 0 ? "warn" : "ok",
          text: parts.length > 0 ? parts.join("，") : "没有可领取的账号",
          details
        });
        await loadAccounts();
        await loadCredits();
      } catch (caught) {
        console.error("[jet-hub] claim credits failed:", caught);
        if (!mounted.current) return;
        setClaimNotice({ tone: "error", text: caught?.message || "领取积分失败" });
      } finally {
        claimInFlightRef.current = false;
        if (mounted.current) setClaiming(false);
      }
    };
    const stopPoll = () => {
      if (pollRef.current !== 0) {
        clearInterval(pollRef.current);
        pollRef.current = 0;
      }
      if (mounted.current) setCreating(false);
    };
    const submitOpencodeKey = async (rawKey) => {
      const key = String(rawKey == null ? "" : rawKey).trim();
      if (key === "") return;
      await submitOpencodeEntry("opencode.addAccount", { apiKey: key });
    };
    const submitAnonymous = async () => {
      await submitOpencodeEntry("opencode.addAnonymous", {});
    };
    const submitOpencodeEntry = async (method, payload) => {
      setCreating(true);
      setError(null);
      try {
        const res = await rpcCall(method, payload);
        setKeyModal(null);
        await loadAccounts();
        if (res && res.existed) setProbeNotice("该 key 已存在，已为你定位到原有账号。");
      } catch (caught) {
        setKeyModal({ error: caught && caught.message ? caught.message : "未知错误" });
      } finally {
        setCreating(false);
      }
    };
    const createAccount = async () => {
      if (pollRef.current !== 0) {
        setProbeNotice({ tone: "warn", text: "上一次登录仍在进行中，请先完成或等待它结束。", details: [] });
        return;
      }
      if (provider === "opencode") {
        setKeyModal({ error: "" });
        return;
      }
      setCreating(true);
      let accountId = "";
      let loginUrl = "";
      try {
        const res = await rpcCall("account.create", buildCreateAccountPayload(provider, zcodeProvider));
        accountId = res.accountId;
        loginUrl = res.loginUrl;
        console.log("[jet-hub] account.create ok, provider =", provider, ", accountId =", accountId);
        if (res.reused) {
          setLoginUrlForManual(null);
          await loadAccounts();
          setProbeNotice({ tone: "ok", text: "已复用本机已有的账号凭据，未新建账号。", details: [] });
          return;
        }
        if (loginUrl) {
          const loginWindow = window.open(loginUrl, "_blank", "width=800,height=600");
          if (!loginWindow || loginWindow.closed) {
            setLoginUrlForManual(loginUrl);
            setLoginLinkCopied(null);
          }
          const deadline = Date.now() + 3e5;
          pollRef.current = setInterval(async () => {
            if (!mounted.current) {
              stopPoll();
              return;
            }
            if (Date.now() > deadline) {
              if (loginWindow && !loginWindow.closed) loginWindow.close();
              stopPoll();
              return;
            }
            try {
              const pollRes = await rpcCall("login.poll", { accountId, provider });
              if (!mounted.current) return;
              if (!pollRes?.done) return;
              if (loginWindow && !loginWindow.closed) loginWindow.close();
              setLoginUrlForManual(null);
              await loadAccounts();
              stopPoll();
            } catch {
            }
          }, 1e3);
        } else {
          setError("后端未返回登录地址（loginUrl 为空）。");
          setPhase("error");
        }
      } catch (caught) {
        console.error("[jet-hub] create account failed:", caught);
        setError("新建账号失败：" + (caught?.message || "未知错误"));
        setPhase("error");
      } finally {
        if (pollRef.current === 0) setCreating(false);
      }
    };
    const toggleAccount = async (accountId, enabled) => {
      try {
        const isLastEnabled = !enabled && disablingLeavesNoEnabledAccount(accountsRef.current, accountId, provider);
        const isFirstEnabled = enabled && !accountsRef.current.some((a) => a.provider === provider && a.id !== accountId && a.enabled !== false);
        await rpcCall("account.update", { accountId, patch: { enabled } });
        await loadAccounts();
        if (isLastEnabled) {
          const closeModels = confirm(
            `该 Provider 已没有启用账号，它的模型不会再被使用。

是否同时关闭它的全部模型（从对话框的模型选择里移除）？
选择「取消」则只停用账号，模型保持现状。`
          );
          if (closeModels) {
            try {
              await rpcCall("model.setAllDisabled", { provider, disabled: true });
            } catch (caught) {
              console.error("[jet-hub] cascade disable models failed:", caught);
              if (mounted.current) {
                setProbeNotice({
                  tone: "warn",
                  text: `账号已停用，但关闭模型失败：${caught?.message || "未知错误"}。可在「显示列表」中手动关闭。`,
                  details: []
                });
              }
            }
          }
          return;
        }
        if (isFirstEnabled) {
          let models;
          try {
            const res = await rpcCall("model.list", { provider });
            models = res.models || [];
          } catch (caught) {
            console.error("[jet-hub] read models for cascade enable failed:", caught);
            models = null;
          }
          if (models !== null && allModelsDisabled(models)) {
            const openModels = confirm(
              `该 Provider 的 ${models.length} 个模型当前全部处于关闭状态。

是否同时打开它们（让模型重新出现在对话框的模型选择里）？`
            );
            if (openModels) {
              try {
                await rpcCall("model.setAllDisabled", { provider, disabled: false });
              } catch (caught) {
                console.error("[jet-hub] cascade enable models failed:", caught);
                if (mounted.current) {
                  setProbeNotice({
                    tone: "warn",
                    text: `账号已启用，但打开模型失败：${caught?.message || "未知错误"}。可在「显示列表」中手动打开。`,
                    details: []
                  });
                }
              }
            }
          }
        }
      } catch (caught) {
        console.error("[jet-hub] toggle failed:", caught);
      }
    };
    const deleteAccount = async (accountId) => {
      if (!confirm("确认删除此账号？关联的凭据也将被清除。")) return;
      try {
        await rpcCall("account.delete", { accountId });
        await loadAccounts();
      } catch (caught) {
        console.error("[jet-hub] delete failed:", caught);
      }
    };
    const rotateFingerprint = async (accountId) => {
      if (!confirm("轮换该账号的指纹？将生成新的 project id（用于与其他账号区分）。")) return;
      try {
        await rpcCall("opencode.rotateFingerprint", { accountId });
        await loadAccounts();
      } catch (caught) {
        alert("轮换失败：" + (caught?.message || String(caught)));
      }
    };
    const commitOrder = async (orderedIds) => {
      const snapshot = accountsRef.current;
      const byId = new Map(snapshot.map((a) => [a.id, a]));
      const next = orderedIds.map((id) => byId.get(id)).filter(Boolean);
      if (next.length !== snapshot.length) return;
      accountsRef.current = next;
      setAccounts(next);
      setReordering(true);
      setReorderError(null);
      try {
        await rpcCall("account.reorder", { provider, orderedIds });
      } catch (caught) {
        console.error("[jet-hub] reorder failed:", caught);
        if (!mounted.current) return;
        accountsRef.current = snapshot;
        setAccounts(snapshot);
        setReorderError(caught?.message || "顺序保存失败");
      } finally {
        if (mounted.current) setReordering(false);
      }
    };
    const computeDropOrder = (sourceId, targetId, position) => orderAfterDrop(accountsRef.current.map((a) => a.id), sourceId, targetId, position);
    const dragPropsFor = (account, index) => {
      if (accounts.length < 2) return { enabled: false };
      return {
        enabled: true,
        order: index,
        isDragging: draggingId === account.id,
        isDropTarget: dropTargetId === account.id && draggingId !== null && draggingId !== account.id,
        dropPosition,
        onDragStart: (event) => {
          setDraggingId(account.id);
          setReorderError(null);
          try {
            event.dataTransfer.setData("text/plain", account.id);
          } catch {
          }
          if (event.dataTransfer) event.dataTransfer.effectAllowed = "move";
        },
        onDragEnd: () => {
          setDraggingId(null);
          setDropTargetId(null);
        },
        onDragOver: (event) => {
          if (draggingId === null || draggingId === account.id) return;
          event.preventDefault();
          if (event.dataTransfer) event.dataTransfer.dropEffect = "move";
          if (dropTargetId !== account.id) setDropTargetId(account.id);
          const rect = event.currentTarget?.getBoundingClientRect?.();
          const next = dropPositionFromPointer(event.clientY, rect);
          if (next !== dropPosition) setDropPosition(next);
        },
        onDrop: (event) => {
          event.preventDefault();
          const sourceId = draggingId;
          setDraggingId(null);
          setDropTargetId(null);
          if (sourceId === null || sourceId === account.id) return;
          const next = computeDropOrder(sourceId, account.id, dropPosition);
          if (next !== null) void commitOrder(next);
        }
      };
    };
    const runLimitAction = async (kind, accountId) => {
      if (kind === "retestAll" && !confirm("将对本页全部账号（含已停用）各发送一条真实消息来验证限流状态，会消耗模型额度。继续？")) {
        return;
      }
      setProbeBusy(kind === "retestAll" || kind === "resetAll" ? "all" : "one");
      setProbeNotice(null);
      try {
        let res;
        if (kind === "retest") res = await rpcCall("account.retest", { accountId });
        else if (kind === "retestAll") res = await rpcCall("account.retestAll", { provider });
        else if (kind === "reset") res = await rpcCall("account.reset", { accountId });
        else res = await rpcCall("account.resetAll", { provider });
        if (!mounted.current) return;
        const details = (res?.accounts || []).flatMap((a) => (a.stillLimited || []).map((m) => `${a.nickname || a.accountId} · ${m.modelId}：${m.message || "仍受限"}`));
        const summary = summarizeProbe(kind, res);
        setProbeNotice({ tone: details.length > 0 ? "warn" : "ok", text: summary, details });
        await loadAccounts();
      } catch (caught) {
        console.error("[jet-hub] limit action failed:", caught);
        if (!mounted.current) return;
        setProbeNotice({ tone: "error", text: `操作失败：${caught?.message || "未知错误"}`, details: [] });
      } finally {
        if (mounted.current) setProbeBusy(null);
      }
    };
    const runTestAction = async (accountId) => {
      setProbeBusy("one");
      setProbeNotice(null);
      try {
        const res = await rpcCall("account.test", { accountId });
        if (!mounted.current) return;
        const text = summarizeTest(res);
        const details = res && res.ok === false && res.message ? [`${res.nickname || res.accountId} · ${res.modelId || "未知模型"}：${res.message}`] : [];
        setProbeNotice({ tone: res?.ok ? "ok" : "warn", text, details });
        await loadAccounts();
      } catch (caught) {
        console.error("[jet-hub] account test failed:", caught);
        if (!mounted.current) return;
        setProbeNotice({ tone: "error", text: `测试失败：${caught?.message || "未知错误"}`, details: [] });
      } finally {
        if (mounted.current) setProbeBusy(null);
      }
    };
    const runAccountCheckin = async (accountId) => {
      if (accountBusy) return;
      setAccountBusy({ kind: "checkin", id: accountId });
      setProbeNotice(null);
      try {
        const res = await rpcCall("account.checkin", { accountId });
        if (!mounted.current) return;
        setProbeNotice({
          tone: res.success ? "ok" : "warn",
          text: `${res.nickname || "账号"}：${res.message || "签到完成"}`,
          details: []
        });
        await loadAccounts();
      } catch (caught) {
        console.error("[jet-hub] account checkin failed:", caught);
        if (!mounted.current) return;
        setProbeNotice({ tone: "error", text: `签到失败：${caught?.message || "未知错误"}`, details: [] });
      } finally {
        if (mounted.current) setAccountBusy(null);
      }
    };
    const runAccountRefresh = async (accountId) => {
      if (accountBusy) return;
      setAccountBusy({ kind: "refresh", id: accountId });
      setProbeNotice(null);
      try {
        const res = await rpcCall("account.refresh", { accountId });
        if (!mounted.current) return;
        setProbeNotice({
          tone: "ok",
          text: `${res.nickname || "账号"}：${res.message || "续期完成"}`,
          details: res.expiresAt ? [`新有效期至 ${formatTime(res.expiresAt) || "未知"}`] : []
        });
        await loadAccounts();
      } catch (caught) {
        console.error("[jet-hub] account refresh failed:", caught);
        if (!mounted.current) return;
        setProbeNotice({ tone: "error", text: `续期失败：${caught?.message || "未知错误"}`, details: [] });
      } finally {
        if (mounted.current) setAccountBusy(null);
      }
    };
    return React2.createElement(
      "section",
      { "aria-label": `${provider} 账号管理` },
      // 标题与按钮分开成两块（而不是同一行的 space-between）：操作按钮多达 5 个，
      // 与面板标题挤在一行时既会被压缩又会溢出。标题独占一行、按钮组另起一行
      // 并允许换行，窄面板下也能完整显示。
      React2.createElement(
        "div",
        { className: "dim-jh-panelHead" },
        React2.createElement(
          "h2",
          { className: "dim-jh-panelTitle" },
          `${PROVIDERS.find((p) => p.id === provider)?.label || provider} 账号管理`
        ),
        React2.createElement(
          "div",
          { className: "dim-jh-headerActions" },
          React2.createElement("button", {
            className: "dim-jh-btn",
            title: MODEL_LIST_HELP,
            onClick: () => setShowModels(true)
          }, "显示列表"),
          // 「订阅额度」放在**面板级**（而不是账号卡片的按钮行）：
          // 那一行已有 5 个按钮且 `flex-wrap: nowrap`，再塞一个必然溢出
          // （该行的注释里记着「领取新手任务」当时就是这么被挤出去的）。
          // 且额度是**跨账号**的读数，放在面板级与它的语义一致。
          canShowSubscriptionQuota ? React2.createElement("button", {
            className: "dim-jh-btn",
            title: "查看 Cline 官方订阅额度窗口（5 小时 / 周 / 月各用掉百分之几）与逐笔请求记录（模型、token、积分）。数据来自官方网关，非本地记账。",
            onClick: () => setShowQuota(true)
          }, "订阅额度") : null,
          canLoadCredits ? React2.createElement("button", {
            className: "dim-jh-btn",
            title: "重新查询本页全部账号的剩余积分（Credits Balance）。余额由服务端实时计算，点此可刷新。",
            disabled: creditsLoading,
            onClick: () => void loadCredits()
          }, creditsLoading ? "查询中…" : "刷新积分") : null,
          supportsCredits ? React2.createElement("button", {
            className: "dim-jh-btn",
            title: supportsGrowthTasks ? `领取全部 ${PROVIDERS.find((p) => p.id === provider)?.label || provider} 账号（含已停用）的每日签到积分，**并完成成长中心任务**。成长一轮单账号实测 90～270s 且全串行，账号多时可能需多点几次；受客户端动作限制的项（微信、夜猫子窗口、判据推不动）会如实标为「需客户端」。` : `领取全部 ${PROVIDERS.find((p) => p.id === provider)?.label || provider} 账号（含已停用）的每日签到积分`,
            disabled: claiming || accounts.length === 0,
            onClick: () => void claimCredits()
          }, claiming ? "领取中…" : supportsGrowthTasks ? "一键领取积分 + 成长任务" : "一键领取积分") : null,
          // 「重测 / 重置」只对**会返回限流错误**的 provider 有意义。
          // ⚠️ Loomy 不会限流（积分耗尽时静默降级为扣永久积分），故对它
          // 隐藏这两个按钮 —— 重测永远测不出限流、重置也没有标记可清，
          // 而重测还会白烧积分（用户报障：「这个 provider 好像没发现模型限流，
          // 把重置所有按钮删掉」）。
          supportsRateLimit(provider) ? React2.createElement("button", {
            className: "dim-jh-btn",
            title: RETEST_ALL_HELP,
            disabled: probeBusy !== null || accounts.length === 0,
            onClick: () => void runLimitAction("retestAll")
          }, probeBusy === "all" ? "重测中…" : "重测所有") : null,
          supportsRateLimit(provider) ? React2.createElement("button", {
            className: "dim-jh-btn",
            title: RESET_ALL_HELP,
            disabled: probeBusy !== null || accounts.length === 0,
            onClick: () => void runLimitAction("resetAll")
          }, "重置所有") : null,
          // 锁定永久积分：只消耗会近期作废的积分，保住长期积分。
          // 文案按 provider 给（Loomy 是「每日赠送额度」，两个 buddy 是
          // 「15 天内到期的积分包」）—— 见 permanentLockCopy 的说明。
          canLockPermanent ? React2.createElement("button", {
            className: "dim-jh-btn",
            "data-kind": permanentLocked ? "primary" : void 0,
            title: permanentLocked ? lockCopy.lockedTitle : lockCopy.lockTitle,
            disabled: lockBusy,
            onClick: () => void togglePermanentLock()
          }, lockBusy ? "处理中…" : permanentLocked ? "解锁永久积分" : "锁定永久积分") : null,
          // ⚠️ 渠道弹窗**只服务 ZCode**：非 zcode 直接发起登录，与「搬进弹窗」
          //   之前逐字等价。上游 15ccae5 把渠道选择搬进弹窗时丢了这层门控，
          //   于是全部 14 个 provider 点「+ 新建账号」都弹出一个「添加 ZCode 账号」。
          React2.createElement("button", {
            className: "dim-jh-btn",
            "data-kind": "primary",
            title: "通过浏览器登录一个新的账号并加入账号池。",
            // ⚠⚠ **必须按 provider 分支**（真实缺陷，Gitee issue IKJLK3 A1/A2/A3）：
            //   这个按钮在**每一个** provider 的面板里都有，而下面的弹窗是
            //   **zCode 专用**的（登录渠道下拉）。原先无条件 `setPendingLogin(false)`，
            //   于是「在 CodeArts 面板点新建账号」弹出的也是「添加 ZCode 账号」，
            //   里面的下拉还不会下发（载荷里没有 `zcodeProvider`），
            //   opencode 则要用户先过一遍 ZCode 弹窗才看到自己的 API key 表单。
            //   ⇒ 判据收在 `newAccountAsksChannel()`：只有 zcode 先问渠道，
            //   其余 provider 点按钮即发起登录。**别把它改回无条件开弹窗。**
            onClick: () => {
              if (newAccountAsksChannel(provider)) setPendingLogin(false);
              else void createAccount();
            },
            disabled: creating
          }, creating ? "正在登录…" : "+ 新建账号"),
          // ★ 渠道选择对话框（用户 2026-10-03：从按钮旁搬到弹窗里）
          //
          // ⚠ `newAccountAsksChannel(provider)` 这道是**防御性**的：按钮已经分支过，
          //   正常路径下非 zcode 永远不会把 `pendingLogin` 置为「开」。
          //   但状态是活的（用户切 provider 时本面板会重新挂载，理论上仍可能
          //   残留一帧），漏掉它就会退回 issue 里那个「非 zcode 弹出 ZCode 窗」的形态。
          //
          // ⚠ 类名 `dim-jh-zcDialog*` 名字里带 zc，但它**已是通用弹窗样式**
          //   （见 `jet-hub-styles.js`），**不是** zCode 专用标记。
          //   曾想改名，但样式表一起动、收益仅是可读性，故保留名字。
          //
          // ⚠ 本弹窗原先有**两个入口**（这里与官方模型卡片里的 ZCode 账号区）；后者
          //   已随 Gitee issue IKJLHQ 整体移除（它占用的 provider-card 槽 key 与第三方
          //   pi-ai 扩展互斥，机制见 index.js 文件头）—— 现在只剩本处一个入口。
          pendingLogin === void 0 || !newAccountAsksChannel(provider) ? null : React2.createElement(
            "div",
            {
              className: "dim-jh-zcDialogMask",
              onClick: () => setPendingLogin(void 0)
            },
            React2.createElement(
              "div",
              {
                className: "dim-jh-zcDialog",
                onClick: (event) => event.stopPropagation()
              },
              React2.createElement(
                "h3",
                { className: "dim-jh-zcDialogTitle" },
                "添加 ZCode 账号"
              ),
              React2.createElement(
                "p",
                { className: "dim-jh-zcDialogHint" },
                "选择登录渠道后才会打开对应的认证页面。两个渠道的授权页与凭据各自独立，",
                "可用 z.ai（国际）登录另一个账号。"
              ),
              React2.createElement(
                "label",
                { className: "dim-jh-zcProvider" },
                "登录渠道",
                React2.createElement(
                  "select",
                  {
                    className: "dim-jh-zcSelect",
                    value: zcodeProvider,
                    onChange: (event) => setZcodeProvider(event.target.value)
                  },
                  React2.createElement("option", { value: "bigmodel" }, "BigModel（智谱开放平台，国内）"),
                  React2.createElement("option", { value: "zai" }, "z.ai（chat.z.ai，国际版）")
                )
              ),
              React2.createElement(
                "div",
                { className: "dim-jh-zcDialogActions" },
                React2.createElement("button", {
                  className: "dim-jh-btn",
                  onClick: () => setPendingLogin(void 0)
                }, "取消"),
                React2.createElement("button", {
                  className: "dim-jh-btn",
                  "data-kind": "primary",
                  onClick: () => {
                    setPendingLogin(void 0);
                    void createAccount();
                  }
                }, "确定并打开认证页")
              )
            )
          )
        ),
        probeNotice ? React2.createElement(
          "div",
          {
            className: "dim-jh-probeNotice",
            "data-tone": probeNotice.tone,
            role: "status"
          },
          React2.createElement("div", null, probeNotice.text),
          probeNotice.details.length > 0 ? React2.createElement(
            "ul",
            { className: "dim-jh-probeDetails" },
            probeNotice.details.map((d, i) => React2.createElement("li", { key: i }, d))
          ) : null
        ) : null,
        lockNotice ? React2.createElement("div", {
          className: "dim-jh-probeNotice",
          "data-tone": lockNotice.tone,
          role: lockNotice.tone === "error" ? "alert" : "status"
        }, lockNotice.text) : null,
        claimNotice ? React2.createElement(
          "div",
          {
            className: "dim-jh-probeNotice",
            "data-tone": claimNotice.tone,
            role: claimNotice.tone === "error" ? "alert" : "status"
          },
          React2.createElement("div", null, claimNotice.text),
          // 逐账号原因列表。没有它时用户只看到「1 个失败」，无从判断是
          // 凭据问题、活动未开、还是解析 bug。
          (claimNotice.details || []).length > 0 ? React2.createElement(
            "ul",
            { className: "dim-jh-probeDetails" },
            claimNotice.details.map((d, i) => React2.createElement("li", { key: i }, d))
          ) : null
        ) : null,
        // 新手任务结果（仅 Loomy，一次性领取）。
        onboardingNotice ? React2.createElement(
          "div",
          {
            className: "dim-jh-probeNotice",
            "data-tone": onboardingNotice.tone,
            role: onboardingNotice.tone === "error" ? "alert" : "status"
          },
          React2.createElement("div", null, onboardingNotice.text),
          (onboardingNotice.details || []).length > 0 ? React2.createElement(
            "ul",
            { className: "dim-jh-probeDetails" },
            onboardingNotice.details.map((line, index) => React2.createElement("li", { key: index }, line))
          ) : null
        ) : null,
        // 弹窗被拦截：给出可点击的登录链接 + 复制按钮。不劫持当前页面（见 createAccount 的说明）。
        // 链接 300+ 字符，用户此刻唯一动作就是复制到别处；手工框选漏一个字符会得到「授权失败」假象。
        loginUrlForManual ? React2.createElement(
          "div",
          {
            className: "dim-jh-probeNotice",
            "data-tone": "warn",
            role: "alert"
          },
          React2.createElement("div", null, "登录窗口被浏览器拦截，请手动打开下方链接完成登录："),
          React2.createElement(
            "div",
            {
              className: "dim-jh-loginLinkRow",
              "data-copy-state": loginLinkCopied === false ? "failed" : "idle"
            },
            React2.createElement("a", {
              className: "dim-jh-loginLink",
              href: loginUrlForManual,
              target: "_blank",
              rel: "noopener noreferrer"
            }, loginUrlForManual),
            // type='button' 必须有：按钮若落在表单里，默认 submit 会连带提交表单。
            React2.createElement("button", {
              className: "dim-jh-btn",
              type: "button",
              title: loginLinkCopied === false ? "复制失败：请手动选中链接后按 Ctrl+C" : "复制授权链接",
              onClick: async () => setLoginLinkCopied(await copyToClipboard(loginUrlForManual))
            }, loginLinkCopied === true ? "已复制 ✓" : loginLinkCopied === false ? "复制失败，请手动选中" : "复制链接")
          )
        ) : null,
        phase === "loading" ? React2.createElement("div", { className: "dim-jh-empty" }, "正在读取账号列表…") : phase === "error" ? React2.createElement(
          "div",
          { className: "dim-jh-empty", role: "alert" },
          React2.createElement("p", null, error),
          React2.createElement("button", { className: "dim-jh-btn", onClick: loadAccounts }, "重新读取")
        ) : accounts.length === 0 ? React2.createElement(
          "div",
          { className: "dim-jh-empty" },
          // ⚠️ opencode 的「新建」是**粘贴 API key**、不是浏览器登录，
          // 空态文案必须跟着变 —— 否则用户会去找一个根本不存在的登录页。
          provider === "opencode" ? React2.createElement(
            "div",
            null,
            React2.createElement(
              "p",
              null,
              "尚未添加账号。免费模型无需账号即可使用；添加自己的 API key 可启用付费模型，并为每个账号配置独立出口。"
            ),
            React2.createElement(
              "p",
              { className: "dim-jh-hint" },
              "API key 在 opencode.ai/auth 生成，形如 sk-…"
            )
          ) : React2.createElement(
            "div",
            null,
            React2.createElement("p", null, "尚未配置账号"),
            React2.createElement("p", null, '点击"+ 新建账号"进行浏览器登录。')
          )
        ) : React2.createElement(
          "div",
          null,
          // ⚠️ opencode 专属策略提示：必须说清「多账号 ≠ 多额度」——
          // 匿名通道按出口 IP 限流，同一出口下的多个账号共用一份额度。
          // 不解释的话，用户加了 5 个号却只看到一份配额，会以为功能坏了。
          provider === "opencode" ? React2.createElement(
            "p",
            { className: "dim-jh-hint" },
            "免费模型在所有通道间自动轮换，收费模型仅「API key 账号」可用。匿名通道无需 key，可添加多条、各自配代理；注意额度按「出口 IP」计算 —— 多条通道共用一个出口不会增加额度，分别配不同代理才会各自获得独立额度。"
          ) : null,
          // 排序提示：顺序会真实影响自动选号，必须让用户知道，否则
          // 「拖了有什么用」无从得知。仅两个以上账号时才显示。
          accounts.length > 1 ? React2.createElement(
            "p",
            { className: "dim-jh-orderHint" },
            "拖动卡片可调整顺序（也可直接拖整张卡片）。顺序即自动选号与限流换号的优先级，排在前面的账号优先使用。"
          ) : null,
          reorderError ? React2.createElement("div", {
            className: "dim-jh-probeNotice",
            "data-tone": "warn",
            role: "alert"
          }, React2.createElement("div", null, `顺序保存失败：${reorderError}`)) : null,
          accounts.map((account, index) => React2.createElement(AccountCard, {
            key: account.id,
            account,
            index,
            // ⚠️ `provider` 必须传进来：积分行的**池名分桶**要按 provider
            // 决定另一个池的标签（Loomy 是「永久」、Raccoon 是「长期」）。
            // 漏传会在渲染时抛 `ReferenceError: provider is not defined`
            // 并让整个 Jet Hub 设置页崩成白屏（真实事故，2026-09-29）。
            provider,
            busy: probeBusy !== null,
            credits: credits[account.id],
            creditsLoading: creditsLoading && credits[account.id] === void 0,
            showCredits: canLoadCredits,
            // 账号名 hover 列资源包：只有余额真由多个包构成的 provider 才挂
            // （loomy 的池是我们合成的、无到期字段，列出来会把「每日赠送」
            // 标成长期 —— 恰好说反）。
            showPackageList: supportsCreditPackageList(provider),
            // 「临时 / 长期」分桶的窗口天数（buddy 系 + TRAE + LobsterAI 才有值）。
            windowDays: expiryWindowDays,
            // 卡片级「重测 / 重置」：只对会返回限流错误的 provider 渲染。
            showRateLimitActions: supportsRateLimit(provider),
            onToggle: toggleAccount,
            onDelete: deleteAccount,
            onRetest: (id) => void runLimitAction("retest", id),
            onReset: (id) => void runLimitAction("reset", id),
            // ⚠️ 「测试」用**独立**的能力位（`supportsAccountTest`）与独立的
            // props 存在性开关，不能挂在 `showRateLimitActions` 上：Gemini
            // 恰好只登记了测试、没登记重测（它的限流是配额窗口制，重测无意义），
            // 绑一起就会二选一地消失一个按钮。
            onTest: supportsAccountTest(provider) ? (id) => void runTestAction(id) : void 0,
            // ⚠️ 仅 opencode：这两个回调只在该 provider 下传，
            // AccountCard 靠「props 存在性」决定是否渲染按钮。
            ...provider === "opencode" ? {
              onOpenProxy: (id, current) => setProxyModal({ accountId: id, current }),
              onRotateFingerprint: (id) => void rotateFingerprint(id)
            } : {},
            // 新手任务（仅 Loomy）：一次性 10000 分，每号只能领一次。
            // 与「一键领取积分」（每日签到）是**不同**的操作，故独立按钮。
            onClaimOnboarding: canClaimOnboarding ? (id) => void claimOnboarding(id) : void 0,
            onboardingBusy: onboardingLoading,
            // 快捷行（签到 / 续期，UTTS app 的 Jet 桥能力）：存在才渲染，
            // AccountCard 靠 props 存在性决定按钮（与 onTest / onOpenProxy 同型）。
            // busy 标识传「当前在途的账号 id」，按钮文字据此切换、其余账号只禁用。
            onCheckin: canCheckinAccount ? (id) => void runAccountCheckin(id) : void 0,
            onRefreshAccount: canRefreshAccount ? (id) => void runAccountRefresh(id) : void 0,
            checkinBusyId: accountBusy?.kind === "checkin" ? accountBusy.id : null,
            refreshBusyId: accountBusy?.kind === "refresh" ? accountBusy.id : null,
            // 提交顺序期间禁用拖拽，避免并发提交互相覆盖。
            drag: reordering ? { enabled: false } : dragPropsFor(account, index)
          }))
        ),
        // 模型列表以 modal 渲染：它是覆盖层，放在账号区之后只是组件树的书写顺序，
        // 实际靠 fixed 定位浮在整个面板之上，不再挤占账号池的版面。
        showModels ? React2.createElement(ModelListPanel, {
          provider,
          rpcCall,
          onClose: () => setShowModels(false)
        }) : null,
        // 「订阅额度」弹窗同样以覆盖层渲染（不挤占账号池版面）。
        // 不复用 ModelListPanel 的 provider 形参：额度端点当前只认 Cline，
        // 由 `canShowSubscriptionQuota` 门控按钮，面板内部固定传 'cline'。
        showQuota ? React2.createElement(ClineQuotaPanel, {
          rpcCall,
          onClose: () => setShowQuota(false)
        }) : null,
        // 出口代理弹窗（仅 opencode）：覆盖层，与上面两个弹窗各自独立 state，
        // 同时打开也只是叠加，不会互相顶掉。
        //
        // ⚠️⚠️ **必须用 `React.createElement(组件, props)`，不能直接调用函数**
        // （真实事故 2026-10-02）：这两个弹窗内部有 `useState`，若在
        // ProviderPanel 的渲染过程中**直接函数调用**，它们的 hook 会被算进
        // ProviderPanel —— 于是「打开弹窗」与「关闭弹窗」两次渲染的 hook 数量
        // 不同，React 抛 #310（"Rendered more hooks than during the previous render"），
        // 整个设置页崩成白屏。仓库其它弹窗（ModelListPanel / BackupPanel /
        // ClineQuotaPanel）都是 `createElement` 形式，正是这个原因。
        proxyModal ? React2.createElement(OpencodeProxyModal, {
          // ⚠️ 传最小 ctx 门面而不是整个面板：弹窗只需要 rpc，
          // 这样它在单测/复用时不必拖上整个 ProviderPanel 的依赖。
          ctx: { rpc: (payload) => rpcCall(payload.method, payload.payload) },
          accountId: proxyModal.accountId,
          current: proxyModal.current,
          onClose: () => setProxyModal(null)
        }) : null,
        // 「添加 opencode 账号」弹窗：自绘而非 window.prompt ——
        // DSH 客户端沙箱里 prompt() 直接抛 `prompt() is not supported`（真机报障）。
        keyModal ? React2.createElement(OpencodeKeyModal, {
          error: keyModal.error,
          busy: creating,
          inputRef: keyInputRef,
          onSubmit: (value) => void submitOpencodeKey(value),
          onSubmitAnonymous: () => void submitAnonymous(),
          onClose: () => setKeyModal(null)
        }) : null
      )
    );
  }
  function BackupPanel({ rpcCall, onImported }) {
    const [dialog, setDialog] = React2.useState(null);
    const [encrypt, setEncrypt] = React2.useState(true);
    const [pass1, setPass1] = React2.useState("");
    const [pass2, setPass2] = React2.useState("");
    const [importFile, setImportFile] = React2.useState(null);
    const [importPass, setImportPass] = React2.useState("");
    const [backupStatus, setBackupStatus] = React2.useState(null);
    const [confirmStep, setConfirmStep] = React2.useState(false);
    const [decryptedPayload, setDecryptedPayload] = React2.useState(null);
    const [busy, setBusy] = React2.useState(false);
    const [notice, setNotice] = React2.useState(null);
    const fileRef = React2.useRef(null);
    const mounted = React2.useRef(true);
    React2.useEffect(() => {
      mounted.current = true;
      return () => {
        mounted.current = false;
      };
    }, []);
    const closeDialog = () => {
      if (!mounted.current) return;
      setDialog(null);
      setNotice(null);
      setBusy(false);
      setEncrypt(true);
      setPass1("");
      setPass2("");
      setImportFile(null);
      setImportPass("");
      setBackupStatus(null);
      setConfirmStep(false);
      setDecryptedPayload(null);
    };
    const safeNotice = (next) => {
      if (mounted.current) setNotice(next);
    };
    const doExport = async () => {
      if (encrypt && pass1.length === 0) {
        safeNotice({ tone: "warn", text: "请设置备份口令" });
        return;
      }
      if (encrypt && pass1 !== pass2) {
        safeNotice({ tone: "warn", text: "两次输入的口令不一致" });
        return;
      }
      setBusy(true);
      setNotice(null);
      try {
        const res = await rpcCall("backup.export", {});
        const payload = res.payload;
        const warnings = res.warnings || [];
        const stamp = (/* @__PURE__ */ new Date()).toISOString().slice(0, 10).replace(/-/g, "");
        let data = payload;
        let filename = `dsh-codearts-backup-${stamp}.json`;
        if (encrypt) {
          data = await encryptBackup(payload, pass1);
          filename = `dsh-codearts-backup-${stamp}.enc.json`;
        }
        downloadJson(filename, data);
        const extra = warnings.length > 0 ? `，${warnings.length} 个账号凭据缺失（已跳过）` : "";
        safeNotice({ tone: "ok", text: `已导出 ${payload.accounts.length} 个账号${encrypt ? "（已加密）" : "（明文）"}${extra}` });
      } catch (caught) {
        console.error("[jet-hub] backup export failed:", caught);
        safeNotice({ tone: "error", text: `导出失败：${caught?.message || "未知错误"}` });
      } finally {
        if (mounted.current) setBusy(false);
      }
    };
    const onFileSelected = async (event) => {
      const file = event.target.files?.[0];
      event.target.value = "";
      if (!file) return;
      safeNotice(null);
      try {
        const text = await readFileAsText(file);
        let parsed;
        try {
          parsed = JSON.parse(text);
        } catch {
          safeNotice({ tone: "error", text: `${file.name} 不是有效的 JSON 备份文件` });
          return;
        }
        if (!mounted.current) return;
        setImportFile({ name: file.name, encrypted: isEncryptedBackup(parsed), parsed });
        setDialog("import");
        setImportPass("");
        try {
          const status = await rpcCall("backup.status", {});
          if (mounted.current) setBackupStatus(status || null);
        } catch (caught) {
          console.warn("[jet-hub] backup.status failed:", caught);
        }
      } catch (caught) {
        console.error("[jet-hub] read backup file failed:", caught);
        safeNotice({ tone: "error", text: `读取文件失败：${caught?.message || "未知错误"}` });
      }
    };
    const stepImport = async () => {
      if (!importFile) return;
      if (importFile.encrypted) {
        if (importPass.length === 0) {
          safeNotice({ tone: "warn", text: "请输入备份口令" });
          return;
        }
        setBusy(true);
        setNotice(null);
        try {
          const payload = await decryptBackup(importFile.parsed, importPass);
          if (!mounted.current) return;
          setDecryptedPayload(payload);
        } catch (caught) {
          console.error("[jet-hub] backup decrypt failed:", caught);
          safeNotice({ tone: "error", text: "解密失败：口令错误或备份文件已被篡改" });
          setBusy(false);
          return;
        }
        setBusy(false);
      } else {
        setDecryptedPayload(importFile.parsed);
      }
      setConfirmStep(true);
      setNotice(null);
    };
    const confirmImport = async () => {
      const payload = decryptedPayload;
      if (!payload) return;
      setBusy(true);
      setNotice(null);
      try {
        const res = await rpcCall("backup.import", { payload });
        const parts = [`已导入 ${res.accountsImported} 个账号`, `${res.credentialsImported} 条凭据`];
        if (res.skipped.length > 0) parts.push(`${res.skipped.length} 条凭据跳过`);
        if (res.expiredAccounts > 0) {
          parts.push(`${res.expiredAccounts} 个凭据已过期（失效账号需重新登录）`);
        }
        if (res.missingCredentials > 0) {
          parts.push(`${res.missingCredentials} 个账号凭据缺失（需重新登录）`);
        }
        onImported?.();
        safeNotice({ tone: "ok", text: parts.join("，") });
      } catch (caught) {
        console.error("[jet-hub] backup import failed:", caught);
        safeNotice({ tone: "error", text: `导入失败：${caught?.message || "未知错误"}` });
      } finally {
        if (mounted.current) setBusy(false);
      }
    };
    const renderDialog = () => {
      const isExport = dialog === "export";
      const title = isExport ? "导出备份" : "导入备份";
      const subtitle = isExport ? "全部 provider 的账号密钥与凭据" : importFile?.name || "";
      const body = isExport ? React2.createElement(
        React2.Fragment,
        null,
        React2.createElement(
          "p",
          { className: "dim-jh-modalHint" },
          "备份文件包含全部账号的密钥与 refresh_token，请",
          React2.createElement("strong", { className: "dim-jh-emph-warn" }, "妥善保管"),
          "。"
        ),
        React2.createElement(
          "p",
          { className: "dim-jh-modalHint" },
          "备份是导出时刻的凭据快照：refresh_token 会随续期轮换或过期，建议导出后尽快迁移，导入后失效的账号需重新登录。"
        ),
        React2.createElement(
          "label",
          { className: "dim-jh-checkRow" },
          React2.createElement("input", {
            type: "checkbox",
            checked: encrypt,
            onChange: (event) => setEncrypt(event.target.checked)
          }),
          "加密备份文件（推荐）"
        ),
        encrypt ? React2.createElement(
          "div",
          { className: "dim-jh-formRows" },
          React2.createElement("input", {
            className: "dim-jh-input",
            type: "password",
            placeholder: "备份口令（用于解密，请牢记）",
            value: pass1,
            onChange: (event) => setPass1(event.target.value)
          }),
          React2.createElement("input", {
            className: "dim-jh-input",
            type: "password",
            placeholder: "再次输入口令",
            value: pass2,
            onChange: (event) => setPass2(event.target.value)
          })
        ) : null
      ) : !isExport && confirmStep ? (
        // 导入确认页（应用内二次确认）：展示覆盖警告与提示
        React2.createElement(
          React2.Fragment,
          null,
          React2.createElement(
            "p",
            { className: "dim-jh-modalHint" },
            "导入将",
            React2.createElement("strong", { className: "dim-jh-emph-danger" }, "覆盖"),
            "当前全部账号与模型开关（共 ",
            React2.createElement("strong", { className: "dim-jh-emph-warn" }, `${decryptedPayload?.accounts?.length ?? 0}`),
            " 个账号），且",
            React2.createElement("strong", { className: "dim-jh-emph-danger" }, "不可撤销"),
            "。"
          ),
          backupStatus?.withoutExpiry > 0 ? React2.createElement(
            "p",
            { className: "dim-jh-modalHint" },
            "当前有 ",
            React2.createElement("strong", { className: "dim-jh-emph-warn" }, `${backupStatus.withoutExpiry}`),
            " 个账号缺少有效期信息（可能是版本切换后自动恢复的），导入将",
            React2.createElement("strong", { className: "dim-jh-emph-warn" }, "整体覆盖"),
            "它们。"
          ) : null
        )
      ) : importFile?.encrypted ? React2.createElement(
        React2.Fragment,
        null,
        React2.createElement(
          "p",
          { className: "dim-jh-modalHint" },
          "该备份已加密，请输入导出时设置的口令。"
        ),
        React2.createElement("input", {
          className: "dim-jh-input",
          type: "password",
          placeholder: "备份口令",
          value: importPass,
          onChange: (event) => setImportPass(event.target.value)
        })
      ) : React2.createElement(
        "p",
        { className: "dim-jh-modalHint" },
        "该备份为明文文件，导入将覆盖当前全部账号与模型开关。"
      );
      return React2.createElement(
        "div",
        {
          className: "dim-jh-modalOverlay",
          onClick: (event) => {
            if (event.target === event.currentTarget) closeDialog();
          }
        },
        React2.createElement(
          "div",
          {
            className: "dim-jh-modal",
            role: "dialog",
            "aria-modal": "true",
            "aria-label": title
          },
          React2.createElement(
            "div",
            { className: "dim-jh-modalHead" },
            React2.createElement(
              "div",
              { className: "dim-jh-modalTitle" },
              React2.createElement("strong", null, title),
              subtitle.length > 0 ? React2.createElement("span", { className: "dim-jh-modalSubtitle" }, subtitle) : null
            ),
            React2.createElement(
              "div",
              { className: "dim-jh-modelPanelActions" },
              React2.createElement("button", {
                className: "dim-jh-btn",
                onClick: closeDialog
              }, "关闭")
            )
          ),
          React2.createElement(
            "div",
            { className: "dim-jh-modalBody" },
            body,
            notice ? React2.createElement("div", {
              className: "dim-jh-probeNotice",
              "data-tone": notice.tone,
              role: notice.tone === "error" ? "alert" : "status"
            }, React2.createElement("div", null, notice.text)) : null,
            React2.createElement(
              "div",
              { className: "dim-jh-modalActions" },
              isExport ? React2.createElement("button", {
                className: "dim-jh-btn",
                "data-kind": "primary",
                disabled: busy,
                onClick: () => void doExport()
              }, busy ? "生成中…" : "生成备份文件") : confirmStep ? React2.createElement(
                React2.Fragment,
                null,
                React2.createElement("button", {
                  className: "dim-jh-btn",
                  disabled: busy,
                  onClick: () => {
                    setConfirmStep(false);
                    setNotice(null);
                  }
                }, "返回"),
                React2.createElement("button", {
                  className: "dim-jh-btn",
                  "data-kind": "primary",
                  disabled: busy,
                  onClick: () => void confirmImport()
                }, busy ? "导入中…" : "确认导入")
              ) : React2.createElement("button", {
                className: "dim-jh-btn",
                "data-kind": "primary",
                disabled: busy,
                onClick: () => void stepImport()
              }, busy ? "处理中…" : "下一步")
            )
          )
        )
      );
    };
    return React2.createElement(
      React2.Fragment,
      null,
      React2.createElement("button", {
        className: "dim-jh-btn",
        title: "导出全部账号的密钥与凭据，便于更换 DSH 版本后导入恢复。",
        onClick: () => {
          setDialog("export");
          setNotice(null);
        }
      }, "备份"),
      React2.createElement("button", {
        className: "dim-jh-btn",
        title: "从备份文件恢复账号与凭据（会覆盖当前全部账号）。",
        onClick: () => fileRef.current?.click()
      }, "恢复"),
      React2.createElement("input", {
        ref: fileRef,
        type: "file",
        accept: ".json,application/json",
        style: { display: "none" },
        onChange: onFileSelected
      }),
      dialog !== null ? renderDialog() : null
    );
  }
  function downloadJson(filename, data) {
    const blob = new Blob([JSON.stringify(data, null, 2)], { type: "application/json" });
    const url = URL.createObjectURL(blob);
    const anchor = document.createElement("a");
    anchor.href = url;
    anchor.download = filename;
    document.body.appendChild(anchor);
    anchor.click();
    anchor.remove();
    setTimeout(() => URL.revokeObjectURL(url), 1e3);
  }
  function readFileAsText(file) {
    return new Promise((resolve, reject) => {
      const reader = new FileReader();
      reader.onload = () => resolve(reader.result);
      reader.onerror = () => reject(reader.error);
      reader.readAsText(file);
    });
  }
  function providerRowSummary(row) {
    if (row.models === null && row.accounts === null) return "状态尚未读取";
    const models = row.models ? `模型 ${row.models.total ?? 0}（已关 ${row.models.disabled ?? 0}）` : "模型 —";
    const accounts = row.accounts ? `账号 ${row.accounts.total ?? 0}（启用 ${row.accounts.enabled ?? 0}）` : "账号 —";
    return `${models} · ${accounts}`;
  }
  function ProviderSwitchPanel({ providers, statuses, statusFailed, busyIds, onToggle, onReload, onClose, order, onCommitOrder, reordering, notice }) {
    React2.useEffect(() => {
      const onKeyDown = (event) => {
        if (event.key === "Escape") onClose();
      };
      document.addEventListener("keydown", onKeyDown);
      return () => document.removeEventListener("keydown", onKeyDown);
    }, [onClose]);
    const [draggingId, setDraggingId] = React2.useState(null);
    const [dropTargetId, setDropTargetId] = React2.useState(null);
    const [dropPosition, setDropPosition] = React2.useState("before");
    const rows = providerSwitchRows(providers, statuses, order);
    const summary = providerToggleSummary(providers, statuses);
    const countText = summary.known ? `共 ${summary.total} 个，已打开 ${summary.open}，已关闭 ${summary.closed}` : statusFailed ? "状态读取失败" : "正在读取状态…";
    const dragEnabled = rows.filter((row) => row.checked).length >= 2 && busyIds.size === 0 && !reordering;
    const dragPropsFor = (row) => {
      if (!row.checked || !dragEnabled) return { enabled: false };
      return {
        enabled: true,
        isDragging: draggingId === row.id,
        isDropTarget: dropTargetId === row.id && draggingId !== null && draggingId !== row.id,
        dropPosition,
        onDragStart: (event) => {
          setDraggingId(row.id);
          try {
            event.dataTransfer.setData("text/plain", row.id);
          } catch {
          }
          if (event.dataTransfer) event.dataTransfer.effectAllowed = "move";
        },
        onDragEnd: () => {
          setDraggingId(null);
          setDropTargetId(null);
        },
        onDragOver: (event) => {
          if (draggingId === null || draggingId === row.id) return;
          event.preventDefault();
          if (event.dataTransfer) event.dataTransfer.dropEffect = "move";
          if (dropTargetId !== row.id) setDropTargetId(row.id);
          const rect = event.currentTarget?.getBoundingClientRect?.();
          const next = dropPositionFromPointer(event.clientY, rect);
          if (next !== dropPosition) setDropPosition(next);
        },
        onDrop: (event) => {
          event.preventDefault();
          const sourceId = draggingId;
          setDraggingId(null);
          setDropTargetId(null);
          if (sourceId === null || sourceId === row.id) return;
          const openIds = rows.filter((r) => r.checked).map((r) => r.id);
          const closedIds = rows.filter((r) => !r.checked).map((r) => r.id);
          const next = nextProviderOrderAfterDrop(openIds, closedIds, sourceId, row.id, dropPosition, order);
          if (next !== null) void onCommitOrder(next);
        }
      };
    };
    return React2.createElement(
      "div",
      {
        // `--top`：顶部锚定。行数固定但窗口高度会变，垂直居中会让弹窗上下跳动
        // （与模型列表同款，见 jet-hub-styles.js 中该修饰类的说明）。
        className: "dim-jh-modalOverlay dim-jh-modalOverlay--top",
        onClick: (event) => {
          if (event.target === event.currentTarget) onClose();
        }
      },
      React2.createElement(
        "div",
        {
          className: "dim-jh-modal",
          role: "dialog",
          "aria-modal": "true",
          "aria-label": "供应商开关"
        },
        React2.createElement(
          "div",
          { className: "dim-jh-modalHead" },
          React2.createElement(
            "div",
            { className: "dim-jh-modalTitle" },
            React2.createElement("strong", null, "供应商开关"),
            React2.createElement("span", { className: "dim-jh-modelPanelCount" }, countText)
          ),
          React2.createElement(
            "div",
            { className: "dim-jh-modelPanelActions" },
            React2.createElement("button", {
              className: "dim-jh-btn",
              title: "重新读取各供应商的模型数与账号数。",
              onClick: () => void onReload()
            }, "刷新"),
            React2.createElement("button", {
              className: "dim-jh-btn",
              "data-kind": "primary",
              onClick: onClose
            }, "完成")
          )
        ),
        React2.createElement(
          "p",
          { className: "dim-jh-modalHint" },
          "关闭一个供应商 = 关闭它的全部模型（从对话框的模型选择里移除）并停用它的全部账号；打开则恢复。关闭前会再确认一次。没有可关闭模型的供应商会被禁用（服务端也会拒绝）。拖动已打开的行可自定义顺序（顺序只保存在本机）；已关闭的供应商仍按默认顺序显示在底部。"
        ),
        // ⚠️ 供应商相关的提示必须在**这里**（弹窗内）显示，不能只挂在页面层：
        // 拖拽与开关切换都发生在弹窗打开期间，而弹窗遮罩是 fixed + z-index 3000，
        // 页面层的 notice 会被它整个盖住 —— 用户只会看到「操作没生效，却什么提示都没有」。
        // ⚠️ 弹窗开着时**显示全部** notice（含 toggleProvider 的结果/失败提示）——
        // 它们此刻在页面层同样被遮住，只按 inModal 过滤会让切换结果一样消失。
        // 弹窗关闭即不挂载、页面层那条照旧渲染，两边不会重复出现。
        // 复用既有 `.dim-jh-probeNotice` + `.dim-jh-modal .dim-jh-probeNotice` 样式，不新增样式。
        notice ? React2.createElement("div", {
          className: "dim-jh-probeNotice",
          "data-tone": notice.tone,
          role: notice.tone === "error" ? "alert" : "status"
        }, React2.createElement("div", null, notice.text)) : null,
        React2.createElement(
          "div",
          { className: "dim-jh-modalBody" },
          React2.createElement(
            "div",
            { className: "dim-jh-modelList" },
            rows.map((row) => {
              const busy = busyIds.has(row.id);
              const action = row.checked ? "关闭" : "打开";
              const drag = dragPropsFor(row);
              return React2.createElement(
                "label",
                {
                  key: row.id,
                  className: "dim-jh-modelRow",
                  // 与模型列表同语义：`data-disabled` 表示**这一项已被关闭**（整行淡出），
                  // 不是「这行点不动」—— 点不动由下面 input 的 disabled 表达。
                  "data-disabled": row.checked ? "false" : "true",
                  // 拖拽排序（与账号卡片同款视觉语言）：源行淡出、落点行画插入线。
                  draggable: drag.enabled ? "true" : void 0,
                  onDragStart: drag.onDragStart,
                  onDragEnd: drag.onDragEnd,
                  onDragOver: drag.onDragOver,
                  onDrop: drag.onDrop,
                  "data-dragging": drag.isDragging ? "true" : void 0,
                  "data-dropBefore": drag.isDropTarget && drag.dropPosition !== "after" ? "true" : void 0,
                  "data-dropAfter": drag.isDropTarget && drag.dropPosition === "after" ? "true" : void 0,
                  title: row.disabled ? row.reason : `${action}「${row.label}」：${action}它的全部模型并${row.checked ? "停用" : "启用"}全部账号`
                },
                drag.enabled ? React2.createElement("span", {
                  className: "dim-jh-dragHandle",
                  title: "拖动以调整顺序（顺序只保存在本机）",
                  "aria-hidden": "true",
                  // ⚠️ 行容器是 <label>：点柄的 click 会沿 label 激活开关。preventDefault
                  // 掐掉这条激活路径 —— 拖柄不是开关，点了不该有开关反应（拖拽本身
                  // 不经过 click，互不影响）。
                  onClick: (event) => {
                    event.preventDefault();
                    event.stopPropagation();
                  }
                }, "⠿") : null,
                React2.createElement(
                  "span",
                  { className: "dim-jh-modelInfo" },
                  React2.createElement("strong", { className: "dim-jh-modelName" }, row.label),
                  React2.createElement("code", { className: "dim-jh-modelId" }, providerRowSummary(row))
                ),
                React2.createElement("input", {
                  type: "checkbox",
                  className: "dim-jh-switch",
                  role: "switch",
                  checked: row.checked,
                  // busy 只锁被点的那一行：状态是服务端推导的，锁整表会让用户以为全挂了。
                  disabled: row.disabled || busy,
                  "aria-label": `${action} ${row.label}`,
                  onChange: () => void onToggle(row.id, !row.checked)
                })
              );
            })
          )
        )
      )
    );
  }
  function GatewayModelRow({ model }) {
    const badge = modelCapabilityBadge(model);
    return React2.createElement(
      "div",
      {
        className: "dim-jh-gatewayModelRow",
        // tooltip = 完整 ID（可直接选中复制）+ 展示名，截图/复制时信息不丢。
        title: `${model.id}
${model.name || ""}`.trim()
      },
      React2.createElement("code", { className: "dim-jh-modelId" }, gatewayModelKeyOf(model)),
      React2.createElement("span", { className: "dim-jh-modelName" }, model.name || model.id),
      badge ? React2.createElement("span", { className: "dim-jh-modelBadge", title: "该模型接受图片输入。" }, badge) : null
    );
  }
  function GatewayEffortRow({ row }) {
    return React2.createElement(
      "div",
      {
        className: "dim-jh-effortRow",
        title: `${row.id}
真实档位：${row.declared}
客户端该填：${row.fill}`
      },
      React2.createElement(
        "div",
        { className: "dim-jh-effortHead" },
        // 卡片头已写明供应商，行内显示去掉前缀的名字；完整 id 在 tooltip 与复制里。
        React2.createElement("code", { className: "dim-jh-effortModel" }, gatewayModelKeyOf(row)),
        row.lossy ? React2.createElement("span", {
          className: "dim-jh-modelBadge",
          title: "该模型的档位名与 OpenAI 客户端不同名：照 DSH 界面上的名字填，网关会按强度就近翻译。"
        }, "需对照") : null
      ),
      React2.createElement(
        "div",
        { className: "dim-jh-effortLine" },
        React2.createElement("span", { className: "dim-jh-effortLabel" }, "真实档位："),
        row.declared
      ),
      React2.createElement(
        "div",
        { className: "dim-jh-effortLine" },
        React2.createElement("span", { className: "dim-jh-effortLabel" }, "客户端该填："),
        row.fill
      )
    );
  }
  function GatewayCards({ cards, renderRow, isExpanded, onToggle, onCopy, isCopied, foldSuffix, foldLabel, copyNoun }) {
    return React2.createElement(
      "div",
      { className: "dim-jh-gatewayCards", style: { marginTop: "6px" } },
      cards.map((card) => {
        const expanded = isExpanded(card.provider);
        const label = gatewayCardLabel(card.provider, providerLabel);
        return React2.createElement(
          "div",
          {
            // ⚠️ 供应商 key 拿不到时用 `__unknown__`：`key: ''` 会让 React 报警并可能丢行。
            key: card.provider === "" ? "__unknown__" : card.provider,
            className: "dim-jh-gatewayCard"
          },
          React2.createElement(
            "div",
            { className: "dim-jh-gatewayCardHead" },
            React2.createElement("button", {
              className: "dim-jh-gatewayCardToggle",
              "aria-expanded": expanded ? "true" : "false",
              // tooltip 里给出**原始 provider key**：卡片标题是展示名，
              // 而用户要复制进配置的是 ID 里的那个 key，两者可能不同。
              title: card.provider === "" ? "这些条目没有回传供应商字段（上游数据异常），仍照实列出。" : `供应商 key：${card.provider}（模型 ID 的前缀就是它）`,
              onClick: () => onToggle(card.provider)
            }, `${expanded ? "▾" : "▸"} ${label}`),
            React2.createElement(
              "span",
              { className: "dim-jh-gatewayCardCount" },
              card.counts.fold > 0 && foldLabel !== "" ? `${card.counts.total} ${foldSuffix} · ${card.counts.fold} 个${foldLabel}` : `${card.counts.total} ${foldSuffix}`
            ),
            React2.createElement("button", {
              className: "dim-jh-btn dim-jh-gatewayCardBtn",
              title: `只复制「${label}」这一家的 ${card.counts.total} ${copyNoun}（每行一个完整 ID）。`,
              onClick: () => onCopy(card)
            }, isCopied(card.provider) ? "已复制 ✓" : "复制本组")
          ),
          expanded ? React2.createElement(
            "div",
            { className: "dim-jh-gatewayCardBody" },
            card.entries.map((entry, index) => React2.createElement(
              React2.Fragment,
              { key: `${card.provider}#${entry.id}#${index}` },
              renderRow(entry)
            ))
          ) : null
        );
      })
    );
  }
  function GatewayPanel({ status, busy, notice, onToggle, onReload, onClose }) {
    const [copied, setCopied] = React2.useState(false);
    const [revealed, setRevealed] = React2.useState(false);
    const [modelsOpen, setModelsOpen] = React2.useState(false);
    const [effortsOpen, setEffortsOpen] = React2.useState(false);
    const [modelsQuery, setModelsQuery] = React2.useState("");
    const [effortsQuery, setEffortsQuery] = React2.useState("");
    const [effortsCopied, setEffortsCopied] = React2.useState(false);
    const [idsCopied, setIdsCopied] = React2.useState(false);
    const [modelCardToggles, setModelCardToggles] = React2.useState({});
    const [effortCardToggles, setEffortCardToggles] = React2.useState({});
    const [copiedModelCard, setCopiedModelCard] = React2.useState(null);
    const [copiedEffortCard, setCopiedEffortCard] = React2.useState(null);
    const apiKey = status?.apiKey ?? null;
    const models = status?.models ?? [];
    const modelsFiltering = isFilterActive({ query: modelsQuery });
    const visibleModels = filterModels(models, { query: modelsQuery });
    const modelCards = groupGatewayModels(visibleModels);
    const effortRows = gatewayEffortRows(models);
    const visibleEffortRows = filterEffortRows(effortRows, effortsQuery);
    const effortsFiltering = isFilterActive({ query: effortsQuery });
    const effortCards = groupGatewayEntries(visibleEffortRows, (row) => row.lossy);
    React2.useEffect(() => {
      setCopied(false);
      setRevealed(false);
    }, [status?.apiKey?.value]);
    React2.useEffect(() => {
      setIdsCopied(false);
      setCopiedModelCard(null);
    }, [models.length, modelsQuery]);
    React2.useEffect(() => {
      setEffortsCopied(false);
      setCopiedEffortCard(null);
    }, [models.length, effortsQuery]);
    React2.useEffect(() => {
      const onKeyDown = (event) => {
        if (event.key === "Escape") onClose();
      };
      document.addEventListener("keydown", onKeyDown);
      return () => document.removeEventListener("keydown", onKeyDown);
    }, [onClose]);
    const handleCopy = async () => {
      if (!apiKey) return;
      const ok = await copyToClipboard(apiKey.value);
      if (ok) {
        setCopied(true);
        return;
      }
      setRevealed(true);
      setCopied(false);
    };
    const handleCopyModelIds = async () => {
      const text = formatModelIdList(visibleModels);
      if (!text) return;
      const ok = await copyToClipboard(text);
      setIdsCopied(ok);
      if (!ok) setModelsOpen(true);
    };
    const handleCopyCardIds = async (card) => {
      const text = formatModelIdList(card.entries);
      if (!text) return;
      const ok = await copyToClipboard(text);
      setCopiedModelCard(ok ? card.provider : null);
      if (!ok) setModelsOpen(true);
    };
    const handleCopyEfforts = async () => {
      const text = gatewayEffortsText(visibleEffortRows);
      if (!text) return;
      const ok = await copyToClipboard(text);
      setEffortsCopied(ok);
      if (!ok) setEffortsOpen(true);
    };
    const handleCopyCardEfforts = async (card) => {
      const text = gatewayEffortsText(card.entries);
      if (!text) return;
      const ok = await copyToClipboard(text);
      setCopiedEffortCard(ok ? card.provider : null);
      if (!ok) setEffortsOpen(true);
    };
    const disabled = gatewaySwitchDisabled(status);
    const action = status?.enabled ? "关闭" : "打开";
    return React2.createElement(
      "div",
      {
        className: "dim-jh-modalOverlay dim-jh-modalOverlay--top",
        onClick: (event) => {
          if (event.target === event.currentTarget) onClose();
        }
      },
      React2.createElement(
        "div",
        {
          className: "dim-jh-modal dim-jh-gatewayModal",
          role: "dialog",
          "aria-modal": "true",
          "aria-label": "本机 OpenAI 网关"
        },
        React2.createElement(
          "div",
          { className: "dim-jh-modalHead" },
          React2.createElement(
            "div",
            { className: "dim-jh-modalTitle" },
            React2.createElement("strong", null, "本机 OpenAI 网关")
          ),
          React2.createElement(
            "div",
            { className: "dim-jh-modelPanelActions" },
            React2.createElement("button", {
              className: "dim-jh-btn",
              title: "重新读取网关状态。",
              onClick: () => void onReload()
            }, "刷新"),
            React2.createElement("button", {
              className: "dim-jh-btn",
              "data-kind": "primary",
              onClick: onClose
            }, "完成")
          )
        ),
        React2.createElement(
          "div",
          { className: "dim-jh-modalBody" },
          // ── 第 1 段：开关与连接信息 ──
          // ⚠️ 用户 2026-10-04 要求把三段用**可见的分段**区分开（原话：「这三坨字全混在
          // 一起有点难看清」）。做法是每段包一个 .dim-jh-gatewaySection：
          // 段与段之间用上边框 + 更大的间距分开，段内保持紧凑。
          // ⚠️ 不要用「给每段加不同底色」：弹窗会被截图，色块在深色主题下还要另配一套 token。
          React2.createElement(
            "div",
            { className: "dim-jh-gatewaySection" },
            React2.createElement(
              "label",
              { className: "dim-jh-modelRow" },
              React2.createElement(
                "span",
                { className: "dim-jh-modelInfo" },
                React2.createElement("strong", { className: "dim-jh-modelName" }, "启用本机网关"),
                // ⚠️ 用户 2026-10-04 要求这句只留「在 127.0.0.1 监听，供客户端调用」——
                // 原先列了 Pi / Continue / Cline / OpenCode 四个客户端名，在窄面板下折成两行
                // 且把「打开/关闭」那个动作词挤到很后面。客户端列举属于 README 的内容。
                React2.createElement(
                  "code",
                  { className: "dim-jh-modelId" },
                  "在 127.0.0.1 监听，供客户端调用"
                )
              ),
              React2.createElement("input", {
                type: "checkbox",
                className: "dim-jh-switch",
                role: "switch",
                checked: status?.enabled === true,
                // 被 env 停用时禁用而非「点了没反应」：让用户做一次明知无效的操作更困惑。
                disabled: disabled || busy,
                "aria-label": action + "本机网关",
                onChange: () => void onToggle(!(status?.enabled === true))
              })
            ),
            gatewayStatusLines(status).map((line, index) => (
              // ⚠️ 顶部三行是同一段话，用更紧的 .dim-jh-gatewayLine（再叠上弹窗级的
              // 6px 会显得像三段互不相干的内容）。
              React2.createElement("p", {
                key: `status-${index}`,
                className: "dim-jh-modalHint dim-jh-gatewayLine"
              }, line)
            )),
            // 凭据区：默认**不**渲染明文（设置页会被截图/录屏/投屏）。点「复制」直接
            // 进剪贴板；复制失败（无 Clipboard API、非用户手势、权限被拒）则退回显示
            // 明文供手动选中 —— 绝不能变成「点了没反应」。
            //
            // ⚠️ 用户 2026-10-04 要求精简：原先是一行「鉴权用 Authorization: Bearer
            // <网关 API Key>。」+ 下面一排按钮，现改为「API KEY：」与两个按钮**同一行**。
            // Bearer 那半句没丢 —— 它移到了 CC Switch 之外的客户端需要的信息里，见 README
            // 的「鉴权使用」；面板上只留用户真正要抄的两个东西（地址、密钥）。
            apiKey ? React2.createElement(
              "div",
              { className: "dim-jh-gatewayKeyRow", style: { marginTop: "6px" } },
              React2.createElement("span", { className: "dim-jh-modalHint dim-jh-gatewayKeyLabel" }, "API KEY："),
              React2.createElement("button", {
                className: "dim-jh-btn",
                "data-kind": "primary",
                title: "把密钥复制到剪贴板。明文会进入剪贴板历史，注意别在不信任的机器上这么做。",
                onClick: () => void handleCopy()
              }, copied ? "已复制 ✓" : "复制密钥"),
              revealed ? React2.createElement("code", {
                className: "dim-jh-modelId",
                style: { userSelect: "all" }
              }, apiKey.value) : React2.createElement("button", {
                className: "dim-jh-btn",
                title: "自动复制不可用时用它显示明文，供手动选中。",
                onClick: () => setRevealed(true)
              }, "显示明文")
            ) : React2.createElement(
              "p",
              { className: "dim-jh-modalHint" },
              "API KEY：尚未生成（启用网关时会自动创建）。"
            ),
            // ⚠️ 只有拿到密钥时才补这一行来源说明：`apiKey` 为空时上面那句
            // 「尚未生成（启用网关时会自动创建）」已经把事情说完，而
            // `gatewayApiKeyHint(null)` 说的是同一件事 —— 两句并排就是冗余，
            // 与本次「精简臃肿」的目标正好相反。
            apiKey ? React2.createElement("p", { className: "dim-jh-modalHint" }, gatewayApiKeyHint(apiKey)) : null
          ),
          // ── 第 2 段：模型目录 ──
          // 存在的理由：有些 agent（ZCode 等）**不会**主动扫 `/v1/models`，要靠用户
          // 手工把 ID 填进配置。而该端点需要 Bearer 头，浏览器地址栏直接打开只会得到
          // 401 —— 所以清单必须出现在设置页里。
          React2.createElement(
            "div",
            { className: "dim-jh-gatewaySection" },
            React2.createElement(
              "p",
              { className: "dim-jh-gatewaySectionTitle" },
              React2.createElement("strong", null, "模型 ID（可用的完整清单）")
            ),
            React2.createElement("p", { className: "dim-jh-modalHint" }, gatewayModelsHint(models, status?.modelsSource)),
            // ⚠️ `dim-jh-gatewayActions` 是给「下方四个按钮压成与密钥行同款小号」用的钩子
            // （见 jet-hub-styles.js 里与 .dim-jh-gatewayKeyRow 合并的那条规则）。
            // 不能直接改 .dim-jh-modelPanelActions .dim-jh-btn —— 那个类在页头、模型面板、
            // 卡片头等 8 处都在用，改它会把无关页面一起改小。
            React2.createElement(
              "div",
              { className: "dim-jh-modelPanelActions dim-jh-gatewayActions", style: { marginTop: "4px" } },
              React2.createElement("button", {
                className: "dim-jh-btn",
                "data-kind": "primary",
                disabled: visibleModels.length === 0,
                title: "把模型 ID 每行一个复制到剪贴板（每行一个完整 ID；有搜索词时只复制筛出来的那些）。",
                onClick: () => void handleCopyModelIds()
              }, idsCopied ? "已复制 ✓" : `复制全部 ${visibleModels.length} 个 ID`),
              React2.createElement("button", {
                className: "dim-jh-btn",
                "aria-expanded": modelsOpen ? "true" : "false",
                onClick: () => {
                  if (modelsOpen) setModelsQuery("");
                  setModelsOpen((open) => !open);
                }
              }, modelsOpen ? "收起清单" : "展开清单")
            ),
            modelsOpen ? React2.createElement(
              React2.Fragment,
              null,
              React2.createElement(
                "div",
                { className: "dim-jh-modelFilterBar" },
                React2.createElement("input", {
                  type: "search",
                  className: "dim-jh-input dim-jh-modelSearch",
                  placeholder: "搜索模型 ID 或展示名…",
                  value: modelsQuery,
                  "aria-label": "搜索模型 ID",
                  onChange: (event) => setModelsQuery(event.target.value)
                }),
                modelsFiltering ? React2.createElement("button", {
                  className: "dim-jh-btn",
                  title: "清空搜索词，恢复完整清单。",
                  onClick: () => setModelsQuery("")
                }, "清空搜索") : null
              ),
              visibleModels.length === 0 ? React2.createElement("div", { className: "dim-jh-gatewayEmpty" }, gatewayModelsEmptyHint(modelsQuery)) : React2.createElement(GatewayCards, {
                cards: modelCards,
                renderRow: (model) => React2.createElement(GatewayModelRow, { model }),
                isExpanded: (provider) => gatewayCardExpanded(
                  modelCards.find((card) => card.provider === provider),
                  // 有搜索词时一律展开：命中结果藏在折叠卡里，用户会以为「搜不到」。
                  { toggled: modelCardToggles[provider], searching: modelsFiltering }
                ),
                onToggle: (provider) => setModelCardToggles((prev) => ({
                  ...prev,
                  [provider]: !gatewayCardExpanded(
                    modelCards.find((card) => card.provider === provider),
                    { toggled: prev[provider], searching: modelsFiltering }
                  )
                })),
                onCopy: (card) => void handleCopyCardIds(card),
                isCopied: (provider) => copiedModelCard === provider,
                foldSuffix: "个模型",
                foldLabel: "可发图片",
                copyNoun: "个完整模型 ID"
              })
            ) : null
          ),
          // ── 第 3 段：思考档位对照表 ──
          // 存在的理由：各 provider 的档位 id 是**上游私有值**（TRAE 的 light/extra_high、
          // LobsterAI 的 xhigh（界面上叫 Max）、Cline 的 max（界面上叫 Extra）），而走
          // OpenAI 协议的客户端只有固定 8 档词汇，表达不出这些私有值。用户照 DSH 界面上
          // 的名字填进 CC Switch/Codex，撞 400 且两端都看不出原因。
          // 网关现在按强度自动翻译（填错不再失败），但**精确对应**只有一列答案 —— 就是这里。
          //
          // ⚠️ 数据只含**已开启**的模型（来自适配器 listModels 的黑名单过滤），
          // 故与对话框里的模型选择器同源。
          React2.createElement(
            "div",
            { className: "dim-jh-gatewaySection" },
            React2.createElement(
              "p",
              { className: "dim-jh-gatewaySectionTitle" },
              React2.createElement("strong", null, "思考档位对照表")
            ),
            // ⚠️ 说明**一行一个 p**（与弹窗顶部 gatewayStatusLines 同款做法）。
            // 用户 2026-10-04 第三次报障：这两句原先拼成一整段交给浏览器断行，
            // 于是品牌名被从中间切开 —— 前一行结尾是「CC」、下一行开头是「Switch」。
            // 两句本就各说一件事，各占一行既不会断开品牌名，也更好读。
            gatewayEffortsHintLines(models).map((line, index) => React2.createElement("p", {
              key: `efforts-hint-${index}`,
              className: "dim-jh-modalHint dim-jh-gatewayLine"
            }, line)),
            // ⚠️ 用户 2026-10-04 要求这两个按钮**互换位置**，与模型清单那段保持一致
            //（那边是「复制全部 …」在前、「展开清单」在后）。顺序统一后，
            // 三段里的按钮位置可预期，用户不必每段重新找。
            React2.createElement(
              "div",
              { className: "dim-jh-modelPanelActions dim-jh-gatewayActions", style: { marginTop: "4px" } },
              // 复制的是**当前筛出来的那些行**：用户搜完再复制，拿到的正是屏幕上看到的内容。
              effortRows.length > 0 ? React2.createElement("button", {
                className: "dim-jh-btn",
                "data-kind": "primary",
                title: "把对照表（含真实档位与该填的值）复制到剪贴板。有搜索词时只复制筛出来的行。",
                onClick: () => void handleCopyEfforts()
              }, effortsCopied ? "已复制 ✓" : `复制对照表（${visibleEffortRows.length} 行）`) : null,
              React2.createElement("button", {
                className: "dim-jh-btn",
                "aria-expanded": effortsOpen ? "true" : "false",
                // 没有模型声明档位时禁用而非「点了没反应」：展开一张空表毫无意义。
                disabled: effortRows.length === 0,
                onClick: () => {
                  if (effortsOpen) setEffortsQuery("");
                  setEffortsOpen((open) => !open);
                }
              }, effortsOpen ? "收起对照表" : `展开对照表（${effortRows.length} 行）`)
            ),
            effortsOpen ? React2.createElement(
              React2.Fragment,
              null,
              // 搜索：对照表本身要能按**档位名**查（「哪些渠道有 xhigh？」「界面上的 Max 是哪个 id？」
              // —— 后者是用户真踩过的坑）。故判据比模型清单的搜索多匹配档位名，
              // 但「空搜索词 = 未搜索」的约定与它共用同一份实现（model-filter.js）。
              //
              // ⚠️ 用户 2026-10-04 要求它**展开后才可用**（原先挂在展开区之外，收起时也在占位）。
              effortRows.length > 0 ? React2.createElement(
                "div",
                { className: "dim-jh-modelFilterBar" },
                React2.createElement("input", {
                  type: "search",
                  className: "dim-jh-input dim-jh-modelSearch",
                  placeholder: "搜索模型 id、展示名或档位名（如 xhigh / Max / Extra）…",
                  value: effortsQuery,
                  "aria-label": "搜索思考档位",
                  onChange: (event) => setEffortsQuery(event.target.value)
                }),
                effortsFiltering ? React2.createElement("button", {
                  className: "dim-jh-btn",
                  title: "清空搜索词，恢复完整对照表。",
                  onClick: () => setEffortsQuery("")
                }, "清空搜索") : null
              ) : null,
              visibleEffortRows.length === 0 ? React2.createElement("div", { className: "dim-jh-effortEmpty" }, gatewayEffortsEmptyHint(effortsQuery)) : React2.createElement(GatewayCards, {
                cards: effortCards,
                renderRow: (row) => React2.createElement(GatewayEffortRow, { row }),
                isExpanded: (provider) => gatewayCardExpanded(
                  effortCards.find((card) => card.provider === provider),
                  // ⚠️ 有搜索词时一律展开：命中结果藏在折叠卡里，用户会以为「搜不到」
                  // （与 `model-groups.js` 的同一取舍）。
                  { toggled: effortCardToggles[provider], searching: effortsFiltering }
                ),
                onToggle: (provider) => setEffortCardToggles((prev) => ({
                  ...prev,
                  [provider]: !gatewayCardExpanded(
                    effortCards.find((card) => card.provider === provider),
                    { toggled: prev[provider], searching: effortsFiltering }
                  )
                })),
                onCopy: (card) => void handleCopyCardEfforts(card),
                isCopied: (provider) => copiedEffortCard === provider,
                foldSuffix: "个可选档位",
                foldLabel: "需对照",
                copyNoun: "个模型的档位对照"
              })
            ) : null
          ),
          // ⚠️ 用户 2026-10-04 要求**删掉两段 curl 命令**（模型目录与对照表各一条）。
          // 它们已被 README 的「拿到地址与密钥」/「思考档位」两节完整收录，
          // 而弹窗里那两段是「命令行查看同一份…」的重复说明 —— 正是用户说的「三坨字混在一起」。
          React2.createElement(
            "p",
            { className: "dim-jh-modalHint dim-jh-gatewayFootnote" },
            "网关只绑定 127.0.0.1，但这挡不住同机的其它用户或进程 —— 真正的隔离靠密钥，不要把它配置进任何浏览器端工具或扩展。"
          ),
          notice ? React2.createElement("div", {
            className: "dim-jh-probeNotice",
            "data-tone": notice.tone,
            role: notice.tone === "error" ? "alert" : "status",
            style: { marginTop: "10px" }
          }, React2.createElement("div", null, notice.text)) : null
        )
      )
    );
  }
  function AggregatePanel({ rpcCall }) {
    const [models, setModels] = React2.useState(null);
    const [rejections, setRejections] = React2.useState({});
    const [busy, setBusy] = React2.useState(false);
    const [notice, setNotice] = React2.useState(null);
    const [expanded, setExpanded] = React2.useState({});
    const load = React2.useCallback(async (force = false) => {
      setBusy(true);
      try {
        const res = await rpcCall("aggregate.catalog", { force });
        const models2 = res?.models ?? [];
        setModels(models2);
        setRejections(rejectionsFromCatalog(models2));
        setNotice(null);
      } catch (caught) {
        setNotice(`读取聚合目录失败：${caught?.message ?? String(caught)}`);
      } finally {
        setBusy(false);
      }
    }, [rpcCall]);
    React2.useEffect(() => {
      void load();
    }, [load]);
    const onToggleRejected = React2.useCallback(async (canonicalId, provider, realId) => {
      const next = !isRejected(rejections, canonicalId, provider, realId);
      setRejections((prev) => toggleRejection(prev, canonicalId, provider, realId, next));
      try {
        const res = await rpcCall("aggregate.setRejected", {
          canonicalId,
          provider,
          realId,
          rejected: next
        });
        await load(true);
        setNotice(null);
      } catch (caught) {
        setNotice(`保存失败：${caught?.message ?? String(caught)}`);
        await load();
      }
    }, [rejections, rpcCall, load]);
    const sortedModels = sortModelsForPanel(models ?? []);
    const [expiryOrder, setExpiryOrder] = React2.useState(null);
    const [expiryBusy, setExpiryBusy] = React2.useState(false);
    const onSortByExpiry = React2.useCallback(async () => {
      setExpiryBusy(true);
      try {
        const res = await rpcCall("aggregate.expiryOrder", {});
        setExpiryOrder(res?.order ?? {});
        setNotice(null);
      } catch (caught) {
        setNotice(`读取临期顺序失败：${caught?.message ?? String(caught)}`);
      } finally {
        setExpiryBusy(false);
      }
    }, [rpcCall]);
    const renderCandidate = (canonicalId, candidate) => {
      const rejected = isRejected(rejections, canonicalId, candidate.provider, candidate.realId);
      const expiry = expiryOrder === null ? void 0 : expiryOrder[candidate.provider];
      return React2.createElement(
        "div",
        {
          className: "dim-jh-aggCandidate" + (rejected ? " dim-jh-aggCandidateRejected" : ""),
          key: `${candidate.provider}\0${candidate.realId}`
        },
        React2.createElement(
          "span",
          { className: "dim-jh-aggCandidateLabel" },
          candidateRowLabel(candidate)
        ),
        // ⚠️ 只有探测过才显示到期提示 —— 否则显示「到期未知」会让用户以为查过了。
        expiry === void 0 ? null : React2.createElement("span", {
          className: "dim-jh-aggCandidateExpiry",
          title: "按临期排序时算出的到期时刻"
        }, expiryLabel(expiry)),
        React2.createElement("button", {
          type: "button",
          className: "dim-jh-aggToggle" + (rejected ? " dim-jh-aggToggleOff" : ""),
          // ⚠️ 语义是「**参与**轮换」的正向开关，与用户直觉一致：
          //    打开 = 允许用这个渠道；关闭 = 不同意参与轮换（规格 §7.1 的原话）。
          "aria-pressed": String(!rejected),
          title: rejected ? "已拒绝：该渠道不参与这个模型的轮换" : "参与轮换",
          onClick: () => {
            void onToggleRejected(canonicalId, candidate.provider, candidate.realId);
          }
        }, rejected ? "已拒绝" : "参与轮换")
      );
    };
    const renderModel = (model) => {
      const open = expanded[model.canonicalId] === true;
      const candidates = expiryOrder === null ? model.candidates : sortCandidatesByExpiry(model.candidates.map((c) => ({
        ...c,
        expiry: expiryOrder[c.provider]
      })));
      return React2.createElement(
        "div",
        { className: "dim-jh-aggModel", key: model.canonicalId },
        React2.createElement(
          "button",
          {
            type: "button",
            className: "dim-jh-aggModelHead",
            "aria-expanded": String(open),
            onClick: () => setExpanded((prev) => ({ ...prev, [model.canonicalId]: !open }))
          },
          React2.createElement(
            "span",
            { className: "dim-jh-aggModelName" },
            model.name || model.canonicalId
          ),
          React2.createElement(
            "span",
            { className: "dim-jh-aggModelMeta" },
            // ⚠️ 措辞必须与**实际**顺序一致：探测过才说「按临期」，
            //    否则说「按渠道名」（原先恒写「临期优先」而实际是字母序 —— 面板没说真话）。
            `${model.candidates.length} 个渠道 · ${expiryOrder === null ? "按渠道名" : "按临期"}`
          ),
          // ⚠️ **候选全部被拒 ⇒ 必须显式提示**（规格 §5.2 末段的成文要求）：
          //    「把某虚拟模型的候选**全部**拒绝（L3）**不等于**关闭该虚拟模型 ——
          //      前者仍然出现在目录里（只是无候选可用，请求时如实报错），后者从目录消失。
          //      **面板需对「候选全部被拒」给出显式提示，避免用户以为模型坏了。**」
          //    ⚠️ 没有它时，用户选了该模型拿到的报错是「没有任何可用候选（渠道被关闭、
          //    模型被关、或所有渠道都无可用账号）」—— **不会**提到「是你自己全部拒绝的」，
          //    于是用户以为模型坏了或以为没登录。
          isAllCandidatesRejected(model, rejections) ? React2.createElement("span", {
            className: "dim-jh-aggAllRejected",
            title: "你把该模型的全部候选都关了「参与轮换」——请求会如实报「没有任何可用候选」。若想恢复，展开后在子列表里重新打开至少一条。"
          }, "⚠️ 全部被拒") : null,
          React2.createElement("span", { className: "dim-jh-aggChevron" }, open ? "▾" : "▸")
        ),
        open ? React2.createElement(
          "div",
          { className: "dim-jh-aggCandidates" },
          candidates.map((candidate) => renderCandidate(model.canonicalId, candidate))
        ) : null
      );
    };
    return React2.createElement(
      "div",
      {
        className: "dim-jh-aggPanel"
      },
      // ── 说明区 ──
      React2.createElement(
        "section",
        { className: "dim-jh-aggIntro" },
        React2.createElement("h2", { className: "dim-jh-aggTitle" }, "聚合"),
        React2.createElement(
          "p",
          { className: "dim-jh-aggIntroLine" },
          "把同一个模型在各渠道的条目聚合成一个名字，请求时按「积分最快作废」自动选渠道。"
        ),
        React2.createElement(
          "p",
          { className: "dim-jh-aggIntroLine" },
          "三层拒绝互不影响：关渠道（各渠道自己的面板）、关模型（模型选择器）、以及本页子列表里逐条拒绝某个候选参与轮换（只影响这一个模型）。"
        ),
        React2.createElement(
          "p",
          { className: "dim-jh-aggIntroWarn" },
          "本 provider 无需登录、没有账号池；积分与限流由各渠道自己管理。"
        ),
        React2.createElement(
          "p",
          { className: "dim-jh-aggIntroLine" },
          "未接入临期折算的渠道不参与轮换（它们仍可在模型选择器里直连使用）。"
        )
      ),
      // ── 工具行 ──
      React2.createElement(
        "div",
        { className: "dim-jh-aggToolbar" },
        React2.createElement(
          "span",
          { className: "dim-jh-aggCount" },
          models === null ? "读取中…" : `${models.length} 个聚合模型`
        ),
        React2.createElement("button", {
          type: "button",
          className: "dim-jh-aggRefresh",
          disabled: busy,
          // ⚠️ **显式刷新必须传 `true`**（绕过宿主侧 60 秒目录缓存）——
          //    否则按钮实际是「读缓存」，用户改了渠道模型后点刷新看不到变化。
          onClick: () => {
            void load(true);
          }
        }, busy ? "刷新中…" : "刷新"),
        // ⚠️ 「按临期排序」**只在用户点击时**才探测余额（每个已接入渠道一次上游 GET）。
        //    默认不探测：面板一打开就打十几次请求是不可接受的（规格 §8.3）。
        React2.createElement("button", {
          type: "button",
          className: "dim-jh-aggRefresh",
          disabled: expiryBusy,
          title: "逐个渠道查询积分到期时间并据此重排（会打上游请求，故默认不查）",
          onClick: () => {
            void onSortByExpiry();
          }
        }, expiryBusy ? "查询中…" : "按临期排序")
      ),
      notice !== null ? React2.createElement("div", { className: "dim-jh-aggNotice" }, notice) : null,
      // ── 空态 ──
      models !== null && models.length === 0 ? React2.createElement(
        "div",
        { className: "dim-jh-aggEmpty" },
        "当前没有可聚合的模型：请先在至少一个已接入临期折算的渠道（buddy / workbuddy / loomy / codearts / zcode / lobsterai / trae）登录账号。"
      ) : null,
      // ── 模型列表（**平铺**，全局按候选渠道数降序；不再有厂商分组标题）──
      // ⚠️ 用户 2026-10-07 明确要求去掉分组标题：渠道维度的信息在**展开后的子列表**里
      //    逐条显示，分组标题是冗余的（且分组标题里的「(2)/(5)」是**模型个数**，
      //    容易被误读成渠道个数）。
      sortedModels.map((model) => renderModel(model)),
      // ── 页脚说明 ──
      React2.createElement(
        "footer",
        { className: "dim-jh-aggFooter" },
        React2.createElement(
          "p",
          null,
          "为什么没有签到 / 重置 / 新建账号按钮：本 provider 不持有账号与积分。"
        ),
        React2.createElement(
          "p",
          null,
          "候选如何得出：字面推导（id / name 双通道）+ 显式映射补丁。"
        ),
        React2.createElement(
          "p",
          null,
          "如何核查：展开任一模型，逐条看渠道与真实 modelId；不同意就关掉那一行。"
        )
      )
    );
  }
  function JetHubPage({ close, rpcCall }) {
    const [selected, setSelected] = React2.useState(PROVIDERS[0].id);
    const [version, setVersion] = React2.useState(0);
    const [checkinBusy, setCheckinBusy] = React2.useState(false);
    const [checkinNotice, setCheckinNotice] = React2.useState(null);
    const [providerStatuses, setProviderStatuses] = React2.useState(null);
    const [providerStatusFailed, setProviderStatusFailed] = React2.useState(false);
    const [showProviderSwitches, setShowProviderSwitches] = React2.useState(false);
    const [providerBusy, setProviderBusy] = React2.useState(() => /* @__PURE__ */ new Set());
    const [providerOrder, setProviderOrder] = React2.useState(null);
    const [providerReordering, setProviderReordering] = React2.useState(false);
    const reorderLockRef = React2.useRef(false);
    const [providerNotice, setProviderNotice] = React2.useState(null);
    const [showGateway, setShowGateway] = React2.useState(false);
    const [gatewayStatus, setGatewayStatus] = React2.useState(null);
    const [gatewayBusy, setGatewayBusy] = React2.useState(false);
    const [gatewayNotice, setGatewayNotice] = React2.useState(null);
    const [showTokenLedger, setShowTokenLedger] = React2.useState(false);
    const mounted = React2.useRef(true);
    React2.useEffect(() => () => {
      mounted.current = false;
    }, []);
    const loadGatewayStatus = React2.useCallback(async () => {
      try {
        const res = await rpcCall("gateway.getEnabled", {});
        if (!mounted.current) return;
        setGatewayStatus(res);
      } catch (caught) {
        console.error("[jet-hub] read gateway status failed:", caught);
        if (!mounted.current) return;
        setGatewayStatus(null);
        setGatewayNotice({ tone: "error", text: "读取网关状态失败：" + (caught?.message || "未知错误") });
      }
    }, [rpcCall]);
    React2.useEffect(() => {
      void loadGatewayStatus();
    }, [loadGatewayStatus]);
    const toggleGateway = React2.useCallback(async (nextEnabled) => {
      setGatewayBusy(true);
      setGatewayNotice(null);
      try {
        const res = await rpcCall("gateway.setEnabled", { enabled: nextEnabled });
        if (!mounted.current) return;
        setGatewayStatus(res);
        setGatewayNotice({ tone: "ok", text: gatewayToggleNotice(res, nextEnabled) });
      } catch (caught) {
        console.error("[jet-hub] toggle gateway failed:", caught);
        if (!mounted.current) return;
        setGatewayNotice({ tone: "error", text: "操作失败：" + (caught?.message || "未知错误") });
        await loadGatewayStatus();
      } finally {
        if (mounted.current) setGatewayBusy(false);
      }
    }, [rpcCall, loadGatewayStatus]);
    const loadProviderStatuses = React2.useCallback(async () => {
      try {
        const res = await rpcCall("provider.status", { providers: PROVIDERS.map((p) => p.id) });
        if (!mounted.current) return;
        setProviderStatuses(res?.statuses || {});
        setProviderStatusFailed(false);
      } catch (caught) {
        console.error("[jet-hub] load provider statuses failed:", caught);
        if (!mounted.current) return;
        setProviderStatuses(null);
        setProviderStatusFailed(true);
        setProviderNotice({
          tone: "warn",
          text: `供应商开关状态读取失败（${caught?.message || "未知错误"}），已按原顺序显示供应商；账号管理不受影响。`
        });
      }
    }, [rpcCall]);
    React2.useEffect(() => {
      mounted.current = true;
      void loadProviderStatuses();
    }, [loadProviderStatuses]);
    const loadProviderOrder = React2.useCallback(async () => {
      try {
        const res = await rpcCall("provider.getOrder", {});
        if (!mounted.current) return;
        setProviderOrder(Array.isArray(res?.order) && res.order.length > 0 ? res.order : null);
      } catch (caught) {
        console.error("[jet-hub] load provider order failed:", caught);
        if (!mounted.current) return;
        setProviderOrder(null);
      }
    }, [rpcCall]);
    React2.useEffect(() => {
      void loadProviderOrder();
    }, [loadProviderOrder]);
    const providerTouchedRef = React2.useRef(false);
    React2.useEffect(() => {
      if (providerTouchedRef.current) return;
      if (!Array.isArray(providerOrder) || providerOrder.length === 0) return;
      const firstUsable = providerOrder.find((id) => providerStatuses === null || providerStatuses?.[id]?.closed !== true) ?? providerOrder[0];
      if (typeof firstUsable === "string" && firstUsable !== selected && PROVIDERS.some((p) => p.id === firstUsable)) {
        setSelected(firstUsable);
        setVersion((v) => v + 1);
      }
    }, [providerOrder, providerStatuses, selected]);
    const commitProviderOrder = React2.useCallback(async (next) => {
      if (!Array.isArray(next)) return;
      if (reorderLockRef.current) return;
      reorderLockRef.current = true;
      setProviderReordering(true);
      setProviderNotice((prev) => prev && prev.inModal ? null : prev);
      try {
        await rpcCall("provider.setOrder", { order: next });
        if (!mounted.current) return;
        setProviderOrder(next);
      } catch (caught) {
        console.error("[jet-hub] commit provider order failed:", caught);
        if (!mounted.current) return;
        setProviderNotice({
          tone: "error",
          text: "保存排序失败：" + (caught?.message || "未知错误") + "。界面顺序保持不变，可重试拖动。",
          // ⚠️ 必须由**弹窗内**呈现：拖拽必然发生在弹窗打开期间，而弹窗遮罩是
          // fixed + z-index 3000，页面层的 notice 会被它整个盖住（用户看不到失败）。
          inModal: true
        });
        await loadProviderOrder();
      } finally {
        reorderLockRef.current = false;
        if (mounted.current) setProviderReordering(false);
      }
    }, [rpcCall, loadProviderOrder]);
    const selectProvider = (id) => {
      providerTouchedRef.current = true;
      setSelected(id);
      setVersion((v) => v + 1);
    };
    const toggleProvider = async (providerId, enabled) => {
      const label = PROVIDERS.find((p) => p.id === providerId)?.label || providerId;
      const status = providerStatuses?.[providerId];
      if (!enabled) {
        const models = status?.models?.total ?? 0;
        const accounts = status?.accounts?.enabled ?? 0;
        const ok = confirm(
          `确认关闭「${label}」？

将关闭它的 ${models} 个模型（从对话框的模型选择里移除），并停用它的 ${accounts} 个启用账号。

取消则不做任何变更。`
        );
        if (!ok) return;
      }
      setProviderBusy((prev) => new Set(prev).add(providerId));
      setProviderNotice(null);
      try {
        const res = await rpcCall("provider.setEnabled", { provider: providerId, enabled });
        if (!mounted.current) return;
        setProviderNotice({
          tone: "ok",
          text: `${label}：${summarizeProviderToggle(enabled, res)}`
        });
        await loadProviderStatuses();
        if (mounted.current) setVersion((v) => v + 1);
      } catch (caught) {
        console.error("[jet-hub] toggle provider failed:", caught);
        if (!mounted.current) return;
        setProviderNotice({
          tone: "error",
          text: `${label} 操作失败：${caught?.message || "未知错误"}`
        });
      } finally {
        if (mounted.current) {
          setProviderBusy((prev) => {
            const next = new Set(prev);
            next.delete(providerId);
            return next;
          });
        }
      }
    };
    const checkinAll = async () => {
      setCheckinBusy(true);
      setCheckinNotice(null);
      const parts = [];
      const notes = [];
      const totalByUnit = { token: 0, credit: 0 };
      let failed = 0;
      for (const provider of checkinProviders()) {
        const label = PROVIDERS.find((p) => p.id === provider)?.label || provider;
        try {
          const res = await rpcCall("credits.claimAll", { provider });
          const s = res?.summary || {};
          const bits = [];
          if (s.claimed > 0) {
            const byUnit = s.totalByUnit || { credit: s.totalCredit, token: 0 };
            totalByUnit.credit += Number(byUnit.credit) || 0;
            totalByUnit.token += Number(byUnit.token) || 0;
            const amount = formatClaimGains(byUnit);
            if (amount !== null) bits.push(amount);
          }
          if (s.alreadyClaimed > 0) bits.push(`${s.alreadyClaimed} 个今日已领`);
          if (s.inactive > 0) bits.push(`${s.inactive} 个暂无活动`);
          if (s.failed > 0) {
            failed += s.failed;
            const reason = (res?.results || []).map((item) => item?.outcome?.message).find((msg) => typeof msg === "string" && msg.length > 0);
            bits.push(`${s.failed} 个失败${reason ? `（${reason}）` : ""}`);
          }
          parts.push(`${label} ${bits.length > 0 ? bits.join("，") : "无账号"}`);
          for (const item of res?.results || []) {
            const outcome = item?.outcome || {};
            if (outcome.actionRequired !== true) continue;
            const msg = outcome.message;
            if (typeof msg !== "string" || msg.length === 0) continue;
            if (!notes.includes(msg)) notes.push(msg);
          }
        } catch (caught) {
          failed += 1;
          parts.push(`${label} 失败（${caught?.message || "未知原因"}）`);
        }
        if (!mounted.current) return;
      }
      if (!mounted.current) return;
      const totalAmount = formatClaimGains(totalByUnit);
      setCheckinNotice({
        // 有待用户处理的提示时用 warn 色调，让那条提示更显眼
        tone: failed > 0 || notes.length > 0 ? "warn" : "ok",
        text: parts.length > 0 ? `一键签到：${parts.join("，")}${totalAmount === null ? "" : `（共 ${totalAmount}）`}` : "一键签到：没有可领取的渠道",
        notes
      });
      setCheckinBusy(false);
      setVersion((v) => v + 1);
    };
    const renderProviderRow = (p) => {
      const closed = providerStatuses?.[p.id]?.closed === true;
      const accountsStat = providerStatuses?.[p.id]?.accounts ?? null;
      const accountsTotal = accountsStat?.total ?? 0;
      const accountsEnabled = accountsStat?.enabled ?? 0;
      return React2.createElement(
        "div",
        { className: "dim-jh-providerRow", key: p.id, "data-provider": p.id },
        React2.createElement(
          "button",
          {
            type: "button",
            role: "tab",
            className: "dim-jh-provider",
            "aria-selected": p.id === selected,
            // 标题带上关闭状态：行本身已经没有开关，用户得知道去哪儿打开它。
            // ⚠️ 按钮名是「供应商」（页头），文案要与它一致，否则用户找不到。
            title: closed ? `${p.label}（已关闭，可在页头「供应商」按钮里打开）` : p.label,
            onClick: () => selectProvider(p.id)
          },
          React2.createElement(ProviderLogo, { provider: p.id }),
          // ⚠️ 这里给 label 加了 `dim-jh-providerLabel` —— 该类在样式表里**早已定义**
          // （含 min-width: 0 与省略号），但此前从未被任何 JS 使用，故真实界面上长
          // 供应商名一直在**折行**。加上它可把折行改为单行省略号（实测：rail 243px 时
          // 只有 WorkBuddy 一行超宽 21px），且列表总高不变（384px）；若不加，行高会
          // 从 48px 被顶到 58px、总高 424px。这是一处左侧的可见变化，已在交付说明中注明。
          React2.createElement(
            "span",
            { className: "dim-jh-providerLabel" },
            React2.createElement("strong", null, p.label)
          ),
          // 账号数小牌：跟在名字后面。⚠️ 判据是 **total > 0** 而不是「读到状态」——
          // provider.status 对没账号的渠道也回 total:0，按「读到就渲染」会挂出一排
          // 恒为 0 的牌（真机预览实锤），让「还没读到」与「一个账号都没有」无法区分。
          accountsStat && accountsTotal > 0 ? React2.createElement("span", {
            className: "dim-jh-providerCount",
            title: `已存 ${accountsTotal} 个账号（启用 ${accountsEnabled}）`
          }, String(accountsTotal)) : null
        )
      );
    };
    const renderRail = () => {
      if (providerStatuses === null) {
        return PROVIDERS.map((p) => renderProviderRow(p));
      }
      const { open, closed } = groupProviders(PROVIDERS, providerStatuses);
      const group = (title, list, key) => React2.createElement(
        "div",
        { className: "dim-jh-railGroup", key },
        React2.createElement("div", { className: "dim-jh-railGroupTitle" }, title),
        list.map((p) => renderProviderRow(p))
      );
      return [
        group(`已打开 (${open.length})`, sortOpenProvidersByOrder(open, providerOrder), "open"),
        group(`已关闭 (${closed.length})`, closed, "closed")
      ];
    };
    const providerSummary = providerToggleSummary(PROVIDERS, providerStatuses);
    return React2.createElement(
      "section",
      { className: "dim-jh-page", "aria-label": "Jet Hub Provider 设置" },
      // ⚠️ 页头左侧的「Jet Hub」标题块已**整块删除**（用户 2026-10-07 要求：
      // 标题只占宽度、不带任何操作，空间全给按钮行）。这里先后删过两样东西，
      // 都**不要**加回来：
      // ① 副标题「提供商凭据与多账号管理」（.dim-jh-brandDesc）—— 2026-10-06，
      //    因为按钮太多，副标题既显示不全又挤占页头高度；
      // ② 标题本体「Jet Hub」（.dim-jh-brand / .dim-jh-brandName）—— 2026-10-07，
      //    页头一排已有 7 个按钮，标题纯属占位。设置页的语义由本节点的
      //    aria-label 与宿主左侧「Jet Hub」导航项承担，不丢。
      React2.createElement(
        "header",
        { className: "dim-jh-header" },
        React2.createElement(
          "div",
          { className: "dim-jh-headerActions" },
          // 「供应商开关」在页头，而不是左侧每个供应商行尾（!25 的原形态）：
          // 它是破坏性批量操作，与「选择看哪个供应商」这个高频无害动作分开摆放，
          // 误点的可能性归零，也让左侧窄栏回到纯导航。见 `ProviderSwitchPanel`。
          // ⚠️ 按钮文字只写「供应商」（不是「供应商开关」）：页头四个按钮要排成
          // 一排，5 个字会把「关闭」挤到第二行 —— 完整语义由 tooltip 与弹窗标题承担。
          React2.createElement("button", {
            className: "dim-jh-btn",
            title: (providerSummary.known ? "供应商开关：逐个打开/关闭（已打开 " + providerSummary.open + "、已关闭 " + providerSummary.closed + "）。" : "供应商开关：逐个打开/关闭。") + "关闭一个供应商 = 关闭它的全部模型并停用它的全部账号。",
            "aria-haspopup": "dialog",
            "aria-expanded": showProviderSwitches ? "true" : "false",
            onClick: () => {
              setShowGateway(false);
              setShowProviderSwitches(true);
            }
          }, "供应商"),
          // 一键签到在备份/恢复**左侧**（需求指定位置）
          React2.createElement("button", {
            className: "dim-jh-btn",
            title: "依次签到全部支持签到的渠道（CodeBuddy / LobsterAI / CodeArts / Qoder / TRAE）。串行执行以避免触发风控。",
            disabled: checkinBusy,
            onClick: () => void checkinAll()
          }, checkinBusy ? "签到中…" : "一键签到"),
          React2.createElement(BackupPanel, {
            rpcCall,
            // 导入成功会整体替换账号，ProviderPanel 只在挂载时拉列表；
            // 递增版号强制重新挂载，让账号列表与模型目录立即反映新状态。
            onImported: () => setVersion((v) => v + 1)
          }),
          // 本机网关开关。⚠️ 文字刻意只写「网关」（见「供应商」按钮上方的同款
          // 注释）：页头按钮排成一行，长文字会把右端「关闭」挤到第二行。
          React2.createElement("button", {
            className: "dim-jh-btn",
            title: gatewayButtonTitle(gatewayStatus),
            "aria-haspopup": "dialog",
            "aria-expanded": showGateway ? "true" : "false",
            onClick: () => {
              setShowProviderSwitches(false);
              setShowGateway(true);
              setGatewayNotice(null);
              void loadGatewayStatus();
            }
          }, gatewayButtonLabel(gatewayStatus)),
          // 「Token 用量」入口：全 provider 的本地记账（直连/网关分开统计）。
          React2.createElement("button", {
            className: "dim-jh-btn",
            title: "Token 用量：按渠道（直连/网关）、供应商与模型统计本机请求的 token 消耗。",
            "aria-haspopup": "dialog",
            "aria-expanded": showTokenLedger ? "true" : "false",
            onClick: () => {
              setShowProviderSwitches(false);
              setShowGateway(false);
              setShowTokenLedger(true);
            }
          }, "Token 用量"),
          close ? React2.createElement("button", {
            className: "dim-jh-btn",
            onClick: close
          }, "关闭") : null
        )
      ),
      // 签到结果放在页头下方横跨整宽：页头是 flex 且不换行，塞进去会挤压按钮。
      // `flex: none` 是必需的 —— `dim-jh-page` 是 column flex 且 `dim-jh-layout`
      // 带 `flex: 1`，不锁住的话提示条会被压扁（与 modal 内同款做法）。
      checkinNotice ? React2.createElement(
        "div",
        {
          className: "dim-jh-probeNotice",
          "data-tone": checkinNotice.tone,
          role: checkinNotice.tone === "error" ? "alert" : "status",
          style: { flex: "none", margin: "12px 24px 0" }
        },
        React2.createElement("div", null, checkinNotice.text),
        // 需要用户操作的提示单列成列表（如「请先用 Qoder 官方客户端登录一次」）。
        // 复用既有的 `dim-jh-probeDetails` 样式，不引入新样式。
        (checkinNotice.notes || []).length > 0 ? React2.createElement(
          "ul",
          { className: "dim-jh-probeDetails" },
          checkinNotice.notes.map((note, index) => React2.createElement("li", { key: index }, note))
        ) : null
      ) : null,
      // ⚠️ `inModal` 的提示交给弹窗自己渲染（见 ProviderSwitchPanel）—— 重复渲染
      // 会让同一条错误在遮罩内外各出现一次。
      providerNotice && !providerNotice.inModal ? React2.createElement("div", {
        className: "dim-jh-probeNotice",
        "data-tone": providerNotice.tone,
        role: providerNotice.tone === "error" ? "alert" : "status",
        style: { flex: "none", margin: "12px 24px 0" }
      }, React2.createElement("div", null, providerNotice.text)) : null,
      React2.createElement(
        "div",
        { className: "dim-jh-layout" },
        React2.createElement(
          "nav",
          { className: "dim-jh-rail", role: "tablist", "aria-label": "Provider 导航" },
          renderRail()
        ),
        React2.createElement(
          "main",
          {
            className: "dim-jh-panel",
            role: "tabpanel"
          },
          PROVIDERS.map((p) => p.id === selected ? p.id === "aggregate" ? React2.createElement(AggregatePanel, { key: p.id + "-" + version, rpcCall }) : React2.createElement(ProviderPanel, {
            key: p.id + "-" + version,
            provider: p.id,
            rpcCall
          }) : null)
        )
      ),
      // 「供应商开关」以 modal 渲染：它是覆盖层，放在布局之后只是组件树的书写顺序
      // （与账号面板里的模型列表同款做法）。关闭即不挂载，避免常驻一份开关列表。
      showProviderSwitches ? React2.createElement(ProviderSwitchPanel, {
        providers: PROVIDERS,
        statuses: providerStatuses,
        statusFailed: providerStatusFailed,
        busyIds: providerBusy,
        onToggle: toggleProvider,
        onReload: loadProviderStatuses,
        onClose: () => setShowProviderSwitches(false),
        order: providerOrder,
        onCommitOrder: commitProviderOrder,
        reordering: providerReordering,
        notice: providerNotice
      }) : null,
      // 同款做法：网关开关也是覆盖层，且与「供应商开关」互斥 —— 两者都是
      // `position: fixed` 的全屏弹窗，同时打开会叠在一起、ESC 只关掉后挂载的那个。
      showGateway ? React2.createElement(GatewayPanel, {
        status: gatewayStatus,
        busy: gatewayBusy,
        notice: gatewayNotice,
        onToggle: toggleGateway,
        onReload: loadGatewayStatus,
        onClose: () => setShowGateway(false)
      }) : null,
      // 「Token 用量」弹窗：同样是覆盖层（createElement 形式 —— 与其它弹窗
      // 同一理由，见 OpencodeProxyModal 上方的 hook 计数事故注释）。
      showTokenLedger ? React2.createElement(TokenLedgerPanel, {
        rpcCall,
        onClose: () => setShowTokenLedger(false)
      }) : null
    );
  }

  // ../dsh-codearts-src/plugin-src/client/jet-hub-styles.js
  var STYLES = `
.dim-jh-page { display: flex; flex-direction: column; height: 100%; }
.dim-jh-header { display: flex; align-items: center; justify-content: space-between; gap: 12px; padding: 16px 24px; border-bottom: 1px solid var(--dsw-alias-border-default, #e5e5e5); }
/* ⚠️ 页头左侧的标题块**已整块删除**（2026-10-07）：
   ① 「Jet Hub」标题本体（.dim-jh-brand / .dim-jh-brandName）—— 页头一排已有
      7 个按钮，标题只占宽度、不带任何操作。CSS 一并删除（留在样式表里的
      「无引用规则」没有任何价值，只会在下次重构时被误当成生效样式）。
   ② 副标题「提供商凭据与多账号管理」（.dim-jh-brandDesc）—— 2026-10-06，
      按钮太多，副标题既显示不全又挤占页头高度。
   ⚠️ **两者都不要加回来**：设置页语义由 .dim-jh-page 的 aria-label
   「Jet Hub Provider 设置」与宿主左侧「Jet Hub」导航项承担，不丢。 */

/* 布局：对齐 dsh-im 的两栏 */
.dim-jh-layout { display: flex; flex: 1; overflow: hidden; }

/* 左侧导航：align dsh-im .dim-rail */
/* ⚠️ 宽度 228px 是「尽量给右侧让位」与「最长行不出省略号」的交点。算式（含滚动条）：
     可用文字宽 = W − 12(rail padding 6×2) − 2(按钮边框) − 20(按钮 padding 10×2)
                     − 30(图标) − 8(图标间距) − 15(Windows 经典滚动条) = W − 85
     W = 228 ⇒ 可用 **143px**，刚好装得下最长行 WorkBuddy (国际版) —— 它实测要 141px
     （!25 的实测表：200px + 行尾开关时标签只剩 77px、该行超宽 64px ⇒ 77 + 64 = 141）。
   与 !25 的 243px 相比省出 15px，再加上开关移走后消失的那条 8px 空列
   ⇒ 右侧账号区净得约 23px。**再往回收就会截断最长行**（用户已报过一次
   「workbuddy国际版有省略号」），要更窄只能改短 label —— 但那会与
   RaccoonProduct / QODER_CN 等 displayName 的跨文件一致性断言冲突，故未做。
   ⚠️ 测量本身的三条坑（inline 元素 clientWidth 恒 0 会得到假阴性、不能靠行高判折行、
   滚动条吃掉约 15px）见工作区 jet-hub-provider-toggle-notes.md。 */
.dim-jh-rail { width: 228px; border-right: 1px solid var(--dsw-alias-border-default, #e5e5e5); padding: 6px; overflow-y: auto; display: grid; align-content: start; gap: 8px; }

/* 每个 provider 按钮：align dsh-im .dim-channel */
/* padding 与图标间距比 !25 各收窄 2px（12→10、10→8）：这两处是**纯开销**，
   省下的每一像素都直接变成标签可用宽度，比加宽 rail 划算 —— 正是靠这 4px
   才把「不截断」所需的 rail 宽度从 232px 压到 228px。 */
.dim-jh-provider { width: 100%; min-height: 48px; display: grid; grid-template-columns: 30px minmax(0, 1fr); align-items: center; gap: 8px; padding: 8px 10px; border: 1px solid var(--dsw-alias-border-l2, #eef0f3); border-radius: 14px; color: inherit; background: var(--dsw-alias-bg-layer-3, #fff); box-shadow: 0 2px 8px rgb(31 35 41 / 3%); font: inherit; text-align: left; cursor: pointer; transition: border-color .16s ease, background .16s ease, box-shadow .16s ease; }
.dim-jh-provider:hover { border-color: color-mix(in srgb, #1677ff 25%, var(--dsw-alias-border-l2, #eef0f3)); background: color-mix(in srgb, #1677ff 2%, var(--dsw-alias-bg-layer-3, #fff)); box-shadow: 0 5px 16px rgb(31 35 41 / 5%); }
.dim-jh-provider[aria-selected="true"] { border-color: color-mix(in srgb, #1677ff 43%, var(--dsw-alias-border-l2, #dfe1e5)); color: #1677ff; background: color-mix(in srgb, #1677ff 12%, var(--dsw-alias-bg-layer-3, #fff)); box-shadow: 0 3px 12px rgb(51 112 255 / 7%); }
.dim-jh-provider:focus-visible { outline: none; border-color: color-mix(in srgb, #1677ff 72%, var(--dsw-alias-border-l2, #dfe1e5)); box-shadow: 0 0 0 1px color-mix(in srgb, #1677ff 24%, transparent) inset, 0 3px 12px rgb(51 112 255 / 7%); }

/* 图标容器：align dsh-im .dim-logo */
.dim-jh-providerIcon { width: 30px; height: 30px; display: grid; place-items: center; border-radius: 9px; box-shadow: 0 1px 3px rgb(31 35 41 / 7%); overflow: hidden; }
.dim-jh-providerIcon img { display: block; width: 20px; height: 20px; border-radius: 2px; }
.dim-jh-providerIcon.codearts { background: white; }
.dim-jh-providerIcon.buddy { background: white; }
.dim-jh-providerIcon.workbuddy { background: white; }
.dim-jh-providerIcon.lobsterai { background: white; }
.dim-jh-providerIcon.qoder { background: white; }
/* Qoder 中国版：官方 ICO 缩图，白底容器中对比度足够。 */
.dim-jh-providerIcon.qodercn { background: white; }
.dim-jh-providerIcon.trae { background: white; }
/* Raccoon Work（商汤）：官方图标是深蓝底白色面具，白底容器中显示清晰。 */
.dim-jh-providerIcon.raccoon { background: white; }
/* MiniMax Code（中国版）：官方 logo **自带浅蓝底** #7DC6FF，白底容器中显示清晰。
   ⚠️ 本文件的样式整体是一个模板字符串 —— 注释里**不能出现反引号**（会提前终止）。 */
.dim-jh-providerIcon.minimax { background: white; }
/*
 * ZCode（智谱）：图标自带深色圆角底 + 青色 Z，本身即完整图形，
 * 故容器保持透明（加白底反而会出现一圈突兀的方块）。
 */
.dim-jh-providerIcon.zcode { background: transparent; }
/* AutoClaw（智谱系渠道）：内联 SVG（蓝紫渐变圆角底 + 白色闪电），白底容器中显示清晰。
   ⚠️ 本文件的样式整体是一个模板字符串 —— 注释里**不能出现反引号**（会提前终止）。 */
.dim-jh-providerIcon.autoclaw { background: white; }
/* Gemini Code Assist：内联 SVG 自带蓝紫渐变星形，白底容器中显示清晰。
   ⚠️ 本文件的样式整体是一个模板字符串 —— 注释里**不能出现反引号**（会提前终止）。 */
.dim-jh-providerIcon.gemini { background: white; }
/* 聚合 provider：内联 SVG（三渠道汇聚），白底容器中显示清晰。
   ⚠️ 加这一条是必要的 —— 上面每家的 logoClass 都各自声明了背景，
      没有兜底规则，缺了会让聚合的图标容器**没有背景**（与其它行不一致）。 */
.dim-jh-providerIcon.aggregate { background: white; }

/* provider 文案：align dsh-im .dim-channelCopy */
.dim-jh-providerLabel { min-width: 0; display: grid; }
.dim-jh-providerLabel strong { overflow: hidden; color: inherit; font-size: 14px; line-height: 20px; font-weight: 680; text-overflow: ellipsis; white-space: nowrap; }
/* 账号数小牌：供应商行名字后面的计数徽标（已存账号数）。
   chip 按钮是纵向 flex，小牌自然落在短名下方，无需额外断点适配。 */
.dim-jh-providerCount { min-width: 16px; height: 16px; padding: 0 4px; border-radius: 8px; background: var(--dsw-alias-bg-layer-3, #f0f1f3); color: #57606a; font-size: 10px; line-height: 16px; text-align: center; }

/* ── 供应商级一键开关（左侧 rail 的分组 + 行尾开关）── */
/* 分组：与 rail 同为 grid，组之间留出间隔。分组只是展示分组，不改变声明顺序。 */
.dim-jh-railGroup { display: grid; gap: 8px; }
.dim-jh-railGroup + .dim-jh-railGroup { margin-top: 10px; }
.dim-jh-railGroupTitle { padding: 2px 4px 0; font-size: 12px; line-height: 16px; font-weight: 600; color: var(--dsw-alias-label-tertiary, #8f959e); }
/* 每行：只有一个选择按钮（开关已搬到页头的「供应商开关」弹窗）。
   ⚠️ 列宽仍写 minmax(0, 1fr) 而不是 1fr：grid 项的 min-width 默认 auto，
   长供应商名会把行撑宽、撑出 rail（与模型行那次「开关不可见」的缺陷同型）。
   ⚠️ **不要再留第二列**：!25 时代这里是 minmax(0, 1fr) auto 给行尾开关用；
   开关移走后那一列虽为 0 宽，**8px 的列间距却照样计入**，于是在卡片右侧
   留下一条看着像「rail 没铺满」的空白。用户报障原话：
   「去掉开关后右边有片空白，要省略让右边的账号池区域显示更宽」。 */
.dim-jh-providerRow { display: grid; grid-template-columns: minmax(0, 1fr); }
.dim-jh-providerRow .dim-jh-provider { min-width: 0; }

/* 右侧面板 */
.dim-jh-panel { flex: 1; padding: 24px; overflow-y: auto; }
.dim-jh-empty { text-align: center; padding: 40px; color: var(--dsw-alias-label-tertiary, #888); }
.dim-jh-empty p { margin: 8px 0; font-size: 14px; }

/* 账号卡片 */
.dim-jh-accountCard { position: relative; border: 1px solid var(--dsw-alias-border-l2, #eef0f3); border-radius: 14px; padding: 14px 16px; margin-bottom: 10px; background: var(--dsw-alias-bg-layer-3, #fff); box-shadow: 0 2px 8px rgb(31 35 41 / 3%); transition: border-color .16s ease, box-shadow .16s ease, opacity .16s ease; }
.dim-jh-accountCard:hover { border-color: color-mix(in srgb, #1677ff 22%, var(--dsw-alias-border-l2, #eef0f3)); box-shadow: 0 5px 16px rgb(31 35 41 / 5%); }
.dim-jh-accountCard[data-enabled="false"] { opacity: 0.62; }

/* 拖拽排序 */
/* 抓取柄：独立的小区域，避免与卡片内的按钮/文本选择冲突 */
.dim-jh-dragHandle { flex: none; width: 16px; height: 20px; display: flex; align-items: center; justify-content: center; cursor: grab; color: var(--dsw-alias-label-tertiary, #9aa0a6); font-size: 12px; line-height: 1; letter-spacing: -1px; user-select: none; border-radius: 4px; }
.dim-jh-dragHandle:hover { color: var(--dsw-alias-label-secondary, #5f6672); background: rgb(31 35 41 / 5%); }
.dim-jh-dragHandle:active { cursor: grabbing; }
/* 正在被拖动的卡片：淡出以表明它已"拿起" */
.dim-jh-accountCard[data-dragging="true"] { opacity: 0.4; border-style: dashed; }
/* 拖拽悬停的目标位置：插入线。上方=插到该卡片之前，下方=之后。 */
.dim-jh-accountCard[data-dropBefore="true"]::before { content: ''; position: absolute; left: 0; right: 0; top: -6px; height: 3px; border-radius: 2px; background: #1677ff; }
.dim-jh-accountCard[data-dropAfter="true"]::after { content: ''; position: absolute; left: 0; right: 0; bottom: -6px; height: 3px; border-radius: 2px; background: #1677ff; }
/* 序号徽标：让当前优先级一目了然（顺序即自动选号优先级） */
.dim-jh-accountOrder { flex: none; min-width: 18px; padding: 0 5px; border-radius: 6px; font-size: 11px; line-height: 17px; font-weight: 600; text-align: center; color: var(--dsw-alias-label-secondary, #5f6672); background: rgb(31 35 41 / 6%); }
.dim-jh-orderHint { margin: 0 0 10px; font-size: 12px; line-height: 18px; color: var(--dsw-alias-label-tertiary, #8f959e); }

/* 顶部一行：状态点 + 名称 + 状态标签 */
.dim-jh-accountTop { display: flex; align-items: center; gap: 8px; }
.dim-jh-accountStatus { flex: none; width: 8px; height: 8px; border-radius: 50%; background: var(--dsw-alias-label-tertiary, #9aa0a6); }
.dim-jh-accountStatus[data-on="true"] { background: #22c55e; box-shadow: 0 0 0 3px rgb(34 197 94 / 14%); }
.dim-jh-accountName { flex: 1 1 auto; min-width: 0; overflow: hidden; font-size: 14px; line-height: 20px; font-weight: 600; color: var(--dsw-alias-label-primary, #1f2329); text-overflow: ellipsis; white-space: nowrap; }
.dim-jh-accountTag { flex: none; padding: 1px 8px; border-radius: 999px; font-size: 11px; line-height: 17px; font-weight: 500; }
.dim-jh-accountTag[data-tone="on"] { color: #15803d; background: rgb(34 197 94 / 12%); }
.dim-jh-accountTag[data-tone="off"] { color: var(--dsw-alias-label-tertiary, #8f959e); background: rgb(143 149 158 / 12%); }

/* 元信息：键值对齐的网格 */
.dim-jh-accountMeta { display: grid; gap: 3px; margin: 8px 0 0; }
.dim-jh-metaRow { display: grid; grid-template-columns: 52px minmax(0, 1fr); align-items: baseline; gap: 8px; }
.dim-jh-metaRow dt { font-size: 12px; line-height: 18px; color: var(--dsw-alias-label-tertiary, #8f959e); }
.dim-jh-metaRow dd { min-width: 0; margin: 0; overflow: hidden; font-size: 12px; line-height: 18px; color: var(--dsw-alias-label-secondary, #646a73); text-overflow: ellipsis; white-space: nowrap; }
.dim-jh-metaRow dd[data-tone="warn"] { color: #e37400; }
/* 积分未取到时的弱化提示。与 warn 区分：这不是异常，只是还没有数据 */
.dim-jh-metaRow dd[data-tone="muted"] { color: var(--dsw-alias-label-tertiary, #8f959e); }
.dim-jh-metaRow code { padding: 1px 5px; border-radius: 5px; background: var(--dsw-alias-bg-layer-2, #f4f5f7); font-family: ui-monospace, SFMono-Regular, Menlo, monospace; font-size: 11px; }

/* 账号卡片上的积分余额。
   覆盖 metaRow 的 overflow:hidden / nowrap —— 这里要的是横向排列的
   数值 + 次要说明，而 dd 默认样式是为单行截断文本准备的。 */
.dim-jh-metaRow dd.dim-jh-creditValue { display: flex; flex-direction: row; align-items: baseline; gap: 6px; overflow: visible; }
.dim-jh-creditTotal { font-size: 13px; font-weight: 600; color: #1677ff; font-variant-numeric: tabular-nums; }
.dim-jh-creditPackages { font-size: 11px; color: var(--dsw-alias-label-tertiary, #8f959e); }
/* 已失效额度：弱化的橙色提示，与主数值的蓝色明确区分 */
.dim-jh-creditExpired { font-size: 11px; color: #b45309; }

/* 限额重置徽章行 */
.dim-jh-rateLimits { display: flex; flex-wrap: wrap; align-items: center; gap: 6px; margin-top: 8px; }
.dim-jh-rateLimitsLabel { font-size: 12px; line-height: 18px; color: var(--dsw-alias-label-tertiary, #8f959e); }

/* 操作按钮：横向一行，右对齐 */
.dim-jh-accountActions { display: flex; flex-direction: row; flex-wrap: nowrap; justify-content: flex-end; gap: 8px; margin-top: 12px; padding-top: 10px; border-top: 1px solid var(--dsw-alias-border-l2, #f0f1f3); }
/* 快捷行（签到 / 续期，UTTS app 的 Jet 桥能力）：仿 accountActions 但左对齐、
   无上边线、margin 更小 —— 它挂在 rateLimits 与 accountActions 之间，是高频操作，
   视觉上要轻于底部的低频按钮行。 */
.dim-jh-quickActions { display: flex; flex-direction: row; gap: 8px; margin-top: 10px; }

/* 按钮：align dsh-im .dim-deliveryButton */
.dim-jh-btn { font-size: 12px; line-height: 18px; padding: 4px 12px; border: 1px solid var(--dsw-alias-border-l2, #dfe1e5); border-radius: 8px; background: var(--dsw-alias-bg-layer-3, #fff); cursor: pointer; color: var(--dsw-alias-label-primary, #1f2329); white-space: nowrap; transition: border-color .15s ease, background .15s ease, color .15s ease; }
.dim-jh-btn:hover:not(:disabled) { border-color: color-mix(in srgb, #1677ff 40%, var(--dsw-alias-border-l2, #dfe1e5)); color: #1677ff; background: color-mix(in srgb, #1677ff 6%, var(--dsw-alias-bg-layer-3, #fff)); }
.dim-jh-btn[data-kind="primary"] { background: #1677ff; color: #fff; border-color: #1677ff; }
.dim-jh-btn[data-kind="primary"]:hover:not(:disabled) { background: #0f5fce; border-color: #0f5fce; color: #fff; }
.dim-jh-btn[data-kind="danger"] { color: #d93025; border-color: color-mix(in srgb, #d93025 35%, var(--dsw-alias-border-l2, #dfe1e5)); }
.dim-jh-btn[data-kind="danger"]:hover:not(:disabled) { color: #b3261e; border-color: #d93025; background: rgb(217 48 37 / 6%); }
.dim-jh-btn:disabled { opacity: 0.5; cursor: default; }


/* 纯图标按钮（如「领取新手任务」的礼物图标）。
   ⚠️ 存在的理由：.dim-jh-accountActions 是 flex-wrap: nowrap，
   行内已有 5 个文字按钮，再加一个「领取新手任务」会被挤出容器（用户报障）。
   故把它压成等宽等高的方形图标按钮，文案移到 title tooltip。
   正方形靠固定 padding（左右 = 上下）实现，不依赖内容宽度。
   ⚠️ 本文件整体是一个 JS 模板字符串，注释里**不能出现反引号** —— 会提前
   终止字符串（本次构建失败的成因）。 */
.dim-jh-iconBtn { display: inline-flex; align-items: center; justify-content: center; padding: 4px 8px; min-width: 26px; }
.dim-jh-iconBtn svg { display: block; }

/* 限流 TTL 徽章 */
.dim-jh-ttlBadge { display: inline-block; padding: 1px 8px; border-radius: 999px; background: rgb(227 116 0 / 10%); color: #b45309; font-size: 11px; line-height: 17px; font-weight: 500; }

/* 面板标题区：标题独占一行，操作按钮另起一行。
   此前用单行 space-between 把标题与 5 个按钮挤在一起，面板一窄就溢出被裁掉。 */
.dim-jh-panelHead { display: flex; flex-direction: column; align-items: flex-start; gap: 10px; margin-bottom: 16px; }
.dim-jh-panelTitle { margin: 0; font-size: 16px; font-weight: 600; color: var(--dsw-alias-label-primary, #1f2329); }

/* 面板标题下方的操作按钮组（显示列表 / 刷新积分 / 一键领取积分 / 重测所有 / 重置所有 / 新建账号）。
   允许换行：按钮数量随 provider 变化（CodeBuddy 有「一键领取积分」，其他没有），
   固定单行在窄面板下必然放不下。 */
/* ⚠️ 本文件 CSS 装在**模板字符串**里 —— 注释内绝不能出现反引号，否则提前
   闭合、esbuild 报 "Expected ; but found ..." 并把指针指到注释行（2026-10-07
   在同一天内踩了两次，故把这条写进最常改的页头段落）。
   ⚠️ 页头左侧标题块（.dim-jh-brand / .dim-jh-brandName）已删除（2026-10-07），
   故按钮组改为**独占整行**（flex: 1 而非 none）：原先 space-between 靠左侧
   标题把按钮推到右边，标题一删按钮会贴左，改用 flex: 1 + 左对齐保持原观感。
   ⚠️ 仍**保留** flex-wrap: wrap：窗口极窄时让按钮换行远好过溢出到窗口外点不到。 */
.dim-jh-headerActions { display: flex; flex-wrap: wrap; align-items: center; gap: 8px; flex: 1 1 auto; min-width: 0; }

/* 上一次「重测 / 重置」的结果提示 */
.dim-jh-probeNotice { margin-bottom: 12px; padding: 10px 12px; border-radius: 10px; border: 1px solid var(--dsw-alias-border-l2, #eef0f3); background: var(--dsw-alias-bg-layer-2, #f7f8fa); font-size: 12px; line-height: 18px; color: var(--dsw-alias-label-secondary, #646a73); }
.dim-jh-probeNotice[data-tone="ok"] { border-color: color-mix(in srgb, #22c55e 35%, var(--dsw-alias-border-l2, #eef0f3)); background: rgb(34 197 94 / 8%); color: #15803d; }
.dim-jh-probeNotice[data-tone="warn"] { border-color: color-mix(in srgb, #e37400 35%, var(--dsw-alias-border-l2, #eef0f3)); background: rgb(227 116 0 / 8%); color: #b45309; }
.dim-jh-probeNotice[data-tone="error"] { border-color: color-mix(in srgb, #d93025 35%, var(--dsw-alias-border-l2, #eef0f3)); background: rgb(217 48 37 / 8%); color: #b3261e; }
.dim-jh-probeDetails { margin: 6px 0 0; padding-left: 18px; display: grid; gap: 2px; }
.dim-jh-probeDetails li { font-size: 12px; line-height: 18px; }
/* 授权链接 + 复制按钮同行。链接不设 min-width: 0 时，300+ 字符的 URL 会把按钮顶出可视区。 */
.dim-jh-loginLinkRow { display: flex; flex-wrap: wrap; align-items: center; gap: 8px; margin-top: 6px; }
.dim-jh-loginLink { flex: 1 1 240px; min-width: 0; color: var(--dsw-alias-link); word-break: break-all; }
/* 复制失败时把链接变成一次点击全选，用户能直接 Ctrl+C。 */
.dim-jh-loginLinkRow[data-copy-state="failed"] .dim-jh-loginLink { user-select: all; }

/* 登录弹窗 */
.dim-jh-loginOverlay { position: fixed; inset: 0; background: rgba(0,0,0,0.3); display: flex; align-items: center; justify-content: center; z-index: 1000; }
.dim-jh-loginDialog { background: var(--dsw-alias-bg-layer-1, #fff); border-radius: 12px; padding: 24px; min-width: 320px; box-shadow: 0 8px 32px rgba(0,0,0,0.15); }
.dim-jh-loginDialog h3 { margin: 0 0 8px; font-size: 16px; }
.dim-jh-loginDialog p { font-size: 13px; color: var(--dsw-alias-label-secondary, #555); margin: 0 0 16px; }
.dim-jh-loginActions { display: flex; gap: 8px; justify-content: flex-end; }

/* ── 模型列表弹窗（「显示列表」） ── */
/* 复用登录弹窗的遮罩模式：fixed 覆盖全屏，z-index 高于设置页内容。
   3000 高于 .dim-jh-loginOverlay 的 1000，保证两个弹窗同时存在时模型列表在上。 */
.dim-jh-modalOverlay { position: fixed; inset: 0; z-index: 3000; display: flex; align-items: center; justify-content: center; padding: 24px; background: rgba(0,0,0,0.32); }
/* 模型列表弹窗：**顶部锚定**而非垂直居中。
   ⚠️ 这是修真实缺陷（用户报障「输入文字后整个弹框的位置会发生改变，有点突兀」）：
   弹窗高度随列表长度变化，而 align-items: center 会把高度变化直接变成**整体
   位置跳动** —— 实测输入搜索词后 top 从 4px 跳到 187px（结果变少 → 弹窗变矮 →
   居中的位置跟着上移）。顶部锚定后上边缘固定，只在下方伸缩，视觉上稳定。
   只作用于模型列表，不影响账号备份弹窗。
   ⚠️ 本文件整体是 JS 模板字符串，注释里**不能出现反引号**（会提前终止字符串）。 */
.dim-jh-modalOverlay--top { align-items: flex-start; padding-top: max(24px, 8vh); }
/* 顶锚后可用高度由 padding 决定，故 max-height 按 padding box 计算（100%），
   不再用 100vh - 48px 这类视口算式 —— 否则 8vh 大于 24px 时会溢出视口。 */
.dim-jh-modalOverlay--top .dim-jh-modal { max-height: 100%; }
.dim-jh-modal { display: flex; flex-direction: column; width: min(560px, 100%); max-height: min(640px, calc(100vh - 48px)); padding: 20px 22px; border-radius: 14px; background: var(--dsw-alias-bg-layer-1, #fff); box-shadow: 0 16px 48px rgba(0,0,0,0.22); }
.dim-jh-modalHead { flex: none; display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.dim-jh-modalTitle { min-width: 0; display: flex; align-items: baseline; flex-wrap: wrap; gap: 8px; font-size: 15px; line-height: 22px; font-weight: 600; color: var(--dsw-alias-label-primary, #1f2329); }
.dim-jh-modalSubtitle { overflow: hidden; font-size: 12px; line-height: 18px; font-weight: 400; color: var(--dsw-alias-label-tertiary, #8f959e); text-overflow: ellipsis; white-space: nowrap; }
/* 头部右侧按钮组与标题里的计数徽标 */
.dim-jh-modelPanelActions { flex: none; display: flex; align-items: center; gap: 8px; }
.dim-jh-modelPanelCount { font-size: 12px; line-height: 18px; font-weight: 400; color: var(--dsw-alias-label-tertiary, #8f959e); }
.dim-jh-modalHint { flex: none; margin: 10px 0 0; font-size: 12px; line-height: 18px; color: var(--dsw-alias-label-tertiary, #8f959e); }
/* 弹窗提示里的强调词：danger=危险操作（覆盖/不可撤销），warn=警示（妥善保管） */
.dim-jh-emph-danger { color: #b3261e; font-weight: 600; }
.dim-jh-emph-warn { color: #b45309; font-weight: 600; }
.dim-jh-modal .dim-jh-probeNotice { flex: none; margin: 10px 0 0; }
/* 批量工具条（打开全部 / 关闭全部）：固定不滚动，紧跟在说明文字下方 */
.dim-jh-modelBulkBar { flex: none; display: flex; align-items: center; flex-wrap: wrap; gap: 8px; margin-top: 10px; }
/* 搜索 + 状态筛选条（Cline 目录近 500 条，没有它就只能一页页翻）。
   允许换行：窄面板下搜索框与三个状态按钮放不进一行。 */
.dim-jh-modelFilterBar { flex: none; display: flex; align-items: center; flex-wrap: wrap; gap: 8px; margin-top: 10px; }
/* 搜索框占据剩余宽度，最小 140px —— 再窄就输不下有意义的模型名片段。 */
.dim-jh-modelSearch { flex: 1 1 140px; min-width: 140px; width: auto; }
.dim-jh-modelStatusFilter { flex: none; display: flex; align-items: center; gap: 6px; }
/* 选中的筛选按钮高亮：三个按钮外观一致时用户看不出当前筛的是什么。 */
.dim-jh-modelStatusFilter .dim-jh-btn[data-active="true"] { border-color: #1677ff; color: #1677ff; background: color-mix(in srgb, #1677ff 10%, var(--dsw-alias-bg-layer-3, #fff)); font-weight: 600; }
/* 列表区独立滚动：头部与说明固定，模型多时只滚中间 */
/* ⚠️ overflow-x: hidden 是**兜底**，不是主修复（主修复见下方 grid 的 minmax）。
   没有它时，任何一行的偶然溢出都会让整个弹窗出现横向滚动条，而横向滚动条会把
   每一行的**开关**一起推出可视区 —— 用户报障「开关在最右边，要横向滑动才看得到」。
   ⚠️ 本文件整体是 JS 模板字符串，注释里**不能出现反引号**（会提前终止字符串，
   本次就因此构建失败过一次）—— 说明 CSS 属性时一律不加反引号。 */
.dim-jh-modalBody { flex: 1 1 auto; min-height: 0; margin-top: 10px; overflow-y: auto; overflow-x: hidden; }
.dim-jh-modalBody .dim-jh-empty { padding: 24px; }

/* 每行一个模型：左侧名称 + id，右侧开关 */
/* ⚠️ grid-template-columns: minmax(0, 1fr) 是**必须的**，不能省。
   单列 grid 的列宽默认是 auto，而 grid 项的 min-width 默认也是 auto ——
   两者叠加会让列宽按**最宽内容**撑开，于是长 id 把行推宽、行末的开关被挤出
   弹窗右边缘（真实缺陷：Cline 有 300 个 id 超过 20 字符，几乎每行都中招，
   表现为「开关在最后，需要横向滑动，我看不到」）。
   minmax(0, 1fr) 把列的最小宽度显式压到 0，行才会跟着容器收缩。 */
.dim-jh-modelList { display: grid; grid-template-columns: minmax(0, 1fr); gap: 2px; }
/* 行本身是 grid 项也是 flex 容器，两处都需要 min-width: 0 才允许收缩。 */
.dim-jh-modelRow { display: flex; align-items: center; gap: 12px; min-width: 0; padding: 7px 8px; border-radius: 8px; cursor: pointer; transition: background .15s ease; }
.dim-jh-modelRow:hover { background: var(--dsw-alias-bg-layer-2, #f7f8fa); }
/* 已关闭的模型整体降透明度：一眼能看出哪些被隐藏了 */
.dim-jh-modelRow[data-disabled="true"] .dim-jh-modelInfo { opacity: 0.5; }

/* 「供应商开关」弹窗的行拖拽（2026-10-06）。
   视觉语言与账号卡片（.dim-jh-accountCard[data-dragging] 系）完全一致：
   源行淡出虚线、落点行画插入线。
   ⚠️ 这几行**必须带 .dim-jh-modal 作用域前缀**：.dim-jh-modelRow 是共享类名，
   ModelToggle（模型列表弹窗）与 GatewayPanel 也在用它们。那些行不设
   data-dropBefore/After，伪元素无 content 故不渲染，但给它们挂 position:relative
   仍是作用域过宽 —— 将来任一处加 absolute 子元素都会莫名受影响。
   与既有的 .dim-jh-modal .dim-jh-probeNotice 同一收窄惯例。
   ⚠️⚠️ 本文件整体是一个 JS **模板字符串**：注释里**禁止**出现反引号，
   否则会提前终止字符串、把后面的 CSS 当成代码解析（实测报 TS2304）。 */
.dim-jh-modal .dim-jh-modelRow { position: relative; }
.dim-jh-modal .dim-jh-modelRow[data-dragging="true"] { opacity: 0.4; border-style: dashed; }
.dim-jh-modal .dim-jh-modelRow[data-dropBefore="true"]::before { content: ''; position: absolute; left: 0; right: 0; top: -5px; height: 3px; border-radius: 2px; background: #1677ff; }
.dim-jh-modal .dim-jh-modelRow[data-dropAfter="true"]::after { content: ''; position: absolute; left: 0; right: 0; bottom: -5px; height: 3px; border-radius: 2px; background: #1677ff; }
/* 拖拽柄在弹窗行里略收窄：行高比账号卡片矮（padding 7px vs 14px），
   16px 宽的柄会顶着模型名，缩到 14px 并把 gap 让出来。 */
.dim-jh-modal .dim-jh-modelRow .dim-jh-dragHandle { width: 14px; }
.dim-jh-modelInfo { flex: 1 1 auto; min-width: 0; display: flex; align-items: baseline; gap: 8px; }
/* 名称与 id 都必须能收缩（min-width: 0 + 可收缩的 flex-basis），否则长内容会
   顶宽整行。展示名优先保留，故 id 另加 max-width 上限。
   ⚠️ id 早期是 flex: none（拒绝收缩）—— 那正是「开关被挤出可视区」最直接的成因。 */
.dim-jh-modelName { flex: 0 1 auto; min-width: 0; overflow: hidden; font-size: 13px; line-height: 19px; font-weight: 500; color: var(--dsw-alias-label-primary, #1f2329); text-overflow: ellipsis; white-space: nowrap; }
.dim-jh-modelId { flex: 0 1 auto; min-width: 0; max-width: 46%; overflow: hidden; padding: 1px 5px; border-radius: 5px; background: var(--dsw-alias-bg-layer-2, #f4f5f7); font-family: ui-monospace, SFMono-Regular, Menlo, monospace; font-size: 11px; color: var(--dsw-alias-label-tertiary, #8f959e); text-overflow: ellipsis; white-space: nowrap; }

/* ── 模型能力标记（如「可发图片」）──
   ⚠️ 只标在**支持**的那一类模型上：不标 = 不支持。这样用户扫一眼清单就知道
   哪些模型能发图，不必靠撞一次 unsupported_content 才发现。
   flex: none 是必需的 —— 它是行尾的固定标签，参与收缩会被 id/name 挤没。 */
.dim-jh-modelBadge { flex: none; padding: 1px 6px; border-radius: 5px; background: var(--dsw-alias-color-success-light, #e8ffea); color: var(--dsw-alias-color-success, #00875a); font-size: 11px; line-height: 16px; white-space: nowrap; }

/* ── 已下架（失效）模型（2026-10-06 复审 !66）──
   ⚠️ **必须放在上面 .dim-jh-modelName / .dim-jh-modelId 基础规则之后**：
   tests/unit/model-filter.spec.ts 的 ruleOf() 按**源码顺序**取第一条匹配规则，
   把带 [data-dead] 的规则写在前面会让它误取到本块、跳过基础规则。
   ⚠️⚠️ **本块内禁止出现反引号**（AGENTS.md 红线：样式是模板字符串，注释里的
   反引号会提前闭合模板；tests/unit/zcode-channel-dialog.spec.ts 有专门用例守着）。
   与「已关闭」**必须视觉可区分**：一个是用户的主动选择（开关可拨回），
   一个是系统判定上游已移除（只能点「重新显示」恢复）。若长得一样，用户会去拨
   那个根本不存在的开关，于是以为「模型被吞了、插件有 bug」。
   ⇒ 用左侧警示竖条 + 琥珀色角标，与「已关闭」的纯淡出区分开。 */
.dim-jh-modelRow[data-dead="true"] { box-shadow: inset 3px 0 0 var(--dsw-alias-warning, #d48806); }
.dim-jh-modelRow[data-dead="true"] .dim-jh-modelInfo { opacity: 1; }
.dim-jh-modelRow[data-dead="true"] .dim-jh-modelName { color: var(--dsw-alias-warning, #d48806); }
.dim-jh-modelDead {
  flex: none;
  padding: 1px 6px;
  border-radius: 4px;
  font-size: 11px;
  line-height: 16px;
  color: var(--dsw-alias-warning, #d48806);
  background: color-mix(in srgb, var(--dsw-alias-warning, #d48806) 12%, transparent);
}
.dim-jh-modelRestore {
  flex: none;
  padding: 2px 10px;
  border-radius: 6px;
  font-size: 12px;
  color: var(--dsw-alias-text-secondary, #4e5969);
  background: var(--dsw-alias-bg-layer-2, #f7f8fa);
  border: 1px solid var(--dsw-alias-border-l2, #dfe1e5);
  cursor: pointer;
}
.dim-jh-modelRestore:hover:not(:disabled) {
  color: #1677ff;
  border-color: color-mix(in srgb, #1677ff 40%, var(--dsw-alias-border-l2, #dfe1e5));
}
.dim-jh-modelRestore:disabled { opacity: 0.5; cursor: default; }
/* 顶部批量恢复条 */
.dim-jh-modelDeadBar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin: 6px 0;
  padding: 7px 10px;
  border-radius: 8px;
  font-size: 12px;
  color: var(--dsw-alias-text-secondary, #4e5969);
  background: color-mix(in srgb, var(--dsw-alias-warning, #d48806) 8%, transparent);
  border: 1px solid color-mix(in srgb, var(--dsw-alias-warning, #d48806) 26%, transparent);
}

/* ── 思考档位对照表 ──
   与模型清单的行**刻意不同**：那两行的 id/name 用 ellipsis 截断（清单要紧凑），
   而对照表的内容本身就是用户要抄的东西 —— 截断等于让功能失效
   （真实反馈：「你这对照表字符过长都被省略了」）。
   故这里用 grid 竖排：第一行模型 id + 徽标，后两行整宽、可换行、可选中复制。
   ⚠️ 绝不能加 text-overflow: ellipsis / white-space: nowrap。 */
.dim-jh-effortRow { display: grid; grid-template-columns: minmax(0, 1fr); gap: 3px; min-width: 0; padding: 7px 8px; border-radius: 8px; }
.dim-jh-effortRow:hover { background: var(--dsw-alias-bg-layer-2, #f7f8fa); }
.dim-jh-effortHead { display: flex; align-items: center; gap: 8px; min-width: 0; }
/* 模型 id 在这里允许换行（对照表不追求单行紧凑），但仍优先占满剩余宽度。 */
.dim-jh-effortModel { flex: 1 1 auto; min-width: 0; overflow-wrap: anywhere; font-family: ui-monospace, SFMono-Regular, Menlo, monospace; font-size: 12px; line-height: 18px; font-weight: 500; color: var(--dsw-alias-label-primary, #1f2329); }
.dim-jh-effortLine { min-width: 0; overflow-wrap: anywhere; font-size: 12px; line-height: 18px; color: var(--dsw-alias-label-secondary, #4e5969); }
.dim-jh-effortLabel { color: var(--dsw-alias-label-tertiary, #8f959e); }
/* 搜索结果为空时的提示（与其它空状态同款观感）。 */
.dim-jh-effortEmpty { padding: 8px; font-size: 12px; line-height: 18px; color: var(--dsw-alias-label-tertiary, #8f959e); }
/* 档位对照行里的模型名也**不得截断**（用户 2026-10-04：「所有胶囊内容都没有
   完整显示」）。这里本就没写 ellipsis，但补一条**显式**声明把意图锁住：
   下面「网关弹窗里的胶囊」那条 .dim-jh-modelId 规则不该被误用到它身上，
   而将来若有人给 .dim-jh-effortModel 加上截断，这条会立刻在单测里变红。
   ⚠️ 本文件整体是一个 JS 模板字符串，注释里**不能出现反引号** —— 会提前
   终止字符串（见 .dim-jh-iconBtn 处的同款警告）。 */
.dim-jh-gatewayModal .dim-jh-effortModel { overflow: visible; text-overflow: clip; white-space: normal; overflow-wrap: anywhere; }

/* ── 网关弹窗的排版收紧（用户 2026-10-04 要求：「这几段话之间的行间距小一点」）──
   ⚠️ 必须加 .dim-jh-gatewayModal 前缀：.dim-jh-modalHint 是**共用**类，
   另有 24 处调用点（账号面板、opencode 弹窗、备份弹窗…），本次只该动网关弹窗
   —— 全局把 10px 改成 6px 会悄悄改掉那些页面的观感。
   本弹窗有 8 处说明行，10px × 8 ≈ 80px 空隙，收成 6px 后观感明显紧凑。 */
.dim-jh-gatewayModal .dim-jh-modalHint { margin-top: 6px; }
/* 弹窗顶部那三行是**同一段话**（推荐 / 协议 / 地址），彼此再紧一档，
   否则它们看起来像三个互不相干的段落。
   ⚠️ 现在「思考档位对照表」那段的两句说明也用这个类（同样是一段话拆成两行，
   为的是不让品牌名 CC Switch 在行尾被从中间切开）。 */
.dim-jh-gatewayModal .dim-jh-gatewayLine { margin-top: 4px; }

/* ── 三段功能区的**分隔**（用户 2026-10-04：「这三坨字全混在一起有点难看清」）──
   用户把弹窗分成三块：① 开关 + 连接信息（推荐 / 协议 / 地址 / API KEY）
   ② 模型 ID 清单 ③ 思考档位对照表。
   此前三段之间只靠一个 16px 的 margin，而每段内部又各有若干行 12px 灰字，
   视觉上连成一片 —— 用户找不到「下一块从哪开始」。

   做法：段间用**一条上边框 + 更大的上间距**切开，段内保持原有的紧凑。
   ⚠️ 用 border-top 而不是背景色块：弹窗会被截图，色块在深色主题下还要另配
   一整套 token（本文件别处同理，见 .dim-jh-gatewayCards 的注释）。
   ⚠️ 第一段**不要**上边框（它紧跟在弹窗标题下，加线会像多出一条分隔条）。 */
.dim-jh-gatewaySection { display: block; }
.dim-jh-gatewaySection + .dim-jh-gatewaySection {
  margin-top: 12px; padding-top: 10px;
  border-top: 1px solid var(--dsw-alias-border-l2, #eef0f3);
}
/* 段标题比正文说明重一档，作为「这块讲什么」的锚点。
   ⚠️ 不能沿用 .dim-jh-modalHint 那套 12px 灰字 —— 段标题与段内说明同为灰色时，
   分隔线的效果会被抵消（用户仍然分不清哪一行是标题）。 */
.dim-jh-gatewaySectionTitle { margin: 0; font-size: 13px; line-height: 19px; font-weight: 600; color: var(--dsw-alias-label-primary, #1f2329); }
/* 段内第一行紧跟段标题，不再叠那 6px。 */
.dim-jh-gatewaySectionTitle + .dim-jh-modalHint { margin-top: 3px; }
/* 弹窗末尾那条安全提示：与上面三段拉开，且**不属于任何一段**（它是整个弹窗的脚注）。 */
.dim-jh-gatewayModal .dim-jh-gatewayFootnote { margin-top: 14px; padding-top: 10px; border-top: 1px solid var(--dsw-alias-border-l2, #eef0f3); }

/* ── 密钥行：小号按钮，不让按钮把这一行撑高 ──
   ⚠️ 用户 2026-10-04 报障：「复制密钥」「显示明文」大到把这行撑开。
   根因是 .dim-jh-btn 是**通用**按钮（padding 4px 12px + line-height 18px
   ⇒ 约 28px 高），而同行只有 18px 高的说明文字 —— 按钮比文字高 10px，
   整行的高度就被它顶起来了。这里单独压到约 22px，与文字行接近。
   ⚠️ 不能直接改 .dim-jh-btn（30+ 处调用点：页头、卡片头、对照表…）。
   ⚠️ 用户随后又要求「下面的四个按钮也按复制密钥的大小来统一」，故把
   清单/对照表那两个操作行（.dim-jh-gatewayActions）并进同一条规则 ——
   两处必须是**同一条规则**，各写一份迟早一处改了另一处没改。 */
.dim-jh-gatewayKeyRow { display: flex; align-items: center; flex-wrap: wrap; gap: 6px; }
.dim-jh-gatewayKeyRow .dim-jh-btn,
.dim-jh-gatewayModal .dim-jh-gatewayActions .dim-jh-btn { padding: 2px 8px; font-size: 11px; line-height: 16px; border-radius: 6px; }
/* 「API KEY：」标签：与按钮同基线、不可压缩（否则窄面板下会折成两行）。 */
.dim-jh-gatewayKeyRow .dim-jh-gatewayKeyLabel { flex: none; margin: 0; }
/* 下方两个操作行（模型清单 / 思考档位对照表）的间距也向密钥行看齐（8px → 6px）：
   四个按钮既然压成小号，原来的 8px 间隔配小按钮会显得松散。
   ⚠️ 另加 flex-wrap: wrap：.dim-jh-modelPanelActions 默认是 nowrap
   （页头那两三个固定按钮用它没问题），而这两行的按钮文案带**动态计数**
   （「复制全部 1234 个 ID」「复制对照表（1234 行）」），计数一长，nowrap
   会把按钮推出容器、点不到。实测当前数据（15 个模型）在 364px 窄弹窗下不出问题，
   但那是**当前数据**的结论 —— 换行让它对任意计数都成立。
   ⚠️ 本文件整体是一个 JS 模板字符串，注释里**不能出现反引号**（会提前终止字符串）。
   我在这条注释里用反引号括了一次类名，直接把文件从中间截断、构建产物语法错误 ——
   写类名时**裸写**，要强调就用「」或**。 */
.dim-jh-gatewayModal .dim-jh-gatewayActions { gap: 6px; flex-wrap: wrap; }

/* ── 网关清单：**一个供应商一张卡片**（模型清单与档位对照表共用）──
   用户 2026-10-03 报障：此前按「一个模型一行」平铺，同一家的
   deepseek-account/deepseek-flash 与 deepseek-account/deepseek-v4-pro
   看起来毫无关系；用户真正要对照的是「哪家有哪些模型」，故改成分组卡片。
   分组判据在 openai-gateway-panel.js 的 groupGatewayEntries（纯函数，可单测）。
   ⚠️ 本文件整体是 JS 模板字符串，注释里**不能出现反引号**（会提前终止字符串）。 */
.dim-jh-gatewayCards { display: grid; grid-template-columns: minmax(0, 1fr); gap: 8px; }
/* 卡片：细边框 + 圆角，与复用的分组头（.dim-jh-modelGroup）同一种规格语言。
   ⚠️ min-width: 0 是**必需**的：卡片是 grid 项，默认 min-width: auto 会被
   内部长 id 撑开，从而把整个弹窗顶出横向滚动条（模型列表踩过同一个坑）。 */
.dim-jh-gatewayCard { min-width: 0; border: .5px solid var(--dsw-alias-border-l3, #e5e5e5); border-radius: 10px; overflow: hidden; }
/* 卡片头：吸顶。清单很长时滚到底部仍要能看出「我在哪一家」。
   背景用不透明层色，否则卡内行会从下面透出来。 */
.dim-jh-gatewayCardHead { position: sticky; top: 0; z-index: 2; display: flex; align-items: flex-start; flex-wrap: wrap; gap: 4px 8px; min-width: 0; padding: 6px 8px; background: var(--dsw-alias-bg-layer-2, #f7f8fa); border-bottom: .5px solid var(--dsw-alias-border-l3, #e5e5e5); }
/* 卡片标题（可点，折叠开关）：占满剩余宽度。
   ⚠️ 用户 2026-10-04 报障「网关里的胶囊内容都没有完整显示」：此处原先是
   ellipsis + nowrap，长供应商名（或展示名）被截成 deepseek-accou…，
   而卡片标题正是用户**唯一**能确认「这一家是谁」的地方。
   改为换行显示：整行放不下时 title 独占一行，计数与「复制本组」按钮落到第二行。
   ⚠️ 绝不能加回 text-overflow: ellipsis / white-space: nowrap。 */
.dim-jh-gatewayCardToggle { flex: 1 1 auto; min-width: 0; padding: 2px 0; border: 0; background: transparent; color: var(--dsw-alias-label-primary, #1f2329); font-size: 12.5px; line-height: 18px; font-weight: 600; text-align: left; overflow-wrap: anywhere; cursor: pointer; }
.dim-jh-gatewayCardToggle:hover { color: var(--dsw-alias-brand-primary, #1677ff); }
/* 计数与按钮：可以掉到第二行，但**不换行、不截断**（flex: none 保证不被挤没）。 */
.dim-jh-gatewayCardCount { flex: 0 0 auto; margin-left: auto; font-size: 11px; line-height: 17px; font-variant-numeric: tabular-nums; white-space: nowrap; color: var(--dsw-alias-label-tertiary, #8f959e); }
.dim-jh-gatewayCardHead .dim-jh-gatewayCardBtn { flex: none; padding: 2px 8px; font-size: 11px; }
.dim-jh-gatewayCardBody { display: grid; grid-template-columns: minmax(0, 1fr); gap: 1px; padding: 3px 4px 5px; }
/* 一行模型（**只读**，无开关）：短名字 + 展示名 + 能力标记。
   ⚠️ 行内显示的是去掉供应商前缀的名字（见 gatewayModelKeyOf）—— 卡片头已经
   写清了是哪一家，再重复一遍前缀只会把整行挤到只剩省略号。
   ⚠️ 这里刻意**不用** .dim-jh-modelRow：那个类带 cursor: pointer 与 hover 底色，
   是给「整行可点切换开关」的 label 用的，用在这里会被读成可点。 */
.dim-jh-gatewayModelRow { display: flex; align-items: center; gap: 8px; min-width: 0; padding: 5px 6px; border-radius: 6px; }
.dim-jh-gatewayModelRow:hover { background: var(--dsw-alias-bg-layer-2, #f4f5f7); }
/* ⚠️ 这里曾有一条「.dim-jh-gatewayModelRow .dim-jh-modelId { max-width: 52% }」
   （把模型列表那 46% 的上限放宽到 52%）。**已删除**，别再写回来：
   它的优先级 (0,2,0) 与下面那条两级通配 (0,2,0) **完全相同**，
   于是谁生效**只取决于源码先后** —— 一个纯粹靠「写在对的位置」维持的假象。
   下面那条把 max-width 直接设成 none（且覆盖了开关行与 curl 代码块），
   一行里本该完整显示的东西，不该还留一个 52% 的上限等着被谁「压过去」。
   ⚠️ 本文件是 JS 模板字符串，注释里**绝不能出现反引号**（我在这条注释上
   第四次踩到同一个坑）—— 类名与代码一律裸写。 */

/* ── 网关弹窗里的「胶囊」一律完整显示（用户报障 2026-10-04）──
   原话：「网关这里面的所有胶囊内容都没有完整显示，应当修正使其完整显示内容」。
   胶囊 = 行内的 <code> ID 块（.dim-jh-modelId）、能力标记（.dim-jh-modelBadge）、
   档位对照行里的模型名（.dim-jh-effortModel）。

   ⚠️ 根因是那三个类**天生带截断**，而且是给**别处**设计的：
   .dim-jh-modelId 是 max-width 46% + ellipsis + nowrap（模型列表要紧凑，
   一行里还得塞下开关）；.dim-jh-modelBadge 是 nowrap；模型名同理。
   而网关弹窗里这些内容**就是要被读/被抄的东西** —— 截断等于让功能失效。

   ⚠️ 只能加 .dim-jh-gatewayModal 前缀：.dim-jh-modelId 在模型列表里
   另有用途（那里的紧凑是刻意的，见 model-filter.js 的模块注释），
   全局去掉 max-width 会把整个模型列表撑坏。

   ⚠️⚠️ **选择器只能写「弹窗 + 类名」两级，绝不能夹中间祖先**（这里返工过一次）：
   第一版写的是「弹窗 + gatewayModelRow + modelId」这类**带中间祖先**的三条规则，
   结果**只覆盖到了卡片内的胶囊**。用户随即又截图框出三处仍在截断的地方，
   它们的祖先链各不相同：
     - 开关行：modalBody > modelRow > modelInfo > code
     - 两处 curl 代码块：modalBody > code
   三条祖先限定规则**一条都匹配不上** ⇒ 用户看到的还是「供 Pi / Cont…」与
   「curl … -H "…"」被吃掉。
   ⚠️ 当时的「机械审计」脚本也没抓到，因为它只检查**类名**有没有被某条网关规则
   覆盖 —— modelId 在卡片那条规则里「有覆盖」，于是整体判通过。
   **教训：判断一个元素会不会被截断，必须看它实际的祖先链，不能看类名是否出现过。**
   现在改成两级通配，弹窗内任何位置的这个类都生效（含 API KEY 明文那块 code）。
   ⚠️ 本文件整体是 JS 模板字符串，注释里**绝不能出现反引号** ——
   我在上面这段注释里用反引号括类名，第三次踩到了同一个坑（构建直接失败）。
   要强调就用「」或 ** 包住类名，永远裸写。 */
.dim-jh-gatewayModal .dim-jh-modelId,
.dim-jh-gatewayModal .dim-jh-modelName {
  max-width: none; overflow: visible; text-overflow: clip; white-space: normal; overflow-wrap: anywhere;
}
/* 能力标记（「可发图片」「需对照」）：短标签，但 flex: none + nowrap 会在
   窄面板下被挤出可视区 —— 允许换行远比被切掉好。 */
.dim-jh-gatewayModal .dim-jh-modelBadge {
  flex: 0 0 auto; white-space: normal; overflow-wrap: anywhere;
}
/* ⚠️ 顶部开关行的标题「启用本机网关」**不许被挤**（去掉 ID 胶囊的 max-width 后
   新引入的问题）：胶囊不再有 46% 上限，于是它把整行宽度吃光，
   而 .dim-jh-modelName 是 flex: 0 1 auto（可收缩）⇒ 标题被压成
   「启用本机网」「关」两行。
   判据：网关弹窗的开关行里，标题保持固有宽度（flex: none），
   让**副标题**去换行 —— 副标题是补充说明，标题是动作主体。
   ⚠️ 本文件是 JS 模板字符串，注释里**绝不能出现反引号**（又踩了一次，第五次）。 */
.dim-jh-gatewayModal .dim-jh-modelInfo > .dim-jh-modelName { flex: none; }
/* 搜索/空结果的容器：与对照表的 .dim-jh-effortEmpty 同款观感。 */
.dim-jh-gatewayEmpty { padding: 8px; font-size: 12px; line-height: 18px; color: var(--dsw-alias-label-tertiary, #8f959e); }

/* ── 「Token 用量」弹窗（渠道 × provider × 模型 三级汇总 + 最近明细）──
   数据来自本机内存账本（usage.tokenLedger），记账点在注册收敛层。
   卡片结构与网关弹窗的供应商卡片同一规格语言（细边框 + 圆角 + 吸顶头），
   但不吸顶：本弹窗层级只有两级，吸顶收益小于复杂度。
   ⚠️ 本文件整体是 JS 模板字符串，注释里绝不能出现反引号 —— 类名一律裸写。 */
.dim-jh-ledgerFilters { display: flex; gap: 6px; margin-bottom: 8px; }
.dim-jh-ledgerFilterBtn { padding: 2px 10px; font-size: 11.5px; line-height: 17px; border-radius: 999px; opacity: .72; }
.dim-jh-ledgerFilterBtn[data-active="true"] { opacity: 1; font-weight: 600; }
.dim-jh-ledgerCard { min-width: 0; border: .5px solid var(--dsw-alias-border-l3, #e5e5e5); border-radius: 10px; overflow: hidden; }
.dim-jh-ledgerCardHead { display: flex; align-items: baseline; flex-wrap: wrap; gap: 4px 8px; padding: 7px 10px; background: var(--dsw-alias-bg-layer-2, #f7f8fa); border-bottom: .5px solid var(--dsw-alias-border-l3, #e5e5e5); }
.dim-jh-ledgerCardHead strong { font-size: 12.5px; line-height: 18px; color: var(--dsw-alias-label-primary, #1f2329); }
.dim-jh-ledgerCardSum { margin-left: auto; font-size: 11.5px; line-height: 17px; font-variant-numeric: tabular-nums; color: var(--dsw-alias-label-secondary, #4e5969); white-space: nowrap; }
.dim-jh-ledgerProvider { padding: 4px 10px 6px; }
.dim-jh-ledgerProvider + .dim-jh-ledgerProvider { border-top: .5px solid var(--dsw-alias-border-l3, #eef0f3); }
/* 账号层（第 2 期）：只在多账号（或含「未归属」）时渲染，缩进一层；
   单账号 provider 不展开 —— 一比一的中间层是纯噪音。 */
.dim-jh-ledgerAccount { min-width: 0; }
.dim-jh-ledgerAccount + .dim-jh-ledgerAccount { border-top: .5px dashed var(--dsw-alias-border-l3, #eef0f3); }
.dim-jh-ledgerAccountHead { display: flex; align-items: baseline; gap: 8px; padding: 3px 0 1px 8px; }
.dim-jh-ledgerAccountName { min-width: 0; font-size: 11.5px; line-height: 17px; font-weight: 600; color: var(--dsw-alias-label-secondary, #4e5969); overflow-wrap: anywhere; }
.dim-jh-ledgerAccountSum { margin-left: auto; flex: none; font-size: 11px; line-height: 16px; font-variant-numeric: tabular-nums; color: var(--dsw-alias-label-tertiary, #8f959e); white-space: nowrap; }
.dim-jh-ledgerProviderHead { display: flex; align-items: baseline; gap: 8px; padding: 4px 0 2px; }
.dim-jh-ledgerProviderName { min-width: 0; font-size: 12px; line-height: 18px; font-weight: 600; color: var(--dsw-alias-label-primary, #1f2329); overflow-wrap: anywhere; }
.dim-jh-ledgerProviderSum { margin-left: auto; flex: none; font-size: 11px; line-height: 16px; font-variant-numeric: tabular-nums; color: var(--dsw-alias-label-tertiary, #8f959e); white-space: nowrap; }
.dim-jh-ledgerModel { display: flex; align-items: baseline; gap: 8px; min-width: 0; padding: 2px 0 2px 10px; }
.dim-jh-ledgerModelName { flex: 1 1 auto; min-width: 0; font-family: ui-monospace, SFMono-Regular, Menlo, monospace; font-size: 11.5px; line-height: 17px; color: var(--dsw-alias-label-secondary, #4e5969); overflow-wrap: anywhere; }
.dim-jh-ledgerModelReq { flex: none; font-size: 11px; line-height: 16px; font-variant-numeric: tabular-nums; color: var(--dsw-alias-label-tertiary, #8f959e); white-space: nowrap; }
.dim-jh-ledgerModelTokens { flex: none; font-size: 11.5px; line-height: 17px; font-variant-numeric: tabular-nums; color: var(--dsw-alias-label-primary, #1f2329); white-space: nowrap; }
/* 均值格（首字 · 速率）：三级汇总行（渠道/账号/模型）共用，等宽数字对齐。 */
.dim-jh-ledgerPerf { flex: none; font-size: 11px; line-height: 16px; font-variant-numeric: tabular-nums; color: var(--dsw-alias-label-tertiary, #8f959e); white-space: nowrap; }
/* ── 历史视图（第 4 期）：趋势柱状图 + 展开的日聚合树 ──
   柱状图用纯 div（不引图表库）：每根柱 = 一天，高度按相对最大值的百分比。
   ⚠️ 容器 height 由 barHeightPercent 的**百分比**决定上限（88%），故这里留出
   足够净高；柱宽**不能**靠 flex 拉伸填满 —— 单日窗口只有一根柱时
   flex: 1 0 14px 会把它横向拉满整行，呈现为「一整条白块」。
   故给单柱/少柱窗口一个 max-width，让它保持柱子的形状。 */
.dim-jh-ledgerSection { margin-bottom: 14px; }
.dim-jh-ledgerSectionTitle { font-size: 13px; line-height: 19px; font-weight: 600; color: var(--dsw-alias-label-primary, #1f2329); margin-bottom: 6px; }
.dim-jh-ledgerTrend { display: flex; align-items: flex-end; gap: 3px; height: 64px; padding: 4px 2px 0; overflow-x: auto; }
/* 柱子列：横向可滚、但单柱不吃满宽（见上方注释）。 */
.dim-jh-ledgerTrendCol { flex: 1 0 14px; min-width: 14px; max-width: 56px; height: 100%; display: flex; align-items: flex-end; cursor: pointer; }
/* ⚠️ 颜色必须**自带可见性**：品牌变量 --dsw-alias-brand-primary 只在变量
   *缺失* 时才走 #1677ff 兜底；一旦宿主主题把它定义成接近底色的浅色，再叠
   opacity 就会退化成一块与背景同色的「白块」（真实缺陷 2026-10-07，深色主题
   实测）。故：① opacity 从 .45 提到 .55（仍是「未选中」的弱化语义），
   ② 补 box-shadow 描边 —— 即使底色与背景同色也留有边界，不会「消失」。 */
.dim-jh-ledgerTrendBar { width: 100%; min-height: 2px; border-radius: 3px 3px 0 0; background: var(--dsw-alias-brand-primary, #1677ff); box-shadow: inset 0 0 0 1px rgba(127,127,127,.45); opacity: .55; transition: opacity .12s ease; }
.dim-jh-ledgerTrendBar:hover { opacity: .8; }
.dim-jh-ledgerTrendBar[data-today="true"] { opacity: .95; }
.dim-jh-ledgerTrendBar[data-active="true"] { opacity: 1; outline: 1.5px solid var(--dsw-alias-brand-primary, #1677ff); }
/* 日期短标签：常驻显示 MM-DD，根治「不知道哪根柱是哪天」。 */
.dim-jh-ledgerTrendLabel { font-size: 10px; line-height: 12px; text-align: center; color: var(--dsw-alias-label-tertiary, #8f959e); font-variant-numeric: tabular-nums; }
/* 明细表：紧凑小字；模型列允许换行（长 wire 名不能截断 —— 网关弹窗同一条教训）。 */
.dim-jh-ledgerTable { width: 100%; margin-top: 10px; border-collapse: collapse; font-size: 11.5px; line-height: 17px; }
.dim-jh-ledgerTable th { position: sticky; top: 0; z-index: 1; padding: 4px 6px; text-align: left; font-weight: 600; color: var(--dsw-alias-label-tertiary, #8f959e); background: var(--dsw-alias-bg-layer-1, #fff); border-bottom: .5px solid var(--dsw-alias-border-l3, #e5e5e5); }
.dim-jh-ledgerTable td { padding: 3px 6px; border-bottom: .5px solid var(--dsw-alias-border-l3, #f0f1f3); font-variant-numeric: tabular-nums; color: var(--dsw-alias-label-secondary, #4e5969); vertical-align: baseline; }
.dim-jh-ledgerTable .dim-jh-ledgerEntryModel { font-family: ui-monospace, SFMono-Regular, Menlo, monospace; color: var(--dsw-alias-label-primary, #1f2329); overflow-wrap: anywhere; }

/* ── 模型列表的「计费/来源」分组（订阅 / 免费 / Cline Cloud / 按量计费）──
   分组由 model-groups.js 的纯函数决定（判据见其模块注释）；这里只管观感。
   ⚠️ 组头是**独立的 div**，绝不能包进 .dim-jh-modelRow 的 <label> 里 ——
   label 内点任意位置都会切换可见性开关（见 jet-hub.js 里「刻意不做多选」的说明）。 */
.dim-jh-modelGroup { min-width: 0; }
/* 组头吸顶：按量计费那组展开后有 460+ 行，滚到底部时也要能看到「我在哪一组」。
   背景用不透明层色，否则行会从下面透出来。 */
.dim-jh-modelGroupHead { position: sticky; top: 0; z-index: 2; display: flex; align-items: center; gap: 8px; min-width: 0; padding: 6px 8px 5px; background: var(--dsw-alias-bg-layer-1, #fff); border-bottom: 0.5px solid var(--dsw-alias-border-l3, #e5e5e5); }
.dim-jh-modelGroupToggle { flex: 1 1 auto; min-width: 0; padding: 2px 0; border: 0; background: transparent; color: var(--dsw-alias-label-primary, #1f2329); font-size: 12.5px; font-weight: 600; text-align: left; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; cursor: pointer; }
.dim-jh-modelGroupToggle:hover { color: var(--dsw-alias-brand-primary, #1677ff); }
.dim-jh-modelGroupCount { flex: none; font-size: 11px; font-variant-numeric: tabular-nums; color: var(--dsw-alias-label-tertiary, #8f959e); }
/* 组头的两个按钮做紧凑处理：组头一行里要放下折叠标题 + 计数 + 两个按钮。 */
.dim-jh-modelGroupHead .dim-jh-modelGroupBtn { flex: none; padding: 2px 8px; font-size: 11px; }
.dim-jh-modelGroupBody { display: grid; grid-template-columns: minmax(0, 1fr); gap: 2px; padding: 2px 0 6px; }

/* 开关：基于 checkbox 绘制，保持原生语义（可聚焦、可键盘操作、可读屏） */
.dim-jh-switch { flex: none; appearance: none; -webkit-appearance: none; position: relative; width: 34px; height: 20px; margin: 0; border-radius: 999px; background: var(--dsw-alias-border-l2, #d0d3d9); cursor: pointer; transition: background .18s ease; }
.dim-jh-switch::after { content: ''; position: absolute; top: 2px; left: 2px; width: 16px; height: 16px; border-radius: 50%; background: #fff; box-shadow: 0 1px 3px rgb(31 35 41 / 20%); transition: transform .18s ease; }
.dim-jh-switch:checked { background: #1677ff; }
.dim-jh-switch:checked::after { transform: translateX(14px); }
.dim-jh-switch:focus-visible { outline: none; box-shadow: 0 0 0 2px color-mix(in srgb, #1677ff 30%, transparent); }
.dim-jh-switch:disabled { opacity: 0.5; cursor: default; }

/* ── 账号备份（导出 / 恢复）── */
/* 口令输入框：宽度撑满弹窗内容区，避免在窄面板下挤坏布局。
   ⚠️ 背景必须用**真实存在**的 token。早期写的是 --dsw-alias-bg-input，而主题里
   根本没有这个 token（真实的是 bg-base / bg-layer-1/2/3）—— var() 遇不存在的
   token **不报错**，静默取 fallback #fff，于是深色模式下变成「浅色文字 + 白底」，
   文字完全看不见（用户报障）。这里对齐官方 Input 原语用的 bg-layer-1，
   并去掉 fallback 以免再次掩盖 token 拼错。 */
.dim-jh-input { box-sizing: border-box; width: 100%; padding: 6px 10px; border: 1px solid var(--dsw-alias-border-l2); border-radius: 6px; background: var(--dsw-alias-bg-layer-1); font-size: 13px; color: var(--dsw-alias-label-primary); }
.dim-jh-input:focus { outline: none; border-color: #1677ff; box-shadow: 0 0 0 2px color-mix(in srgb, #1677ff 20%, transparent); }
/* placeholder 用官方 Input 的 dimmed 色：默认色在深色模式下对比度不足。 */
.dim-jh-input::placeholder { color: var(--dsw-alias-label-dimmed); }
/* 加密勾选行：勾选框 + 文案一行排开 */
.dim-jh-checkRow { display: flex; align-items: center; gap: 8px; margin: 10px 0 4px; font-size: 13px; color: var(--dsw-alias-label-primary, #1f2329); cursor: pointer; }
.dim-jh-checkRow input[type="checkbox"] { margin: 0; accent-color: #1677ff; }
/* 两次口令输入：纵向堆叠 */
.dim-jh-formRows { display: flex; flex-direction: column; gap: 8px; margin: 8px 0 4px; }
/* 弹窗底部动作区：右对齐（生成/确认按钮） */
.dim-jh-modalActions { display: flex; justify-content: flex-end; gap: 8px; margin-top: 14px; }


/* ── Cline「订阅额度」弹窗（官方额度窗口 + 请求记录）── */
/* 账号翻页器:⚠️ **一次只看一个账号**(参考实现同款,多账号全铺开会让
   额度卡与记录表都极长);额度窗口与请求记录**共享同一个索引**。
   ⚠️ 单账号时整行都不渲染(见 renderQuota)——箭头无处可去。 */
.dim-jh-quotaGroup { display: flex; flex-direction: column; gap: 12px; }
.dim-jh-quotaPager { display: flex; align-items: center; gap: 8px; }
/* 24×24 方形按钮(参考实现 .cp-usage-nav):箭头是导航控件,不是文字按钮。 */
.dim-jh-quotaArrow { box-sizing: border-box; width: 24px; height: 24px; flex: none; padding: 0; border: 0.5px solid var(--dsw-alias-border-l2, #d0d3d9); border-radius: 6px; background: transparent; color: var(--dsw-alias-label-primary, #1f2329); font-size: 13px; line-height: 1; cursor: pointer; }
.dim-jh-quotaArrow:hover { border-color: var(--dsw-alias-brand-primary, #1677ff); }
.dim-jh-quotaAccountName { display: flex; align-items: center; gap: 8px; flex: 1 1 auto; min-width: 0; }
.dim-jh-quotaAccountLabel { overflow: hidden; font-size: 13px; font-weight: 600; color: var(--dsw-alias-label-primary, #1f2329); text-overflow: ellipsis; white-space: nowrap; }
.dim-jh-quotaIndex { flex: none; font-size: 12px; font-variant-numeric: tabular-nums; color: var(--dsw-alias-label-tertiary, #8f959e); }
/* 读数区:**多窗口并排卡片**(grid,参考实现同款)。
   ⚠️ auto-fit + 最小 170px:窄面板自动换列,不会把卡片压成一条。 */
.dim-jh-quotaWindows { display: grid; grid-template-columns: repeat(auto-fit, minmax(170px, 1fr)); gap: 10px; }
.dim-jh-quotaWindow { display: flex; flex-direction: column; gap: 8px; padding: 10px 12px; border: 0.5px solid var(--dsw-alias-border-l2, #d0d3d9); border-radius: 8px; }
.dim-jh-quotaWindowHead { display: flex; align-items: baseline; justify-content: space-between; gap: 8px; }
.dim-jh-quotaWindowName { font-size: 12px; font-weight: 600; color: var(--dsw-alias-label-secondary, #555); }
/* 百分比是这张卡唯一要读的数 —— 18px 大字(参考实现同款) */
.dim-jh-quotaWindowPercent { font-size: 18px; font-weight: 600; font-variant-numeric: tabular-nums; color: var(--dsw-alias-label-primary, #1f2329); }
.dim-jh-quotaWindowPercent[data-tone="warn"] { color: var(--dsw-alias-state-warn-primary, #d9822b); }
.dim-jh-quotaWindowPercent[data-tone="error"] { color: var(--dsw-alias-state-error-primary, #d93025); }
/* 进度条:宽度用的就是**夹取后**的百分比(与文案同一个值)。
   ⚠️ 正常档是**绿色**(参考实现 usageColor):全染品牌蓝会让「用掉九成」
   与「用掉一成」看起来一样,额度条就失去警示作用。 */
.dim-jh-quotaBar { height: 6px; overflow: hidden; border-radius: 999px; background: var(--dsw-alias-border-l2, #d0d3d9); }
.dim-jh-quotaBarFill { height: 100%; border-radius: 999px; background: var(--dsw-alias-state-success-primary, #2ea043); transition: width .3s; }
.dim-jh-quotaBarFill[data-tone="warn"] { background: var(--dsw-alias-state-warn-primary, #d9822b); }
.dim-jh-quotaBarFill[data-tone="error"] { background: var(--dsw-alias-state-error-primary, #d93025); }
.dim-jh-quotaReset { font-size: 12px; color: var(--dsw-alias-label-tertiary, #8f959e); }
/* 不可用 / 无窗口的静默文案(参考实现 .cp-muted) */
.dim-jh-quotaMuted { font-size: 12px; line-height: 17px; color: var(--dsw-alias-label-tertiary, #8f959e); word-break: break-word; }

/* 请求记录区:与额度区用上边框分开 */
.dim-jh-quotaLog { margin-top: 14px; padding-top: 12px; border-top: 1px solid var(--dsw-alias-border-default, #e5e5e5); }
.dim-jh-quotaSectionTitle { margin: 0 0 4px; font-size: 13px; font-weight: 600; color: var(--dsw-alias-label-primary, #1f2329); }
/* 说明「记录是本地流水」的提示:让用户知道重启会清空,而不是丢数据。 */
.dim-jh-quotaLogHint { margin: 0 0 8px; font-size: 11px; line-height: 16px; color: var(--dsw-alias-label-tertiary, #8f959e); }
/* 表格容器:⚠️ 自带纵向滚动 + 表头 sticky(参考实现 .cp-history 的 280px):
   长列表在弹窗内滚,表头始终可见。 */
.dim-jh-quotaTableWrap { max-height: 280px; overflow: auto; padding: 0 4px 2px; }
.dim-jh-quotaTable { width: 100%; border-collapse: collapse; table-layout: auto; font-size: 12px; }
/* ⚠️ td 默认 overflow:hidden:TOKEN / 延迟 / 错误列必须各自改成 normal+visible,
   否则它们继承的截断会把内容吃掉(参考实现踩过同一个坑)。 */
.dim-jh-quotaTable th, .dim-jh-quotaTable td { padding: 6px 0; border-bottom: 0.5px solid var(--dsw-alias-border-l2, #eee); font-size: 12px; vertical-align: middle; text-align: center; overflow: hidden; }
.dim-jh-quotaTable th { position: sticky; top: 0; z-index: 1; background: var(--dsw-alias-bg-layer-1, #fff); font-weight: 400; font-size: 11px; color: var(--dsw-alias-label-tertiary, #8f959e); white-space: nowrap; }
.dim-jh-quotaTable tbody tr:hover td { background: var(--dsw-alias-bg-layer-2, #f4f5f7); }
/* 列宽:状态点 16px、时间 82px(参考实现实测值,防时间戳被截断)。
   ⚠️ 用复合选择器:单独一个类的优先级压不过 .dim-jh-quotaTable td 的 (0,1,1)。 */
.dim-jh-quotaTable .dim-jh-quotaDotCol { width: 16px; }
.dim-jh-quotaTable .dim-jh-quotaWhenCol, .dim-jh-quotaTable .dim-jh-quotaWhen { width: 82px; }
.dim-jh-quotaTable td.dim-jh-quotaWhen { color: var(--dsw-alias-label-tertiary, #8f959e); font-variant-numeric: tabular-nums; }
/* 状态点:绿=成功、红=失败(参考实现同款;错误消息在 title 里)。 */
.dim-jh-quotaDot { display: inline-block; width: 8px; height: 8px; border-radius: 50%; background: var(--dsw-alias-state-success-primary, #2ea043); }
.dim-jh-quotaDot[data-tone="error"] { background: var(--dsw-alias-state-error-primary, #d93025); }
/* 模型列:等宽字 + 省略号(模型 id 是最该被扫到的标识);上游做成 tag。 */
.dim-jh-quotaModel { display: block; max-width: 100%; overflow: hidden; font-family: ui-monospace, SFMono-Regular, Menlo, monospace; color: var(--dsw-alias-label-primary, #1f2329); text-overflow: ellipsis; white-space: nowrap; }
.dim-jh-quotaMeta { display: flex; align-items: center; justify-content: center; gap: 6px; min-width: 0; overflow: hidden; margin-top: 2px; }
.dim-jh-quotaTag { padding: 1px 6px; border-radius: 5px; background: var(--dsw-alias-bg-layer-2, #f4f5f7); font-size: 11px; color: var(--dsw-alias-label-tertiary, #8f959e); white-space: nowrap; }
/* TOKEN 列:⚠️ 必须**允许折行**(参考实现同款)—— nowrap 会让
   「↓12.3k ↑4.5k ⚡1.2k 🧠89」把表格撑出横向滚动。 */
.dim-jh-quotaTable td.dim-jh-quotaTokens { font-size: 11.5px; font-variant-numeric: tabular-nums; color: var(--dsw-alias-label-secondary, #555); white-space: normal; overflow: visible; }
/* 延迟列:三行(首字 / 总耗时 / 输出速率),标签左、数值右(参考实现同款)。 */
.dim-jh-quotaTable td.dim-jh-quotaLoad { font-variant-numeric: tabular-nums; white-space: normal; overflow: visible; }
.dim-jh-quotaLoadRow { display: flex; justify-content: space-between; gap: 5px; max-width: 112px; margin: 0 auto; font-size: 11.5px; line-height: 1.5; }
.dim-jh-quotaLoadRow > span { white-space: nowrap; }
.dim-jh-quotaLoadRow > span:first-child { flex: none; }
.dim-jh-quotaLoadRow > span:last-child { min-width: 0; overflow: hidden; text-align: right; text-overflow: ellipsis; }
.dim-jh-quotaLoadKey { color: var(--dsw-alias-label-tertiary, #8f959e); }
/* 失败行:错误消息随行横跨数据列。⚠️ 必须允许折行 —— 错误文案(429 / 11140)
   很长,继承 td 的 nowrap + hidden 会把表格撑出横向滚动(参考实现的原坑)。 */
.dim-jh-quotaTable tr[data-error="true"] td { color: var(--dsw-alias-state-error-primary, #d93025); }
.dim-jh-quotaTable td.dim-jh-quotaError { font-size: 11.5px; line-height: 1.5; white-space: normal; overflow: visible; text-overflow: clip; word-break: break-word; }

/*
 * ── ZCode 登录渠道弹窗（Jet Hub 设置页；dim-jh-zcDialog / dim-jh-zcSelect） ──
 *
 * 这套样式**只服务 Jet Hub 设置页**的「添加账号 → 登录渠道」弹窗
 * （BigModel 国内 / z.ai 国际，见 jet-hub.js 的 zcodeProvider 下拉）。
 *
 * ⚠⚠️ 随 Gitee issue IKJLHQ 一并移除的还有一整套「官方「模型卡片 → 编辑」内的
 * ZCode 账号区」样式（.dim-jh-zcSection / zcSummary / zcAccount / zcMeta / zcBtn
 * / zcEmpty / zcNotice / zcLink，以及 [data-jet-hub-hidden]）。那套 UI 靠占用
 * settings.models.provider-card 槽的 llm-pi-ai key 实现，而该 key 与第三方
 * pi-ai 扩展**物理互斥**（机制见 index.js 文件头的长注释）—— ZCode 账号管理
 * 现在只在 Jet Hub 页。**别把那套样式加回来。**
 *
 * 颜色一律走 --dsw-alias-* 令牌；变量取不到时用 CSS 系统色（Canvas / CanvasText）
 * 兜底，跟随系统深浅色。
 * ⚠ 本文件整体是 JS 模板字符串，注释里**不能出现反引号**（会提前终止字符串）。
 */
/* 登录渠道选择（BigModel 国内 / z.ai 国际） */
.dim-jh-zcProvider { display: inline-flex; align-items: center; gap: 6px; color: var(--dsw-alias-label-secondary, GrayText); font-size: 12px; line-height: 18px; }
/* ⚠ background/color 的变量**必须带 fallback**（真实报障 2026-10-03：下拉里两项
   全是白的，只有 hover 才看见内容）。
   两个原因叠加：
   ① 原写法 background 设为全透明（0 0）+ color 用了无 fallback 的变量
      —— 变量在弹窗 scope 里取不到时 color 整条失效，退回继承。
   ② ⚠ Windows/Electron 下 **option 的背景不继承 select 的 background**，
      由系统主题决定；文字却**继承** color。
   ⇒ 浅色系统上就是「白底 + 失效/白字」⇒ 看不见。
   修法：① 变量都带 fallback，且 fallback 用 **CSS 系统色**（Canvas/CanvasText），
   它跟随系统深浅色；② 给 option 单独声明背景与文字色。
   ⚠⚠ 本注释里**绝不能出现反引号**（STYLES 是模板字符串，见 AGENTS.md）。 */
.dim-jh-zcSelect { box-sizing: border-box; height: 28px; padding: 0 8px; border: .5px solid var(--dsw-alias-border-l3, #dee0e3); border-radius: 14px; background: var(--dsw-alias-bg-layer-3, Canvas); color: var(--dsw-alias-label-primary, CanvasText); font-size: 12px; line-height: 18px; cursor: pointer; }
/* ⚠ 必须单独声明：option 的背景不继承 select（见上）。缺这条就是「下拉项全白」。 */
.dim-jh-zcSelect option { color: var(--dsw-alias-label-primary, CanvasText); background-color: var(--dsw-alias-bg-layer-3, Canvas); }
.dim-jh-zcSelect:disabled { opacity: .4; cursor: default; }
.dim-jh-zcSelect:focus-visible { outline: none; box-shadow: 0 0 0 2px var(--dsw-alias-border-l3, #dee0e3); }
.dim-jh-zcDialogMask { position: fixed; inset: 0; z-index: 1000; display: flex; align-items: center; justify-content: center; background: rgba(0, 0, 0, .45); }
.dim-jh-zcDialog { box-sizing: border-box; width: min(420px, calc(100vw - 32px)); padding: 18px 20px; border-radius: 10px; background: var(--dsw-alias-bg-layer-3, Canvas); box-shadow: 0 8px 32px rgba(0, 0, 0, .28); }
/* ⚠ 背景**必须**用 bg-layer-3（真实报障 2026-10-03：深色主题下弹窗是白的）。
   原先写的是 --dsw-alias-surface —— 那是**猜的变量名，DSH 里并不存在**，
   于是 fallback 里的浅色生效 ⇒ 深色主题下白底 + 浅色字，几乎看不见。
   本文件通篇用 bg-layer-1/2/3；fallback 用 CSS 系统色 Canvas（跟随系统深浅），
   这样变量取不到时也不会退回浅色。 */
.dim-jh-zcDialogTitle { margin: 0 0 6px; font-size: 15px; font-weight: 600; color: var(--dsw-alias-label-primary, CanvasText); }
.dim-jh-zcDialogHint { margin: 0 0 12px; font-size: 12px; line-height: 17px; color: var(--dsw-alias-label-tertiary, GrayText); }
.dim-jh-zcDialog .dim-jh-zcProvider { display: flex; align-items: center; gap: 8px; margin: 0 0 16px; font-size: 13px; }
.dim-jh-zcDialogActions { display: flex; justify-content: flex-end; gap: 8px; }

/* ── 用量徽标（会话输入区，模型选择器旁） ────────────────────────────────
   折叠态是一枚紧凑按钮，浮层用 position:absolute + bottom:calc(100% + 8px)
   向上展开（贴着输入区上沿，不遮挡输入框）。
   ⚠ 输入区（RlGAzG_root / dock / trailing / standardControls）没有
   overflow:hidden（只有文本域 .RlGAzG_scroll 是 overflow-y:auto），故浮层
   不会被裁剪 —— 若将来上游给这些容器加上裁剪，这里要改成固定定位 + 锚点换算。

   浮层尺寸口径（用户 2026-10-02：「小巧、美观，但信息不能缺失」→「不够小巧和精致」
   →「额度那块文字居中 + 浅色模式下按钮和线条太不明显」三轮迭代后定稿）：
   - 宽 **280px**、正文字号 10–11px、节间距 7px、进度条 3px；
   - **每个订阅窗口只占一行**：名称 / 进度条 / 百分比 / 重置倒计时；
   - 订阅额度那块用 grid **整块水平居中**（justify-content: center），列仍对齐；
   - 按钮与分隔线一律用**主题描边**（--dsw-alias-border-l2，全不透明）——
     试过「去线条」，浅色模式下按钮和分区线会看不见，用户明确反馈后撤回；
   - 阴影双层（近处极淡 + 远处扩散），比单层大阴影更精致。
   信息项一项未减（渠道名、更新时间与缓存标记、偏好三态、窗口百分比与倒计时、
   套餐名称与到期与账号数、逐账号余额与分桶、停用/失败计数、两个签到按钮）。 */
/* ⚠️ 徽标宽度会**挤压右侧的模型选择器**（真机报障 2026-10-02）。
 *
 * ## 症状
 *
 * 徽标与模型选择器同在 composer 一行（徽标 order:100 在模型选择器左侧），
 * 而本元素是 flex: none —— 不参与收缩。原先 max-width: 280px 会把
 * 「图标 + 模型名 + ▾」的模型选择器压到只剩几十 px，**图标被挤没**，
 * 用户看到「只有放大到很大才能看到那个图标」。
 *
 * ## 为什么会暴露
 *
 * 早期徽标永不显示（store.current 恒为 null 的缺陷期），模型选择器独占
 * 整行所以一直正常；徽标修好后开始占位，才暴露出这个抢占。
 *
 * ## 两级收敛（用户定：1+2）
 *
 * ## 宽度：**内容自适应**，216px 只是上限（不是目标宽度）
 *
 * 胶囊本身是**内容自适应**的：宽度由实际文字撑开，短内容就短
 * （Cline • 5积分 仅 93px），不会有固定留白。下面的 max-width 只在
 * 内容**过长**时兜底，防止挤压右侧的模型选择器。
 *
 * ⚠️ 2026-10-03 上限从 150px 提到 **216px**（用户报障「供应商名字显示不完整」
 * 之后又提了一次）。两次报障、同一条链路的两个不同成因：
 * 1. **数字被截**（第一次）：整句塞进一个 overflow:hidden 的 span，省略号从右往左
 *    吃掉了余额。修法是**结构**（拆成可独立收缩的 span，见 badge-model.js 的
 *    readingOf 与 usage-badge.js 的三段式渲染）；
 * 2. **渠道名被截**（第二次）：结构修好后，186px 仍不够长渠道名 + 长余额，
 *    于是省略号落到**名字**上（用户截图：CodeArts (华为…）。216px 解决它。
 *
 * **216 是量出来的，不是拍的**：实测 13 个渠道展示名 × 10 种现实读数
 * （含 123456.78积分 / 9499.84积分 / 94.54MToken / 0积分）共 130 组，
 * 需要的最宽度是 **209.34px**（WorkBuddy (国际版) • 123456.78积分），
 * 216px 全部装得下且留 6.7px 余量（字体回退时字宽会变）。
 *
 * ⚠️ **上限不能再往上放**：实测把上限放到「无上限」时，极窄 composer（220px）
 * 下胶囊会**溢出容器 8.3px** —— 那正是 2026-10-02「模型选择器图标被挤没」
 * 的形态。内容自适应 + 合理上限，两者缺一不可。
 * 兜底退化顺序：超长时**渠道名**先出省略号，**余额一个字都不会少**
 * （.dim-jh-badgeReading 是 flex: none）。
 *
 * ⚠️ button 元素**本来就是 border-box**（UA 默认样式表），故下面的 box-sizing
 * 是**显式写出**（防止哪天元素换成 span/div 时语义悄悄变成内容盒），不是修复。
 * ⚠️⚠️ 本段注释里**不能出现反引号**（本文件整体是 JS 模板字符串，会提前终止）。
 * ② 容器再窄时（< 720px，媒体查询挂在**全局宽度**上：composer 宽度受
 *    侧栏影响，用容器查询无法表达「右侧还剩多少」）**只留状态点**，
 *    文字与 chevron 全部隐藏 —— 此时代理器优先级让给模型选择器。 */
/* ⚠️ flex: 0 1 auto（而非 none）：胶囊自身也要能收缩。它是 flex: none 时
   宽度只由 max-width 兜底，而 max-width 只在**内容超标**时生效 —— 容器更窄时
   它会连同右侧模型选择器一起溢出。允许收缩后，空间不足时胶囊与选择器一起让位，
   且胶囊内部退化是有序的（先名字出省略号，数字最后才动）。 */
.dim-jh-badge { position: relative; display: flex; align-items: center; flex: 0 1 auto; min-width: 0; }
/* ⚠️ box-sizing 是**显式写出**的，不是修复：button 的 UA 默认样式表本就是
   border-box（实测 offsetWidth 与 max-width 相等）。写上它只为在元素类型被改
   （span/div 默认是 content-box）时语义不悄悄漂移 —— 那种情况下 max-width 会从
   「占位宽度」变成「内容宽度」，胶囊会突然变宽 19px 而这行代码看着毫无变化。 */
.dim-jh-badgeBtn { box-sizing: border-box; display: flex; align-items: center; gap: 5px; max-width: 216px; min-width: 0; padding: 2px 9px; border: .5px solid var(--dsw-alias-border-l2); border-radius: 999px; background: var(--dsw-alias-bg-layer-2); color: var(--dsw-alias-label-secondary); font: inherit; font-size: 11px; line-height: 1.5; cursor: pointer; white-space: nowrap; font-variant-numeric: tabular-nums; transition: background .15s ease, color .15s ease, border-color .15s ease; }
.dim-jh-badgeBtn:hover { background: var(--dsw-alias-bg-layer-3); color: var(--dsw-alias-label-primary); }
.dim-jh-badgeBtn[aria-expanded="true"] { background: var(--dsw-alias-bg-layer-3); color: var(--dsw-alias-label-primary); border-color: color-mix(in srgb, #1677ff 45%, var(--dsw-alias-border-l2)); }
/* 三段式文本容器：渠道名 + 分隔符 + 读数。
   ⚠️ 必须是 flex 容器，两个文本子项才能各自独立收缩（这正是「数字不被截」的前提）。 */
.dim-jh-badgeText { display: flex; align-items: center; min-width: 0; }
/* ⚠️ 让位顺序：中段说明(999) → 渠道名(99) → 读数(none，**不参与收缩**)。
   收缩量按 flex-shrink × flex-basis 加权分配，999 与 99 相差一个数量级，
   故「包名先走、渠道名其次」，读数一个字都不会少。
   flex-grow 一律为 0：胶囊是内容自适应宽度，任何 grow 都只会让它在宽容器里虚胖。
   min-width:0 是省略号生效的前提（flex 子项默认 min-width:auto，会撑破 max-width）。 */
.dim-jh-badgeName { flex: 0 99 auto; min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
/* 分隔符「•」：定宽不收缩，两侧间距由 margin 给。
   ⚠️ 这里的 3px **不能**换成前后空格：flex 容器把 span 之间只含空白的文本节点
   渲染成**零宽**（实测 gap=0，节点还在但看不见），空格会静默消失。
   带空格的那份写法（BADGE_SEP = ' • '）只用于 title / aria-label 那句整文。 */
.dim-jh-badgeSep { flex: none; margin: 0 3px; }
/* 中段说明（只有套餐模式有值，如包名 Free Plan Subscription）。
   ⚠️ 收缩权重**最大**（999）：它比渠道名更该让位 —— 包名多为样板字、且长，
   而用户认得出自己选的渠道。
   ⚠️ 它被压到 0 宽时（内容过长）该节点仍在，会留下一个「悬空的分隔符」；
   这在胶囊这一行是可接受的（完整文案在 title 与弹窗里），故不额外做条件渲染。 */
.dim-jh-badgeDetail { flex: 0 999 auto; min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
/* ⚠️⚠️ 读数必须 flex: none（**完全不收缩**），不能写 flex: 0 1 auto。
   这是第二处、也是更隐蔽的一处「数字被截」成因，**靠截图才发现**：

   收缩量按 flex-shrink × flex-basis 加权分配，读数那一份虽小却**不为零** ——
   实测读数被分到 0.77px 的收缩（getBoundingClientRect 宽 78.23 / 内容宽 79）。
   而 text-overflow: ellipsis 的触发条件是 scrollWidth > clientWidth，
   **亚像素级的收缩就足以命中** ⇒ 最后一个字被换成「…」，
   视觉上和「整段被截」一样严重。

   ⚠️ 更坑的是**量不出来**：clientWidth 取整，53.97 报成 54，
   于是 scrollWidth > clientWidth 判为 false —— 我用它写的「数字是否完整」
   断言一路全绿，而截图里明明白白写着「347.87积…」。
   教训：判断省略号要看**截图**或用小数宽度（getBoundingClientRect），
   不能用取整后的 clientWidth。

   ⚠️ 类名刻意**不叫 .dim-jh-badgeValue**：那个名字已被弹窗读数占用，且那条规则
   （font-weight:600）排在样式表更后面 —— 同名会让胶囊数字被静默加粗。

   ⇒ 读数恒定完整；全部收缩由 .dim-jh-badgeDetail(999) 与 .dim-jh-badgeName(99)
   承担，两者最坏缩到 0 也放得下（读数最宽约 97px + 分隔符与内边距 ≈ 131px
   < 186px 上限）。 */
.dim-jh-badgeReading { flex: none; min-width: 0; white-space: nowrap; }
/* 「这个读数不完整」的标记（合计少算了账号时才出现），排在读数**之后**。
   ⚠️ 必须与读数同级 flex: none：它是**补语**，被省略号吃掉就等于没标 —— 而它要
   提醒的恰恰是「别把这个数字当真」，看不见比不标更糟。
   ⚠️ 用 warn 而不是 error：账号读不到多半是临时的（凭据过期 / 网络抖动），
   error 是留给「整个渠道都不可用」的（那会走 empty 模式）。
   字号小一档并压紧行高，让它读起来像角标而不是读数的一部分。 */
.dim-jh-badgeWarn { flex: none; font-size: 10px; line-height: 1; color: var(--dsw-alias-state-warn-primary); cursor: help; }

/* ② 窄屏收敛：只留状态点，文字与 chevron 让位给模型选择器。
 * 阈值 720px 是实测桌面版在 100% 缩放下「徽标 216px + 模型选择器 ≥ 240px」的临界值。
 * ⚠️⚠️ 本段的**位置**是功能性的，不要为了排版把它挪到 .dim-jh-badgeText 之前：
 * 它与 ".dim-jh-badgeText { display: flex }" 特异性相同（都是单个类），
 * 靠**源顺序**决定胜负 —— 一旦被排到前面，窄屏下 display:none 会被
 * 后面的 display:flex 覆盖，文字重新出现并挤压模型选择器（静默回归）。 */
@media (max-width: 720px) { .dim-jh-badgeText, .dim-jh-badgeBtn > svg, .dim-jh-badgeBtn > .dim-jh-badgeChevron { display: none; } .dim-jh-badgeBtn { max-width: none; padding: 2px 6px; } }
.dim-jh-badgeDot { width: 5px; height: 5px; flex: none; border-radius: 999px; background: var(--dsw-alias-state-success-primary); }
.dim-jh-badgeDot[data-tone="warn"] { background: var(--dsw-alias-state-warn-primary); }
.dim-jh-badgeDot[data-tone="error"] { background: var(--dsw-alias-state-error-primary); }
.dim-jh-badgeDot[data-tone="muted"] { background: var(--dsw-alias-label-tertiary); }
.dim-jh-badgePop { position: absolute; bottom: calc(100% + 8px); right: 0; z-index: 40; width: 280px; max-width: min(280px, 86vw); max-height: 58vh; overflow-y: auto; display: flex; flex-direction: column; padding: 9px 10px 10px; border: .5px solid var(--dsw-alias-border-l2); border-radius: 11px; background: var(--dsw-alias-bg-layer-1); box-shadow: 0 1px 2px rgba(0, 0, 0, .06), 0 8px 24px rgba(0, 0, 0, .14); text-align: left; white-space: normal; }
/* 窄屏时徽标只留状态点，弹窗也随之收窄（否则它会盖住模型选择器）。
 * 宽度写 min() 而非媒体查询覆盖：弹窗是 absolute，媒体查询命中时按钮虽已
 * 收窄，但弹窗仍按 280px 渲染会显得与触发点不匹配。 */
@media (max-width: 720px) { .dim-jh-badgePop { width: 220px; max-width: min(220px, 76vw); } }
.dim-jh-badgeHead { display: flex; align-items: center; gap: 5px; padding-bottom: 7px; }
.dim-jh-badgeTitle { flex: none; max-width: 118px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; font-size: 11.5px; font-weight: 600; color: var(--dsw-alias-label-primary); }
.dim-jh-badgeAt { flex: 1; min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; font-size: 10px; color: var(--dsw-alias-label-tertiary); font-variant-numeric: tabular-nums; }
/* 刷新：默认**无边框无底色**（降低视觉重量），hover 才浮起；文字说明放 title/aria-label
   ⚠️ 2026-10-02 用户反馈「浅色模式下按钮和线条不太明显」：这里从「完全透明」
   改回**主题描边 + layer-2 底**（浅色下 layer-2 与弹窗底色太接近，靠描边才立得住）。 */
.dim-jh-badgeRefresh { flex: none; width: 20px; height: 20px; display: grid; place-items: center; border: .5px solid var(--dsw-alias-border-l2); border-radius: 6px; background: var(--dsw-alias-bg-layer-2); color: var(--dsw-alias-label-secondary); font: inherit; font-size: 12px; line-height: 1; cursor: pointer; transition: color .15s ease, background .15s ease, border-color .15s ease; }
.dim-jh-badgeRefresh:hover:not(:disabled) { border-color: color-mix(in srgb, #1677ff 45%, var(--dsw-alias-border-l2)); background: var(--dsw-alias-bg-layer-3); color: var(--dsw-alias-brand-primary); }
.dim-jh-badgeRefresh:disabled { opacity: .5; cursor: default; }
/* 自动签到状态灯：与刷新键**同尺寸同描边**（浅色下靠描边才立得住），紧挨它左侧。
   ⚠️ 状态**不只用颜色**表达：灯本身有形态差异（关=空心环 / 开=实心点 /
   今天已跑=实心点带外环 / 进行中=省略号），且 title 与 aria-label 都带完整文字说明，
   故色觉障碍与读屏都能分辨「开还是关、今天跑没跑」。 */
.dim-jh-badgeAuto { flex: none; width: 20px; height: 20px; display: grid; place-items: center; border: .5px solid var(--dsw-alias-border-l2); border-radius: 6px; background: var(--dsw-alias-bg-layer-2); color: var(--dsw-alias-label-tertiary); font: inherit; font-size: 12px; line-height: 1; cursor: pointer; transition: color .15s ease, background .15s ease, border-color .15s ease; }
.dim-jh-badgeAuto:hover { border-color: color-mix(in srgb, #1677ff 45%, var(--dsw-alias-border-l2)); background: var(--dsw-alias-bg-layer-3); }
.dim-jh-badgeAutoDot { width: 7px; height: 7px; box-sizing: border-box; border-radius: 999px; background: transparent; border: 1.5px solid currentColor; }
.dim-jh-badgeAuto[data-state="on"],
.dim-jh-badgeAuto[data-state="done"] { color: var(--dsw-alias-state-success-primary); }
.dim-jh-badgeAuto[data-state="on"] .dim-jh-badgeAutoDot,
.dim-jh-badgeAuto[data-state="done"] .dim-jh-badgeAutoDot { background: currentColor; border: 0; }
.dim-jh-badgeAuto[data-state="done"] .dim-jh-badgeAutoDot { box-shadow: 0 0 0 2px color-mix(in srgb, var(--dsw-alias-state-success-primary) 28%, transparent); }
.dim-jh-badgeAuto[data-running="true"] { color: var(--dsw-alias-brand-primary); }
/* ⚠️ 这里曾有一枚「自动签到 已关闭 / 今天已完成」的小胶囊（右对齐在签到按钮上方）。
   用户 2026-10-02 反馈「新加的这个感觉有点不是太好看」，改为在「全部渠道签到」
   按钮文案后加「（自动）」后缀（只在开关打开时加）⇒ 相关样式整段删除。
   状态本身的说明仍由右上角状态灯的 title 承载。 */
/* **常驻**的自动签到状态文字（用户 2026-10-02：自动签到下也要显示各渠道状态，
   但**不要自动消失**，改为手动关闭 ⇒ 小按钮在文字**上方**）。
   ⚠️ 与 .dim-jh-badgeNotice（手动签到结果，8s/20s 自动消失）是两种语义，
   样式刻意区分：这里用中性底 + 细描边（「状态」），那里用带色调的提示块（「回执」）。 */
.dim-jh-badgeAutoStatus { margin-top: 5px; padding: 5px 6px 6px; border: .5px solid var(--dsw-alias-border-l2); border-radius: 7px; background: var(--dsw-alias-bg-layer-2); }
.dim-jh-badgeAutoCloseRow { display: flex; justify-content: flex-end; margin-bottom: 2px; }
.dim-jh-badgeAutoClose { width: 14px; height: 14px; display: grid; place-items: center; border: .5px solid var(--dsw-alias-border-l2); border-radius: 4px; background: var(--dsw-alias-bg-layer-1); color: var(--dsw-alias-label-tertiary); font: inherit; font-size: 10px; line-height: 1; cursor: pointer; transition: color .15s ease, background .15s ease, border-color .15s ease; }
.dim-jh-badgeAutoClose:hover { border-color: color-mix(in srgb, #1677ff 45%, var(--dsw-alias-border-l2)); background: var(--dsw-alias-bg-layer-3); color: var(--dsw-alias-brand-primary); }
.dim-jh-badgeAutoStatusHead { font-size: 10.5px; line-height: 1.5; color: var(--dsw-alias-label-secondary); }
.dim-jh-badgeAutoChannels { display: flex; flex-wrap: wrap; gap: 2px 5px; margin-top: 3px; font-size: 10px; line-height: 1.6; color: var(--dsw-alias-label-tertiary); }
/* 分隔符跟在条目**后面**（不是用 ::before 加在下一个前面）：9 个渠道在 280px 里必然
   换行，::before 的写法会让换行处那一行**以孤立的点开头**（预览里实测到了）；
   ::after 则表现为行尾的「·」，与行内文本的分隔习惯一致。 */
.dim-jh-badgeAutoChannel:not(:last-child)::after { content: " ·"; opacity: .6; }
/* 偏好：分段控件（未选中透明、选中浮起），比三个独立胶囊更紧凑整齐 */
.dim-jh-badgePref { display: flex; gap: 2px; padding: 2px; border: .5px solid var(--dsw-alias-border-l2); border-radius: 7px; background: var(--dsw-alias-bg-layer-2); }
.dim-jh-badgePrefBtn { flex: 1; min-width: 0; padding: 2px; border: 0; border-radius: 5px; background: transparent; color: var(--dsw-alias-label-secondary); font: inherit; font-size: 10px; line-height: 1.5; white-space: nowrap; cursor: pointer; transition: background .15s ease, color .15s ease; }
.dim-jh-badgePrefBtn:hover { color: var(--dsw-alias-label-primary); }
/* 选中项自带描边：浅色下只靠白色底与底色区分太弱 */
.dim-jh-badgePrefBtn[aria-pressed="true"] { border: .5px solid color-mix(in srgb, var(--dsw-alias-brand-primary) 45%, var(--dsw-alias-border-l2)); background: var(--dsw-alias-bg-layer-1); color: var(--dsw-alias-brand-primary); font-weight: 600; box-shadow: 0 1px 2px rgba(0, 0, 0, .06); }
/* 分区：细分隔线分组，间距 5px（比 4px 多 1px 呼吸：账号备注是 10px 灰字，
   紧贴下一个账号名会读成同一块；再大就不「小巧」了）
   ⚠️ 分隔线用**全不透明**的 border-l2：此前用 color-mix 降到 75%，浅色下几乎看不见。 */
.dim-jh-badgeSection { display: flex; flex-direction: column; gap: 5px; margin-top: 7px; padding-top: 7px; border-top: .5px solid var(--dsw-alias-border-l2); }
.dim-jh-badgeSectionTitle { display: flex; align-items: baseline; justify-content: space-between; gap: 8px; font-size: 10px; font-weight: 600; letter-spacing: .02em; color: var(--dsw-alias-label-tertiary); }
.dim-jh-badgeSectionSum { font-weight: 600; color: var(--dsw-alias-label-primary); font-variant-numeric: tabular-nums; }
.dim-jh-badgeRow { display: flex; flex-direction: column; gap: 1px; }
/* 一行放下「名字 …… 数值」（名字可省略号，数值不换行） */
.dim-jh-badgeRowHead { display: flex; align-items: baseline; justify-content: space-between; gap: 8px; font-size: 11px; color: var(--dsw-alias-label-primary); }
.dim-jh-badgeRowName { flex: 1; min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.dim-jh-badgeRowNote { font-size: 10px; line-height: 14px; color: var(--dsw-alias-label-tertiary); }
/* 窗口：**一行**放下 名称 / 进度条 / 百分比 / 重置倒计时。
   ⚠️ 用户 2026-10-02 明确口径（附截图）：「像两边对齐，但进度条要一样长，文字部分
   左右分别对齐」⇒ 用**共享列宽的 grid**（不是整块居中、也不是每行各自 flex）：
   - 列 1 max-content：标签统一按最宽那个对齐，**靠左**，于是三行进度条起点也一致；
   - 列 2 minmax(60px, 1fr)：进度条吃掉剩余宽度 ⇒ 三行**等长**且自适应；
   - 列 3 max-content：百分比紧跟在条后；
   - 列 4 固定 100px + text-align: right：倒计时**贴右边缘**，各行对齐。
   （早先试过「整块居中 + 固定 64px 条」——被否掉：那样两侧不对齐。） */
.dim-jh-badgeWins { display: grid; grid-template-columns: max-content minmax(60px, 1fr) max-content 100px; align-items: center; gap: 4px 6px; }
.dim-jh-badgeWin { display: contents; }
.dim-jh-badgeWinLabel { font-size: 10.5px; color: var(--dsw-alias-label-secondary); text-align: left; white-space: nowrap; }
.dim-jh-badgeWin .dim-jh-quotaBar { height: 3px; }
.dim-jh-badgeWinReset { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; font-size: 10px; color: var(--dsw-alias-label-tertiary); text-align: right; }
.dim-jh-badgeValue { flex: none; font-size: 11px; font-weight: 600; font-variant-numeric: tabular-nums; }
.dim-jh-badgeValue[data-tone="warn"] { color: var(--dsw-alias-state-warn-primary); font-weight: 500; }
.dim-jh-badgeNote { font-size: 10px; line-height: 14px; color: var(--dsw-alias-label-tertiary); }
/* 签到：两个按钮并排、等分（本渠道按钮在不支持签到时不渲染，另一个占满整行）
   ⚠️ 保留 .5px 主题描边：浅色下 layer-2 底与弹窗底色几乎同色，无描边就看不出是按钮。 */
.dim-jh-badgeClaim { gap: 5px; }
.dim-jh-badgeClaimRow { display: flex; gap: 5px; }
.dim-jh-badgeAction { flex: 1; min-width: 0; padding: 3px 7px; border: .5px solid var(--dsw-alias-border-l2); border-radius: 7px; background: var(--dsw-alias-bg-layer-2); color: var(--dsw-alias-label-primary); font: inherit; font-size: 10.5px; line-height: 1.5; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; cursor: pointer; transition: background .15s ease, color .15s ease, border-color .15s ease; }
.dim-jh-badgeAction:hover:not(:disabled) { border-color: color-mix(in srgb, #1677ff 45%, var(--dsw-alias-border-l2)); background: color-mix(in srgb, #1677ff 10%, var(--dsw-alias-bg-layer-2)); color: var(--dsw-alias-brand-primary); }
.dim-jh-badgeAction:disabled { opacity: .55; cursor: default; }
.dim-jh-badgeNotice { font-size: 10px; line-height: 14px; color: var(--dsw-alias-label-secondary); }
.dim-jh-badgeNotice[data-tone="warn"] { color: var(--dsw-alias-state-warn-primary); }
.dim-jh-badgeFail { font-size: 10px; line-height: 14px; color: var(--dsw-alias-state-error-primary); }
.dim-jh-badgeFoot { font-size: 10px; line-height: 14px; color: var(--dsw-alias-label-tertiary); }
/* 「展开其余 N 个账号」/「收起」：纯文字按钮，左对齐、不占满宽。
   ⚠️ 刻意**不复用** .dim-jh-badgeAction：那条规则是 flex:1 的实心按钮（用于签到），
   放在明细列表下面会抢走视觉焦点 —— 用户要的是「明细可以收起来」，不是一个主操作。
   align-self: flex-start 是因为父容器 .dim-jh-badgePop 是 flex 列（默认 stretch）。 */
.dim-jh-badgeMore { align-self: flex-start; margin-top: 3px; padding: 1px 0; border: 0; background: none; font: inherit; font-size: 10px; line-height: 1.6; color: var(--dsw-alias-link); cursor: pointer; }
.dim-jh-badgeMore:hover { color: var(--dsw-alias-brand-primary); text-decoration: underline; }

/* ── 聚合 provider 的面板（AggregatePanel）──
   ⚠️ 本文件是模板字符串 ⇒ 注释里**禁止反引号**（会提前闭合）。
   ⚠️ 带属性选择器的规则必须放在**基础规则之后**（model-filter.spec.ts 的 ruleOf()
      按源码顺序取第一条匹配规则，写在前面会让它误取到新块、跳过基础规则）。 */
.dim-jh-aggPanel { padding: 16px 20px; overflow-y: auto; display: flex; flex-direction: column; gap: 12px; }
.dim-jh-aggIntro { display: flex; flex-direction: column; gap: 4px; }
.dim-jh-aggTitle { margin: 0; font-size: 16px; font-weight: 600; color: var(--dsw-alias-label-primary, #1a1a1a); }
.dim-jh-aggIntroLine { margin: 0; font-size: 12px; line-height: 1.7; color: var(--dsw-alias-label-secondary); }
.dim-jh-aggIntroWarn { margin: 0; font-size: 12px; line-height: 1.7; color: var(--dsw-alias-label-tertiary); }
.dim-jh-aggToolbar { display: flex; align-items: center; gap: 10px; }
.dim-jh-aggCount { font-size: 12px; color: var(--dsw-alias-label-secondary); }
.dim-jh-aggRefresh { padding: 3px 10px; border: 1px solid var(--dsw-alias-border-secondary); border-radius: 6px; background: none; font: inherit; font-size: 12px; color: var(--dsw-alias-label-primary); cursor: pointer; }
.dim-jh-aggRefresh:disabled { opacity: 0.6; cursor: default; }
.dim-jh-aggNotice { padding: 6px 10px; border-radius: 6px; font-size: 12px; color: var(--dsw-alias-label-error, #c0392b); background: var(--dsw-alias-bg-error, rgba(192, 57, 43, 0.08)); }
.dim-jh-aggEmpty { padding: 12px; font-size: 12px; line-height: 1.7; color: var(--dsw-alias-label-secondary); }
/* ⚠️ 原先这里还有 .dim-jh-aggGroup / .dim-jh-aggGroupTitle（厂商分组标题），
   已按用户 2026-10-07 的要求删除 —— 渠道维度信息在**展开后的子列表**里逐条显示，
   分组标题冗余（且其中的「(2)/(5)」是**模型个数**，容易被误读成渠道个数）。
   ⚠️ 本文件的样式整体是一个模板字符串 —— 注释里**不能出现反引号**（会提前终止，
   有专门用例守着：usage-badge-client.spec.ts / zcode-channel-dialog.spec.ts）。 */
.dim-jh-aggModel { border: 1px solid var(--dsw-alias-border-secondary); border-radius: 8px; overflow: hidden; }
/* 模型平铺后的行间距：原先由 .dim-jh-aggGroup 的 gap: 4px 提供，分组去掉后
   改由相邻兄弟选择器承担（与 .dim-jh-railGroup + .dim-jh-railGroup 同一模式）。 */
.dim-jh-aggModel + .dim-jh-aggModel { margin-top: 4px; }
.dim-jh-aggModelHead { display: flex; align-items: center; gap: 8px; width: 100%; padding: 7px 10px; border: 0; background: none; font: inherit; font-size: 13px; text-align: left; color: var(--dsw-alias-label-primary); cursor: pointer; }
.dim-jh-aggModelHead:hover { background: var(--dsw-alias-bg-secondary, rgba(0, 0, 0, 0.03)); }
.dim-jh-aggModelName { flex: 1; min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.dim-jh-aggModelMeta { font-size: 11px; color: var(--dsw-alias-label-tertiary); }
/* 「候选全部被拒」提示（规格 §5.2 要求显式提示，避免用户以为模型坏了）。
   ⚠️ 用 warning 色与上面那格的中性灰区分开（它是**需要注意**的状态，不是说明文字）。 */
.dim-jh-aggAllRejected { flex: none; padding: 1px 6px; border-radius: 4px; font-size: 11px; color: var(--dsw-alias-state-warn-primary, #b45309); background: var(--dsw-alias-bg-warn, rgba(180, 83, 9, 0.10)); }
.dim-jh-aggChevron { font-size: 11px; color: var(--dsw-alias-label-tertiary); }
.dim-jh-aggCandidates { display: flex; flex-direction: column; border-top: 1px solid var(--dsw-alias-border-secondary); }
.dim-jh-aggCandidate { display: flex; align-items: center; gap: 8px; padding: 5px 10px 5px 20px; font-size: 12px; }
.dim-jh-aggCandidate + .dim-jh-aggCandidate { border-top: 1px solid var(--dsw-alias-border-tertiary, rgba(0, 0, 0, 0.04)); }
.dim-jh-aggCandidateRejected { opacity: 0.55; }
.dim-jh-aggCandidateLabel { flex: 1; min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; color: var(--dsw-alias-label-secondary); }
/* 「按临期排序」后显示在候选行右侧的到期提示（相对时间，如「3 天」）。
   ⚠️ flex: none 让它不参与压缩（候选名那一格已经 flex:1 吃掉剩余宽度）。 */
.dim-jh-aggCandidateExpiry { flex: none; font-size: 11px; color: var(--dsw-alias-label-tertiary); }
.dim-jh-aggToggle { flex: none; padding: 2px 8px; border: 1px solid var(--dsw-alias-border-secondary); border-radius: 5px; background: none; font: inherit; font-size: 11px; color: var(--dsw-alias-link); cursor: pointer; }
.dim-jh-aggToggleOff { color: var(--dsw-alias-label-tertiary); text-decoration: line-through; }
.dim-jh-aggFooter { display: flex; flex-direction: column; gap: 2px; padding-top: 8px; border-top: 1px solid var(--dsw-alias-border-secondary); }
.dim-jh-aggFooter p { margin: 0; font-size: 11px; line-height: 1.7; color: var(--dsw-alias-label-tertiary); }

`;
  var injected = false;
  function installJetHubStyles() {
    if (injected) return () => {
    };
    injected = true;
    const style = document.createElement("style");
    style.textContent = STYLES;
    document.head.appendChild(style);
    return () => {
      style.remove();
      injected = false;
    };
  }

  // entry.js
  var root = null;
  function mount(el, opts = {}) {
    installJetHubStyles();
    const rpcCall = opts.rpcCall || (async (endpoint, payload) => {
      console.warn("[JetHubUI] no rpcCall provided, endpoint =", endpoint, payload);
      return { ok: false, error: "no rpcCall" };
    });
    root = (0, import_client.createRoot)(el);
    root.render(React3.createElement(JetHubPage, { close: opts.onClose || (() => {
    }), rpcCall }));
  }
  function unmount() {
    if (root) {
      root.unmount();
      root = null;
    }
  }
  window.JetHubUI = { mount, unmount, version: "standalone-1" };
})();
