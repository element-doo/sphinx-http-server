package hr.element.sphinx.server;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.sphx.api.SphinxException;

public class SphinxHttpServlet extends HttpServlet{

  private static final long serialVersionUID = 1L;

//  @Override
//  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
//
//    String text  = "";
//
//    final String index = req.getPathInfo().replace("/", "");
//
//    System.out.println("GET");
//    System.out.println("index: " + index);
//
//    String json = new String();
//    text = "elezovic";
//
//    if (index != null && text != null) {
//      SphinxSearch sphinx = new SphinxSearch(index, text);
//      try {
//        json = sphinx.search();
//      } catch (SphinxException e) {
//        e.printStackTrace();
//      }
//    }
//
//    resp.setContentType("application/json");
//    resp.setCharacterEncoding("UTF-8");
//
//    PrintWriter out = resp.getWriter();
//    out.write(json);
//    out.flush();
//  }

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
      SphinxSearch sphinx = new SphinxSearch(index, data);
      try {
        json = sphinx.search();
      } catch (SphinxException e) {
        e.printStackTrace();
      }
    } else {
      json = "No text in request body found";
    }

    resp.setContentType("application/json");
    resp.setCharacterEncoding("UTF-8");

    PrintWriter out = resp.getWriter();
    out.write(json);
    out.flush();
  }

}
