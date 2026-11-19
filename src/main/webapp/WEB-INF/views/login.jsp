<%@ include file="/WEB-INF/views/header.jspf" %>
<main class="auth">
  <section class="auth-intro">
    <p class="eyebrow">Natural language to SQL</p>
    <h1>Ask your database a question in plain English.</h1>
    <p class="lead">Type what you want to know. LexiQuery writes the query, checks it, and shows you the result along with the SQL it ran.</p>
    <ol class="points">
      <li><span class="no">01</span><div><b>Checked before it runs</b><span>Only a single, plain SELECT statement is ever allowed through.</span></div></li>
      <li><span class="no">02</span><div><b>Read-only access</b><span>Queries run on a database account that cannot change data.</span></div></li>
      <li><span class="no">03</span><div><b>Role-based tables</b><span>What you can query depends on your role.</span></div></li>
    </ol>
  </section>

  <section class="auth-form">
    <div class="auth-card">
      <h2>Sign in</h2>
      <p class="hint">Use the account provided to you.</p>
      <c:if test="${param.error != null}"><div class="notice error"><div><strong>Could not sign in</strong>Invalid credentials, or the account is temporarily locked.</div></div></c:if>
      <c:if test="${param.logout != null}"><div class="notice ok"><div><strong>Signed out</strong>You have been signed out.</div></div></c:if>
      <form action="<c:url value='/login'/>" method="post">
        <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
        <label class="field"><span class="field-label">Username</span>
          <input type="text" name="username" autocomplete="username" required autofocus></label>
        <label class="field"><span class="field-label">Password</span>
          <input type="password" name="password" autocomplete="current-password" required></label>
        <button type="submit" class="btn block">Sign in</button>
      </form>
    </div>
  </section>
</main>
<%@ include file="/WEB-INF/views/footer.jspf" %>
