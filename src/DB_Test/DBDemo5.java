package DB_Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class DBDemo5 {
  public static void main(String[] args) {
    Connection con = DBDemo.makeConnection();
    String sql =
        "select * from clerk where name = '코스타'";
    Statement stmt = null;
    try {
      stmt = con.createStatement();
      stmt.executeQuery(sql);
//      if(count > 0) {
//        System.out.println(count + " 건의 데이터가 추가되었습니다.");
//      } else {
//        System.out.println("데이터 입력에 실패했습니다.");
//     }
    } catch (SQLException e) {
      System.out.println("데이터 입력에 실패했습니다.");
    }
  }

}