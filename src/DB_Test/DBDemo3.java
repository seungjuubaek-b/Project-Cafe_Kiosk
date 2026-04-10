package DB_Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class DBDemo3 {
  public static void main(String[] args) {
    Connection con = DBDemo.makeConnection();
    String sql = "UPDATE clerk SET name = ? WHERE (id = ?)";

    PreparedStatement stmt = null;
    try {
      stmt = con.prepareStatement(sql);
      stmt.setString(1,"김오리");
      stmt.setInt(2,1);
      int count = stmt.executeUpdate();
      if(count > 0) {
        System.out.println(count + " 건의 데이터가 수정되었습니다.");
      } else {
        System.out.println("데이터 수정에 실패했습니다.");
      }
    } catch (SQLException e) {
      System.out.println("데이터 수정에 실패했습니다.");
    }
  } // DELETE FROM `test`.`table1` WHERE (`id` = '4');

}