<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="active" value="admin" scope="request"/>
<%@ include file="/WEB-INF/views/header.jspf" %>
<main class="page">
  <div class="page-head">
    <p class="eyebrow">Admin</p>
    <h1>Overview and evaluation</h1>
    <p class="lead">Query outcomes across all users, a benchmark that compares the three engines, and the security audit.</p>
  </div>

  <div class="stats">
    <div class="stat ok"><div class="label">Successful</div><div class="value"><c:out value="${success}"/></div></div>
    <div class="stat warn"><div class="label">Failed</div><div class="value"><c:out value="${failed}"/></div></div>
    <div class="stat bad"><div class="label">Blocked</div><div class="value"><c:out value="${blocked}"/></div></div>
  </div>

  <section class="panel">
    <div class="panel-head">
      <h2>Strategy evaluation</h2>
      <div class="btn-group">
        <form action="<c:url value='/admin/refresh-schema'/>" method="post" class="inline">
          <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
          <button class="btn ghost small" type="submit">Reload schema</button>
        </form>
        <form action="<c:url value='/admin/evaluate'/>" method="post" class="inline" data-loading="Running evaluation&hellip;">
          <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
          <button class="btn small" type="submit">Run evaluation</button>
        </form>
      </div>
    </div>
    <div class="panel-body">
      <p class="hint">Runs each question in <code>benchmark/sample.csv</code> through RULE, LLM and HYBRID and compares the result set with the reference SQL. This calls the LLM and can take a while.</p>
    </div>
    <c:if test="${not empty evalRows}">
      <div class="table-wrap">
        <table>
          <thead><tr><th>Strategy</th><th class="num">Questions</th><th class="num">Correct</th><th class="num">Failed / blocked</th><th>Execution accuracy</th><th class="num">Avg latency (ms)</th></tr></thead>
          <tbody>
            <c:forEach items="${evalRows}" var="e">
              <tr>
                <td><strong><c:out value="${e.strategy}"/></strong></td>
                <td class="num"><c:out value="${e.total}"/></td>
                <td class="num"><c:out value="${e.correct}"/></td>
                <td class="num"><c:out value="${e.failed}"/></td>
                <td><div class="acc"><span class="bar"><i data-pct="${e.accuracy}"></i></span><span class="accnum"><c:out value="${e.accuracy}"/>%</span></div></td>
                <td class="num"><c:out value="${e.avgLatencyMs}"/></td>
              </tr>
            </c:forEach>
          </tbody>
        </table>
      </div>
    </c:if>
  </section>

  <section class="panel">
    <div class="panel-head"><h2>Usage by engine</h2></div>
    <div class="table-wrap">
      <table>
        <thead><tr><th>Engine</th><th class="num">Successful queries</th><th class="num">Avg latency (ms)</th></tr></thead>
        <tbody>
          <c:forEach items="${stats}" var="s">
            <tr><td><c:out value="${s[0]}"/></td><td class="num"><c:out value="${s[1]}"/></td><td class="num"><c:out value="${s[2]}"/></td></tr>
          </c:forEach>
          <c:if test="${empty stats}"><tr><td colspan="3" class="empty">No data yet.</td></tr></c:if>
        </tbody>
      </table>
    </div>
  </section>

  <section class="panel">
    <div class="panel-head"><h2>Blocked requests</h2><span class="hint">Security audit, latest 20</span></div>
    <div class="table-wrap">
      <table>
        <thead><tr><th>Date</th><th>User</th><th>Question</th><th>Reason</th></tr></thead>
        <tbody>
          <c:forEach items="${blockedItems}" var="b">
            <tr>
              <td class="when"><time class="ts" datetime="${b.createdAt}"><c:out value="${b.createdAt}"/></time></td>
              <td><c:out value="${b.username}"/></td>
              <td class="question"><c:out value="${b.question}"/></td>
              <td><c:out value="${b.message}"/></td>
            </tr>
          </c:forEach>
          <c:if test="${empty blockedItems}"><tr><td colspan="4" class="empty">No blocked requests.</td></tr></c:if>
        </tbody>
      </table>
    </div>
  </section>
</main>
<%@ include file="/WEB-INF/views/footer.jspf" %>
