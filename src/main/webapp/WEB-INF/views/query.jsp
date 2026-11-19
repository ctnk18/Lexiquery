<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="active" value="query" scope="request"/>
<%@ include file="/WEB-INF/views/header.jspf" %>
<main class="page">
  <div class="page-head">
    <p class="eyebrow">Query</p>
    <h1>Ask a question about your data</h1>
    <p class="lead">Describe what you want in everyday words. The generated SQL is shown with the result, so you can always see what was run.</p>
  </div>

  <form action="<c:url value='/query'/>" method="post" data-loading="Running&hellip;">
    <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
    <section class="panel">
      <div class="panel-body">
        <label class="field-label" for="question">Your question</label>
        <textarea id="question" name="question" rows="4" maxlength="500" placeholder="e.g. top 3 customers by total order value in 2024" required><c:out value="${question}"/></textarea>
        <div class="field-meta">
          <div class="examples">
            <span class="section-label">Try</span>
            <button type="button" class="example" data-example="show all customers">show all customers</button>
            <button type="button" class="example" data-example="how many orders">how many orders</button>
            <button type="button" class="example" data-example="top 3 products by price">top 3 products by price</button>
            <button type="button" class="example" data-example="average product price per category">average price per category</button>
          </div>
          <span class="counter" id="qcount">0 / 500</span>
        </div>

        <fieldset class="engine">
          <legend class="section-label">Engine</legend>
          <div class="engine-options">
            <c:forEach items="${strategies}" var="s">
              <label class="engine-opt">
                <input type="radio" name="strategy" value="${s}" ${s == selected ? 'checked' : ''}>
                <span class="engine-box">
                  <b><c:out value="${s}"/></b>
                  <small>
                    <c:choose>
                      <c:when test="${s eq 'RULE'}">Fixed sentence patterns. Fast, but only handles simple questions.</c:when>
                      <c:when test="${s eq 'LLM'}">A language model reads the table layout and writes the SQL.</c:when>
                      <c:otherwise>Simple questions use rules, harder ones go to the language model.</c:otherwise>
                    </c:choose>
                  </small>
                </span>
              </label>
            </c:forEach>
          </div>
        </fieldset>
      </div>
      <div class="panel-foot">
        <span class="hint">Press <span class="kbd">Ctrl</span> + <span class="kbd">Enter</span> to run</span>
        <button type="submit" class="btn">Run query</button>
      </div>
    </section>
  </form>

  <c:if test="${not empty error}">
    <div class="notice error"><div><strong>Check your question</strong><c:out value="${error}"/></div></div>
  </c:if>

  <c:if test="${not empty result}">
    <section class="panel">
      <div class="result-head">
        <span class="badge ${result.status}"><c:out value="${result.status}"/></span>
        <dl class="facts">
          <c:if test="${not empty result.strategyUsed}"><div><dt>Engine</dt><dd><c:out value="${result.strategyUsed}"/></dd></div></c:if>
          <c:if test="${not empty result.complexity}"><div><dt>Complexity</dt><dd><c:out value="${result.complexity}"/></dd></div></c:if>
          <div><dt>Time</dt><dd><c:out value="${result.latencyMs}"/> ms</dd></div>
          <div><dt>Attempts</dt><dd><c:out value="${result.attempts}"/></dd></div>
        </dl>
      </div>

      <div class="panel-body">
        <c:if test="${not empty result.message}">
          <div class="notice ${result.status}">
            <div>
              <strong>
                <c:choose>
                  <c:when test="${result.status eq 'BLOCKED'}">Stopped by the security check</c:when>
                  <c:otherwise>This query could not be completed</c:otherwise>
                </c:choose>
              </strong>
              <c:out value="${result.message}"/>
            </div>
          </div>
        </c:if>

        <c:if test="${not empty result.sql}">
          <div class="result-section">
            <div class="block-title"><h3>Generated SQL</h3></div>
            <div class="codebox">
              <div class="codebar"><span>MySQL</span><button type="button" class="copy" data-copy-target="sql-out">Copy</button></div>
              <pre class="sql" id="sql-out"><c:out value="${result.sql}"/></pre>
            </div>
          </div>
        </c:if>

        <c:if test="${result.success}">
          <div class="result-section">
            <div class="block-title"><h3>Result<span class="count"><c:out value="${result.rows.size()}"/> rows</span></h3></div>
            <div class="table-wrap">
              <table class="results">
                <thead><tr><c:forEach items="${result.columns}" var="col"><th><c:out value="${col}"/></th></c:forEach></tr></thead>
                <tbody>
                  <c:forEach items="${result.rows}" var="row">
                    <tr><c:forEach items="${row}" var="cell"><td><c:out value="${cell}"/></td></c:forEach></tr>
                  </c:forEach>
                </tbody>
              </table>
            </div>
          </div>
        </c:if>
      </div>

      <c:if test="${result.success}">
        <div class="panel-foot">
          <span class="hint">Is this result correct? Approved queries are reused as examples for later questions.</span>
          <form action="<c:url value='/feedback'/>" method="post" class="inline">
            <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
            <input type="hidden" name="id" value="${result.historyId}"/>
            <span class="btn-group">
              <button class="btn small ghost" name="value" value="1" type="submit">Correct</button>
              <button class="btn small danger" name="value" value="-1" type="submit">Wrong</button>
            </span>
          </form>
        </div>
      </c:if>
    </section>
  </c:if>
</main>
<%@ include file="/WEB-INF/views/footer.jspf" %>
