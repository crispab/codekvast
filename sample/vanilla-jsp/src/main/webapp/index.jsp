<%@ page import="sample.vanilla_jsp.VanillaUtils" %>
<h1>Hello, World from vanilla_jsp.war!</h1>

<p>Time is now <%= VanillaUtils.getCurrentDate()%>
</p>

<ul>
    <li><a href="page2.jsp">page2</a></li>
    <li><a href="foobar/page3.jsp">page3</a></li>
</ul>

