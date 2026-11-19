/* LexiQuery - small UI helpers. Purely cosmetic: every page works without this file.
   Loaded from the same origin, so it is allowed by the CSP (script-src 'self'). */
(function () {
  "use strict";

  function ready(fn) {
    if (document.readyState !== "loading") fn();
    else document.addEventListener("DOMContentLoaded", fn);
  }

  ready(function () {
    var question = document.getElementById("question");
    var counter = document.getElementById("qcount");

    /* character counter */
    function updateCount() {
      if (question && counter) counter.textContent = question.value.length + " / " + (question.maxLength > 0 ? question.maxLength : 500);
    }
    if (question) {
      question.addEventListener("input", updateCount);
      updateCount();

      /* Ctrl/Cmd + Enter submits */
      question.addEventListener("keydown", function (e) {
        if ((e.ctrlKey || e.metaKey) && e.key === "Enter" && question.form) {
          if (question.form.checkValidity()) question.form.requestSubmit();
          else question.form.reportValidity();
        }
      });
    }

    /* example questions */
    document.querySelectorAll("[data-example]").forEach(function (btn) {
      btn.addEventListener("click", function () {
        if (!question) return;
        question.value = btn.getAttribute("data-example");
        updateCount();
        question.focus();
      });
    });

    /* copy generated SQL */
    document.querySelectorAll("[data-copy-target]").forEach(function (btn) {
      btn.addEventListener("click", function () {
        var target = document.getElementById(btn.getAttribute("data-copy-target"));
        if (!target) return;
        var text = target.textContent;
        var done = function () {
          var old = btn.textContent;
          btn.textContent = "Copied";
          setTimeout(function () { btn.textContent = old; }, 1400);
        };
        if (navigator.clipboard && window.isSecureContext) {
          navigator.clipboard.writeText(text).then(done, function () {});
        } else {
          var ta = document.createElement("textarea");
          ta.value = text;
          ta.setAttribute("readonly", "");
          ta.className = "sr-copy";
          document.body.appendChild(ta);
          ta.select();
          try { document.execCommand("copy"); done(); } catch (err) { /* ignore */ }
          document.body.removeChild(ta);
        }
      });
    });

    /* busy state on long-running forms */
    document.querySelectorAll("form[data-loading]").forEach(function (form) {
      form.addEventListener("submit", function () {
        var btn = form.querySelector("button[type=submit]");
        if (!btn) return;
        var label = form.getAttribute("data-loading");
        setTimeout(function () {
          btn.disabled = true;
          btn.textContent = label;
        }, 0);
      });
    });

    /* local, readable timestamps */
    document.querySelectorAll("time.ts").forEach(function (el) {
      var raw = el.getAttribute("datetime") || el.textContent;
      var d = new Date(String(raw).replace(/(\.\d{3})\d+/, "$1"));
      if (isNaN(d.getTime())) return;
      el.textContent = d.toLocaleString("en-IN", { day: "2-digit", month: "short", year: "numeric", hour: "numeric", minute: "2-digit" });
      el.title = d.toISOString();
    });

    /* right-align numeric result columns */
    document.querySelectorAll("table.results").forEach(function (table) {
      var rows = table.tBodies[0] ? Array.prototype.slice.call(table.tBodies[0].rows) : [];
      var heads = table.tHead ? table.tHead.rows[0].cells : [];
      for (var c = 0; c < heads.length; c++) {
        var numeric = rows.length > 0;
        for (var r = 0; r < rows.length && numeric; r++) {
          var cell = rows[r].cells[c];
          var v = cell ? cell.textContent.trim() : "";
          if (v !== "" && v !== "null" && !/^-?\d+(\.\d+)?$/.test(v)) numeric = false;
        }
        if (!numeric) continue;
        heads[c].className += " num";
        rows.forEach(function (row) { if (row.cells[c]) row.cells[c].className += " num"; });
      }
    });

    /* accuracy bars (width is set from script, so no inline style attribute is needed) */
    var bars = document.querySelectorAll(".bar i[data-pct]");
    if (bars.length) {
      requestAnimationFrame(function () {
        bars.forEach(function (bar) {
          var pct = parseFloat(String(bar.getAttribute("data-pct")).replace(",", "."));
          if (!isNaN(pct)) bar.style.width = Math.max(0, Math.min(100, pct)) + "%";
        });
      });
    }

    /* history filter */
    var filter = document.getElementById("filter");
    var body = document.getElementById("history-body");
    if (filter && body) {
      filter.addEventListener("input", function () {
        var q = filter.value.trim().toLowerCase();
        body.querySelectorAll("tr[data-row]").forEach(function (row) {
          row.hidden = q !== "" && row.textContent.toLowerCase().indexOf(q) === -1;
        });
      });
    }
  });
})();
