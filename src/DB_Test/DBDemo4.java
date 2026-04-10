package DB_Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class DBDemo4 {
  public static void main(String[] args) {
    Connection con = DBDemo.makeConnection();
    String sql = "DELETE FROM product WHERE (id = ?)";

    PreparedStatement stmt = null;
    try {
      stmt = con.prepareStatement(sql);
      stmt.setInt(1, 9);
      int count = stmt.executeUpdate();

      stmt.setInt(1, 11);
      count = stmt.executeUpdate();

      stmt.setInt(1, 12);
      count = stmt.executeUpdate();
      if(count > 0) {
        System.out.println(count + " 건의 데이터가 삭제되었습니다.");
      } else {
        System.out.println("데이터 삭제에 실패했습니다.");
      }
    } catch (SQLException e) {
      System.out.println("데이터 삭제에 실패했습니다.");
    }
  } // DELETE FROM `test`.`table1` WHERE (`id` = '4');

}