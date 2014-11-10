package hr.element.sphinx.server;

import java.io.ByteArrayOutputStream;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.sphx.api.SphinxException;

public class SphinxHttpServlet extends HttpServlet{

  private static final long serialVersionUID = 1L;
  private SphinxSearch sphinx = null;

  public void init(ServletConfig servletConfig) throws ServletException {
    final String configPath = servletConfig.getServletContext().getRealPath("/WEB-INF/config.xml");
    this.sphinx = new SphinxSearch(configPath);
  }

  protected Map<String, Object> parseParams(HttpServletRequest req)
  {
    Map<String, Object> queryParams = new HashMap<String, Object>();
    queryParams.put("limit",  null);
    queryParams.put("offset", null);

    for (Map.Entry<String, Object> i : queryParams.entrySet()) {
      try {
        i.setValue(Integer.parseInt(req.getParameter(i.getKey())));
      } catch (Exception e) { }
    }

    final String order = req.getParameter("order");
    queryParams.put("order", order == null ? null : order.split(","));

    return queryParams;
  }

  @Override
  protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    InputStream inputStream = req.getInputStream();

    byte[] buffer = new byte[8192];
    int bytesRead;
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    while ((bytesRead = inputStream.read(buffer)) != -1){
      output.write(buffer, 0, bytesRead);
    }

    String data = output.toString();
    final String index = req.getPathInfo().replace("/", "");

    System.out.println("\nPUT");
    System.out.println("data: " + data + "; index: " + index + "; content-type: " +req.getContentType() + "\n");

    String json = new String();

    if (data != null && !data.equals("")) {
      try {
        Map<String, Object> queryParams = parseParams(req);
          json = this.sphinx.search(
          index,
          data,
          (Integer) queryParams.get("limit"),
          (Integer) queryParams.get("offset"),
          (String[]) queryParams.get("order")
        );
      } catch (SphinxException e) {
        e.printStackTrace();
      }
    } else {
      json = "\"No text in request body found\"";
    }

    resp.setContentType("application/json");
    resp.setCharacterEncoding("UTF-8");

    PrintWriter out = resp.getWriter();
    out.write(json);
    out.flush();
  }
}
