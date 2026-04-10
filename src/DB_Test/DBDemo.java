package DB_Test;

import java.sql.*;


public class DBDemo {
  public static Connection makeConnection() {
    String url = "jdbc:mysql://localhost:3306/cafe_kiosk";

    Connection con = null;
    try {
      Class.forName("com.mysql.cj.jdbc.Driver");

      System.out.println("1. 데이터베이스 연결중 ...");
      con = DriverManager.getConnection(url, "root", "1111");
      System.out.println("1. 데이터베이스 연결 성공");
    } catch (ClassNotFoundException e) {
      System.out.println("JDBC 드라이버를 찾지 못했습니다...");
    } catch (SQLException e) {
      System.out.println("데이터베이스 연결 실패");
      System.out.println(e.getMessage());
    }
    return con;

  }

  public static void main(String[] args) {
    Connection con = makeConnection();// MySql 드라이버 연결하고 접속한 conn 을 반환해 주는 메소드 호출
    String sql = "SELECT * FROM product where productid = ? and productname= ? ";

    PreparedStatement stmt = null;
    try {
      stmt = con.prepareStatement(sql);
      stmt.setInt(1, 1);
      stmt.setString(2, "에스프레소");
      ResultSet rs = stmt.executeQuery();

      while(rs.next()) {
        System.out.println(rs.getString("productname"));
        System.out.println(rs.getString("price"));
      }

      stmt.setInt(1, 2);
      stmt.setString(2, "손흥민");
      rs = stmt.executeQuery();
      while(rs.next()) {
        System.out.println(rs.getString("name"));
        System.out.println(rs.getString("address"));
      }

    } catch (SQLException e) {
      throw new RuntimeException(e);
    }

  }

}
