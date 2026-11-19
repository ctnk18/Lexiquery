<%@ include file="/WEB-INF/views/header.jspf" %>
<main class="page">
  <div class="centered">
    <div class="code">!</div>
    <h1>Something went wrong</h1>
    <p>The request could not be completed. Please go back and try again.</p>
    <a class="btn" href="<c:url value='/query'/>">Back to query</a>
  </div>
</main>
<%@ include file="/WEB-INF/views/footer.jspf" %>
