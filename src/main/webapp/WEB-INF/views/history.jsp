<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="active" value="history" scope="request"/>
<%@ include file="/WEB-INF/views/header.jspf" %>
<main class="page">
  <div class="page-head">
    <p class="eyebrow">History</p>
    <h1>Your recent queries</h1>
    <p class="lead">The last 50 questions you asked, with the SQL that was generated for each.</p>
  </div>

  <section class="panel">
    <div class="panel-head">
      <h2>Query log</h2>
      <div class="toolbar"><input type="search" id="filter" placeholder="Filter by question or SQL" aria-label="Filter history"></div>
    </div>
    <div class="table-wrap">
      <table>
        <thead><tr><th>Date</th><th>Question</th><th>SQL</th><th>Engine</th><th>Status</th><th class="num">Rows</th><th class="num">Time (ms)</th><th>Feedback</th></tr></thead>
        <tbody id="history-body">
          <c:forEach items="${items}" var="h">
            <tr data-row>
              <td class="when"><time class="ts" datetime="${h.createdAt}"><c:out value="${h.createdAt}"/></time></td>
              <td class="question"><c:out value="${h.question}"/></td>
              <td class="sqlcell"><code><c:out value="${h.generatedSql}"/></code></td>
              <td><c:out value="${h.strategy}"/></td>
              <td><span class="badge ${h.status}"><c:out value="${h.status}"/></span></td>
              <td class="num"><c:out value="${h.rowCount}"/></td>
              <td class="num"><c:out value="${h.latencyMs}"/></td>
              <td><c:choose>
                <c:when test="${h.feedback == 1}"><span class="thumb up">Correct</span></c:when>
                <c:when test="${h.feedback == -1}"><span class="thumb down">Wrong</span></c:when>
                <c:otherwise><span class="dash">&ndash;</span></c:otherwise>
              </c:choose></td>
            </tr>
          </c:forEach>
          <c:if test="${empty items}"><tr><td colspan="8" class="empty">No queries yet. Ask your first question on the Query page.</td></tr></c:if>
        </tbody>
      </table>
    </div>
  </section>
</main>
<%@ include file="/WEB-INF/views/footer.jspf" %>
